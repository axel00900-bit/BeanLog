package com.example.beanlog

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.beanlog.ui.theme.LatteVariant
import com.example.beanlog.ui.theme.CoffeeCard
import com.example.beanlog.ui.theme.CoffeeCardText
import com.example.beanlog.ui.theme.CoffeeCream
import com.example.beanlog.ui.theme.DarkRoast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ROAST_LEVELS = listOf("Light", "Medium-Light", "Medium", "Medium-Dark", "Dark")

private enum class BeanIconAction { BEAN, ADD, EDIT, DELETE, SAVE }

/** BeanLog's shared coffee-themed action icons. */
@Composable
private fun BeanActionIcon(
    action: BeanIconAction,
    contentDescription: String?,
    modifier: Modifier = Modifier.size(24.dp),
    color: Color = DarkRoast
) {
    when (action) {
        BeanIconAction.BEAN, BeanIconAction.ADD -> {
            Box(modifier, contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.mipmap.bean_launcher_foreground),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                        color
                    ),
                    contentScale = ContentScale.Fit
                )
                if (action == BeanIconAction.ADD) {
                    Canvas(Modifier.fillMaxSize()) {
                        val plus = Color(0xFFB98961)
                        drawLine(plus, androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .28f),
                            androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .72f),
                            size.width * .105f, cap = StrokeCap.Round)
                        drawLine(plus, androidx.compose.ui.geometry.Offset(size.width * .28f, size.height * .5f),
                            androidx.compose.ui.geometry.Offset(size.width * .72f, size.height * .5f),
                            size.width * .105f, cap = StrokeCap.Round)
                    }
                }
            }
        }
        BeanIconAction.EDIT -> Canvas(modifier) {
            val cream = color
            rotate(-45f, pivot = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)) {
                val w = size.width
                val h = size.height
                // Pencil point and long handle.
                val pencil = Path().apply {
                    moveTo(w * .39f, h * .39f)
                    lineTo(w * .61f, h * .39f)
                    lineTo(w * .61f, h * .78f)
                    lineTo(w * .50f, h * .96f)
                    lineTo(w * .39f, h * .78f)
                    close()
                }
                drawPath(pencil, cream)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .50f, h * .84f),
                    androidx.compose.ui.geometry.Offset(w * .50f, h * .94f), w * .035f)
                // Round portafilter basket, rim and perforations.
                drawCircle(cream, radius = w * .235f, center = androidx.compose.ui.geometry.Offset(w * .50f, h * .25f))
                drawCircle(color, radius = w * .185f, center = androidx.compose.ui.geometry.Offset(w * .50f, h * .25f))
                drawCircle(cream, radius = w * .145f, center = androidx.compose.ui.geometry.Offset(w * .50f, h * .25f), style = Stroke(width = w * .025f))
                for (row in 0..3) for (column in 0..3) {
                    val x = w * (.405f + column * .063f)
                    val y = h * (.155f + row * .063f)
                    drawCircle(cream, radius = w * .012f, center = androidx.compose.ui.geometry.Offset(x, y))
                }
                // Neck connecting the basket to the pencil-handle.
                drawRect(cream, topLeft = androidx.compose.ui.geometry.Offset(w * .43f, h * .43f),
                    size = androidx.compose.ui.geometry.Size(w * .14f, h * .055f))
            }
        }
        BeanIconAction.DELETE -> Canvas(modifier) {
            val cream = color
            val w = size.width
            val h = size.height
            drawLine(cream, androidx.compose.ui.geometry.Offset(w * .20f, h * .27f),
                androidx.compose.ui.geometry.Offset(w * .80f, h * .27f), w * .085f, cap = StrokeCap.Round)
            drawLine(cream, androidx.compose.ui.geometry.Offset(w * .38f, h * .16f),
                androidx.compose.ui.geometry.Offset(w * .62f, h * .16f), w * .075f, cap = StrokeCap.Round)
            val bin = Path().apply {
                moveTo(w * .27f, h * .34f)
                lineTo(w * .33f, h * .86f)
                quadraticTo(w * .34f, h * .92f, w * .41f, h * .92f)
                lineTo(w * .59f, h * .92f)
                quadraticTo(w * .66f, h * .92f, w * .67f, h * .86f)
                lineTo(w * .73f, h * .34f)
                close()
            }
            drawPath(bin, cream, style = Stroke(width = w * .075f))
            drawLine(cream, androidx.compose.ui.geometry.Offset(w * .44f, h * .44f),
                androidx.compose.ui.geometry.Offset(w * .45f, h * .80f), w * .055f, cap = StrokeCap.Round)
            drawLine(cream, androidx.compose.ui.geometry.Offset(w * .56f, h * .44f),
                androidx.compose.ui.geometry.Offset(w * .55f, h * .80f), w * .055f, cap = StrokeCap.Round)
        }
        BeanIconAction.SAVE -> Canvas(modifier) {
            val cream = color
            val check = Path().apply {
                moveTo(size.width * .18f, size.height * .52f)
                lineTo(size.width * .42f, size.height * .75f)
                lineTo(size.width * .84f, size.height * .25f)
            }
            drawPath(check, cream, style = Stroke(width = size.width * .11f, cap = StrokeCap.Round))
        }
    }
}

/** Illustrated coffee bag placeholder for beans without a user photo. */
@Composable
private fun CoffeeBagIcon(modifier: Modifier = Modifier.size(64.dp)) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val outline = Color(0xFF4B3022)
        val bag = Color(0xFFE4C39D)
        val fold = Color(0xFFB98961)
        val label = Color(0xFFFFF8EC)

        // Main gusseted bag body.
        val body = Path().apply {
            moveTo(w * .23f, h * .20f)
            lineTo(w * .77f, h * .20f)
            lineTo(w * .83f, h * .88f)
            quadraticTo(w * .50f, h * .98f, w * .17f, h * .88f)
            close()
        }
        drawPath(body, bag)
        drawPath(body, outline, style = Stroke(width = w * .045f))

        // Folded/sealed top.
        val top = Path().apply {
            moveTo(w * .23f, h * .20f)
            lineTo(w * .27f, h * .08f)
            lineTo(w * .73f, h * .08f)
            lineTo(w * .77f, h * .20f)
            close()
        }
        drawPath(top, fold)
        drawPath(top, outline, style = Stroke(width = w * .04f))
        drawLine(outline, androidx.compose.ui.geometry.Offset(w * .29f, h * .15f),
            androidx.compose.ui.geometry.Offset(w * .71f, h * .15f), w * .025f)

        // Front label.
        drawRoundRect(label, topLeft = androidx.compose.ui.geometry.Offset(w * .29f, h * .36f),
            size = androidx.compose.ui.geometry.Size(w * .42f, h * .34f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .035f, w * .035f))
        drawRoundRect(outline, topLeft = androidx.compose.ui.geometry.Offset(w * .29f, h * .36f),
            size = androidx.compose.ui.geometry.Size(w * .42f, h * .34f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .035f, w * .035f),
            style = Stroke(width = w * .025f))

        // Small coffee bean emblem on the label.
        val bean = Path().apply {
            moveTo(w * .50f, h * .40f)
            cubicTo(w * .62f, h * .39f, w * .66f, h * .51f, w * .57f, h * .59f)
            cubicTo(w * .48f, h * .67f, w * .37f, h * .61f, w * .39f, h * .51f)
            cubicTo(w * .40f, h * .45f, w * .45f, h * .41f, w * .50f, h * .40f)
            close()
        }
        drawPath(bean, outline)
        drawLine(label, androidx.compose.ui.geometry.Offset(w * .57f, h * .42f),
            androidx.compose.ui.geometry.Offset(w * .45f, h * .60f), w * .018f, cap = StrokeCap.Round)

        // Subtle label rules.
        drawLine(fold, androidx.compose.ui.geometry.Offset(w * .37f, h * .75f),
            androidx.compose.ui.geometry.Offset(w * .63f, h * .75f), w * .025f, cap = StrokeCap.Round)
    }
}

private fun String.num() = replace(',', '.').toDoubleOrNull()

private fun countryFlag(country: String): String {
    val aliases = mapOf("USA" to "US", "United States" to "US", "UK" to "GB", "England" to "GB",
        "South Korea" to "KR", "Czech Republic" to "CZ", "Vietnam" to "VN", "Ivory Coast" to "CI")
    val countryName = country.trim().substringAfterLast(",").trim()
    val code = aliases[countryName] ?: Locale.getISOCountries().firstOrNull {
        Locale("", it).getDisplayCountry(Locale.ENGLISH).equals(countryName, ignoreCase = true)
    } ?: return ""
    return code.uppercase().map { String(Character.toChars(0x1F1E6 + it.code - 65)) }.joinToString("")
}

/**
 * A softly tilted coffee-bean silhouette. The photo itself remains a normal image;
 * only its on-screen presentation is clipped to this shape.
 */
private val CoffeeBeanPhotoShape = GenericShape { size, _ ->
    moveTo(size.width * 0.52f, size.height * 0.02f)
    cubicTo(size.width * 0.82f, size.height * -0.01f,
        size.width * 1.02f, size.height * 0.22f,
        size.width * 0.95f, size.height * 0.50f)
    cubicTo(size.width * 0.89f, size.height * 0.75f,
        size.width * 0.68f, size.height * 0.98f,
        size.width * 0.43f, size.height * 0.97f)
    cubicTo(size.width * 0.17f, size.height * 0.96f,
        size.width * 0.01f, size.height * 0.76f,
        size.width * 0.04f, size.height * 0.49f)
    cubicTo(size.width * 0.07f, size.height * 0.22f,
        size.width * 0.28f, size.height * 0.04f,
        size.width * 0.52f, size.height * 0.02f)
    close()
}

/** Display a photo in BeanLog's coffee-bean silhouette with a subtle centre crease. */
@Composable
private fun BeanPhoto(path: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CoffeeBeanPhotoShape)
            .border(1.dp, DarkRoast.copy(alpha = 0.38f), CoffeeBeanPhotoShape)
    ) {
        AsyncImage(
            model = File(path),
            contentDescription = "Coffee bean photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Canvas(Modifier.fillMaxSize()) {
            val crease = Path().apply {
                moveTo(size.width * 0.64f, size.height * 0.10f)
                cubicTo(size.width * 0.28f, size.height * 0.31f,
                    size.width * 0.73f, size.height * 0.66f,
                    size.width * 0.36f, size.height * 0.91f)
            }
            drawPath(
                crease,
                Color(0xFF2B1A12).copy(alpha = 0.28f),
                style = Stroke(width = size.width * 0.018f, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Decode with sampling, then cap the longest edge at 1200 px and store a
 * moderately compressed JPEG. Returns null rather than crashing if the URI
 * cannot be read or the image cannot be decoded.
 */
private fun compressPickedImage(ctx: Context, uri: Uri): String? {
    var decoded: Bitmap? = null
    var scaled: Bitmap? = null
    var output: File? = null
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val maxDimension = maxOf(bounds.outWidth, bounds.outHeight)
        var sampleSize = 1
        while (maxDimension / sampleSize > 2400) sampleSize *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        decoded = ctx.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val decodedBitmap = decoded ?: return null
        val decodedMax = maxOf(decodedBitmap.width, decodedBitmap.height)
        val scale = minOf(1f, 1200f / decodedMax.toFloat())
        val finalBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decodedBitmap,
                (decodedBitmap.width * scale).toInt().coerceAtLeast(1),
                (decodedBitmap.height * scale).toInt().coerceAtLeast(1),
                true
            ).also { scaled = it }
        } else decodedBitmap

        output = File.createTempFile("bean_", ".jpg", ctx.filesDir)
        FileOutputStream(output).use { stream ->
            if (!finalBitmap.compress(Bitmap.CompressFormat.JPEG, 84, stream)) {
                throw IllegalStateException("Could not compress selected image")
            }
            stream.flush()
        }
        output.absolutePath
    } catch (_: Exception) {
        output?.delete()
        null
    } catch (_: OutOfMemoryError) {
        output?.delete()
        null
    } finally {
        if (scaled != null && scaled !== decoded) scaled?.recycle()
        decoded?.recycle()
    }
}

/** Delete only image files managed by BeanLog inside its private files directory. */
private fun deleteManagedPhoto(ctx: Context, path: String?) {
    if (path.isNullOrBlank()) return
    runCatching {
        val file = File(path)
        if (file.canonicalFile.parentFile == ctx.filesDir.canonicalFile &&
            (file.name.startsWith("bean_") || file.name.startsWith("brew_")) && file.extension.equals("jpg", ignoreCase = true)
        ) file.delete()
    }
}

@Composable
fun App(dao: BeanDao, initialBeanId: Long? = null, initialBagId: Long? = null, initialBrewId: Long? = null, darkMode: Boolean = false, onThemeChange: (Boolean) -> Unit = {}) {
    val nav = rememberNavController()
    LaunchedEffect(initialBeanId, initialBagId, initialBrewId) {
        if (initialBeanId != null) {
            nav.navigate("detail/$initialBeanId/false/${initialBagId ?: 0L}/${initialBrewId ?: 0L}") {
                popUpTo("list") { inclusive = false }
            }
        }
    }
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(colors = listOf(LatteVariant, CoffeeCream))
        ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.mipmap.bean_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(3120.dp).alpha(0.12f),
            contentScale = ContentScale.Fit
        )
        NavHost(nav, startDestination = "list") {
            composable("list") {
                BeanList(dao, onOpen = { nav.navigate("detail/$it/false/0/0") },
                    onAdd = { nav.navigate("edit/0") },
                    onRoasters = { nav.navigate("roasters/${System.currentTimeMillis()}") },
                    onInsights = { nav.navigate("insights") }, darkMode = darkMode, onThemeChange = onThemeChange)
            }
            composable("roasters/{session}") {
                RoasterDirectoryScreen(dao, onBack = { nav.popBackStack() })
            }
            composable("insights") {
                InsightsScreen(dao, onBack = { nav.popBackStack() })
            }
            composable("detail/{id}/{addBag}/{openBagId}/{openBrewId}") {
                val id = it.arguments!!.getString("id")!!.toLong()
                val addBag = it.arguments!!.getString("addBag")!!.toBoolean()
                val openBagId = it.arguments!!.getString("openBagId")!!.toLong().takeIf { value -> value > 0L }
                val openBrewId = it.arguments!!.getString("openBrewId")!!.toLong().takeIf { value -> value > 0L }
                DetailScreen(dao, id, autoOpenAddBag = addBag, openBagId = openBagId, openBrewId = openBrewId,
                    onEdit = { nav.navigate("edit/$id") }, onBack = { nav.popBackStack() })
            }
            composable("edit/{id}") {
                val id = it.arguments!!.getString("id")!!.toLong()
                EditScreen(dao, id, onDone = { savedId, isNew ->
                    nav.popBackStack()
                    if (isNew) nav.navigate("detail/$savedId/true/0/0")
                })
            }
        }
    }
}



