package com.trackr.core.network.remote.chelaile

import com.trackr.core.network.testing.mockHttpClient
import com.trackr.core.network.testing.mockResponse
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 车来了个别线路上游只给 `lineNo`、`name` 给空串。
 *
 * 以前 `name` 原样带出去，线路号徽标画出来是个空块、收藏一条线之后首页只剩一个空白圆角块（issue #20）。
 * 现在解析时就用线路号顶上。
 */
class ChelaileLineNameFallbackTest {

    private val blankNameDetail = """
        {
          "jsonr": {
            "data": {
              "stationList": [
                {
                  "sn": "人民广场",
                  "sId": "stn_1",
                  "physicalStId": "phys_1",
                  "lat": 31.2304,
                  "lng": 121.4737,
                  "lines": [
                    {
                      "line": {
                        "lineId": "cl_line_blank",
                        "name": "",
                        "lineNo": "M200",
                        "direction": 0,
                        "startSn": "科技园",
                        "endSn": "人民广场"
                      },
                      "targetStation": { "order": 3, "sId": "stn_1", "sn": "人民广场" },
                      "stnStates": []
                    }
                  ]
                }
              ]
            }
          }
        }
    """.trimIndent()

    @Test
    fun stopDetail_whenLineNameIsBlank_fallsBackToLineNo() = runBlocking {
        val client = ChelaileApiClient(mockHttpClient { mockResponse(it, blankNameDetail) })

        val line = client.getStopDetail(cityId = "034", physicalStId = "phys_1")
            .single()
            .lines
            .single()
            .line

        assertEquals("M200", line.name)
        assertEquals("M200", line.lineNo)
    }

    @Test
    fun lineDetail_whenLineNameIsBlank_fallsBackToLineNo() = runBlocking {
        val body = """
            {
              "jsonr": {
                "data": {
                  "line": {
                    "lineId": "cl_line_blank",
                    "name": "",
                    "lineNo": "M200",
                    "direction": 0,
                    "startSn": "科技园",
                    "endSn": "人民广场"
                  },
                  "stops": []
                }
              }
            }
        """.trimIndent()
        val client = ChelaileApiClient(mockHttpClient { mockResponse(it, body) })

        val detail = client.getLineDetail(cityId = "034", lineId = "cl_line_blank")

        assertEquals("M200", detail.line.name)
    }

    @Test
    fun lineDetail_loopLine_parsesMidStopAndHasNoReverse() = runBlocking {
        val body = """
            {
              "jsonr": {
                "data": {
                  "line": {
                    "lineId": "0755-M3243-0",
                    "name": "M324",
                    "lineNo": "M3243",
                    "direction": 0,
                    "startSn": "秀峰公交总站",
                    "endSn": "秀峰公交总站",
                    "loopMidStopName": "六约新村东"
                  },
                  "otherlines": [],
                  "stations": []
                }
              }
            }
        """.trimIndent()
        val client = ChelaileApiClient(mockHttpClient { mockResponse(it, body) })

        val detail = client.getLineDetail(cityId = "014", lineId = "0755-M3243-0")

        assertEquals("六约新村东", detail.line.loopMidStopName)
        assertEquals("秀峰公交总站", detail.line.startStop)
        assertEquals("秀峰公交总站", detail.line.endStop)
        assertNull(detail.reverseDirection)
    }

    @Test
    fun nearbyStops_surfacesCityToast() = runBlocking {
        val body = """
            {
              "jsonr": {
                "data": {
                  "toast": "未找到附近车站，已为您选择莆田市中心的车站",
                  "nearSts": [
                    { "sn": "龙桥市场", "sId": "s1", "lat": 25.43, "lng": 119.01, "lines": [] }
                  ]
                }
              }
            }
        """.trimIndent()
        val client = ChelaileApiClient(mockHttpClient { mockResponse(it, body) })

        val page = client.getNearbyStops(cityId = "051", wgsLat = 22.69, wgsLng = 114.02, limit = 4)

        assertEquals("未找到附近车站，已为您选择莆田市中心的车站", page.notice)
        assertEquals("龙桥市场", page.stops.single().name)
    }

    @Test
    fun nearbyStops_blankToast_isNull() = runBlocking {
        val body = """
            {
              "jsonr": {
                "data": {
                  "toast": "",
                  "nearSts": [
                    { "sn": "茜坑老村东", "sId": "s1", "lat": 22.69, "lng": 114.02, "lines": [] }
                  ]
                }
              }
            }
        """.trimIndent()
        val client = ChelaileApiClient(mockHttpClient { mockResponse(it, body) })

        val page = client.getNearbyStops(cityId = "014", wgsLat = 22.69, wgsLng = 114.02, limit = 4)

        assertNull(page.notice)
        assertEquals("茜坑老村东", page.stops.single().name)
    }
}
