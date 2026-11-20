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
    val preferredLanguage: AppLanguage = AppLanguage.HAUSA,
    val phoneHash: String = ""
)

data class DirectoryUser(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = ""
)

enum class MessageStatus(val value: String) {
    SENT("sent"),
    DELIVERED("delivered"),
    READ("read");

    companion object {
        fun fromValue(raw: String?): MessageStatus {
            return entries.firstOrNull { it.value == raw } ?: SENT
        }
    }
}

data class ChatThread(
    val chatId: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantPhotos: Map<String, String> = emptyMap(),
    val lastMessageText: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageAtMillis: Long = 0L,
    val unreadCounts: Map<String, Int> = emptyMap(),
    val typingUsers: Map<String, Boolean> = emptyMap()
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

    fun unreadCount(currentUid: String): Int {
        return (unreadCounts[currentUid] ?: 0).coerceAtLeast(0)
    }

    fun isPeerTyping(currentUid: String): Boolean {
        val peer = peerUid(currentUid)
        return typingUsers[peer] == true
    }
}

const val PHOTO_MESSAGE_MARKER = "__IMAGE__"

data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val participantIds: List<String> = emptyList(),
    val text: String = "",
    val imageBase64: String = "",
    val status: MessageStatus = MessageStatus.SENT,
    val createdAtMillis: Long = 0L,
    val deliveredAtMillis: Long = 0L,
    val readAtMillis: Long = 0L
)
