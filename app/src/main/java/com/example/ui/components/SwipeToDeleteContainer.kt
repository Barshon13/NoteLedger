package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Reusable container providing smooth swipe-to-delete functionality for list items.
 *
 * Supports intuitive swipe gestures in either direction with haptic feedback,
 * animated red background, delete indicator icons, and smooth collapse animations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    enableDismissFromStartToEnd: Boolean = true,
    enableDismissFromEndToStart: Boolean = true,
    testTag: String = "swipe_to_delete_container",
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isDismissed by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isDismissed = true
                true
            } else {
                false
            }
        }
    )

    LaunchedEffect(isDismissed) {
        if (isDismissed) {
            delay(280) // Allow exit collapse animation to render cleanly
            onDelete()
        }
    }

    AnimatedVisibility(
        visible = !isDismissed,
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = 280),
            shrinkTowards = Alignment.Top
        ) + fadeOut(animationSpec = tween(durationMillis = 240)),
        modifier = modifier.testTag(testTag)
    ) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = enableDismissFromStartToEnd,
            enableDismissFromEndToStart = enableDismissFromEndToStart,
            modifier = Modifier.clip(shape),
            backgroundContent = {
                val isSwipedFar = dismissState.targetValue != SwipeToDismissBoxValue.Settled

                val backgroundColor = if (isSwipedFar) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }

                val contentColor = if (isSwipedFar) {
                    MaterialTheme.colorScheme.onError
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }

                val alignment = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.CenterEnd
                }

                val isStartToEnd = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .background(backgroundColor)
                        .padding(horizontal = 20.dp),
                    contentAlignment = alignment
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.scale(if (isSwipedFar) 1.12f else 1.0f)
                    ) {
                        if (isStartToEnd) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete item",
                                tint = contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Delete",
                                color = contentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                text = "Delete",
                                color = contentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete item",
                                tint = contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        ) {
            content()
        }
    }
}
