package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ui.ConnectraApp
import com.example.ui.theme.ConnectraTheme

class MainActivity : ComponentActivity() {

  companion object {
    private const val TAG = "MainActivity"
    private const val PERMISSIONS_REQUEST_CODE = 1001
  }

  // Secure image picker launcher using ActivityResultContracts.PickVisualMedia
  private lateinit var pickVisualMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>
  private var onImagePickedListener: ((Uri?) -> Unit)? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Register ActivityResultContracts.PickVisualMedia launcher safely during activity creation
    pickVisualMediaLauncher = registerForActivityResult(
      ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
      if (uri != null) {
        Log.d(TAG, "Successfully picked image URI: $uri")
        Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show()
      } else {
        Log.d(TAG, "No image selected")
      }
      onImagePickedListener?.invoke(uri)
      onImagePickedListener = null
    }

    checkAndRequestPermissions()

    setContent {
      ConnectraTheme {
        ConnectraApp()
      }
    }
  }

  /**
   * Triggers the secure image picker upon user UI interaction.
   * PickVisualMedia does not require storage permissions, avoiding startup crashes.
   */
  fun launchImagePicker(onResult: ((Uri?) -> Unit)? = null) {
    onImagePickedListener = onResult
    try {
      pickVisualMediaLauncher.launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
      )
    } catch (e: Exception) {
      Log.e(TAG, "Error launching PickVisualMedia", e)
      Toast.makeText(this, "Unable to launch photo picker", Toast.LENGTH_SHORT).show()
      onResult?.invoke(null)
    }
  }

  private fun checkAndRequestPermissions() {
    val permissionsToRequest = mutableListOf<String>()

    // Only request notification permissions at startup on Android 13+.
    // Storage/media permissions are NOT requested on startup to prevent crashes or unnecessary prompts,
    // as PickVisualMedia handles media picking securely on user demand.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
      ) {
        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
      }
    }

    if (permissionsToRequest.isNotEmpty()) {
      ActivityCompat.requestPermissions(
        this,
        permissionsToRequest.toTypedArray(),
        PERMISSIONS_REQUEST_CODE
      )
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Connectra $name", modifier = modifier)
}



