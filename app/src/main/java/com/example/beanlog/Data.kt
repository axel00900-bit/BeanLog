package com.example.beanlog

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/** A coffee bean + its recommended brew recipe and optional origin details. */
@Entity
data class Bean(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roastery: String,
    val roasteryCountry: String,
    val name: String,
    val flavourNotes: String = "",
    val roastLevel: String = "Medium",
    val cuppingScore: Double? = null,
    val photoPath: String? = null,
    val coffeeInG: Double? = null,
    val coffeeOutG: Double? = null,
    val brewTimeSec: Int? = null,
    @ColumnInfo(defaultValue = "''") val origin: String = "",
    @ColumnInfo(defaultValue = "''") val farm: String = "",
    @ColumnInfo(defaultValue = "''") val region: String = "",
    @ColumnInfo(defaultValue = "''") val variety: String = "",
    @ColumnInfo(defaultValue = "''") val processing: String = "",
    @ColumnInfo(defaultValue = "0") val liked: Boolean = false,
)

/** A specific bag you bought of a bean. */
@Entity(
    foreignKeys = [ForeignKey(
        entity = Bean::class, parentColumns = ["id"],
        childColumns = ["beanId"], onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("beanId")]
)
data class Bag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val beanId: Long,
    val roastDate: String,
    val weightG: Int,
    val price: Double,
    val currency: String = "EUR",
    val lotNumber: String = "",
    @ColumnInfo(defaultValue = "0") val isFinished: Boolean = false,
    @ColumnInfo(defaultValue = "''") val recommendedGrindSetting: String = "",
    val liked: Boolean? = null,
    @ColumnInfo(defaultValue = "0") val lastBrewAtMillis: Long = 0L,
)

/** Individual brew record saved for a specific bag. */
@Entity(
    foreignKeys = [ForeignKey(
        entity = Bag::class, parentColumns = ["id"],
        childColumns = ["bagId"], onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("bagId")]
)
data class Shot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bagId: Long,
    val groundCoffeeG: Double,
    val yieldG: Double,
    val timeSec: Int,
    @ColumnInfo(defaultValue = "''") val grindSetting: String = "",
    val liked: Boolean? = null,
    @ColumnInfo(defaultValue = "0") val pulledAtMillis: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "''") val note: String = "",
)

/** Bean plus sorting information for the front page. */
data class BeanWithLatestBag(
    @Embedded val bean: Bean,
    val latestBagId: Long,
    val latestRoastDate: String?,
    val openBagCount: Int,
    val latestBagLiked: Boolean?,
    val latestBrewAtMillis: Long,
)

@Dao
interface BeanDao {
    @Query("""
        SELECT Bean.*,
            COALESCE(MAX(Bag.id), 0) AS latestBagId,
            MAX(Bag.roastDate) AS latestRoastDate,
            COALESCE(SUM(CASE WHEN Bag.isFinished = 0 THEN 1 ELSE 0 END), 0) AS openBagCount,
            (SELECT liked FROM Bag AS LatestBag WHERE LatestBag.beanId = Bean.id
                ORDER BY isFinished ASC, lastBrewAtMillis DESC, roastDate DESC, id DESC LIMIT 1) AS latestBagLiked,
            COALESCE(MAX(Bag.lastBrewAtMillis), 0) AS latestBrewAtMillis
        FROM Bean LEFT JOIN Bag ON Bag.beanId = Bean.id
        GROUP BY Bean.id
    """)
    fun beansWithLatestBag(): Flow<List<BeanWithLatestBag>>

