package win.haya.doko

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YamaokayaFinderTest {

    @Test
    fun findNearest_fromSapporoStation_returnsNearestShop() {
        // 札幌駅付近
        val current = Coordinates(43.068664, 141.350755)
        val nearest = YamaokayaFinder.findNearest(current)

        assertNotNull(nearest)
        assertTrue(nearest!!.distanceMeters < 5000f)
    }

    @Test
    fun getRegisteredShops_isNotEmpty() {
        assertTrue(YamaokayaFinder.getRegisteredShops().isNotEmpty())
    }

    @Test
    fun getShopNames_matchesRegisteredShopCount() {
        assertEquals(
            YamaokayaFinder.getRegisteredShops().size,
            YamaokayaFinder.getShopNames().size
        )
    }

    @Test
    fun getPrefectures_containsHokkaido() {
        val prefectures = YamaokayaFinder.getPrefectures()
        assertTrue(prefectures.contains("北海道"))
    }

    @Test
    fun getShopsByPrefecture_hokkaido_returnsHokkaidoShopsOnly() {
        val shops = YamaokayaFinder.getShopsByPrefecture("北海道")
        assertTrue(shops.isNotEmpty())
        assertTrue(shops.all { it.prefecture == "北海道" })
    }
}
