package com.jarvis.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private val Background = Color(0xFF050B16)
private val Cyan = Color(0xFF45DFFF)
private const val HISTORY_PREFS = "jarvis_chat_history"
private const val HISTORY_KEY = "messages"

private data class ChatMessage(val text: String, val fromUser: Boolean)

class MainActivity : ComponentActivity() {
    private var speechEngine: TextToSpeech? = null
    private var speechReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speechEngine = TextToSpeech(this) { result ->
            speechReady = result == TextToSpeech.SUCCESS
            if (speechReady) speechEngine?.setSpeechRate(0.95f)
        }
        setContent {
            JarvisHome { text, languageTag ->
                if (speechReady) {
                    val locale = Locale.forLanguageTag(languageTag)
                    val languageResult = speechEngine?.setLanguage(locale)
                    if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                        languageResult == TextToSpeech.LANG_NOT_SUPPORTED
                    ) speechEngine?.setLanguage(Locale.US)
                    speechEngine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
                }
            }
        }
    }

    override fun onDestroy() {
        speechEngine?.stop()
        speechEngine?.shutdown()
        speechEngine = null
        super.onDestroy()
    }
}

@Composable
fun JarvisHome(onSpeak: (String, String) -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("SYSTEM ONLINE") }
    var selectedLanguage by remember { mutableStateOf("en-IN") }
    val history = remember {
        mutableStateListOf<ChatMessage>().apply {
            val saved = context.getSharedPreferences(HISTORY_PREFS, 0).getString(HISTORY_KEY, "[]")
            try {
                val array = JSONArray(saved)
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(ChatMessage(item.getString("text"), item.getBoolean("fromUser")))
                }
            } catch (_: Exception) { clear() }
        }
    }

    fun saveHistory() {
        val array = JSONArray()
        history.forEach { item ->
            array.put(JSONObject().put("text", item.text).put("fromUser", item.fromUser))
        }
        context.getSharedPreferences(HISTORY_PREFS, 0).edit()
            .putString(HISTORY_KEY, array.toString()).apply()
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) {
            status = "TYPE OR SPEAK A MESSAGE"
            return
        }
        history.add(ChatMessage(text.trim(), true))
        val answer = handlePhoneCommand(context, text, selectedLanguage)
            ?: localAssistantReply(text, selectedLanguage)
        history.add(ChatMessage(answer, false))
        while (history.size > 100) history.removeAt(0)
        saveHistory()
        status = "JARVIS READY"
        message = ""
        onSpeak(answer, selectedLanguage)
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenWords = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenWords.isNullOrBlank()) {
                sendMessage(spokenWords)
                status = "VOICE COMMAND PROCESSED"
            } else status = "NO SPEECH DETECTED"
        } else status = "LISTENING CANCELLED"
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startSpeechRecognition(context, selectedLanguage, speechLauncher::launch)
            status = "LISTENING..."
        } else status = "MICROPHONE PERMISSION DENIED"
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("J.A.R.V.I.S", color = Cyan, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("PERSONAL AI ASSISTANT", color = Color.LightGray, fontSize = 10.sp, letterSpacing = 3.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(status, color = Cyan, fontSize = 12.sp, letterSpacing = 1.sp)
                    TextButton(onClick = {
                        history.clear()
                        saveHistory()
                        status = "CHAT HISTORY CLEARED"
                    }) { Text("CLEAR CHAT", color = Color(0xFFFF8A80), fontSize = 11.sp) }
                }

                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(150.dp).background(
                                    Brush.radialGradient(listOf(Color(0xFF176D91), Color(0xFF102B49), Background)),
                                    CircleShape
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    modifier = Modifier.size(110.dp), shape = CircleShape,
                                    color = Color(0xFF0A1B2E), border = BorderStroke(2.dp, Cyan)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("J", color = Cyan, fontSize = 64.sp, fontWeight = FontWeight.Light)
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Your conversations will appear here", color = Color.LightGray, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(history) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (item.fromUser) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    color = if (item.fromUser) Color(0xFF075477) else Color(0xFF14283D),
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp, topEnd = 16.dp,
                                        bottomStart = if (item.fromUser) 16.dp else 4.dp,
                                        bottomEnd = if (item.fromUser) 4.dp else 16.dp
                                    ),
                                    border = BorderStroke(1.dp, if (item.fromUser) Cyan.copy(alpha = 0.35f) else Color(0xFF29445B)),
                                    modifier = Modifier.widthIn(max = 300.dp)
                                ) {
                                    Text(
                                        item.text,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Text("VOICE LANGUAGE", color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LanguageButton("English", selectedLanguage == "en-IN", Modifier.weight(1f)) { selectedLanguage = "en-IN" }
                    LanguageButton("ಕನ್ನಡ", selectedLanguage == "kn-IN", Modifier.weight(1f)) { selectedLanguage = "kn-IN" }
                    LanguageButton("हिन्दी", selectedLanguage == "hi-IN", Modifier.weight(1f)) { selectedLanguage = "hi-IN" }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = message, onValueChange = { message = it },
                    placeholder = { Text("Ask JARVIS anything...") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Cyan, unfocusedBorderColor = Color.Gray
                    )
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            if (allowed) {
                                startSpeechRecognition(context, selectedLanguage, speechLauncher::launch)
                                status = "LISTENING..."
                            } else microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF087DA8))
                    ) { Text("🎤 SPEAK") }
                    Button(
                        onClick = { sendMessage(message) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF12415A))
                    ) { Text("SEND", letterSpacing = 2.sp) }
                }
                Spacer(Modifier.height(8.dp))
                Text("JARVIS AI • CHAT HISTORY ENABLED", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun LanguageButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 7.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (selected) Cyan else Color.LightGray),
        border = BorderStroke(1.dp, if (selected) Cyan else Color.Gray)
    ) { Text(label, fontSize = 12.sp) }
}

