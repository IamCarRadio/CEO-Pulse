package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ShimmerBase
import com.example.ui.theme.ShimmerHighlight

fun Modifier.skeleton(
  isLoading: Boolean = true,
  shape: Shape = RoundedCornerShape(8.dp),
  baseColor: Color = ShimmerBase,
  highlightColor: Color = ShimmerHighlight
): Modifier = composed {
  if (!isLoading) return@composed this

  val transition = rememberInfiniteTransition(label = "skeleton_transition")
  val translateAnimation by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1200f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = 1300,
        easing = FastOutSlowInEasing
      ),
      repeatMode = RepeatMode.Restart
    ),
    label = "skeleton_shimmer"
  )

  val shimmerColors = listOf(
    baseColor.copy(alpha = 0.65f),
    highlightColor.copy(alpha = 0.95f),
    baseColor.copy(alpha = 0.65f)
  )

  val brush = Brush.linearGradient(
    colors = shimmerColors,
    start = Offset(x = translateAnimation - 400f, y = translateAnimation - 400f),
    end = Offset(x = translateAnimation, y = translateAnimation)
  )

  this
    .clip(shape)
    .background(brush)
}

@Composable
fun SkeletonBox(
  modifier: Modifier = Modifier,
  height: Dp = 20.dp,
  shape: Shape = RoundedCornerShape(8.dp)
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
      .skeleton(isLoading = true, shape = shape)
  )
}
