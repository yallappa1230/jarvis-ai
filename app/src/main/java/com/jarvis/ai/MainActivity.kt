package com.jarvis.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF050B16)
private val Cyan = Color(0xFF45DFFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JarvisHome() }
    }
}

@Composable
fun JarvisHome() {
    var message by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("SYSTEM ONLINE") }
    var response by remember {
        mutableStateOf("Hello! I am JARVIS. Your personal AI assistant.")
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
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (message.isNotBlank()) {
                                response = "Message received: $message"
                                status = "MESSAGE RECEIVED"
                            } else {
                                status = "VOICE ENGINE COMING NEXT"
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF087DA8)
                        )
                    ) {
                        Text("ACTIVATE JARVIS", letterSpacing = 2.sp)
                    }
                    Spacer(Modifier.height(15.dp))
                    Text(
                        "JARVIS AI • VERSION 1.0",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}
