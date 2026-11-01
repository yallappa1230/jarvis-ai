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
        val answer = localAssistantReply(text, selectedLanguage)
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
            else -> "Try asking: hello, what is your name, what time is it, today's date, or thank you."
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
