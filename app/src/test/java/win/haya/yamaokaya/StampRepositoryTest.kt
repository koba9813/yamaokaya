package win.haya.yamaokaya

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class StampRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: StampRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("stamp_rally", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        repository = StampRepository(context)
    }

    @Test
    fun checkIn_incrementsCount() {
        assertTrue(repository.checkIn("山岡家 テスト店", nowMillis = 1000L))
        assertEquals(1, repository.getCount("山岡家 テスト店"))
    }

    @Test
    fun checkIn_withinCooldown_isRejected() {
        val baseTime = 1000L
        assertTrue(repository.checkIn("山岡家 テスト店", nowMillis = baseTime))
        assertFalse(repository.checkIn("山岡家 テスト店", nowMillis = baseTime + 60_000L))
        assertEquals(1, repository.getCount("山岡家 テスト店"))
    }

    @Test
    fun checkIn_afterCooldown_isAccepted() {
        val baseTime = 1000L
        assertTrue(repository.checkIn("山岡家 テスト店", nowMillis = baseTime))
        assertTrue(repository.checkIn("山岡家 テスト店", nowMillis = baseTime + 3 * 60 * 60 * 1000L + 1L))
        assertEquals(2, repository.getCount("山岡家 テスト店"))
    }

    @Test
    fun canCheckIn_returnsFalseWithinCooldown() {
        val baseTime = 1000L
        repository.checkIn("山岡家 テスト店", nowMillis = baseTime)
        assertFalse(repository.canCheckIn("山岡家 テスト店", nowMillis = baseTime + 60_000L))
    }

    @Test
    fun getRemainingCooldownMillis_returnsPositiveWithinCooldown() {
        val baseTime = 1000L
        repository.checkIn("山岡家 テスト店", nowMillis = baseTime)
        val remaining = repository.getRemainingCooldownMillis("山岡家 テスト店", nowMillis = baseTime + 60_000L)
        assertTrue(remaining > 0L)
    }

    @Test
    fun getRemainingCooldownMillis_returnsZeroAfterCooldown() {
        val baseTime = 1000L
        repository.checkIn("山岡家 テスト店", nowMillis = baseTime)
        val remaining = repository.getRemainingCooldownMillis(
            "山岡家 テスト店",
            nowMillis = baseTime + 3 * 60 * 60 * 1000L + 1L
        )
        assertEquals(0L, remaining)
    }

    @Test
    fun getRanking_returnsSortedByCount() {
        repository.checkIn("shop_a", nowMillis = 1000L)
        repository.checkIn("shop_a", nowMillis = 4 * 60 * 60 * 1000L)
        repository.checkIn("shop_b", nowMillis = 1000L)

        val ranking = repository.getRanking(listOf("shop_a", "shop_b", "shop_c"))
        assertEquals("shop_a", ranking[0].first)
        assertEquals(2, ranking[0].second)
        assertEquals("shop_b", ranking[1].first)
        assertEquals(1, ranking[1].second)
    }

    @Test
    fun getVisitedCount_returnsOnlyVisitedShops() {
        repository.checkIn("shop_a", nowMillis = 1000L)
        repository.checkIn("shop_b", nowMillis = 1000L)

        assertEquals(2, repository.getVisitedCount(listOf("shop_a", "shop_b", "shop_c")))
    }

    @Test
    fun getTotalCheckIns_returnsSum() {
        repository.checkIn("shop_a", nowMillis = 1000L)
        repository.checkIn("shop_a", nowMillis = 4 * 60 * 60 * 1000L)
        repository.checkIn("shop_b", nowMillis = 1000L)

        assertEquals(3, repository.getTotalCheckIns(listOf("shop_a", "shop_b", "shop_c")))
    }
}
