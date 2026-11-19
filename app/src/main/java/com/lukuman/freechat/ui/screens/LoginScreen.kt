package com.lukuman.freechat.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lukuman.freechat.data.model.AppLanguage
import com.lukuman.freechat.ui.components.BrandMark
import com.lukuman.freechat.ui.i18n.AppStrings
import com.lukuman.freechat.ui.theme.DeepBluePalette

@Composable
fun LoginScreen(
    strings: AppStrings,
    currentLanguage: AppLanguage,
    isSigningIn: Boolean,
    errorMessage: String?,
    onSelectLanguage: (AppLanguage) -> Unit,
    onGoogleSignInClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepBluePalette.DeepOcean,
                        DeepBluePalette.MidnightNavy,
                        DeepBluePalette.MidnightNavy
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Language switch on the first screen (Hausa default)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(DeepBluePalette.AbyssSurface, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AppLanguage.entries.forEach { lang ->
                val selected = lang == currentLanguage
                Surface(
                    onClick = { onSelectLanguage(lang) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) DeepBluePalette.RoyalCobalt else DeepBluePalette.AbyssSurface,
                    border = if (selected) null else BorderStroke(0.dp, DeepBluePalette.AbyssSurface)
                ) {
                    Text(
                        text = lang.displayLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) DeepBluePalette.IceInk else DeepBluePalette.SteelMuted,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandMark(size = 88.dp)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = strings.appName,
                style = MaterialTheme.typography.headlineLarge,
                color = DeepBluePalette.IceInk,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = strings.loginTagline,
                style = MaterialTheme.typography.titleMedium,
                color = DeepBluePalette.SapphireSky,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = strings.loginSubtext,
                style = MaterialTheme.typography.bodyMedium,
                color = DeepBluePalette.SteelMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = onGoogleSignInClick,
                enabled = !isSigningIn,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepBluePalette.RoyalCobalt,
                    contentColor = DeepBluePalette.IceInk,
                    disabledContainerColor = DeepBluePalette.ElevatedSlateBlue
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(
                        color = DeepBluePalette.IceInk,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = strings.signingIn,
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Text(
                        text = strings.signInWithGoogle,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = DeepBluePalette.AbyssSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DeepBluePalette.ErrorRed.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepBluePalette.ErrorRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Exact required credit on the login screen
        Text(
            text = strings.creditText,
            style = MaterialTheme.typography.labelMedium,
            color = DeepBluePalette.SteelMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
    }
}
