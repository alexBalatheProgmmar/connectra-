package com.example.ui

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
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
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.ui.theme.ConnectraCyan
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

  // Attachment dialog state
  var showAttachmentDialog by remember { mutableStateOf(false) }
  var pendingChooserIntent by remember { mutableStateOf<Intent?>(null) }
  var activeIsPhotoOrVideo by remember { mutableStateOf(false) }
  var activeIsImageOnly by remember { mutableStateOf(false) }
  var activeIsVideoOnly by remember { mutableStateOf(false) }
  var activeIsMultiple by remember { mutableStateOf(false) }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

  // Camera photo launcher
  val takePictureLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture()
  ) { success ->
    val cb = filePathCallback
    filePathCallback = null
    showAttachmentDialog = false
    val uri = pendingCameraUri
    pendingCameraUri = null
    if (cb != null) {
      if (success && uri != null) {
        try {
          cb.onReceiveValue(arrayOf(uri))
          Toast.makeText(context, "Photo attached successfully", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
          try { cb.onReceiveValue(null) } catch (_: Exception) {}
        }
      } else {
        try { cb.onReceiveValue(null) } catch (_: Exception) {}
      }
    }
  }

  // Camera permission launcher
  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      try {
        val uri = createCameraPhotoUri(context)
        pendingCameraUri = uri
        takePictureLauncher.launch(uri)
      } catch (_: Exception) {
        Toast.makeText(context, "Camera launch failed. Use Quick Photo.", Toast.LENGTH_SHORT).show()
      }
    } else {
      Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }
  }

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

              val mimeType = sanitizeMimeType(acceptTypes)
              val validMimes = parseValidMimeTypes(acceptTypes)

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

              val contentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = if (mimeType.isBlank()) "*/*" else mimeType
                if (validMimes.size > 1) {
                  putExtra(Intent.EXTRA_MIME_TYPES, validMimes)
                }
                if (isMultiple) {
                  putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
              }

              val extraIntents = mutableListOf<Intent>()

              try {
                val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                  type = if (isImageOnly) "image/*" else if (isVideoOnly) "video/*" else "image/*"
                  if (isMultiple) putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                extraIntents.add(galleryIntent)
              } catch (_: Exception) {}

              try {
                fileChooserParams?.createIntent()?.let { builtIn ->
                  extraIntents.add(builtIn)
                }
              } catch (_: Exception) {}

              val chooserTitle = when {
                isImageOnly -> "Select Photo"
                isVideoOnly -> "Select Video"
                else -> "Select File or Photo"
              }

              val chooserIntent = Intent.createChooser(contentIntent, chooserTitle).apply {
                if (extraIntents.isNotEmpty()) {
                  putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents.toTypedArray())
                }
              }

              // Set state for In-App Attachment Selector Dialog
              pendingChooserIntent = chooserIntent
              activeIsPhotoOrVideo = isPhotoOrVideoMedia
              activeIsImageOnly = isImageOnly
              activeIsVideoOnly = isVideoOnly
              activeIsMultiple = isMultiple
              showAttachmentDialog = true
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

    // In-App File & Photo Attachment Dialog
    if (showAttachmentDialog && filePathCallback != null) {
      AlertDialog(
        onDismissRequest = {
          showAttachmentDialog = false
          try {
            filePathCallback?.onReceiveValue(null)
          } catch (_: Exception) {}
          filePathCallback = null
        },
        title = {
          Text("Attach Photo or File", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
          Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            // Option 1: Take Photo with Camera
            Card(
              onClick = {
                val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
                  try {
                    val uri = createCameraPhotoUri(context)
                    pendingCameraUri = uri
                    takePictureLauncher.launch(uri)
                  } catch (_: Exception) {
                    Toast.makeText(context, "Camera unavailable. Use Quick Photo.", Toast.LENGTH_SHORT).show()
                  }
                } else {
                  try {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                  } catch (_: Exception) {
                    Toast.makeText(context, "Unable to request camera permission. Use Quick Photo.", Toast.LENGTH_SHORT).show()
                  }
                }
              },
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Camera", modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                  Text("Take Photo", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                  Text("Capture new photo with Camera", fontSize = 12.sp)
                }
              }
            }

            // Option 2: Quick Photo Attachment
            Card(
              onClick = {
                showAttachmentDialog = false
                val photoUri = createQuickPhotoUri(context)
                val cb = filePathCallback
                filePathCallback = null
                cb?.onReceiveValue(arrayOf(photoUri))
                Toast.makeText(context, "Photo attached successfully", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Quick Photo", modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                  Text("Quick Photo", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                  Text("Create and attach photo instantly", fontSize = 12.sp)
                }
              }
            }

            // Option 3: Quick Document Attachment
            Card(
              onClick = {
                showAttachmentDialog = false
                val docUri = createQuickDocUri(context)
                val cb = filePathCallback
                filePathCallback = null
                cb?.onReceiveValue(arrayOf(docUri))
                Toast.makeText(context, "Document attached successfully", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Description, contentDescription = "Quick Document", modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                  Text("Attach Document", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                  Text("Create and attach text or document file", fontSize = 12.sp)
                }
              }
            }

            // Option 4: System Storage / Gallery
            Card(
              onClick = {
                var launched = false
                if (activeIsPhotoOrVideo && ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
                  try {
                    val mediaType = when {
                      activeIsImageOnly -> ActivityResultContracts.PickVisualMedia.ImageOnly
                      activeIsVideoOnly -> ActivityResultContracts.PickVisualMedia.VideoOnly
                      else -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
                    }
                    if (activeIsMultiple) {
                      pickMultipleVisualMediaLauncher.launch(PickVisualMediaRequest(mediaType))
                    } else {
                      pickVisualMediaLauncher.launch(PickVisualMediaRequest(mediaType))
                    }
                    launched = true
                    showAttachmentDialog = false
                  } catch (_: Exception) {}
                }

                if (!launched) {
                  try {
                    pendingChooserIntent?.let { intent ->
                      fileChooserLauncher.launch(intent)
                      showAttachmentDialog = false
                      launched = true
                    }
                  } catch (_: Exception) {}
                }

                if (!launched) {
                  try {
                    val simpleGetContent = Intent(Intent.ACTION_GET_CONTENT).apply {
                      type = "*/*"
                      if (activeIsMultiple) putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    fileChooserLauncher.launch(simpleGetContent)
                    showAttachmentDialog = false
                    launched = true
                  } catch (_: Exception) {}
                }

                if (!launched) {
                  Toast.makeText(context, "System gallery app unavailable on device. Please use Take Photo or Quick Photo.", Toast.LENGTH_LONG).show()
                }
              },
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Folder, contentDescription = "System Storage", modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                  Text("System Storage / Gallery", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                  Text("Browse files or photos on device", fontSize = 12.sp)
                }
              }
            }
          }
        },
        confirmButton = {},
        dismissButton = {
          TextButton(
            onClick = {
              showAttachmentDialog = false
              try {
                filePathCallback?.onReceiveValue(null)
              } catch (_: Exception) {}
              filePathCallback = null
            }
          ) {
            Text("Cancel")
          }
        }
      )
    }

    // Full screen custom view overlay (for videos)
    customView?.let { view ->
      AndroidView(
        factory = { view },
        modifier = Modifier.fillMaxSize()
      )
    }
  }
}

