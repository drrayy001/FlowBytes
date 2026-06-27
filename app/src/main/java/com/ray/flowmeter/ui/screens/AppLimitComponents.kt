// Shared UI widgets for individual app limit details, usage progress bars,
// and action menus on the limit configuration screen.
package com.ray.flowmeter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ray.flowmeter.utils.SpeedFormatter

import androidx.compose.ui.text.style.TextAlign

@Composable
fun StatusIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier.size(16.dp),
        tint = color
    )
}

@Composable
fun EmptyLimitsPlaceholder(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer glowing/soft ring
                Surface(
                    modifier = Modifier.size(130.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                    shape = CircleShape
                ) {}
                // Inner ring
                Surface(
                    modifier = Modifier.size(100.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {}
                // Center icon background
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun LimitInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    onUnitChange: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.all { char -> char.isDigit() }) onValueChange(it) },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            trailingIcon = {
                var expanded by remember { mutableStateOf(value = false) }
                Box {
                    TextButton(
                        onClick = { expanded = true },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(unit, fontWeight = FontWeight.ExtraBold)
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        DropdownMenuItem(
                            text = { Text("MB", fontWeight = FontWeight.Bold) },
                            onClick = { onUnitChange("MB"); expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("GB", fontWeight = FontWeight.Bold) },
                            onClick = { onUnitChange("GB"); expanded = false }
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodChip(selected: Boolean, onClick: () -> Unit, label: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
        shape = RoundedCornerShape(12.dp),
        leadingIcon = if (selected) { { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp)) } } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconColor = MaterialTheme.colorScheme.primary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = Color.Transparent
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkChip(selected: Boolean, onClick: () -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
        shape = RoundedCornerShape(12.dp),
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconColor = MaterialTheme.colorScheme.primary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = Color.Transparent
        )
    )
}

fun formatUsage(bytes: Long): String = SpeedFormatter.formatUsage(bytes)
