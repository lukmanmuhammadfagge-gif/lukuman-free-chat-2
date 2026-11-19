package com.lukuman.freechat.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lukuman.freechat.data.model.AppLanguage
import com.lukuman.freechat.data.model.UserProfile
import com.lukuman.freechat.ui.components.UserAvatar
import com.lukuman.freechat.ui.i18n.AppStrings
import com.lukuman.freechat.ui.theme.DeepBluePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    strings: AppStrings,
    user: UserProfile,
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = DeepBluePalette.MidnightNavy,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepBluePalette.DeepOcean,
                    titleContentColor = DeepBluePalette.IceInk
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                            tint = DeepBluePalette.IceInk
                        )
                    }
                },
                title = {
                    Text(
                        text = strings.profileTitle,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            UserAvatar(
                name = user.displayName,
                photoUrl = user.photoUrl,
                size = 96.dp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = user.displayName,
                style = MaterialTheme.typography.titleLarge,
                color = DeepBluePalette.IceInk,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = DeepBluePalette.SapphireSky,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                color = DeepBluePalette.AbyssSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DeepBluePalette.HairlineBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = strings.languageLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = DeepBluePalette.SteelMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepBluePalette.DeepOcean, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            val selected = lang == currentLanguage
                            Surface(
                                onClick = { onSelectLanguage(lang) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) DeepBluePalette.RoyalCobalt else DeepBluePalette.DeepOcean,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = lang.displayLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) DeepBluePalette.IceInk else DeepBluePalette.SteelMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = strings.securityNote,
                style = MaterialTheme.typography.bodyMedium,
                color = DeepBluePalette.SteelMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onSignOut,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepBluePalette.ElevatedSlateBlue,
                    contentColor = DeepBluePalette.ErrorRed
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = strings.signOutLabel,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
