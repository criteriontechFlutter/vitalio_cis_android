package com.critetiontech.ctvitalio.yonker

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

// ─── Models ───────────────────────────────────────────────────────────────────

enum class YonkerKind(
    val code: String,
    val label: String,
    /** Names the device advertises over Bluetooth: its BLE module's name, or the model (YK-BPA1). */
    private val bleNames: Set<String>,
    private val modelPrefix: String,
) {
    BP("bp", "BP Machine", setOf("BleModuleB"), "YK-BP"),
    OXIMETER("oximeter", "Pulse Oximeter", setOf("BleModuleA"), "YK-8");

    fun matches(name: String?) = name != null && (name in bleNames || name.trim().uppercase().startsWith(modelPrefix))

    /**
     * A device advertising Yonker's data service under a name we don't know yet (a newer model).
     * BP machines and oximeters share the service, so a name of the other kind rules it out.
     */
    fun matchesUnknown(name: String?, advertisesService: Boolean) =
        advertisesService && entries.none { it != this && it.matches(name) }

    companion object {
        fun byCode(code: String) = entries.firstOrNull { it.code == code }
    }
}

/** A finished BP measurement, or the oximeter's current values. [time] is when the phone received it. */
data class YonkerReading(
    val time: Instant,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val pulse: Int? = null,
    val spo2: Int? = null,
) {
    val isBloodPressure get() = systolic != null && diastolic != null
}

/** What the device tells us, in order: found → connected → measuring/readings, until it disconnects. */
sealed interface YonkerEvent {
    data object Connecting : YonkerEvent
    /** Connected and listening; the user starts a measurement on the device. */
    data object Connected : YonkerEvent
    /** BP cuff pressure while the machine inflates/deflates. */
    data class Pressure(val mmHg: Int) : YonkerEvent
    data class Reading(val reading: YonkerReading) : YonkerEvent
    /** Oximeter is on but has no valid signal (finger out or still settling). */
    data object NoSignal : YonkerEvent
}

class YonkerException(message: String) : Exception(message)

// ─── Frames ───────────────────────────────────────────────────────────────────

/**
 * Yonker's BLE modules notify short frames whose first byte is the type:
 * - BP `0x80 pressureHi pressureLo` while measuring, `0x81 sys dia pulse` when done (YK-BPA1 pads
 *   frames to 15 bytes and sends `01 00` on connect).
 * - Oximeter `0x81 spo2 pulse` about once a second.
 * Other frame types are ignored.
 */
internal object YonkerFrames {
    private const val MEASURING = 0x80
    private const val RESULT = 0x81

    fun parse(kind: YonkerKind, frame: ByteArray, now: Instant = Instant.now()): YonkerEvent? {
        if (frame.isEmpty()) return null
        val b = frame.map { it.toInt() and 0xFF }
        return when (kind) {
            YonkerKind.BP -> when {
                b[0] == MEASURING && b.size > 2 -> YonkerEvent.Pressure((b[1] shl 8) or b[2])
                b[0] == RESULT && b.size > 3 -> {
                    val (sys, dia, pulse) = Triple(b[1], b[2], b[3])
                    // Anything else is the machine reporting an error, or an oximeter frame (no pulse byte)
                    if (sys !in 40..260 || dia !in 20..200 || dia >= sys || pulse !in 25..250) null
                    else YonkerEvent.Reading(YonkerReading(now, systolic = sys, diastolic = dia, pulse = pulse))
                }
                else -> null
            }
            YonkerKind.OXIMETER -> when {
                b[0] == RESULT && b.size > 2 -> {
                    val (spo2, pulse) = b[1] to b[2]
                    if (spo2 !in 35..100 || pulse !in 25..250) YonkerEvent.NoSignal
                    else YonkerEvent.Reading(YonkerReading(now, pulse = pulse, spo2 = spo2))
                }
                else -> null
            }
        }
    }
}

// ─── Manager ──────────────────────────────────────────────────────────────────

/**
 * Talks to Yonker BP machines and oximeters directly over BLE (no SDK, no pairing): finds the
 * device by its advertised name, connects, and turns on notifications for the data characteristic.
 * Readings are live, so the device must be measuring while connected; nothing is read from its memory.
 */
@SuppressLint("MissingPermission") // The screen asks for Nearby devices before connecting
object YonkerManager {

    private const val TAG = "YonkerManager"
    private const val SCAN_TIMEOUT_MS = 30_000L
    /** How long to scan before trying a device already paired in phone settings, which may have stopped advertising. */
    private const val BONDED_FALLBACK_MS = 8_000L

