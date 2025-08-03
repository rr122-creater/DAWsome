// database/DAWDatabase.kt
@Database(
    entities = [
        AudioProject::class,
        Track::class, 
        AudioClip::class,
        Effect::class,
        TrackEffect::class,
        Automation::class,
        Preset::class,
        Marker::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DAWDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun trackDao(): TrackDao
    abstract fun audioClipDao(): AudioClipDao
    abstract fun effectDao(): EffectDao
    abstract fun automationDao(): AutomationDao
    abstract fun presetDao(): PresetDao
    abstract fun markerDao(): MarkerDao
    
    companion object {
        @Volatile
        private var INSTANCE: DAWDatabase? = null
        
        fun getDatabase(context: Context): DAWDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DAWDatabase::class.java,
                    "daw_database"
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
    
    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Populate with default effects
            CoroutineScope(Dispatchers.IO).launch {
                populateDefaultEffects(db)
            }
        }
    }
}
