package com.lukuman.freechat.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lukuman.freechat.data.model.AppLanguage
import com.lukuman.freechat.data.model.ChatMessage
import com.lukuman.freechat.data.model.ChatThread
import com.lukuman.freechat.data.model.DirectoryUser
import com.lukuman.freechat.data.model.UserProfile
import com.lukuman.freechat.data.repository.AuthRepository
import com.lukuman.freechat.data.repository.ChatRepository
import kotlinx.coroutines.Job
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
    val searchQuery: String = "",
    val searchResults: List<DirectoryUser> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
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
        chatsJob = viewModelScope.launch {
            chatRepository.observeChats(uid)
                .catch { error ->
                    _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                }
                .collect { list ->
                    _uiState.update { it.copy(chats = list) }
                }
        }
    }

    fun openChat(chatId: String, fallbackName: String = "", fallbackPhoto: String = "") {
        val uid = _uiState.value.currentUser?.uid ?: return
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
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepository.observeMessages(chatId, uid)
                .catch { error ->
                    _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                }
                .collect { messages ->
                    _uiState.update { it.copy(activeMessages = messages) }
                }
        }
    }

    fun closeChat() {
        messagesJob?.cancel()
        _uiState.update {
            it.copy(
                activeChatId = null,
                activePeerName = "",
                activePeerPhoto = "",
                activeMessages = emptyList()
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun runUserSearch() {
        val uid = _uiState.value.currentUser?.uid ?: return
        val query = _uiState.value.searchQuery
        viewModelScope.launch {
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

        viewModelScope.launch {
            try {
                chatRepository.sendMessage(
                    chatId = chatId,
                    senderUid = user.uid,
                    participantIds = participants,
                    rawText = text
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun signOut(context: Context) {
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
}