private data class TasteStat(val name: String, val likedWeight: Int, val totalWeight: Int) {
    val percent: Int get() = if (totalWeight == 0) 0 else (likedWeight * 100.0 / totalWeight).toInt()
}

private data class HistoryStat(val name: String, val count: Int, val percent: Int)

@Composable
private fun InsightsScreen(dao: BeanDao, onBack: () -> Unit) {
    var beans by remember { mutableStateOf<List<Bean>>(emptyList()) }
    var bags by remember { mutableStateOf<List<Bag>>(emptyList()) }
    var brews by remember { mutableStateOf<List<Shot>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    LaunchedEffect(dao) {
        withContext(Dispatchers.IO) {
            beans = dao.allBeans()
            bags = dao.allBags()
            brews = dao.allShots()
        }
    }
    val bagsByBean = bags.groupBy { it.beanId }
    val bagsById = bags.associateBy { it.id }
    val brewsByBean = brews.groupBy { brew -> bagsById[brew.bagId]?.beanId }
    val tasteGroups = listOf(
        "Variety" to { bean: Bean -> bean.variety },
        "Farm" to { bean: Bean -> bean.farm },
        "Roast level" to { bean: Bean -> bean.roastLevel },
        "Roaster" to { bean: Bean -> bean.roastery },
        "Processing" to { bean: Bean -> bean.processing }
    ).mapNotNull { (category, valueOf) ->
        val totals = linkedMapOf<String, Pair<Int, Int>>()
        beans.forEach { bean ->
            val label = valueOf(bean).trim()
            if (label.isNotBlank()) {
                var liked = 0
                var total = 0
                (brewsByBean[bean.id] ?: emptyList()).forEach { brew ->
                    if (brew.liked != null) {
                        total += 1
                        if (brew.liked == true) liked += 1
                    }
                }
                (bagsByBean[bean.id] ?: emptyList()).forEach { bag ->
                    if (bag.liked != null) {
                        total += 3
                        if (bag.liked == true) liked += 3
                    }
                }
                if (total > 0) {
                    val previous = totals[label] ?: (0 to 0)
                    totals[label] = (previous.first + liked) to (previous.second + total)
                }
            }
        }
        val stats = totals.map { (name, weights) -> TasteStat(name, weights.first, weights.second) }
            .sortedWith(compareByDescending<TasteStat> { it.percent }.thenByDescending { it.totalWeight })
        if (stats.isEmpty()) null else category to stats
    }
    val historyGroups = listOf(
        "Variety" to { bean: Bean -> bean.variety },
        "Roaster" to { bean: Bean -> bean.roastery },
        "Processing" to { bean: Bean -> bean.processing },
        "Farm" to { bean: Bean -> bean.farm },
        "Roast level" to { bean: Bean -> bean.roastLevel },
        "Origin" to { bean: Bean -> bean.origin }
    ).mapNotNull { (category, valueOf) ->
        val values = beans.map { valueOf(it).trim() }.filter { it.isNotBlank() }
        val total = values.size
        val stats = values.groupingBy { it }.eachCount().map { (name, count) ->
            HistoryStat(name, count, if (total == 0) 0 else (count * 100.0 / total).toInt())
        }.sortedWith(compareByDescending<HistoryStat> { it.count }.thenBy { it.name })
        if (stats.isEmpty()) null else category to stats
    }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Coffee insights", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = DarkRoast) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("What you like") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Coffee history") })
            }
            if (selectedTab == 0) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text("Your taste profile", style = MaterialTheme.typography.titleLarge, color = DarkRoast)
                        Text("Shot ratings count as 1 vote. Liking a bag to buy again counts as 3 votes. Unrated brews and bags are excluded.", style = MaterialTheme.typography.bodySmall, color = DarkRoast.copy(alpha = .76f))
                    }
                    if (tasteGroups.isEmpty()) item {
                        Text("Rate brews or mark a bag as one you'd buy again to start building your taste profile.", color = DarkRoast)
                    }
                    tasteGroups.forEach { (category, stats) ->
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = LatteVariant), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Text(category, style = MaterialTheme.typography.titleMedium, color = DarkRoast)
                                    stats.forEach { stat ->
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(stat.name, color = DarkRoast, style = MaterialTheme.typography.bodyMedium)
                                                Text("${stat.percent}% liked · ${stat.totalWeight} votes", color = DarkRoast.copy(alpha = .76f), style = MaterialTheme.typography.labelSmall)
                                            }
                                            LinearProgressIndicator(
                                                progress = { stat.percent / 100f },
                                                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                                                color = Color(0xFF987052),
                                                trackColor = DarkRoast.copy(alpha = .12f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text("Your coffee history", style = MaterialTheme.typography.titleLarge, color = DarkRoast)
                        Text("${beans.size} coffees recorded. Percentages are based on the number of bean entries with that detail, not bag weight or brew count.", style = MaterialTheme.typography.bodySmall, color = DarkRoast.copy(alpha = .76f))
                    }
                    if (historyGroups.isEmpty()) item { Text("Add coffees with variety, roaster or processing details to build your history.", color = DarkRoast) }
                    historyGroups.forEach { (category, stats) ->
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = LatteVariant), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Text(category, style = MaterialTheme.typography.titleMedium, color = DarkRoast)
                                    stats.forEach { stat ->
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(stat.name, color = DarkRoast, style = MaterialTheme.typography.bodyMedium)
                                                Text("${stat.percent}% · ${stat.count} coffees", color = DarkRoast.copy(alpha = .76f), style = MaterialTheme.typography.labelSmall)
                                            }
                                            LinearProgressIndicator(
                                                progress = { stat.percent / 100f },
                                                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                                                color = Color(0xFF987052),
                                                trackColor = DarkRoast.copy(alpha = .12f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- Danish roastery directory ----------
private data class RoasterLocation(
    val name: String,
    val address: String,
    val city: String,
    val type: String,
    val hours: String = ""
)

private data class RoasterLink(val label: String, val url: String)

private data class RoasterProfile(
    val name: String,
    val area: String,
    val micro: String,
    val country: String = "Denmark",
    val description: String,
    val instagram: String? = null,
    val website: String? = null,
    val locations: List<RoasterLocation> = emptyList(),
    val aliases: List<String> = emptyList(),
    val awards: String = "",
    val extraLinks: List<RoasterLink> = emptyList(),
    val note: String = ""
)

private val ROASTERS = listOf(
    RoasterProfile(
        name = "La Cabra", area = "Aarhus · Copenhagen", micro = "No",
        description = "Founded in Aarhus in 2012 by Esben Piper. Light, clear Nordic-style roasting with a focus on origin and transparency. Also operates bakery cafés.",
        instagram = "https://www.instagram.com/lacabracoffee/", website = "https://lacabra.com",
        locations = listOf(
            RoasterLocation("Nordhavn", "Århusgade 118X, 2150", "Nordhavn, Denmark", "Café / coffee shop", "Mon–Fri 07:30–18:00 · Sat–Sun 08:00–17:00"),
            RoasterLocation("Møntergade", "Møntergade 3A, 1116", "Copenhagen K, Denmark", "Café / coffee shop", "Mon–Fri 08:00–18:00 · Sat–Sun 09:00–17:00"),
            RoasterLocation("Kampanilen", "Irma Pedersens Gade 120, 8000", "Aarhus C, Denmark", "Café / coffee shop", "Mon–Sat 08:00–18:00 · Sun 09:00–17:00"),
            RoasterLocation("Graven", "Graven 20, 8000", "Aarhus C, Denmark", "Café / coffee shop", "Mon–Sat 08:00–18:00 · Sun 09:00–17:00")
        ),
        aliases = listOf("La Cabra Coffee"),
        awards = "Top 10 specialty cafés in Denmark, European Coffee Trip Awards 2025. OAD Cheap Eats in Europe: #19 in 2024 and #30 in 2025. Reported international exports to 60+ countries."
    ),
    RoasterProfile(
        name = "Stillers Coffee", area = "Aarhus", micro = "No",
        description = "Coffee-focused craft and expertise. Roasts on site and sells beans, brewing equipment and accessories.",
        instagram = "https://www.instagram.com/stillerscoffee/", website = "https://stillerscoffee.dk",
        locations = listOf(RoasterLocation("Klostergade", "Klostergade 32 E", "Aarhus, Denmark", "Café / shop / roastery")),
        aliases = listOf("Stillers", "Stillers coffee")
    ),
    RoasterProfile(
        name = "Mood Coffee Roasters", area = "Aarhus", micro = "Unknown",
        description = "A small coffee shop where customers can taste and smell coffee before buying. Micro-roastery status is being kept as unknown.",
        instagram = "https://www.instagram.com/moodcoffee_aarhus/", website = "https://moodcoffeeroasters.dk",
        locations = listOf(RoasterLocation("Ankersgade", "Ankersgade 12B", "Aarhus C, Denmark", "Coffee shop")),
        note = "The roastery's website has described it as a micro-roastery, but the directory status remains Unknown as requested."
    ),
    RoasterProfile(
        name = "Berry & Bean", area = "Aarhus", micro = "Unknown",
        description = "Roastery and wholesaler selling coffee, equipment, tea and specialty products. Customer feedback mentions helpful service and consistent roasts.",
        instagram = "https://www.instagram.com/berrybeandk/", website = "https://berrybean.dk",
        locations = listOf(RoasterLocation("KaffeMekka", "Bredskiftevej 22B, 8210", "Aarhus V, Denmark", "Shop / wholesale")),
        aliases = listOf("KaffeMekka", "Berry and Bean")
    ),
    RoasterProfile(
        name = "Coffee Collective", area = "Copenhagen · Frederiksberg · Aarhus", micro = "No",
        description = "Founded in 2007. Focuses on transparent farmer relationships and its Direct Trade approach, with light roasts and filter coffee.",
        instagram = "https://www.instagram.com/coffeecollectif/", website = "https://coffeecollective.dk",
        locations = listOf(
            RoasterLocation("Godthåbsvej", "Godthåbsvej 34 B", "Frederiksberg, Denmark", "Roastery café"),
            RoasterLocation("Aarhus shop", "Guldsmedgade 13", "Aarhus, Denmark", "Coffee shop")
        ),
        aliases = listOf("Coffee Collective Aarhus"),
        awards = "Klaus Thomsen: World Barista Champion 2006. Casper Rasmussen: World Cup Tasters Champion 2008. Sprudgie Best Packaging 2015. Bernikow café in ECT Awards 2025 top 10. Also reported #2 in a Roastful ranking; year not specified."
    ),
    RoasterProfile(
        name = "April Coffee Roasters", area = "Copenhagen", micro = "Unknown",
        description = "Founded in 2016 by Patrik Rolf. Very light, delicate, tea-like coffees and close collaboration with coffee farms.",
        instagram = "https://www.instagram.com/aprilcoffeecph/",
        aliases = listOf("April Coffee"),
        awards = "Danish Roasting Championship 2019; reported World Brewers Cup silver 2019; Patrik Rolf was a 2022 World Brewers Cup finalist. Café in ECT Awards 2025 top 10.",
        note = "Website and customer-facing address have not yet been verified. Micro-roastery status is unclear."
    ),
    RoasterProfile(
        name = "Prolog Coffee", area = "Copenhagen", micro = "Unknown",
        description = "Founded in 2016 by Sebastian Quistorff and Jonas Gehl. Focuses on precise filter and espresso in a relaxed coffee bar.",
        instagram = "https://www.instagram.com/prologcoffee.cph/",
        locations = listOf(RoasterLocation("Høkerboderne", "Høkerboderne 16", "Copenhagen, Denmark", "Coffee bar")),
        awards = "Prolog Coffee Bar in the European Coffee Trip Awards 2025 top 10.",
        note = "Refshaleøen has been mentioned as a roasting area, but roasting-facility addresses are deliberately not recorded."
    ),
    RoasterProfile(
        name = "Impact Roasters", area = "Copenhagen", micro = "Unknown",
        description = "Founded in 2015 by Daniel and Nidzara. Focuses on quality coffee from the Ethiopian highlands and development projects in southern Ethiopia.",
        instagram = "https://www.instagram.com/impact_roasters/",
        note = "Three Copenhagen locations were mentioned, but exact customer-facing addresses and which ones sell beans still need confirmation. Flintholm Allé café was listed in ECT Awards 2025 top 10."
    ),
    RoasterProfile(
        name = "Pontes Roastery & Coffee Bar", area = "Copenhagen", micro = "Yes",
        description = "Small coffee bar and micro-roastery opened in 2025 by Brian Christie and Andreas May. Roasts tiny batches on a 1 kg machine. House espresso: Hidden Hour.",
        instagram = "https://www.instagram.com/pontes_coffee/",
        locations = listOf(RoasterLocation("Toftegårds Plads", "Toftegårds Plads 2", "Copenhagen, Denmark", "Coffee bar / micro-roastery")),
        awards = "European Coffee Trip Awards 2025 top 10 specialty cafés in Denmark."
    ),
    RoasterProfile(
        name = "Kontra Coffee", area = "Copenhagen", micro = "Unknown",
        description = "Sources high-quality coffee from sustainable and ethical suppliers.",
        instagram = "https://www.instagram.com/kontracoffee/",
        note = "Customer-facing address and further company details have not yet been collected."
    ),
    RoasterProfile(
        name = "Copenhagen Coffee Lab", area = "Copenhagen", micro = "Unknown",
        description = "Described as a growing coffee wholesaler.",
        instagram = "https://www.instagram.com/cphcoffeelab/",
        note = "Customer-facing locations and additional company details have not yet been collected. No website is currently listed."
    ),
    RoasterProfile(
        name = "Aoife Coffee", area = "Ebeltoft · Aarhus", micro = "Yes",
        description = "Third-wave coffee from a harbour-side micro-roastery. The name is Irish for 'beauty'. The Aarhus shop offers naturally processed beans and pastries from a neighbouring bakery.",
        instagram = "https://www.instagram.com/aoife.coffee/",
        locations = listOf(
            RoasterLocation("Ebeltoft", "Stockflethsvej 14", "Ebeltoft, Denmark", "Coffee shop / roastery"),
            RoasterLocation("Aarhus", "Volden 21", "Aarhus C, Denmark", "Coffee shop")
        ),
        awards = "European Coffee Trip Awards 2025 top 10 specialty cafés in Denmark.",
        note = "No website listed, as requested."
    ),
    RoasterProfile(
        name = "Komma Kaffe", area = "Værløse", micro = "Yes",
        description = "Family-owned, founded in 2020 by Omar and Mathilde. Direct-trade specialty coffee from an organic-certified roastery.",
        note = "Approx. two employees according to the supplied information. Website, Instagram and customer-facing shop address still needed."
    ),
    RoasterProfile(
        name = "KaffeBrænderiet", area = "Charlottenlund", micro = "Unknown",
        description = "A roastery whose café was listed among Denmark's top 10 specialty cafés at the European Coffee Trip Awards 2025.",
        awards = "European Coffee Trip Awards 2025 top 10 specialty cafés in Denmark.",
        note = "Further company details, links and customer-facing address still needed."
    ),
    RoasterProfile(
        name = "Clever Coffee", area = "Odder", micro = "Unknown",
        description = "Specialty coffee with a positive impact. Based in Odder since 2014, founded by Lars Frello. Certified B Corp; one coffee was reportedly purchased at 175% of the market price.",
        note = "B Corp is a company certification, not a competition award. Website, Instagram and customer-facing address still needed."
    ),
    RoasterProfile(
        name = "Behag din smag", area = "Aalborg", micro = "Unknown",
        description = "Award-winning café, named Denmark's winning café at the European Coffee Trip Awards 2025.",
        note = "It is not yet confirmed whether the café roasts its own coffee. Kept as an unverified roaster candidate."
    ),
    RoasterProfile(
        name = "Holy Bean Coffee Roasters", area = "Aarup · Funen", micro = "Unknown",
        description = "Well-established, women-owned roastery on Funen, supplying several Odense cafés with single-origin coffees.",
        note = "Customer-facing address, website and Instagram still needed."
    ),
    RoasterProfile(
        name = "Kaffe Lars", area = "Near Odense", micro = "Unknown",
        description = "Traditional roaster slow-roasting since 1912, using a vintage German gas-powered drum roaster from 1941.",
        note = "Customer-facing address, website and Instagram still needed."
    ),
    RoasterProfile(
        name = "Karma Kaffe", area = "Odense", micro = "Unknown",
        description = "Says its coffee is roasted at one of Denmark's oldest roasteries, so it may not roast its own beans.",
        note = "Kept as a coffee brand candidate until its own-roasting status and customer-facing sales locations are confirmed."
    ),
    RoasterProfile(
        name = "Strandvejsristeriet", area = "Helsingør", micro = "Unknown",
        description = "A roastery reported to supply the Lagoni coffee bar in Helsingør.",
        note = "Customer-facing address, website and Instagram still needed."
    )
,
    // Portugal

    RoasterProfile(
        name = "Fábrica Coffee Roasters", area = "Lisbon", country = "Portugal", micro = "Unknown",
        description = "Founded in 2015 and described as Portugal's first specialty roastery. Focuses on direct partnerships with farmers and roasting in Lisbon.",
        locations = listOf(
            RoasterLocation("Portas de Santo Antão", "R. das Portas de Santo Antão 136", "Lisbon, Portugal", "Coffee shop"),
            RoasterLocation("Rua do Comércio", "R. do Comércio 111", "Lisbon, Portugal", "Coffee shop")
        ),
        note = "Micro-roastery status is unconfirmed. No prizes recorded. Website and Instagram still need verification."
    ),
    RoasterProfile(
        name = "Comete Coffee Roasters", area = "Lisbon", country = "Portugal", micro = "Unknown",
        description = "Roaster and coffee shop known for light, Scandinavian-style roasts. Part of the specialty coffee movement since 2022, shipping across Portugal and Europe.",
        locations = listOf(RoasterLocation("Comete", "R. Dr. Almeida Amaral 15A", "Lisbon, Portugal", "Coffee shop / roastery")),
        note = "Likely a micro-roastery, based on descriptions by reviewers. Entered the 2024 Best Coffee Roaster competition; no prize recorded. Website and Instagram still need verification."
    ),
    RoasterProfile(
        name = "Olisipo Coffee Roasters", area = "Lisbon", country = "Portugal", micro = "Yes",
        description = "Small-batch roaster focused on sustainable, seasonal specialty coffee, also supplying other specialty cafés. Founded by Antony Watson and Sofia Gonçalves.",
        locations = listOf(RoasterLocation("Olisipo", "Rua do Cruzeiro 84", "Lisbon, Portugal", "Coffee shop / roastery")),
        note = "Opening hours are unclear: reports mention Saturdays only, while map listings show weekday hours. Check before visiting. No prizes recorded; website and Instagram still need verification."
    ),
    RoasterProfile(
        name = "Torra Roasting & Coffee", area = "Lisbon", country = "Portugal", micro = "Unknown",
        description = "Lisbon specialty coffee roaster.",
        locations = listOf(RoasterLocation("Torra", "Tv. do Conde da Ponte 1", "Lisbon, Portugal", "Coffee shop")),
        note = "Entered the 2024 Best Coffee Roaster competition; no prize recorded. Micro-roastery status, website and Instagram still need verification."
    ),
    RoasterProfile(
        name = "Baobá Specialty Coffee Roasters", area = "Lisbon · Marvila", country = "Portugal", micro = "Unknown",
        description = "Coffee sourced from its own farm in Brazil and roasted in Lisbon, with a farm-to-cup approach.",
        locations = listOf(
            RoasterLocation("Marvila roastery shop", "R. Zófimo Pedroso 18-22", "Marvila, Lisbon, Portugal", "Coffee shop / roastery"),
            RoasterLocation("Baobá Café Bica", "Rua de S. Paulo 256-258", "Lisbon, Portugal", "Coffee shop")
        ),
        note = "Micro-roastery status and awards are unconfirmed. Website and Instagram still need verification."
    ),
    RoasterProfile(
        name = "Simpli Coffee Roasters", area = "Lisbon · Picoas", country = "Portugal", micro = "Unknown",
        description = "Founded in 2017. Sells its coffee in its own coffee shops alongside food.",
        locations = listOf(
            RoasterLocation("Picoas", "Av. Fontes Pereira de Melo 41", "Picoas, Lisbon, Portugal", "Coffee shop"),
            RoasterLocation("Braamcamp", "R. Braamcamp 64", "Lisbon, Portugal", "Coffee shop")
        ),
        note = "Micro-roastery status and awards are unconfirmed. Website and Instagram still need verification."
    ),
    RoasterProfile(
        name = "SoLo Brewing", area = "Lisbon", country = "Portugal", micro = "Unknown",
        description = "Experimental coffee lab and brunch spot focused on creative roasting, origin transparency and sustainable practices. Sells beans, brewing accessories and roasting experiences, shipping to Portugal and Spain.",
        website = "https://solobrewing.pt",
        locations = listOf(RoasterLocation("SoLo Brewing", "R. Pinheiro Chagas 16B", "Lisbon, Portugal", "Coffee lab / brunch / coffee shop")),
        awards = "Best Coffee Roaster in Portugal 2025, Porto Coffee Week.",
        note = "Micro-roastery status and Instagram still need verification."
    ),
    RoasterProfile(
        name = "Unity Coffee Roasters", area = "Cascais · Oeiras", country = "Portugal", micro = "Unknown",
        description = "Roastery using a Giesen roaster, with a flagship café in Cascais and an Oeiras café opened in December 2024.",
        awards = "Finalist for Best Coffee Roaster in Portugal 2025, Porto Coffee Week.",
        note = "Exact customer-facing addresses, website and Instagram still need verification. Micro-roastery status is unconfirmed."
    ),
    RoasterProfile(
        name = "Buraca Roasters", area = "São Domingos de Rana", country = "Portugal", micro = "Unknown",
        description = "Roastery and coffee shop with brunch, where visitors can watch the beans being roasted. Also supplies wholesale.",
        locations = listOf(RoasterLocation("Buraca", "Jardins do Buzano 148 A", "São Domingos de Rana, Portugal", "Coffee shop / roastery")),
        note = "Entered the 2024 Best Coffee Roaster competition; no prize recorded. Micro-roastery status, website and Instagram still need verification."
    ),
    RoasterProfile(
        name = "Senzu Coffee Roasters", area = "Porto", country = "Portugal", micro = "Yes",
        description = "High-end specialty coffee roaster focused on coffee education. Supplies Michelin-starred restaurants and collaborates with craft brewers and chocolate makers. Roasting is led by Diogo Amorim, who also teaches roasting; founded around 2017–2019, originally as Luso Coffee Roasters.",
        locations = listOf(RoasterLocation("Senzu", "Rua do Rosário 211", "Porto, Portugal", "Coffee shop / roastery")),
        awards = "Portuguese Roaster of the Year 2023; Best Coffee Roaster in Portugal 2024, Porto Coffee Week; also reported as Best Coffee Roaster at Lisbon Coffee Week.",
        note = "Micro-roaster description reported. Website and Instagram still need verification."
    ),
    RoasterProfile(
        name = "SO Coffee Roasters", area = "Porto · Lisbon", country = "Portugal", micro = "Unknown",
        description = "Founded in Porto in 2019 by three friends. Design-led roastery and coffee shops in Porto and Lisbon, with beans sold online and a mission to make specialty coffee accessible.",
        locations = listOf(
            RoasterLocation("Sá de Noronha", "R. de Sá de Noronha 119", "Porto, Portugal", "Coffee shop / roastery"),
            RoasterLocation("Rua da Restauração", "Rua da Restauração 455", "Porto, Portugal", "Coffee shop")
        ),
        awards = "Team members won the Portuguese Barista Championship 2022 (Rui Pedrosa) and SCA Portugal Brewers Cup 2024 (Luiz Lahutte Motta).",
        note = "A Lisbon shop was also reported, but its address has not yet been supplied. Micro-roastery status, website and Instagram still need verification."
    ),
    RoasterProfile(
        name = "Combi Coffee Roasters", area = "Porto", country = "Portugal", micro = "Unknown",
        description = "Porto specialty coffee roaster that opened a second, larger location with its roasting operation.",
        locations = listOf(RoasterLocation("Combi", "R. do Morgado de Mateus 29", "Porto, Portugal", "Coffee shop / roastery")),
        note = "Micro-roastery status, awards, website and Instagram still need verification."
    ),
    RoasterProfile(
        name = "7g Roaster", area = "Vila Nova de Gaia", country = "Portugal", micro = "Unknown",
        description = "Specialty coffee roaster in Vila Nova de Gaia, across the river from Porto.",
        locations = listOf(
            RoasterLocation("Rua de França 10", "R. de França 10", "Vila Nova de Gaia, Portugal", "Coffee shop"),
            RoasterLocation("Rua de França 52", "R. de França 52", "Vila Nova de Gaia, Portugal", "Coffee shop")
        ),
        note = "Entered the 2024 Best Coffee Roaster competition; no prize recorded. Micro-roastery status, website and Instagram still need verification."
    ),
    RoasterProfile(
        name = "Al-Gharb Coffee Roasters", area = "Albufeira · Algarve", country = "Portugal", micro = "Yes",
        description = "Micro-roastery, coffee shop and brunch restaurant bringing specialty coffee to the Algarve. Roasts on a hot-air (fluid-bed) roaster; co-founder and roast master is Henning Schlueter.",
        locations = listOf(RoasterLocation("Al-Gharb", "Beco do Sol 3", "Albufeira, Portugal", "Coffee shop / micro-roastery / brunch")),
        awards = "Best Challenge Roast at Porto Coffee Week's Best Coffee Roaster Competition; finalist for Best Coffee Roaster in Portugal 2025.",
        note = "Micro-roastery status reported. Website and Instagram still need verification."
    ),
    RoasterProfile(
        name = "Clifftop Coffee Roasters", area = "Lagos · Algarve", country = "Portugal", micro = "Unknown",
        description = "Specialty coffee roaster in the Algarve, shipping across Europe and describing itself as the region's only specialty roastery.",
        website = "https://clifftopcoffee.com",
        locations = listOf(RoasterLocation("LACO Business Hub", "LACO Business Hub, Chinicato", "Lagos, Portugal", "Roastery (visitor access limited)", "Open Tuesdays and on special occasions")),
        note = "Another specialty roastery operates in Albufeira. Micro-roastery status, awards and Instagram still need verification."
    ),
    // Benelux — Netherlands
    RoasterProfile(
        name = "DAK Coffee Roasters", area = "Amsterdam", country = "Netherlands", micro = "Unknown",
        description = "Amsterdam specialty roaster with a strong focus on expressive coffees, sourcing and roasting. Its showroom offers coffee to drink and beans to take home.",
        website = "https://www.dakcoffeeroasters.com",
        locations = listOf(RoasterLocation("DAK Showroom", "Bellamyplein 16H, 1053 AS", "Amsterdam, Netherlands", "Showroom / coffee bar")),
        note = "The Nieuwendammerdijk roasting facility is not open to the public, so it is not listed as a customer-facing location."
    ),
    RoasterProfile(
        name = "Friedhats Coffee Roasters", area = "Amsterdam", country = "Netherlands", micro = "Unknown",
        description = "Independent Amsterdam specialty roaster known for distinctive, carefully sourced coffees and a playful approach to roasting.",
        website = "https://friedhats.com",
        locations = listOf(RoasterLocation("Fuku Café", "Bos en Lommerweg 136, 1055 ED", "Amsterdam, Netherlands", "Coffee shop / retail")),
        note = "The roastery itself is not open for general visits; visits are arranged for wholesale accounts by appointment."
    ),
    RoasterProfile(
        name = "LOT61 Coffee Roasters", area = "Amsterdam · Leidschendam · Almere", country = "Netherlands", micro = "Unknown",
        description = "Amsterdam-founded specialty coffee roaster with cafés, retail coffee and a dedicated roasting operation.",
        website = "https://lot61.com",
        locations = listOf(
            RoasterLocation("Centraal Station", "Oosterdoksstraat 4, 1011 DK", "Amsterdam, Netherlands", "Coffee shop"),
            RoasterLocation("Kinkerstraat", "Kinkerstraat 112, 1053 ED", "Amsterdam, Netherlands", "Coffee shop"),
            RoasterLocation("Hendrik Jacobszstraat", "Hendrik Jacobszstraat 18, 1075 PD", "Amsterdam, Netherlands", "Coffee shop"),
            RoasterLocation("Westfield", "Liguster 52, 2262 AC", "Leidschendam, Netherlands", "Coffee shop"),
            RoasterLocation("Almere", "Stationsplein 42, 1315 KT", "Almere, Netherlands", "Coffee shop")
        ),
        note = "The Archangelkade roasting address is not listed as a shop."
    ),
    RoasterProfile(
        name = "Rum Baba Coffee Roasters", area = "Amsterdam · Haarlem", country = "Netherlands", micro = "Unknown",
        description = "Independent Amsterdam specialty roastery with a focus on carefully sourced coffees, expressive roast profiles and neighbourhood coffee bars.",
        website = "https://rumbaba.nl",
        locations = listOf(
            RoasterLocation("Elandsgracht", "Elandsgracht 134", "Amsterdam, Netherlands", "Brew bar / shop"),
            RoasterLocation("East", "Pretoriusstraat 15", "Amsterdam, Netherlands", "Bakery / coffee shop")
        ),
        note = "The Haarlem roastery is available for order pickup by appointment; it is not listed as a regular walk-in café."
    ),
    RoasterProfile(
        name = "Manhattan Coffee Roasters", area = "Rotterdam", country = "Netherlands", micro = "Unknown",
        description = "Rotterdam specialty coffee roaster known for carefully selected lots and distinctive, modern roasting.",
        website = "https://manhattancoffeeroasters.com",
        note = "A Rotterdam base is listed by specialty-coffee directories, but a current public-facing shop address has not yet been confirmed."
    ),
    // Benelux — Belgium
    RoasterProfile(
        name = "Andy Roasters", area = "Antwerp", country = "Belgium", micro = "Unknown",
        description = "Antwerp roaster founded by coffee professionals, roasting its own coffees and serving them in its cafés.",
        website = "https://andy-roasters.be",
        locations = listOf(
            RoasterLocation("Flagship Store", "Amerikalei 82, 2000", "Antwerp, Belgium", "Coffee shop"),
            RoasterLocation("Cafe Betty", "Belpairestraat 28, 2600", "Berchem, Antwerp, Belgium", "Coffee shop")
        )
    ),
    RoasterProfile(
        name = "Caffenation Specialty Coffee Roasters", area = "Antwerp", country = "Belgium", micro = "Unknown",
        description = "Antwerp-born specialty roaster with a long-standing focus on freshly roasted coffee, espresso and filter.",
        website = "https://caffenation.be",
        locations = listOf(
            RoasterLocation("PAKT", "Lamorinièrestraat 161, 2018", "Antwerp, Belgium", "Coffee bar / roastery / offices"),
            RoasterLocation("City Centre", "Mechelsesteenweg 16, 2000", "Antwerp, Belgium", "Coffee bar")
        )
    ),
    RoasterProfile(
        name = "Wide Awake Coffee Roasters", area = "Brussels · Ixelles", country = "Belgium", micro = "Unknown",
        description = "Brussels specialty coffee roaster with cafés focused on hospitality, carefully prepared coffee and a changing selection.",
        website = "https://wideawake.coffee",
        locations = listOf(
            RoasterLocation("Saint-Catherine", "Rue Sainte-Catherine 2, 1000", "Brussels, Belgium", "Flagship café / shop"),
            RoasterLocation("Flagey", "Rue Lesbroussart 34, 1050", "Ixelles, Belgium", "Espresso bar")
        ),
        note = "The roastery address at Parc Magellan, Rue de Ransbeek 230 1A, is not listed as a public shop."
    ),
    // Belgium — additional independent and traditional roasters (provisional entries)
    RoasterProfile(
        name = "Kolonel Coffee", area = "Antwerp", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Antwerp. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://kolonelcoffee.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Normo Coffee", area = "Antwerp", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Antwerp. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://normo.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Rush Rush Coffee Roasters", area = "Antwerp", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Antwerp. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://rushrush.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Cross Roast", area = "Antwerp", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Antwerp. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://crossroast.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Cuperus", area = "Antwerp", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Antwerp. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://cuperus.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Koffie Verheyen", area = "Antwerp", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Antwerp. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Mattheus B", area = "Antwerp", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Antwerp. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "St Michel", area = "Antwerp", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Antwerp. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Koffiebranderij Kavus", area = "Antwerp", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Antwerp. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Golden Bean", area = "Antwerp", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Antwerp. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Café7", area = "Antwerp", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Antwerp. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Parlor Coffee Roasters", area = "Brussels", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Brussels. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://parlor.coffee",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Belga & Co", area = "Brussels", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Brussels. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://belga-co.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Corica", area = "Brussels", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Brussels. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://corica.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Café Velvet", area = "Jodoigne", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Jodoigne. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Aksum Coffee House", area = "Brussels", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Brussels. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Javry", area = "Brussels", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Brussels. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://javry.com",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Black Potion Coffee", area = "Brussels", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Brussels. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://blackpotioncoffee.com",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Boo Coffee Roasters", area = "Brussels", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Brussels. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://boocoffee.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Café Capitale", area = "Brussels", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Brussels. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://cafecapitale.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Café Colombia", area = "Brussels", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Brussels. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://cafecolombia.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Gust Coffee Roasters", area = "Beersel", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Beersel. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://gust.coffee",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Natural Caffè", area = "Brussels", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Brussels. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Winok", area = "Brussels", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Brussels. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://winok.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Santos Palace", area = "Brussels", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Brussels. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "MOK Coffee", area = "Brussels", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Brussels. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://mokcoffee.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "WAY Coffee Roasters", area = "Ghent", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Ghent. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://waycoffee.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Full Circle Coffee", area = "Ghent", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Ghent. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://fullcircle.coffee",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Mokabon", area = "Ghent", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Ghent. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Café Labath", area = "Ghent", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Ghent. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Vandekerckhove's Koffiebranderij", area = "Ghent", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Ghent. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://vandekerckhove-koffie.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Koffiebranderij De Draak", area = "Ghent", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Ghent. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Koffiebranderij Sao Paulo", area = "Ghent", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Ghent. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Drip Coffee", area = "Zottegem", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Zottegem. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Hoorens Koffie", area = "Zottegem", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Zottegem. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Viva Sara", area = "Vichte", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Vichte. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://vivasara.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Maeskes Roem", area = "Moorsel", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Moorsel. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://maeskesroem.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Maximiliaan", area = "Ghent", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Ghent. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "King's Koffie", area = "Ghent", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Ghent. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Madmum Coffee Roasters", area = "Leuven", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Leuven. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://madmumcoffee.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "NAFT Coffee Roasters", area = "Leuven", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Leuven. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://naft.coffee",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Onan", area = "Leuven", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Leuven. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "NAGA Coffee", area = "Rixensart", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Rixensart. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://nagacoffee.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Ross Specialty Coffee Roastery", area = "Leuven", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Leuven. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Dees Koffiebranders", area = "Bruges", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Bruges. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://dees.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Jolicoeur Coffee Roasters", area = "Mons", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Mons. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://jolicoeur.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Matubu Coffee Roastery", area = "Bruges", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Bruges. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://matubu.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Koffiebranderij Grymonprez", area = "Bruges", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Bruges. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Carantos", area = "Blankenberge", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Blankenberge. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://carantos.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Brazila", area = "Bruges", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Bruges. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Two Oh Five", area = "Maasmechelen", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Maasmechelen. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://twoohfive.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Delahaut", area = "Namur", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Namur. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Charles Liégeois", area = "Thimister", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Thimister. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://charlesliegeois.com",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "2:AM Coffee", area = "Namur", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Namur. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "STYLE COFFEE", area = "Merchtem", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Merchtem. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Fabo Coffee Roasters", area = "Mol", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Mol. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Ray & Jules", area = "Hamme-Mille", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Hamme-Mille. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://ray-jules.com",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Torrefactory", area = "Mont-Saint-Guibert", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Mont-Saint-Guibert. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://torrefactory.coffee",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "De Witte Zwaan", area = "Lievegem", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Lievegem. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "My Father Coffee", area = "Aalst", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Aalst. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Sas Coffee", area = "Beerse", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Beerse. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Taeymans Coffee Roasters", area = "Minderhout", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Minderhout. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Monx Roasters", area = "Belgium", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Belgium. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "De Lou", area = "Pelt", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Pelt. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Stroobants Koffie", area = "Diest", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Diest. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        website = "https://stroobantskoffie.be",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    RoasterProfile(
        name = "Koffiebranderij Deprez", area = "Vichte", country = "Belgium", micro = "Unknown",
        description = "Belgian coffee roaster or coffee-roasting business based in Vichte. The directory entry is included from the Belgian roaster review list; specific sourcing, roasting details and public-facing locations are being verified.",
        note = "Public retail address, Instagram and detailed profile information still need verification. This is a provisional directory entry."
    ),
    // Benelux — Luxembourg
    RoasterProfile(
        name = "Feierboun Coffeeroasters", area = "Luxembourg City · Bascharage", country = "Luxembourg", micro = "Unknown",
        description = "Luxembourg specialty roaster established in 2020, producing single-farm coffees and specialty blends with an emphasis on responsible sourcing.",
        website = "https://www.feierboun.lu",
        locations = listOf(
            RoasterLocation("Luxembourg City Coffeeshop", "3 Rue des Bains, L-1212", "Luxembourg City, Luxembourg", "Coffee shop"),
            RoasterLocation("Roastery & Headquarters", "34 Boulevard J-F Kennedy, L-4930", "Bascharage, Luxembourg", "Roastery (by appointment)")
        )
    ),
    // Germany
    RoasterProfile(
        name = "Bonanza Coffee Roasters", area = "Berlin", country = "Germany", micro = "Unknown",
        description = "Berlin specialty coffee pioneer, roasting since 2006 with an emphasis on clarity, precision and expressive coffees.",
        website = "https://bonanzacoffee.de",
        locations = listOf(
            RoasterLocation("Mitte", "Alte Schönhauser Straße 15, 10119", "Berlin, Germany", "Coffee shop"),
            RoasterLocation("Jägerstraße", "Jägerstraße 58–60, 10117", "Berlin, Germany", "Coffee shop"),
            RoasterLocation("Roastery", "Adalbertstraße 70, 10999", "Berlin, Germany", "Roastery café"),
            RoasterLocation("Oderberger", "Oderberger Straße 35, 10435", "Berlin, Germany", "Coffee shop")
        )
    ),
    RoasterProfile(
        name = "THE BARN Coffee Roasters", area = "Berlin · Munich", country = "Germany", micro = "Unknown",
        description = "Berlin specialty coffee roaster founded in 2010, known for transparent sourcing, single-origin coffees and a network of cafés.",
        website = "https://thebarn.de",
        locations = listOf(
            RoasterLocation("Roastery", "Wattstraße 10–13, 13355", "Berlin, Germany", "Roastery (not open to the public)"),
            RoasterLocation("Mitte", "Auguststraße 58, 10119", "Berlin, Germany", "Coffee shop"),
            RoasterLocation("Rosenthaler Platz", "Weinbergsweg 1, 10119", "Berlin, Germany", "Coffee shop"),
            RoasterLocation("Hackescher Markt", "Neue Schönhauser Straße 12, 10178", "Berlin, Germany", "Coffee shop"),
            RoasterLocation("Neukölln", "Friedelstraße 27, 12047", "Berlin, Germany", "Coffee shop"),
            RoasterLocation("Checkpoint Charlie", "Friedrichstraße 215, 10969", "Berlin, Germany", "Coffee shop"),
            RoasterLocation("München Altstadt", "Tal 15, 80331", "Munich, Germany", "Coffee shop"),
            RoasterLocation("München Glockenbachviertel", "Müllerstraße 38, 80469", "Munich, Germany", "Coffee shop")
        ),
        note = "The Wattstraße production roastery is not open to the public; cafés are listed separately."
    ),
    RoasterProfile(
        name = "Five Elephant", area = "Berlin", country = "Germany", micro = "Unknown",
        description = "Berlin specialty coffee roaster and bakery founded in 2010, combining carefully sourced coffee with a strong café and food programme.",
        website = "https://www.fiveelephant.com",
        locations = listOf(
            RoasterLocation("Mitte", "Alte Schönhauser Straße 14, 10119", "Berlin, Germany", "Coffee shop"),
            RoasterLocation("Kollwitz", "Kollwitzstraße 98, 10435", "Berlin, Germany", "Coffee shop"),
            RoasterLocation("Schwedter", "Schwedter Straße 11, 10119", "Berlin, Germany", "Coffee shop"),
            RoasterLocation("Kreuzberg", "Glogauer Straße 5, 10999", "Berlin, Germany", "Coffee shop / bakery")
        )
    ),
    RoasterProfile(
        name = "Flying Roasters", area = "Berlin", country = "Germany", micro = "Unknown",
        description = "Berlin roastery sourcing directly from producing cooperatives, with a focus on organic coffee and fair working conditions.",
        website = "https://flyingroasters.de",
        locations = listOf(RoasterLocation("Kaffeeladen", "Hochstraße 34, 13357", "Berlin, Germany", "Coffee shop / retail"))
    ),
    RoasterProfile(
        name = "19grams Coffee Roasters", area = "Berlin", country = "Germany", micro = "Unknown",
        description = "Berlin coffee roaster whose story began in Friedrichshain in 2002. Operates cafés where customers can drink coffee and buy freshly roasted beans.",
        website = "https://19grams.coffee",
        locations = listOf(
            RoasterLocation("Alex Roastery & Lab", "Karl-Liebknecht-Straße 13, 10178", "Berlin, Germany", "Roastery café / lab"),
            RoasterLocation("Boxi", "Boxhagener Straße 74, 10245", "Berlin, Germany", "Coffee shop"),
            RoasterLocation("Chaussee", "Chausseestraße 36, 10115", "Berlin, Germany", "Coffee shop"),
            RoasterLocation("Schlesi", "Schlesische Straße 38, 10997", "Berlin, Germany", "Coffee shop")
        )
    ),
    // Sweden
    RoasterProfile(
        name = "Drop Coffee Roasters", area = "Stockholm", country = "Sweden", micro = "Unknown",
        description = "Independent Stockholm specialty roastery and café, roasting in small batches since 2013. Known for transparent sourcing and clear, bright roast profiles.",
        website = "https://www.dropcoffee.com",
        locations = listOf(RoasterLocation("Mariatorget Café", "Wollmar Yxkullsgatan 10, 118 50", "Stockholm, Sweden", "Coffee shop / roastery café")),
        awards = "Silver medal at the World Coffee Roasting Championship."
    ),
    RoasterProfile(
        name = "Koppi Roasters", area = "Helsingborg", country = "Sweden", micro = "Unknown",
        description = "Specialty coffee roastery founded in 2007 by Anne Lunell and Charles Nystrand, working closely with coffee producers and roasting to highlight each coffee's origin.",
        website = "https://koppi.se",
        locations = listOf(RoasterLocation("Koppi Roastery", "Cindersgatan 8, 252 25", "Helsingborg, Sweden", "Roastery (visits by appointment)")),
        note = "The roastery is visited by appointment; no regular public café address is listed."
    )
)

private fun roasterBroadArea(roaster: RoasterProfile): String {
    val places = (listOf(roaster.area) + roaster.locations.map { it.city })
        .joinToString(" ").lowercase()
    return when (roaster.country.lowercase()) {
        "denmark" -> when {
            listOf("aarhus", "odder", "silkeborg", "herning").any { it in places } -> "Greater Aarhus"
            listOf("copenhagen", "frederiksberg", "nordhavn", "charlottenlund").any { it in places } -> "Greater Copenhagen"
            listOf("aalborg").any { it in places } -> "North Jutland"
            listOf("aarup", "odense", "funen").any { it in places } -> "Funen"
            listOf("helsingør", "værløse").any { it in places } -> "North Zealand"
            listOf("ebeltoft").any { it in places } -> "Djursland"
            else -> roaster.area.substringBefore("·").trim()
        }
        "portugal" -> when {
            listOf("lisbon", "lisboa", "oeiras", "cascais", "são domingos de rana").any { it in places } -> "Greater Lisbon"
            listOf("porto", "gaia", "vila nova de gaia").any { it in places } -> "Greater Porto"
            listOf("albufeira", "lagos", "algarve").any { it in places } -> "Algarve"
            else -> roaster.area.substringBefore("·").trim()
        }
        "netherlands" -> when {
            "amsterdam" in places || "leidschendam" in places || "almere" in places -> "Greater Amsterdam"
            "rotterdam" in places -> "Rotterdam"
            "haarlem" in places -> "Haarlem"
            else -> roaster.area.substringBefore("·").trim()
        }
        "belgium" -> when {
            "antwerp" in places || "antwerpen" in places || "berchem" in places -> "Antwerp"
            "brussels" in places || "ixelles" in places -> "Brussels"
            else -> roaster.area.substringBefore("·").trim()
        }
        "luxembourg" -> when {
            "luxembourg city" in places || "luxemburg-city" in places || "bascharage" in places -> "Luxembourg"
            else -> roaster.area.substringBefore("·").trim()
        }
        "germany" -> when {
            "berlin" in places -> "Berlin"
            "munich" in places || "münchen" in places -> "Munich"
            else -> roaster.area.substringBefore("·").trim()
        }
        "sweden" -> when {
            "stockholm" in places || "rosersberg" in places -> "Stockholm"
            "helsingborg" in places -> "Skåne"
            else -> roaster.area.substringBefore("·").trim()
        }
        else -> roaster.area.substringBefore("·").trim()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoasterDirectoryScreen(dao: BeanDao, onBack: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val beans by dao.beansWithLatestBag().collectAsState(emptyList())
    // The order is shuffled for each fresh directory entry, then remains stable while browsing.
    val randomizedRoasters = remember { ROASTERS.shuffled() }
    var selectedRoaster by remember { mutableStateOf<RoasterProfile?>(null) }
    var search by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf("All countries") }
    var selectedArea by remember { mutableStateOf("All areas") }
    var countryMenu by remember { mutableStateOf(false) }
    var areaMenu by remember { mutableStateOf(false) }
    val countries = remember(randomizedRoasters) {
        listOf("All countries") + randomizedRoasters.map { it.country }.distinct().sorted()
    }
    val areasForCountry = remember(randomizedRoasters, selectedCountry) {
        if (selectedCountry == "All countries") emptyList()
        else randomizedRoasters.filter { it.country.equals(selectedCountry, ignoreCase = true) }
            .map { roasterBroadArea(it) }
            .distinct()
            .sorted()
    }
    val visibleRoasters = randomizedRoasters.filter { roaster ->
        val matchesSearch = search.isBlank() || listOf(roaster.name, roaster.area, roaster.description)
            .any { it.contains(search.trim(), ignoreCase = true) } ||
            roaster.aliases.any { it.contains(search.trim(), ignoreCase = true) }
        val matchesCountry = selectedCountry == "All countries" ||
            roaster.country.equals(selectedCountry, ignoreCase = true)
        val matchesArea = selectedArea == "All areas" ||
            roasterBroadArea(roaster).equals(selectedArea, ignoreCase = true)
        matchesSearch && matchesCountry && matchesArea
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            SectionSwitchBar(label = "My Beans", isRoasters = true, onClick = onBack)
        },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(selectedRoaster?.name ?: "Roasters", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = { if (selectedRoaster != null) selectedRoaster = null else onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DarkRoast)
                    }
                }
            )
        }
    ) { padding ->
        if (selectedRoaster == null) {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                item {
                    Text("Discover coffee roasters", color = DarkRoast.copy(alpha = .78f),
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = search, onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Search roasters or cities") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        colors = beanTextFieldColors()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { countryMenu = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Public, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(selectedCountry, modifier = Modifier.weight(1f), maxLines = 1)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose country")
                            }
                            DropdownMenu(expanded = countryMenu, onDismissRequest = { countryMenu = false }) {
                                countries.forEach { country ->
                                    DropdownMenuItem(
                                        text = { Text(country) },
                                        onClick = {
                                            selectedCountry = country
                                            selectedArea = "All areas"
                                            countryMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        Box(Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { if (selectedCountry != "All countries") areaMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = selectedCountry != "All countries"
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(selectedArea, modifier = Modifier.weight(1f), maxLines = 1)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose area")
                            }
                            DropdownMenu(expanded = areaMenu, onDismissRequest = { areaMenu = false }) {
                                (listOf("All areas") + areasForCountry).forEach { area ->
                                    DropdownMenuItem(
                                        text = { Text(area) },
                                        onClick = { selectedArea = area; areaMenu = false }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("${visibleRoasters.size} roasters", style = MaterialTheme.typography.titleSmall, color = DarkRoast)
                        Text("Random order", style = MaterialTheme.typography.labelSmall, color = DarkRoast.copy(alpha = .7f))
                    }
                }
                items(visibleRoasters, key = { it.name }) { roaster ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LatteVariant, contentColor = DarkRoast),
                        border = BorderStroke(1.dp, DarkRoast.copy(alpha = .24f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    roaster.name,
                                    modifier = Modifier.weight(1f).clickable {
                                        val url = roaster.instagram
                                        if (url != null) uriHandler.openUri(url) else selectedRoaster = roaster
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DarkRoast
                                )
                            }
                            Text("${roaster.area} · ${roaster.country}", style = MaterialTheme.typography.bodySmall, color = DarkRoast.copy(alpha = .72f))
                            Text(roaster.description, style = MaterialTheme.typography.bodyMedium, color = DarkRoast)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { selectedRoaster = roaster }) {
                                    Text("View profile")
                                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                                }
                                Text(
                                    if (roaster.instagram != null) "Instagram ↗" else "Instagram not added",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DarkRoast.copy(alpha = .7f)
                                )
                            }
                        }
                    }
                }
                if (visibleRoasters.isEmpty()) item {
                    Text("No roasters match this search.", modifier = Modifier.padding(20.dp), color = DarkRoast)
                }
            }
        } else {
            val roaster = selectedRoaster!!
            val connectedBeans = beans.filter { row ->
                val name = row.bean.roastery.trim()
                name.equals(roaster.name, ignoreCase = true) ||
                    roaster.aliases.any { it.equals(name, ignoreCase = true) }
            }
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("${roaster.area} · ${roaster.country}", color = DarkRoast.copy(alpha = .72f), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text(roaster.description, color = DarkRoast, style = MaterialTheme.typography.bodyLarge)
                    if (roaster.awards.isNotBlank()) {
                        Text("Awards & recognition", style = MaterialTheme.typography.titleMedium, color = DarkRoast)
                        Text(roaster.awards, color = DarkRoast)
                    }
                    if (roaster.note.isNotBlank()) {
                        Text(roaster.note, color = DarkRoast.copy(alpha = .78f), style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Links", style = MaterialTheme.typography.titleMedium, color = DarkRoast)
                    if (roaster.instagram != null) {
                        TextButton(onClick = { uriHandler.openUri(roaster.instagram) }) {
                            Text("Instagram ↗")
                        }
                    }
                    if (roaster.website != null) {
                        TextButton(onClick = { uriHandler.openUri(roaster.website) }) {
                            Text("Website ↗")
                        }
                    }
                    TextButton(onClick = { uriHandler.openUri("https://europeancoffeetrip.com/?s=${Uri.encode(roaster.name)}") }) {
                        Text("Find on European Coffee Trip ↗")
                    }
                    roaster.extraLinks.forEach { link ->
                        TextButton(onClick = { uriHandler.openUri(link.url) }) { Text("${link.label} ↗") }
                    }
                    Text("Locations (${roaster.locations.size})", style = MaterialTheme.typography.titleMedium, color = DarkRoast)
                    if (roaster.locations.isEmpty()) {
                        Text("Customer-facing addresses still need to be added.", color = DarkRoast.copy(alpha = .7f))
                    }
                }
                items(roaster.locations) { location ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LatteVariant),
                        border = BorderStroke(1.dp, DarkRoast.copy(alpha = .2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(location.name, style = MaterialTheme.typography.titleSmall, color = DarkRoast)
                            Text(location.type, style = MaterialTheme.typography.labelSmall, color = DarkRoast.copy(alpha = .7f))
                            Text(location.address, color = DarkRoast)
                            Text(location.city, color = DarkRoast)
                            if (location.hours.isNotBlank()) Text(location.hours, style = MaterialTheme.typography.bodySmall, color = DarkRoast)
                            TextButton(onClick = {
                                val query = Uri.encode("${location.address}, ${location.city}")
                                uriHandler.openUri("https://www.google.com/maps/search/?api=1&query=$query")
                            }) {
                                Icon(Icons.Default.Map, contentDescription = null)
                                Spacer(Modifier.width(5.dp))
                                Text("Open in Maps")
                            }
                        }
                    }
                }
                item {
                    Text("Your coffees from this roaster", style = MaterialTheme.typography.titleMedium, color = DarkRoast)
                    if (connectedBeans.isEmpty()) {
                        Text("No coffees linked yet.", color = DarkRoast.copy(alpha = .7f))
                    } else {
                        connectedBeans.forEach { row ->
                            TextButton(onClick = { }) {
                                Text(row.bean.name.ifBlank { row.bean.roastery }, color = DarkRoast)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun SectionSwitchBar(
    label: String,
    isRoasters: Boolean,
    onClick: () -> Unit,
    onAdd: (() -> Unit)? = null
) {
    Surface(
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                onClick = onClick,
                color = CoffeeCard,
                contentColor = CoffeeCardText,
                shape = MaterialTheme.shapes.large,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    if (isRoasters) {
                        BeanActionIcon(
                            BeanIconAction.BEAN,
                            "My Beans",
                            modifier = Modifier.size(23.dp),
                            color = CoffeeCardText
                        )
                    } else {
                        Icon(Icons.Default.Storefront, contentDescription = "Roasters")
                    }
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
            if (onAdd != null) {
                Surface(
                    onClick = onAdd,
                    color = CoffeeCard,
                    contentColor = CoffeeCardText,
                    shape = MaterialTheme.shapes.large,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Text("Add bean", style = MaterialTheme.typography.labelLarge)
                        BeanActionIcon(BeanIconAction.BEAN, "Add bean", Modifier.size(23.dp), CoffeeCardText)
                    }
                }
            }
        }
    }
}

// ---------- List ----------
private enum class BeanSortOption(val label: String) {
    ROAST_DATE("Roast date"),
    RECENTLY_ADDED("Recently added"),
    NAME("Name")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeanList(dao: BeanDao, onOpen: (Long) -> Unit, onAdd: () -> Unit, onRoasters: () -> Unit, onInsights: () -> Unit, darkMode: Boolean, onThemeChange: (Boolean) -> Unit) {
    val beans by dao.beansWithLatestBag().collectAsState(emptyList())
    var sortOption by remember { mutableStateOf(BeanSortOption.ROAST_DATE) }
    // Recently added defaults to newest first; Name defaults to A–Z.
    var ascending by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var settingsOpen by remember { mutableStateOf(false) }
    var remindersOn by remember { mutableStateOf(shotRemindersEnabled(context)) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val scope = rememberCoroutineScope()
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var restoreError by remember { mutableStateOf<String?>(null) }
    var pendingBackup by remember { mutableStateOf<BackupContents?>(null) }
    var pendingPreview by remember { mutableStateOf<BackupPreview?>(null) }
    var restoreBusy by remember { mutableStateOf(false) }
    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri)
                        ?: error("Could not open the selected location.")
                    output.use { BeanLogBackup.write(dao, it) }
                }
            }
            backupMessage = if (result.isSuccess) "Your BeanLog backup was saved successfully." else
                "The backup could not be created: ${result.exceptionOrNull()?.localizedMessage ?: "unknown error"}"
        }
    }
    val openBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            val result = runCatching {
                val parsed = withContext(Dispatchers.IO) {
                    val input = context.contentResolver.openInputStream(uri)
                        ?: error("Could not open the selected backup.")
                    input.use { BeanLogBackup.read(it) }
                }
                val mergePreview = withContext(Dispatchers.IO) {
                    BeanLogBackup.previewMerge(dao, parsed.second)
                }
                mergePreview to parsed.second
            }
            result.onSuccess { (preview, contents) ->
                pendingPreview = preview
                pendingBackup = contents
            }.onFailure {
                restoreError = it.localizedMessage ?: "This file could not be read as a BeanLog backup."
            }
        }
    }

    val sortedBeans = remember(beans, sortOption, ascending) {
        when (sortOption) {
            BeanSortOption.ROAST_DATE -> beans.sortedWith(Comparator { a, b ->
                val da=a.latestRoastDate; val db=b.latestRoastDate
                when { da==null && db!=null -> 1; da!=null && db==null -> -1
                    else -> { val c=(da ?: "").compareTo(db ?: ""); if(ascending)c else -c } }
            })
            BeanSortOption.RECENTLY_ADDED -> beans.sortedWith(
                compareBy<BeanWithLatestBag> { it.latestBagId == 0L }
                    .thenBy { if (ascending) it.latestBagId else -it.latestBagId }
                    .thenBy { it.bean.name.lowercase() }
            )
            BeanSortOption.NAME -> {
                val byName = compareBy<BeanWithLatestBag> { it.bean.name.lowercase() }
                    .thenBy { it.bean.roastery.lowercase() }
                beans.sortedWith(if (ascending) byName else byName.reversed())
            }
        }
            // Beans with at least one unfinished bag always stay above beans with no available bags.
            .sortedByDescending { it.latestBrewAtMillis }
            .sortedBy { if (it.openBagCount > 0) 0 else 1 }
            .map { Triple(it.bean, it.openBagCount, it.bean.liked) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.height(120.dp),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Text("My Beans", style = MaterialTheme.typography.headlineMedium)
                },
                actions = {
                    IconButton(onClick = onInsights) {
                        Icon(Icons.Default.BarChart, "Taste and coffee history", tint = DarkRoast)
                    }
                    IconButton(onClick = { settingsOpen = true }) {
                        Icon(Icons.Default.Settings, "Settings", tint = DarkRoast)
                    }
                }
            )
        },
        bottomBar = {
            SectionSwitchBar(label = "Roasters", isRoasters = false, onClick = onRoasters, onAdd = onAdd)
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    OutlinedButton(onClick = { sortMenuExpanded = true }) {
                        Text("Sort: ${sortOption.label} ${if (ascending) "↑" else "↓"}")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose sort order")
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        BeanSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(option.label + if (sortOption == option) {
                                        " ${if (ascending) "↑" else "↓"}"
                                    } else "")
                                },
                                onClick = {
                                    if (sortOption == option) {
                                        ascending = !ascending
                                    } else {
                                        sortOption = option
                                        ascending = option == BeanSortOption.NAME
                                    }
                                    sortMenuExpanded = false
                                },
                                leadingIcon = {
                                    if (sortOption == option) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected")
                                    }
                                }
                            )
                        }
                    }
                }
            }
            items(sortedBeans, key = { it.first.id }) { (b, count, beanLiked) ->
                Card(onClick = { onOpen(b.id) }, modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (count == 0) CoffeeCard.copy(alpha = 0.78f) else CoffeeCard,
                        contentColor = CoffeeCardText
                    ),
                    border = BorderStroke(1.dp, DarkRoast.copy(alpha = if (count == 0) 0.16f else 0.28f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (count == 0) 1.dp else 5.dp)) {
                    Row(Modifier.padding(if (count == 0) 8.dp else 12.dp).alpha(if(count==0) .88f else 1f), verticalAlignment=Alignment.CenterVertically) {
                        val imageSize = if (count == 0) 44.dp else 64.dp
                        if (b.photoPath != null) BeanPhoto(b.photoPath, Modifier.size(imageSize * 0.9f, imageSize))
                        else CoffeeBagIcon(Modifier.size(imageSize))
                        Spacer(Modifier.width(if (count == 0) 8.dp else 12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(b.name, modifier = Modifier.weight(1f), style=if (count == 0) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium)
                                if (beanLiked) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Liked coffee",
                                        tint = Color(0xFF27834A), modifier = Modifier.size(22.dp))
                                }
                            }
                            Text("${b.roastery} · ${countryFlag(b.roasteryCountry)}", style=if (count == 0) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium)
                            Text(b.roastLevel,style=MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
    if(settingsOpen) AlertDialog(
        onDismissRequest={settingsOpen=false},
        title={Text("Settings")},
        text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Text("Dark appearance",modifier=Modifier.weight(1f))
                Switch(checked=darkMode,onCheckedChange=onThemeChange)
            }
            HorizontalDivider()
            Row(verticalAlignment=Alignment.CenterVertically){
                Text("Brew feedback reminder (30 min)",modifier=Modifier.weight(1f))
                Switch(checked=remindersOn,onCheckedChange={enabled->
                    remindersOn=enabled
                    setShotRemindersEnabled(context,enabled)
                    if(enabled && Build.VERSION.SDK_INT>=33 &&
                        ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                })
            }
            Text("When enabled, BeanLog asks whether you liked each brew 30 minutes after it is saved. Notifications must also be allowed by Android.",style=MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Text("Backup & Restore",style=MaterialTheme.typography.titleMedium,color=DarkRoast)
            Text("Save your beans, bags, brew history, notes and coffee photos in one backup file.",style=MaterialTheme.typography.bodySmall)
            OutlinedButton(
                onClick={
                    settingsOpen=false
                    val filename="BeanLog-backup-${SimpleDateFormat("yyyyMMdd-HHmm",Locale.US).format(Date())}.zip"
                    createBackupLauncher.launch(filename)
                },
                modifier=Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Backup,contentDescription=null)
                Spacer(Modifier.width(8.dp))
                Text("Back up BeanLog")
            }
            OutlinedButton(
                onClick={
                    settingsOpen=false
                    openBackupLauncher.launch(arrayOf("application/zip","application/octet-stream"))
                },
                modifier=Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Restore,contentDescription=null)
                Spacer(Modifier.width(8.dp))
                Text("Restore from backup")
            }
            Text("Choose a location such as your phone, Google Drive, Dropbox or another file service.",style=MaterialTheme.typography.bodySmall)
        }},
        confirmButton={TextButton(onClick={settingsOpen=false}){Text("Done")}}
    )
    pendingBackup?.let { contents ->
        val preview = pendingPreview
        if (preview != null) AlertDialog(
            onDismissRequest={if(!restoreBusy){pendingBackup=null;pendingPreview=null}},
            title={Text("Restore BeanLog backup?",color=DarkRoast)},
            text={
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("This backup contains:")
                    Text("• ${preview.beans} coffee beans")
                    Text("• ${preview.bags} coffee bags")
                    Text("• ${preview.shots} brews")
                    Text("Choose how to restore this data.")
                    Text(
                        "Merge will add ${preview.beansToAdd} beans, ${preview.bagsToAdd} bags and ${preview.shotsToAdd} shots. Existing matches will be skipped.",
                        style=MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Replace permanently clears your current beans, bags and shot history, then restores all ${preview.beans} beans, ${preview.bags} bags and ${preview.shots} brews from this backup.",
                        style=MaterialTheme.typography.bodySmall
                    )
                    if(restoreBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            confirmButton={
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    TextButton(enabled=!restoreBusy,onClick={
                        restoreBusy=true
                        scope.launch {
                            val result=runCatching {
                                withContext(Dispatchers.IO) { BeanLogBackup.restore(context,dao,contents,false) }
                            }
                            restoreBusy=false
                            pendingBackup=null;pendingPreview=null
                            backupMessage=result.fold(
                                { "Merge complete: added ${it.beansAdded} beans, ${it.bagsAdded} bags and ${it.shotsAdded} shots." },
                                { "Restore failed: ${it.localizedMessage ?: "unknown error"}" }
                            )
                        }
                    }) { Text("Merge") }
                    TextButton(enabled=!restoreBusy,onClick={
                        restoreBusy=true
                        scope.launch {
                            val result=runCatching {
                                withContext(Dispatchers.IO) { BeanLogBackup.restore(context,dao,contents,true) }
                            }
                            restoreBusy=false
                            pendingBackup=null;pendingPreview=null
                            backupMessage=result.fold(
                                { "Restore complete: added ${it.beansAdded} beans, ${it.bagsAdded} bags and ${it.shotsAdded} shots." },
                                { "Restore failed: ${it.localizedMessage ?: "unknown error"}" }
                            )
                        }
                    }) { Text("Replace all",color=Color(0xFF9B2C20)) }
                }
            },
            dismissButton={TextButton(enabled=!restoreBusy,onClick={pendingBackup=null;pendingPreview=null}){Text("Cancel")}},
            containerColor=CoffeeCream,titleContentColor=DarkRoast,textContentColor=DarkRoast
        )
    }
    (backupMessage ?: restoreError)?.let { message ->
        AlertDialog(
            onDismissRequest={backupMessage=null;restoreError=null},
            title={Text(if(message.contains("failed",true)||message.contains("could not",true)) "Backup / restore" else "BeanLog backup",color=DarkRoast)},
            text={Text(message,color=DarkRoast)},
            confirmButton={TextButton(onClick={backupMessage=null;restoreError=null}){Text("OK")}},
            containerColor=Color(0xFFFFFCF6),titleContentColor=DarkRoast,textContentColor=DarkRoast
        )
    }
}

