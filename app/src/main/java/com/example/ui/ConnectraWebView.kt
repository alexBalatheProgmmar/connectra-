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
import android.webkit.MimeTypeMap
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
import androidx.activity.result.PickVisualMediaRequest
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
  isOnline: Boolean,
  onPageError: (Boolean) -> Unit,
  onProgressChanged: (Float) -> Unit,
  onWebViewCreated: (WebView) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
  var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }
  var customView by remember { mutableStateOf<View?>(null) }
  var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

  // Modern PickVisualMedia launcher for single photo/video picking
  val pickVisualMediaLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    val callback = filePathCallback
    filePathCallback = null
    if (callback != null) {
      try {
        val results = if (uri != null) arrayOf(uri) else null
        callback.onReceiveValue(results)
      } catch (_: Exception) {
        try {
          callback.onReceiveValue(null)
        } catch (_: Exception) {}
      }
    }
  }

  // Modern PickMultipleVisualMedia launcher for multiple photos/videos picking
  val pickMultipleVisualMediaLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia()
  ) { uris: List<Uri> ->
    val callback = filePathCallback
    filePathCallback = null
    if (callback != null) {
      try {
        val results = if (uris.isNotEmpty()) uris.toTypedArray() else null
        callback.onReceiveValue(results)
      } catch (_: Exception) {
        try {
          callback.onReceiveValue(null)
        } catch (_: Exception) {}
      }
    }
  }

  // Fallback / General file chooser activity launcher
  val fileChooserLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    val callback = filePathCallback
    filePathCallback = null

    if (callback != null) {
      try {
        var results: Array<Uri>? = null
        if (result.resultCode == Activity.RESULT_OK) {
          results = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
          if (results == null && result.data != null) {
            result.data?.data?.let { uri ->
              results = arrayOf(uri)
            } ?: result.data?.clipData?.let { clipData ->
              if (clipData.itemCount > 0) {
                results = Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
              }
            }
          }
        }
        callback.onReceiveValue(results)
      } catch (e: Exception) {
        try {
          callback.onReceiveValue(null)
        } catch (_: Exception) {}
      }
    }
  }

  // Permission request launcher for WebRTC / Camera & Microphone
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val granted = permissions.values.all { it }
    pendingPermissionRequest?.let { request ->
      try {
        if (granted) {
          request.grant(request.resources)
        } else {
          request.deny()
        }
      } catch (_: Exception) {}
      pendingPermissionRequest = null
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      if (filePathCallback != null) {
        try {
          filePathCallback?.onReceiveValue(null)
        } catch (_: Exception) {}
        filePathCallback = null
      }
      try {
        CookieManager.getInstance().flush()
      } catch (_: Exception) {}
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    AndroidView(
      factory = { ctx ->
        WebView(ctx).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )

          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            setGeolocationEnabled(true)
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            displayZoomControls = false
            val currentUa = userAgentString ?: ""
            if (!currentUa.contains("ConnectraAndroidApp")) {
              userAgentString = "$currentUa ConnectraAndroidApp/1.0"
            }
          }

          // Setup Cookies
          try {
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
          } catch (_: Exception) {}

          webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
              onProgressChanged(newProgress / 100f)
            }

            override fun onShowFileChooser(
              webView: WebView?,
              filePathCallbackParam: ValueCallback<Array<Uri>>?,
              fileChooserParams: FileChooserParams?
            ): Boolean {
              // Cancel any existing pending callback to avoid Chromium native callback error
              if (filePathCallback != null) {
                try {
                  filePathCallback?.onReceiveValue(null)
                } catch (_: Exception) {}
                filePathCallback = null
              }
              filePathCallback = filePathCallbackParam

              val acceptTypes = fileChooserParams?.acceptTypes.orEmpty()
              val isMultiple = fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE

              val isImageOnly = acceptTypes.isNotEmpty() && acceptTypes.any { type ->
                type.contains("image", ignoreCase = true) || type.contains("jpg", ignoreCase = true) || type.contains("png", ignoreCase = true) || type.contains("jpeg", ignoreCase = true)
              } && acceptTypes.none { type ->
                type.contains("video", ignoreCase = true) || type == "*/*" || type == "application/*"
              }

              val isVideoOnly = acceptTypes.isNotEmpty() && acceptTypes.any { type ->
                type.contains("video", ignoreCase = true) || type.contains("mp4", ignoreCase = true)
              } && acceptTypes.none { type ->
                type.contains("image", ignoreCase = true) || type == "*/*"
              }

              val isPhotoOrVideoMedia = isImageOnly || isVideoOnly || (acceptTypes.isNotEmpty() && acceptTypes.any { type ->
                type.contains("image", ignoreCase = true) || type.contains("video", ignoreCase = true)
              })

              // 1. If web page specifically requested image or video media, launch ActivityResultContracts.PickVisualMedia
              if (isPhotoOrVideoMedia) {
                val mediaType = when {
                  isImageOnly -> ActivityResultContracts.PickVisualMedia.ImageOnly
                  isVideoOnly -> ActivityResultContracts.PickVisualMedia.VideoOnly
                  else -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
                }
                try {
                  if (isMultiple) {
                    pickMultipleVisualMediaLauncher.launch(PickVisualMediaRequest(mediaType))
                  } else {
                    pickVisualMediaLauncher.launch(PickVisualMediaRequest(mediaType))
                  }
                  return true
                } catch (_: Exception) {
                  // Fall back to general file launchers below
                }
              }

              // 2. Fallback / General Files (PDFs, Docs, Images, Any files)
              val mimeType = sanitizeMimeType(acceptTypes)
              val validMimes = parseValidMimeTypes(acceptTypes)

              val candidateIntents = mutableListOf<Intent>()

              // Intent A: WebChromeClient native built-in intent if available
              try {
                fileChooserParams?.createIntent()?.let { candidateIntents.add(it) }
              } catch (_: Exception) {}

              // Intent B: ACTION_GET_CONTENT
              candidateIntents.add(Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mimeType
                if (validMimes.size > 1) putExtra(Intent.EXTRA_MIME_TYPES, validMimes)
                if (isMultiple) putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
              })

              // Intent C: ACTION_PICK for MediaStore
              candidateIntents.add(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = if (mimeType.startsWith("image/")) mimeType else "image/*"
                if (isMultiple) putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
              })

              // Intent D: ACTION_OPEN_DOCUMENT (SAF)
              candidateIntents.add(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mimeType
                if (validMimes.size > 1) putExtra(Intent.EXTRA_MIME_TYPES, validMimes)
                if (isMultiple) putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
              })

              // Intent E: Ultimate fallback ACTION_GET_CONTENT */*
              candidateIntents.add(Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
              })

              // Try launching candidate intents until one succeeds
              for (intent in candidateIntents) {
                try {
                  fileChooserLauncher.launch(intent)
                  return true
                } catch (_: Exception) {}
                try {
                  val chooserIntent = Intent.createChooser(intent, "Select File or Photo")
                  fileChooserLauncher.launch(chooserIntent)
                  return true
                } catch (_: Exception) {}
              }

              // If everything fails, safely clear callback and notify user
              try {
                filePathCallback?.onReceiveValue(null)
              } catch (_: Exception) {}
              filePathCallback = null
              Toast.makeText(context, "No app available to choose files", Toast.LENGTH_SHORT).show()
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
                try {
                  request.grant(requestedResources)
                } catch (_: Exception) {}
                return
              }

              val notGranted = requiredPermissions.filter {
                ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED
              }

              if (notGranted.isEmpty()) {
                try {
                  request.grant(requestedResources)
                } catch (_: Exception) {}
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
              try {
                customViewCallback?.onCustomViewHidden()
              } catch (_: Exception) {}
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
              try {
                CookieManager.getInstance().flush()
              } catch (_: Exception) {}
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
          loadUrl(BASE_URL)
        }
      },
      update = { _ -> },
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

private fun sanitizeMimeType(acceptTypes: Array<out String>?): String {
  if (acceptTypes.isNullOrEmpty()) return "*/*"
  val first = acceptTypes.firstOrNull { !it.isNullOrBlank() } ?: return "*/*"
  if (first.contains("/")) return first
  if (first.startsWith(".")) {
    val ext = first.removePrefix(".")
    val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
    if (!mime.isNullOrBlank()) return mime
  }
  return "*/*"
}

private fun parseValidMimeTypes(acceptTypes: Array<out String>?): Array<String> {
  if (acceptTypes.isNullOrEmpty()) return emptyArray()
  val list = mutableListOf<String>()
  for (type in acceptTypes) {
    if (type.isNullOrBlank()) continue
    if (type.contains("/")) {
      list.add(type)
    } else if (type.startsWith(".")) {
      val ext = type.removePrefix(".")
      val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
      if (!mime.isNullOrBlank()) {
        list.add(mime)
      }
    }
  }
  return list.distinct().toTypedArray()
}

// Utility object for file name guessing
private object URLUtil {
  fun guessFileName(url: String, contentDisposition: String?, mimeType: String?): String {
    return android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
  }
}