    private val SERVICE = UUID.fromString("cdeacd80-5235-4c07-8846-93a37ee6b86d")
    private val DATA = UUID.fromString("cdeacd81-5235-4c07-8846-93a37ee6b86d")
    private val CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /**
     * Finds the device, connects and emits what it sends. Completes when the device disconnects
     * (e.g. it switched itself off); fails with [YonkerException] if it can't be found or connected.
     * Cancelling the collector disconnects.
     */
    fun connect(context: Context, kind: YonkerKind): Flow<YonkerEvent> = callbackFlow {
        val app = context.applicationContext
        val adapter = app.getSystemService(BluetoothManager::class.java)?.adapter
            ?: throw YonkerException("This phone doesn't support Bluetooth.")
        if (!adapter.isEnabled) throw YonkerException("Bluetooth is turned off. Turn it on and try again.")
        val scanner = adapter.bluetoothLeScanner ?: throw YonkerException("Bluetooth is turned off. Turn it on and try again.")

        val gatt = AtomicReference<BluetoothGatt?>(null)
        val found = AtomicBoolean(false)
        val subscribed = AtomicBoolean(false)

        fun onFrame(frame: ByteArray) {
            Log.d(TAG, "frame ${frame.joinToString(" ") { "%02X".format(it) }}")
            YonkerFrames.parse(kind, frame)?.let { trySend(it) }
        }

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                Log.d(TAG, "connection state $newState status $status")
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> g.discoverServices()
                    BluetoothProfile.STATE_DISCONNECTED ->
                        // Dropping before we ever listened is a failed connect, afterwards the device just went away
                        if (subscribed.get()) close()
                        else close(YonkerException("Couldn't connect to the device. Keep it close to your phone and try again."))
                }
            }

            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                g.services.forEach { svc ->
                    Log.d(TAG, "service ${svc.uuid}: " + svc.characteristics.joinToString { "${it.uuid} props=0x%02X".format(it.properties) })
                }
                val data = g.getService(SERVICE)?.getCharacteristic(DATA)
                if (status != BluetoothGatt.GATT_SUCCESS || data == null) {
                    close(YonkerException("Connected, but this device doesn't send readings the way we expect (status $status). Please share the app log with support."))
                    return
                }
                g.setCharacteristicNotification(data, true)
                val cccd = data.getDescriptor(CCCD)
                if (cccd == null) {
                    // Some modules notify without a CCCD; listen anyway
                    subscribed.set(true)
                    trySend(YonkerEvent.Connected)
                    return
                }
                val value = if (data.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0)
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                else BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    g.writeDescriptor(cccd, value)
                } else {
                    @Suppress("DEPRECATION")
                    cccd.value = value
                    @Suppress("DEPRECATION")
                    g.writeDescriptor(cccd)
                }
            }

            override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                if (descriptor.uuid != CCCD) return
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    close(YonkerException("Couldn't start reading from the device. Please try again."))
                    return
                }
                subscribed.set(true)
                trySend(YonkerEvent.Connected)
            }

            override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray) {
                if (c.uuid == DATA) onFrame(value)
            }

            @Deprecated("Called instead of the overload above before Android 13")
            override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && c.uuid == DATA) onFrame(c.value ?: return)
            }
        }

        fun connectTo(device: BluetoothDevice, how: String) {
            if (!found.compareAndSet(false, true)) return
            Log.d(TAG, "$how ${device.name} ${device.address}")
            trySend(YonkerEvent.Connecting)
            gatt.set(device.connectGatt(app, false, gattCallback, BluetoothDevice.TRANSPORT_LE))
        }

        val seen = mutableSetOf<String>()
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val record = result.scanRecord
                val name = (record?.deviceName ?: result.device.name)?.filter { it.code in 0x20..0x7E }
                val uuids = record?.serviceUuids.orEmpty()
                val advertisesService = uuids.any { it.uuid == SERVICE }
                if (seen.add(result.device.address)) {
                    Log.d(TAG, "scan saw ${name ?: "(no name)"} ${result.device.address} rssi ${result.rssi} services $uuids " +
                        "manufacturer ${record?.manufacturerSpecificData?.let { m -> (0 until m.size()).joinToString { "0x%04X".format(m.keyAt(it)) } }}")
                }
                if (found.get()) return
                when {
                    kind.matches(name) -> connectTo(result.device, "found")
                    kind.matchesUnknown(name, advertisesService) -> connectTo(result.device, "found by service")
                    else -> return
                }
                runCatching { scanner.stopScan(this) }
            }

            override fun onScanFailed(errorCode: Int) {
                close(YonkerException("Bluetooth scan failed (error $errorCode). Turn Bluetooth off and on, then try again."))
            }
        }

        // No service filter: the modules don't advertise their service, only their name
        scanner.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), scanCallback)
        // Paired from phone settings (YK-BPA1 pairs as classic Bluetooth): it may stop advertising, so try it directly
        val bonded = adapter.bondedDevices.orEmpty().firstOrNull { kind.matches(it.name) }
        if (bonded != null) launch {
            delay(BONDED_FALLBACK_MS)
            if (!found.get()) {
                runCatching { scanner.stopScan(scanCallback) }
                connectTo(bonded, "not advertising, trying paired")
            }
        }
        launch {
            delay(SCAN_TIMEOUT_MS)
            if (found.compareAndSet(false, true)) {
                runCatching { scanner.stopScan(scanCallback) }
                close(YonkerException("Device not found. Switch it on, keep it next to your phone and try again."))
            }
        }

        awaitClose {
            if (found.compareAndSet(false, true)) runCatching { scanner.stopScan(scanCallback) }
            gatt.getAndSet(null)?.let { runCatching { it.disconnect() }; runCatching { it.close() } }
        }
    }

    /** AddVital fields for [reading]. */
    fun vitalFields(reading: YonkerReading): Map<String, String> = buildMap {
        reading.systolic?.let { put("vmValueBPSys", it.toString()) }
        reading.diastolic?.let { put("vmValueBPDias", it.toString()) }
        reading.spo2?.let { put("vmValueSPO2", it.toString()) }
        reading.pulse?.let { put("vmValuePulse", it.toString()) }
    }
}
