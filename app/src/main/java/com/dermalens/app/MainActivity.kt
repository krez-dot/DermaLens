package com.dermalens.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dermalens.app.navigation.DermaLensNavGraph
import com.dermalens.app.ui.theme.DermaLensTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DermaLensTheme {
                DermaLensNavGraph()
            }
        }
    }
}