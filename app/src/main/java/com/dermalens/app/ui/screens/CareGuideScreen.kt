package com.dermalens.app.ui.screens

import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// ── Care Guide Data ───────────────────────────────────────────────────────────
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
    CareGuideData(
        condition = "Acne Vulgaris",
        emoji = "🔴",
        color = Color(0xFFE53935),
        overview = "Acne vulgaris occurs when hair follicles become clogged with oil and dead skin cells. It is most common in teenagers but can affect people of all ages. Proper skincare and lifestyle changes can significantly reduce breakouts.",
        routine = listOf(
            "Morning: Gentle foaming cleanser → lightweight moisturizer → SPF 30+ sunscreen",
            "Evening: Oil-free cleanser → toner (optional) → spot treatment → non-comedogenic moisturizer",
            "Weekly: Gentle exfoliation (1-2x per week)"
        ),
        dos = listOf(
            "Wash your face twice daily with a gentle cleanser",
            "Use non-comedogenic (non-pore-clogging) products",
            "Apply sunscreen daily — some acne treatments increase sun sensitivity",
            "Change pillowcases frequently (every 2-3 days)",
            "Stay hydrated and maintain a balanced diet",
            "Consult a dermatologist for persistent acne"
        ),
        donts = listOf(
            "Don't pick, pop, or squeeze pimples — it causes scarring",
            "Don't over-wash your face — it can irritate skin and worsen acne",
            "Don't use heavy, oily makeup or skincare products",
            "Don't touch your face frequently throughout the day",
            "Don't skip moisturizer — even oily skin needs hydration"
        ),
        treatments = listOf(
            "Benzoyl peroxide (OTC) — kills acne-causing bacteria",
            "Salicylic acid (OTC) — unclogs pores and reduces inflammation",
            "Retinoids (prescription) — promotes skin cell turnover",
            "Antibiotics (prescription) — reduces bacteria and inflammation",
            "Isotretinoin (prescription) — for severe, cystic acne"
        )
    ),
    CareGuideData(
        condition = "Atopic Dermatitis",
        emoji = "🟠",
        color = Color(0xFFFF9800),
        overview = "Atopic dermatitis (eczema) is a chronic condition causing dry, itchy, and inflamed skin. It often appears in childhood and can flare up periodically. Managing triggers and keeping skin moisturized are key to controlling symptoms.",
        routine = listOf(
            "Morning: Gentle fragrance-free cleanser → thick moisturizer or emollient immediately after bathing",
            "Evening: Lukewarm bath (10-15 min) → pat dry gently → apply prescribed topical treatment → seal with thick moisturizer",
            "Throughout day: Reapply moisturizer whenever skin feels dry"
        ),
        dos = listOf(
            "Moisturize immediately after bathing while skin is still damp",
            "Use fragrance-free, dye-free skincare and laundry products",
            "Wear soft, breathable fabrics like cotton",
            "Keep fingernails short to minimize skin damage from scratching",
            "Use a humidifier in dry environments",
            "Identify and avoid personal triggers (dust, pet dander, certain foods)"
        ),
        donts = listOf(
            "Don't take hot showers or baths — use lukewarm water",
            "Don't use harsh soaps, detergents, or fragranced products",
            "Don't scratch affected areas — it worsens inflammation",
            "Don't wear rough or synthetic fabrics like wool or polyester",
            "Don't introduce multiple new products at once"
        ),
        treatments = listOf(
            "Topical corticosteroids (prescription) — reduces inflammation",
            "Topical calcineurin inhibitors (prescription) — non-steroidal anti-inflammatory",
            "Dupilumab (prescription injection) — for moderate to severe eczema",
            "Antihistamines — helps with itching, especially at night",
            "Wet wrap therapy — for severe flare-ups under medical supervision"
        )
    ),
    CareGuideData(
        condition = "Melasma",
        emoji = "🟤",
        color = Color(0xFF795548),
        overview = "Melasma causes brown or grayish-brown patches on the skin, usually on the face. It is more common in women and is associated with hormonal changes and sun exposure. While it is harmless, it can affect self-confidence.",
        routine = listOf(
            "Morning: Gentle cleanser → vitamin C serum (brightening) → moisturizer → SPF 50+ broad-spectrum sunscreen (most important step!)",
            "Evening: Double cleanse → brightening serum (niacinamide or kojic acid) → moisturizer",
            "Weekly: Gentle chemical exfoliant (AHA/BHA) to promote cell turnover"
        ),
        dos = listOf(
            "Apply broad-spectrum SPF 50+ sunscreen every day — even indoors",
            "Reapply sunscreen every 2 hours when outdoors",
            "Wear wide-brimmed hats and protective clothing outdoors",
            "Use gentle, brightening ingredients like niacinamide and vitamin C",
            "Be patient — melasma treatment takes months to show results",
            "Consult a dermatologist for prescription-strength treatments"
        ),
        donts = listOf(
            "Don't skip sunscreen — UV exposure is the #1 trigger for melasma",
            "Don't use harsh scrubs or irritating products — they worsen pigmentation",
            "Don't pick at or scratch the affected areas",
            "Don't use unregulated skin-lightening products with mercury or steroids",
            "Don't expose skin to excessive heat (saunas, hot yoga) — heat worsens melasma"
        ),
        treatments = listOf(
            "Hydroquinone (prescription) — gold standard skin-lightening agent",
            "Tretinoin (prescription) — promotes skin cell renewal",
            "Azelaic acid (OTC/prescription) — gentle brightening agent",
            "Chemical peels (dermatologist) — removes pigmented skin layers",
            "Laser therapy (dermatologist) — targets deep pigmentation"
        )
    ),
    CareGuideData(
        condition = "Tinea",
        emoji = "🟢",
        color = Color(0xFF4CAF50),
        overview = "Tinea is a fungal skin infection that can affect different body parts. It appears as a ring-shaped, scaly rash that is often itchy. It spreads through direct contact with infected skin, objects, or animals.",
        routine = listOf(
            "Morning: Wash affected area with antifungal soap → dry thoroughly → apply antifungal cream as prescribed",
            "Evening: Cleanse area again → ensure completely dry → apply antifungal treatment",
            "Throughout day: Keep area dry and clean, especially after sweating"
        ),
        dos = listOf(
            "Keep the affected area clean and completely dry",
            "Apply antifungal cream as directed by your doctor",
            "Wash towels, clothing, and bedding frequently in hot water",
            "Wear loose-fitting, breathable clothing",
            "Use separate towels for the affected area",
            "Complete the full course of antifungal treatment even if symptoms improve"
        ),
        donts = listOf(
            "Don't share towels, clothing, or personal items",
            "Don't walk barefoot in public showers, pools, or locker rooms",
            "Don't wear tight-fitting or synthetic clothing over affected areas",
            "Don't stop treatment early — fungal infections can return",
            "Don't scratch the affected area — it can spread the infection"
        ),
        treatments = listOf(
            "Clotrimazole (OTC cream) — commonly used antifungal",
            "Miconazole (OTC cream) — effective for skin fungal infections",
            "Terbinafine (OTC/prescription) — kills fungal cells",
            "Fluconazole (prescription oral) — for widespread or resistant infections",
            "Griseofulvin (prescription oral) — for scalp or nail tinea"
        )
    ),
    CareGuideData(
        condition = "Warts",
        emoji = "🟣",
        color = Color(0xFF9C27B0),
        overview = "Warts are benign skin growths caused by the human papillomavirus (HPV). They can appear anywhere on the body and vary in appearance. Most warts are harmless and may resolve on their own, but treatment can speed up removal.",
        routine = listOf(
            "Daily: Keep wart covered with a bandage to prevent spreading",
            "If using salicylic acid: Soak wart in warm water (5 min) → file dead skin → apply treatment → cover overnight",
            "Weekly: Gently file away softened dead skin with a clean nail file (use only for warts)"
        ),
        dos = listOf(
            "Keep warts covered to prevent spreading to others or other body parts",
            "Wash hands thoroughly after touching warts",
            "Use a dedicated nail file only for filing the wart — discard after use",
            "Consult a dermatologist if wart is painful, spreading, or on face/genitals",
            "Boost your immune system — healthy lifestyle helps clear HPV faster",
            "Be patient — wart treatment can take weeks to months"
        ),
        donts = listOf(
            "Don't pick, scratch, or bite warts — it spreads the virus",
            "Don't share towels, razors, or personal items",
            "Don't walk barefoot in public areas if you have plantar warts",
            "Don't try to cut or burn warts at home",
            "Don't shave over warts — it spreads HPV to other areas"
        ),
        treatments = listOf(
            "Salicylic acid (OTC) — dissolves wart tissue over time",
            "Cryotherapy (dermatologist) — freezes wart with liquid nitrogen",
            "Cantharidin (dermatologist) — causes blister to form under wart",
            "Electrosurgery (dermatologist) — burns wart with electric current",
            "Laser treatment (dermatologist) — destroys wart blood vessels"
        )
    ),
    CareGuideData(
        condition = "Scabies",
        emoji = "🔴",
        color = Color(0xFFF44336),
        overview = "Scabies is a highly contagious skin infestation caused by the Sarcoptes scabiei mite. It causes intense itching and a pimple-like rash. It spreads through prolonged skin-to-skin contact and requires prompt medical treatment.",
        routine = listOf(
            "During treatment: Apply prescribed scabicide cream from neck down → leave for 8-14 hours → rinse thoroughly",
            "After treatment: Continue skincare routine to soothe irritated skin",
            "Decontamination: Wash all clothing, bedding, and towels in hot water (60°C+) on treatment day"
        ),
        dos = listOf(
            "Seek immediate medical treatment — scabies requires prescription medication",
            "Treat all household members and close contacts simultaneously",
            "Wash all clothing, bedding, and towels in hot water on treatment day",
            "Vacuum furniture, carpets, and mattresses thoroughly",
            "Place items that cannot be washed in sealed plastic bags for 72 hours",
            "Follow up with your doctor if symptoms persist after treatment"
        ),
        donts = listOf(
            "Don't share clothing, bedding, or towels with others",
            "Don't have close skin contact with others until treatment is complete",
            "Don't scratch — it can cause secondary bacterial infections",
            "Don't use OTC creams as the only treatment — prescription medication is needed",
            "Don't delay treatment — scabies spreads rapidly to others"
        ),
        treatments = listOf(
            "Permethrin 5% cream (prescription) — first-line treatment, applied to whole body",
            "Ivermectin (prescription oral) — for crusted scabies or treatment failures",
            "Benzyl benzoate (prescription) — alternative topical treatment",
            "Antihistamines — relieves itching during and after treatment",
            "Topical corticosteroids — manages post-scabies itch after mites are eliminated"
        )
    )
)

