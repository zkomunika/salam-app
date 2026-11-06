package app.pantauizin.ui.pengaturan

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.pantauizin.data.SettingsStore
import app.pantauizin.domain.DefaultTemplates
import app.pantauizin.domain.TemplateVariant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class PengaturanViewModelTest {
    private lateinit var settings: SettingsStore
    private var rescheduleCount = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        settings = SettingsStore(context)
        rescheduleCount = 0
        kotlinx.coroutines.runBlocking {
            settings.setJamPengingat(LocalTime.of(8, 0))
            settings.resetTemplate(TemplateVariant.A)
            settings.resetTemplate(TemplateVariant.B)
        }
    }

    @Test
    fun ubahJamMenyimpanNilaiDanMemicuRescheduleOnlyTepatSekali() = runTest {
        val vm = PengaturanViewModel(settings) { rescheduleCount++ }

        vm.ubahJam(LocalTime.of(10, 0))

        assertEquals(LocalTime.of(10, 0), settings.jamPengingat.first())
        assertEquals(1, rescheduleCount)
    }

    @Test
    fun simpanTemplateMenolakKosongDanKembalikanTemplateMengembalikanDefault() = runTest {
        val vm = PengaturanViewModel(settings) { rescheduleCount++ }

        assertFalse(vm.simpanTemplate(TemplateVariant.A, "   "))
        assertEquals(DefaultTemplates.A, settings.templateA.first())

        assertTrue(vm.simpanTemplate(TemplateVariant.A, "Template kustom {nama}"))
        assertEquals("Template kustom {nama}", settings.templateA.first())

        vm.kembalikanTemplate(TemplateVariant.A)
        assertEquals(DefaultTemplates.A, settings.templateA.first())
    }
}

