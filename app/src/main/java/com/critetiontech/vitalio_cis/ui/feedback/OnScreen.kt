package com.critetiontech.ctvitalio.ui.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Whether an element is at least partly visible in the window. Screens built as one scrolling
 * Column (like Home) keep every part composed, so an endless animation keeps running and drawing
 * after it's scrolled away; read [visible] to run it only while it can be seen.
 */
class OnScreen {
    var visible by mutableStateOf(true)
        internal set
}

@Composable
fun rememberOnScreen(): OnScreen = remember { OnScreen() }

/**
 * Updates [state] as this element scrolls in and out of view. [boundsInWindow] is clipped by the
 * scrolling parent, so it's empty once the element is fully off screen.
 */
fun Modifier.trackOnScreen(state: OnScreen): Modifier = onGloballyPositioned { coordinates ->
    val bounds = coordinates.boundsInWindow()
    val visible = bounds.width > 0f && bounds.height > 0f
    if (state.visible != visible) state.visible = visible
}
