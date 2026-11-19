# Security Specification — Lukuman Free Chat

## 1. Data Invariants
1. **Private User PII Isolation (`/users/{userId}`)**: Raw email addresses live exclusively in `/users/{userId}`, readable and writable only when `request.auth.uid == userId`. List queries on `/users` are unconditionally forbidden.
2. **Searchable Directory Without PII (`/user_directory/{userId}` & `/email_lookup/{emailHash}`)**: Finding users by name queries `/user_directory` (`searchable == true`, no raw email stored). Finding users by email performs a single-document `get` on `/email_lookup/{sha256(lowercase(email))}` with `allow list: if false;` to prevent user enumeration.
3. **Deterministic 1:1 Chat Ownership (`/chats/{chatId}`)**: Every chat has `participantIds` of exact length `2` (`participantIds[0] < participantIds[1]`) and `chatId == participantIds[0] + '_' + participantIds[1]`. Only those two users may read, list, create, or update that chat.
4. **Participant-Locked Real-Time Messages (`/chats/{chatId}/messages/{messageId}`)**: Messages cannot be created unless the parent `/chats/{chatId}` exists, `request.auth.uid == incoming().senderId`, and `incoming().participantIds` matches the parent chat's `participantIds`. Messages are immutable once sent (no update/delete).
5. **Verified Identity & Temporal Integrity**: All writes require `request.auth.token.email_verified == true` and server-verified timestamps (`request.time`).

## 2. The "Dirty Dozen" Attack Payloads
1. **Unauthenticated Chat Read**: Unauthenticated client attempts `get(/chats/userA_userB)` -> `PERMISSION_DENIED`.
2. **Cross-User Chat Spying**: User C (`uidC`) attempts `get(/chats/userA_userB)` or `list(/chats)` querying `userA` -> `PERMISSION_DENIED`.
3. **Cross-User Message Injection**: User C attempts `create(/chats/userA_userB/messages/msg1)` with `senderId: "userA"` -> `PERMISSION_DENIED`.
4. **Sender Spoofing Inside Valid Chat**: User B attempts `create(/chats/userA_userB/messages/msg1)` with `senderId: "userA"` -> `PERMISSION_DENIED`.
5. **PII Harvesting via Users List**: User B attempts `list(/users)` -> `PERMISSION_DENIED`.
6. **PII Harvesting via Other User Get**: User B attempts `get(/users/userA)` -> `PERMISSION_DENIED`.
7. **Email Enumeration via Lookup List**: User B attempts `list(/email_lookup)` -> `PERMISSION_DENIED`.
8. **Shadow Field Injection on Chat**: User A attempts `update(/chats/userA_userB)` adding `isAdmin: true` -> `PERMISSION_DENIED`.
9. **Chat Hijacking / Participant Swap**: User A attempts `update(/chats/userA_userB)` changing `participantIds` to `["userA", "userC"]` -> `PERMISSION_DENIED`.
10. **Unbounded Participant Array**: User A attempts `create(/chats/userA_userB)` with 3 participants `["userA", "userB", "userC"]` -> `PERMISSION_DENIED`.
11. **Oversized Resource Poisoning**: User A attempts `create(/chats/userA_userB/messages/msg1)` with a 5,000-char `text` or 500-char document ID -> `PERMISSION_DENIED`.
12. **Unverified Email Spoofing**: Authenticated user with `email_verified: false` attempts `create(/users/userA)` or `create(/chats/userA_userB/messages/msg1)` -> `PERMISSION_DENIED`.
