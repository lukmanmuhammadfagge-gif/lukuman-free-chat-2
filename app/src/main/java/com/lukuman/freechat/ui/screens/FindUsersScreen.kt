package com.lukuman.freechat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lukuman.freechat.data.model.DirectoryUser
import com.lukuman.freechat.ui.components.UserAvatar
import com.lukuman.freechat.ui.i18n.AppStrings
import com.lukuman.freechat.ui.theme.DeepBluePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindUsersScreen(
    strings: AppStrings,
    query: String,
    results: List<DirectoryUser>,
    isSearching: Boolean,
    hasSearched: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSelectUser: (DirectoryUser) -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        onSearch()
    }

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
                        text = strings.findUsersTitle,
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
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepBluePalette.DeepOcean)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = strings.searchPlaceholder,
                            color = DeepBluePalette.SteelMuted
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepBluePalette.SapphireSky,
                        unfocusedBorderColor = DeepBluePalette.HairlineBorder,
                        focusedTextColor = DeepBluePalette.IceInk,
                        unfocusedTextColor = DeepBluePalette.IceInk,
                        focusedContainerColor = DeepBluePalette.AbyssSurface,
                        unfocusedContainerColor = DeepBluePalette.AbyssSurface
                    ),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onSearch,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepBluePalette.RoyalCobalt,
                        contentColor = DeepBluePalette.IceInk
                    ),
                    modifier = Modifier.height(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = strings.findUsersTitle
                    )
                }
            }

            if (isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DeepBluePalette.SapphireSky)
                }
            } else if (results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (hasSearched && query.isNotBlank()) strings.noUsersFound else strings.searchPrompt,
                        style = MaterialTheme.typography.bodyLarge,
                        color = DeepBluePalette.SteelMuted,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(results, key = { it.uid }) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectUser(user) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                name = user.displayName,
                                photoUrl = user.photoUrl,
                                size = 48.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = user.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepBluePalette.IceInk,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = strings.startNewChat,
                                style = MaterialTheme.typography.labelMedium,
                                color = DeepBluePalette.SapphireSky
                            )
                        }
                        HorizontalDivider(
                            color = DeepBluePalette.HairlineBorder.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 78.dp, end = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