private fun startSpeechRecognition(context: android.content.Context, languageTag: String, launch: (Intent) -> Unit) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS")
    }
    try { launch(intent) } catch (_: Exception) { }
}

private fun localAssistantReply(message: String, languageTag: String): String {
    val q = message.trim().lowercase(Locale.ROOT)
    val greeting = listOf("hello", "hi", "hey", "ನಮಸ್ಕಾರ", "ಹಾಯ್", "नमस्ते", "हेलो")
    val thanks = listOf("thank", "ಧನ್ಯವಾದ", "धन्यवाद")
    val identity = listOf("who are you", "your name", "ನಿನ್ನ ಹೆಸರು", "ನೀನು ಯಾರು", "तुम कौन", "तुम्हारा नाम")
    val help = listOf("help", "ಸಹಾಯ", "मदद")
    val time = listOf("time", "ಸಮಯ", "समय")
    val date = listOf("date", "today", "ದಿನಾಂಕ", "ಇವತ್ತು", "आज", "तारीख")

    return when {
        greeting.any { q.contains(it) } -> when (languageTag) {
            "kn-IN" -> "ನಮಸ್ಕಾರ! ನಾನು JARVIS. ನಿಮಗೆ ಹೇಗೆ ಸಹಾಯ ಮಾಡಲಿ?"
            "hi-IN" -> "नमस्ते! मैं JARVIS हूँ। मैं आपकी क्या मदद कर सकता हूँ?"
            else -> "Hello! I am JARVIS. How can I help you?"
        }
        thanks.any { q.contains(it) } -> when (languageTag) {
            "kn-IN" -> "ಸ್ವಾಗತ! ಇನ್ನೇನಾದರೂ ಸಹಾಯ ಬೇಕೇ?"
            "hi-IN" -> "आपका स्वागत है! क्या मैं और मदद कर सकता हूँ?"
            else -> "You're welcome! Is there anything else I can help with?"
        }
        identity.any { q.contains(it) } -> when (languageTag) {
            "kn-IN" -> "ನಾನು JARVIS, ನಿಮ್ಮ ವೈಯಕ್ತಿಕ ಸಹಾಯಕ. ಈಗ ನಾನು ಮೂಲಭೂತ ಆಫ್‌ಲೈನ್ ಪ್ರಶ್ನೆಗಳಿಗೆ ಉತ್ತರಿಸಬಲ್ಲೆ."
            "hi-IN" -> "मैं JARVIS, आपका निजी सहायक हूँ। अभी मैं कुछ बुनियादी ऑफ़लाइन सवालों के जवाब दे सकता हूँ।"
            else -> "I am JARVIS, your personal assistant. I can currently answer a small set of basic offline questions."
        }
        help.any { q.contains(it) } -> when (languageTag) {
            "kn-IN" -> "ನಮಸ್ಕಾರ, ಸಮಯ, ದಿನಾಂಕ, ನನ್ನ ಹೆಸರು ಅಥವಾ ಧನ್ಯವಾದ ಎಂದು ಕೇಳಿ ಪ್ರಯತ್ನಿಸಿ."
            "hi-IN" -> "नमस्ते, समय, तारीख, मेरा नाम या धन्यवाद पूछकर देखें।"
            else -> "JARVIS tools: open camera; open WhatsApp, YouTube or Chrome; open settings; battery status; calculate 25 * 4; search web for a topic; Wikipedia topic; open website domain; play music; take note text; show notes; remember fact; my memories; add task title; list tasks; complete task: title; delete task: title; set alarm 6:30 PM; tell me a joke; time; date."
        }
        time.any { q.contains(it) } -> {
            val now = java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(java.util.Date())
            when (languageTag) {
                "kn-IN" -> "ಈಗ ಸಮಯ $now."
                "hi-IN" -> "अभी समय $now है।"
                else -> "The current time is $now."
            }
        }
        date.any { q.contains(it) } -> {
            val today = java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(java.util.Date())
            when (languageTag) {
                "kn-IN" -> "ಇಂದಿನ ದಿನಾಂಕ $today."
                "hi-IN" -> "आज की तारीख $today है।"
                else -> "Today's date is $today."
            }
        }
        else -> when (languageTag) {
            "kn-IN" -> "ಕ್ಷಮಿಸಿ, ಈ ಪ್ರಶ್ನೆಗೆ ಇನ್ನೂ ಉತ್ತರಿಸಲು ನನಗೆ AI ಸಂಪರ್ಕವಿಲ್ಲ. 'ಸಹಾಯ' ಎಂದು ಕೇಳಿ ಲಭ್ಯವಿರುವ ಆಫ್‌ಲೈನ್ ವೈಶಿಷ್ಟ್ಯಗಳನ್ನು ನೋಡಿ."
            "hi-IN" -> "माफ़ कीजिए, इस सवाल का जवाब देने के लिए अभी AI कनेक्शन उपलब्ध नहीं है। उपलब्ध ऑफ़लाइन सुविधाओं के लिए 'मदद' पूछें।"
            else -> "I don't have an AI connection for that question yet. Ask 'help' to see what I can do offline."
        }
    }
}