@Composable
private fun beanTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = DarkRoast,
    unfocusedTextColor = DarkRoast,
    focusedLabelColor = DarkRoast,
    unfocusedLabelColor = DarkRoast.copy(alpha = 0.82f),
    focusedBorderColor = DarkRoast,
    unfocusedBorderColor = DarkRoast.copy(alpha = 0.55f),
    cursorColor = DarkRoast,
    focusedContainerColor = OatMilk.copy(alpha = 0.90f),
    unfocusedContainerColor = Color(0xFFF1E3D0).copy(alpha = 0.82f)
)

private val beanNextKeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)

private fun beanNextKeyboardActions(focusManager: androidx.compose.ui.focus.FocusManager) = KeyboardActions(
    onNext = { focusManager.moveFocus(FocusDirection.Next) }
)

@Composable
private fun FieldLabel(label: String, explanation: String) {
    var showHelp by remember(label) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Surface(
            Modifier.size(20.dp).clickable { showHelp = true },
            shape = CircleShape,
            color = DarkRoast.copy(alpha = 0.12f),
            contentColor = DarkRoast
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("?", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    if (showHelp) AlertDialog(
        onDismissRequest = { showHelp = false },
        title = { Text(label, color = DarkRoast) },
        text = { Text(explanation, color = DarkRoast) },
        confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Got it") } },
        containerColor = Color(0xFFFFFCF6), titleContentColor = DarkRoast, textContentColor = DarkRoast
    )
}

// ---------- Add / edit bean ----------
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(dao: BeanDao, id: Long, onDone: (Long, Boolean) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var roastery by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var roast by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<String?>(null) }
    var originalPhoto by remember { mutableStateOf<String?>(null) }
    var cIn by remember { mutableStateOf("") }
    var cOut by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var origin by remember { mutableStateOf("") }; var farm by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var variety by remember { mutableStateOf("") }; var processing by remember { mutableStateOf("") }

    LaunchedEffect(id) {
        if (id != 0L) dao.bean(id).first()?.let {
            roastery = it.roastery; country = it.roasteryCountry; name = it.name
            notes = it.flavourNotes; roast = it.roastLevel
            score = it.cuppingScore?.toString() ?: ""; photo = it.photoPath; originalPhoto = it.photoPath
            cIn = it.coffeeInG?.toString() ?: ""; cOut = it.coffeeOutG?.toString() ?: ""
            time = it.brewTimeSec?.toString() ?: ""
            origin=it.origin; farm=it.farm; region=it.region; variety=it.variety; processing=it.processing
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val compressedPath = compressPickedImage(ctx, uri)
            if (compressedPath != null) {
                val previousSelection = photo
                photo = compressedPath
                if (previousSelection != originalPhoto) deleteManagedPhoto(ctx, previousSelection)
            }
        }
    }

    Scaffold(containerColor = Color.Transparent, topBar = {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            title = { Text(if (id == 0L) "New bean" else "Edit bean") }, actions = {
            TextButton(onClick = {
                if (photo != originalPhoto) deleteManagedPhoto(ctx, photo)
                onDone(id, false)
            }) { Text("Cancel") }
            TextButton(enabled = roastery.isNotBlank(), onClick = {
                scope.launch {
                    val bean = Bean(id, roastery.trim(), country.trim(), name.trim(), notes.trim(), roast,
                        score.num(), photo, cIn.num(), cOut.num(), time.toIntOrNull(),
                        origin=origin.trim(), farm=farm.trim(), region=region.trim(), variety=variety.trim(), processing=processing.trim())
                    val savedId = if (id == 0L) dao.insertBean(bean) else {
                        dao.save(bean)
                        id
                    }
                    if (originalPhoto != photo) deleteManagedPhoto(ctx, originalPhoto)
                    originalPhoto = photo
                    onDone(savedId, id == 0L)
                }
            }) { Text("Save") }
        })
    }) { pad ->
        Column(Modifier.padding(pad).imePadding().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (photo != null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    BeanPhoto(photo!!, Modifier.size(150.dp, 178.dp))
                }
            }
            OutlinedButton(onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Icon(Icons.Default.AddAPhoto, null); Spacer(Modifier.width(8.dp)); Text("Add picture") }

            OutlinedTextField(roastery, { roastery = it }, label = { Text("Roastery *") }, modifier = Modifier.fillMaxWidth(), colors = beanTextFieldColors(), keyboardOptions = beanNextKeyboardOptions, keyboardActions = beanNextKeyboardActions(focusManager))
            OutlinedTextField(country, { country = it }, label = { Text("Country of roaster") }, modifier = Modifier.fillMaxWidth(), colors = beanTextFieldColors())
            OutlinedTextField(name, { name = it }, label = { Text("Coffee name") }, modifier = Modifier.fillMaxWidth(), colors = beanTextFieldColors())
            FieldLabel("Flavour notes", "These are the flavours the roaster describes for the coffee. They give you an indication of what to look for when tasting your brewed coffee and help you understand whether your brewing recipe brings out the intended characteristics.")
            OutlinedTextField(notes, { notes = it }, placeholder = { Text("e.g. chocolate, citrus, caramel") }, modifier = Modifier.fillMaxWidth(), colors = beanTextFieldColors())
            FieldLabel("Roast level", "Roast-level colours are approximate indications of how dark or light the coffee has been roasted. Lighter roasts can sometimes be more difficult to grind and extract, depending on your grinder and brewing equipment.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ROAST_LEVELS.forEach { level ->
                    val roastColor = when (level) {
                        "Light" -> Color(0xFFE8CFA8)
                        "Medium-Light" -> Color(0xFFC99A68)
                        "Medium" -> Color(0xFF986744)
                        "Medium-Dark" -> Color(0xFF65432F)
                        else -> Color(0xFF321D14)
                    }
                    val selected = roast == level
                    FilterChip(
                        selected = selected,
                        onClick = { roast = if (selected) "" else level },
                        label = { Text(level) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = roastColor.copy(alpha = 0.16f),
                            labelColor = DarkRoast,
                            selectedContainerColor = roastColor,
                            selectedLabelColor = if (level == "Light") DarkRoast else CoffeeCream
                        ),
                        border = BorderStroke(1.dp, roastColor)
                    )
                }
            }
            OutlinedTextField(score, { score = it }, label = { FieldLabel("Cupping score", "A cupping score is a standardised 100-point quality rating given to specialty coffee by trained tasters, known as Q Graders, during a sensory evaluation process called cupping.") }, modifier = Modifier.fillMaxWidth(), colors = beanTextFieldColors())
            HorizontalDivider(Modifier.padding(vertical=8.dp))
            Text("Coffee details",style=MaterialTheme.typography.titleMedium)
            OutlinedTextField(origin,{origin=it},label={FieldLabel("Origin", "Where was the coffee grown? Origin is important for transparency and traceability. Different countries, regions and growing elevations can influence flavour.")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())
            OutlinedTextField(farm,{farm=it},label={FieldLabel("Farm", "Knowing the farm helps with transparency and traceability. Different farms can produce different flavour profiles, and farm information can help you understand how and where the coffee was produced.")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())
            OutlinedTextField(region,{region=it},label={FieldLabel("Region", "The wider geographical area where the coffee was grown. Regional climate, soil, elevation and local practices can influence flavour.")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())
            OutlinedTextField(variety,{variety=it},label={FieldLabel("Variety", "Arabica and Robusta are the two best-known coffee species. Within them, there are many varieties with different characteristics, plant structures, yields and potential flavour profiles.")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())
            OutlinedTextField(processing,{processing=it},label={FieldLabel("Processing", "Coffee processing prepares the coffee seed after harvest. Common methods include Washed (fruit removed before drying), Natural (whole cherry dried around the seed), Honey (some fruit mucilage remains), Anaerobic fermentation (fermentation with limited oxygen) and Wet-hulled (parchment removed while the coffee is still wet). These methods can influence sweetness, acidity, body and fruit character.")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            FieldLabel("Recommended brew settings", "The roaster sometimes provides a recommended dose, yield and approximate brewing time. These are useful starting points. You can also enter your own recipe after experimenting and compare it with the shots you record in BeanLog to see which recipes work best and which you like or dislike.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(cIn, { cIn = it }, label = { Text("In (g)") }, modifier = Modifier.weight(1f), colors = beanTextFieldColors())
                OutlinedTextField(cOut, { cOut = it }, label = { Text("Out (g)") }, modifier = Modifier.weight(1f), colors = beanTextFieldColors())
                OutlinedTextField(time, { time = it }, label = { Text("Time (s)") }, modifier = Modifier.weight(1f), colors = beanTextFieldColors())
            }
        }
    }
}

// ---------- Detail + bags ----------
@Composable
private fun CurrencyMenu(selected: String?, allowNone: Boolean, onPick: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text(selected?.let { "${currencySymbol(it)} $it" } ?: "As paid") }
        DropdownMenu(open, { open = false }) {
            if (allowNone) DropdownMenuItem(text = { Text("As paid") }, onClick = { onPick(null); open = false })
            CURRENCIES.forEach { c ->
                DropdownMenuItem(text = { Text("${currencySymbol(c)} $c") }, onClick = { onPick(c); open = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(dao: BeanDao, id: Long, autoOpenAddBag: Boolean = false, openBagId: Long? = null, openBrewId: Long? = null, onEdit: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val bean by dao.bean(id).collectAsState(null)
    val bags by dao.bags(id).collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    var showBagDialog by remember { mutableStateOf(false) }
    var askToAddBag by remember { mutableStateOf(false) }
    var confirmDeleteBean by remember { mutableStateOf(false) }
    LaunchedEffect(autoOpenAddBag) {
        if (autoOpenAddBag) askToAddBag = true
    }
    var editingBag by remember { mutableStateOf<Bag?>(null) }
    var showFinishedBags by remember { mutableStateOf(false) }
    var displayCur by remember { mutableStateOf<String?>(null) }   // null = show as paid
    var ratesOk by remember { mutableStateOf(Rates.loaded) }
    LaunchedEffect(context) { ratesOk = Rates.load(context) }
    val b = bean ?: return

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(b.name) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onEdit) { BeanActionIcon(BeanIconAction.EDIT, "Edit") }
                    IconButton({ confirmDeleteBean = true }) {
                        BeanActionIcon(BeanIconAction.DELETE, "Delete")
                    }
                })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showBagDialog = true },
                icon = { BeanActionIcon(BeanIconAction.ADD, "Add") }, text = { Text("Add bag") })
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                if (b.photoPath != null) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        // Three times the 64 dp main-list thumbnail in both dimensions.
                        BeanPhoto(b.photoPath, Modifier.size(192.dp, 192.dp))
                    }
                }
                Text("${b.roastery} · ${countryFlag(b.roasteryCountry)}", style = MaterialTheme.typography.titleMedium)
                if (b.roastLevel.isNotBlank()) Text("Roast: ${b.roastLevel}")
                b.cuppingScore?.let { Text("Cupping score: $it") }
                if (b.flavourNotes.isNotBlank()) Text("Flavour notes: ${b.flavourNotes}")
                listOf(
                    "Origin" to (b.origin.takeIf{it.isNotBlank()}?.let{ "${countryFlag(it)} $it" } ?: ""),
                    "Farm" to b.farm, "Region" to b.region, "Variety" to b.variety, "Processing" to b.processing
                ).filter{it.second.isNotBlank()}.forEach{(k,v)->Text("$k: $v")}
                Spacer(Modifier.height(8.dp))
                Text("Recommended brew", style = MaterialTheme.typography.titleMedium)
                Text("${b.coffeeInG ?: "–"} g in → ${b.coffeeOutG ?: "–"} g out in ${b.brewTimeSec ?: "–"} s")
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { scope.launch { dao.save(b.copy(liked = !b.liked)) } },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (b.liked) Color(0xFF397A45) else DarkRoast,
                        contentColor = Color(0xFFFFFCF6)
                    )
                ) {
                    Text(if (b.liked) "✓ Liked" else "♡ Like")
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bags", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("Show in: ")
                    CurrencyMenu(displayCur, allowNone = true) { displayCur = it }
                }
                if (displayCur != null) {
                    Text(
                        "Conversions are approximate. Exchange rates are updated with app updates. Check online for exact rates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkRoast.copy(alpha = 0.78f)
                    )
                }
            }
            items(bags.filter { !it.isFinished }, key={it.id}) { bag ->
                BagCard(dao,b,bag,displayCur,{editingBag=bag},{scope.launch{dao.deleteBag(bag)}}, initiallyShowHistory = bag.id == openBagId, focusBrewId = openBrewId)
            }
            if(bags.any{it.isFinished}) {
                item { TextButton(onClick={showFinishedBags=!showFinishedBags},modifier=Modifier.fillMaxWidth()) {
                    Text("Finished bags (${bags.count{it.isFinished}})")
                    Icon(if(showFinishedBags) Icons.Default.ExpandLess else Icons.Default.ExpandMore,null)
                } }
                if(showFinishedBags) items(bags.filter{it.isFinished},key={it.id}) { bag ->
                    BagCard(dao,b,bag,displayCur,{editingBag=bag},{scope.launch{dao.deleteBag(bag)}},true, bag.id == openBagId, openBrewId)
                }
            }
        }
    }

    if (confirmDeleteBean) AlertDialog(
        onDismissRequest = { confirmDeleteBean = false },
        title = { Text("Delete coffee bean?", color = DarkRoast) },
        text = { Text("This will permanently delete this bean and all its bags and shot history. This cannot be undone.", color = DarkRoast) },
        confirmButton = {
            TextButton(onClick = {
                confirmDeleteBean = false
                scope.launch {
                    val bagIds = dao.allBags().filter { it.beanId == b.id }.map { it.id }.toSet()
                    dao.allShots().filter { it.bagId in bagIds }.forEach { deleteManagedPhoto(context, it.photoPath) }
                    dao.delete(b)
                    deleteManagedPhoto(context, b.photoPath)
                    onBack()
                }
            }) { Text("Delete", color = Color(0xFF9B2C20)) }
        },
        dismissButton = { TextButton(onClick = { confirmDeleteBean = false }) { Text("Cancel") } },
        containerColor = Color(0xFFFFFCF6), titleContentColor = DarkRoast, textContentColor = DarkRoast
    )

    if (askToAddBag) AlertDialog(
        onDismissRequest = { askToAddBag = false },
        title = { Text("Add a coffee bag?", color = DarkRoast) },
        text = { Text("Would you like to add a coffee bag for this bean?", color = DarkRoast) },
        confirmButton = { TextButton(onClick = { askToAddBag = false; showBagDialog = true }) { Text("Yes") } },
        dismissButton = { TextButton(onClick = { askToAddBag = false }) { Text("No") } },
        containerColor = Color(0xFFFFFCF6), titleContentColor = DarkRoast, textContentColor = DarkRoast
    )
    if(showBagDialog) BagDialog(null,{showBagDialog=false}) { scope.launch{dao.saveBag(it.copy(beanId=id));showBagDialog=false} }
    editingBag?.let { bag -> BagDialog(bag,{editingBag=null}) { scope.launch{dao.saveBag(it.copy(beanId=id));editingBag=null} } }
}

