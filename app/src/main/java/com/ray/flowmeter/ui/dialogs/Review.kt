// Dialog requesting user feedback and ratings, linked directly to the app store listing.
package com.ray.flowmeter.ui.dialogs

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.ray.flowmeter.R
import com.ray.flowmeter.ui.theme.bounceClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDialog(
    onDismiss: () -> Unit,
    onNeverShowAgain: () -> Unit,
    onLater: () -> Unit,
    onReviewCompleted: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var rating by remember { mutableIntStateOf(0) }
    val feedbackSubject = stringResource(R.string.feedback_email_subject, rating)
    val toastFeedback = stringResource(R.string.review_toast_feedback_submitted)
    var feedbackText by remember { mutableStateOf("") }
    
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
                    .padding(bottom = 40.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                AnimatedContent(
                    targetState = rating,
                    transitionSpec = {
                        if (initialState == 0) {
                            (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                                    (slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                                    (slideOutHorizontally { width -> width } + fadeOut())
                        }.using(
                            SizeTransform(clip = false)
                        )
                    },
                    label = "review_dialog_flow"
                ) { targetRating ->
                    when {
                        targetRating == 0 -> {
                            RatingSelectionStep(
                                currentRating = rating,
                                onRatingSelected = { rating = it },
                                onLater = onLater,
                                onNever = onNeverShowAgain
                            )
                        }
                        targetRating >= 4 -> {
                            PositiveFeedbackStep(
                                onRateNow = {
                                    val packageName = context.packageName
                                    val intent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri()))
                                        } catch (_: Exception) {}
                                    }
                                    onReviewCompleted()
                                },
                                onLater = onLater,
                                onNever = onNeverShowAgain
                            )
                        }
                        else -> {
                            NegativeFeedbackStep(
                                feedbackText = feedbackText,
                                onFeedbackChange = { feedbackText = it },
                                onSubmitFeedback = {
                                    if (feedbackText.isNotBlank()) {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = "mailto:".toUri()
                                            putExtra(Intent.EXTRA_EMAIL, arrayOf("drrayy001@gmail.com"))
                                            putExtra(Intent.EXTRA_SUBJECT, feedbackSubject)
                                            putExtra(Intent.EXTRA_TEXT, feedbackText)
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                        }
                                    }
                                    Toast.makeText(context, toastFeedback, Toast.LENGTH_SHORT).show()
                                    onReviewCompleted()
                                },
                                onCancel = onLater
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingSelectionStep(
    currentRating: Int,
    onRatingSelected: (Int) -> Unit,
    onLater: () -> Unit,
    onNever: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.review_dialog_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Text(
            text = stringResource(R.string.review_dialog_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 28.dp),
            lineHeight = 20.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..5) {
                val isSelected = i <= currentRating
                val interactionSource = remember { MutableInteractionSource() }
                Icon(
                    imageVector = if (isSelected) Icons.Rounded.Star else Icons.Outlined.Star,
                    contentDescription = "Rate $i stars",
                    tint = if (isSelected) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(48.dp)
                        .padding(horizontal = 4.dp)
                        .bounceClick(
                            interactionSource = interactionSource,
                        ) { onRatingSelected(i) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = onNever,
                modifier = Modifier.bounceClick()
            ) {
                Text(
                    text = stringResource(R.string.review_never),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }

            TextButton(
                onClick = onLater,
                modifier = Modifier.bounceClick()
            ) {
                Text(
                    text = stringResource(R.string.review_later),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PositiveFeedbackStep(
    onRateNow: () -> Unit,
    onLater: () -> Unit,
    onNever: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.review_play_store_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Text(
            text = stringResource(R.string.review_play_store_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 28.dp),
            lineHeight = 20.sp
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .bounceClick { onRateNow() },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.review_rate_on_play_store),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = onNever,
                modifier = Modifier.bounceClick()
            ) {
                Text(
                    text = stringResource(R.string.review_never),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }

            TextButton(
                onClick = onLater,
                modifier = Modifier.bounceClick()
            ) {
                Text(
                    text = stringResource(R.string.review_later),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun NegativeFeedbackStep(
    feedbackText: String,
    onFeedbackChange: (String) -> Unit,
    onSubmitFeedback: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.review_feedback_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Text(
            text = stringResource(R.string.review_feedback_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp),
            lineHeight = 20.sp
        )

        OutlinedTextField(
            value = feedbackText,
            onValueChange = onFeedbackChange,
            placeholder = {
                Text(
                    text = stringResource(R.string.review_feedback_placeholder),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .bounceClick(enabled = feedbackText.isNotBlank()) {
                    if (feedbackText.isNotBlank()) {
                        onSubmitFeedback()
                    }
                },
            shape = RoundedCornerShape(16.dp),
            color = if (feedbackText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            contentColor = if (feedbackText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.review_submit_feedback),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.bounceClick()
            ) {
                Text(
                    text = stringResource(R.string.btn_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
