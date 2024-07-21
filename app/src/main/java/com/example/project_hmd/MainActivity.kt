package com.example.project_hmd

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.project_hmd.ui.theme.Project_HMDTheme
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var speechRecognizerLauncher: ActivityResultLauncher<Intent>
    private var isTraining = false
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences("VoicePatternPrefs", Context.MODE_PRIVATE)

        speechRecognizerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val matches = result.data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val recognizedText = matches?.get(0) ?: ""
                if (isTraining) {
                    saveVoicePattern(recognizedText)
                    isTraining = false
                    recognizedTextState.value = "Voice pattern saved"
                    Toast.makeText(this, "Voice pattern saved", Toast.LENGTH_SHORT).show()
                } else {
                    recognizedTextState.value = recognizedText
                    if (isVoicePatternMatch(recognizedText)) {
                        unlockDevice()
                    } else {
                        Toast.makeText(this, "Voice command not recognized", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        setContent {
            Project_HMDTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    UnlockScreen(
                        modifier = Modifier.padding(innerPadding),
                        onSpeakClick = { startSpeechRecognition() },
                        onPatternSubmit = { pattern -> checkPattern(pattern) },
                        onTrainVoice = { startVoiceTraining() },
                        recognizedText = recognizedTextState.value,
                        isTraining = isTraining
                    )
                }
            }
        }
    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Your device doesn't support speech recognition", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startVoiceTraining() {
        isTraining = true
        recognizedTextState.value = "Recording voice..."
        startSpeechRecognition()
    }

    private fun checkPattern(pattern: String) {
        val correctPattern = "1234" // This should be securely stored and retrieved
        if (pattern == correctPattern) {
            unlockDevice()
        } else {
            Toast.makeText(this, "Incorrect pattern", Toast.LENGTH_SHORT).show()
        }
    }

    private fun unlockDevice() {
        Toast.makeText(this, "Device Unlocked", Toast.LENGTH_SHORT).show()
        navigateToHome()
    }

    private fun navigateToHome() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        startActivity(intent)
        finish() // Close the current lock screen activity
    }

    private fun saveVoicePattern(pattern: String) {
        with(sharedPreferences.edit()) {
            putString("VOICE_PATTERN", pattern)
            apply()
        }
    }

    private fun isVoicePatternMatch(pattern: String): Boolean {
        val savedPattern = sharedPreferences.getString("VOICE_PATTERN", null)
        return savedPattern != null && savedPattern == pattern
    }

    companion object {
        var recognizedTextState: MutableState<String> = mutableStateOf("")
    }
}

@Composable
fun UnlockScreen(
    modifier: Modifier = Modifier,
    onSpeakClick: () -> Unit,
    onPatternSubmit: (String) -> Unit,
    onTrainVoice: () -> Unit,
    recognizedText: String,
    isTraining: Boolean
) {
    var pattern by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "PhD Speech Unlock App", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Enter Pattern:")
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = pattern,
            onValueChange = { pattern = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onPatternSubmit(pattern) }) {
            Text(text = "Unlock with Pattern")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = if (isTraining) "Recording voice..." else "Recognized Text: $recognizedText")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSpeakClick) {
            Text(text = "Unlock with Voice")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onTrainVoice) {
            Text(text = "Train Voice")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UnlockScreenPreview() {
    Project_HMDTheme {
        UnlockScreen(
            onSpeakClick = {},
            onPatternSubmit = {},
            onTrainVoice = {},
            recognizedText = "Hello Android!",
            isTraining = false
        )
    }
}
