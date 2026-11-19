package com.lukuman.freechat.data.model

enum class AppLanguage(val code: String, val displayLabel: String) {
    HAUSA("ha", "Hausa"),
    ENGLISH("en", "English");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code == code } ?: HAUSA
        }
    }
}

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val preferredLanguage: AppLanguage = AppLanguage.HAUSA
)

data class DirectoryUser(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = ""
)

data class ChatThread(
    val chatId: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantPhotos: Map<String, String> = emptyMap(),
    val lastMessageText: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageAtMillis: Long = 0L
) {
    fun peerUid(currentUid: String): String {
        return participantIds.firstOrNull { it != currentUid } ?: currentUid
    }

    fun peerName(currentUid: String): String {
        val peer = peerUid(currentUid)
        return participantNames[peer].orEmpty().ifBlank { "User" }
    }

    fun peerPhoto(currentUid: String): String {
        val peer = peerUid(currentUid)
        return participantPhotos[peer].orEmpty()
    }
}

data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val participantIds: List<String> = emptyList(),
    val text: String = "",
    val createdAtMillis: Long = 0L
)
