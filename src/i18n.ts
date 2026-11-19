export type AppLanguage = 'ha' | 'en';

export interface WebStrings {
  appName: string;
  loginTagline: string;
  loginSubtext: string;
  signInWithGoogle: string;
  signingIn: string;
  creditText: string;
  chatsTitle: string;
  findUsersTitle: string;
  profileTitle: string;
  androidProjectTitle: string;
  liveChatTab: string;
  androidCodeTab: string;
  searchPlaceholder: string;
  searchButton: string;
  searchPrompt: string;
  noChatsTitle: string;
  noChatsBody: string;
  startNewChat: string;
  noUsersFound: string;
  youPrefix: string;
  newChatPlaceholder: string;
  messageHint: string;
  sendLabel: string;
  emptyMessages: string;
  selectChatPrompt: string;
  emailLabel: string;
  nameLabel: string;
  languageLabel: string;
  signOutLabel: string;
  securityNote: string;
  yesterdayLabel: string;
  downloadZip: string;
  downloadingZip: string;
  closeLabel: string;
}

export const STRINGS: Record<AppLanguage, WebStrings> = {
  ha: {
    appName: 'Lukuman Free Chat',
    loginTagline: 'Saƙonni cikin sauri, tsaro, da sauƙi.',
    loginSubtext:
      'Tattaunawa ta mutum-da-mutum kai tsaye ta amfani da asusun Google da tsarin tsaro na Firestore.',
    signInWithGoogle: 'Shiga da Google',
    signingIn: 'Ana shiga...',
    creditText: 'Created by Lukuman Muhammad Abubakar Fagge',
    chatsTitle: 'Hirarraki',
    findUsersTitle: 'Nemo Abokan Hira',
    profileTitle: 'Bayanan Asusu',
    androidProjectTitle: 'Aikin Android (Kotlin & Compose)',
    liveChatTab: 'Hira Kai Tsaye',
    androidCodeTab: 'Lambar Android',
    searchPlaceholder: 'Nemo da suna ko adireshin imel...',
    searchButton: 'Nema',
    searchPrompt: 'Shigar da suna ko cikakken imel don fara sabuwar hira.',
    noChatsTitle: 'Babu tattaunawa tukuna',
    noChatsBody: 'Nemo wani da suna ko imel don fara sabuwar tattaunawa.',
    startNewChat: 'Fara Hira',
    noUsersFound: 'Ba a sami kowa mai wannan suna ko imel ba.',
    youPrefix: 'Kai: ',
    newChatPlaceholder: 'Fara sabuwar tattaunawa',
    messageHint: 'Rubuta saƙo...',
    sendLabel: 'Aika',
    emptyMessages: 'Babu saƙo tukuna. Aika gaisuwa ta farko!',
    selectChatPrompt: 'Zaɓi tattaunawa daga gefe ko nemo abokin hira.',
    emailLabel: 'Adireshin Imel',
    nameLabel: 'Cikakken Suna',
    languageLabel: 'Harshe',
    signOutLabel: 'Fita Daga Asusu',
    securityNote: 'Hirarrakinka suna kare ne kawai tsakanin kai da wanda kuke magana.',
    yesterdayLabel: 'Jiya',
    downloadZip: 'Sauke Aikin Android (.zip)',
    downloadingZip: 'Ana shirya ZIP...',
    closeLabel: 'Rufe',
  },
  en: {
    appName: 'Lukuman Free Chat',
    loginTagline: 'Fast, private, direct messaging.',
    loginSubtext:
      'Real-time one-to-one text conversations secured with your Google account and strict Firestore rules.',
    signInWithGoogle: 'Sign in with Google',
    signingIn: 'Signing in...',
    creditText: 'Created by Lukuman Muhammad Abubakar Fagge',
    chatsTitle: 'Chats',
    findUsersTitle: 'Find Users',
    profileTitle: 'Profile',
    androidProjectTitle: 'Android Project (Kotlin & Compose)',
    liveChatTab: 'Live Chat',
    androidCodeTab: 'Android Source',
    searchPlaceholder: 'Search by name or email...',
    searchButton: 'Search',
    searchPrompt: 'Enter a display name or full email address to start a chat.',
    noChatsTitle: 'No conversations yet',
    noChatsBody: 'Find someone by name or email to start a one-to-one chat.',
    startNewChat: 'Start Chat',
    noUsersFound: 'No matching users found.',
    youPrefix: 'You: ',
    newChatPlaceholder: 'Start a conversation',
    messageHint: 'Write a message...',
    sendLabel: 'Send',
    emptyMessages: 'No messages yet. Say hello!',
    selectChatPrompt: 'Select a conversation or find a user to begin chatting.',
    emailLabel: 'Email Address',
    nameLabel: 'Display Name',
    languageLabel: 'Language',
    signOutLabel: 'Sign Out',
    securityNote: 'Your chats are strictly private between you and your chat partner.',
    yesterdayLabel: 'Yesterday',
    downloadZip: 'Download Android Project (.zip)',
    downloadingZip: 'Preparing ZIP...',
    closeLabel: 'Close',
  },
};
