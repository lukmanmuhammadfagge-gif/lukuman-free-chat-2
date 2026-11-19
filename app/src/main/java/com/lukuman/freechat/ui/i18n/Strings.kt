package com.lukuman.freechat.ui.i18n

import com.lukuman.freechat.data.model.AppLanguage

data class AppStrings(
    val appName: String,
    val loginTagline: String,
    val loginSubtext: String,
    val signInWithGoogle: String,
    val signingIn: String,
    val languageLabel: String,
    val hausaLabel: String,
    val englishLabel: String,
    val creditText: String,
    val missingGoogleServicesHint: String,
    val chatsTitle: String,
    val findUsersTitle: String,
    val profileTitle: String,
    val searchPlaceholder: String,
    val searchPrompt: String,
    val noChatsTitle: String,
    val noChatsBody: String,
    val startNewChat: String,
    val noUsersFound: String,
    val youPrefix: String,
    val newChatPlaceholder: String,
    val messageHint: String,
    val sendLabel: String,
    val emptyMessages: String,
    val emailLabel: String,
    val nameLabel: String,
    val signOutLabel: String,
    val securityNote: String,
    val yesterdayLabel: String,
    val justNowLabel: String
)

object BilingualStrings {
    val Hausa = AppStrings(
        appName = "Lukuman Free Chat",
        loginTagline = "Saƙonni cikin sauri, tsaro, da sauƙi.",
        loginSubtext = "Tattaunawa ta mutum-da-mutum kai tsaye ta amfani da asusun Google.",
        signInWithGoogle = "Shiga da Google",
        signingIn = "Ana shiga...",
        languageLabel = "Zaɓi Harshe",
        hausaLabel = "Hausa",
        englishLabel = "English",
        creditText = "Created by Lukuman Muhammad Abubakar Fagge",
        missingGoogleServicesHint = "Sanya google-services.json a cikin app/ don haɗawa da Firebase.",
        chatsTitle = "Hirarraki",
        findUsersTitle = "Nemo Abokan Hira",
        profileTitle = "Bayanan Asusu",
        searchPlaceholder = "Nemo da suna ko adireshin imel...",
        searchPrompt = "Shigar da suna ko cikakken imel don fara sabuwar hira.",
        noChatsTitle = "Babu tattaunawa tukuna",
        noChatsBody = "Danna maɓallin ƙasa don nemo wani da suna ko imel ku fara magana.",
        startNewChat = "Fara Hira",
        noUsersFound = "Ba a sami kowa mai wannan suna ko imel ba.",
        youPrefix = "Kai: ",
        newChatPlaceholder = "Fara sabuwar tattaunawa",
        messageHint = "Rubuta saƙo...",
        sendLabel = "Aika",
        emptyMessages = "Babu saƙo tukuna. Aika gaisuwa ta farko!",
        emailLabel = "Adireshin Imel",
        nameLabel = "Cikakken Suna",
        signOutLabel = "Fita Daga Asusu",
        securityNote = "Hirarrakinka suna kare ne kawai tsakanin kai da wanda kuke magana.",
        yesterdayLabel = "Jiya",
        justNowLabel = "Yanzu"
    )

    val English = AppStrings(
        appName = "Lukuman Free Chat",
        loginTagline = "Fast, private, direct messaging.",
        loginSubtext = "Real-time one-to-one text conversations secured with your Google account.",
        signInWithGoogle = "Sign in with Google",
        signingIn = "Signing in...",
        languageLabel = "Select Language",
        hausaLabel = "Hausa",
        englishLabel = "English",
        creditText = "Created by Lukuman Muhammad Abubakar Fagge",
        missingGoogleServicesHint = "Place google-services.json inside app/ to connect your Firebase project.",
        chatsTitle = "Chats",
        findUsersTitle = "Find Users",
        profileTitle = "Profile",
        searchPlaceholder = "Search by name or email...",
        searchPrompt = "Enter a display name or full email address to start a chat.",
        noChatsTitle = "No conversations yet",
        noChatsBody = "Tap the button below to find someone by name or email and start chatting.",
        startNewChat = "Start Chat",
        noUsersFound = "No matching users found.",
        youPrefix = "You: ",
        newChatPlaceholder = "Start a conversation",
        messageHint = "Write a message...",
        sendLabel = "Send",
        emptyMessages = "No messages yet. Say hello!",
        emailLabel = "Email Address",
        nameLabel = "Display Name",
        signOutLabel = "Sign Out",
        securityNote = "Your chats are strictly private between you and your chat partner.",
        yesterdayLabel = "Yesterday",
        justNowLabel = "Just now"
    )

    fun forLanguage(language: AppLanguage): AppStrings {
        return when (language) {
            AppLanguage.HAUSA -> Hausa
            AppLanguage.ENGLISH -> English
        }
    }
}
