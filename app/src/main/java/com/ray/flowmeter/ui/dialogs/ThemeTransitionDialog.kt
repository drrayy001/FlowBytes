// Theme transition effect selector dialog offering selection of Nitro transition animation styles.
package com.ray.flowmeter.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ray.flowmeter.R
import com.ray.flowmeter.ui.theme.ThemeTransitionKind
import com.ray.flowmeter.ui.theme.bounceClick

data class TransitionOption(
    val kind: ThemeTransitionKind,
    val titleRes: Int,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeTransitionDialog(
    currentKind: String,
    title: String = stringResource(R.string.dialog_select_theme_transition_title),
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val options = remember {
        listOf(
            TransitionOption(ThemeTransitionKind.NONE, R.string.transition_none, Icons.Rounded.Block),
            TransitionOption(ThemeTransitionKind.CIRCULAR_REVEAL, R.string.transition_circular_reveal, Icons.Rounded.RadioButtonChecked),
            TransitionOption(ThemeTransitionKind.CIRCULAR_REVEAL_INVERSE, R.string.transition_circular_reveal_inverse, Icons.Rounded.ChangeCircle),
            TransitionOption(ThemeTransitionKind.IRIS_HEXAGON, R.string.transition_iris, Icons.Rounded.Hexagon),
            TransitionOption(ThemeTransitionKind.IRIS_DIAMOND, R.string.transition_iris_diamond, Icons.Rounded.Diamond),
            TransitionOption(ThemeTransitionKind.WIPE_RIGHT, R.string.transition_wipe, Icons.Rounded.SwipeRight),
            TransitionOption(ThemeTransitionKind.SPLIT, R.string.transition_split, Icons.Rounded.VerticalSplit),
            TransitionOption(ThemeTransitionKind.BARN_DOOR, R.string.transition_barn_door, Icons.Rounded.MeetingRoom),
            TransitionOption(ThemeTransitionKind.BLINDS, R.string.transition_blinds, Icons.Rounded.TableRows),
            TransitionOption(ThemeTransitionKind.RIPPLE, R.string.transition_ripple, Icons.Rounded.Waves),
            TransitionOption(ThemeTransitionKind.STRIPES, R.string.transition_stripes, Icons.Rounded.ViewStream),
            TransitionOption(ThemeTransitionKind.FADE, R.string.transition_fade, Icons.Rounded.Gradient),
            TransitionOption(ThemeTransitionKind.ZOOM, R.string.transition_zoom, Icons.Rounded.ZoomIn),
            TransitionOption(ThemeTransitionKind.RANDOM, R.string.transition_random, Icons.Rounded.Shuffle)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
    ) {
        AnimatedDialogContent(onBack = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(options) { item ->
                        val isSelected = currentKind.equals(item.kind.key, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick {
                                    onSelect(item.kind.key)
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = stringResource(item.titleRes),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
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
}
