package com.example.beanlog

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Outlined
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
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.beanlog.ui.theme.LatteVariant
import com.example.beanlog.ui.theme.CoffeeCard
import com.example.beanlog.ui.theme.CoffeeCardText
import com.example.beanlog.ui.theme.CoffeeCream
import com.example.beanlog.ui.theme.DarkRoast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ROAST_LEVELS = listOf("Light", "Medium-Light", "Medium", "Medium-Dark", "Dark")

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

/** Copy the picked image into app storage so it stays available. */
private fun copyToInternal(ctx: Context, uri: Uri): String {
    val f = File(ctx.filesDir, "bean_${System.currentTimeMillis()}.jpg")
    ctx.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
    return f.absolutePath
}

@Composable
fun App(dao: BeanDao) {
    val nav = rememberNavController()
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(colors = listOf(Color(0xFFF1E5D7), Color(0xFFFFFCF6)))
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
                BeanList(dao, onOpen = { nav.navigate("detail/$it") }, onAdd = { nav.navigate("edit/0") })
            }
            composable("detail/{id}") {
                val id = it.arguments!!.getString("id")!!.toLong()
                DetailScreen(dao, id, onEdit = { nav.navigate("edit/$id") }, onBack = { nav.popBackStack() })
            }
            composable("edit/{id}") {
                val id = it.arguments!!.getString("id")!!.toLong()
                EditScreen(dao, id, onDone = { nav.popBackStack() })
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
fun BeanList(dao: BeanDao, onOpen: (Long) -> Unit, onAdd: () -> Unit) {
    val beans by dao.beansWithLatestBag().collectAsState(emptyList())
    var sortOption by remember { mutableStateOf(BeanSortOption.ROAST_DATE) }
    // Recently added defaults to newest first; Name defaults to A–Z.
    var ascending by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var settingsOpen by remember { mutableStateOf(false) }
    var remindersOn by remember { mutableStateOf(shotRemindersEnabled(context)) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

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
            .sortedBy { if (it.openBagCount > 0) 0 else 1 }
            .map { it.bean to it.openBagCount }
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
                    IconButton(onClick = { settingsOpen = true }) {
                        Icon(Icons.Default.Settings, "Settings", tint = DarkRoast)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Default.Add, "Add bean") }
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
            items(sortedBeans, key = { it.first.id }) { (b, count) ->
                Card(onClick = { onOpen(b.id) }, modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor=CoffeeCard, contentColor=CoffeeCardText),
                    border = BorderStroke(1.dp, DarkRoast.copy(alpha = 0.28f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)) {
                    Row(Modifier.padding(if (count == 0) 8.dp else 12.dp).alpha(if(count==0) .48f else 1f), verticalAlignment=Alignment.CenterVertically) {
                        val imageSize = if (count == 0) 44.dp else 64.dp
                        if(b.photoPath!=null) AsyncImage(File(b.photoPath),null,Modifier.size(imageSize),contentScale=ContentScale.Crop)
                        else Icon(Icons.Default.Coffee,null,Modifier.size(imageSize))
                        Spacer(Modifier.width(if (count == 0) 8.dp else 12.dp))
                        Column {
                            Text(b.name,style=if (count == 0) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium)
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
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Text("Shot feedback reminder (30 min)",modifier=Modifier.weight(1f))
                Switch(checked=remindersOn,onCheckedChange={enabled->
                    remindersOn=enabled
                    setShotRemindersEnabled(context,enabled)
                    if(enabled && Build.VERSION.SDK_INT>=33 &&
                        ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                })
            }
            Text("When enabled, BeanLog asks whether you liked each shot 30 minutes after it is saved. Notifications must also be allowed by Android.",style=MaterialTheme.typography.bodySmall)
        }},
        confirmButton={TextButton(onClick={settingsOpen=false}){Text("Done")}}
    )
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
    focusedContainerColor = Color(0xFFF1E3D0).copy(alpha = 0.90f),
    unfocusedContainerColor = Color(0xFFF1E3D0).copy(alpha = 0.82f)
)

// ---------- Add / edit bean ----------
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(dao: BeanDao, id: Long, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var roastery by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var roast by remember { mutableStateOf("Medium") }
    var score by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<String?>(null) }
    var cIn by remember { mutableStateOf("") }
    var cOut by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var origin by remember { mutableStateOf("") }; var farm by remember { mutableStateOf("") }
    var variety by remember { mutableStateOf("") }; var processing by remember { mutableStateOf("") }

    LaunchedEffect(id) {
        if (id != 0L) dao.bean(id).first()?.let {
            roastery = it.roastery; country = it.roasteryCountry; name = it.name
            notes = it.flavourNotes; roast = it.roastLevel
            score = it.cuppingScore?.toString() ?: ""; photo = it.photoPath
            cIn = it.coffeeInG?.toString() ?: ""; cOut = it.coffeeOutG?.toString() ?: ""
            time = it.brewTimeSec?.toString() ?: ""
            origin=it.origin; farm=it.farm; variety=it.variety; processing=it.processing
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photo = copyToInternal(ctx, uri)
    }

    Scaffold(containerColor = Color.Transparent, topBar = {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            title = { Text(if (id == 0L) "New bean" else "Edit bean") }, actions = {
            TextButton(onClick = onDone) { Text("Cancel") }
            TextButton(enabled = name.isNotBlank() && roastery.isNotBlank(), onClick = {
                scope.launch {
                    dao.save(Bean(id, roastery.trim(), country.trim(), name.trim(), notes.trim(), roast,
                        score.num(), photo, cIn.num(), cOut.num(), time.toIntOrNull(),
                        origin=origin.trim(), farm=farm.trim(), variety=variety.trim(), processing=processing.trim()))
                    onDone()
                }
            }) { Text("Save") }
        })
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (photo != null) AsyncImage(File(photo!!), null,
                Modifier.fillMaxWidth().height(180.dp), contentScale = ContentScale.Crop)
            OutlinedButton(onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Icon(Icons.Default.AddAPhoto, null); Spacer(Modifier.width(8.dp)); Text("Add picture") }

            OutlinedTextField(roastery, { roastery = it }, label = { Text("Roastery") }, modifier = Modifier.fillMaxWidth(), colors = beanTextFieldColors())
            OutlinedTextField(country, { country = it }, label = { Text("Country of roaster") }, modifier = Modifier.fillMaxWidth(), colors = beanTextFieldColors())
            OutlinedTextField(name, { name = it }, label = { Text("Coffee name") }, modifier = Modifier.fillMaxWidth(), colors = beanTextFieldColors())
            OutlinedTextField(notes, { notes = it }, label = { Text("Flavour notes") }, modifier = Modifier.fillMaxWidth(), colors = beanTextFieldColors())
            Text("Roast level", style = MaterialTheme.typography.labelLarge)
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
                        onClick = { roast = level },
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
            OutlinedTextField(score, { score = it }, label = { Text("Cupping score") }, modifier = Modifier.fillMaxWidth(), colors = beanTextFieldColors())
            HorizontalDivider(Modifier.padding(vertical=8.dp))
            Text("Coffee details (optional)",style=MaterialTheme.typography.titleMedium)
            OutlinedTextField(origin,{origin=it},label={Text("Origin")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())
            OutlinedTextField(farm,{farm=it},label={Text("Farm")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())
            OutlinedTextField(variety,{variety=it},label={Text("Variety")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())
            OutlinedTextField(processing,{processing=it},label={Text("Processing")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Recommended brew settings", style = MaterialTheme.typography.titleMedium)
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
        OutlinedButton(onClick = { open = true }) { Text(selected ?: "As paid") }
        DropdownMenu(open, { open = false }) {
            if (allowNone) DropdownMenuItem(text = { Text("As paid") }, onClick = { onPick(null); open = false })
            CURRENCIES.forEach { c ->
                DropdownMenuItem(text = { Text(c) }, onClick = { onPick(c); open = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(dao: BeanDao, id: Long, onEdit: () -> Unit, onBack: () -> Unit) {
    val bean by dao.bean(id).collectAsState(null)
    val bags by dao.bags(id).collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    var showBagDialog by remember { mutableStateOf(false) }
    var editingBag by remember { mutableStateOf<Bag?>(null) }
    var showFinishedBags by remember { mutableStateOf(false) }
    var displayCur by remember { mutableStateOf<String?>(null) }   // null = show as paid
    var ratesOk by remember { mutableStateOf(Rates.loaded) }
    LaunchedEffect(Unit) { ratesOk = Rates.load() }
    val b = bean ?: return

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(b.name) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onEdit) { Icon(Icons.Default.Edit, "Edit") }
                    IconButton({ scope.launch { dao.delete(b); onBack() } }) { Icon(Icons.Default.Delete, "Delete") }
                })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showBagDialog = true },
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add bag") })
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                if (b.photoPath != null) AsyncImage(File(b.photoPath), null,
                    Modifier.fillMaxWidth().height(200.dp), contentScale = ContentScale.Crop)
                Text("${b.roastery} · ${countryFlag(b.roasteryCountry)}", style = MaterialTheme.typography.titleMedium)
                Text("Roast: ${b.roastLevel}" + (b.cuppingScore?.let { " · Cup Score: $it" } ?: ""))
                if (b.flavourNotes.isNotBlank()) Text("Notes: ${b.flavourNotes}")
                listOf("Origin" to (b.origin.takeIf{it.isNotBlank()}?.let{ "${countryFlag(it)} $it" } ?: ""), "Farm" to b.farm,"Variety" to b.variety,"Processing" to b.processing).filter{it.second.isNotBlank()}.forEach{(k,v)->Text("$k: $v")}
                Spacer(Modifier.height(8.dp))
                Text("Recommended brew", style = MaterialTheme.typography.titleMedium)
                Text("${b.coffeeInG ?: "–"} g in → ${b.coffeeOutG ?: "–"} g out in ${b.brewTimeSec ?: "–"} s")
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bags", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("Show in: ")
                    CurrencyMenu(displayCur, allowNone = true) { displayCur = it }
                }
                if (displayCur != null && !ratesOk)
                    Text("Exchange rates unavailable (check your internet connection).",
                        style = MaterialTheme.typography.bodySmall)
            }
            items(bags.filter { !it.isFinished }, key={it.id}) { bag ->
                BagCard(dao,bag,displayCur,{editingBag=bag},{scope.launch{dao.deleteBag(bag)}})
            }
            if(bags.any{it.isFinished}) {
                item { TextButton(onClick={showFinishedBags=!showFinishedBags},modifier=Modifier.fillMaxWidth()) {
                    Text("Finished bags (${bags.count{it.isFinished}})")
                    Icon(if(showFinishedBags) Icons.Default.ExpandLess else Icons.Default.ExpandMore,null)
                } }
                if(showFinishedBags) items(bags.filter{it.isFinished},key={it.id}) { bag ->
                    BagCard(dao,bag,displayCur,{editingBag=bag},{scope.launch{dao.deleteBag(bag)}},true)
                }
            }
        }
    }

    if(showBagDialog) BagDialog(null,{showBagDialog=false}) { scope.launch{dao.saveBag(it.copy(beanId=id));showBagDialog=false} }
    editingBag?.let { bag -> BagDialog(bag,{editingBag=null}) { scope.launch{dao.saveBag(it.copy(beanId=id));editingBag=null} } }
}

@Composable
private fun BagCard(dao:BeanDao,bag:Bag,cur:String?,edit:()->Unit,delete:()->Unit,muted:Boolean=false) {
 val shots by dao.shots(bag.id).collectAsState(emptyList())
 val scope=rememberCoroutineScope(); val context=LocalContext.current; var add by remember{mutableStateOf(false)}; var showHistory by remember{mutableStateOf(false)}
 Card(onClick={showHistory=true},modifier=Modifier.fillMaxWidth(),
  colors=CardDefaults.cardColors(containerColor=LatteVariant),
  elevation=CardDefaults.cardElevation(defaultElevation=0.dp)) {
  Column(Modifier.padding(12.dp).alpha(if(muted) 0.55f else 1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
   Row(verticalAlignment=Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
     Text("Roasted ${isoToDisplay(bag.roastDate)}",style=MaterialTheme.typography.titleMedium)
     Text("${bag.weightG} g"+if(bag.lotNumber.isNotBlank())" · Lot ${bag.lotNumber}" else "")
     Text("Paid ${money(bag.price)} ${bag.currency} (${money(bag.price/bag.weightG,3)} ${bag.currency}/g)")
     cur?.let{c->if(c!=bag.currency)Rates.convert(bag.price,bag.currency,c)?.let{Text("≈ ${money(it)} $c")}}
     if(bag.isFinished)Text("Finished",style=MaterialTheme.typography.labelMedium)
     if(bag.recommendedGrindSetting.isNotBlank())Text("Recommended grind: ${bag.recommendedGrindSetting}")
     Text("Bag rating: ${when(bag.liked){true->"Liked";false->"Disliked";null->"Not rated"}}",style=MaterialTheme.typography.bodySmall)
     shotSentiment(shots)?.let { Text("Shot average: ${it.emoji} · ${it.percent}% liked",style=MaterialTheme.typography.bodySmall) }
    }
    IconButton(onClick=edit){Icon(Icons.Default.Edit,"Edit bag")}
    IconButton(onClick=delete){Icon(Icons.Default.Delete,"Delete bag")}
   }
   HorizontalDivider()
   Row(verticalAlignment=Alignment.CenterVertically) {
    Text("Shots (${shots.size})",modifier=Modifier.weight(1f),style=MaterialTheme.typography.titleSmall)
    TextButton(onClick={add=true}){Icon(Icons.Default.Add,null);Text("Add shot")}
   }
   shotSentiment(shots)?.let { Text("Shot average: ${it.emoji} · ${it.percent}% liked (${it.liked}/${it.rated} shots)",style=MaterialTheme.typography.bodySmall) }
   shots.take(2).forEach{shot->Text(shotSummary(shot),style=MaterialTheme.typography.bodySmall)}
   if(shots.size>2) Text("Tap bag to see all ${shots.size} shots",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
  }
 }
 if(add)ShotDialog({add=false}){shot->scope.launch{val id=dao.insertShot(shot.copy(bagId=bag.id)); if(shotRemindersEnabled(context)) scheduleShotReminder(context,id); add=false}}
 if(showHistory) BagHistoryDialog(bag,shots,{showHistory=false},edit,scope,dao)
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

@Composable
private fun BagHistoryDialog(bag:Bag,shots:List<Shot>,dismiss:()->Unit,edit:()->Unit,scope:kotlinx.coroutines.CoroutineScope,dao:BeanDao) {
 AlertDialog(
  onDismissRequest=dismiss,
  title={Text("Shot history · ${isoToDisplay(bag.roastDate)}", color=DarkRoast)},
  text={
   Column(Modifier.fillMaxWidth().heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
    Text("Recommended grind: ${bag.recommendedGrindSetting.ifBlank{"Not set"}}")
    Text("Bag rating: ${when(bag.liked){true->"Liked";false->"Disliked";null->"Not rated"}}")
    shotSentiment(shots)?.let { Text("Shot average: ${it.emoji} · ${it.percent}% liked (${it.liked}/${it.rated} shots)") }
    HorizontalDivider(color=DarkRoast.copy(alpha=0.25f))
    if(shots.isEmpty()) Text("No shots recorded yet")
    else shots.sortedBy{it.id}.forEachIndexed{index,shot->
     Card(
      modifier=Modifier.fillMaxWidth(),
      colors=CardDefaults.cardColors(containerColor=CoffeeCard,contentColor=CoffeeCardText),
      border=BorderStroke(1.dp,DarkRoast.copy(alpha=0.35f)),
      elevation=CardDefaults.cardElevation(defaultElevation=4.dp)
     ){
      Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
       Row(verticalAlignment=Alignment.CenterVertically){
        Text("SHOT ${(index+1).toString().padStart(2,'0')}",style=MaterialTheme.typography.labelMedium,color=Color(0xFFE7C9A6))
        Spacer(Modifier.weight(1f))
        Text(shotDateTime(shot.pulledAtMillis),style=MaterialTheme.typography.labelSmall,color=CoffeeCardText.copy(alpha=0.82f))
       }
       Text(shotSummary(shot),style=MaterialTheme.typography.bodyLarge,color=CoffeeCardText)
       Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
        TextButton(onClick={scope.launch{dao.saveShot(shot.copy(liked=true))}}){Text("👍 Like",color=CoffeeCardText)}
        TextButton(onClick={scope.launch{dao.saveShot(shot.copy(liked=false))}}){Text("👎 Dislike",color=CoffeeCardText)}
        TextButton(onClick={scope.launch{dao.deleteShot(shot)}}){Text("Delete",color=CoffeeCardText)}
       }
      }
     }
    }
   }
  },
  confirmButton={TextButton(onClick=dismiss){Text("Close")}},
  dismissButton={TextButton(onClick={dismiss();edit()}){Text("Edit bag")}},
  shape=MaterialTheme.shapes.extraLarge,
  colors=AlertDialogDefaults.colors(containerColor=Color(0xFFFFFCF6),titleContentColor=DarkRoast,textContentColor=DarkRoast)
 )
}
private fun shotDateTime(timestamp:Long):String =
 if(timestamp<=0L) "Date not recorded"
 else SimpleDateFormat("d MMM yyyy · HH:mm",Locale.getDefault()).format(Date(timestamp))

private fun ratioText(i:Double,o:Double)=if(i>0)String.format(java.util.Locale.US,"%.2f",o/i).trimEnd('0').trimEnd('.') else "–"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BagDialog(initial:Bag?,dismiss:()->Unit,save:(Bag)->Unit) {
 var date by remember(initial?.id){mutableStateOf(initial?.roastDate?:todayIso())}
 var picker by remember{mutableStateOf(false)}
 var weight by remember(initial?.id){mutableStateOf(initial?.weightG?.toString()?:"250")}
 var price by remember(initial?.id){mutableStateOf(initial?.price?.toString()?:"")}
 var currency by remember(initial?.id){mutableStateOf(initial?.currency?:"EUR")}
 var lot by remember(initial?.id){mutableStateOf(initial?.lotNumber?:"")}
 var finished by remember(initial?.id){mutableStateOf(initial?.isFinished?:false)}
 var recommendedGrind by remember(initial?.id){mutableStateOf(initial?.recommendedGrindSetting?:"")}
 var liked by remember(initial?.id){mutableStateOf(initial?.liked)}
 AlertDialog(onDismissRequest=dismiss,title={Text(if(initial==null)"New bag" else "Edit bag",color=DarkRoast)},
 text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
  OutlinedButton(onClick={picker=true},modifier=Modifier.fillMaxWidth()){Text("Roast date: ${isoToDisplay(date)}")}
  OutlinedTextField(weight,{weight=it},label={Text("Weight (g)")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())
  Row{OutlinedTextField(price,{price=it},label={Text("Price")},modifier=Modifier.weight(1f), colors = beanTextFieldColors());CurrencyMenu(currency,false){currency=it?:currency}}
  OutlinedTextField(lot,{lot=it},label={Text("Lot number")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())
  OutlinedTextField(recommendedGrind,{recommendedGrind=it},label={Text("Recommended grind setting")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())
  Text("Did you like this coffee?",style=MaterialTheme.typography.labelLarge)
  Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
   FilterChip(selected=liked==null,onClick={liked=null},label={Text("Not rated")})
   FilterChip(selected=liked==true,onClick={liked=true},label={Text("Liked")})
   FilterChip(selected=liked==false,onClick={liked=false},label={Text("Disliked")})
  }
  Row(verticalAlignment=Alignment.CenterVertically){Text("Finished bag",modifier=Modifier.weight(1f));Switch(finished,{finished=it})}
 }},
 confirmButton={TextButton(enabled=(weight.toIntOrNull()?:0)>0&&price.num()!=null,onClick={
  val bag=initial?.copy(roastDate=date,weightG=weight.toInt(),price=price.num()!!,currency=currency,lotNumber=lot,isFinished=finished,recommendedGrindSetting=recommendedGrind,liked=liked)
   ?:Bag(beanId=0,roastDate=date,weightG=weight.toInt(),price=price.num()!!,currency=currency,lotNumber=lot,isFinished=finished)
  save(bag)
 }){Text("Save")}},
 dismissButton={TextButton(onClick=dismiss){Text("Cancel")}},
 shape=MaterialTheme.shapes.extraLarge,
 colors=AlertDialogDefaults.colors(containerColor=Color(0xFFFFFCF6),titleContentColor=DarkRoast,textContentColor=DarkRoast))
 if(picker){val state=rememberDatePickerState(initialSelectedDateMillis=isoToMillis(date))
  DatePickerDialog(onDismissRequest={picker=false},confirmButton={TextButton(onClick={state.selectedDateMillis?.let{date=millisToIso(it)};picker=false}){Text("OK")}},
  dismissButton={TextButton(onClick={picker=false}){Text("Cancel")}}){DatePicker(state)}
 }
}
@Composable
private fun ShotDialog(dismiss:()->Unit,save:(Shot)->Unit) {
 var input by remember{mutableStateOf("18")}
 var output by remember{mutableStateOf("36")}
 var seconds by remember{mutableStateOf("28")}
 var grind by remember{mutableStateOf("")}
 val ratio=input.num()?.takeIf{it>0}?.let{output.num()?.div(it)}
 AlertDialog(
  onDismissRequest=dismiss,
  title={Text("Add shot",color=DarkRoast)},
  text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
   Text("SHOT DETAILS",style=MaterialTheme.typography.labelMedium,color=DarkRoast)
   OutlinedTextField(input,{input=it},label={Text("Ground coffee (g)")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())
   OutlinedTextField(output,{output=it},label={Text("Yield (g)")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())
   Card(colors=CardDefaults.cardColors(containerColor=LatteVariant),border=BorderStroke(1.dp,DarkRoast.copy(alpha=0.20f))){
    Text(ratio?.let{"Brew ratio   1:${String.format(java.util.Locale.US,"%.2f",it).trimEnd('0').trimEnd('.')}"}?:"Brew ratio will appear here",
     modifier=Modifier.padding(12.dp),style=MaterialTheme.typography.titleMedium,color=DarkRoast)
   }
   OutlinedTextField(seconds,{seconds=it},label={Text("Time (seconds)")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())
   OutlinedTextField(grind,{grind=it},label={Text("Grind setting used")},modifier=Modifier.fillMaxWidth(), colors = beanTextFieldColors())
  }},
  confirmButton={TextButton(enabled=(input.num()?:0.0)>0&&(output.num()?:0.0)>0&&(seconds.toIntOrNull()?:0)>0,onClick={save(Shot(bagId=0,groundCoffeeG=input.num()!!,yieldG=output.num()!!,timeSec=seconds.toInt(),grindSetting=grind.trim()))}){Text("Save")}},
  dismissButton={TextButton(onClick=dismiss){Text("Cancel")}},
  shape=MaterialTheme.shapes.extraLarge,
  colors=AlertDialogDefaults.colors(containerColor=Color(0xFFFFFCF6),titleContentColor=DarkRoast,textContentColor=DarkRoast)
 )
}
