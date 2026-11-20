package com.lukuman.freechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.lukuman.freechat.ui.i18n.BilingualStrings
import com.lukuman.freechat.ui.screens.ChatDetailScreen
import com.lukuman.freechat.ui.screens.ChatListScreen
import com.lukuman.freechat.ui.screens.FindUsersScreen
import com.lukuman.freechat.ui.screens.LoginScreen
import com.lukuman.freechat.ui.screens.ProfileScreen
import com.lukuman.freechat.ui.theme.DeepBluePalette
import com.lukuman.freechat.ui.theme.LukumanFreeChatTheme
import com.lukuman.freechat.ui.viewmodel.ChatViewModel

private enum class AppRoute {
    CHATS,
    FIND_USERS,
    CHAT_DETAIL,
    PROFILE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LukumanFreeChatTheme {
                LukumanFreeChatRoot()
            }
        }
    }
}

@Composable
fun LukumanFreeChatRoot(viewModel: ChatViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val strings = BilingualStrings.forLanguage(state.language)
    val context = LocalContext.current
    var currentRoute by rememberSaveable { mutableStateOf(AppRoute.CHATS) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (!idToken.isNullOrBlank()) {
                viewModel.handleGoogleIdToken(idToken)
            } else {
                viewModel.reportError(strings.missingGoogleServicesHint)
            }
        } catch (e: ApiException) {
            viewModel.reportError(e.localizedMessage ?: strings.missingGoogleServicesHint)
        }
    }

    if (state.isInitializing) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepBluePalette.MidnightNavy),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = DeepBluePalette.SapphireSky)
        }
        return
    }

    val currentUser = state.currentUser
    if (currentUser == null) {
        LoginScreen(
            strings = strings,
            currentLanguage = state.language,
            isSigningIn = state.isSigningIn,
            errorMessage = state.errorMessage,
            onSelectLanguage = viewModel::setLanguage,
            onGoogleSignInClick = {
                viewModel.clearError()
                val webClientIdRes = context.resources.getIdentifier(
                    "default_web_client_id",
                    "string",
                    context.packageName
                )
                if (webClientIdRes == 0) {
                    viewModel.reportError(strings.missingGoogleServicesHint)
                    return@LoginScreen
                }
                val webClientId = context.getString(webClientIdRes)
                if (webClientId.isBlank() || webClientId.contains("ciplaceholder")) {
                    viewModel.reportError(strings.missingGoogleServicesHint)
                    return@LoginScreen
                }
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(context, gso)
                googleSignInLauncher.launch(client.signInIntent)
            }
        )
        return
    }

    when (currentRoute) {
        AppRoute.CHATS -> {
            ChatListScreen(
                strings = strings,
                currentUser = currentUser,
                chats = state.chats,
                onOpenChat = { chatId ->
                    viewModel.openChat(chatId)
                    currentRoute = AppRoute.CHAT_DETAIL
                },
                onOpenFindUsers = { currentRoute = AppRoute.FIND_USERS },
                onOpenProfile = { currentRoute = AppRoute.PROFILE }
            )
        }

        AppRoute.FIND_USERS -> {
            FindUsersScreen(
                strings = strings,
                query = state.searchQuery,
                results = state.searchResults,
                isSearching = state.isSearching,
                hasSearched = state.hasSearched,
                onQueryChange = viewModel::updateSearchQuery,
                onSearch = viewModel::runUserSearch,
                onSelectUser = { user ->
                    viewModel.startChatWithUser(user) {
                        currentRoute = AppRoute.CHAT_DETAIL
                    }
                },
                onBack = { currentRoute = AppRoute.CHATS }
            )
        }

        AppRoute.CHAT_DETAIL -> {
            val activeThread = state.chats.firstOrNull { it.chatId == state.activeChatId }
            ChatDetailScreen(
                strings = strings,
                currentUid = currentUser.uid,
                peerName = activeThread?.peerName(currentUser.uid)?.ifBlank { state.activePeerName }
                    ?: state.activePeerName.ifBlank { strings.appName },
                peerPhoto = activeThread?.peerPhoto(currentUser.uid)?.ifBlank { state.activePeerPhoto }
                    ?: state.activePeerPhoto,
                isPeerTyping = activeThread?.isPeerTyping(currentUser.uid) == true,
                messages = state.activeMessages,
                isCompressingImage = state.isCompressingImage,
                stagedImageBase64 = state.stagedImageBase64,
                errorMessage = state.errorMessage,
                onDraftChange = viewModel::onDraftChanged,
                onPickImageUri = { uri ->
                    viewModel.pickAndCompressImage(context, uri, strings.imageErrorLabel)
                },
                onClearStagedImage = viewModel::clearStagedImage,
                onSendMessage = viewModel::sendMessage,
                onBack = {
                    viewModel.closeChat()
                    currentRoute = AppRoute.CHATS
                }
            )
        }

        AppRoute.PROFILE -> {
            ProfileScreen(
                strings = strings,
                user = currentUser,
                currentLanguage = state.language,
                isSavingPhone = state.isSavingPhone,
                phoneStatusMessage = state.phoneStatusMessage,
                phoneStatusIsError = state.phoneStatusIsError,
                onSelectLanguage = viewModel::setLanguage,
                onSavePhone = { rawPhone -> viewModel.savePhoneNumber(rawPhone, strings) },
                onRemovePhone = { viewModel.savePhoneNumber("", strings) },
                onClearPhoneFeedback = viewModel::clearPhoneFeedback,
                onSignOut = {
                    currentRoute = AppRoute.CHATS
                    viewModel.signOut(context)
                },
                onBack = {
                    viewModel.clearPhoneFeedback()
                    currentRoute = AppRoute.CHATS
                }
            )
        }
    }
}
