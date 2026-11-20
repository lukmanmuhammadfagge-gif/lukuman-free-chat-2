import { User } from 'firebase/auth';
import {
  collection,
  doc,
  getDoc,
  getDocs,
  increment,
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
import { normalizeNigerianPhone } from '../utils/phoneUtils';

export interface UserProfile {
  uid: string;
  displayName: string;
  email: string;
  photoUrl: string;
  preferredLanguage: AppLanguage;
  phoneHash: string;
}

export interface DirectoryUser {
  uid: string;
  displayName: string;
  photoUrl: string;
}

export type MessageStatus = 'sent' | 'delivered' | 'read';

export interface ChatThread {
  chatId: string;
  participantIds: string[];
  participantNames: Record<string, string>;
  participantPhotos: Record<string, string>;
  lastMessageText: string;
  lastMessageSenderId: string;
  lastMessageAtMillis: number;
  unreadCounts: Record<string, number>;
  typingUsers: Record<string, boolean>;
}

export const PHOTO_MESSAGE_MARKER = '__IMAGE__';

export interface ChatMessage {
  id: string;
  chatId: string;
  senderId: string;
  participantIds: string[];
  text: string;
  imageBase64: string;
  status: MessageStatus;
  createdAtMillis: number;
  deliveredAtMillis: number;
  readAtMillis: number;
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
      phoneHash: String(data.phoneHash || ''),
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
  const existingPhoneHash = existingSnap.exists()
    ? String(existingSnap.data().phoneHash || '')
    : '';

  const batch = writeBatch(db);
  if (existingSnap.exists()) {
    batch.update(userRef, {
      displayName,
      email,
      photoUrl,
      preferredLanguage: resolvedLanguage,
      phoneHash: existingPhoneHash,
      updatedAt: serverTimestamp(),
    });
  } else {
    batch.set(userRef, {
      uid,
      displayName,
      email,
      photoUrl,
      preferredLanguage: resolvedLanguage,
      phoneHash: '',
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

  if (existingPhoneHash.length === 64) {
    batch.set(doc(db, 'phone_lookup', existingPhoneHash), {
      uid,
      displayName,
      photoUrl,
      updatedAt: serverTimestamp(),
    });
  }

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
    phoneHash: existingPhoneHash,
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

export async function saveUserPhoneHash(
  user: UserProfile,
  rawPhone: string,
  invalidPhoneError: string,
  duplicatePhoneError: string
): Promise<string> {
  const trimmed = rawPhone.trim();
  const oldHash = user.phoneHash || '';

  if (!trimmed) {
    const batch = writeBatch(db);
    if (oldHash.length === 64) {
      batch.delete(doc(db, 'phone_lookup', oldHash));
    }
    batch.update(doc(db, 'users', user.uid), {
      phoneHash: '',
      updatedAt: serverTimestamp(),
    });
    try {
      await batch.commit();
      return '';
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, `users/${user.uid}`);
    }
  }

  const normalized = normalizeNigerianPhone(trimmed);
  if (!normalized) {
    throw new Error(invalidPhoneError);
  }
  const newHash = await sha256Hex(normalized);

  if (newHash !== oldHash) {
    let existingSnap;
    try {
      existingSnap = await getDoc(doc(db, 'phone_lookup', newHash));
    } catch (error) {
      handleFirestoreError(error, OperationType.GET, `phone_lookup/${newHash}`);
    }
    if (existingSnap.exists() && String(existingSnap.data().uid || '') !== user.uid) {
      throw new Error(duplicatePhoneError);
    }
  }

  const batch = writeBatch(db);
  if (oldHash.length === 64 && oldHash !== newHash) {
    batch.delete(doc(db, 'phone_lookup', oldHash));
  }
  batch.set(doc(db, 'phone_lookup', newHash), {
    uid: user.uid,
    displayName: (user.displayName || 'Lukuman User').slice(0, 100),
    photoUrl: (user.photoUrl || '').slice(0, 500),
    updatedAt: serverTimestamp(),
  });
  batch.update(doc(db, 'users', user.uid), {
    phoneHash: newHash,
    updatedAt: serverTimestamp(),
  });

  try {
    await batch.commit();
    return newHash;
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, `phone_lookup/${newHash}`);
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

        const rawUnread = (data.unreadCounts as Record<string, unknown>) || {};
        const unreadCounts: Record<string, number> = {};
        for (const uid of participantIds) {
          const val = rawUnread[uid];
          unreadCounts[uid] = typeof val === 'number' && Number.isFinite(val) ? Math.max(0, val) : 0;
        }

        const rawTyping = (data.typingUsers as Record<string, unknown>) || {};
        const typingUsers: Record<string, boolean> = {};
        for (const uid of participantIds) {
          typingUsers[uid] = rawTyping[uid] === true;
        }

        threads.push({
          chatId: String(data.chatId || docSnap.id),
          participantIds,
          participantNames: (data.participantNames as Record<string, string>) || {},
          participantPhotos: (data.participantPhotos as Record<string, string>) || {},
          lastMessageText: String(data.lastMessageText || ''),
          lastMessageSenderId: String(data.lastMessageSenderId || ''),
          lastMessageAtMillis: lastAt,
          unreadCounts,
          typingUsers,
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
        const deliveredAtMillis =
          data.deliveredAt instanceof Timestamp ? data.deliveredAt.toMillis() : 0;
        const readAtMillis =
          data.readAt instanceof Timestamp ? data.readAt.toMillis() : 0;
        const rawStatus = data.status;
        const status: MessageStatus =
          rawStatus === 'read' || rawStatus === 'delivered' ? rawStatus : 'sent';
        return {
          id: String(data.id || docSnap.id),
          chatId: String(data.chatId || chatId),
          senderId: String(data.senderId || ''),
          participantIds: Array.isArray(data.participantIds)
            ? data.participantIds.map(String)
            : [],
          text: String(data.text || ''),
          imageBase64: String(data.imageBase64 || ''),
          status,
          createdAtMillis,
          deliveredAtMillis,
          readAtMillis,
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

  const normalizedPhone = normalizeNigerianPhone(trimmed);
  if (normalizedPhone) {
    const phoneHash = await sha256Hex(normalizedPhone);
    const path = `phone_lookup/${phoneHash}`;
    try {
      const snap = await getDoc(doc(db, 'phone_lookup', phoneHash));
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
      limit(200)
    );
    const snap = await getDocs(dirQuery);
    const lower = trimmed.toLowerCase();

    return snap.docs
      .map((d) => {
        const data = d.data();
        const displayName = String(data.displayName || '');
        const displayNameLower = String(data.displayNameLower || displayName.toLowerCase());
        return {
          uid: String(data.uid || ''),
          displayName,
          displayNameLower,
          photoUrl: String(data.photoUrl || ''),
        };
      })
      .filter(
        (u) =>
          u.uid &&
          u.uid !== currentUid &&
          (!lower ||
            u.displayNameLower.includes(lower) ||
            u.displayName.toLowerCase().includes(lower))
      )
      .sort((a, b) => {
        if (lower) {
          const aPrefix = a.displayNameLower.startsWith(lower) ? 0 : 1;
          const bPrefix = b.displayNameLower.startsWith(lower) ? 0 : 1;
          if (aPrefix !== bPrefix) return aPrefix - bPrefix;
        }
        return a.displayName.localeCompare(b.displayName);
      })
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
        unreadCounts: {
          [currentUser.uid]: 0,
          [otherUser.uid]: 0,
        },
        typingUsers: {
          [currentUser.uid]: false,
          [otherUser.uid]: false,
        },
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
  rawText: string,
  imageBase64 = ''
): Promise<void> {
  const text = rawText.trim().slice(0, 2000);
  const cleanImage = imageBase64.trim();
  if ((!text && !cleanImage) || participantIds.length !== 2) return;
  if (cleanImage.length > 220000) return;

  const peerUid = participantIds.find((id) => id !== senderUid);
  if (!peerUid) return;

  const messageId = crypto.randomUUID().replace(/-/g, '');
  const chatRef = doc(db, 'chats', chatId);
  const messageRef = doc(db, 'chats', chatId, 'messages', messageId);
  const lastSummary = text || PHOTO_MESSAGE_MARKER;

  const batch = writeBatch(db);
  batch.set(messageRef, {
    id: messageId,
    chatId,
    senderId: senderUid,
    participantIds,
    text,
    imageBase64: cleanImage,
    status: 'sent',
    createdAt: serverTimestamp(),
  });
  batch.update(chatRef, {
    lastMessageText: lastSummary,
    lastMessageSenderId: senderUid,
    lastMessageAt: serverTimestamp(),
    [`unreadCounts.${peerUid}`]: increment(1),
    [`typingUsers.${senderUid}`]: false,
    updatedAt: serverTimestamp(),
  });

  try {
    await batch.commit();
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, `chats/${chatId}/messages/${messageId}`);
  }
}

export async function setTypingStatus(
  chatId: string,
  uid: string,
  isTyping: boolean
): Promise<void> {
  try {
    await updateDoc(doc(db, 'chats', chatId), {
      [`typingUsers.${uid}`]: isTyping,
      updatedAt: serverTimestamp(),
    });
  } catch {
    // Non-fatal UI indicator update
  }
}

export async function clearUnreadCount(chatId: string, uid: string): Promise<void> {
  try {
    await updateDoc(doc(db, 'chats', chatId), {
      [`unreadCounts.${uid}`]: 0,
      updatedAt: serverTimestamp(),
    });
  } catch {
    // Non-fatal counter reset
  }
}

export async function markChatMessagesDelivered(
  chatId: string,
  currentUid: string
): Promise<void> {
  try {
    const messagesQuery = query(
      collection(db, 'chats', chatId, 'messages'),
      where('participantIds', 'array-contains', currentUid)
    );
    const snap = await getDocs(messagesQuery);
    const pending = snap.docs.filter((docSnap) => {
      const data = docSnap.data();
      const senderId = String(data.senderId || '');
      const status = data.status;
      return senderId !== currentUid && (!status || status === 'sent');
    });
    if (pending.length === 0) return;

    for (let i = 0; i < pending.length; i += 400) {
      const chunk = pending.slice(i, i + 400);
      const batch = writeBatch(db);
      for (const docSnap of chunk) {
        batch.update(docSnap.ref, {
          status: 'delivered',
          deliveredAt: serverTimestamp(),
        });
      }
      await batch.commit();
    }
  } catch {
    // Non-fatal status sync
  }
}

export async function markMessagesAsRead(
  chatId: string,
  currentUid: string,
  messages: ChatMessage[]
): Promise<void> {
  const unreadIncoming = messages.filter(
    (msg) => msg.senderId !== currentUid && msg.status !== 'read'
  );
  if (unreadIncoming.length === 0) return;

  try {
    for (let i = 0; i < unreadIncoming.length; i += 400) {
      const chunk = unreadIncoming.slice(i, i + 400);
      const batch = writeBatch(db);
      for (const msg of chunk) {
        const updatePayload: Record<string, unknown> = {
          status: 'read',
          readAt: serverTimestamp(),
        };
        if (msg.deliveredAtMillis <= 0) {
          updatePayload.deliveredAt = serverTimestamp();
        }
        batch.update(doc(db, 'chats', chatId, 'messages', msg.id), updatePayload);
      }
      await batch.commit();
    }
  } catch {
    // Non-fatal status sync
  }
}
