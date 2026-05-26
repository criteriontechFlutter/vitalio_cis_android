package com.example.vitalio_cis.viewmodel

import androidx.lifecycle.ViewModel
import com.example.vitalio_cis.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ═══════════════════════════════════════════════════════════════════════════════
//  VoiceCommandViewModel
//  Owns all voice-assistant state so the composable stays stateless.
//  The composable wires SpeechRecognizer callbacks → these methods.
// ═══════════════════════════════════════════════════════════════════════════════

/** Every stage the voice assistant passes through during a single listen session */
enum class ListeningState {
    IDLE,        // sheet closed; no recognizer running
    LISTENING,   // mic open, waiting for user to speak
    PROCESSING,  // speech captured; recognition in progress
    MATCHED,     // a route was found – navigation is about to happen
    NO_MATCH,    // speech was heard but no command keyword matched
    ERROR        // microphone or recognition failure
}

/**
 * Pairs a list of spoken trigger keywords with a navigation route and
 * a short human-readable label shown on the hint chips inside the sheet.
 */
data class VoiceCommand(
    val keywords: List<String>,
    val route: String,
    val label: String
)

class VoiceCommandViewModel : ViewModel() {

    // ── Exposed state ──────────────────────────────────────────────────────────

    private val _listeningState = MutableStateFlow(ListeningState.IDLE)
    val listeningState: StateFlow<ListeningState> = _listeningState.asStateFlow()

    /** Live partial transcript shown while the user is speaking */
    private val _spokenText = MutableStateFlow("")
    val spokenText: StateFlow<String> = _spokenText.asStateFlow()

    /** Human-readable name of the matched destination (e.g. "Vitals") */
    private val _matchedLabel = MutableStateFlow<String?>(null)
    val matchedLabel: StateFlow<String?> = _matchedLabel.asStateFlow()

    /** The Routes constant to navigate to on a successful match */
    private val _matchedRoute = MutableStateFlow<String?>(null)
    val matchedRoute: StateFlow<String?> = _matchedRoute.asStateFlow()

    // ── Full voice command catalog ─────────────────────────────────────────────
    // To add a new route: append a VoiceCommand entry here.
    // The first 12 entries are shown as hint chips in the bottom sheet.