@Composable
private fun BagCard(dao:BeanDao,bean:Bean,bag:Bag,cur:String?,edit:()->Unit,delete:()->Unit,muted:Boolean=false,initiallyShowHistory:Boolean=false,focusBrewId:Long?=null) {
 val shots by dao.shots(bag.id).collectAsState(emptyList())
 val scope=rememberCoroutineScope(); val context=LocalContext.current
 var add by remember{mutableStateOf(false)}
 var shotDefaults by remember{mutableStateOf<Shot?>(null)}
 var showHistory by remember(bag.id, initiallyShowHistory){mutableStateOf(initiallyShowHistory)}
 var confirmDeleteBag by remember{mutableStateOf(false)}
 Card(onClick={showHistory=true},modifier=Modifier.fillMaxWidth(),
  colors=CardDefaults.cardColors(containerColor=LatteVariant),
  elevation=CardDefaults.cardElevation(defaultElevation=0.dp)) {
  Column(Modifier.padding(12.dp).alpha(if(muted) 0.55f else 1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
   Row(verticalAlignment=Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
     if (bag.roastDate.isNotBlank()) Text("Roasted ${isoToDisplay(bag.roastDate)}",style=MaterialTheme.typography.titleMedium)
     Text("${bag.weightG} g")
     if (bag.price > 0.0) {
      Text("Paid ${moneyWithCurrency(bag.price, bag.currency)} (${moneyWithCurrency(bag.price / bag.weightG.coerceAtLeast(1), bag.currency, 3)}/g)")
      cur?.let { c ->
       if (c != bag.currency) {
        Rates.convert(bag.price, bag.currency, c)?.let { convertedTotal ->
         Surface(
          color=OatMilk.copy(alpha=0.78f),
          shape=MaterialTheme.shapes.medium,
          modifier=Modifier.fillMaxWidth()
         ) {
          Column(Modifier.padding(horizontal=10.dp, vertical=7.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
           Text("Approximate conversion · $c",style=MaterialTheme.typography.labelMedium)
           Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text("Total bag price",style=MaterialTheme.typography.bodySmall)
            Text(moneyWithCurrency(convertedTotal,c),style=MaterialTheme.typography.bodySmall)
           }
           Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text("Price per gram",style=MaterialTheme.typography.bodySmall)
            Text("${moneyWithCurrency(convertedTotal / bag.weightG.coerceAtLeast(1),c,3)}/g",style=MaterialTheme.typography.bodySmall)
           }
          }
         }
        }
       }
      }
     }
     if(bag.isFinished)Text("Finished",style=MaterialTheme.typography.labelMedium)
     if(bag.recommendedGrindSetting.isNotBlank())Text("Recommended grind: ${bag.recommendedGrindSetting}")
     shotSentiment(shots)?.let { Text("Brew average: ${it.emoji} · ${it.percent}% liked",style=MaterialTheme.typography.bodySmall) }
    }
    TextButton(onClick={scope.launch{dao.saveBag(bag.copy(liked=if(bag.liked==true)null else true))}}){Text(if(bag.liked==true) "♥ Buy again" else "♡ Buy again",color=if(bag.liked==true) Color(0xFF397A45) else DarkRoast)}
    IconButton(onClick=edit){BeanActionIcon(BeanIconAction.EDIT,"Edit bag")}
    IconButton(onClick={confirmDeleteBag=true}){BeanActionIcon(BeanIconAction.DELETE,"Delete bag")}
   }
   HorizontalDivider()
   Row(verticalAlignment=Alignment.CenterVertically) {
    Text("Brews (${shots.size})",modifier=Modifier.weight(1f),style=MaterialTheme.typography.titleSmall)
    TextButton(onClick={
      scope.launch {
       val current = shots.maxByOrNull { it.id }
       val defaults = if(current != null) current
       else {
        val recommended = if(bean.coffeeInG != null && bean.coffeeOutG != null && bean.brewTimeSec != null)
         Shot(bagId=bag.id,groundCoffeeG=bean.coffeeInG,yieldG=bean.coffeeOutG,timeSec=bean.brewTimeSec,grindSetting=bag.recommendedGrindSetting)
        else null
        recommended ?: run {
         val allBags = dao.allBags().filter { it.beanId==bean.id && it.id!=bag.id }.map { it.id }.toSet()
         dao.allShots().filter { it.bagId in allBags }.maxByOrNull { it.id }
        } ?: Shot(bagId=bag.id,groundCoffeeG=18.0,yieldG=36.0,timeSec=28,grindSetting=bag.recommendedGrindSetting)
       }
       shotDefaults = defaults.copy(bagId=bag.id,liked=null,note="",pulledAtMillis=System.currentTimeMillis())
       add=true
      }
     }){BeanActionIcon(BeanIconAction.ADD,"Add shot");Text("Add brew")}
   }
   shotSentiment(shots)?.let { Text("Shot average: ${it.emoji} · ${it.percent}% liked (${it.liked}/${it.rated} shots)",style=MaterialTheme.typography.bodySmall) }
   shots.sortedByDescending { it.id }.take(2).forEach{shot->
    Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
     Text(shotSummary(shot),style=MaterialTheme.typography.bodySmall)
     if (shot.note.isNotBlank()) Text("Note: ${shot.note}",style=MaterialTheme.typography.bodySmall,color=DarkRoast.copy(alpha=.78f))
    }
   }
   if(shots.size>2) Text("Tap bag to see all ${shots.size} brews",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
  }
 }
 if(add)ShotDialog(shotDefaults,{add=false}){shot->scope.launch{val id=dao.insertShot(shot.copy(bagId=bag.id)); if(shotRemindersEnabled(context)) scheduleShotReminder(context,id); dao.saveBag(bag.copy(lastBrewAtMillis=shot.pulledAtMillis)); add=false}}
 if(showHistory) BagHistoryDialog(bag,shots,{showHistory=false},edit,scope,dao,focusBrewId)
 if(confirmDeleteBag) AlertDialog(
  onDismissRequest={confirmDeleteBag=false},
  title={Text("Delete coffee bag?",color=DarkRoast)},
  text={Text("This will permanently delete this bag and all its shot history. This cannot be undone.",color=DarkRoast)},
  confirmButton={TextButton(onClick={confirmDeleteBag=false;shots.forEach{deleteManagedPhoto(context,it.photoPath)};delete()}){Text("Delete",color=Color(0xFF9B2C20))}},
  dismissButton={TextButton(onClick={confirmDeleteBag=false}){Text("Cancel")}},
  containerColor=Color(0xFFFFFCF6),titleContentColor=DarkRoast,textContentColor=DarkRoast
 )
}
private data class ShotSentiment(val emoji:String,val percent:Int,val liked:Int,val rated:Int)

