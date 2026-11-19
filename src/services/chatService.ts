import { User } from 'firebase/auth';
import {
  collection,
  doc,
  getDoc,
  getDocs,
  limit,
  onSnapshot,
  query,
  serverTimestamp,
  Timestamp,
  updateDoc,
  where,
  writeBatch,
} from 'firebase/firestore';
import { db, handleFirestoreError, OperationType } from '../firebase';
import { AppLanguage } from '../i18n';

export interface UserProfile {
  uid: string;
  displayName: string;
  email: string;
  photoUrl: string;
  preferredLanguage: AppLanguage;
}

export interface DirectoryUser {
  uid: string;
  displayName: string;
  photoUrl: string;
}

export interface ChatThread {
  chatId: string;
  participantIds: string[];
  participantNames: Record<string, string>;
  participantPhotos: Record<string, string>;
  lastMessageText: string;
  lastMessageSenderId: string;
  lastMessageAtMillis: number;
}

export interface ChatMessage {
  id: string;
  chatId: string;
  senderId: string;
  participantIds: string[];
  text: string;
  createdAtMillis: number;
}

export async function sha256Hex(input: string): Promise<string> {
  const data = new TextEncoder().encode(input);
  const hashBuffer = await crypto.subtle.digest('SHA-256', data);
  return Array.from(new Uint8Array(hashBuffer))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

export async function loadUserProfile(uid: string): Promise<UserProfile | null> {
  const path = `users/${uid}`;
  try {
    const snap = await getDoc(doc(db, 'users', uid));
    if (!snap.exists()) return null;
    const data = snap.data();
    return {
      uid: String(data.uid || uid),
      displayName: String(data.displayName || 'User'),
      email: String(data.email || ''),
      photoUrl: String(data.photoUrl || ''),
      preferredLanguage: data.preferredLanguage === 'en' ? 'en' : 'ha',
    };
  } catch (error) {
    handleFirestoreError(error, OperationType.GET, path);
  }
}

export async function syncUserProfile(
  firebaseUser: User,
  preferredLanguage: AppLanguage
): Promise<UserProfile> {
  const uid = firebaseUser.uid;
  const displayName = (
    firebaseUser.displayName?.trim() ||
    firebaseUser.email?.split('@')[0] ||
    'Lukuman User'
  ).slice(0, 100);
  const email = (firebaseUser.email?.trim() || '').slice(0, 200);
  const photoUrl = (firebaseUser.photoURL || '').slice(0, 500);
  const emailHash = await sha256Hex(email.toLowerCase());

  const userRef = doc(db, 'users', uid);
  let existingSnap;
  try {
    existingSnap = await getDoc(userRef);
  } catch (error) {
    handleFirestoreError(error, OperationType.GET, `users/${uid}`);
  }

  const resolvedLanguage: AppLanguage = existingSnap.exists()
    ? existingSnap.data().preferredLanguage === 'en'
      ? 'en'
      : 'ha'
    : preferredLanguage;

  const batch = writeBatch(db);
  if (existingSnap.exists()) {
    batch.update(userRef, {
      displayName,
      email,
      photoUrl,
      preferredLanguage: resolvedLanguage,
      updatedAt: serverTimestamp(),
    });
  } else {
    batch.set(userRef, {
      uid,
      displayName,
      email,
      photoUrl,
      preferredLanguage: resolvedLanguage,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  }

  batch.set(doc(db, 'user_directory', uid), {
    uid,
    displayName,
    displayNameLower: displayName.toLowerCase(),
    photoUrl,
    emailHash,
    searchable: true,
    updatedAt: serverTimestamp(),
  });

  batch.set(doc(db, 'email_lookup', emailHash), {
    uid,
    displayName,
    photoUrl,
    updatedAt: serverTimestamp(),
  });

  try {
    await batch.commit();
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, `users/${uid}`);
  }

  return {
    uid,
    displayName,
    email,
    photoUrl,
    preferredLanguage: resolvedLanguage,
  };
}

export async function updatePreferredLanguage(
  uid: string,
  preferredLanguage: AppLanguage
): Promise<void> {
  const path = `users/${uid}`;
  try {
    await updateDoc(doc(db, 'users', uid), {
      preferredLanguage,
      updatedAt: serverTimestamp(),
    });
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, path);
  }
}

export function subscribeUserChats(
  currentUid: string,
  onChange: (chats: ChatThread[]) => void
): () => void {
  const chatsQuery = query(
    collection(db, 'chats'),
    where('participantIds', 'array-contains', currentUid)
  );

  return onSnapshot(
    chatsQuery,
    (snapshot) => {
      const threads: ChatThread[] = [];
      for (const docSnap of snapshot.docs) {
        const data = docSnap.data();
        const participantIds = Array.isArray(data.participantIds)
          ? data.participantIds.map(String)
          : [];
        if (participantIds.length !== 2) continue;

        const lastAt =
          data.lastMessageAt instanceof Timestamp ? data.lastMessageAt.toMillis() : 0;

        threads.push({
          chatId: String(data.chatId || docSnap.id),
          participantIds,
          participantNames: (data.participantNames as Record<string, string>) || {},
          participantPhotos: (data.participantPhotos as Record<string, string>) || {},
          lastMessageText: String(data.lastMessageText || ''),
          lastMessageSenderId: String(data.lastMessageSenderId || ''),
          lastMessageAtMillis: lastAt,
        });
      }
      threads.sort((a, b) => b.lastMessageAtMillis - a.lastMessageAtMillis);
      onChange(threads);
    },
    (error) => {
      handleFirestoreError(error, OperationType.LIST, 'chats');
    }
  );
}

export function subscribeChatMessages(
  chatId: string,
  currentUid: string,
  onChange: (messages: ChatMessage[]) => void
): () => void {
  const messagesQuery = query(
    collection(db, 'chats', chatId, 'messages'),
    where('participantIds', 'array-contains', currentUid)
  );

  return onSnapshot(
    messagesQuery,
    (snapshot) => {
      const messages: ChatMessage[] = snapshot.docs.map((docSnap) => {
        const data = docSnap.data();
        const createdAtMillis =
          data.createdAt instanceof Timestamp ? data.createdAt.toMillis() : Date.now();
        return {
          id: String(data.id || docSnap.id),
          chatId: String(data.chatId || chatId),
          senderId: String(data.senderId || ''),
          participantIds: Array.isArray(data.participantIds)
            ? data.participantIds.map(String)
            : [],
          text: String(data.text || ''),
          createdAtMillis,
        };
      });
      messages.sort((a, b) => a.createdAtMillis - b.createdAtMillis);
      onChange(messages);
    },
    (error) => {
      handleFirestoreError(error, OperationType.LIST, `chats/${chatId}/messages`);
    }
  );
}

export async function searchUsers(
  rawQuery: string,
  currentUid: string
): Promise<DirectoryUser[]> {
  const trimmed = rawQuery.trim();

  if (trimmed.includes('@')) {
    const hash = await sha256Hex(trimmed.toLowerCase());
    const path = `email_lookup/${hash}`;
    try {
      const snap = await getDoc(doc(db, 'email_lookup', hash));
      if (!snap.exists()) return [];
      const data = snap.data();
      const uid = String(data.uid || '');
      if (!uid || uid === currentUid) return [];
      return [
        {
          uid,
          displayName: String(data.displayName || 'User'),
          photoUrl: String(data.photoUrl || ''),
        },
      ];
    } catch (error) {
      handleFirestoreError(error, OperationType.GET, path);
    }
  }

  try {
    const dirQuery = query(
      collection(db, 'user_directory'),
      where('searchable', '==', true),
      limit(100)
    );
    const snap = await getDocs(dirQuery);
    const lower = trimmed.toLowerCase();

    return snap.docs
      .map((d) => {
        const data = d.data();
        return {
          uid: String(data.uid || ''),
          displayName: String(data.displayName || ''),
          displayNameLower: String(data.displayNameLower || ''),
          photoUrl: String(data.photoUrl || ''),
        };
      })
      .filter(
        (u) =>
          u.uid &&
          u.uid !== currentUid &&
          (!lower || u.displayNameLower.includes(lower))
      )
      .sort((a, b) => a.displayName.localeCompare(b.displayName))
      .map(({ uid, displayName, photoUrl }) => ({ uid, displayName, photoUrl }));
  } catch (error) {
    handleFirestoreError(error, OperationType.LIST, 'user_directory');
  }
}

export async function openOrCreateChat(
  currentUser: UserProfile,
  otherUser: DirectoryUser
): Promise<string> {
  const sortedIds = [currentUser.uid, otherUser.uid].sort();
  const chatId = `${sortedIds[0]}_${sortedIds[1]}`;
  const chatRef = doc(db, 'chats', chatId);
  const path = `chats/${chatId}`;

  try {
    const existing = await getDoc(chatRef);
    if (!existing.exists()) {
      const batch = writeBatch(db);
      batch.set(chatRef, {
        chatId,
        participantIds: sortedIds,
        participantNames: {
          [currentUser.uid]: (currentUser.displayName || 'User').slice(0, 100),
          [otherUser.uid]: (otherUser.displayName || 'User').slice(0, 100),
        },
        participantPhotos: {
          [currentUser.uid]: (currentUser.photoUrl || '').slice(0, 500),
          [otherUser.uid]: (otherUser.photoUrl || '').slice(0, 500),
        },
        lastMessageText: '',
        lastMessageSenderId: '',
        lastMessageAt: serverTimestamp(),
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      await batch.commit();
    }
    return chatId;
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, path);
  }
}

export async function sendMessage(
  chatId: string,
  senderUid: string,
  participantIds: string[],
  rawText: string
): Promise<void> {
  const text = rawText.trim().slice(0, 2000);
  if (!text || participantIds.length !== 2) return;

  const messageId = crypto.randomUUID().replace(/-/g, '');
  const chatRef = doc(db, 'chats', chatId);
  const messageRef = doc(db, 'chats', chatId, 'messages', messageId);

  const batch = writeBatch(db);
  batch.set(messageRef, {
    id: messageId,
    chatId,
    senderId: senderUid,
    participantIds,
    text,
    createdAt: serverTimestamp(),
  });
  batch.update(chatRef, {
    lastMessageText: text,
    lastMessageSenderId: senderUid,
    lastMessageAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  });

  try {
    await batch.commit();
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, `chats/${chatId}/messages/${messageId}`);
  }
}
