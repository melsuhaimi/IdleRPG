package com.idlerpg.game.ui.component.premium

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.idlerpg.game.R
import com.idlerpg.game.ui.theme.LocalReducedMotion
import com.idlerpg.game.ui.theme.ObsidianBackground

@Composable
fun GameLoadingSurface(title: String) {
    val transition = rememberInfiniteTransition(label = "game_loading")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_rotation"
    )
    val displayRotation = if (LocalReducedMotion.current) 0f else rotation

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        Image(
            painter = painterResource(R.drawable.bg_battle_premium),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.24f),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xDD060811),
                            Color(0x99060811),
                            Color(0xF0060811)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(156.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = displayRotation },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    strokeWidth = 3.dp
                )
                Image(
                    painter = painterResource(R.drawable.hero_echo_bound),
                    contentDescription = null,
                    modifier = Modifier
                        .size(108.dp)
                        .graphicsLayer {
                            rotationZ = displayRotation / 18f
                            alpha = 0.94f
                        },
                    contentScale = ContentScale.Fit
                )
            }
            Text(
                text = stringResource(R.string.app_name_display).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            GameStatusPill(
                text = title,
                accent = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = stringResource(R.string.runtime_loading_tip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
