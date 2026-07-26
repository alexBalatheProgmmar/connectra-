package com.example.ui

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.ui.theme.ConnectraCyan
import java.io.File

const val BASE_URL = "https://connectra-app.lovable.app/chats"

@Composable
fun ConnectraWebView(
  webView: WebView,
  isOnline: Boolean,
  onPageError: (Boolean) -> Unit,
  onProgressChanged: (Float) -> Unit,
  onWebViewCreated: (WebView) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
  var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
  var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
  var customView by remember { mutableStateOf<View?>(null) }
  var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

  // File chooser activity launcher
  val fileChooserLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (filePathCallback == null) return@rememberLauncherForActivityResult
    val results: Array<Uri>? = if (result.resultCode == Activity.RESULT_OK) {
      result.data?.data?.let { arrayOf(it) }
        ?: result.data?.clipData?.let { clipData ->
          Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
        }
        ?: cameraImageUri?.let { arrayOf(it) }
    } else {
      null
    }
    filePathCallback?.onReceiveValue(results)
    filePathCallback = null
  }

  // Permission request launcher for WebRTC / Camera & Microphone
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val granted = permissions.values.all { it }
    pendingPermissionRequest?.let { request ->
      if (granted) {
        request.grant(request.resources)
      } else {
        request.deny()
      }
      pendingPermissionRequest = null
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      CookieManager.getInstance().flush()
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    AndroidView(
      factory = { ctx ->
        webView.apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )

          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            setGeolocationEnabled(true)
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            displayZoomControls = false
            userAgentString = userAgentString + " ConnectraAndroidApp/1.0"
          }

          // Setup Cookies
          CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
          }

          webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
              onProgressChanged(newProgress / 100f)
            }

            override fun onShowFileChooser(
              webView: WebView?,
              filePathCallbackParam: ValueCallback<Array<Uri>>?,
              fileChooserParams: FileChooserParams?
            ): Boolean {
              filePathCallback?.onReceiveValue(null)
              filePathCallback = filePathCallbackParam

              // Create intent for file selection / camera capture
              val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
              }

              val intentArray = arrayOf<Intent>()
              val chooserIntent = Intent(Intent.ACTION_CHOOSER).apply {
                putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                putExtra(Intent.EXTRA_TITLE, "Select File or Media")
                putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
              }

              fileChooserLauncher.launch(chooserIntent)
              return true
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
              request ?: return
              val requestedResources = request.resources
              val requiredPermissions = mutableListOf<String>()

              for (resource in requestedResources) {
                if (resource == PermissionRequest.RESOURCE_AUDIO_CAPTURE) {
                  requiredPermissions.add(Manifest.permission.RECORD_AUDIO)
                }
                if (resource == PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                  requiredPermissions.add(Manifest.permission.CAMERA)
                }
              }

              if (requiredPermissions.isEmpty()) {
                request.grant(requestedResources)
                return
              }

              val notGranted = requiredPermissions.filter {
                ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED
              }

              if (notGranted.isEmpty()) {
                request.grant(requestedResources)
              } else {
                pendingPermissionRequest = request
                permissionLauncher.launch(notGranted.toTypedArray())
              }
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
              customView = view
              customViewCallback = callback
            }

            override fun onHideCustomView() {
              customViewCallback?.onCustomViewHidden()
              customView = null
              customViewCallback = null
            }
          }

          webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
              view: WebView?,
              request: WebResourceRequest?
            ): Boolean {
              val uri = request?.url ?: return false
              val url = uri.toString()

              // Internal Connectra domain or relative paths stay in WebView
              if (url.contains("connectra-app.lovable.app") || url.startsWith("file:///")) {
                return false
              }

              // Handle external web links by opening in default external browser
              if (url.startsWith("http://") || url.startsWith("https://")) {
                try {
                  val intent = Intent(Intent.ACTION_VIEW, uri)
                  ctx.startActivity(intent)
                } catch (e: Exception) {
                  Toast.makeText(ctx, "No browser app found to open link", Toast.LENGTH_SHORT).show()
                }
                return true
              }

              // Handle non-web intents (tel:, mailto:, whatsapp:, etc.)
              try {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                ctx.startActivity(intent)
              } catch (e: Exception) {
                Toast.makeText(ctx, "Cannot open external application", Toast.LENGTH_SHORT).show()
              }
              return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
              super.onPageFinished(view, url)
              CookieManager.getInstance().flush()
            }

            override fun onReceivedError(
              view: WebView?,
              request: WebResourceRequest?,
              error: WebResourceError?
            ) {
              if (request?.isForMainFrame == true) {
                onPageError(true)
              }
            }
          }

          setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            try {
              val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimetype)
                addRequestHeader("User-Agent", userAgent)
                addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
                setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
                setDescription("Downloading file from Connectra...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(
                  Environment.DIRECTORY_DOWNLOADS,
                  URLUtil.guessFileName(url, contentDisposition, mimetype)
                )
              }
              val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
              dm.enqueue(request)
              Toast.makeText(ctx, "Download started...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
              Toast.makeText(ctx, "Failed to download file", Toast.LENGTH_SHORT).show()
            }
          }

          onWebViewCreated(this)

          if (url == null) {
            loadUrl(BASE_URL)
          }
        }
      },
      update = { view ->
        // Active updates if needed
      },
      modifier = Modifier
        .fillMaxSize()
        .testTag("connectra_webview")
    )

    // Full screen custom view overlay (for videos)
    customView?.let { view ->
      AndroidView(
        factory = { view },
        modifier = Modifier.fillMaxSize()
      )
    }
  }
}

// Utility object for file name guessing
private object URLUtil {
  fun guessFileName(url: String, contentDisposition: String?, mimeType: String?): String {
    return android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
  }
}
