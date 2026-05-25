package com.dermalens.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.dermalens.app.navigation.Screen
import java.util.concurrent.Executors
import androidx.compose.foundation.BorderStroke

@Composable
fun ScanScreen(navController: NavController) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) CameraPreviewScreen(navController)
    else CameraPermissionDeniedScreen(onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }, navController = navController)
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
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) selectedImageUri = uri }

    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    fun startCamera(frontCamera: Boolean = false) {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val cameraSelector = if (frontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            } catch (e: Exception) { Log.e("DermaLens", "Camera binding failed", e) }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(isFrontCamera) { startCamera(isFrontCamera) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        if (selectedImageUri != null) {
            AsyncImage(
                model = selectedImageUri,
                contentDescription = "Selected image",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
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
                            isScanning = true
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                isScanning = false
                                navController.navigate(Screen.ScanResult.createRoute(selectedImageUri?.toString()))
                            }, 2000)
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
fun CameraPermissionDeniedScreen(onRequestPermission: () -> Unit, navController: NavController) {
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
            Text("DermaLens needs camera access to scan your skin for conditions. Please grant camera permission to continue.", fontSize = 14.sp, color = Color(0xFF6B7280), textAlign = TextAlign.Center, lineHeight = 22.sp)
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DermaGreen)
            ) {
                Text("Grant Camera Permission", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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