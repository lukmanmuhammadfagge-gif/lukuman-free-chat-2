package com.lukuman.freechat.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.lukuman.freechat.data.model.ChatMessage
import com.lukuman.freechat.data.model.ChatThread
import com.lukuman.freechat.data.model.DirectoryUser
import com.lukuman.freechat.data.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun observeChats(currentUid: String): Flow<List<ChatThread>> = callbackFlow {
        val registration = db.collection("chats")
            .whereArrayContains("participantIds", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val participantIds = (doc.get("participantIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()
                    if (participantIds.size != 2) return@mapNotNull null

                    val namesRaw = doc.get("participantNames") as? Map<*, *>
                    val photosRaw = doc.get("participantPhotos") as? Map<*, *>
                    val participantNames = namesRaw
                        ?.mapNotNull { (k, v) -> if (k is String && v is String) k to v else null }
                        ?.toMap() ?: emptyMap()
                    val participantPhotos = photosRaw
                        ?.mapNotNull { (k, v) -> if (k is String && v is String) k to v else null }
                        ?.toMap() ?: emptyMap()

                    val lastAt = doc.getTimestamp("lastMessageAt")?.toDate()?.time ?: 0L
                    ChatThread(
                        chatId = doc.getString("chatId").orEmpty().ifBlank { doc.id },
                        participantIds = participantIds,
                        participantNames = participantNames,
                        participantPhotos = participantPhotos,
                        lastMessageText = doc.getString("lastMessageText").orEmpty(),
                        lastMessageSenderId = doc.getString("lastMessageSenderId").orEmpty(),
                        lastMessageAtMillis = lastAt
                    )
                }?.sortedByDescending { it.lastMessageAtMillis } ?: emptyList()

                trySend(items)
            }

        awaitClose { registration.remove() }
    }

    fun observeMessages(chatId: String, currentUid: String): Flow<List<ChatMessage>> = callbackFlow {
        val registration = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .whereArrayContains("participantIds", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.map { doc ->
                    val participantIds = (doc.get("participantIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?: emptyList()
                    ChatMessage(
                        id = doc.getString("id").orEmpty().ifBlank { doc.id },
                        chatId = doc.getString("chatId").orEmpty().ifBlank { chatId },
                        senderId = doc.getString("senderId").orEmpty(),
                        participantIds = participantIds,
                        text = doc.getString("text").orEmpty(),
                        createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    )
                }?.sortedBy { it.createdAtMillis } ?: emptyList()

                trySend(messages)
            }

        awaitClose { registration.remove() }
    }

    suspend fun searchUsers(query: String, currentUid: String): List<DirectoryUser> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            val snapshot = db.collection("user_directory")
                .whereEqualTo("searchable", true)
                .limit(50)
                .get()
                .await()
            return snapshot.documents.mapNotNull { doc ->
                val uid = doc.getString("uid").orEmpty()
                if (uid.isBlank() || uid == currentUid) return@mapNotNull null
                DirectoryUser(
                    uid = uid,
                    displayName = doc.getString("displayName").orEmpty(),
                    photoUrl = doc.getString("photoUrl").orEmpty()
                )
            }.sortedBy { it.displayName.lowercase() }
        }

        if (trimmed.contains("@")) {
            val hash = AuthRepository.sha256Hex(trimmed.lowercase())
            val doc = db.collection("email_lookup").document(hash).get().await()
            if (!doc.exists()) return emptyList()
            val uid = doc.getString("uid").orEmpty()
            if (uid.isBlank() || uid == currentUid) return emptyList()
            return listOf(
                DirectoryUser(
                    uid = uid,
                    displayName = doc.getString("displayName").orEmpty(),
                    photoUrl = doc.getString("photoUrl").orEmpty()
                )
            )
        }

        val lowerQuery = trimmed.lowercase()
        val snapshot = db.collection("user_directory")
            .whereEqualTo("searchable", true)
            .limit(100)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val uid = doc.getString("uid").orEmpty()
            val name = doc.getString("displayName").orEmpty()
            val nameLower = doc.getString("displayNameLower").orEmpty()
            if (uid.isBlank() || uid == currentUid) return@mapNotNull null
            if (!nameLower.contains(lowerQuery)) return@mapNotNull null
            DirectoryUser(
                uid = uid,
                displayName = name,
                photoUrl = doc.getString("photoUrl").orEmpty()
            )
        }.sortedBy { it.displayName.lowercase() }
    }

    suspend fun openOrCreateChat(currentUser: UserProfile, otherUser: DirectoryUser): String {
        val sortedIds = listOf(currentUser.uid, otherUser.uid).sorted()
        val chatId = "${sortedIds[0]}_${sortedIds[1]}"
        val chatRef = db.collection("chats").document(chatId)
        val existing = chatRef.get().await()
        if (!existing.exists()) {
            val names = mapOf(
                currentUser.uid to currentUser.displayName.take(100).ifBlank { "User" },
                otherUser.uid to otherUser.displayName.take(100).ifBlank { "User" }
            )
            val photos = mapOf(
                currentUser.uid to currentUser.photoUrl.take(500),
                otherUser.uid to otherUser.photoUrl.take(500)
            )
            chatRef.set(
                mapOf(
                    "chatId" to chatId,
                    "participantIds" to sortedIds,
                    "participantNames" to names,
                    "participantPhotos" to photos,
                    "lastMessageText" to "",
                    "lastMessageSenderId" to "",
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        return chatId
    }

    suspend fun sendMessage(
        chatId: String,
        senderUid: String,
        participantIds: List<String>,
        rawText: String
    ) {
        val text = rawText.trim().take(2000)
        if (text.isEmpty() || participantIds.size != 2) return

        val messageId = UUID.randomUUID().toString().replace("-", "")
        val chatRef = db.collection("chats").document(chatId)
        val messageRef = chatRef.collection("messages").document(messageId)

        val batch = db.batch()
        batch.set(
            messageRef,
            mapOf(
                "id" to messageId,
                "chatId" to chatId,
                "senderId" to senderUid,
                "participantIds" to participantIds,
                "text" to text,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            chatRef,
            mapOf(
                "lastMessageText" to text,
                "lastMessageSenderId" to senderUid,
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
    }
}
