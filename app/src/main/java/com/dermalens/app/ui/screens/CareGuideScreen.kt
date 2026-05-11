package com.dermalens.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dermalens.app.ui.LocalAppSettings

data class CareGuideData(
    val condition: String,
    val emoji: String,
    val color: Color,
    val overview: String,
    val routine: List<String>,
    val dos: List<String>,
    val donts: List<String>,
    val treatments: List<String>
)

val careGuideList = listOf(
    CareGuideData("Acne Vulgaris", "🔴", Color(0xFFE53935),
        "Acne vulgaris occurs when hair follicles become clogged with oil and dead skin cells. It is most common in teenagers but can affect people of all ages. Proper skincare and lifestyle changes can significantly reduce breakouts.",
        listOf("Morning: Gentle foaming cleanser → lightweight moisturizer → SPF 30+ sunscreen", "Evening: Oil-free cleanser → toner (optional) → spot treatment → non-comedogenic moisturizer", "Weekly: Gentle exfoliation (1-2x per week)"),
        listOf("Wash your face twice daily with a gentle cleanser", "Use non-comedogenic (non-pore-clogging) products", "Apply sunscreen daily", "Change pillowcases frequently (every 2-3 days)", "Stay hydrated and maintain a balanced diet"),
        listOf("Don't pick, pop, or squeeze pimples — it causes scarring", "Don't over-wash your face — it can irritate skin", "Don't use heavy, oily makeup or skincare products", "Don't touch your face frequently throughout the day"),
        listOf("Benzoyl peroxide (OTC) — kills acne-causing bacteria", "Salicylic acid (OTC) — unclogs pores", "Retinoids (prescription) — promotes skin cell turnover", "Antibiotics (prescription) — reduces inflammation")
    ),
    CareGuideData("Atopic Dermatitis", "🟠", Color(0xFFFF9800),
        "Atopic dermatitis (eczema) is a chronic condition causing dry, itchy, and inflamed skin. It often appears in childhood and can flare up periodically. Managing triggers and keeping skin moisturized are key.",
        listOf("Morning: Gentle fragrance-free cleanser → thick moisturizer immediately after bathing", "Evening: Lukewarm bath (10-15 min) → pat dry gently → apply topical treatment → seal with moisturizer", "Throughout day: Reapply moisturizer whenever skin feels dry"),
        listOf("Moisturize immediately after bathing while skin is still damp", "Use fragrance-free, dye-free skincare products", "Wear soft, breathable fabrics like cotton", "Keep fingernails short to minimize scratching damage"),
        listOf("Don't take hot showers or baths — use lukewarm water", "Don't use harsh soaps or fragranced products", "Don't scratch affected areas — it worsens inflammation", "Don't wear rough or synthetic fabrics"),
        listOf("Topical corticosteroids (prescription) — reduces inflammation", "Topical calcineurin inhibitors (prescription) — non-steroidal", "Dupilumab (prescription injection) — for moderate to severe eczema", "Antihistamines — helps with itching at night")
    ),
    CareGuideData("Melasma", "🟤", Color(0xFF795548),
        "Melasma causes brown or grayish-brown patches on the skin, usually on the face. It is more common in women and is associated with hormonal changes and sun exposure. While harmless, it can affect self-confidence.",
        listOf("Morning: Gentle cleanser → vitamin C serum → moisturizer → SPF 50+ sunscreen (most important!)", "Evening: Double cleanse → brightening serum (niacinamide) → moisturizer", "Weekly: Gentle chemical exfoliant (AHA/BHA) to promote cell turnover"),
        listOf("Apply broad-spectrum SPF 50+ sunscreen every day — even indoors", "Reapply sunscreen every 2 hours when outdoors", "Wear wide-brimmed hats and protective clothing", "Use gentle brightening ingredients like niacinamide and vitamin C"),
        listOf("Don't skip sunscreen — UV exposure is the #1 trigger", "Don't use harsh scrubs — they worsen pigmentation", "Don't pick at or scratch the affected areas", "Don't expose skin to excessive heat (saunas, hot yoga)"),
        listOf("Hydroquinone (prescription) — gold standard skin-lightening", "Tretinoin (prescription) — promotes skin cell renewal", "Azelaic acid (OTC/prescription) — gentle brightening", "Chemical peels (dermatologist) — removes pigmented layers")
    ),
    CareGuideData("Tinea", "🟢", Color(0xFF4CAF50),
        "Tinea is a fungal skin infection that can affect different body parts. It appears as a ring-shaped, scaly rash that is often itchy. It spreads through direct contact with infected skin or objects.",
        listOf("Morning: Wash affected area with antifungal soap → dry thoroughly → apply antifungal cream", "Evening: Cleanse area again → ensure completely dry → apply antifungal treatment", "Throughout day: Keep area dry and clean, especially after sweating"),
        listOf("Keep the affected area clean and completely dry", "Apply antifungal cream as directed by your doctor", "Wash towels and clothing frequently in hot water", "Wear loose-fitting, breathable clothing"),
        listOf("Don't share towels, clothing, or personal items", "Don't walk barefoot in public showers or pools", "Don't wear tight-fitting clothing over affected areas", "Don't stop treatment early — fungal infections can return"),
        listOf("Clotrimazole (OTC cream) — commonly used antifungal", "Miconazole (OTC cream) — effective for skin infections", "Terbinafine (OTC/prescription) — kills fungal cells", "Fluconazole (prescription oral) — for widespread infections")
    ),
    CareGuideData("Warts", "🟣", Color(0xFF9C27B0),
        "Warts are benign skin growths caused by the human papillomavirus (HPV). They can appear anywhere on the body and are usually harmless. Most warts resolve on their own but treatment can speed up removal.",
        listOf("Daily: Keep wart covered with a bandage to prevent spreading", "If using salicylic acid: Soak wart in warm water → file dead skin → apply treatment → cover overnight", "Weekly: Gently file away softened dead skin with a clean nail file"),
        listOf("Keep warts covered to prevent spreading", "Wash hands thoroughly after touching warts", "Consult a dermatologist if wart is painful or spreading", "Boost your immune system — healthy lifestyle helps clear HPV"),
        listOf("Don't pick, scratch, or bite warts — it spreads the virus", "Don't share towels, razors, or personal items", "Don't walk barefoot in public areas if you have plantar warts", "Don't try to cut or burn warts at home"),
        listOf("Salicylic acid (OTC) — dissolves wart tissue over time", "Cryotherapy (dermatologist) — freezes wart with liquid nitrogen", "Cantharidin (dermatologist) — causes blister under wart", "Laser treatment (dermatologist) — destroys wart blood vessels")
    ),
    CareGuideData("Scabies", "🔴", Color(0xFFF44336),
        "Scabies is a highly contagious skin infestation caused by a tiny mite. It causes intense itching and a pimple-like rash. It spreads through prolonged skin-to-skin contact and requires prompt medical treatment.",
        listOf("During treatment: Apply prescribed scabicide cream from neck down → leave 8-14 hours → rinse", "After treatment: Continue skincare to soothe irritated skin", "Decontamination: Wash all clothing and bedding in hot water (60°C+) on treatment day"),
        listOf("Seek immediate medical treatment — requires prescription medication", "Treat all household members and close contacts simultaneously", "Wash all clothing and bedding in hot water on treatment day", "Vacuum furniture, carpets, and mattresses thoroughly"),
        listOf("Don't share clothing, bedding, or towels with others", "Don't have close skin contact with others until treatment is complete", "Don't scratch — it can cause secondary bacterial infections", "Don't delay treatment — scabies spreads rapidly"),
        listOf("Permethrin 5% cream (prescription) — first-line treatment", "Ivermectin (prescription oral) — for crusted scabies", "Benzyl benzoate (prescription) — alternative topical", "Antihistamines — relieves itching during treatment")
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareGuideScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }
    val guide = careGuideList[selectedTab]
    val settings = LocalAppSettings.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Care Guide", fontWeight = FontWeight.Bold, fontSize = settings.textXl.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White, titleContentColor = settings.textPrimary)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).background(if (settings.highContrast) Color.White else Color(0xFFF8F9FA))) {

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = DermaGreen,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = DermaGreen)
                }
            ) {
                careGuideList.forEachIndexed { index, item ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        modifier = Modifier.semantics { contentDescription = "Care guide for ${item.condition}" },
                        text = {
                            Text(
                                "${item.emoji} ${item.condition.split(" ").first()}",
                                fontSize = settings.textBase.sp,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selectedTab == index) DermaGreen else if (settings.highContrast) Color(0xFF444444) else Color(0xFF6B7280)
                            )
                        }
                    )
                }
            }

            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // Banner
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Brush.verticalGradient(colors = listOf(guide.color.copy(alpha = 0.85f), guide.color)))
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .then(if (settings.highContrast) Modifier.border(1.dp, Color.White, RoundedCornerShape(16.dp)) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(guide.emoji, fontSize = 28.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(guide.condition, fontSize = settings.textXxl.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Skincare Guide", fontSize = settings.textBase.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    CareSection(icon = Icons.Default.Info, iconBg = DermaGreenLight, iconTint = DermaGreen, title = "Overview") {
                        Text(guide.overview, fontSize = settings.textMd.sp, color = settings.textPrimary, lineHeight = 22.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    CareSection(icon = Icons.Default.WbSunny, iconBg = Color(0xFFFEF3C7), iconTint = Color(0xFFD97706), title = "Skincare Routine") {
                        guide.routine.forEachIndexed { index, step ->
                            Row(modifier = Modifier.padding(vertical = 5.dp).semantics { contentDescription = "Step ${index + 1}: $step" }, verticalAlignment = Alignment.Top) {
                                Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(DermaGreen), contentAlignment = Alignment.Center) {
                                    Text("${index + 1}", fontSize = settings.textSm.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(step, fontSize = settings.textBase.sp, color = settings.textPrimary, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    CareSection(icon = Icons.Default.CheckCircle, iconBg = Color(0xFFF0FDF4), iconTint = Color(0xFF16A34A), title = "Do's") {
                        guide.dos.forEach { item ->
                            Row(modifier = Modifier.padding(vertical = 4.dp).semantics { contentDescription = "Do: $item" }, verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier.size(18.dp).clip(CircleShape)
                                        .background(if (settings.highContrast) Color(0xFFBBF7D0) else Color(0xFFDCFCE7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(12.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(item, fontSize = settings.textBase.sp, color = settings.textPrimary, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    CareSection(icon = Icons.Default.Cancel, iconBg = Color(0xFFFEF2F2), iconTint = Color(0xFFDC2626), title = "Don'ts") {
                        guide.donts.forEach { item ->
                            Row(modifier = Modifier.padding(vertical = 4.dp).semantics { contentDescription = "Don't: $item" }, verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier.size(18.dp).clip(CircleShape)
                                        .background(if (settings.highContrast) Color(0xFFFECACA) else Color(0xFFFEE2E2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(12.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(item, fontSize = settings.textBase.sp, color = settings.textPrimary, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    CareSection(icon = Icons.Default.LocalPharmacy, iconBg = Color(0xFFEFF6FF), iconTint = Color(0xFF2563EB), title = "Treatment Options") {
                        guide.treatments.forEach { treatment ->
                            Row(modifier = Modifier.padding(vertical = 4.dp).semantics { contentDescription = "Treatment: $treatment" }, verticalAlignment = Alignment.Top) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(guide.color).padding(top = 7.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(treatment, fontSize = settings.textBase.sp, color = settings.textPrimary, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (settings.highContrast) Color(0xFFFFE0B2) else Color(0xFFFFF7ED))
                            .then(if (settings.highContrast) Modifier.border(1.dp, Color(0xFFE65100), RoundedCornerShape(12.dp)) else Modifier)
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("⚕️", fontSize = settings.textMd.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("This guide is for informational purposes only. Always consult a licensed dermatologist for proper diagnosis and treatment.", fontSize = settings.textBase.sp, color = Color(0xFF92400E), lineHeight = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun CareSection(icon: ImageVector, iconBg: Color, iconTint: Color, title: String, content: @Composable ColumnScope.() -> Unit) {
    val settings = LocalAppSettings.current
    Card(
        modifier = Modifier.fillMaxWidth()
            .then(if (settings.highContrast) Modifier.border(1.dp, Color.Black, RoundedCornerShape(16.dp)) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (settings.highContrast) Color(0xFFF0F0F0) else Color.White),
        elevation = CardDefaults.cardElevation(if (settings.highContrast) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, fontSize = settings.textLg.sp, fontWeight = FontWeight.SemiBold, color = settings.textPrimary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}