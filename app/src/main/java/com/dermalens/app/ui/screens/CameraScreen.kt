package com.dermalens.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.dermalens.app.navigation.Screen
import java.util.concurrent.Executors
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Crops [sourceUri] to exactly what's visible inside the guide box, given how the user has
 * panned/zoomed it. Mirrors the same transform the UI applies (ContentScale.Fit base fit,
 * centered, then the user's additional scale/offset) as an android.graphics.Matrix, inverts it,
 * and maps the guide box's screen corners back into the original bitmap's pixel space.
 */
private fun cropGalleryImageToFrame(
    context: android.content.Context,
    sourceUri: android.net.Uri,
    containerWidthPx: Float,
    containerHeightPx: Float,
    guideBoxSizePx: Float,
    userScale: Float,
    userOffsetX: Float,
    userOffsetY: Float
): android.net.Uri? {
    return try {
        val bitmap = context.contentResolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it) } ?: return null
        val bitmapW = bitmap.width.toFloat()
        val bitmapH = bitmap.height.toFloat()

        val baseScale = minOf(containerWidthPx / bitmapW, containerHeightPx / bitmapH)
        val cx = containerWidthPx / 2f
        val cy = containerHeightPx / 2f

        val matrix = Matrix()
        matrix.postScale(baseScale, baseScale)
        matrix.postTranslate(cx - bitmapW * baseScale / 2f, cy - bitmapH * baseScale / 2f)
        matrix.postScale(userScale, userScale, cx, cy)
        matrix.postTranslate(userOffsetX, userOffsetY)

        val inverse = Matrix()
        if (!matrix.invert(inverse)) return null

        val half = guideBoxSizePx / 2f
        val corners = floatArrayOf(
            cx - half, cy - half,
            cx + half, cy - half,
            cx + half, cy + half,
            cx - half, cy + half
        )
        inverse.mapPoints(corners)

        val xs = floatArrayOf(corners[0], corners[2], corners[4], corners[6])
        val ys = floatArrayOf(corners[1], corners[3], corners[5], corners[7])
        val left = (xs.min().coerceIn(0f, bitmapW)).toInt()
        val top = (ys.min().coerceIn(0f, bitmapH)).toInt()
        val right = (xs.max().coerceIn(0f, bitmapW)).toInt()
        val bottom = (ys.max().coerceIn(0f, bitmapH)).toInt()
        val cropW = (right - left).coerceAtLeast(1)
        val cropH = (bottom - top).coerceAtLeast(1)

        val cropped = Bitmap.createBitmap(bitmap, left, top, cropW, cropH)
        val file = java.io.File(context.cacheDir, "scan_crop_${System.currentTimeMillis()}.jpg")
        java.io.FileOutputStream(file).use { out -> cropped.compress(Bitmap.CompressFormat.JPEG, 92, out) }
        android.net.Uri.fromFile(file)
    } catch (e: Exception) {
        Log.e("DermaLens", "Gallery image crop failed", e)
        null
    }
}

@Composable
fun ScanScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    // Once the user denies without a rationale being showable again, Android won't re-prompt --
    // the only way back in is the system Settings screen.
    var permanentlyDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted && activity != null) {
            permanentlyDenied = !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) CameraPreviewScreen(navController)
    else CameraPermissionDeniedScreen(
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        onOpenSettings = {
            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        },
        permanentlyDenied = permanentlyDenied,
        navController = navController
    )
}