// ── Care Guide Screen ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareGuideScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }
    val guide = careGuideList[selectedTab]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Care Guide", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1a1a1a)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FA))
        ) {
            // ── Condition Tabs ────────────────────────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = DermaGreen,
                edgePadding = 8.dp
            ) {
                careGuideList.forEachIndexed { index, item ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = "${item.emoji} ${item.condition.split(" ").first()}",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) DermaGreen else Color.Gray
                            )
                        }
                    )
                }
            }

            // ── Content ───────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(guide.color.copy(alpha = 0.8f), guide.color)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text(guide.emoji, fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(guide.condition, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Skincare Guide", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                    // Overview
                    CareGuideSection(
                        icon = Icons.Default.Info,
                        title = "Overview",
                        iconBg = DermaGreenLight,
                        iconTint = DermaGreen
                    ) {
                        Text(guide.overview, fontSize = 14.sp, color = Color(0xFF444444), lineHeight = 22.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Daily Routine
                    CareGuideSection(
                        icon = Icons.Default.WbSunny,
                        title = "Skincare Routine",
                        iconBg = Color(0xFFFFF9C4),
                        iconTint = Color(0xFFF9A825)
                    ) {
                        guide.routine.forEachIndexed { index, step ->
                            Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier.size(22.dp).clip(CircleShape).background(DermaGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${index + 1}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(step, fontSize = 13.sp, color = Color(0xFF444444), lineHeight = 20.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Do's
                    CareGuideSection(
                        icon = Icons.Default.CheckCircle,
                        title = "Do's ✅",
                        iconBg = Color(0xFFE8F5E9),
                        iconTint = Color(0xFF2E7D32)
                    ) {
                        guide.dos.forEach { item ->
                            Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                                Text("✅", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(item, fontSize = 13.sp, color = Color(0xFF444444), lineHeight = 20.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Don'ts
                    CareGuideSection(
                        icon = Icons.Default.Cancel,
                        title = "Don'ts ❌",
                        iconBg = Color(0xFFFFEBEE),
                        iconTint = Color(0xFFC62828)
                    ) {
                        guide.donts.forEach { item ->
                            Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                                Text("❌", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(item, fontSize = 13.sp, color = Color(0xFF444444), lineHeight = 20.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Treatments
                    CareGuideSection(
                        icon = Icons.Default.LocalPharmacy,
                        title = "Treatment Options 💊",
                        iconBg = Color(0xFFE3F2FD),
                        iconTint = Color(0xFF1565C0)
                    ) {
                        guide.treatments.forEach { treatment ->
                            Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(guide.color).padding(top = 6.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(treatment, fontSize = 13.sp, color = Color(0xFF444444), lineHeight = 20.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Disclaimer
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Text(
                            text = "⚕️ This guide is for informational purposes only. Always consult a licensed dermatologist for proper diagnosis and personalized treatment.",
                            fontSize = 11.sp,
                            color = Color(0xFFE65100),
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// ── Reusable Section Card ─────────────────────────────────────────────────────
@Composable
fun CareGuideSection(
    icon: ImageVector,
    title: String,
    iconBg: Color,
    iconTint: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1a1a1a))
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}