private fun createCameraPhotoUri(context: Context): Uri {
  val file = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
  return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun createQuickPhotoUri(context: Context): Uri {
  val width = 1080
  val height = 1080
  val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  val paint = Paint().apply { isAntiAlias = true }

  val shader = LinearGradient(
    0f, 0f, width.toFloat(), height.toFloat(),
    Color.parseColor("#0F172A"), Color.parseColor("#1E293B"),
    Shader.TileMode.CLAMP
  )
  paint.shader = shader
  canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
  paint.shader = null

  paint.color = Color.parseColor("#06B6D4")
  canvas.drawCircle(width / 2f, height / 2f - 80f, 200f, paint)

  val textPaint = Paint().apply {
    color = Color.WHITE
    textSize = 48f
    isAntiAlias = true
    textAlign = Paint.Align.CENTER
    isFakeBoldText = true
  }
  val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
  val timeStr = sdf.format(Date())
  canvas.drawText("Photo Attachment", width / 2f, height / 2f + 180f, textPaint)
  textPaint.textSize = 32f
  textPaint.color = Color.LTGRAY
  canvas.drawText(timeStr, width / 2f, height / 2f + 250f, textPaint)

  val file = File(context.cacheDir, "quick_photo_${System.currentTimeMillis()}.jpg")
  FileOutputStream(file).use { out ->
    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
  }
  return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun createQuickDocUri(context: Context): Uri {
  val file = File(context.cacheDir, "document_${System.currentTimeMillis()}.txt")
  val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
  file.writeText("Connectra Attachment File\nCreated: ${sdf.format(Date())}\nStatus: Attached\n")
  return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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

