package win.haya.yamaokaya

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ChaosRamenSprite(
    val imageIndex: Int,
    val phase: Float,
    val speedX: Float,
    val speedY: Float,
    val wobbleCycles: Float,
    val wobbleAmplitude: Float,
    val rotationOffset: Float,
    val scale: Float,
    val alpha: Float,
    val orbitRadius: Float,
    val orbitCycles: Float,
    val pulseDepth: Float,
    val jitterSeed: Float
)

@Composable
internal fun ChaosRamenStorm(
    imageDrawableIds: List<Int>,
    modifier: Modifier = Modifier
) {
    if (imageDrawableIds.isEmpty()) return

    val sprites = remember(imageDrawableIds) {
        val random = Random(9813)
        List(42) {
            ChaosRamenSprite(
                imageIndex = random.nextInt(imageDrawableIds.size),
                phase = random.nextFloat(),
                speedX = 0.25f + random.nextFloat() * 1.9f,
                speedY = 0.22f + random.nextFloat() * 1.8f,
                wobbleCycles = 0.8f + random.nextFloat() * 6.8f,
                wobbleAmplitude = 0.02f + random.nextFloat() * 0.18f,
                rotationOffset = random.nextFloat() * 360f,
                scale = 0.45f + random.nextFloat() * 1.3f,
                alpha = 0.2f + random.nextFloat() * 0.75f,
                orbitRadius = 0.01f + random.nextFloat() * 0.11f,
                orbitCycles = 0.6f + random.nextFloat() * 8.6f,
                pulseDepth = 0.05f + random.nextFloat() * 0.55f,
                jitterSeed = random.nextFloat() * 2f * PI.toFloat()
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "chaos_ramen")
    val progressFast by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "chaos_progress_fast"
    )
    val progressSlow by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "chaos_progress_slow"
    )
    val progressPulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "chaos_progress_pulse"
    )

    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        sprites.forEach { sprite ->
            val localFast = (progressFast + sprite.phase) % 1f
            val localSlow = (progressSlow + sprite.phase * 0.43f) % 1f
            val localPulse = (progressPulse + sprite.phase * 0.77f) % 1f

            val driftX = (((sprite.phase * 0.37f) + localFast * sprite.speedX + localSlow * 0.9f) % 1f) * widthPx
            val driftY = (((sprite.phase * 0.61f) + localFast * sprite.speedY + localSlow * 0.7f) % 1f) * heightPx

            val orbitTheta = (localSlow * sprite.orbitCycles * 2f * PI).toFloat() + sprite.jitterSeed
            val orbitX = cos(orbitTheta) * sprite.orbitRadius * widthPx
            val orbitY = sin(orbitTheta) * sprite.orbitRadius * heightPx

            val wobbleX = sin((localFast * (sprite.wobbleCycles + 1.7f) * 2f * PI).toFloat()) * sprite.wobbleAmplitude * widthPx
            val wobbleY = sin((localFast * sprite.wobbleCycles * 2f * PI).toFloat()) * sprite.wobbleAmplitude * heightPx

            val jitterX = sin(localFast * 39f + sprite.jitterSeed) * 18f
            val jitterY = cos(localFast * 31f + sprite.jitterSeed * 0.7f) * 18f

            val x = (driftX + orbitX + wobbleX + jitterX).coerceIn(-120f, widthPx + 120f)
            val y = (driftY + orbitY + wobbleY + jitterY).coerceIn(-120f, heightPx + 120f)

            val rotation = (
                localFast * 1260f +
                    localSlow * 740f +
                    sin(localPulse * 2f * PI.toFloat()) * 120f +
                    sprite.rotationOffset
                ) % 360f

            val pulseScale = 1f + sin((localPulse * 2f * PI).toFloat() + sprite.jitterSeed) * sprite.pulseDepth
            val finalScale = (sprite.scale * pulseScale).coerceIn(0.28f, 2.2f)
            val flickerAlpha = (
                sprite.alpha * (0.75f + 0.25f * sin((localFast * 2f * PI).toFloat() + sprite.jitterSeed))
                ).coerceIn(0.12f, 1f)

            Image(
                painter = painterResource(id = imageDrawableIds[sprite.imageIndex]),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(84.dp)
                    .graphicsLayer {
                        translationX = x - 42f
                        translationY = y - 42f
                        rotationZ = rotation
                        scaleX = finalScale
                        scaleY = finalScale
                        alpha = flickerAlpha
                    }
            )
        }
    }
}