    val commands: List<VoiceCommand> = listOf(
        VoiceCommand(
            keywords = listOf("dashboard", "home", "main screen"),
            route = Routes.DASHBOARD,
            label = "Home"
        ),
        VoiceCommand(
            keywords = listOf("vitals", "vital details", "blood pressure", "bp", "heart rate", "spo2", "temperature", "weight", "rr"),
            route = Routes.VITALS,
            label = "Vitals"
        ),
        VoiceCommand(
            keywords = listOf("medicine", "medication", "tablet", "drug"),
            route = Routes.MEDICINE,
            label = "Medicine"
        ),
        VoiceCommand(
            keywords = listOf("reminder", "reminders", "alarm"),
            route = Routes.REMINDERS,
            label = "Reminders"
        ),
        VoiceCommand(
            keywords = listOf("lab report", "lab", "test result"),
            route = Routes.LABREPORTS,
            label = "Lab Reports"
        ),
        VoiceCommand(
            keywords = listOf("diet", "diet checklist", "food"),
            route = Routes.DIETCHECKLIST,
            label = "Diet"
        ),
        VoiceCommand(
            // "fluid" alone is matched here; "fluid output" is a separate command lower in the list,
            // but because firstOrNull stops at the first match the more-specific phrase should
            // come BEFORE this one – see FLUIDOUTPUTHISTORY entry below.
            keywords = listOf("fluid intake", "fluid", "water", "milk", "juice", "tea", "coffee", "beverage"),
            route = Routes.FLUIDDATAINPUT,
            label = "Fluid Intake"
        ),
        VoiceCommand(
            keywords = listOf("symptoms", "symptom tracker", "fever", "cough", "pain"),
            route = Routes.SYMPTOMSTRACKER,
            label = "Symptoms"
        ),
        VoiceCommand(
            keywords = listOf("find doctor", "doctor", "appointment"),
            route = Routes.FINDDOCTOR,
            label = "Find Doctor"
        ),
        VoiceCommand(
            keywords = listOf("research articles", "articles", "article", "research"),
            route = Routes.RESEARCHARTICLES,
            label = "Articles"
        ),
        VoiceCommand(
            keywords = listOf("medical profile", "profile"),
            route = Routes.MEDICALPROFILE,
            label = "Profile"
        ),
        VoiceCommand(
            keywords = listOf("emergency contacts", "emergency", "sos"),
            route = Routes.EMERGENCYCONTACTS,
            label = "Emergency"
        ),

        // ── Less-common routes (shown if user speaks their keyword) ────────────

        VoiceCommand(
            // Must be listed BEFORE the generic "fluid" entry to win the firstOrNull match
            keywords = listOf("fluid output", "urine output", "output history"),
            route = Routes.FLUIDOUTPUTHISTORY,
            label = "Fluid Output"
        ),
        VoiceCommand(
            keywords = listOf("fluid history", "intake history"),
            route = Routes.FLUIDINPUTHISTORY,
            label = "Fluid History"
        ),
        VoiceCommand(
            keywords = listOf("vital history", "past vitals"),
            route = Routes.VITALHISTORY,
            label = "Vital History"
        ),
        VoiceCommand(
            keywords = listOf("connect watch", "smartwatch", "watch"),
            route = Routes.CONNECTWATCH,
            label = "Connect Watch"
        ),
        VoiceCommand(
            keywords = listOf("family health", "family history", "family"),
            route = Routes.FAMILYHEALTH,
            label = "Family Health"
        ),
        VoiceCommand(
            keywords = listOf("shared account", "shared"),
            route = Routes.SHAREDACCOUNT,
            label = "Shared Account"
        ),
        VoiceCommand(
            keywords = listOf("my observers", "observer"),
            route = Routes.MYOBSERVERS,
            label = "My Observers"
        ),
        VoiceCommand(
            keywords = listOf("prescription", "prescriptions"),
            route = Routes.PRESCRIPTION,
            label = "Prescriptions"
        ),
        VoiceCommand(
            keywords = listOf("allergies", "allergy"),
            route = Routes.ALLERGIESSCREEN,
            label = "Allergies"
        ),
        VoiceCommand(
            keywords = listOf("manage medicine", "manage medications"),
            route = Routes.MANAGE_MEDICINE,
            label = "Manage Meds"
        ),
        VoiceCommand(
            keywords = listOf("add medicine reminder", "add medicine", "new medicine"),
            route = Routes.ADDMEDICINEREMINDER,
            label = "Add Medicine"
        ),
        VoiceCommand(
            keywords = listOf("personal info", "personal information"),
            route = Routes.PERSONALIFSCREEN,
            label = "Personal Info"
        ),
        VoiceCommand(
            keywords = listOf("ai report", "smart report", "ai lab"),
            route = Routes.AIREPORT,
            label = "AI Report"
        ),
        VoiceCommand(
            keywords = listOf("interaction checker", "drug interaction", "interaction"),
            route = Routes.INTERACTIONCHECKER,
            label = "Interactions"
        ),
        VoiceCommand(
            keywords = listOf("upload report", "add report", "upload"),
            route = Routes.ADDLABRESULTS,
            label = "Upload Report"
        ),
        VoiceCommand(
            keywords = listOf("faq", "frequently asked", "help center"),
            route = Routes.FAQ,
            label = "FAQ"
        ),
        VoiceCommand(
            keywords = listOf("feedback", "suggestion"),
            route = Routes.FEEDBACK,
            label = "Feedback"
        ),

        // ── Additional routes (1.4 – full route coverage) ─────────────────────

        VoiceCommand(
            // ADDACTIVITY is tab 1 on the dashboard but also its own route
            keywords = listOf("add activity", "log activity", "activity tracker"),
            route = Routes.ADDACTIVITY,
            label = "Add Activity"
        ),
        VoiceCommand(
            // Standalone appointments overview (distinct from Find Doctor)
            keywords = listOf("view appointments", "my appointments", "appointment list"),
            route = Routes.APPOINTMENTS,
            label = "Appointments"
        ),
        VoiceCommand(
            keywords = listOf("add family member", "add member", "new family member"),
            route = Routes.ADDMEMBER,
            label = "Add Member"
        ),
        VoiceCommand(
            keywords = listOf("device connection", "connect device", "connection status"),
            route = Routes.CONNECTION,
            label = "Connection"
        ),
        VoiceCommand(
            // FLUID is a general fluid overview; FLUIDDATAINPUT is the entry screen
            keywords = listOf("fluid overview", "fluid summary", "fluid screen"),
            route = Routes.FLUID,
            label = "Fluid Overview"
        ),
        VoiceCommand(
            // SYMPTOMS is a symptom-list screen; SYMPTOMSTRACKER is the tracker dashboard
            keywords = listOf("symptom list", "browse symptoms", "symptoms list"),
            route = Routes.SYMPTOMS,
            label = "Symptom List"
        ),
        VoiceCommand(
            // DRAWER gives access to the navigation side panel
            keywords = listOf("open menu", "side menu", "navigation drawer", "drawer"),
            route = Routes.DRAWER,
            label = "Menu"
        ),
    )

    /** First 12 commands displayed as hint chips so the user knows what to say */
    val hintCommands: List<VoiceCommand> get() = commands.take(12)

    // ── State transition methods ───────────────────────────────────────────────
    // Called from the RecognitionListener inside VoiceAssistantSheet.

    /** Microphone is open; reset text and enter LISTENING state */
    fun onReadyForSpeech() {
        _listeningState.value = ListeningState.LISTENING
        _spokenText.value = ""
    }

    /** Update the displayed text with each incremental partial transcript */
    fun onPartialResult(partial: String) {
        _spokenText.value = partial
    }

    /** User stopped speaking; recognition engine is now processing */
    fun onSpeechEnded() {
        _listeningState.value = ListeningState.PROCESSING
    }

    /**
     * Final recognition result received.
     * Scans [commands] for the first keyword match (case-insensitive, substring).
     * Sets MATCHED when found, NO_MATCH otherwise.
     */
    fun onResult(spokenText: String) {
        val lowered = spokenText.lowercase()
        val match = commands.firstOrNull { cmd ->
            cmd.keywords.any { keyword -> lowered.contains(keyword.lowercase()) }
        }
        _spokenText.value = spokenText
        _matchedRoute.value = match?.route
        _matchedLabel.value = match?.label
        _listeningState.value = if (match != null) ListeningState.MATCHED else ListeningState.NO_MATCH
    }

    /** Called on SpeechRecognizer error codes */
    fun onRecognitionError() {
        _listeningState.value = ListeningState.ERROR
    }

    /** Resets all fields to IDLE – call when the sheet is dismissed */
    fun reset() {
        _listeningState.value = ListeningState.IDLE
        _spokenText.value = ""
        _matchedRoute.value = null
        _matchedLabel.value = null
    }
}
