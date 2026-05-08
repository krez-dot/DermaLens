package com.dermalens.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

@Composable
fun ScanScreen(navController: NavController) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
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

        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(40.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Skin Scan", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            IconButton(
                onClick = { isFlashOn = !isFlashOn; camera?.cameraControl?.enableTorch(isFlashOn) },
                modifier = Modifier.size(40.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff, contentDescription = "Flash", tint = if (isFlashOn) Color.Yellow else Color.White)
            }
        }

        // Scan Frame
        Box(modifier = Modifier.size(260.dp).align(Alignment.Center).border(2.dp, DermaGreen, RoundedCornerShape(24.dp)))

        Text(
            text = "Position affected skin area within the frame",
            color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center).offset(y = 160.dp).padding(horizontal = 32.dp)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
        )

        // Bottom Controls
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.6f)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isScanning) {
                CircularProgressIndicator(color = DermaGreen, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Analyzing skin...", color = Color.White, fontSize = 14.sp)
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { isFrontCamera = !isFrontCamera }, modifier = Modifier.size(52.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip", tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                    Box(modifier = Modifier.size(72.dp).background(DermaGreen, CircleShape).border(4.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                        IconButton(onClick = {
                            isScanning = true
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                isScanning = false
                                navController.navigate(Screen.ScanResult.route)
                            }, 2000)
                        }) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Scan", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                    IconButton(onClick = { }, modifier = Modifier.size(52.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Tap the button to scan your skin", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun CameraPermissionDeniedScreen(onRequestPermission: () -> Unit, navController: NavController) {
    Scaffold(bottomBar = { DermaBottomNavBar(navController) }) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))
            Text("Camera Permission Required", fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Text("DermaLens needs camera access to scan your skin for conditions. Please grant camera permission to continue.", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = DermaGreen)) {
                Text("Grant Camera Permission", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) {
                Text("Go Back")
            }
        }
    }
}