    @Query("SELECT * FROM Bean WHERE id = :id") fun bean(id: Long): Flow<Bean?>
    @Insert suspend fun insertBean(bean: Bean): Long
    @Upsert suspend fun save(bean: Bean)
    @Query("SELECT * FROM Bean") suspend fun allBeans(): List<Bean>
    @Query("SELECT * FROM Bag") suspend fun allBags(): List<Bag>
    @Query("SELECT * FROM Shot") suspend fun allShots(): List<Shot>
    @Query("SELECT * FROM Bean") fun observeAllBeans(): Flow<List<Bean>>
    @Query("SELECT * FROM Bag") fun observeAllBags(): Flow<List<Bag>>
    @Query("SELECT * FROM Shot") fun observeAllShots(): Flow<List<Shot>>
    @Insert suspend fun insertBag(bag: Bag): Long
    @Query("DELETE FROM Shot") suspend fun clearShots()
    @Query("DELETE FROM Bag") suspend fun clearBags()
    @Query("DELETE FROM Bean") suspend fun clearBeans()
    @Delete suspend fun delete(bean: Bean)

    @Query("SELECT * FROM Bag WHERE beanId = :beanId ORDER BY isFinished ASC, lastBrewAtMillis DESC, roastDate DESC, id DESC")
    fun bags(beanId: Long): Flow<List<Bag>>
    @Upsert suspend fun saveBag(bag: Bag)
    @Delete suspend fun deleteBag(bag: Bag)

    @Query("SELECT * FROM Shot WHERE bagId = :bagId ORDER BY id DESC")
    fun shots(bagId: Long): Flow<List<Shot>>
    @Insert suspend fun insertShot(shot: Shot): Long
    @Upsert suspend fun saveShot(shot: Shot)
    @Delete suspend fun deleteShot(shot: Shot)
    @Query("SELECT * FROM Shot WHERE id = :id LIMIT 1") suspend fun shotById(id: Long): Shot?
    @Query("SELECT * FROM Bag WHERE id = :id LIMIT 1") suspend fun bagById(id: Long): Bag?
}

@Database(entities = [Bean::class, Bag::class, Shot::class], version = 9, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): BeanDao

    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Bean ADD COLUMN origin TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE Bean ADD COLUMN farm TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE Bean ADD COLUMN variety TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE Bean ADD COLUMN processing TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE Bag ADD COLUMN isFinished INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS Shot (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bagId INTEGER NOT NULL,
                        groundCoffeeG REAL NOT NULL,
                        yieldG REAL NOT NULL,
                        timeSec INTEGER NOT NULL,
                        FOREIGN KEY(bagId) REFERENCES Bag(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_Shot_bagId ON Shot(bagId)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Bag ADD COLUMN recommendedGrindSetting TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE Bag ADD COLUMN liked INTEGER")
                db.execSQL("ALTER TABLE Shot ADD COLUMN grindSetting TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE Shot ADD COLUMN liked INTEGER")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Shot ADD COLUMN pulledAtMillis INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Bean ADD COLUMN liked INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Bean ADD COLUMN region TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE Shot ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Shot ADD COLUMN photoPath TEXT DEFAULT ''")
                db.execSQL("ALTER TABLE Bag ADD COLUMN lastBrewAtMillis INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Remove the obsolete Brew photo column without touching Bean photos.
                db.execSQL("""
                    CREATE TABLE Shot_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bagId INTEGER NOT NULL,
                        groundCoffeeG REAL NOT NULL,
                        yieldG REAL NOT NULL,
                        timeSec INTEGER NOT NULL,
                        grindSetting TEXT NOT NULL DEFAULT '',
                        liked INTEGER,
                        pulledAtMillis INTEGER NOT NULL DEFAULT 0,
                        note TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(bagId) REFERENCES Bag(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO Shot_new (id, bagId, groundCoffeeG, yieldG, timeSec, grindSetting, liked, pulledAtMillis, note)
                    SELECT id, bagId, groundCoffeeG, yieldG, timeSec, grindSetting, liked, pulledAtMillis, note
                    FROM Shot
                """.trimIndent())
                db.execSQL("DROP TABLE Shot")
                db.execSQL("ALTER TABLE Shot_new RENAME TO Shot")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_Shot_bagId ON Shot(bagId)")
            }
        }

        fun create(c: Context) =
            Room.databaseBuilder(c, AppDb::class.java, "beans.db")
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                // Never silently erase a user's coffee records when a migration is missing.
                .build()
    }
}
