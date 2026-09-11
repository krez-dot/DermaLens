package com.dermalens.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dermalens.app.ui.LocalAppSettings

/**
 * Static reference screen showing a detected condition's real clinical relatives/subtypes --
 * education, not a detection capability. Nothing here implies the app can tell these apart on a
 * scan; that distinction matters since it's shown right after a real detection result.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyTreeScreen(navController: NavController, condition: String) {
    val settings = LocalAppSettings.current
    val tree = familyTrees[condition]

    Scaffold(
        topBar = {
            DermaGlassTopBar(
                title = "$condition Family Tree",
                onBack = { navController.popBackStack() },
                titleColor = settings.textPrimary
            )
        }
    ) { innerPadding ->
        if (tree == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No family tree reference is available for $condition yet.",
                    fontSize = settings.textMd.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(24.dp)
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .background(if (settings.highContrast) Color.White else Color(0xFFF8F9FA)),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                ResultCard(icon = Icons.Default.Info, iconBg = Color(0xFFEFF6FF), iconTint = Color(0xFF2563EB), title = "Why these are grouped together") {
                    Text(tree.groupingNote, fontSize = settings.textMd.sp, color = Color(0xFF374151), lineHeight = 22.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Reference only -- this app doesn't detect which of these a photo shows. See a dermatologist for an actual diagnosis.",
                    fontSize = settings.textSm.sp,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(tree.relatives) { relative ->
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                    // A simple branch indicator -- a dot and a short stem -- gives the "tree" a
                    // visual identity without needing a full custom-drawn diagram.
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp).padding(top = 18.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(DermaGreen))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(relative.name, fontSize = settings.textMd.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(relative.description, fontSize = settings.textSm.sp, color = Color(0xFF4B5563), lineHeight = 18.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                Icon(Icons.Default.AccountTree, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(relative.distinguishingFeature, fontSize = settings.textSm.sp, color = Color(0xFF6B7280), lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
