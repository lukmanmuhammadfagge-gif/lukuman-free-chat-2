package com.lukuman.freechat.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lukuman.freechat.data.model.ChatMessage
import com.lukuman.freechat.data.model.MessageStatus
import com.lukuman.freechat.ui.components.UserAvatar
import com.lukuman.freechat.ui.i18n.AppStrings
import com.lukuman.freechat.ui.theme.DeepBluePalette
import com.lukuman.freechat.util.ImageCompressor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    strings: AppStrings,
    currentUid: String,
    peerName: String,
    peerPhoto: String,
    isPeerTyping: Boolean,
    messages: List<ChatMessage>,
    isCompressingImage: Boolean,
    stagedImageBase64: String?,
    errorMessage: String?,
    onDraftChange: (String) -> Unit,
    onPickImageUri: (Uri) -> Unit,
    onClearStagedImage: () -> Unit,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    var draft by rememberSaveable { mutableStateOf("") }
    var fullScreenImageBase64 by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickImageUri(uri)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val canSend = (draft.trim().isNotEmpty() || !stagedImageBase64.isNullOrBlank()) && !isCompressingImage

    Scaffold(
        containerColor = DeepBluePalette.MidnightNavy,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepBluePalette.DeepOcean,
                    titleContentColor = DeepBluePalette.IceInk
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                            tint = DeepBluePalette.IceInk
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(
                            name = peerName,
                            photoUrl = peerPhoto,
                            size = 38.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = peerName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isPeerTyping) {
                                Text(
                                    text = strings.typingIndicatorLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DeepBluePalette.SapphireSky,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepBluePalette.DeepOcean)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = DeepBluePalette.ErrorRed,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                if (isCompressingImage || !stagedImageBase64.isNullOrBlank()) {
                    StagedImagePreviewBar(
                        strings = strings,
                        isCompressing = isCompressingImage,
                        imageBase64 = stagedImageBase64,
                        onClear = onClearStagedImage
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            if (!isCompressingImage) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = DeepBluePalette.AbyssSurface,
                        border = BorderStroke(1.dp, DeepBluePalette.HairlineBorder),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Image,
                                contentDescription = strings.attachImageLabel,
                                tint = DeepBluePalette.SapphireSky
                            )
                        }
                    }

                    OutlinedTextField(
                        value = draft,
                        onValueChange = {
                            if (it.length <= 2000) {
                                draft = it
                                onDraftChange(it)
                            }
                        },
                        placeholder = {
                            Text(
                                text = strings.messageHint,
                                color = DeepBluePalette.SteelMuted
                            )
                        },
                        maxLines = 4,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepBluePalette.SapphireSky,
                            unfocusedBorderColor = DeepBluePalette.HairlineBorder,
                            focusedTextColor = DeepBluePalette.IceInk,
                            unfocusedTextColor = DeepBluePalette.IceInk,
                            focusedContainerColor = DeepBluePalette.AbyssSurface,
                            unfocusedContainerColor = DeepBluePalette.AbyssSurface
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        onClick = {
                            if (canSend) {
                                val clean = draft.trim()
                                onSendMessage(clean)
                                draft = ""
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (canSend) DeepBluePalette.RoyalCobalt else DeepBluePalette.ElevatedSlateBlue,
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = strings.sendLabel,
                                tint = DeepBluePalette.IceInk
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.emptyMessages,
                    style = MaterialTheme.typography.bodyLarge,
                    color = DeepBluePalette.SteelMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    val isMine = message.senderId == currentUid
                    MessageBubble(
                        message = message,
                        isMine = isMine,
                        photoContentDescription = strings.photoLabel,
                        onImageClick = { base64 ->
                            fullScreenImageBase64 = base64
                        }
                    )
                }
            }
        }
    }

    fullScreenImageBase64?.let { base64 ->
        FullScreenImageDialog(
            title = strings.photoViewerTitle,
            closeLabel = strings.closeLabel,
            imageBase64 = base64,
            onDismiss = { fullScreenImageBase64 = null }
        )
    }
}

@Composable
private fun StagedImagePreviewBar(
    strings: AppStrings,
    isCompressing: Boolean,
    imageBase64: String?,
    onClear: () -> Unit
) {
    val previewBitmap = remember(imageBase64) {
        imageBase64?.let { ImageCompressor.decodeBase64Bitmap(it)?.asImageBitmap() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepBluePalette.AbyssSurface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isCompressing) {
            CircularProgressIndicator(
                color = DeepBluePalette.SapphireSky,
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = strings.compressingImageLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = DeepBluePalette.SteelMuted,
                modifier = Modifier.weight(1f)
            )
        } else if (previewBitmap != null) {
            Image(
                bitmap = previewBitmap,
                contentDescription = strings.photoPreviewLabel,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Text(
                text = strings.photoPreviewLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = DeepBluePalette.IceInk,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = strings.removeImageLabel,
                    tint = DeepBluePalette.SteelMuted
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    photoContentDescription: String,
    onImageClick: (String) -> Unit
) {
    val imageBitmap = remember(message.id, message.imageBase64) {
        if (message.imageBase64.isNotBlank()) {
            ImageCompressor.decodeBase64Bitmap(message.imageBase64)?.asImageBitmap()
        } else {
            null
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isMine) DeepBluePalette.OutgoingBubble else DeepBluePalette.IncomingBubble,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMine) 18.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 18.dp
            ),
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = photoContentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 260.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(message.imageBase64) }
                    )
                    if (message.text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = DeepBluePalette.IceInk,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                if (message.createdAtMillis > 0L || isMine) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (message.createdAtMillis > 0L) {
                            Text(
                                text = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    .format(Date(message.createdAtMillis)),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isMine) DeepBluePalette.SapphireSky else DeepBluePalette.SteelMuted
                            )
                        }
                        if (isMine) {
                            val tickIcon = when (message.status) {
                                MessageStatus.SENT -> Icons.Rounded.Done
                                MessageStatus.DELIVERED, MessageStatus.READ -> Icons.Rounded.DoneAll
                            }
                            val tickColor = when (message.status) {
                                MessageStatus.SENT, MessageStatus.DELIVERED -> DeepBluePalette.SteelMuted
                                MessageStatus.READ -> DeepBluePalette.SapphireSky
                            }
                            Icon(
                                imageVector = tickIcon,
                                contentDescription = message.status.value,
                                tint = tickColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenImageDialog(
    title: String,
    closeLabel: String,
    imageBase64: String,
    onDismiss: () -> Unit
) {
    val fullBitmap = remember(imageBase64) {
        ImageCompressor.decodeBase64Bitmap(imageBase64)?.asImageBitmap()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF5050B18))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            if (fullBitmap != null) {
                Image(
                    bitmap = fullBitmap,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .align(Alignment.Center)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepBluePalette.IceInk
                )
                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = DeepBluePalette.AbyssSurface.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, DeepBluePalette.HairlineBorder)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = closeLabel,
                        tint = DeepBluePalette.IceInk,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}