private fun handlePhoneCommand(
    context: android.content.Context,
    message: String,
    languageTag: String
): String? {
    val q = message.trim().lowercase(Locale.ROOT)

    fun reply(en: String, kn: String, hi: String): String = when (languageTag) {
        "kn-IN" -> kn
        "hi-IN" -> hi
        else -> en
    }

    // Personal memory is saved only after an explicit "remember" request.
    val brain = LocalBrainStore(context)
    val rememberPrefixes = listOf("remember that ", "remember ", "ನೆನಪಿಡು ", "याद रखो ")
    val rememberPrefix = rememberPrefixes.firstOrNull { q.startsWith(it) }
    if (rememberPrefix != null) {
        val fact = message.trim().drop(rememberPrefix.length).trim()
        if (fact.isBlank()) return reply("Tell me what to remember.", "ಏನನ್ನು ನೆನಪಿಡಬೇಕು ಎಂದು ಹೇಳಿ.", "क्या याद रखना है, बताइए।")
        return try {
            brain.addMemory(fact)
            reply("Saved to your on-device memory.", "ನಿಮ್ಮ ಫೋನ್‌ನಲ್ಲಿರುವ ವೈಯಕ್ತಿಕ ನೆನಪಿನಲ್ಲಿ ಉಳಿಸಲಾಗಿದೆ.", "आपकी डिवाइस मेमोरी में सहेज लिया।")
        } catch (_: Exception) {
            reply("I couldn't save that memory.", "ಆ ನೆನಪನ್ನು ಉಳಿಸಲಾಗಲಿಲ್ಲ.", "यह जानकारी सहेजी नहीं जा सकी।")
        }
    }

    if (listOf("my memories", "what do you remember", "ನನ್ನ ನೆನಪುಗಳು", "ನಿನಗೆ ಏನು ನೆನಪಿದೆ", "मेरी यादें", "तुम्हें क्या याद है").any { q.contains(it) }) {
        val memories = brain.searchMemories()
        return if (memories.isEmpty()) reply("You have no saved memories yet.", "ಇನ್ನೂ ಯಾವುದೇ ನೆನಪುಗಳನ್ನು ಉಳಿಸಿಲ್ಲ.", "अभी कोई यादें सेव नहीं हैं।")
        else reply("Saved memories:\n" + memories.joinToString("\n• ", "• "),
            "ಉಳಿಸಿದ ನೆನಪುಗಳು:\n" + memories.joinToString("\n• ", "• "),
            "सहेजी गई यादें:\n" + memories.joinToString("\n• ", "• "))
    }

    val forgetPrefix = listOf("forget memory: ", "ನೆನಪು ಅಳಿಸು: ", "याद मिटाओ: ")
        .firstOrNull { q.startsWith(it) }
    if (forgetPrefix != null) {
        val fact = message.trim().drop(forgetPrefix.length).trim()
        return if (fact.isNotBlank() && brain.deleteMemory(fact))
            reply("That exact memory was deleted.", "ಆ ನೆನಪನ್ನು ಅಳಿಸಲಾಗಿದೆ.", "वह याद मिटा दी गई।")
        else reply("I couldn't find that exact memory.", "ಆ ನಿಖರವಾದ ನೆನಪು ಸಿಗಲಿಲ್ಲ.", "वह सटीक याद नहीं मिली।")
    }

    val taskPrefixes = listOf("add task ", "create task ", "remind me to ", "ಕಾರ್ಯ ಸೇರಿಸಿ ", "ಕೆಲಸ ಸೇರಿಸು ", "कार्य जोड़ो ")
    val taskPrefix = taskPrefixes.firstOrNull { q.startsWith(it) }
    if (taskPrefix != null) {
        val title = message.trim().drop(taskPrefix.length).trim()
        if (title.isBlank()) return reply("Tell me the task.", "ಕಾರ್ಯವನ್ನು ಹೇಳಿ.", "कौन सा काम जोड़ना है, बताइए।")
        return try {
            brain.addTask(title)
            reply("Task saved: $title. This is a task entry, not a timed notification yet.",
                "ಕಾರ್ಯ ಉಳಿಸಲಾಗಿದೆ: $title. ಇದು ಕಾರ್ಯಪಟ್ಟಿ ನಮೂದು; ಸಮಯದ ಅಧಿಸೂಚನೆ ಇನ್ನೂ ಸಕ್ರಿಯವಾಗಿಲ್ಲ.",
                "काम सेव हुआ: $title। यह कार्य सूची में है, समयबद्ध सूचना अभी नहीं है।")
        } catch (_: Exception) {
            reply("I couldn't save that task.", "ಕಾರ್ಯವನ್ನು ಉಳಿಸಲಾಗಲಿಲ್ಲ.", "काम सेव नहीं हो सका।")
        }
    }

    if (listOf("list tasks", "my tasks", "pending tasks", "ನನ್ನ ಕಾರ್ಯಗಳು", "ಬಾಕಿ ಕೆಲಸ", "मेरे काम", "बाकी काम").any { q.contains(it) }) {
        val tasks = brain.pendingTasks()
        return if (tasks.isEmpty()) reply("You have no pending tasks.", "ಬಾಕಿ ಕಾರ್ಯಗಳಿಲ್ಲ.", "कोई बाकी काम नहीं है।")
        else reply("Pending tasks:\n" + tasks.joinToString("\n• ", "• "),
            "ಬಾಕಿ ಕಾರ್ಯಗಳು:\n" + tasks.joinToString("\n• ", "• "),
            "बाकी काम:\n" + tasks.joinToString("\n• ", "• "))
    }

    val completePrefix = listOf("complete task: ", "done task: ", "ಕಾರ್ಯ ಪೂರ್ಣ: ", "काम पूरा: ")
        .firstOrNull { q.startsWith(it) }
    if (completePrefix != null) {
        val title = message.trim().drop(completePrefix.length).trim()
        return if (title.isNotBlank() && brain.completeTask(title))
            reply("Task marked complete.", "ಕಾರ್ಯ ಪೂರ್ಣಗೊಂಡಿದೆ ಎಂದು ಗುರುತಿಸಲಾಗಿದೆ.", "काम पूरा चिह्नित किया गया।")
        else reply("I couldn't find that exact pending task.", "ಆ ಬಾಕಿ ಕಾರ್ಯ ಸಿಗಲಿಲ್ಲ.", "वह बाकी काम नहीं मिला।")
    }

    val deleteTaskPrefix = listOf("delete task: ", "ಕಾರ್ಯ ಅಳಿಸು: ", "काम हटाओ: ")
        .firstOrNull { q.startsWith(it) }
    if (deleteTaskPrefix != null) {
        val title = message.trim().drop(deleteTaskPrefix.length).trim()
        return if (title.isNotBlank() && brain.deleteTask(title))
            reply("Task deleted.", "ಕಾರ್ಯ ಅಳಿಸಲಾಗಿದೆ.", "काम हटा दिया गया।")
        else reply("I couldn't find that exact task.", "ಆ ನಿಖರವಾದ ಕಾರ್ಯ ಸಿಗಲಿಲ್ಲ.", "वह सटीक काम नहीं मिला।")
    }

    // Android-native camera tool. The user controls capture; JARVIS never captures silently.
    if (listOf("open camera", "launch camera", "ಕ್ಯಾಮೆರಾ ತೆರೆಯಿರಿ", "कैमरा खोलो").any { q.contains(it) }) {
        val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            if (cameraIntent.resolveActivity(context.packageManager) == null) reply("No camera app is available.", "ಕ್ಯಾಮೆರಾ ಆಪ್ ಲಭ್ಯವಿಲ್ಲ.", "कैमरा ऐप उपलब्ध नहीं है।")
            else { context.startActivity(cameraIntent); reply("Camera opened. You control when to take a photo.", "ಕ್ಯಾಮೆರಾ ತೆರೆಯಲಾಗಿದೆ. ಫೋಟೋ ತೆಗೆದುಕೊಳ್ಳುವುದನ್ನು ನೀವೇ ನಿಯಂತ್ರಿಸುತ್ತೀರಿ.", "कैमरा खुल गया। फोटो लेना आपके नियंत्रण में है।") }
        } catch (_: Exception) { reply("I couldn't open the camera.", "ಕ್ಯಾಮೆರಾ ತೆರೆಯಲು ಆಗಲಿಲ್ಲ.", "कैमरा नहीं खुल सका।") }
    }

    // Deterministic calculator: supports a simple two-number expression only.
    val calcPrefix = listOf("calculate ", "ಗಣನೆ ", "गणना ").firstOrNull { q.startsWith(it) }
    if (calcPrefix != null) {
        val expression = message.trim().drop(calcPrefix.length).replace("×", "*").replace("÷", "/").trim()
        val parts = expression.split("+", "-", "*", "/").map { it.trim() }
        val operator = expression.firstOrNull { it in "+-*/" }
        val a = parts.getOrNull(0)?.toDoubleOrNull(); val b = parts.getOrNull(1)?.toDoubleOrNull()
        val result = if (a == null || b == null || operator == null) null else when (operator) { '+' -> a + b; '-' -> a - b; '*' -> a * b; '/' -> if (b == 0.0) null else a / b; else -> null }
        return if (result == null) reply("Try a simple calculation, e.g. calculate 25 * 4.", "ಸರಳ ಲೆಕ್ಕ ಬಳಸಿ. ಉದಾ: calculate 25 * 4.", "सरल गणना लिखें, जैसे calculate 25 * 4।")
        else reply("The answer is $result.", "ಉತ್ತರ $result.", "उत्तर $result।")
    }
    // Safe, explicit app shortcuts. The app never launches arbitrary AI-supplied packages.
    val appShortcuts = listOf(
        Triple(listOf("open youtube", "ಯೂಟ್ಯೂಬ್ ತೆರೆಯಿರಿ", "यूट्यूब खोलो"), "com.google.android.youtube", "YouTube"),
        Triple(listOf("open whatsapp", "ವಾಟ್ಸಾಪ್ ತೆರೆಯಿರಿ", "व्हाट्सएप खोलो"), "com.whatsapp", "WhatsApp"),
        Triple(listOf("open chrome", "ಕ್ರೋಮ್ ತೆರೆಯಿರಿ", "क्रोम खोलो"), "com.android.chrome", "Chrome")
    )
    val shortcut = appShortcuts.firstOrNull { item -> item.first.any { q.contains(it) } }
    if (shortcut != null) {
        val launch = context.packageManager.getLaunchIntentForPackage(shortcut.second)
            ?: return reply(
                "${shortcut.third} is not installed or has no launchable activity.",
                "${shortcut.third} ಇನ್‌ಸ್ಟಾಲ್ ಆಗಿಲ್ಲ ಅಥವಾ ತೆರೆಯಲು ಸಾಧ್ಯವಿಲ್ಲ.",
                "${shortcut.third} इंस्टॉल नहीं है या खोला नहीं जा सकता।"
            )
        return try {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launch)
            reply("${shortcut.third} opened.", "${shortcut.third} ತೆರೆಯಲಾಗಿದೆ.", "${shortcut.third} खोल दिया गया।")
        } catch (_: Exception) {
            reply("I couldn't open ${shortcut.third}.", "${shortcut.third} ತೆರೆಯಲು ಸಾಧ್ಯವಾಗಲಿಲ್ಲ.", "${shortcut.third} नहीं खुल सका।")
        }
    }

    if (listOf("open settings", "ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯಿರಿ", "सेटिंग्स खोलो").any { q.contains(it) }) {
        return try {
            context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            reply("Android Settings opened.", "Android ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯಲಾಗಿದೆ.", "Android सेटिंग्स खोल दी गई हैं।")
        } catch (_: Exception) {
            reply("I couldn't open Settings.", "ಸೆಟ್ಟಿಂಗ್ಸ್ ತೆರೆಯಲು ಸಾಧ್ಯವಾಗಲಿಲ್ಲ.", "सेटिंग्स नहीं खुल सकीं।")
        }
    }

    if (listOf("battery", "battery status", "ಬ್ಯಾಟರಿ", "बैटरी").any { q.contains(it) }) {
        val batteryManager = context.getSystemService(android.content.Context.BATTERY_SERVICE) as? android.os.BatteryManager
        val level = batteryManager?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        return if (level in 0..100) reply(
            "Your battery is at $level percent.",
            "ನಿಮ್ಮ ಬ್ಯಾಟರಿ $level ಪ್ರತಿಶತ ಇದೆ.",
            "आपकी बैटरी $level प्रतिशत है।"
        ) else reply("Battery level is unavailable.", "ಬ್ಯಾಟರಿ ಮಟ್ಟ ಲಭ್ಯವಿಲ್ಲ.", "बैटरी स्तर उपलब्ध नहीं है।")
    }

    val searchPrefixes = listOf("search web for ", "search for ", "ವೆಬ್‌ನಲ್ಲಿ ಹುಡುಕಿ ", "ಹುಡುಕಿ ", "वेब पर खोजो ")
    val searchPrefix = searchPrefixes.firstOrNull { q.startsWith(it) }
    if (searchPrefix != null) {
        val query = message.trim().drop(searchPrefix.length).trim()
        if (query.isBlank()) return reply(
            "Tell me what to search for.", "ಏನು ಹುಡುಕಬೇಕು ಎಂದು ಹೇಳಿ.", "क्या खोजना है, बताइए।"
        )
        return try {
            val uri = android.net.Uri.parse("https://www.google.com/search").buildUpon()
                .appendQueryParameter("q", query).build()
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            reply("I opened a web search for: $query", "$query ಗಾಗಿ ವೆಬ್ ಹುಡುಕಾಟ ತೆರೆಯಲಾಗಿದೆ.", "$query के लिए वेब खोज खोली गई है।")
        } catch (_: Exception) {
            reply("I couldn't open web search.", "ವೆಬ್ ಹುಡುಕಾಟ ತೆರೆಯಲು ಸಾಧ್ಯವಾಗಲಿಲ್ಲ.", "वेब खोज नहीं खुल सकी।")
        }
    }

    // Desktop-JARVIS-inspired utilities adapted for Android.
    val notePrefixes = listOf("take note ", "note ", "save note ", "ನೋಟ್ ಬರೆಯಿರಿ ", "ಟಿಪ್ಪಣಿ ", "नोट लिखो ", "नोट ")
    val notePrefix = notePrefixes.firstOrNull { q.startsWith(it) }
    if (notePrefix != null) {
        val note = message.trim().drop(notePrefix.length).trim()
        if (note.isBlank()) return reply("Tell me what to write in the note.", "ನೋಟ್‌ನಲ್ಲಿ ಏನು ಬರೆಯಬೇಕು ಎಂದು ಹೇಳಿ.", "नोट में क्या लिखना है, बताइए।")
        val prefs = context.getSharedPreferences("jarvis_notes", 0)
        val notes = org.json.JSONArray(prefs.getString("items", "[]"))
        notes.put(org.json.JSONObject().put("text", note).put("time", System.currentTimeMillis()))
        prefs.edit().putString("items", notes.toString()).apply()
        return reply("Note saved on this phone.", "ನೋಟ್ ಈ ಫೋನ್‌ನಲ್ಲಿ ಉಳಿಸಲಾಗಿದೆ.", "नोट इस फोन में सेव कर दिया गया।")
    }

    if (listOf("show notes", "my notes", "list notes", "ನನ್ನ ನೋಟ್‌ಗಳು", "ನನ್ನ ನೋಟ್ಸ್", "मेरे नोट", "नोट दिखाओ").any { q.contains(it) }) {
        val notes = context.getSharedPreferences("jarvis_notes", 0).getString("items", "[]")
        val array = try { org.json.JSONArray(notes) } catch (_: Exception) { org.json.JSONArray() }
        if (array.length() == 0) return reply("You have no saved notes.", "ಉಳಿಸಿದ ನೋಟ್‌ಗಳಿಲ್ಲ.", "कोई सेव किए हुए नोट नहीं हैं।")
        val lines = (0 until minOf(array.length(), 15)).mapNotNull { i ->
            try { array.getJSONObject(i).optString("text").takeIf { it.isNotBlank() } } catch (_: Exception) { null }
        }
        return reply("Your notes:\\n" + lines.joinToString("\\n• ", "• "),
            "ನಿಮ್ಮ ನೋಟ್‌ಗಳು:\\n" + lines.joinToString("\\n• ", "• "),
            "आपके नोट:\\n" + lines.joinToString("\\n• ", "• "))
    }

    if (listOf("tell me a joke", "joke", "make me laugh", "ಜೋಕ್ ಹೇಳು", "ಒಂದು ಜೋಕ್", "मजाक सुनाओ", "चुटकुला").any { q.contains(it) }) {
        val jokes = when (languageTag) {
            "kn-IN" -> listOf("ಶಿಕ್ಷಕ: ಹೋಂವರ್ಕ್ ಎಲ್ಲಿ? ವಿದ್ಯಾರ್ಥಿ: ಸರ್, ಪುಸ್ತಕದಲ್ಲೇ ಇದೆ!", "ಕಂಪ್ಯೂಟರ್ ಏಕೆ ತಣ್ಣಗಿತ್ತು? ಅದಕ್ಕೆ Windows ತೆರೆದಿತ್ತು!")
            "hi-IN" -> listOf("टीचर: होमवर्क कहाँ है? छात्र: किताब में ही है!", "कंप्यूटर ठंडा क्यों था? क्योंकि उसकी Windows खुली थी!")
            else -> listOf("Why did the computer go to the doctor? It had a virus!", "Why was the maths book sad? It had too many problems!", "I told my phone a joke. It didn't laugh, but it did crack up!")
        }
        return jokes.random()
    }

    val wikiPrefixes = listOf("wikipedia ", "who is ", "what is ", "ವಿಕಿಪೀಡಿಯ ", "ಯಾರು ", "ಏನು ", "विकिपीडिया ", "कौन है ", "क्या है ")
    val wikiPrefix = wikiPrefixes.firstOrNull { q.startsWith(it) }
    if (wikiPrefix != null) {
        val topic = message.trim().drop(wikiPrefix.length).trim()
        if (topic.isBlank()) return reply("Tell me a topic to look up.", "ಹುಡುಕಬೇಕಾದ ವಿಷಯವನ್ನು ಹೇಳಿ.", "खोजने के लिए विषय बताइए।")
        return try {
            val uri = android.net.Uri.parse("https://en.wikipedia.org/wiki/Special:Search").buildUpon()
                .appendQueryParameter("search", topic).build()
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            reply("I opened Wikipedia search for $topic. The article will open in your browser.",
                "$topic ಗಾಗಿ ವಿಕಿಪೀಡಿಯ ಹುಡುಕಾಟ ತೆರೆಯಲಾಗಿದೆ.",
                "$topic के लिए विकिपीडिया खोज खोली गई है।")
        } catch (_: Exception) {
            reply("I couldn't open Wikipedia.", "ವಿಕಿಪೀಡಿಯ ತೆರೆಯಲು ಆಗಲಿಲ್ಲ.", "विकिपीडिया नहीं खुल सका।")
        }
    }

    val websitePrefix = listOf("open website ", "open site ", "ವೆಬ್‌ಸೈಟ್ ತೆರೆಯಿರಿ ", "वेबसाइट खोलो ")
        .firstOrNull { q.startsWith(it) }
    if (websitePrefix != null) {
        val raw = message.trim().drop(websitePrefix.length).trim()
        if (raw.isBlank()) return reply("Tell me the website name or address.", "ವೆಬ್‌ಸೈಟ್ ಹೆಸರು ಅಥವಾ ವಿಳಾಸ ಹೇಳಿ.", "वेबसाइट का नाम या पता बताइए।")
        val host = raw.removePrefix("https://").removePrefix("http://").substringBefore("/").lowercase(Locale.ROOT)
        if (!host.contains(".") || host.any { it.isWhitespace() }) return reply(
            "Please say a valid website, such as wikipedia.org.", "ದಯವಿಟ್ಟು wikipedia.org ಹೀಗೆ ಸರಿಯಾದ ವೆಬ್‌ಸೈಟ್ ಹೇಳಿ.", "कृपया wikipedia.org जैसा सही वेबसाइट पता बताएं।")
        return try {
            val uri = android.net.Uri.parse(if (raw.startsWith("http")) raw else "https://$raw")
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            reply("Opening $host.", "$host ತೆರೆಯಲಾಗುತ್ತಿದೆ.", "$host खोला जा रहा है।")
        } catch (_: Exception) {
            reply("I couldn't open that website.", "ಆ ವೆಬ್‌ಸೈಟ್ ತೆರೆಯಲು ಆಗಲಿಲ್ಲ.", "वह वेबसाइट नहीं खुल सकी।")
        }
    }

    if (listOf("play music", "open music", "ಸಂಗೀತ ಪ್ಲೇ ಮಾಡಿ", "ಹಾಡು ಪ್ಲೇ ಮಾಡಿ", "गाना चलाओ", "म्यूजिक चलाओ").any { q.contains(it) }) {
        return try {
            val musicIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(musicIntent)
            reply("Opening your music app. Choose a track to play.", "ನಿಮ್ಮ ಮ್ಯೂಸಿಕ್ ಆಪ್ ತೆರೆಯಲಾಗಿದೆ. ಹಾಡನ್ನು ಆಯ್ಕೆಮಾಡಿ.", "आपका म्यूजिक ऐप खुल गया है। गाना चुनें।")
        } catch (_: Exception) {
            reply("I couldn't find a music app on this phone.", "ಈ ಫೋನ್‌ನಲ್ಲಿ ಮ್ಯೂಸಿಕ್ ಆಪ್ ಸಿಗಲಿಲ್ಲ.", "इस फोन पर म्यूजिक ऐप नहीं मिला।")
        }
    }

    val isAlarmRequest = listOf("set alarm", "alarm", "o'clock", "oclock", "ಅಲಾರಂ", "ಎಚ್ಚರಿಕೆ", "ಗಂಟೆ", "अलार्म", "बजे")
        .any { q.contains(it) }
    if (!isAlarmRequest) return null

    val timeMatch = Regex("""(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""", RegexOption.IGNORE_CASE)
        .find(q) ?: return when (languageTag) {
            "kn-IN" -> "ದಯವಿಟ್ಟು ಸಮಯವನ್ನು ಹೇಳಿ. ಉದಾಹರಣೆ: ಅಲಾರಂ 6:30 AM."
            "hi-IN" -> "कृपया समय बताएं। उदाहरण: अलार्म 6:30 AM।"
            else -> "Please include a time, for example: set alarm 6:30 AM."
        }

    val hourInput = timeMatch.groupValues[1].toIntOrNull() ?: return null
    val minute = timeMatch.groupValues[2].ifBlank { "0" }.toIntOrNull() ?: 0
    val meridiem = timeMatch.groupValues[3].lowercase(Locale.ROOT)

    if (hourInput !in 1..12 || minute !in 0..59) {
        return when (languageTag) {
            "kn-IN" -> "ಸಮಯ ಸರಿಯಿಲ್ಲ. ದಯವಿಟ್ಟು ಸರಿಯಾದ ಗಂಟೆ ಮತ್ತು ನಿಮಿಷ ಬಳಸಿ."
            "hi-IN" -> "समय सही नहीं है। कृपया सही घंटा और मिनट दर्ज करें।"
            else -> "That time doesn't look valid. Please use an hour from 1–12 and minutes from 00–59."
        }
    }

    if (meridiem.isBlank()) {
        return when (languageTag) {
            "kn-IN" -> "$hourInput ಗಂಟೆ ಎಂದರೆ AM ಅಥವಾ PM? ದಯವಿಟ್ಟು ಸ್ಪಷ್ಟಪಡಿಸಿ."
            "hi-IN" -> "$hourInput बजे का मतलब AM या PM? कृपया बताएं।"
            else -> "Do you mean $hourInput AM or PM? Please specify."
        }
    }

    val hour24 = when {
        meridiem == "am" && hourInput == 12 -> 0
        meridiem == "pm" && hourInput != 12 -> hourInput + 12
        else -> hourInput
    }
    val alarmIntent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
        putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour24)
        putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
        putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, "JARVIS alarm")
        putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, false)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
        context.startActivity(alarmIntent)
        when (languageTag) {
            "kn-IN" -> "ಅಲಾರಂ ವಿವರಗಳೊಂದಿಗೆ ಗಡಿಯಾರ ಅಪ್ಲಿಕೇಶನ್ ತೆರೆಯಲಾಗಿದೆ. ದಯವಿಟ್ಟು ಪರಿಶೀಲಿಸಿ ದೃಢೀಕರಿಸಿ."
            "hi-IN" -> "अलार्म विवरण के साथ घड़ी ऐप खुल गया है। कृपया जाँचकर पुष्टि करें।"
            else -> "Clock opened with the alarm details. Please review and confirm the alarm."
        }
    } catch (_: android.content.ActivityNotFoundException) {
        try {
            context.startActivity(
                Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            when (languageTag) {
                "kn-IN" -> "ಗಡಿಯಾರ ಅಪ್ಲಿಕೇಶನ್ ತೆರೆಯಲಾಗಿದೆ. ಅಲಾರಂ ಅನ್ನು ಕೈಯಾರೆ ಸೇರಿಸಿ."
                "hi-IN" -> "घड़ी ऐप खुल गया है। कृपया अलार्म हाथ से जोड़ें।"
                else -> "I opened the alarms screen. Please add the alarm there manually."
            }
        } catch (_: Exception) {
            when (languageTag) {
                "kn-IN" -> "ಈ ಫೋನ್‌ನಲ್ಲಿ ಹೊಂದಾಣಿಕೆಯ ಗಡಿಯಾರ ಅಪ್ಲಿಕೇಶನ್ ಸಿಗಲಿಲ್ಲ."
                "hi-IN" -> "इस फ़ोन पर संगत घड़ी ऐप नहीं मिला।"
                else -> "No compatible Clock app was found on this phone."
            }
        }
    } catch (_: SecurityException) {
        when (languageTag) {
            "kn-IN" -> "ಅಲಾರಂ ಅನುಮತಿ ನಿರಾಕರಿಸಲಾಗಿದೆ. Android Settings ನಲ್ಲಿ ಅನುಮತಿಯನ್ನು ಪರಿಶೀಲಿಸಿ."
            "hi-IN" -> "अलार्म अनुमति नहीं मिली। Android Settings में अनुमति जाँचें।"
            else -> "Android denied the alarm request. Please check the alarm permission in Settings."
        }
    }
}
