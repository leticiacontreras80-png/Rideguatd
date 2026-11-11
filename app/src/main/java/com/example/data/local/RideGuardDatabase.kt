package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Contact
import com.example.data.model.SafetyEvent
import com.example.data.model.SafetyEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Converters {
    @TypeConverter
    fun fromSafetyEventType(value: SafetyEventType): String = value.name

    @TypeConverter
    fun toSafetyEventType(value: String): SafetyEventType = try {
        SafetyEventType.valueOf(value)
    } catch (e: Exception) {
        SafetyEventType.ERRATIC_DRIVING
    }
}

@Database(
    entities = [Contact::class, SafetyEvent::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RideGuardDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun safetyEventDao(): SafetyEventDao

    companion object {
        @Volatile
        private var INSTANCE: RideGuardDatabase? = null

        fun getDatabase(context: Context): RideGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RideGuardDatabase::class.java,
                    "ride_guard_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getDatabase(context)
                                database.contactDao().insertContacts(
                                    listOf(
                                        Contact(
                                            name = "Mom (Sarah)",
                                            phone = "+1 (555) 234-5678",
                                            relationship = "Mother",
                                            isEmergencyAlertEnabled = true
                                        ),
                                        Contact(
                                            name = "Partner (Jordan)",
                                            phone = "+1 (555) 876-5432",
                                            relationship = "Spouse",
                                            isEmergencyAlertEnabled = true
                                        ),
                                        Contact(
                                            name = "Emergency Services (Dispatch)",
                                            phone = "911",
                                            relationship = "Emergency Hotline",
                                            isEmergencyAlertEnabled = false
                                        )
                                    )
                                )
                                database.safetyEventDao().insertEvent(
                                    SafetyEvent(
                                        eventType = SafetyEventType.RIDE_STARTED,
                                        title = "Ride Monitoring Started",
                                        description = "RideGuard telemetry and live safety monitoring activated for Toyota Camry (Alex Rivera).",
                                        severityLevel = 1
                                    )
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
