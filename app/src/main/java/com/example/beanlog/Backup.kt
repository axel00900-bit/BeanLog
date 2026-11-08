package com.example.beanlog

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupPreview(
    val beans: Int,
    val bags: Int,
    val shots: Int,
    val beansToAdd: Int = beans,
    val bagsToAdd: Int = bags,
    val shotsToAdd: Int = shots
)
data class RestoreResult(val beansAdded: Int, val bagsAdded: Int, val shotsAdded: Int)

data class BackupContents(
    val beans: List<Bean>,
    val bags: List<Bag>,
    val shots: List<Shot>,
    val photos: Map<String, ByteArray>
)

object BeanLogBackup {
    private const val DATA_ENTRY = "beanlog.json"
    private const val BACKUP_VERSION = 1

    suspend fun write(dao: BeanDao, output: OutputStream) {
        val beans = dao.allBeans()
        val bags = dao.allBags()
        val shots = dao.allShots()
        val photos = linkedMapOf<String, ByteArray>()
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("beans", JSONArray().apply {
            beans.forEach { bean ->
                val photoName = bean.photoPath?.let { path ->
                    runCatching {
                        val file = File(path)
                        if (file.isFile && file.length() > 0L) {
                            val entry = "photos/${file.name}"
                            photos[entry] = file.readBytes()
                            entry
                        } else null
                    }.getOrNull()
                }
                put(JSONObject().apply {
                    put("id", bean.id); put("roastery", bean.roastery)
                    put("roasteryCountry", bean.roasteryCountry); put("name", bean.name)
                    put("flavourNotes", bean.flavourNotes); put("roastLevel", bean.roastLevel)
                    put("cuppingScore", bean.cuppingScore ?: JSONObject.NULL)
                    put("photoEntry", photoName ?: JSONObject.NULL)
                    put("coffeeInG", bean.coffeeInG ?: JSONObject.NULL)
                    put("coffeeOutG", bean.coffeeOutG ?: JSONObject.NULL)
                    put("brewTimeSec", bean.brewTimeSec ?: JSONObject.NULL)
                    put("origin", bean.origin); put("farm", bean.farm); put("region", bean.region)
                    put("variety", bean.variety); put("processing", bean.processing); put("liked", bean.liked)
                })
            }
        })
        root.put("bags", JSONArray().apply {
            bags.forEach { bag -> put(JSONObject().apply {
                put("id", bag.id); put("beanId", bag.beanId); put("roastDate", bag.roastDate)
                put("weightG", bag.weightG); put("price", bag.price); put("currency", bag.currency)
                put("lotNumber", bag.lotNumber); put("isFinished", bag.isFinished)
                put("recommendedGrindSetting", bag.recommendedGrindSetting)
                put("liked", bag.liked ?: JSONObject.NULL)
                put("lastBrewAtMillis", bag.lastBrewAtMillis)
            }) }
        })
        root.put("shots", JSONArray().apply {
            shots.forEach { shot ->
                put(JSONObject().apply {
                    put("id", shot.id); put("bagId", shot.bagId); put("groundCoffeeG", shot.groundCoffeeG)
                    put("yieldG", shot.yieldG); put("timeSec", shot.timeSec)
                    put("grindSetting", shot.grindSetting); put("liked", shot.liked ?: JSONObject.NULL)
                    put("pulledAtMillis", shot.pulledAtMillis); put("note", shot.note)
                    put("tastingAroma", shot.tastingAroma); put("tastingFlavour", shot.tastingFlavour)
                    put("tastingAcidity", shot.tastingAcidity); put("tastingSweetness", shot.tastingSweetness)
                    put("tastingBody", shot.tastingBody); put("tastingAftertaste", shot.tastingAftertaste)
                    put("overallImpression", shot.overallImpression ?: JSONObject.NULL)
                })
            }
        })
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(DATA_ENTRY))
            zip.write(root.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            photos.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry()
            }
        }
    }

    fun read(input: InputStream): Pair<BackupPreview, BackupContents> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) {
                    val bytes = ByteArrayOutputStream()
                    zip.copyTo(bytes)
                    entries[entry.name] = bytes.toByteArray()
                }
                zip.closeEntry()
            }
        }
        val jsonBytes = entries[DATA_ENTRY] ?: error("This is not a valid BeanLog backup.")
        val root = JSONObject(String(jsonBytes, Charsets.UTF_8))
        require(root.optInt("version", -1) in 1..BACKUP_VERSION) { "This backup version is not supported by this version of BeanLog." }

        val beans = root.optJSONArray("beans").toObjects { o ->
            Bean(
                id = o.optLong("id"), roastery = o.optString("roastery"),
                roasteryCountry = o.optString("roasteryCountry"), name = o.optString("name"),
                flavourNotes = o.optString("flavourNotes"), roastLevel = o.optString("roastLevel"),
                cuppingScore = o.doubleOrNull("cuppingScore"), photoPath = o.optString("photoEntry").takeUnless { it.isBlank() || it == "null" },
                coffeeInG = o.doubleOrNull("coffeeInG"), coffeeOutG = o.doubleOrNull("coffeeOutG"),
                brewTimeSec = o.intOrNull("brewTimeSec"), origin = o.optString("origin"),
                farm = o.optString("farm"), region = o.optString("region"), variety = o.optString("variety"),
                processing = o.optString("processing"), liked = o.optBoolean("liked")
            )
        }
        val bags = root.optJSONArray("bags").toObjects { o ->
            Bag(
                id = o.optLong("id"), beanId = o.optLong("beanId"), roastDate = o.optString("roastDate"),
                weightG = o.optInt("weightG"), price = o.optDouble("price"), currency = o.optString("currency", "EUR"),
                lotNumber = o.optString("lotNumber"), isFinished = o.optBoolean("isFinished"),
                recommendedGrindSetting = o.optString("recommendedGrindSetting"), liked = o.booleanOrNull("liked"),
                lastBrewAtMillis = o.optLong("lastBrewAtMillis", 0L)
            )
        }
        val shots = root.optJSONArray("shots").toObjects { o ->
            Shot(
                id = o.optLong("id"), bagId = o.optLong("bagId"), groundCoffeeG = o.optDouble("groundCoffeeG"),
                yieldG = o.optDouble("yieldG"), timeSec = o.optInt("timeSec"),
                grindSetting = o.optString("grindSetting"), liked = o.booleanOrNull("liked"),
                pulledAtMillis = o.optLong("pulledAtMillis"), note = o.optString("note"),
                tastingAroma = o.optString("tastingAroma"), tastingFlavour = o.optString("tastingFlavour"),
                tastingAcidity = o.optString("tastingAcidity"), tastingSweetness = o.optString("tastingSweetness"),
                tastingBody = o.optString("tastingBody"), tastingAftertaste = o.optString("tastingAftertaste"),
                overallImpression = o.intOrNull("overallImpression"))
        }
        val beanPhotoEntries = beans.mapNotNull { it.photoPath }.toSet()
        // Ignore legacy Brew-photo entries from older backups; only Bean photos remain supported.
        val contents = BackupContents(beans, bags, shots, entries.filterKeys { it in beanPhotoEntries })
        return BackupPreview(beans.size, bags.size, shots.size) to contents
    }

    /** Counts which records a merge would add, using the same identity rules as restore. */
    suspend fun previewMerge(dao: BeanDao, contents: BackupContents): BackupPreview {
        val plannedBeans = dao.allBeans().toMutableList()
        val beanTargets = mutableMapOf<Long, Long>()
        var syntheticId = -1L
        var beansToAdd = 0
        contents.beans.forEach { source ->
            val target = plannedBeans.firstOrNull { beanKey(it) == beanKey(source) }
                ?: source.copy(id = syntheticId--).also {
                    plannedBeans += it
                    beansToAdd++
                }
            beanTargets[source.id] = target.id
        }

        val plannedBags = dao.allBags().toMutableList()
        val bagTargets = mutableMapOf<Long, Long>()
        var bagsToAdd = 0
        contents.bags.forEach { source ->
            val targetBeanId = beanTargets[source.beanId] ?: return@forEach
            val target = plannedBags.firstOrNull {
                it.beanId == targetBeanId && bagKey(it) == bagKey(source)
            } ?: source.copy(id = syntheticId--, beanId = targetBeanId).also {
                plannedBags += it
                bagsToAdd++
            }
            bagTargets[source.id] = target.id
        }

        val plannedShots = dao.allShots().toMutableList()
        var shotsToAdd = 0
        contents.shots.forEach { source ->
            val targetBagId = bagTargets[source.bagId] ?: return@forEach
            val exists = plannedShots.any {
                it.bagId == targetBagId && shotKey(it) == shotKey(source)
            }
            if (!exists) {
                plannedShots += source.copy(id = syntheticId--, bagId = targetBagId)
                shotsToAdd++
            }
        }
        return BackupPreview(
            beans = contents.beans.size,
            bags = contents.bags.size,
            shots = contents.shots.size,
            beansToAdd = beansToAdd,
            bagsToAdd = bagsToAdd,
            shotsToAdd = shotsToAdd
        )
    }

    suspend fun restore(context: Context, dao: BeanDao, contents: BackupContents, replace: Boolean): RestoreResult {
        if (replace) {
            dao.clearShots()
            dao.clearBags()
            dao.clearBeans()
            context.filesDir.listFiles()?.filter {
                it.name.startsWith("bean_") && it.extension.equals("jpg", true)
            }?.forEach { it.delete() }
        }

        val currentBeans = dao.allBeans().toMutableList()
        val beanIds = mutableMapOf<Long, Long>()
        var beansAdded = 0
        contents.beans.forEach { source ->
            val existing = if (replace) null else currentBeans.firstOrNull { beanKey(it) == beanKey(source) }
            val photoEntry = source.photoPath
            val restoredPhoto = photoEntry?.let { contents.photos[it] }?.let { savePhoto(context, it) }
            if (existing != null) {
                beanIds[source.id] = existing.id
                if (existing.photoPath.isNullOrBlank() && restoredPhoto != null) {
                    val updated = existing.copy(photoPath = restoredPhoto)
                    dao.save(updated)
                    currentBeans[currentBeans.indexOf(existing)] = updated
                } else if (restoredPhoto != null) File(restoredPhoto).delete()
            } else {
                val id = dao.insertBean(source.copy(id = 0, photoPath = restoredPhoto))
                beanIds[source.id] = id
                currentBeans += source.copy(id = id, photoPath = restoredPhoto)
                beansAdded++
            }
        }

        val currentBags = dao.allBags().toMutableList()
        val bagIds = mutableMapOf<Long, Long>()
        var bagsAdded = 0
        contents.bags.forEach { source ->
            val targetBeanId = beanIds[source.beanId] ?: return@forEach
            val existing = if (replace) null else currentBags.firstOrNull {
                it.beanId == targetBeanId && bagKey(it) == bagKey(source)
            }
            if (existing != null) {
                bagIds[source.id] = existing.id
            } else {
                val id = dao.insertBag(source.copy(id = 0, beanId = targetBeanId))
                bagIds[source.id] = id
                currentBags += source.copy(id = id, beanId = targetBeanId)
                bagsAdded++
            }
        }

        val currentShots = dao.allShots().toMutableList()
        var shotsAdded = 0
        contents.shots.forEach { source ->
            val targetBagId = bagIds[source.bagId] ?: return@forEach
            val exists = !replace && currentShots.any {
                it.bagId == targetBagId && shotKey(it) == shotKey(source)
            }
            if (!exists) {
                dao.insertShot(source.copy(id = 0, bagId = targetBagId))
                shotsAdded++
            }
        }

        // Rebuild this denormalized timestamp from the actual Brew history. This
        // also repairs older backups that predate lastBrewAtMillis.
        val shotsByBag = dao.allShots().groupBy { it.bagId }
        dao.allBags().forEach { bag ->
            val latestBrew = shotsByBag[bag.id]?.maxOfOrNull { it.pulledAtMillis } ?: 0L
            if (bag.lastBrewAtMillis != latestBrew) {
                dao.saveBag(bag.copy(lastBrewAtMillis = latestBrew))
            }
        }
        return RestoreResult(beansAdded, bagsAdded, shotsAdded)
    }

    private fun beanKey(b: Bean) = listOf(b.roastery, b.roasteryCountry, b.name, b.origin, b.farm, b.region, b.variety, b.processing)
        .joinToString("|") { it.trim().lowercase() }
    private fun bagKey(b: Bag) = listOf(b.roastDate, b.weightG, b.price, b.currency, b.isFinished, b.recommendedGrindSetting)
        .joinToString("|")
    private fun shotKey(s: Shot) = listOf(s.pulledAtMillis, s.groundCoffeeG, s.yieldG, s.timeSec, s.grindSetting, s.note, s.liked)
        .joinToString("|")

    private fun savePhoto(context: Context, bytes: ByteArray): String? = runCatching {
        val file = File(context.filesDir, "bean_${UUID.randomUUID()}.jpg")
        file.writeBytes(bytes)
        file.absolutePath
    }.getOrNull()

    private fun JSONObject.doubleOrNull(key: String): Double? = if (isNull(key)) null else optDouble(key).takeIf { it.isFinite() }
    private fun JSONObject.intOrNull(key: String): Int? = if (isNull(key)) null else optInt(key)
    private fun JSONObject.booleanOrNull(key: String): Boolean? = if (isNull(key)) null else optBoolean(key)
    private fun <T> JSONArray?.toObjects(map: (JSONObject) -> T): List<T> =
        if (this == null) emptyList() else (0 until length()).map { map(getJSONObject(it)) }
}
