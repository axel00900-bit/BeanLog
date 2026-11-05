package com.example.beanlog

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** A coffee bean + its recommended brew recipe. */
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
    // Recommended brew settings
    val coffeeInG: Double? = null,
    val coffeeOutG: Double? = null,
    val brewTimeSec: Int? = null,
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
    val roastDate: String,      // stored as yyyy-MM-dd, shown as dd/MM/yyyy
    val weightG: Int,
    val price: Double,          // price in the currency you paid
    val currency: String = "EUR",
    val lotNumber: String = "",
)

@Dao
interface BeanDao {
    @Query("SELECT * FROM Bean ORDER BY roastery, name") fun beans(): Flow<List<Bean>>
    @Query("SELECT * FROM Bean WHERE id = :id") fun bean(id: Long): Flow<Bean?>
    @Upsert suspend fun save(bean: Bean)
    @Delete suspend fun delete(bean: Bean)

    @Query("SELECT * FROM Bag WHERE beanId = :beanId ORDER BY roastDate DESC")
    fun bags(beanId: Long): Flow<List<Bag>>
    @Upsert suspend fun saveBag(bag: Bag)
    @Delete suspend fun deleteBag(bag: Bag)
}

@Database(entities = [Bean::class, Bag::class], version = 2, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): BeanDao

    companion object {
        fun create(c: Context) =
            Room.databaseBuilder(c, AppDb::class.java, "beans.db")
                .fallbackToDestructiveMigration(true) // wipes test data when the schema changes
                .build()
    }
}