private fun shotSentiment(shots:List<Shot>):ShotSentiment? {
    val rated=shots.filter{it.liked!=null}
    if(rated.isEmpty()) return null
    val likes=rated.count{it.liked==true}
    val percent=(likes*100.0/rated.size).toInt()
    val emoji=when {
        percent>=80 -> "😁"
        percent>=60 -> "🙂"
        percent>=40 -> "😐"
        percent>=20 -> "🙁"
        else -> "😢"
    }
    return ShotSentiment(emoji,percent,likes,rated.size)
}

private fun shotSummary(shot:Shot):String =
    "${money(shot.groundCoffeeG)} g in → ${money(shot.yieldG)} g out · 1:${ratioText(shot.groundCoffeeG,shot.yieldG)} · ${shot.timeSec}s" +
        (shot.grindSetting.takeIf{it.isNotBlank()}?.let{" · Grind $it"} ?: "") +
        (when(shot.liked){true->" · 👍";false->" · 👎";null->""})

private fun shotTasteTips(note: String): List<String> {
    val text = note.lowercase()
    val tips = mutableListOf<String>()
    if (listOf("bitter", "harsh", "over-extract").any { it in text })
        tips += "Bitter or harsh: try grinding a little coarser, shortening extraction, or lowering water temperature. Change one variable at a time."
    if (listOf("sour", "sharp", "under-extract", "acidic").any { it in text })
        tips += "Sour or sharp: try grinding a little finer, extending extraction, or raising water temperature. Naturally bright coffee can still taste pleasantly acidic."
    if (listOf("watery", "weak", "thin").any { it in text })
        tips += "Watery or weak: check dose and yield, and make sure the coffee is extracting evenly. A shorter yield can increase strength."
    if (listOf("dry", "astringent").any { it in text })
        tips += "Dry or astringent: check for uneven extraction and channeling. Improve distribution and puck preparation before changing several variables."
    if (listOf("smooth", "balanced", "sweet", "nice").any { it in text })
        tips += "You described a pleasant cup. Save this recipe as a reference and change only one thing when experimenting."
    tips += "Tamping: distribute the grounds evenly, keep the tamper level, and press straight down with consistent pressure. A level puck matters more than pressing as hard as possible."
    tips += "Puck preparation: break up clumps and distribute grounds evenly to reduce channeling. Keep the basket and portafilter clean."
    tips += "These are starting points, not universal rules. Coffee, roast, grinder, water and personal taste all affect the result."
    return tips
}

