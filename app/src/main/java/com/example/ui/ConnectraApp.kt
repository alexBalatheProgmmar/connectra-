package com.example.ui

import android.app.Activity
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ConnectraCyan
import com.example.util.NetworkMonitor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectraApp() {
  val context = LocalContext.current
  val activity = context as? Activity
  val networkMonitor = remember { NetworkMonitor(context) }
  val isOnline by networkMonitor.isOnline.collectAsState(initial = networkMonitor.isCurrentlyOnline())

  var showSplash by remember { mutableStateOf(true) }
  var isLocked by remember { mutableStateOf(true) }
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var progress by remember { mutableFloatStateOf(0f) }
  var isPageError by remember { mutableStateOf(false) }
  var isRefreshing by remember { mutableStateOf(false) }
  var lastBackPressTime by remember { mutableLongStateOf(0L) }

  // Auto retry loading webview when internet becomes available
  LaunchedEffect(isOnline) {
    if (isOnline && isPageError) {
      isPageError = false
      webViewRef?.reload()
    }
  }

  // Custom system back-navigation handling for WebView page history navigation
  BackHandler(enabled = !showSplash) {
    if (isLocked) {
      activity?.finish()
      return@BackHandler
    }
    val webView = webViewRef
    if (webView != null && webView.canGoBack()) {
      webView.goBack()
    } else {
      val currentTime = System.currentTimeMillis()
      if (currentTime - lastBackPressTime < 2000) {
        activity?.finish()
      } else {
        lastBackPressTime = currentTime
        Toast.makeText(context, "Press back again to exit Connectra", Toast.LENGTH_SHORT).show()
      }
    }
  }

  val pullToRefreshState = rememberPullToRefreshState()

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    modifier = Modifier.fillMaxSize()
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.background)
    ) {
      // Pull to refresh container for WebView
      PullToRefreshBox(
        isRefreshing = isRefreshing,
        state = pullToRefreshState,
        onRefresh = {
          isRefreshing = true
          isPageError = false
          webViewRef?.reload()
          isRefreshing = false
        },
        modifier = Modifier.fillMaxSize()
      ) {
        ConnectraWebView(
          isOnline = isOnline,
          onPageError = { isError ->
            isPageError = isError
          },
          onProgressChanged = { newProgress ->
            progress = newProgress
            if (newProgress >= 1.0f) {
              isRefreshing = false
            }
          },
          onWebViewCreated = { wv ->
            webViewRef = wv
          },
          modifier = Modifier.fillMaxSize()
        )
      }

      // Top Progress Indicator
      if (progress in 0.01f..0.99f && isOnline && !isPageError) {
        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .align(Alignment.TopCenter),
          color = ConnectraCyan,
          trackColor = Color.Transparent
        )
      }

      // Offline Screen overlay
      if (!isOnline || isPageError) {
        OfflineScreen(
          onRetry = {
            if (networkMonitor.isCurrentlyOnline()) {
              isPageError = false
              webViewRef?.reload()
            } else {
              Toast.makeText(context, "Still offline. Please check your network.", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier.fillMaxSize()
        )
      }

      // Biometric Lock Screen overlay
      if (!showSplash && isLocked) {
        BiometricLockScreen(
          onUnlocked = {
            isLocked = false
          },
          modifier = Modifier.fillMaxSize()
        )
      }

      // Native Splash Screen overlay
      AnimatedVisibility(
        visible = showSplash,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        SplashScreen(
          onSplashFinished = {
            showSplash = false
          }
        )
      }
    }
  }
}
