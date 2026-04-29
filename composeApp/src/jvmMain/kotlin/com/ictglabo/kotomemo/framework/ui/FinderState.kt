package com.ictglabo.kotomemo.framework.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class FinderState {
    enum class Mode { Find, Replace }

    var visible by mutableStateOf(false)
    var mode by mutableStateOf(Mode.Find)
    var query by mutableStateOf("")
    var replacement by mutableStateOf("")
    var regex by mutableStateOf(false)
    var caseSensitive by mutableStateOf(false)
    var lastMatchCount by mutableStateOf(0)
    var lastReplaceCount by mutableStateOf(-1)
    var wrappedAround by mutableStateOf(false)
    var focusTick by mutableStateOf(0)

    fun toggleFind() {
        if (visible && mode == Mode.Find) {
            hide()
        } else {
            mode = Mode.Find
            visible = true
            focusTick++
        }
    }

    fun toggleReplace() {
        if (visible && mode == Mode.Replace) {
            hide()
        } else {
            mode = Mode.Replace
            visible = true
            focusTick++
        }
    }

    fun hide() {
        visible = false
        lastReplaceCount = -1
        wrappedAround = false
    }
}