@Composable
private fun BagHistoryDialog(bag:Bag,shots:List<Shot>,dismiss:()->Unit,edit:()->Unit,scope:kotlinx.coroutines.CoroutineScope,dao:BeanDao,focusBrewId:Long?=null) {
 val context = LocalContext.current
 AlertDialog(
  onDismissRequest=dismiss,
  title={Text(if (bag.roastDate.isBlank()) "Brew history" else "Brew history · ${isoToDisplay(bag.roastDate)}", color=DarkRoast)},
  text={
   Column(Modifier.fillMaxWidth().imePadding().heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
    if (bag.recommendedGrindSetting.isNotBlank()) Text("Recommended grind: ${bag.recommendedGrindSetting}",style=MaterialTheme.typography.bodyLarge)
    shotSentiment(shots)?.let { Text("Shot average: ${it.emoji} · ${it.percent}% liked (${it.liked}/${it.rated} shots)",style=MaterialTheme.typography.bodyLarge) }
    HorizontalDivider(color=DarkRoast.copy(alpha=0.25f))
    if(shots.isEmpty()) Text("No shots recorded yet")
    else shots.sortedByDescending { it.id }.forEachIndexed{index,shot->
     Card(
      modifier=Modifier.fillMaxWidth(),
      colors=CardDefaults.cardColors(containerColor=CoffeeCard,contentColor=CoffeeCardText),
      border=BorderStroke(if (shot.id == focusBrewId) 3.dp else 1.dp, if (shot.id == focusBrewId) Color(0xFFB98961) else DarkRoast.copy(alpha=0.35f)),
      elevation=CardDefaults.cardElevation(defaultElevation=4.dp)
     ){
      Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
       Row(verticalAlignment=Alignment.CenterVertically){
        Text("BREW ${(shots.size-index).toString().padStart(2,'0')}",style=MaterialTheme.typography.labelMedium,color=Color(0xFFE7C9A6))
        Spacer(Modifier.weight(1f))
        Text(shotDateTime(shot.pulledAtMillis),style=MaterialTheme.typography.labelSmall,color=CoffeeCardText.copy(alpha=0.82f))
       }
       Text(shotSummary(shot),style=MaterialTheme.typography.bodyLarge,color=CoffeeCardText)
       shot.photoPath?.let { AsyncImage(model=File(it),contentDescription="Brew result photo",modifier=Modifier.fillMaxWidth().heightIn(max=220.dp).clip(MaterialTheme.shapes.medium),contentScale=ContentScale.Crop) }
       var showTips by remember(shot.id) { mutableStateOf(false) }
       var editingFeedback by remember { mutableStateOf<Shot?>(null) }
       var feedbackText by remember(shot.id) { mutableStateOf(shot.note) }
       Row(
        modifier=Modifier.fillMaxWidth(),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(6.dp)
       ) {
        Text(shot.note,modifier=Modifier.weight(1f).clickable { if(shot.note.isNotBlank()){editingFeedback=shot; feedbackText=shot.note} },style=MaterialTheme.typography.bodyMedium,color=CoffeeCardText)
        Surface(
         modifier=Modifier.clickable { showTips=!showTips },
         color=Color(0xFFEADCCB).copy(alpha=0.58f),
         contentColor=DarkRoast.copy(alpha=0.86f),
         shape=MaterialTheme.shapes.small
        ) {
         Text(if(showTips) "Hide tips" else "☼ Tips",
          modifier=Modifier.padding(horizontal=10.dp,vertical=7.dp),
          style=MaterialTheme.typography.labelMedium)
        }
       }
       if(shot.note.isBlank()) TextButton(onClick={editingFeedback=shot;feedbackText=""}) { Text("+ Add feedback",color=CoffeeCardText) }
       editingFeedback?.let { target ->
        AlertDialog(
         onDismissRequest={editingFeedback=null},
         title={Text("Edit feedback",color=DarkRoast)},
         text={OutlinedTextField(feedbackText,{feedbackText=it},modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=4,label={Text("Feedback")},colors=beanTextFieldColors())},
         confirmButton={TextButton(onClick={scope.launch{dao.saveShot(target.copy(note=feedbackText.trim()));editingFeedback=null}}){Text("Save")}},
         dismissButton={TextButton(onClick={editingFeedback=null}){Text("Cancel")}},
         containerColor=Color(0xFFFFFCF6),titleContentColor=DarkRoast,textContentColor=DarkRoast
        )
       }
       if(showTips) Surface(color=Color(0xFFEADCCB).copy(alpha=0.72f),shape=MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
         shotTasteTips(shot.note).forEach { Text("• $it",style=MaterialTheme.typography.bodySmall,color=DarkRoast) }
        }
       }
       Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
        TextButton(onClick={scope.launch{dao.saveShot(shot.copy(liked=true));cancelShotNotification(context,shot.id)}}){Text("👍 Like",color=CoffeeCardText)}
        TextButton(onClick={scope.launch{dao.saveShot(shot.copy(liked=false));cancelShotNotification(context,shot.id)}}){Text("👎 Dislike",color=CoffeeCardText)}
        TextButton(onClick={scope.launch{dao.deleteShot(shot);deleteManagedPhoto(context,shot.photoPath)}}){BeanActionIcon(BeanIconAction.DELETE,"Delete shot",Modifier.size(20.dp),Color.White)}
       }
      }
     }
    }
   }
  },
  confirmButton={TextButton(onClick=dismiss){Text("Close")}},
  dismissButton={TextButton(onClick={dismiss();edit()}){Text("Edit bag")}},
  shape=MaterialTheme.shapes.extraLarge,
  containerColor=Color(0xFFFFFCF6),titleContentColor=DarkRoast,textContentColor=DarkRoast
 )
}
private fun shotDateTime(timestamp:Long):String =
 if(timestamp<=0L) "Date not recorded"
 else SimpleDateFormat("d MMM yyyy · HH:mm",Locale.getDefault()).format(Date(timestamp))

