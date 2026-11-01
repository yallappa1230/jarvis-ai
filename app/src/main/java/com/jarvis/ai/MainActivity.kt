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
import java.util.Locale

private val Background = Color(0xFF050B16)
private val Cyan = Color(0xFF45DFFF)

class MainActivity : ComponentActivity() {
    private var speechEngine: TextToSpeech? = null
    private var speechReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speechEngine = TextToSpeech(this) { result ->
            speechReady = result == TextToSpeech.SUCCESS
            if (speechReady) {
                speechEngine?.setSpeechRate(0.95f)
            }
        }

        setContent {
            JarvisHome(
                onSpeak = { text, languageTag ->
                    if (speechReady) {
                        val locale = Locale.forLanguageTag(languageTag)
                        val languageResult = speechEngine?.setLanguage(locale)
                        if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
                        ) {
                            speechEngine?.setLanguage(Locale.US)
                        }
                        speechEngine?.speak(
                            text,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "jarvis_reply"
                        )
                    }
                }
            )
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
    var response by remember {
        mutableStateOf("Hello! I am JARVIS. Your personal AI assistant.")
    }
    var selectedLanguage by remember { mutableStateOf("en-IN") }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenWords = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenWords.isNullOrBlank()) {
                message = spokenWords
                response = "I heard: $spokenWords"
                status = "VOICE RECEIVED"
                onSpeak("I heard you say: $spokenWords", selectedLanguage)
            } else {
                status = "NO SPEECH DETECTED"
            }
        } else {
            status = "LISTENING CANCELLED"
        }
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startSpeechRecognition(context, selectedLanguage, speechLauncher::launch)
            status = "LISTENING..."
        } else {
            status = "MICROPHONE PERMISSION DENIED"
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(25.dp))
                    Text(
                        "J.A.R.V.I.S",
                        color = Cyan,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "PERSONAL AI ASSISTANT",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        letterSpacing = 3.sp
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(205.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF176D91), Color(0xFF102B49), Background)
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(145.dp),
                            shape = CircleShape,
                            color = Color(0xFF0A1B2E),
                            border = BorderStroke(2.dp, Cyan)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "J",
                                    color = Cyan,
                                    fontSize = 72.sp,
                                    fontWeight = FontWeight.Light
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(25.dp))
                    Text(status, color = Cyan, fontSize = 14.sp, letterSpacing = 2.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(response, color = Color.White, fontSize = 17.sp)
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("VOICE LANGUAGE", color = Color.LightGray, fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LanguageButton("English", selectedLanguage == "en-IN", Modifier.weight(1f)) {
                            selectedLanguage = "en-IN"
                        }
                        LanguageButton("ಕನ್ನಡ", selectedLanguage == "kn-IN", Modifier.weight(1f)) {
                            selectedLanguage = "kn-IN"
                        }
                        LanguageButton("हिन्दी", selectedLanguage == "hi-IN", Modifier.weight(1f)) {
                            selectedLanguage = "hi-IN"
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        placeholder = { Text("Ask JARVIS anything...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Cyan,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    startSpeechRecognition(
                                        context,
                                        selectedLanguage,
                                        speechLauncher::launch
                                    )
                                    status = "LISTENING..."
                                } else {
                                    microphonePermissionLauncher.launch(
                                        Manifest.permission.RECORD_AUDIO
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).height(55.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF087DA8)
                            )
                        ) {
                            Text("🎤 SPEAK", letterSpacing = 1.sp)
                        }

                        Button(
                            onClick = {
                                if (message.isNotBlank()) {
                                    response = localAssistantReply(message, selectedLanguage)
                                    status = "JARVIS READY"
                                    onSpeak(response, selectedLanguage)
                                } else {
                                    status = "TYPE OR SPEAK A MESSAGE"
                                }
                            },
                            modifier = Modifier.weight(1f).height(55.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF12415A)
                            )
                        ) {
                            Text("SEND", letterSpacing = 2.sp)
                        }
                    }
                    Spacer(Modifier.height(15.dp))
                    Text(
                        "JARVIS AI • VOICE TEST BUILD 1",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (selected) Cyan else Color.LightGray
        ),
        border = BorderStroke(1.dp, if (selected) Cyan else Color.Gray)
    ) {
        Text(label, fontSize = 12.sp)
    }
}

private fun startSpeechRecognition(
    context: android.content.Context,
    languageTag: String,
    launch: (Intent) -> Unit
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS")
    }

    try {
        launch(intent)
    } catch (_: Exception) {
        // A compatible speech recognition service may not be installed.
    }
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
