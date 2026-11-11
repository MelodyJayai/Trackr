package com.trackr.ui.screens.linedetail

import com.trackr.core.model.BusLineDirection
import com.trackr.core.model.BusStop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoopLineCaptionTest {

    private val stops = listOf(BusStop(stopId = "s1", name = "秀峰公交总站", order = 1))

    private fun line(
        start: String = "秀峰公交总站",
        end: String = "秀峰公交总站",
        mid: String = ""
    ) = BusLineDirection(
        lineId = "0755-M3243-0",
        name = "M324",
        startStop = start,
        endStop = end,
        loopMidStopName = mid
    )

    @Test
    fun loopWithMidStop_explainsTurnaround() {
        val static = LineStatic(line(mid = "六约新村东"), reverseDirection = null, stops = stops)
        assertEquals(LoopLineCaption.Via("六约新村东"), loopLineCaption(static))
    }

    @Test
    fun sameTerminalWithoutMidStop_isPlainLoop() {
        val static = LineStatic(line(), reverseDirection = null, stops = stops)
        assertEquals(LoopLineCaption.Plain, loopLineCaption(static))
    }

    @Test
    fun reverseLine_hidesLoopCaption() {
        val reverse = line(start = "火车站", end = "科技园").copy(lineId = "rev")
        val static = LineStatic(
            line = line(start = "科技园", end = "火车站", mid = "不应显示"),
            reverseDirection = reverse,
            stops = stops
        )
        assertNull(loopLineCaption(static))
    }

    @Test
    fun oneWayLine_hasNoCaption() {
        val static = LineStatic(
            line = line(start = "科技园", end = "火车站"),
            reverseDirection = null,
            stops = stops
        )
        assertNull(loopLineCaption(static))
    }

    @Test
    fun blankReverseLineId_stillShowsLoopCaption() {
        val static = LineStatic(
            line = line(mid = "六约新村东"),
            reverseDirection = line().copy(lineId = ""),
            stops = stops
        )
        assertEquals(LoopLineCaption.Via("六约新村东"), loopLineCaption(static))
    }
}