private fun ratioText(i:Double,o:Double)=if(i>0)String.format(java.util.Locale.US,"%.2f",o/i).trimEnd('0').trimEnd('.') else "–"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BagDialog(initial:Bag?,dismiss:()->Unit,save:(Bag)->Unit) {
 val focusManager = LocalFocusManager.current
 var date by remember(initial?.id){mutableStateOf(initial?.roastDate?.takeIf{it.isNotBlank()}?:todayIso())}
 var unknownDate by remember(initial?.id){mutableStateOf(initial != null && initial.roastDate.isBlank())}
 var picker by remember{mutableStateOf(false)}
 var weight by remember(initial?.id){mutableStateOf(initial?.weightG?.toString()?:"250")}
 var price by remember(initial?.id){mutableStateOf(initial?.price?.takeIf{it>0.0}?.toString()?:"")}
 var currency by remember(initial?.id){mutableStateOf(initial?.currency?:"EUR")}
 var currencyMenuOpen by remember(initial?.id){mutableStateOf(false)}
 var finished by remember(initial?.id){mutableStateOf(initial?.isFinished?:false)}
 var recommendedGrind by remember(initial?.id){mutableStateOf(initial?.recommendedGrindSetting?:"")}
 AlertDialog(onDismissRequest=dismiss,title={Text(if(initial==null)"New bag" else "Edit bag",color=DarkRoast)},
 text={Column(Modifier.imePadding().heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
  FieldLabel("Roast date", "Knowing the roast date helps you understand freshness and how the coffee changes as it rests and ages. The ideal resting time depends on the coffee, roast level and brewing method.")
  Row(verticalAlignment=Alignment.CenterVertically) {
   OutlinedButton(onClick={picker=true},modifier=Modifier.weight(1f),enabled=!unknownDate){Text(if(unknownDate) "Roast date unknown" else "Roast date: ${isoToDisplay(date)}")}
   Row(verticalAlignment=Alignment.CenterVertically) {
    Checkbox(checked=unknownDate,onCheckedChange={unknownDate=it})
    Text("Unknown")
   }
  }
  OutlinedTextField(weight,{weight=it},label={Text("Weight (g)")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())
  Row(
   modifier=Modifier.fillMaxWidth(),
   horizontalArrangement=Arrangement.spacedBy(8.dp),
   verticalAlignment=Alignment.CenterVertically
  ) {
   OutlinedTextField(
    value=price,
    onValueChange={price=it},
    label={Text("Price (optional)")},
    modifier=Modifier.weight(1f),
    singleLine=true,
    colors=beanTextFieldColors(),
    keyboardOptions=beanNextKeyboardOptions,
    keyboardActions=beanNextKeyboardActions(focusManager)
   )
   Box {
    OutlinedButton(
     onClick={currencyMenuOpen=true},
     modifier=Modifier.height(56.dp),
     contentPadding=PaddingValues(horizontal=14.dp,vertical=0.dp)
    ) { Text("${currencySymbol(currency)} $currency",style=MaterialTheme.typography.bodyMedium) }
    DropdownMenu(expanded=currencyMenuOpen,onDismissRequest={currencyMenuOpen=false}) {
     CURRENCIES.forEach { c ->
      DropdownMenuItem(text={Text("${currencySymbol(c)} $c")},onClick={currency=c;currencyMenuOpen=false})
     }
    }
   }
  }
  var showRecommendedGrind by remember(initial?.id) {
   mutableStateOf(!initial?.recommendedGrindSetting.isNullOrBlank())
  }
  if (showRecommendedGrind) {
   OutlinedTextField(
    recommendedGrind,
    {recommendedGrind=it},
    label={Text("Recommended grind setting")},
    modifier=Modifier.fillMaxWidth(),
    colors=beanTextFieldColors(),
    trailingIcon={
     IconButton(onClick={showRecommendedGrind=false;recommendedGrind=""}) {
      Icon(Icons.Default.Close,contentDescription="Remove grind setting")
     }
    }
   )
  } else {
   TextButton(
    onClick={showRecommendedGrind=true},
    contentPadding=PaddingValues(horizontal=4.dp,vertical=2.dp)
   ) {
    Icon(Icons.Default.Add,contentDescription=null)
    Spacer(Modifier.width(6.dp))
    Text("Do you want to add a grind setting?")
   }
  }
  if(initial!=null) Row(verticalAlignment=Alignment.CenterVertically){Text("Finished bag",modifier=Modifier.weight(1f));Switch(finished,{finished=it})}
 }},
 confirmButton={TextButton(enabled=(weight.toIntOrNull()?:0)>0,onClick={
  val savedDate = if (unknownDate) "" else date
  val savedPrice = price.num() ?: 0.0
  val bag=initial?.copy(roastDate=savedDate,weightG=weight.toInt(),price=savedPrice,currency=currency,isFinished=finished,recommendedGrindSetting=recommendedGrind)
   ?:Bag(beanId=0,roastDate=savedDate,weightG=weight.toInt(),price=savedPrice,currency=currency,isFinished=finished,recommendedGrindSetting=recommendedGrind)
  save(bag)
 }){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){BeanActionIcon(BeanIconAction.SAVE,"Save",Modifier.size(18.dp));Text("Save")}}},
 dismissButton={TextButton(onClick=dismiss){Text("Cancel")}},
 shape=MaterialTheme.shapes.extraLarge,
 containerColor=Color(0xFFFFFCF6),titleContentColor=DarkRoast,textContentColor=DarkRoast)
 if(picker && !unknownDate){val state=rememberDatePickerState(initialSelectedDateMillis=isoToMillis(date))
  DatePickerDialog(onDismissRequest={picker=false},confirmButton={TextButton(onClick={state.selectedDateMillis?.let{date=millisToIso(it)};picker=false}){Text("OK")}},
  dismissButton={TextButton(onClick={picker=false}){Text("Cancel")}}){DatePicker(state)}
 }
}
@Composable
private fun ShotDialog(initial:Shot?,dismiss:()->Unit,save:(Shot)->Unit) {
 val context = LocalContext.current
 var input by remember(initial){mutableStateOf(initial?.groundCoffeeG?.let{money(it)} ?: "18")}
 var output by remember(initial){mutableStateOf(initial?.yieldG?.let{money(it)} ?: "36")}
 var seconds by remember(initial){mutableStateOf((initial?.timeSec ?: 28).toString())}
 var grind by remember(initial){mutableStateOf(initial?.grindSetting ?: "")}
 var note by remember{mutableStateOf("")}
 var showFeedback by remember{mutableStateOf(false)}
 var photoPath by remember(initial){mutableStateOf(initial?.photoPath)}
 val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
  if (uri != null) {
   val newPath = compressPickedImage(context, uri)
   if (newPath != null) {
    if (photoPath != initial?.photoPath) deleteManagedPhoto(context, photoPath)
    photoPath = newPath
   }
  }
 }
 val canSave=(input.num()?:0.0)>0&&(output.num()?:0.0)>0&&(seconds.toIntOrNull()?:0)>0
 fun closeDialog() {
  if (photoPath != initial?.photoPath) deleteManagedPhoto(context, photoPath)
  dismiss()
 }
 AlertDialog(
  onDismissRequest={closeDialog()},
  title={Text("Add brew",color=DarkRoast)},
  text={Column(Modifier.imePadding().heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
   Text("BREW DETAILS",style=MaterialTheme.typography.labelMedium,color=DarkRoast)
   OutlinedTextField(input,{input=it},label={Text("Ground coffee (g)")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())
   OutlinedTextField(output,{output=it},label={Text("Yield (g)")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())
   OutlinedTextField(seconds,{seconds=it},label={Text("Time (seconds)")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())
   OutlinedTextField(grind,{grind=it},label={Text("Grind setting used")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    IconButton(onClick={photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}) {
     Icon(Icons.Default.AddAPhoto,contentDescription="Add brew photo",tint=DarkRoast)
    }
    Text("Add photo",style=MaterialTheme.typography.bodyMedium,color=DarkRoast)
    photoPath?.let {
     AsyncImage(model=File(it),contentDescription="Brew result preview",modifier=Modifier.size(54.dp).clip(MaterialTheme.shapes.small),contentScale=ContentScale.Crop)
     IconButton(onClick={deleteManagedPhoto(context,photoPath);photoPath=null}) {
      Icon(Icons.Default.Close,contentDescription="Remove brew photo",tint=DarkRoast)
     }
    }
   }
   if(showFeedback) {
    OutlinedTextField(note,{note=it},label={Text("Feedback (optional)")},modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=4,colors=beanTextFieldColors())
   } else {
    TextButton(onClick={showFeedback=true}) { Text("+ Add feedback") }
   }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End,verticalAlignment=Alignment.CenterVertically) {
    TextButton(onClick={closeDialog}) { Text("Cancel") }
    TextButton(enabled=canSave,onClick={
     save(Shot(bagId=0,groundCoffeeG=input.num()!!,yieldG=output.num()!!,timeSec=seconds.toInt(),grindSetting=grind.trim(),note=note.trim(),photoPath=photoPath))
    }) { Text("Save") }
   }
  }},
  confirmButton={},
  dismissButton={},
  shape=MaterialTheme.shapes.extraLarge,
  containerColor=Color(0xFFFFFCF6),titleContentColor=DarkRoast,textContentColor=DarkRoast
 )
}
