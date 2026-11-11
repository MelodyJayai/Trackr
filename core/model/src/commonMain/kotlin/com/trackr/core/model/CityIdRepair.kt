package com.trackr.core.model

/**
 * 纠正「城市名和车来了 cityId 对不上」的已保存城市。
 *
 * 旧的内置城市表把深圳写成了 051，而 051 在车来了是莆田。这个错误的编号一旦写进
 * `selected_city_json`，之后即使网络恢复也不会再改，附近站就会一直是莆田（龙桥市场）。
 */
object CityIdRepair {

    /**
     * 已保存的 cityId 在快照里属于另一个城市、而城市名又能在快照里对上时，改成名字对应的编号。
     *
     * 快照里没有这对矛盾（编号是快照不认识的新编号，或名字对不上）时原样返回，避免用过期快照盖掉服务器后来发的编号。
     */
    fun repairSnapshotMismatch(city: TransitCity): TransitCity {
        if (city.cityId.isBlank() || city.cityName.isBlank()) return city
        val snapshot = ChelaileCitySnapshot.cities()
        val cleanName = ProvinceCityCatalog.cleanCityName(city.cityName)
        if (cleanName.isBlank()) return city
        val byName = snapshot.firstOrNull {
            ProvinceCityCatalog.cleanCityName(it.cityName) == cleanName && it.cityId.isNotBlank()
        } ?: return city
        if (byName.cityId == city.cityId) return city
        val byId = snapshot.firstOrNull { it.cityId == city.cityId } ?: return city
        if (ProvinceCityCatalog.cleanCityName(byId.cityName) == cleanName) return city
        return copyId(city, byName)
    }

    /**
     * 权威列表（服务器刚拉到的城市表）里，同名城市的 cityId 和已保存的不一样时，返回应写回的城市。
     * 对不上、或同名有多条又分不清省份时返回 null，不把用户切到另一个城市。
     */
    fun alignWithAuthoritative(current: TransitCity, authoritative: List<TransitCity>): TransitCity? {
        val cleanName = ProvinceCityCatalog.cleanCityName(current.cityName)
        if (cleanName.isBlank()) return null
        val named = authoritative.filter {
            ProvinceCityCatalog.cleanCityName(it.cityName) == cleanName && it.cityId.isNotBlank()
        }
        val match = when (named.size) {
            0 -> return null
            1 -> named[0]
            else -> {
                val prov = current.province.ifBlank { ProvinceCityCatalog.inferProvince(current.cityName) }
                named.firstOrNull { city ->
                    val p = city.province.ifBlank { ProvinceCityCatalog.inferProvince(city.cityName) }
                    prov.isNotBlank() && prov != "其他" && p == prov
                } ?: return null
            }
        }
        if (match.cityId == current.cityId) return null
        return copyId(current, match)
    }

    private fun copyId(current: TransitCity, source: TransitCity): TransitCity = current.copy(
        cityId = source.cityId,
        pinyin = source.pinyin.ifBlank { current.pinyin },
        supportSubway = source.supportSubway,
        isHot = source.isHot,
        mygolbsCityCode = source.mygolbsCityCode ?: current.mygolbsCityCode,
        province = current.province.ifBlank { source.province }
    )
}
