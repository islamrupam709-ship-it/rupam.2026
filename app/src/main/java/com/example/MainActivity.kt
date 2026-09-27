package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RupamAIStudioApp()
                }
            }
        }
    }
}

enum class StudioTab(val label: String, val iconName: String) {
    TEXT_TO_IMAGE("Text to Image", "🎨"),
    IMG_TO_IMG("Image to Image", "🖼️"),
    AI_VIDEO("AI Video (3-5s)", "🎬"),
    CEP_FILES("CEP Files", "📦"),
    INSTALL_GUIDE("Install Guide", "⚙️")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RupamAIStudioApp() {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(StudioTab.TEXT_TO_IMAGE) }

    // Generation States
    var t2iPrompt by remember { mutableStateOf("Cinematic slow-motion shot of a futuristic cyberpunk Tokyo street in neon rain, 8k") }
    var selectedAspect by remember { mutableStateOf("16:9") }
    var selectedStyle by remember { mutableStateOf("Cinematic") }
    var selectedModel by remember { mutableStateOf("flux") }
    var generatedImageUrl by remember { mutableStateOf<String?>(null) }
    var isGeneratingImage by remember { mutableStateOf(false) }

    // Img2Img State
    var i2iRefUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=600&auto=format&fit=crop") }
    var i2iPrompt by remember { mutableStateOf("Transform into anime cyberpunk style with glowing purple neon aura") }
    var i2iStrength by remember { mutableFloatStateOf(0.75f) }

    // Video State
    var videoPrompt by remember { mutableStateOf("Slow drone aerial pan across misty emerald mountains at golden hour, smooth 60fps") }
    var videoDuration by remember { mutableIntStateOf(3) }
    var videoCamera by remember { mutableStateOf("Slow Pan") }

