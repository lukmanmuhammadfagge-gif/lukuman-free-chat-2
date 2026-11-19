/**
 * Security Rules Specification Verification Suite for Lukuman Free Chat.
 * Defines the "Dirty Dozen" adversarial payloads verified against firestore.rules.
 */

export interface DirtyDozenCase {
  id: number;
  name: string;
  operation: 'get' | 'list' | 'create' | 'update' | 'delete';
  path: string;
  auth: { uid: string; email_verified: boolean } | null;
  payload?: Record<string, unknown>;
  expected: 'PERMISSION_DENIED';
}

export const DIRTY_DOZEN_PAYLOADS: DirtyDozenCase[] = [
  {
    id: 1,
    name: 'Unauthenticated Chat Read',
    operation: 'get',
    path: '/chats/userA_userB',
    auth: null,
    expected: 'PERMISSION_DENIED',
  },
  {
    id: 2,
    name: 'Cross-User Chat Spying',
    operation: 'get',
    path: '/chats/userA_userB',
    auth: { uid: 'userC', email_verified: true },
    expected: 'PERMISSION_DENIED',
  },
  {
    id: 3,
    name: 'Cross-User Message Injection',
    operation: 'create',
    path: '/chats/userA_userB/messages/msg1',
    auth: { uid: 'userC', email_verified: true },
    payload: {
      id: 'msg1',
      chatId: 'userA_userB',
      senderId: 'userA',
      participantIds: ['userA', 'userB'],
      text: 'Injected',
    },
    expected: 'PERMISSION_DENIED',
  },
  {
    id: 4,
    name: 'Sender Spoofing Inside Valid Chat',
    operation: 'create',
    path: '/chats/userA_userB/messages/msg2',
    auth: { uid: 'userB', email_verified: true },
    payload: {
      id: 'msg2',
      chatId: 'userA_userB',
      senderId: 'userA',
      participantIds: ['userA', 'userB'],
      text: 'Spoofed sender',
    },
    expected: 'PERMISSION_DENIED',
  },
  {
    id: 5,
    name: 'PII Harvesting via Users List',
    operation: 'list',
    path: '/users',
    auth: { uid: 'userB', email_verified: true },
    expected: 'PERMISSION_DENIED',
  },
  {
    id: 6,
    name: 'PII Harvesting via Other User Get',
    operation: 'get',
    path: '/users/userA',
    auth: { uid: 'userB', email_verified: true },
    expected: 'PERMISSION_DENIED',
  },
  {
    id: 7,
    name: 'Email Enumeration via Lookup List',
    operation: 'list',
    path: '/email_lookup',
    auth: { uid: 'userB', email_verified: true },
    expected: 'PERMISSION_DENIED',
  },
  {
    id: 8,
    name: 'Shadow Field Injection on Chat',
    operation: 'update',
    path: '/chats/userA_userB',
    auth: { uid: 'userA', email_verified: true },
    payload: {
      lastMessageText: 'hi',
      lastMessageSenderId: 'userA',
      isAdmin: true,
    },
    expected: 'PERMISSION_DENIED',
  },
  {
    id: 9,
    name: 'Chat Hijacking / Participant Swap',
    operation: 'update',
    path: '/chats/userA_userB',
    auth: { uid: 'userA', email_verified: true },
    payload: {
      participantIds: ['userA', 'userC'],
    },
    expected: 'PERMISSION_DENIED',
  },
  {
    id: 10,
    name: 'Unbounded Participant Array',
    operation: 'create',
    path: '/chats/userA_userB',
    auth: { uid: 'userA', email_verified: true },
    payload: {
      chatId: 'userA_userB',
      participantIds: ['userA', 'userB', 'userC'],
      participantNames: {},
      participantPhotos: {},
      lastMessageText: '',
      lastMessageSenderId: '',
    },
    expected: 'PERMISSION_DENIED',
  },
  {
    id: 11,
    name: 'Oversized Resource Poisoning',
    operation: 'create',
    path: '/chats/userA_userB/messages/' + 'x'.repeat(200),
    auth: { uid: 'userA', email_verified: true },
    payload: {
      id: 'x'.repeat(200),
      chatId: 'userA_userB',
      senderId: 'userA',
      participantIds: ['userA', 'userB'],
      text: 'a'.repeat(5000),
    },
    expected: 'PERMISSION_DENIED',
  },
  {
    id: 12,
    name: 'Unverified Email Spoofing',
    operation: 'create',
    path: '/users/userA',
    auth: { uid: 'userA', email_verified: false },
    payload: {
      uid: 'userA',
      displayName: 'Attacker',
      email: 'attacker@example.com',
      photoUrl: '',
      preferredLanguage: 'ha',
    },
    expected: 'PERMISSION_DENIED',
  },
];
