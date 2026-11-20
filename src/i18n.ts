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
  attachImageLabel: string;
  photoLabel: string;
  photoPreviewLabel: string;
  photoViewerTitle: string;
  compressingImageLabel: string;
  imageErrorLabel: string;
  removeImageLabel: string;
  phoneLabel: string;
  phonePlaceholder: string;
  phonePrivacyHint: string;
  phoneSaveLabel: string;
  phoneSavingLabel: string;
  phoneRemoveLabel: string;
  phoneSavedStatus: string;
  phoneRemovedStatus: string;
  phoneInvalidError: string;
  phoneAlreadyTakenError: string;
  typingIndicatorLabel: string;
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
    searchPlaceholder: 'Nemo da suna, imel, ko lamba (+234)...',
    searchButton: 'Nema',
    searchPrompt: 'Shigar da suna (ko wani ɓangare), imel, ko cikakkiyar lambar waya (+234) don fara hira.',
    noChatsTitle: 'Babu tattaunawa tukuna',
    noChatsBody: 'Nemo wani da suna, imel, ko lambar waya don fara sabuwar tattaunawa.',
    startNewChat: 'Fara Hira',
    noUsersFound: 'Ba a sami kowa mai wannan suna, imel, ko lambar waya ba.',
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
    attachImageLabel: 'Zaɓi Hoto',
    photoLabel: 'Hoto',
    photoPreviewLabel: 'Hoto a shirye don aikawa',
    photoViewerTitle: 'Duba Hoto',
    compressingImageLabel: 'Ana shirya hoto...',
    imageErrorLabel: 'Ba a iya sarrafa wannan hoton ba.',
    removeImageLabel: 'Cire hoto',
    phoneLabel: 'Lambar Waya (Na Zaɓi, +234)',
    phonePlaceholder: '+234 801 234 5678',
    phonePrivacyHint:
      'Ana adana SHA-256 hash ne kawai don bincike; ba a taɓa adana ainihin lambar ba.',
    phoneSaveLabel: 'Adana Lamba',
    phoneSavingLabel: 'Ana adanawa...',
    phoneRemoveLabel: 'Cire',
    phoneSavedStatus: 'An adana hash na lambar Najeriya (+234) don bincike.',
    phoneRemovedStatus: 'An cire lambar waya daga bincike.',
    phoneInvalidError:
      'Shigar da ingantacciyar lambar Najeriya, misali +2348012345678 ko 08012345678.',
    phoneAlreadyTakenError: 'Wannan lambar wayar tana haɗe da wani asusu.',
    typingIndicatorLabel: 'yana rubutu...',
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
    searchPlaceholder: 'Search name, email, or +234 phone...',
    searchButton: 'Search',
    searchPrompt:
      'Enter a name (even partial), full email, or +234 phone number to start a chat.',
    noChatsTitle: 'No conversations yet',
    noChatsBody: 'Find someone by name, email, or phone number to start a one-to-one chat.',
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
    attachImageLabel: 'Pick Photo',
    photoLabel: 'Photo',
    photoPreviewLabel: 'Photo ready to send',
    photoViewerTitle: 'Photo',
    compressingImageLabel: 'Preparing photo...',
    imageErrorLabel: 'Could not process this image.',
    removeImageLabel: 'Remove photo',
    phoneLabel: 'Phone Number (Optional, +234)',
    phonePlaceholder: '+234 801 234 5678',
    phonePrivacyHint:
      'Only a SHA-256 hash is stored for search; your raw number is never saved.',
    phoneSaveLabel: 'Save Number',
    phoneSavingLabel: 'Saving...',
    phoneRemoveLabel: 'Remove',
    phoneSavedStatus: 'Nigerian (+234) phone hash saved for search.',
    phoneRemovedStatus: 'Phone number removed from search.',
    phoneInvalidError: 'Enter a valid Nigerian number, e.g. +2348012345678 or 08012345678.',
    phoneAlreadyTakenError: 'This phone number is already linked to another account.',
    typingIndicatorLabel: 'typing...',
  },
};
