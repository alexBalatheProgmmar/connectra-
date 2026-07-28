package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ConnectraBlue
import com.example.ui.theme.ConnectraCyan
import com.example.ui.theme.ConnectraDarkBlue

@Composable
fun SplashScreen(
  onSplashFinished: () -> Unit
) {
  val scaleAnim = remember { Animatable(0.8f) }

  LaunchedEffect(Unit) {
    scaleAnim.animateTo(
      targetValue = 1.0f,
      animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
    )
    kotlinx.coroutines.delay(1200)
    onSplashFinished()
  }

  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale = infiniteTransition.animateFloat(
    initialValue = 0.96f,
    targetValue = 1.04f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        brush = Brush.verticalGradient(
          colors = listOf(
            ConnectraDarkBlue,
            Color(0xFF0F203C),
            Color(0xFF0369A1)
          )
        )
      )
      .testTag("splash_screen"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(32.dp)
    ) {
      Box(
        modifier = Modifier
          .scale(scaleAnim.value * pulseScale.value)
          .size(120.dp)
          .clip(RoundedCornerShape(30.dp))
          .background(
            brush = Brush.linearGradient(
              colors = listOf(ConnectraBlue, ConnectraCyan)
            )
          )
          .padding(16.dp),
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_connectra_logo),
          contentDescription = "Connectra Logo",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }

      Spacer(modifier = Modifier.height(28.dp))

      Text(
        text = "Connectra",
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        letterSpacing = 1.2.sp
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Connecting People & Conversations",
        fontSize = 14.sp,
        color = Color(0xFFBAE6FD),
        fontWeight = FontWeight.Medium
      )

      Spacer(modifier = Modifier.height(48.dp))

      CircularProgressIndicator(
        modifier = Modifier.size(32.dp),
        color = ConnectraCyan,
        strokeWidth = 3.dp
      )
    }
  }
}
