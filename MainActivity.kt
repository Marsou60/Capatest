\
package com.example.capatest

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scroll = rememberScrollState()

    var lastSpeech by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var joke by remember { mutableStateOf("(Vide)") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var note by remember { mutableStateOf("") }

    val requestPermissions = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    val client = remember {
        HttpClient(Android) {
            install(ContentNegotiation) { json() }
        }
    }

    val speechRecognizer = remember {
        SpeechRecognizer.createSpeechRecognizer(ctx)
    }

    DisposableEffect(Unit) {
        onDispose { speechRecognizer.destroy() }
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: Bundle) {
                val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (text != null) lastSpeech = text
            }
            override fun onPartialResults(partialResults: Bundle) {
                val text = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (text != null) lastSpeech = text
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer.startListening(intent)
    }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (!success) photoUri = null
    }

    fun newImageUri(): Uri {
        val imagesDir = File(ctx.cacheDir, "images").apply { mkdirs() }
        val image = File.createTempFile("shot_", ".jpg", imagesDir)
        return FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", image)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Test des capacités", fontWeight = FontWeight.Bold) }) }
    ) { paddings ->
        Column(
            modifier = Modifier
                .padding(paddings)
                .padding(16.dp)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("1) Reconnaissance vocale", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    requestPermissions.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                    startListening()
                }) {
                    Text(if (isListening) "Écoute…" else "Parler")
                }
                Text(if (lastSpeech.isBlank()) "(dites quelque chose)" else lastSpeech)
            }

            Divider()

            Text("2) Photo rapide", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val uri = newImageUri()
                    photoUri = uri
                    takePictureLauncher.launch(uri)
                }) { Text("Prendre une photo") }
            }
            photoUri?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(model = uri),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            }

            Divider()

            Text("3) Requête HTTP (Chuck Norris API)", style = MaterialTheme.typography.titleMedium)
            Button(onClick = {
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    joke = try {
                        val data: Joke = client.get("https://api.chucknorris.io/jokes/random").body()
                        data.value
                    } catch (e: Exception) {
                        "Erreur: ${e.message}"
                    }
                }
            }) { Text("Charger une blague") }
            Text(joke)

            Divider()

            Text("4) Stockage local (SharedPreferences)", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val prefs = ctx.getSharedPreferences("demo", 0)
                    prefs.edit().putString("note", note).apply()
                }) { Text("Enregistrer") }
                Button(onClick = {
                    val prefs = ctx.getSharedPreferences("demo", 0)
                    note = prefs.getString("note", "") ?: ""
                }) { Text("Lire") }
            }
        }
    }
}

@Serializable
data class Joke(@SerialName("value") val value: String)
