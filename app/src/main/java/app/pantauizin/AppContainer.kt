package app.pantauizin

import android.content.Context
import app.pantauizin.data.AndroidBuktiStorage
import app.pantauizin.data.AppDatabase
import app.pantauizin.data.BuktiStorage
import app.pantauizin.data.IzinRepository
import app.pantauizin.data.NotifikasiRepository
import app.pantauizin.data.SantriRepository
import app.pantauizin.data.SantriSeeder
import app.pantauizin.data.SettingsStore
import app.pantauizin.notify.AlarmScheduler
import app.pantauizin.notify.NotificationHelper
import app.pantauizin.notify.SweepRunner
import java.time.LocalDateTime

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase = AppDatabase.build(appContext)
    val settingsStore: SettingsStore = SettingsStore(appContext)
    val buktiStorage: BuktiStorage = AndroidBuktiStorage(appContext)
    val santriRepository: SantriRepository = SantriRepository(database.santriDao())
    val santriSeeder: SantriSeeder = SantriSeeder(appContext, database.santriDao())
    val izinRepository: IzinRepository = IzinRepository(
        izinDao = database.izinDao(),
        konfirmasiDao = database.konfirmasiDao(),
        bukti = buktiStorage
    )
    val notifikasiRepository: NotifikasiRepository = NotifikasiRepository(database.notifikasiDao())
    val notificationHelper: NotificationHelper = NotificationHelper(appContext)
    val alarmScheduler: AlarmScheduler = AlarmScheduler(appContext)
    val sweepRunner: SweepRunner = SweepRunner(
        izin = izinRepository,
        notif = database.notifikasiDao(),
        settings = settingsStore,
        notifier = notificationHelper,
        scheduler = alarmScheduler,
        clock = { LocalDateTime.now() }
    )
}

