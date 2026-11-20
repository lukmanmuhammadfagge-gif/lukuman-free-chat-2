package com.lukuman.freechat.data.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.lukuman.freechat.data.model.AppLanguage
import com.lukuman.freechat.data.model.UserProfile
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun currentFirebaseUser(): FirebaseUser? = auth.currentUser

    suspend fun signInWithGoogleIdToken(
        idToken: String,
        preferredLanguage: AppLanguage
    ): UserProfile {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: throw IllegalStateException("Authentication failed.")
        return syncSignedInUser(user, preferredLanguage)
    }

    suspend fun syncSignedInUser(
        user: FirebaseUser,
        preferredLanguage: AppLanguage
    ): UserProfile {
        val uid = user.uid
        val displayName = (user.displayName?.trim().takeIf { !it.isNullOrBlank() }
            ?: user.email?.substringBefore("@")
            ?: "Lukuman User").take(100)
        val email = (user.email?.trim().orEmpty()).take(200)
        val photoUrl = (user.photoUrl?.toString().orEmpty()).take(500)
        val emailHash = sha256Hex(email.lowercase())

        val userRef = db.collection("users").document(uid)
        val existingUser = userRef.get().await()

        val resolvedLanguage = if (existingUser.exists()) {
            AppLanguage.fromCode(existingUser.getString("preferredLanguage"))
        } else {
            preferredLanguage
        }
        val existingPhoneHash = if (existingUser.exists()) {
            existingUser.getString("phoneHash").orEmpty()
        } else {
            ""
        }

        val batch = db.batch()
        if (existingUser.exists()) {
            batch.update(
                userRef,
                mapOf(
                    "displayName" to displayName,
                    "email" to email,
                    "photoUrl" to photoUrl,
                    "preferredLanguage" to resolvedLanguage.code,
                    "phoneHash" to existingPhoneHash,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        } else {
            batch.set(
                userRef,
                mapOf(
                    "uid" to uid,
                    "displayName" to displayName,
                    "email" to email,
                    "photoUrl" to photoUrl,
                    "preferredLanguage" to resolvedLanguage.code,
                    "phoneHash" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        val directoryRef = db.collection("user_directory").document(uid)
        batch.set(
            directoryRef,
            mapOf(
                "uid" to uid,
                "displayName" to displayName,
                "displayNameLower" to displayName.lowercase(),
                "photoUrl" to photoUrl,
                "emailHash" to emailHash,
                "searchable" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        val lookupRef = db.collection("email_lookup").document(emailHash)
        batch.set(
            lookupRef,
            mapOf(
                "uid" to uid,
                "displayName" to displayName,
                "photoUrl" to photoUrl,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        if (existingPhoneHash.length == 64) {
            val phoneLookupRef = db.collection("phone_lookup").document(existingPhoneHash)
            batch.set(
                phoneLookupRef,
                mapOf(
                    "uid" to uid,
                    "displayName" to displayName,
                    "photoUrl" to photoUrl,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        batch.commit().await()

        return UserProfile(
            uid = uid,
            displayName = displayName,
            email = email,
            photoUrl = photoUrl,
            preferredLanguage = resolvedLanguage,
            phoneHash = existingPhoneHash
        )
    }

    suspend fun loadUserProfile(uid: String): UserProfile? {
        val snapshot = db.collection("users").document(uid).get().await()
        if (!snapshot.exists()) return null
        return UserProfile(
            uid = snapshot.getString("uid").orEmpty(),
            displayName = snapshot.getString("displayName").orEmpty(),
            email = snapshot.getString("email").orEmpty(),
            photoUrl = snapshot.getString("photoUrl").orEmpty(),
            preferredLanguage = AppLanguage.fromCode(snapshot.getString("preferredLanguage")),
            phoneHash = snapshot.getString("phoneHash").orEmpty()
        )
    }

    suspend fun updatePreferredLanguage(uid: String, language: AppLanguage) {
        db.collection("users").document(uid).update(
            mapOf(
                "preferredLanguage" to language.code,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun updatePhoneNumber(
        user: UserProfile,
        rawPhone: String,
        invalidPhoneError: String,
        duplicatePhoneError: String
    ): String {
        val trimmed = rawPhone.trim()
        val oldHash = user.phoneHash

        if (trimmed.isEmpty()) {
            val batch = db.batch()
            if (oldHash.length == 64) {
                batch.delete(db.collection("phone_lookup").document(oldHash))
            }
            batch.update(
                db.collection("users").document(user.uid),
                mapOf(
                    "phoneHash" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            return ""
        }

        val normalized = com.lukuman.freechat.util.PhoneUtils.normalizeNigerianPhone(trimmed)
            ?: throw IllegalArgumentException(invalidPhoneError)
        val newHash = sha256Hex(normalized)

        if (newHash != oldHash) {
            val existingLookup = db.collection("phone_lookup").document(newHash).get().await()
            if (existingLookup.exists() && existingLookup.getString("uid") != user.uid) {
                throw IllegalStateException(duplicatePhoneError)
            }
        }

        val batch = db.batch()
        if (oldHash.length == 64 && oldHash != newHash) {
            batch.delete(db.collection("phone_lookup").document(oldHash))
        }
        batch.set(
            db.collection("phone_lookup").document(newHash),
            mapOf(
                "uid" to user.uid,
                "displayName" to user.displayName.take(100).ifBlank { "Lukuman User" },
                "photoUrl" to user.photoUrl.take(500),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            db.collection("users").document(user.uid),
            mapOf(
                "phoneHash" to newHash,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        return newHash
    }

    fun signOut(context: Context) {
        auth.signOut()
        val webClientIdRes = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (webClientIdRes != 0) {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(webClientIdRes))
                .requestEmail()
                .build()
            GoogleSignIn.getClient(context, gso).signOut()
        }
    }

    companion object {
        fun sha256Hex(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
