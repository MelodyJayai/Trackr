package com.trackr.core.data.repository

import com.trackr.core.model.ChelaileCitySnapshot
import com.trackr.core.model.CityIdRepair
import com.trackr.core.model.TransitCity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CityIdRepairTest {

    @Test
    fun snapshot_shenzhenIsNotPutian() {
        val cities = ChelaileCitySnapshot.cities()
        val shenzhen = cities.single { it.cityName == "深圳" }
        val putian = cities.single { it.cityName == "莆田" }
        val hangzhou = cities.single { it.cityName == "杭州" }
        val guangzhou = cities.single { it.cityName == "广州" }
        val chengdu = cities.single { it.cityName == "成都" }
        assertEquals("014", shenzhen.cityId)
        assertEquals("051", putian.cityId)
        assertEquals("004", hangzhou.cityId)
        assertEquals("040", guangzhou.cityId)
        assertEquals("007", chengdu.cityId)
        assertTrue(shenzhen.cityId != putian.cityId)
    }

    @Test
    fun repair_rewritesShenzhenStoredAsPutian() {
        val repaired = CityIdRepair.repairSnapshotMismatch(
            TransitCity(cityId = "051", cityName = "深圳", pinyin = "ShenZhen", province = "广东")
        )
        assertEquals("014", repaired.cityId)
        assertEquals("深圳", repaired.cityName)
        assertEquals("广东", repaired.province)
    }

    @Test
    fun repair_leavesConsistentCitiesAlone() {
        val putian = TransitCity(cityId = "051", cityName = "莆田", province = "福建")
        assertEquals(putian, CityIdRepair.repairSnapshotMismatch(putian))
        val shenzhen = TransitCity(cityId = "014", cityName = "深圳", province = "广东")
        assertEquals(shenzhen.cityId, CityIdRepair.repairSnapshotMismatch(shenzhen).cityId)
    }

    @Test
    fun repair_doesNotOverwriteUnknownId() {
        val newer = TransitCity(cityId = "9999", cityName = "深圳", province = "广东")
        assertEquals(newer, CityIdRepair.repairSnapshotMismatch(newer))
    }

    @Test
    fun align_updatesMismatchedIdFromAuthoritativeList() {
        val list = listOf(
            TransitCity(cityId = "014", cityName = "深圳", pinyin = "ShenZhen", province = "广东"),
            TransitCity(cityId = "051", cityName = "莆田", province = "福建")
        )
        val corrected = CityIdRepair.alignWithAuthoritative(
            TransitCity(cityId = "051", cityName = "深圳", province = "广东"),
            list
        )
        assertEquals("014", corrected?.cityId)
        assertEquals("深圳", corrected?.cityName)
        assertNull(
            CityIdRepair.alignWithAuthoritative(
                TransitCity(cityId = "051", cityName = "莆田", province = "福建"),
                list
            )
        )
    }
}
