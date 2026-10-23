package com.critetiontech.ctvitalio.wear.session

import com.critetiontech.ctvitalio.sync.WatchSessionContract
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/** Receives login / logout updates from the phone, even when the watch app is closed. */
class SessionListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.dataItem.uri.path != WatchSessionContract.PATH) return@forEach
            when (event.type) {
                DataEvent.TYPE_CHANGED -> WatchSessionStore.apply(DataMapItem.fromDataItem(event.dataItem).dataMap)
                DataEvent.TYPE_DELETED -> WatchSessionStore.clear()
            }
        }
    }
}