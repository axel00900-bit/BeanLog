package com.example.beanlog

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

private val ROAST_LEVELS = listOf("Light", "Medium-Light", "Medium", "Medium-Dark", "Dark")

private fun String.num() = replace(',', '.').toDoubleOrNull()

/** Copy the picked image into app storage so it stays available. */
private fun copyToInternal(ctx: Context, uri: Uri): String {
    val f = File(ctx.filesDir, "bean_${System.currentTimeMillis()}.jpg")
    ctx.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
    return f.absolutePath
}

@Composable
fun App(dao: BeanDao) {
    val nav = rememberNavController()
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

// ---------- List ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeanList(dao: BeanDao, onOpen: (Long) -> Unit, onAdd: () -> Unit) {
    val beans by dao.beans().collectAsState(emptyList())
    Scaffold(
        topBar = { TopAppBar(title = { Text("My beans") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, "Add bean") }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(beans) { b ->
                Card(onClick = { onOpen(b.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (b.photoPath != null)
                            AsyncImage(File(b.photoPath), null, Modifier.size(64.dp), contentScale = ContentScale.Crop)
                        else Icon(Icons.Default.Coffee, null, Modifier.size(64.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(b.name, style = MaterialTheme.typography.titleMedium)
                            Text("${b.roastery} · ${b.roasteryCountry}")
                            Text(b.roastLevel, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

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

    LaunchedEffect(id) {
        if (id != 0L) dao.bean(id).first()?.let {
            roastery = it.roastery; country = it.roasteryCountry; name = it.name
            notes = it.flavourNotes; roast = it.roastLevel
            score = it.cuppingScore?.toString() ?: ""; photo = it.photoPath
            cIn = it.coffeeInG?.toString() ?: ""; cOut = it.coffeeOutG?.toString() ?: ""
            time = it.brewTimeSec?.toString() ?: ""
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photo = copyToInternal(ctx, uri)
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(if (id == 0L) "New bean" else "Edit bean") }, actions = {
            TextButton(onClick = onDone) { Text("Cancel") }
            TextButton(enabled = name.isNotBlank() && roastery.isNotBlank(), onClick = {
                scope.launch {
                    dao.save(Bean(id, roastery.trim(), country.trim(), name.trim(), notes.trim(), roast,
                        score.num(), photo, cIn.num(), cOut.num(), time.toIntOrNull()))
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

            OutlinedTextField(roastery, { roastery = it }, label = { Text("Roastery") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(country, { country = it }, label = { Text("Country of roaster") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(name, { name = it }, label = { Text("Coffee name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(notes, { notes = it }, label = { Text("Flavour notes") }, modifier = Modifier.fillMaxWidth())
            Text("Roast level", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ROAST_LEVELS.forEach { FilterChip(roast == it, { roast = it }, label = { Text(it) }) }
            }
            OutlinedTextField(score, { score = it }, label = { Text("Cupping score") }, modifier = Modifier.fillMaxWidth())

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Recommended brew settings", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(cIn, { cIn = it }, label = { Text("In (g)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(cOut, { cOut = it }, label = { Text("Out (g)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(time, { time = it }, label = { Text("Time (s)") }, modifier = Modifier.weight(1f))
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
    var displayCur by remember { mutableStateOf<String?>(null) }   // null = show as paid
    var ratesOk by remember { mutableStateOf(Rates.loaded) }
    LaunchedEffect(Unit) { ratesOk = Rates.load() }
    val b = bean ?: return

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(b.name) },
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
                Text("${b.roastery} (${b.roasteryCountry})", style = MaterialTheme.typography.titleMedium)
                Text("Roast: ${b.roastLevel}" + (b.cuppingScore?.let { " · Cup Score: $it" } ?: ""))
                if (b.flavourNotes.isNotBlank()) Text("Notes: ${b.flavourNotes}")
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
            items(bags) { bag ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Roasted ${isoToDisplay(bag.roastDate)}")
                            Text("${bag.weightG} g" +
                                    if (bag.lotNumber.isNotBlank()) " · Lot ${bag.lotNumber}" else "")
                            Text("Paid ${money(bag.price)} ${bag.currency} " +
                                    "(${money(bag.price / bag.weightG, 3)} ${bag.currency}/g)")
                            displayCur?.let { cur ->
                                if (cur != bag.currency) Rates.convert(bag.price, bag.currency, cur)?.let { v ->
                                    Text("≈ ${money(v)} $cur (${money(v / bag.weightG, 3)} $cur/g)",
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        IconButton({ scope.launch { dao.deleteBag(bag) } }) { Icon(Icons.Default.Delete, "Delete bag") }
                    }
                }
            }
        }
    }

    if (showBagDialog) AddBagDialog(
        onDismiss = { showBagDialog = false },
        onSave = { scope.launch { dao.saveBag(it.copy(beanId = id)); showBagDialog = false } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBagDialog(onDismiss: () -> Unit, onSave: (Bag) -> Unit) {
    var date by remember { mutableStateOf(todayIso()) }
    var showPicker by remember { mutableStateOf(false) }
    var weight by remember { mutableStateOf("250") }
    var price by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("EUR") }
    var lot by remember { mutableStateOf("") }

    val perGram = price.num()?.let { p -> weight.toIntOrNull()?.takeIf { it > 0 }?.let { p / it } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New bag") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CalendarMonth, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Roast date: ${isoToDisplay(date)}")
                }
                OutlinedTextField(weight, { weight = it }, label = { Text("Weight (g)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(price, { price = it }, label = { Text("Price") }, modifier = Modifier.weight(1f))
                    CurrencyMenu(currency, allowNone = false) { currency = it ?: currency }
                }
                Text(perGram?.let { "= ${money(it, 3)} $currency per gram" } ?: "Price per gram shows here",
                    style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(lot, { lot = it }, label = { Text("Lot number") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = weight.toIntOrNull() != null && price.num() != null, onClick = {
                onSave(Bag(beanId = 0, roastDate = date, weightG = weight.toInt(),
                    price = price.num()!!, currency = currency, lotNumber = lot))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = isoToMillis(date))
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = millisToIso(it) }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state) }
    }
}