@Composable
fun CameraPreviewScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

    // Pan/zoom for a gallery-picked image, so the user can position it within the guide frame
    // before scanning. Resets whenever a new image is picked.
    var galleryScale by remember { mutableFloatStateOf(1f) }
    var galleryOffsetX by remember { mutableFloatStateOf(0f) }
    var galleryOffsetY by remember { mutableFloatStateOf(0f) }
    var containerSizePx by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val guideBoxSizePx = with(density) { 260.dp.toPx() }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            galleryScale = 1f
            galleryOffsetX = 0f
            galleryOffsetY = 0f
        }
    }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    fun startCamera(frontCamera: Boolean = false) {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetResolution(android.util.Size(1280, 1280))
                .build()
            val cameraSelector = if (frontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, capture)
                imageCapture = capture
            } catch (e: Exception) { Log.e("DermaLens", "Camera binding failed", e) }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(isFrontCamera) { startCamera(isFrontCamera) }

    val scope = rememberCoroutineScope()

    fun capturePhoto(capture: ImageCapture, onCaptured: (android.net.Uri) -> Unit, onFailed: () -> Unit) {
        val photoFile = java.io.File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        capture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = android.net.Uri.fromFile(photoFile)
                    android.os.Handler(android.os.Looper.getMainLooper()).post { onCaptured(uri) }
                }
                override fun onError(exception: ImageCaptureException) {
                    Log.e("DermaLens", "Photo capture failed", exception)
                    android.os.Handler(android.os.Looper.getMainLooper()).post { onFailed() }
                }
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black)
            .onSizeChanged { containerSizePx = it }
    ) {

        if (selectedImageUri != null) {
            AsyncImage(
                model = selectedImageUri,
                contentDescription = "Selected image (pinch to zoom, drag to pan)",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = galleryScale,
                        scaleY = galleryScale,
                        translationX = galleryOffsetX,
                        translationY = galleryOffsetY
                    )
                    .pointerInput(selectedImageUri) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            galleryScale = (galleryScale * zoom).coerceIn(1f, 5f)
                            galleryOffsetX += pan.x
                            galleryOffsetY += pan.y
                        }
                    },
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } else {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(camera) {
                        detectTransformGestures { _, _, zoom, _ ->
                            val cam = camera ?: return@detectTransformGestures
                            val zoomState = cam.cameraInfo.zoomState.value ?: return@detectTransformGestures
                            val newRatio = (zoomState.zoomRatio * zoom)
                                .coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                            cam.cameraControl.setZoomRatio(newRatio)
                        }
                    }
            )
        }

        // Dark overlay at top and bottom
        Box(modifier = Modifier.fillMaxWidth().height(120.dp).align(Alignment.TopCenter).background(Color.Black.copy(alpha = 0.5f)))
        Box(modifier = Modifier.fillMaxWidth().height(180.dp).align(Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.6f)))

        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 48.dp).align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.size(42.dp).background(Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Skin Scan", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Position skin within frame", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }

            IconButton(
                onClick = { isFlashOn = !isFlashOn; camera?.cameraControl?.enableTorch(isFlashOn) },
                modifier = Modifier.size(42.dp).background(
                    if (isFlashOn) DermaGreen else Color.White.copy(alpha = 0.15f), CircleShape
                )
            ) {
                Icon(
                    if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Flash",
                    tint = Color.White
                )
            }
        }

        // Scan Frame
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.Center)
                .border(2.dp, DermaGreen, RoundedCornerShape(24.dp))
        )

        // Bottom Controls
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isScanning) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = DermaGreen, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Analyzing skin...", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flip camera
                IconButton(
                    onClick = { isFrontCamera = !isFrontCamera },
                    modifier = Modifier.size(52.dp).background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip", tint = Color.White, modifier = Modifier.size(24.dp))
                }

                // Capture button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .border(3.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(DermaGreen),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            val galleryUri = selectedImageUri
                            if (galleryUri != null) {
                                isScanning = true
                                scope.launch {
                                    val croppedUri = withContext(Dispatchers.IO) {
                                        cropGalleryImageToFrame(
                                            context, galleryUri,
                                            containerSizePx.width.toFloat(), containerSizePx.height.toFloat(),
                                            guideBoxSizePx, galleryScale, galleryOffsetX, galleryOffsetY
                                        )
                                    }
                                    isScanning = false
                                    navController.navigate(Screen.ScanResult.createRoute((croppedUri ?: galleryUri).toString()))
                                }
                            } else {
                                val capture = imageCapture
                                if (capture != null) {
                                    isScanning = true
                                    capturePhoto(
                                        capture,
                                        onCaptured = { uri ->
                                            isScanning = false
                                            navController.navigate(Screen.ScanResult.createRoute(uri.toString()))
                                        },
                                        onFailed = { isScanning = false }
                                    )
                                }
                            }
                        },
                        enabled = !isScanning
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Scan", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }

                // Gallery
                IconButton(
                    onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.size(52.dp).background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Tap to scan your skin", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
    }
}

@Composable
fun CameraPermissionDeniedScreen(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    permanentlyDenied: Boolean,
    navController: NavController
) {
    Scaffold(bottomBar = { DermaBottomNavBar(navController) }) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp).background(Color.White),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(88.dp).clip(RoundedCornerShape(24.dp)).background(DermaGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(44.dp), tint = DermaGreen)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Camera Access Required", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                if (permanentlyDenied)
                    "Camera access was denied and can no longer be requested from within the app. Please enable it from Settings to continue."
                else
                    "DermaLens needs camera access to scan your skin for conditions. Please grant camera permission to continue.",
                fontSize = 14.sp, color = Color(0xFF6B7280), textAlign = TextAlign.Center, lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = if (permanentlyDenied) onOpenSettings else onRequestPermission,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DermaGreen)
            ) {
                Text(if (permanentlyDenied) "Open Settings" else "Grant Camera Permission", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, Color(0xFFE5E7EB))
            ) {
                Text("Go Back", color = Color(0xFF374151))
            }
        }
    }
}