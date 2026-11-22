package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    // Choreographed animation tracks
    val auraAlpha = remember { Animatable(0f) }
    val auraScale = remember { Animatable(0.65f) }
    val ringRotation = remember { Animatable(0f) }

    val emblemScale = remember { Animatable(0.68f) }
    val emblemAlpha = remember { Animatable(0f) }
    val emblemTranslationY = remember { Animatable(48f) }
    val emblemTiltX = remember { Animatable(18f) }

    val shimmerProgress = remember { Animatable(-0.6f) }

    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(26f) }
    val titleLetterSpacing = remember { Animatable(6.5f) }

    val dividerProgress = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val subtitleOffsetY = remember { Animatable(16f) }

    val progressBar = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Phase 1: Ambient obsidian-gold aura & orbital hairline rings
        launch {
            auraAlpha.animateTo(1f, tween(650, easing = EmphasizedDecelerate))
        }
        launch {
            auraScale.animateTo(1f, tween(950, easing = EmphasizedDecelerate))
        }
        launch {
            ringRotation.animateTo(38f, tween(2100, easing = LinearEasing))
        }

        // Phase 2: 3D spring entrance of the sculpted NoteLedger emblem
        launch {
            delay(80)
            launch {
                emblemAlpha.animateTo(1f, tween(420, easing = EmphasizedDecelerate))
            }
            launch {
                emblemScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.68f,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                emblemTranslationY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(680, easing = EmphasizedDecelerate)
                )
            }
            launch {
                emblemTiltX.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(760, easing = EmphasizedDecelerate)
                )
            }
        }

        // Phase 3: Editorial wordmark with tightening letter-spacing & hairline gold accent
        launch {
            delay(360)
            launch {
                titleAlpha.animateTo(1f, tween(520, easing = EmphasizedDecelerate))
            }
            launch {
                titleOffsetY.animateTo(0f, tween(620, easing = EmphasizedDecelerate))
            }
            launch {
                titleLetterSpacing.animateTo(0.8f, tween(760, easing = EmphasizedDecelerate))
            }
        }

        // Phase 4: Diagonal metallic champagne-gold sheen sweep across the emblem
        launch {
            delay(540)
            shimmerProgress.animateTo(1.75f, tween(780, easing = FastOutSlowInEasing))
        }

        // Phase 5: Hairline gold divider + subtitle reveal
        launch {
            delay(600)
            launch {
                dividerProgress.animateTo(1f, tween(480, easing = EmphasizedDecelerate))
            }
            launch {
                delay(100)
                launch {
                    subtitleAlpha.animateTo(1f, tween(480, easing = EmphasizedDecelerate))
                }
                launch {
                    subtitleOffsetY.animateTo(0f, tween(500, easing = EmphasizedDecelerate))
                }
            }
        }

        // Smooth bottom progress glide
        launch {
            delay(200)
            progressBar.animateTo(1f, tween(1500, easing = EmphasizedDecelerate))
        }

        delay(1850)
        onAnimationFinished()
    }

    // Palette tuned to match the obsidian-emerald & brushed champagne-gold icon
    val deepObsidian = Color(0xFF07130E)
    val forestEmerald = Color(0xFF0E231B)
    val champagneGold = Color(0xFFD9BC82)
    val warmIvory = Color(0xFFF6F2E9)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(forestEmerald, deepObsidian),
                    radius = 1250f
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Atmospheric ambient glow & refined precision hairline rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.44f)
            val baseRadius = size.minDimension * 0.42f * auraScale.value

            // Soft warm champagne ambient radial halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        champagneGold.copy(alpha = 0.16f * auraAlpha.value),
                        Color(0xFF1F4D3A).copy(alpha = 0.12f * auraAlpha.value),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.45f
                ),
                radius = baseRadius * 1.45f,
                center = center
            )

            // Outer fine geometric orbital ring
            rotate(degrees = ringRotation.value, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            champagneGold.copy(alpha = 0.24f * auraAlpha.value),
                            Color.Transparent,
                            champagneGold.copy(alpha = 0.12f * auraAlpha.value),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius * 0.72f, center.y - baseRadius * 0.72f),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 1.44f, baseRadius * 1.44f),
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Inner counter-rotating subtle hairline ring
            rotate(degrees = -ringRotation.value * 0.7f, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            warmIvory.copy(alpha = 0.16f * auraAlpha.value),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 25f,
                    sweepAngle = 210f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius * 0.54f, center.y - baseRadius * 0.54f),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 1.08f, baseRadius * 1.08f),
                    style = Stroke(width = 0.9.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Centered brand emblem + editorial typography
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 36.dp)
        ) {
            val emblemShape = RoundedCornerShape(32.dp)

            // Sculpted Icon Container with 3D perspective spring & metallic sheen sweep
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .graphicsLayer {
                        scaleX = emblemScale.value
                        scaleY = emblemScale.value
                        alpha = emblemAlpha.value
                        translationY = emblemTranslationY.value
                        rotationX = emblemTiltX.value
                        cameraDistance = 16f * density
                    }
                    .shadow(
                        elevation = 28.dp,
                        shape = emblemShape,
                        ambientColor = champagneGold.copy(alpha = 0.25f),
                        spotColor = Color.Black.copy(alpha = 0.75f)
                    )
                    .clip(emblemShape)
                    .border(
                        width = 1.25.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                champagneGold.copy(alpha = 0.65f),
                                champagneGold.copy(alpha = 0.12f),
                                warmIvory.copy(alpha = 0.30f)
                            )
                        ),
                        shape = emblemShape
                    )
                    .drawWithContent {
                        drawContent()
                        // Diagonal luxury specular light sheen gliding across the emblem
                        val sweep = shimmerProgress.value
                        val bandWidth = size.width * 0.45f
                        val startX = (sweep * size.width) - bandWidth
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.28f),
                                    champagneGold.copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                start = Offset(startX, 0f),
                                end = Offset(startX + bandWidth, size.height)
                            )
                        )
                    }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.noteledger_premium_icon_1795336835657),
                    contentDescription = "NoteLedger App Emblem",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Editorial Wordmark with animated letter-spacing tightening
            Text(
                text = "NoteLedger",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = titleLetterSpacing.value.sp
                ),
                color = warmIvory,
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha.value
                    translationY = titleOffsetY.value
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Expanding hairline champagne-gold rule
            Box(
                modifier = Modifier
                    .width((56f * dividerProgress.value).dp)
                    .height(1.5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                champagneGold.copy(alpha = 0.9f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Crisp architectural subtitle
            Text(
                text = "PRIVATE NOTES  •  SMART LEDGER",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.4.sp
                ),
                color = champagneGold.copy(alpha = 0.82f),
                modifier = Modifier.graphicsLayer {
                    alpha = subtitleAlpha.value
                    translationY = subtitleOffsetY.value
                }
            )
        }

        // Minimal hairline progress indicator anchored elegantly at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .width(72.dp)
                .height(2.5.dp)
                .clip(RoundedCornerShape(50))
                .background(warmIvory.copy(alpha = 0.10f))
        ) {
            Box(
                modifier = Modifier
                    .width((72f * progressBar.value).dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                champagneGold.copy(alpha = 0.45f),
                                champagneGold
                            )
                        )
                    )
            )
        }
    }
}
