package com.critetiontech.ctvitalio.ui.feedback

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ripple
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.launch

/**
 * App-wide indication: the Material ripple plus a light tap haptic when a press turns into a
 * click. Provided as `LocalIndication` in the theme, so every `Modifier.clickable` in the app
 * gets it without changing call sites. Material buttons draw their own ripple, so they get
 * haptics from the wrappers in HapticControls.kt instead.
 */
object HapticIndication : IndicationNodeFactory {

    private val ripple = ripple()

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        HapticIndicationNode(interactionSource, ripple.create(interactionSource))

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = javaClass.hashCode()
}

private class HapticIndicationNode(
    private val interactionSource: InteractionSource,
    rippleNode: DelegatableNode,
) : DelegatingNode(), CompositionLocalConsumerModifierNode {

    init {
        delegate(rippleNode)
    }

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                // Release means the press completed as a click; Cancel (scroll started) stays silent.
                if (interaction is PressInteraction.Release) {
                    Haptics.perform(currentValueOf(LocalView), HapticEvent.Tap)
                }
            }
        }
    }
}
