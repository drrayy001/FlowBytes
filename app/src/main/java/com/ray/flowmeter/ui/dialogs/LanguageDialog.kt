// Selector listing available localizations to apply runtime locale overrides.
package com.ray.flowmeter.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ray.flowmeter.R
import com.ray.flowmeter.ui.theme.bounceClick

data class LanguageOption(val name: String, val code: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDialog(
    currentLanguageCode: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val languageOptions = listOf(
            LanguageOption(stringResource(R.string.language_default), ""),
            LanguageOption(stringResource(R.string.language_arabic), "ar"),
            LanguageOption(stringResource(R.string.language_french), "fr"),
            LanguageOption(stringResource(R.string.language_spanish), "es"),
            LanguageOption(stringResource(R.string.language_german), "de"),
            LanguageOption(stringResource(R.string.language_portuguese), "pt"),
            LanguageOption(stringResource(R.string.language_italian), "it"),
            LanguageOption(stringResource(R.string.language_chinese), "zh"),
            LanguageOption(stringResource(R.string.language_hindi), "hi"),
            LanguageOption(stringResource(R.string.language_japanese), "ja"),
            LanguageOption(stringResource(R.string.language_korean), "ko"),
            LanguageOption(stringResource(R.string.language_russian), "ru"),
            LanguageOption(stringResource(R.string.language_turkish), "tr"),
            LanguageOption(stringResource(R.string.language_indonesian), "id"),
            LanguageOption(stringResource(R.string.language_vietnamese), "vi"),
            LanguageOption(stringResource(R.string.language_polish), "pl"),
            LanguageOption(stringResource(R.string.language_ukrainian), "uk")
        )

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.dialog_select_language_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                items(languageOptions) { option ->
                    val isSelected = currentLanguageCode == option.code
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .bounceClick {
                                onSelect(option.code)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            RadioButton(
                                selected = isSelected,
                                onClick = null
                            )
                        }
                    }
                }
            }
        }
    }
}