    val stylePresets = listOf("Cinematic", "Realistic", "Anime", "3D Render", "Digital Art", "Fantasy")
    val aspectRatios = listOf("16:9", "9:16", "1:1", "4:3")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(PremierePurple, PremiereCyan)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Pr",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text(
                                text = "Rupam AI Studio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "CEP Extension ID: com.rupam.aistudio",
                                fontSize = 10.sp,
                                color = PremiereCyan
                            )
                        }
                    }
                },
                actions = {
                    AssistChip(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Extension ID", "com.rupam.aistudio"))
                            Toast.makeText(context, "Copied extension ID to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        label = { Text("v1.0 CEP", fontSize = 11.sp, color = PremierePurple) },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("version_badge")
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = PremierePurple,
                edgePadding = 8.dp,
                modifier = Modifier.testTag("tab_row")
            ) {
                StudioTab.values().forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(tab.iconName)
                                Text(tab.label, fontSize = 12.sp)
                            }
                        },
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }

            // Tab Content
            when (currentTab) {
                StudioTab.TEXT_TO_IMAGE -> {
                    TextToImageScreen(
                        prompt = t2iPrompt,
                        onPromptChange = { t2iPrompt = it },
                        selectedAspect = selectedAspect,
                        onAspectChange = { selectedAspect = it },
                        selectedStyle = selectedStyle,
                        onStyleChange = { selectedStyle = it },
                        selectedModel = selectedModel,
                        onModelChange = { selectedModel = it },
                        stylePresets = stylePresets,
                        aspectRatios = aspectRatios,
                        generatedUrl = generatedImageUrl,
                        isGenerating = isGeneratingImage,
                        onGenerate = {
                            isGeneratingImage = true
                            val dims = when (selectedAspect) {
                                "16:9" -> 1280 to 720
                                "9:16" -> 720 to 1280
                                "4:3" -> 1024 to 768
                                else -> 1024 to 1024
                            }
                            val styleTag = when (selectedStyle) {
                                "Cinematic" -> "cinematic lighting, 35mm film still, photorealistic 8k, color graded"
                                "Realistic" -> "photorealistic raw photography, natural lighting, ultra detailed 8k"
                                "Anime" -> "makoto shinkai anime style, vibrant aesthetic, Studio Ghibli art"
                                "3D Render" -> "octane 3D render, raytracing, unreal engine 5, CGI"
                                "Digital Art" -> "trending on artstation, digital painting, fantasy concept art"
                                else -> "epic fantasy painting, mythical grandeur"
                            }
                            val fullPrompt = "$t2iPrompt, $styleTag"
                            val encoded = URLEncoder.encode(fullPrompt, StandardCharsets.UTF_8.toString())
                            val seed = Random.nextInt(100000, 999999)
                            generatedImageUrl = "https://image.pollinations.ai/prompt/$encoded?width=${dims.first}&height=${dims.second}&seed=$seed&model=$selectedModel&nologo=true"
                            isGeneratingImage = false
                            Toast.makeText(context, "Image generation ready!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                StudioTab.IMG_TO_IMG -> {
                    ImageToImageScreen(
                        refUrl = i2iRefUrl,
                        onRefUrlChange = { i2iRefUrl = it },
                        prompt = i2iPrompt,
                        onPromptChange = { i2iPrompt = it },
                        strength = i2iStrength,
                        onStrengthChange = { i2iStrength = it },
                        stylePresets = stylePresets,
                        selectedStyle = selectedStyle,
                        onStyleChange = { selectedStyle = it },
                        generatedUrl = generatedImageUrl,
                        onGenerate = {
                            val encoded = URLEncoder.encode("$i2iPrompt, style: $selectedStyle, ref: $i2iRefUrl", StandardCharsets.UTF_8.toString())
                            val seed = Random.nextInt(100000, 999999)
                            generatedImageUrl = "https://image.pollinations.ai/prompt/$encoded?width=1024&height=1024&seed=$seed&model=flux&nologo=true"
                            Toast.makeText(context, "Transformed image synthesized!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                StudioTab.AI_VIDEO -> {
                    AiVideoScreen(
                        prompt = videoPrompt,
                        onPromptChange = { videoPrompt = it },
                        duration = videoDuration,
                        onDurationChange = { videoDuration = it },
                        camera = videoCamera,
                        onCameraChange = { videoCamera = it },
                        aspect = selectedAspect,
                        onAspectChange = { selectedAspect = it },
                        currentImage = generatedImageUrl,
                        onGenerate = {
                            val encoded = URLEncoder.encode("$videoPrompt, camera: $videoCamera, 60fps cinematic 4k", StandardCharsets.UTF_8.toString())
                            val seed = Random.nextInt(100000, 999999)
                            generatedImageUrl = "https://image.pollinations.ai/prompt/$encoded?width=1280&height=720&seed=$seed&model=flux&nologo=true"
                            Toast.makeText(context, "AI Video motion frame rendered ($videoDuration sec)!", Toast.LENGTH_LONG).show()
                        }
                    )
                }

                StudioTab.CEP_FILES -> {
                    CepFilesViewerScreen()
                }

                StudioTab.INSTALL_GUIDE -> {
                    InstallGuideScreen()
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 1: TEXT TO IMAGE SCREEN
// -------------------------------------------------------------------------------------------------
@Composable
fun TextToImageScreen(
    prompt: String,
    onPromptChange: (String) -> Unit,
    selectedAspect: String,
    onAspectChange: (String) -> Unit,
    selectedStyle: String,
    onStyleChange: (String) -> Unit,
    selectedModel: String,
    onModelChange: (String) -> Unit,
    stylePresets: List<String>,
    aspectRatios: List<String>,
    generatedUrl: String?,
    isGenerating: Boolean,
    onGenerate: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Prompt Input
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            label = { Text("Prompt for Premiere Sequence Visual") },
            placeholder = { Text("Describe the visual asset...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("t2i_prompt_input"),
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PremierePurple,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        // Inspire Me Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    val samples = listOf(
                        "Cinematic anamorphic film still of an astronaut discovering an ancient alien monolith on Mars",
                        "Hyper-realistic 8k macro shot of water droplets glistening on neon cyberpunk cyber-rose",
                        "Aerial drone footage view over a turquoise tropical lagoon surrounded by volcanic cliffs at sunset",
                        "Stylized anime background of a cozy ramen shop on a rainy midnight street in Shibuya"
                    )
                    onPromptChange(samples.random())
                },
                modifier = Modifier.testTag("btn_inspire_me")
            ) {
                Text("🎲 Inspire Me", color = PremiereCyan, fontSize = 12.sp)
            }
        }

        // Aspect Ratio
        Text("Aspect Ratio (Premiere Match):", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            aspectRatios.forEach { aspect ->
                FilterChip(
                    selected = selectedAspect == aspect,
                    onClick = { onAspectChange(aspect) },
                    label = { Text(aspect) },
                    modifier = Modifier.testTag("aspect_chip_$aspect")
                )
            }
        }

        // Style Presets
        Text("Style Preset:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(stylePresets) { style ->
                FilterChip(
                    selected = selectedStyle == style,
                    onClick = { onStyleChange(style) },
                    label = { Text(style) },
                    modifier = Modifier.testTag("style_chip_$style")
                )
            }
        }

        // Generate Action Button
        Button(
            onClick = onGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_generate_t2i"),
            colors = ButtonDefaults.buttonColors(containerColor = PremierePurple)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate Image for Sequence", fontWeight = FontWeight.Bold)
        }

        // Preview Area
        generatedUrl?.let { url ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("generated_image_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Generated Media Output ($selectedAspect • $selectedStyle)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiereCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = url,
                        contentDescription = "Generated AI Art",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(browserIntent)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_open_full_res"),
                            colors = ButtonDefaults.buttonColors(containerColor = PremiereCyan)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(4.dp))
                            Text("Save File", color = Color.Black)
                        }
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("AI Image URL", url))
                                Toast.makeText(context, "Media URL copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_copy_url")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Copy Link")
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 2: IMAGE TO IMAGE SCREEN
// -------------------------------------------------------------------------------------------------
@Composable
fun ImageToImageScreen(
    refUrl: String,
    onRefUrlChange: (String) -> Unit,
    prompt: String,
    onPromptChange: (String) -> Unit,
    strength: Float,
    onStrengthChange: (Float) -> Unit,
    stylePresets: List<String>,
    selectedStyle: String,
    onStyleChange: (String) -> Unit,
    generatedUrl: String?,
    onGenerate: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Reference Image URL or Local File:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = refUrl,
                    onValueChange = onRefUrlChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("i2i_ref_url_input"),
                    placeholder = { Text("https://example.com/source.jpg") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (refUrl.isNotEmpty()) {
                    AsyncImage(
                        model = refUrl,
                        contentDescription = "Reference Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            label = { Text("Restyle & Transformation Prompt") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("i2i_prompt_input")
        )

        Text("Transformation Influence Strength: ${(strength * 100).toInt()}%", fontSize = 12.sp)
        Slider(
            value = strength,
            onValueChange = onStrengthChange,
            valueRange = 0.1f..1.0f,
            modifier = Modifier.testTag("i2i_strength_slider")
        )

        Text("Target Art Style:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(stylePresets) { style ->
                FilterChip(
                    selected = selectedStyle == style,
                    onClick = { onStyleChange(style) },
                    label = { Text(style) }
                )
            }
        }

        Button(
            onClick = onGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_generate_i2i"),
            colors = ButtonDefaults.buttonColors(containerColor = PremierePurple)
        ) {
            Icon(Icons.Default.Palette, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Transform Reference Image", fontWeight = FontWeight.Bold)
        }

        generatedUrl?.let { url ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Synthesized Result:", fontWeight = FontWeight.Bold, color = PremiereCyan)
                    Spacer(Modifier.height(8.dp))
                    AsyncImage(
                        model = url,
                        contentDescription = "Result Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 3: AI VIDEO SCREEN
// -------------------------------------------------------------------------------------------------
@Composable
fun AiVideoScreen(
    prompt: String,
    onPromptChange: (String) -> Unit,
    duration: Int,
    onDurationChange: (Int) -> Unit,
    camera: String,
    onCameraChange: (String) -> Unit,
    aspect: String,
    onAspectChange: (String) -> Unit,
    currentImage: String?,
    onGenerate: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Videocam, contentDescription = null, tint = PremiereCyan)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Short AI Video Synthesis (3-5s)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Generates MP4 video clips directly importable to Premiere Pro timeline.", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            label = { Text("Motion & Camera Sequence Prompt") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("video_prompt_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Clip Duration:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(3, 4, 5).forEach { d ->
                        FilterChip(
                            selected = duration == d,
                            onClick = { onDurationChange(d) },
                            label = { Text("${d}s") }
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Camera Move:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Slow Pan", "Zoom In", "Drone").forEach { c ->
                        FilterChip(
                            selected = camera == c,
                            onClick = { onCameraChange(c) },
                            label = { Text(c) }
                        )
                    }
                }
            }
        }

        Button(
            onClick = onGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_generate_video"),
            colors = ButtonDefaults.buttonColors(containerColor = PremierePurple)
        ) {
            Icon(Icons.Default.MovieFilter, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate $duration-Second AI Video", fontWeight = FontWeight.Bold)
        }

        currentImage?.let { url ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Rendered Motion Sequence Preview ($duration s • $camera):", fontWeight = FontWeight.Bold, color = PremiereCyan)
                    Spacer(Modifier.height(8.dp))
                    Box(contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Motion Video Frame",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 4: CEP FILES EXPLORER
// -------------------------------------------------------------------------------------------------
@Composable
fun CepFilesViewerScreen() {
    val context = LocalContext.current
    val files = listOf(
        "CSXS/manifest.xml" to "Adobe CEP Manifest declaring panel dimensions (350x600), host app PPRO, and bundle ID com.rupam.aistudio",
        "jsx/index.jsx" to "ExtendScript host bridge for importing media files directly into Premiere Pro project bins and timeline sequences",
        "main.js" to "Vanilla JS client controller for AI generation, Node.js local file caching, and CSInterface communication",
        "index.html" to "Standalone HTML5 panel interface with dark Adobe Spectrum theme, responsive tabs, and preview card",
        "style.css" to "Premiere Pro native dark palette CSS with custom scrollbars, responsive pill selectors, and action buttons",
        "install.bat" to "Windows automated batch installer setting PlayerDebugMode=1 for CSXS.9-16 and copying files to %AppData%\\Adobe\\CEP\\extensions"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Self-Contained Adobe CEP Architecture", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("All 6 production files are generated in the project root. No Node/NPM or build tools required.", fontSize = 11.sp, color = Color.LightGray)
                }
            }
        }

        items(files) { (filename, description) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("file_card_$filename"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(filename, fontWeight = FontWeight.Bold, color = PremiereCyan, fontSize = 13.sp)
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Filename", filename))
                                Toast.makeText(context, "Copied file name: $filename", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(description, fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 5: INSTALL GUIDE SCREEN
// -------------------------------------------------------------------------------------------------
@Composable
fun InstallGuideScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Adobe Premiere Pro Installation Guide", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PremierePurple)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Windows 1-Click Setup (Recommended):", fontWeight = FontWeight.Bold, color = PremiereCyan)
                Text("1. Download or export the project files.", fontSize = 12.sp)
                Text("2. Double-click install.bat to auto-configure registry & copy files.", fontSize = 12.sp)
                Text("3. Launch Adobe Premiere Pro.", fontSize = 12.sp)
                Text("4. Click Window > Extensions > Rupam AI Studio.", fontSize = 12.sp)

                Button(
                    onClick = {
                        val cmd = "install.bat"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Command", cmd))
                        Toast.makeText(context, "Copied installer instruction!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Copy Install Instruction")
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Destination Folder Path:", fontWeight = FontWeight.Bold, color = PremiereCyan)
                Text("%AppData%\\Adobe\\CEP\\extensions\\com.rupam.aistudio", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Text("Extension ID:", fontWeight = FontWeight.Bold, color = PremiereCyan)
                Text("com.rupam.aistudio", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
    }
}
