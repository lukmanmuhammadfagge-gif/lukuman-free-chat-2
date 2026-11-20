package com.lukuman.freechat.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lukuman.freechat.data.model.AppLanguage
import com.lukuman.freechat.data.model.ChatMessage
import com.lukuman.freechat.data.model.ChatThread
import com.lukuman.freechat.data.model.DirectoryUser
import com.lukuman.freechat.data.model.UserProfile
import com.lukuman.freechat.data.repository.AuthRepository
import com.lukuman.freechat.data.repository.ChatRepository
import com.lukuman.freechat.ui.i18n.AppStrings
import com.lukuman.freechat.util.ImageCompressor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val isInitializing: Boolean = true,
    val isSigningIn: Boolean = false,
    val language: AppLanguage = AppLanguage.HAUSA,
    val currentUser: UserProfile? = null,
    val chats: List<ChatThread> = emptyList(),
    val activeChatId: String? = null,
    val activePeerName: String = "",
    val activePeerPhoto: String = "",
    val activeMessages: List<ChatMessage> = emptyList(),
    val isCompressingImage: Boolean = false,
    val stagedImageBase64: String? = null,
    val searchQuery: String = "",
    val searchResults: List<DirectoryUser> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val isSavingPhone: Boolean = false,
    val phoneStatusMessage: String? = null,
    val phoneStatusIsError: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val chatRepository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var chatsJob: Job? = null
    private var messagesJob: Job? = null
    private var searchJob: Job? = null
    private var typingTimeoutJob: Job? = null
    private var isLocallyTyping: Boolean = false
    private var typingChatId: String? = null
    private val deliveredSyncedAt = mutableMapOf<String, Long>()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val firebaseUser = authRepository.currentFirebaseUser()
            if (firebaseUser == null) {
                _uiState.update { it.copy(isInitializing = false) }
                return@launch
            }
            try {
                val existing = authRepository.loadUserProfile(firebaseUser.uid)
                    ?: authRepository.syncSignedInUser(firebaseUser, _uiState.value.language)
                _uiState.update {
                    it.copy(
                        isInitializing = false,
                        currentUser = existing,
                        language = existing.preferredLanguage
                    )
                }
                startObservingChats(existing.uid)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isInitializing = false,
                        errorMessage = e.localizedMessage
                    )
                }
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        _uiState.update { it.copy(language = language) }
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            try {
                authRepository.updatePreferredLanguage(user.uid, language)
                _uiState.update { it.copy(currentUser = user.copy(preferredLanguage = language)) }
            } catch (_: Exception) {
            }
        }
    }

    fun handleGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningIn = true, errorMessage = null) }
            try {
                val profile = authRepository.signInWithGoogleIdToken(
                    idToken = idToken,
                    preferredLanguage = _uiState.value.language
                )
                _uiState.update {
                    it.copy(
                        isSigningIn = false,
                        currentUser = profile,
                        language = profile.preferredLanguage
                    )
                }
                startObservingChats(profile.uid)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSigningIn = false,
                        errorMessage = e.localizedMessage ?: "Sign-in failed."
                    )
                }
            }
        }
    }

    fun reportError(message: String) {
        _uiState.update { it.copy(isSigningIn = false, errorMessage = message) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun startObservingChats(uid: String) {
        chatsJob?.cancel()
        deliveredSyncedAt.clear()
        chatsJob = viewModelScope.launch {
            chatRepository.observeChats(uid)
                .catch { error ->
                    _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                }
                .collect { list ->
                    _uiState.update { it.copy(chats = list) }
                    val openChatId = _uiState.value.activeChatId
                    list.forEach { thread ->
                        if (thread.chatId == openChatId) {
                            if (thread.unreadCount(uid) > 0) {
                                viewModelScope.launch {
                                    runCatching { chatRepository.clearUnreadCount(thread.chatId, uid) }
                                }
                            }
                        } else if (
                            thread.lastMessageSenderId.isNotBlank() &&
                            thread.lastMessageSenderId != uid &&
                            thread.lastMessageAtMillis > (deliveredSyncedAt[thread.chatId] ?: -1L)
                        ) {
                            deliveredSyncedAt[thread.chatId] = thread.lastMessageAtMillis
                            viewModelScope.launch {
                                runCatching { chatRepository.markChatMessagesDelivered(thread.chatId, uid) }
                            }
                        }
                    }
                }
        }
    }

    fun openChat(chatId: String, fallbackName: String = "", fallbackPhoto: String = "") {
        val uid = _uiState.value.currentUser?.uid ?: return
        stopTypingNow()
        val thread = _uiState.value.chats.firstOrNull { it.chatId == chatId }
        val resolvedName = thread?.peerName(uid)?.ifBlank { fallbackName } ?: fallbackName
        val resolvedPhoto = thread?.peerPhoto(uid)?.ifBlank { fallbackPhoto } ?: fallbackPhoto
        _uiState.update {
            it.copy(
                activeChatId = chatId,
                activePeerName = resolvedName,
                activePeerPhoto = resolvedPhoto,
                activeMessages = emptyList()
            )
        }
        if (thread != null && thread.unreadCount(uid) > 0) {
            viewModelScope.launch {
                runCatching { chatRepository.clearUnreadCount(chatId, uid) }
            }
        }
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepository.observeMessages(chatId, uid)
                .catch { error ->
                    _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                }
                .collect { messages ->
                    _uiState.update { it.copy(activeMessages = messages) }
                    val hasUnreadIncoming = messages.any { msg ->
                        msg.senderId.isNotBlank() &&
                            msg.senderId != uid &&
                            msg.status != com.lukuman.freechat.data.model.MessageStatus.READ
                    }
                    if (hasUnreadIncoming) {
                        viewModelScope.launch {
                            runCatching { chatRepository.markMessagesAsRead(chatId, uid, messages) }
                            val currentThread = _uiState.value.chats.firstOrNull { it.chatId == chatId }
                            if (currentThread != null && currentThread.unreadCount(uid) > 0) {
                                runCatching { chatRepository.clearUnreadCount(chatId, uid) }
                            }
                        }
                    }
                }
        }
    }

    fun closeChat() {
        stopTypingNow()
        messagesJob?.cancel()
        _uiState.update {
            it.copy(
                activeChatId = null,
                activePeerName = "",
                activePeerPhoto = "",
                activeMessages = emptyList(),
                isCompressingImage = false,
                stagedImageBase64 = null
            )
        }
    }

    fun onDraftChanged(newDraft: String) {
        val uid = _uiState.value.currentUser?.uid ?: return
        val chatId = _uiState.value.activeChatId ?: return

        if (newDraft.isBlank()) {
            stopTypingNow()
            return
        }

        typingTimeoutJob?.cancel()
        if (!isLocallyTyping || typingChatId != chatId) {
            isLocallyTyping = true
            typingChatId = chatId
            viewModelScope.launch {
                runCatching { chatRepository.setTypingStatus(chatId, uid, true) }
            }
        }
        typingTimeoutJob = viewModelScope.launch {
            delay(3000L)
            stopTypingNow()
        }
    }

    private fun stopTypingNow() {
        typingTimeoutJob?.cancel()
        typingTimeoutJob = null
        val uid = _uiState.value.currentUser?.uid
        val targetChatId = typingChatId
        if (isLocallyTyping && uid != null && targetChatId != null) {
            isLocallyTyping = false
            typingChatId = null
            viewModelScope.launch {
                runCatching { chatRepository.setTypingStatus(targetChatId, uid, false) }
            }
        } else {
            isLocallyTyping = false
            typingChatId = null
        }
    }

    fun pickAndCompressImage(context: Context, uri: Uri, errorText: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCompressingImage = true, errorMessage = null) }
            try {
                val base64 = ImageCompressor.compressUriToBase64(context, uri)
                if (base64.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(isCompressingImage = false, errorMessage = errorText)
                    }
                } else {
                    _uiState.update {
                        it.copy(isCompressingImage = false, stagedImageBase64 = base64)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCompressingImage = false,
                        errorMessage = e.localizedMessage ?: errorText
                    )
                }
            }
        }
    }

    fun clearStagedImage() {
        _uiState.update { it.copy(stagedImageBase64 = null) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        val uid = _uiState.value.currentUser?.uid ?: return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250)
            executeUserSearch(query, uid)
        }
    }

    fun runUserSearch() {
        val uid = _uiState.value.currentUser?.uid ?: return
        val query = _uiState.value.searchQuery
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            executeUserSearch(query, uid)
        }
    }

    private suspend fun executeUserSearch(query: String, uid: String) {
        _uiState.update { it.copy(isSearching = true, errorMessage = null) }
        try {
            val results = chatRepository.searchUsers(query, uid)
            _uiState.update {
                it.copy(
                    isSearching = false,
                    hasSearched = true,
                    searchResults = results
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isSearching = false,
                    hasSearched = true,
                    errorMessage = e.localizedMessage
                )
            }
        }
    }

    fun savePhoneNumber(rawPhone: String, strings: AppStrings) {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSavingPhone = true, phoneStatusMessage = null, phoneStatusIsError = false)
            }
            try {
                val nextHash = authRepository.updatePhoneNumber(
                    user = user,
                    rawPhone = rawPhone,
                    invalidPhoneError = strings.phoneInvalidError,
                    duplicatePhoneError = strings.phoneAlreadyTakenError
                )
                _uiState.update {
                    it.copy(
                        isSavingPhone = false,
                        currentUser = user.copy(phoneHash = nextHash),
                        phoneStatusMessage = if (nextHash.isEmpty()) {
                            strings.phoneRemovedStatus
                        } else {
                            strings.phoneSavedStatus
                        },
                        phoneStatusIsError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingPhone = false,
                        phoneStatusMessage = e.localizedMessage ?: strings.phoneInvalidError,
                        phoneStatusIsError = true
                    )
                }
            }
        }
    }

    fun clearPhoneFeedback() {
        _uiState.update { it.copy(phoneStatusMessage = null, phoneStatusIsError = false) }
    }

    fun startChatWithUser(otherUser: DirectoryUser, onReady: (String) -> Unit) {
        val current = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            try {
                val chatId = chatRepository.openOrCreateChat(current, otherUser)
                openChat(chatId, otherUser.displayName, otherUser.photoUrl)
                onReady(chatId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun sendMessage(text: String) {
        val user = _uiState.value.currentUser ?: return
        val chatId = _uiState.value.activeChatId ?: return
        val thread = _uiState.value.chats.firstOrNull { it.chatId == chatId }
        val participants = thread?.participantIds ?: chatId.split("_")
        if (participants.size != 2) return
        val stagedImage = _uiState.value.stagedImageBase64.orEmpty()
        if (text.trim().isEmpty() && stagedImage.isEmpty()) return

        typingTimeoutJob?.cancel()
        typingTimeoutJob = null
        isLocallyTyping = false
        typingChatId = null

        _uiState.update { it.copy(stagedImageBase64 = null) }
        viewModelScope.launch {
            try {
                chatRepository.sendMessage(
                    chatId = chatId,
                    senderUid = user.uid,
                    participantIds = participants,
                    rawText = text,
                    imageBase64 = stagedImage
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun signOut(context: Context) {
        stopTypingNow()
        deliveredSyncedAt.clear()
        chatsJob?.cancel()
        messagesJob?.cancel()
        authRepository.signOut(context)
        _uiState.update {
            MainUiState(
                isInitializing = false,
                language = it.language
            )
        }
    }

    override fun onCleared() {
        stopTypingNow()
        super.onCleared()
    }
}
