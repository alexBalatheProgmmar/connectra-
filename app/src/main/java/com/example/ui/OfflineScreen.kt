package com.example.ui

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ConnectraBlue

@Composable
fun OfflineScreen(
  onRetry: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(24.dp)
      .testTag("offline_screen"),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 4.dp,
      shadowElevation = 8.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
      ) {
        Box(
          modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Color(0xFFFEF2F2)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = "Offline",
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(40.dp)
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(Color(0xFFFEF2F2))
            .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "No Connection",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF991B1B)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "You're Offline",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Connectra requires an active internet connection to load messages and connect to chat servers.",
          fontSize = 14.sp,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
          onClick = onRetry,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = ConnectraBlue,
            contentColor = Color.White
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("retry_button")
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Retry",
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Try Again",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}
