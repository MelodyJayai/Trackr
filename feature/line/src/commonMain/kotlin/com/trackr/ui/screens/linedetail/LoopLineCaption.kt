package com.trackr.ui.screens.linedetail

/**
 * 没有反向线路时，用车来了的折返站说明这是环线，而不是漏了换向按钮。
 */
internal sealed interface LoopLineCaption {
    /** 「环线 · 经六约新村东折返」 */
    data class Via(val midStop: String) : LoopLineCaption

    /** 起终点同名、但上游没给折返站 */
    data object Plain : LoopLineCaption
}

internal fun loopLineCaption(static: LineStatic): LoopLineCaption? {
    if (static.reverseDirection?.lineId?.isNotBlank() == true) return null
    val mid = static.line.loopMidStopName.trim()
    if (mid.isNotBlank()) return LoopLineCaption.Via(mid)
    val start = static.line.startStop.trim()
    val end = static.line.endStop.trim()
    if (start.isNotBlank() && start == end) return LoopLineCaption.Plain
    return null
}
