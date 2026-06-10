package com.stefanoneve.ultimatenotes.util

import android.view.MotionEvent
import kotlinx.coroutines.flow.MutableSharedFlow

/** A press of the S Pen barrel button, with the window position of the tip. */
data class SPenButtonPress(val x: Float, val y: Float)

/**
 * Routes raw motion events from the Activity to Compose. The S Pen barrel
 * button arrives as BUTTON_STYLUS_PRIMARY (touching) or as a generic
 * ACTION_BUTTON_PRESS while hovering; both paths land here.
 */
object SPenEvents {

    val buttonPresses = MutableSharedFlow<SPenButtonPress>(extraBufferCapacity = 4)

    private var lastButtonState = 0

    fun onMotion(ev: MotionEvent) {
        val isStylus =
            ev.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS ||
                ev.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER
        if (!isStylus) {
            lastButtonState = 0
            return
        }
        val stylusButtons =
            MotionEvent.BUTTON_STYLUS_PRIMARY or MotionEvent.BUTTON_SECONDARY
        val pressedNow = ev.buttonState and stylusButtons
        val pressedBefore = lastButtonState and stylusButtons
        val isPressEdge =
            ev.actionMasked == MotionEvent.ACTION_BUTTON_PRESS ||
                (pressedNow != 0 && pressedBefore == 0)
        if (isPressEdge) {
            buttonPresses.tryEmit(SPenButtonPress(ev.x, ev.y))
        }
        lastButtonState = ev.buttonState
    }
}
