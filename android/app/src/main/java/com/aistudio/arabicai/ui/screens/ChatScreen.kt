package com.aistudio.arabicai.ui.screens

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.arabicai.data.model.AttachedImage
import com.aistudio.arabicai.data.model.Message
import com.aistudio.arabicai.data.model.Persona
import com.aistudio.arabicai.ui.components.ChatMessageItem
import com.aistudio.arabicai.ui.components.VoiceSpeechRecognizer
import com.aistudio.arabicai.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<Message>,
    selectedPersona: Persona,
    isStreaming: Boolean,
    onSendMessage: (String, List<AttachedImage>) -> Unit,
    onStopStreaming: () -> Unit,
    onRetryMessage: () -> Unit,
    tts: TextToSpeech?
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    val attachedImages = remember { mutableStateListOf<AttachedImage>() }
    var isListening by remember { mutableStateOf(false) }

    // =========================
    // Speech Recognizer
    // =========================
    val speechRecognizer = remember {
        VoiceSpeechRecognizer(
            context = context,
            onResult = { recognizedText ->
                inputText =
                    if (inputText.isNotBlank()) {
                        "$inputText $recognizedText"
                    } else {
                        recognizedText
                    }
            },
            onError = {
                isListening = false
            },
            onStateChange = { listening ->
                isListening = listening
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
        }
    }

    // =========================
    // Image Picker
    // =========================
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->

        uris.forEachIndexed { index, uri ->
            try {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(
                        context.contentResolver,
                        uri
                    )
                } else {
                    val source = ImageDecoder.createSource(
                        context.contentResolver,
                        uri
                    )

                    ImageDecoder.decodeBitmap(source)
                }

                attachedImages.add(
                    AttachedImage(
                        id = "img_${System.currentTimeMillis()}_$index",
                        bitmap = bitmap
                    )
                )

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // =========================
    // Auto Scroll
    // =========================
    LaunchedEffect(
        messages.size,
        messages.lastOrNull()?.content
    ) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(
                messages.size - 1
            )
        }
    }

    // =========================
    // Main Layout
    // =========================
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
    ) {

        // =========================
        // Messages Area
        // =========================
        Box(
            modifier = Modifier.weight(1f)
        ) {

            if (messages.isEmpty()) {

                // =========================
                // Welcome Screen
                // =========================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    item {

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(
                                    RoundedCornerShape(20.dp)
                                )
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Indigo600,
                                            Violet600
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Slate100,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        Text(
                            text = selectedPersona.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Slate100
                        )

                        Text(
                            text = selectedPersona.description,
                            fontSize = 13.sp,
                            color = Slate400,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(
                                horizontal = 24.dp,
                                vertical = 8.dp
                            )
                        )

                        Spacer(
                            modifier = Modifier.height(20.dp)
                        )

                        Text(
                            text = "اقتراحات لبدء المحادثة:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate300,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 8.dp,
                                    vertical = 6.dp
                                )
                        )
                    }

                    items(
                        selectedPersona.suggestedPrompts
                    ) { prompt ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    onSendMessage(
                                        prompt,
                                        emptyList()
                                    )
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = Slate900
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Slate800
                            )
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween,
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Text(
                                    text = prompt,
                                    fontSize = 13.sp,
                                    color = Slate100,
                                    modifier = Modifier.weight(1f)
                                )

                                Icon(
                                    imageVector =
                                        Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = Indigo400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

            } else {

                // =========================
                // Messages List
                // =========================
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {

                    items(messages) { msg ->

                        ChatMessageItem(
                            message = msg,
                            personaName = selectedPersona.name,
                            tts = tts,
                            onRetry = onRetryMessage
                        )

                        HorizontalDivider(
                            color = Slate800.copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }

        // =========================
        // Voice Listening Banner
        // =========================
        if (isListening) {

            Surface(
                color = Rose950,
                modifier = Modifier.fillMaxWidth()
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 6.dp
                        ),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            "جاري الاستماع لصوتك باللغة العربية... تحدث الآن",
                        fontSize = 12.sp,
                        color = Slate100
                    )

                    TextButton(
                        onClick = {
                            speechRecognizer.stopListening()
                        }
                    ) {

                        Text(
                            text = "إيقاف",
                            color = Rose500,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // =========================
        // Image Preview
        // =========================
        if (attachedImages.isNotEmpty()) {

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate900)
                    .padding(8.dp),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                items(attachedImages) { img ->

                    Box(
                        modifier = Modifier.size(60.dp)
                    ) {

                        img.bitmap?.let { bitmap ->

                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentScale =
                                    ContentScale.Crop
                            )
                        }

                        IconButton(
                            onClick = {
