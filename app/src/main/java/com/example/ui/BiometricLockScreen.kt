package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.ui.theme.ConnectraBlue
import com.example.ui.theme.ConnectraCyan
import com.example.ui.theme.ConnectraDarkBlue
import com.example.util.BiometricAuthManager

@Composable
fun BiometricLockScreen(
  onUnlocked: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val activity = context as? FragmentActivity
  val biometricAuthManager = remember { BiometricAuthManager(context) }
  val canUseBiometrics = remember { biometricAuthManager.canAuthenticate() }

  fun triggerAuth() {
    if (activity != null && canUseBiometrics) {
      biometricAuthManager.showBiometricPrompt(
        activity = activity,
        onSuccess = {
          onUnlocked()
        },
        onError = { error ->
          Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
      )
    } else {
      // Hardware biometric unavailable or not set up
      Toast.makeText(context, "Biometric authentication bypassed or unavailable", Toast.LENGTH_SHORT).show()
      onUnlocked()
    }
  }

  // Trigger prompt automatically on display
  LaunchedEffect(Unit) {
    if (canUseBiometrics) {
      triggerAuth()
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        brush = Brush.verticalGradient(
          colors = listOf(
            ConnectraDarkBlue,
            Color(0xFF0F172A),
            Color(0xFF0369A1)
          )
        )
      )
      .padding(24.dp)
      .testTag("biometric_lock_screen"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxWidth()
    ) {
      // App logo icon badge
      Box(
        modifier = Modifier
          .size(96.dp)
          .clip(RoundedCornerShape(24.dp))
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
          modifier = Modifier.fillMaxSize()
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = "Connectra",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clip(RoundedCornerShape(50.dp))
          .background(Color(0x3338BDF8))
          .padding(horizontal = 12.dp, vertical = 6.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Security,
          contentDescription = "Secure",
          tint = ConnectraCyan,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Protected Chat Session",
          fontSize = 12.sp,
          color = Color.White,
          fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.height(36.dp))

      Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0x1AFFFFFF),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(24.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = "Fingerprint Unlock",
            tint = ConnectraCyan,
            modifier = Modifier.size(64.dp)
          )

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "Authentication Required",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = if (canUseBiometrics) "Use your fingerprint or face scan to unlock Connectra chat." else "Biometrics not configured on this device. Tap below to unlock.",
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFFBAE6FD),
            lineHeight = 18.sp
          )

          Spacer(modifier = Modifier.height(24.dp))

          Button(
            onClick = { triggerAuth() },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = ConnectraBlue,
              contentColor = Color.White
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("unlock_biometric_button")
          ) {
            Icon(
              imageVector = if (canUseBiometrics) Icons.Default.Fingerprint else Icons.Default.Lock,
              contentDescription = null,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (canUseBiometrics) "Unlock with Biometrics" else "Unlock Connectra",
              fontSize = 15.sp,
              fontWeight = FontWeight.SemiBold
            )
          }

          if (canUseBiometrics) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
              onClick = { onUnlocked() },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "Bypass Unlock",
                color = Color.White,
                fontSize = 13.sp
              )
            }
          }
        }
      }
    }
  }
}
