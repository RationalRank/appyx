package com.bumble.appyx.navmodel.modal.operation

import com.bumble.appyx.navmodel.modal.Modal
import com.bumble.appyx.navmodel.modal.Modal.TransitionState.MODAL
import com.bumble.appyx.navmodel.modal.ModalElements
import com.bumble.appyx.core.navigation.RoutingKey
import kotlinx.parcelize.Parcelize

@Parcelize
data class Show<T : Any>(
    private val key: RoutingKey<T>
) : ModalOperation<T> {

    override fun isApplicable(elements: ModalElements<T>) = true

    override fun invoke(elements: ModalElements<T>): ModalElements<T> {
        return elements.map {
            if (it.key == key) {
                it.transitionTo(
                    newTargetState = MODAL,
                    operation = this
                )
            } else {
                it
            }
        }
    }
}

fun <T : Any> Modal<T>.show(key: RoutingKey<T>) {
    accept(Show(key))
}
