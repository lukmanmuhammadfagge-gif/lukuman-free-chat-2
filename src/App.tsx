import React, { useEffect, useMemo, useRef, useState } from 'react';
import { onAuthStateChanged, signInWithPopup, signOut } from 'firebase/auth';
import {
  ArrowLeft,
  Code2,
  LogOut,
  MessageSquare,
  Search,
  Send,
  UserPlus,
  X,
} from 'lucide-react';
import { auth, googleProvider } from './firebase';
import { AppLanguage, STRINGS } from './i18n';
import {
  ChatMessage,
  ChatThread,
  DirectoryUser,
  loadUserProfile,
  openOrCreateChat,
  searchUsers,
  sendMessage,
  subscribeChatMessages,
  subscribeUserChats,
  syncUserProfile,
  updatePreferredLanguage,
  UserProfile,
} from './services/chatService';
import { BrandMark } from './components/BrandMark';
import { UserAvatar } from './components/UserAvatar';
import { AndroidProjectExplorer } from './components/AndroidProjectExplorer';

type WorkspaceMode = 'chat' | 'android';

export function App() {
  const [language, setLanguage] = useState<AppLanguage>('ha');
  const [initializing, setInitializing] = useState(true);
  const [signingIn, setSigningIn] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(null);

  const [mode, setMode] = useState<WorkspaceMode>('chat');
  const [chats, setChats] = useState<ChatThread[]>([]);
  const [activeChatId, setActiveChatId] = useState<string | null>(null);
  const [activePeer, setActivePeer] = useState<{ name: string; photoUrl: string } | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [draft, setDraft] = useState('');

  const [showFindUsers, setShowFindUsers] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<DirectoryUser[]>([]);
  const [searching, setSearching] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  const [showProfileModal, setShowProfileModal] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const strings = STRINGS[language];

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (firebaseUser) => {
      if (!firebaseUser) {
        setCurrentUser(null);
        setChats([]);
        setActiveChatId(null);
        setMessages([]);
        setInitializing(false);
        return;
      }

      try {
        let profile = await loadUserProfile(firebaseUser.uid);
        if (!profile) {
          profile = await syncUserProfile(firebaseUser, language);
        }
        setCurrentUser(profile);
        setLanguage(profile.preferredLanguage);
      } catch (error) {
        setAuthError(error instanceof Error ? error.message : 'Unable to load profile');
      } finally {
        setInitializing(false);
      }
    });

    return () => unsubscribe();
  }, []);

  useEffect(() => {
    if (!currentUser) return;
    const unsubscribe = subscribeUserChats(currentUser.uid, (nextChats) => {
      setChats(nextChats);
    });
    return () => unsubscribe();
  }, [currentUser?.uid]);

  useEffect(() => {
    if (!currentUser || !activeChatId) {
      setMessages([]);
      return;
    }
    const unsubscribe = subscribeChatMessages(activeChatId, currentUser.uid, (nextMessages) => {
      setMessages(nextMessages);
    });
    return () => unsubscribe();
  }, [currentUser?.uid, activeChatId]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages.length]);

  const handleLanguageChange = async (nextLang: AppLanguage) => {
    setLanguage(nextLang);
    if (currentUser) {
      setCurrentUser({ ...currentUser, preferredLanguage: nextLang });
      await updatePreferredLanguage(currentUser.uid, nextLang);
    }
  };

  const handleGoogleSignIn = async () => {
    setSigningIn(true);
    setAuthError(null);
    try {
      const cred = await signInWithPopup(auth, googleProvider);
      const profile = await syncUserProfile(cred.user, language);
      setCurrentUser(profile);
      setLanguage(profile.preferredLanguage);
    } catch (error) {
      setAuthError(error instanceof Error ? error.message : 'Google Sign-In failed');
    } finally {
      setSigningIn(false);
    }
  };

  const handleSignOut = async () => {
    setShowProfileModal(false);
    await signOut(auth);
  };

  const handleRunSearch = async (queryText = searchQuery) => {
    if (!currentUser) return;
    setSearching(true);
    try {
      const results = await searchUsers(queryText, currentUser.uid);
      setSearchResults(results);
      setHasSearched(true);
    } finally {
      setSearching(false);
    }
  };

  const handleOpenFindUsers = async () => {
    setShowFindUsers(true);
    await handleRunSearch('');
  };

  const handleStartChat = async (user: DirectoryUser) => {
    if (!currentUser) return;
    const chatId = await openOrCreateChat(currentUser, user);
    setActivePeer({ name: user.displayName, photoUrl: user.photoUrl });
    setActiveChatId(chatId);
    setShowFindUsers(false);
    setMode('chat');
  };

  const activeThread = useMemo(
    () => chats.find((c) => c.chatId === activeChatId) || null,
    [chats, activeChatId]
  );

  const resolvedPeer = useMemo(() => {
    if (!currentUser) return { name: '', photoUrl: '' };
    if (activeThread) {
      const peerId =
        activeThread.participantIds.find((id) => id !== currentUser.uid) || currentUser.uid;
      return {
        name: activeThread.participantNames[peerId] || activePeer?.name || 'User',
        photoUrl: activeThread.participantPhotos[peerId] || activePeer?.photoUrl || '',
      };
    }
    return activePeer || { name: '', photoUrl: '' };
  }, [activeThread, activePeer, currentUser]);

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentUser || !activeChatId) return;
    const clean = draft.trim();
    if (!clean) return;
    const participants = activeThread?.participantIds || activeChatId.split('_');
    setDraft('');
    await sendMessage(activeChatId, currentUser.uid, participants, clean);
  };

  const formatTime = (millis: number): string => {
    if (!millis) return '';
    const date = new Date(millis);
    const now = new Date();
    const sameDay =
      date.getFullYear() === now.getFullYear() &&
      date.getMonth() === now.getMonth() &&
      date.getDate() === now.getDate();
    if (sameDay) {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
    const yesterday = new Date(now);
    yesterday.setDate(now.getDate() - 1);
    const isYesterday =
      date.getFullYear() === yesterday.getFullYear() &&
      date.getMonth() === yesterday.getMonth() &&
      date.getDate() === yesterday.getDate();
    if (isYesterday) return strings.yesterdayLabel;
    return date.toLocaleDateString([], { day: '2-digit', month: '2-digit' });
  };

  if (initializing) {
    return (
      <div className="min-h-screen bg-[#071126] flex items-center justify-center">
        <div className="w-9 h-9 rounded-full border-2 border-[#38BDF8] border-t-transparent animate-spin" />
      </div>
    );
  }

  if (!currentUser) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-[#0C1B3B] via-[#071126] to-[#071126] text-[#F0F6FF] flex flex-col">
        <header className="max-w-7xl w-full mx-auto px-4 sm:px-6 py-4 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <BrandMark size={36} />
            <span className="font-bold text-base tracking-tight whitespace-nowrap">
              {strings.appName}
            </span>
          </div>

          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setMode(mode === 'chat' ? 'android' : 'chat')}
              className="px-3.5 py-2 rounded-xl bg-[#122650] hover:bg-[#173166] border border-[#213C73] text-xs font-medium text-[#F0F6FF] transition-colors inline-flex items-center gap-2 whitespace-nowrap shrink-0 cursor-pointer"
            >
              <Code2 className="w-3.5 h-3.5 text-[#38BDF8]" />
              <span>{mode === 'chat' ? strings.androidCodeTab : strings.liveChatTab}</span>
            </button>

            <div className="flex items-center bg-[#122650] p-1 rounded-xl border border-[#213C73]">
              {(['ha', 'en'] as const).map((lang) => (
                <button
                  key={lang}
                  type="button"
                  onClick={() => handleLanguageChange(lang)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                    language === lang
                      ? 'bg-[#1D4ED8] text-[#F0F6FF]'
                      : 'text-[#96ACD0] hover:text-[#F0F6FF]'
                  }`}
                >
                  {lang === 'ha' ? 'Hausa' : 'English'}
                </button>
              ))}
            </div>
          </div>
        </header>

        {mode === 'android' ? (
          <main className="flex-1">
            <AndroidProjectExplorer strings={strings} />
          </main>
        ) : (
          <main className="flex-1 flex flex-col items-center justify-center px-6 py-12">
            <div className="max-w-md w-full bg-[#0C1B3B] border border-[#213C73] rounded-3xl p-8 text-center space-y-6">
              <div className="flex justify-center">
                <BrandMark size={80} />
              </div>

              <div className="space-y-2.5">
                <h1 className="text-3xl font-bold tracking-tight text-[#F0F6FF]">
                  {strings.appName}
                </h1>
                <p className="text-base font-medium text-[#38BDF8]">{strings.loginTagline}</p>
                <p className="text-sm text-[#96ACD0] leading-relaxed">{strings.loginSubtext}</p>
              </div>

              <button
                type="button"
                onClick={handleGoogleSignIn}
                disabled={signingIn}
                className="w-full h-13 rounded-2xl bg-[#1D4ED8] hover:bg-[#2563EB] disabled:opacity-60 text-[#F0F6FF] font-semibold text-sm flex items-center justify-center gap-3 transition-colors cursor-pointer"
              >
                <span>{signingIn ? strings.signingIn : strings.signInWithGoogle}</span>
              </button>

              {authError && (
                <div className="p-3 rounded-xl bg-[#122650] border border-red-400/40 text-xs text-red-300">
                  {authError}
                </div>
              )}

              <p className="pt-2 text-xs text-[#96ACD0] border-t border-[#213C73]/70">
                {strings.creditText}
              </p>
            </div>
          </main>
        )}
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#071126] text-[#F0F6FF] flex flex-col">
      <header className="h-16 border-b border-[#213C73] bg-[#0C1B3B] px-4 sm:px-6 flex items-center justify-between gap-8 shrink-0">
        <div className="flex items-center gap-3 whitespace-nowrap shrink-0">
          <BrandMark size={32} />
          <span className="text-base font-bold tracking-tight text-[#F0F6FF]">
            {strings.appName}
          </span>
        </div>

        <nav className="flex items-center gap-6 text-sm font-medium">
          <button
            type="button"
            onClick={() => setMode('chat')}
            className={`whitespace-nowrap shrink-0 py-1 border-b-2 transition-colors cursor-pointer ${
              mode === 'chat'
                ? 'border-[#38BDF8] text-[#F0F6FF]'
                : 'border-transparent text-[#96ACD0] hover:text-[#F0F6FF]'
            }`}
          >
            {strings.liveChatTab}
          </button>
          <button
            type="button"
            onClick={() => setMode('android')}
            className={`whitespace-nowrap shrink-0 py-1 border-b-2 transition-colors cursor-pointer ${
              mode === 'android'
                ? 'border-[#38BDF8] text-[#F0F6FF]'
                : 'border-transparent text-[#96ACD0] hover:text-[#F0F6FF]'
            }`}
          >
            {strings.androidCodeTab}
          </button>
        </nav>

        <button
          type="button"
          onClick={() => setShowProfileModal(true)}
          className="flex items-center gap-2.5 px-2 py-1.5 rounded-xl hover:bg-[#122650] transition-colors shrink-0 cursor-pointer"
        >
          <UserAvatar
            name={currentUser.displayName}
            photoUrl={currentUser.photoUrl}
            sizeClass="w-8 h-8 text-xs"
          />
          <span className="hidden sm:inline text-sm font-medium text-[#F0F6FF] max-w-[140px] truncate">
            {currentUser.displayName}
          </span>
        </button>
      </header>

      {mode === 'android' ? (
        <main className="flex-1 overflow-y-auto">
          <AndroidProjectExplorer strings={strings} />
        </main>
      ) : (
        <main className="flex-1 max-w-7xl w-full mx-auto grid grid-cols-1 md:grid-cols-12 h-[calc(100vh-4rem)]">
          {/* Left Sidebar: Chat List or Find Users */}
          <aside
            className={`md:col-span-4 lg:col-span-4 border-r border-[#213C73] bg-[#0C1B3B] flex flex-col min-h-0 ${
              activeChatId ? 'hidden md:flex' : 'flex'
            }`}
          >
            <div className="p-4 border-b border-[#213C73] flex items-center justify-between gap-3">
              <h2 className="text-base font-semibold text-[#F0F6FF]">
                {showFindUsers ? strings.findUsersTitle : strings.chatsTitle}
              </h2>
              {showFindUsers ? (
                <button
                  type="button"
                  onClick={() => setShowFindUsers(false)}
                  className="p-2 rounded-xl bg-[#122650] hover:bg-[#173166] text-[#96ACD0] hover:text-[#F0F6FF] transition-colors cursor-pointer"
                  aria-label={strings.closeLabel}
                >
                  <X className="w-4 h-4" />
                </button>
              ) : (
                <button
                  type="button"
                  onClick={handleOpenFindUsers}
                  className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-[#1D4ED8] hover:bg-[#2563EB] text-xs font-semibold text-[#F0F6FF] transition-colors whitespace-nowrap shrink-0 cursor-pointer"
                >
                  <UserPlus className="w-3.5 h-3.5" />
                  <span>{strings.startNewChat}</span>
                </button>
              )}
            </div>

            {showFindUsers ? (
              <div className="flex-1 flex flex-col min-h-0">
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    void handleRunSearch();
                  }}
                  className="p-3 border-b border-[#213C73] flex gap-2"
                >
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder={strings.searchPlaceholder}
                    className="flex-1 bg-[#122650] border border-[#213C73] focus:border-[#38BDF8] rounded-xl px-3.5 py-2.5 text-sm text-[#F0F6FF] placeholder-[#96ACD0] outline-none"
                  />
                  <button
                    type="submit"
                    className="px-3.5 rounded-xl bg-[#1D4ED8] hover:bg-[#2563EB] text-[#F0F6FF] flex items-center justify-center cursor-pointer"
                    aria-label={strings.searchButton}
                  >
                    <Search className="w-4 h-4" />
                  </button>
                </form>

                <div className="flex-1 overflow-y-auto divide-y divide-[#173166]">
                  {searching ? (
                    <div className="p-8 flex justify-center">
                      <div className="w-6 h-6 rounded-full border-2 border-[#38BDF8] border-t-transparent animate-spin" />
                    </div>
                  ) : searchResults.length === 0 ? (
                    <p className="p-6 text-sm text-[#96ACD0] text-center">
                      {hasSearched && searchQuery.trim()
                        ? strings.noUsersFound
                        : strings.searchPrompt}
                    </p>
                  ) : (
                    searchResults.map((user) => (
                      <button
                        key={user.uid}
                        type="button"
                        onClick={() => handleStartChat(user)}
                        className="w-full px-4 py-3.5 flex items-center gap-3 hover:bg-[#122650] transition-colors text-left cursor-pointer"
                      >
                        <UserAvatar name={user.displayName} photoUrl={user.photoUrl} />
                        <div className="flex-1 min-w-0">
                          <div className="text-sm font-semibold text-[#F0F6FF] truncate">
                            {user.displayName}
                          </div>
                          <div className="text-xs text-[#38BDF8]">{strings.startNewChat}</div>
                        </div>
                      </button>
                    ))
                  )}
                </div>
              </div>
            ) : (
              <div className="flex-1 overflow-y-auto divide-y divide-[#173166]">
                {chats.length === 0 ? (
                  <div className="p-8 text-center space-y-3">
                    <div className="w-12 h-12 mx-auto rounded-2xl bg-[#122650] flex items-center justify-center text-[#38BDF8]">
                      <MessageSquare className="w-6 h-6" />
                    </div>
                    <h3 className="text-sm font-semibold text-[#F0F6FF]">
                      {strings.noChatsTitle}
                    </h3>
                    <p className="text-xs text-[#96ACD0] leading-relaxed">
                      {strings.noChatsBody}
                    </p>
                  </div>
                ) : (
                  chats.map((thread) => {
                    const peerId =
                      thread.participantIds.find((id) => id !== currentUser.uid) ||
                      currentUser.uid;
                    const peerName = thread.participantNames[peerId] || 'User';
                    const peerPhoto = thread.participantPhotos[peerId] || '';
                    const isSelected = thread.chatId === activeChatId;
                    const preview = !thread.lastMessageText
                      ? strings.newChatPlaceholder
                      : thread.lastMessageSenderId === currentUser.uid
                      ? `${strings.youPrefix}${thread.lastMessageText}`
                      : thread.lastMessageText;

                    return (
                      <button
                        key={thread.chatId}
                        type="button"
                        onClick={() => {
                          setActivePeer({ name: peerName, photoUrl: peerPhoto });
                          setActiveChatId(thread.chatId);
                        }}
                        className={`w-full px-4 py-3.5 flex items-center gap-3 text-left transition-colors cursor-pointer ${
                          isSelected ? 'bg-[#173166]' : 'hover:bg-[#122650]'
                        }`}
                      >
                        <UserAvatar name={peerName} photoUrl={peerPhoto} />
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center justify-between gap-2">
                            <span className="text-sm font-semibold text-[#F0F6FF] truncate">
                              {peerName}
                            </span>
                            <span className="text-xs text-[#38BDF8] tabular-nums shrink-0">
                              {formatTime(thread.lastMessageAtMillis)}
                            </span>
                          </div>
                          <p className="text-xs text-[#96ACD0] truncate mt-0.5">{preview}</p>
                        </div>
                      </button>
                    );
                  })
                )}
              </div>
            )}
          </aside>

          {/* Right Pane: Real-time 1:1 Chat */}
          <section
            className={`md:col-span-8 lg:col-span-8 bg-[#071126] flex flex-col min-h-0 ${
              activeChatId ? 'flex' : 'hidden md:flex'
            }`}
          >
            {!activeChatId ? (
              <div className="flex-1 flex flex-col items-center justify-center p-8 text-center space-y-4">
                <BrandMark size={64} />
                <p className="text-sm text-[#96ACD0] max-w-sm">{strings.selectChatPrompt}</p>
              </div>
            ) : (
              <>
                <div className="h-16 px-4 border-b border-[#213C73] bg-[#0C1B3B] flex items-center gap-3 shrink-0">
                  <button
                    type="button"
                    onClick={() => setActiveChatId(null)}
                    className="md:hidden p-2 rounded-xl hover:bg-[#122650] text-[#F0F6FF] cursor-pointer"
                  >
                    <ArrowLeft className="w-5 h-5" />
                  </button>
                  <UserAvatar
                    name={resolvedPeer.name || 'User'}
                    photoUrl={resolvedPeer.photoUrl}
                    sizeClass="w-10 h-10 text-xs"
                  />
                  <span className="font-semibold text-sm text-[#F0F6FF] truncate">
                    {resolvedPeer.name}
                  </span>
                </div>

                <div className="flex-1 overflow-y-auto px-4 sm:px-6 py-4 space-y-3">
                  {messages.length === 0 ? (
                    <div className="h-full flex items-center justify-center">
                      <p className="text-sm text-[#96ACD0]">{strings.emptyMessages}</p>
                    </div>
                  ) : (
                    messages.map((msg) => {
                      const isMine = msg.senderId === currentUser.uid;
                      return (
                        <div
                          key={msg.id}
                          className={`flex ${isMine ? 'justify-end' : 'justify-start'}`}
                        >
                          <div
                            className={`max-w-[78%] sm:max-w-md px-4 py-2.5 rounded-2xl ${
                              isMine
                                ? 'bg-[#1E40AF] text-[#F0F6FF] rounded-br-sm'
                                : 'bg-[#132954] text-[#F0F6FF] rounded-bl-sm'
                            }`}
                          >
                            <p className="text-sm leading-relaxed whitespace-pre-wrap break-words">
                              {msg.text}
                            </p>
                            <div
                              className={`text-[11px] mt-1 text-right tabular-nums ${
                                isMine ? 'text-[#38BDF8]' : 'text-[#96ACD0]'
                              }`}
                            >
                              {formatTime(msg.createdAtMillis)}
                            </div>
                          </div>
                        </div>
                      );
                    })
                  )}
                  <div ref={messagesEndRef} />
                </div>

                <form
                  onSubmit={handleSendMessage}
                  className="p-3 sm:p-4 border-t border-[#213C73] bg-[#0C1B3B] flex items-center gap-3 shrink-0"
                >
                  <input
                    type="text"
                    value={draft}
                    maxLength={2000}
                    onChange={(e) => setDraft(e.target.value)}
                    placeholder={strings.messageHint}
                    className="flex-1 bg-[#122650] border border-[#213C73] focus:border-[#38BDF8] rounded-2xl px-4 py-3 text-sm text-[#F0F6FF] placeholder-[#96ACD0] outline-none"
                  />
                  <button
                    type="submit"
                    disabled={!draft.trim()}
                    className="h-11 w-11 rounded-2xl bg-[#1D4ED8] hover:bg-[#2563EB] disabled:opacity-50 text-[#F0F6FF] flex items-center justify-center transition-colors shrink-0 cursor-pointer"
                    aria-label={strings.sendLabel}
                  >
                    <Send className="w-4 h-4" />
                  </button>
                </form>
              </>
            )}
          </section>
        </main>
      )}

      {/* Profile Modal */}
      {showProfileModal && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="max-w-md w-full bg-[#0C1B3B] border border-[#213C73] rounded-3xl p-6 space-y-6">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-[#F0F6FF]">{strings.profileTitle}</h2>
              <button
                type="button"
                onClick={() => setShowProfileModal(false)}
                className="p-2 rounded-xl hover:bg-[#122650] text-[#96ACD0] hover:text-[#F0F6FF] cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="flex flex-col items-center text-center space-y-3">
              <UserAvatar
                name={currentUser.displayName}
                photoUrl={currentUser.photoUrl}
                sizeClass="w-20 h-20 text-xl"
              />
              <div>
                <div className="text-lg font-bold text-[#F0F6FF]">{currentUser.displayName}</div>
                <div className="text-sm text-[#38BDF8]">{currentUser.email}</div>
              </div>
            </div>

            <div className="bg-[#122650] border border-[#213C73] rounded-2xl p-4 space-y-2.5">
              <div className="text-xs font-medium text-[#96ACD0]">{strings.languageLabel}</div>
              <div className="grid grid-cols-2 gap-2 bg-[#0C1B3B] p-1 rounded-xl">
                {(['ha', 'en'] as const).map((lang) => (
                  <button
                    key={lang}
                    type="button"
                    onClick={() => handleLanguageChange(lang)}
                    className={`py-2 rounded-lg text-sm font-semibold transition-colors cursor-pointer ${
                      language === lang
                        ? 'bg-[#1D4ED8] text-[#F0F6FF]'
                        : 'text-[#96ACD0] hover:text-[#F0F6FF]'
                    }`}
                  >
                    {lang === 'ha' ? 'Hausa' : 'English'}
                  </button>
                ))}
              </div>
            </div>

            <p className="text-xs text-[#96ACD0] text-center">{strings.securityNote}</p>

            <button
              type="button"
              onClick={handleSignOut}
              className="w-full py-3 rounded-xl bg-[#173166] hover:bg-[#1E3B7A] text-sm font-semibold text-red-300 flex items-center justify-center gap-2 transition-colors cursor-pointer"
            >
              <LogOut className="w-4 h-4" />
              <span>{strings.signOutLabel}</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
