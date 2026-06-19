package com.example.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.playerGestures(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    screenWidth: Int
) = this.pointerInput(Unit) {
    detectDragGestures(
        onDrag = { change, dragAmount ->
            change.consume()
            // Right half drag (Volume)
            if (change.position.x > screenWidth / 2f && Math.abs(dragAmount.y) > Math.abs(dragAmount.x)) {
                onVolumeChange(-dragAmount.y)
            } else if (Math.abs(dragAmount.x) > Math.abs(dragAmount.y) && Math.abs(dragAmount.x) > 10) {
                if (dragAmount.x > 0) onSwipeRight() else onSwipeLeft()
            }
        }
    )
}
