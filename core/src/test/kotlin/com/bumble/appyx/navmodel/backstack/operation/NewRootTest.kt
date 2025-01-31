package com.bumble.appyx.navmodel.backstack.operation

import com.bumble.appyx.core.navigation.Operation.Noop
import com.bumble.appyx.navmodel.assertRoutingElementsEqual
import com.bumble.appyx.navmodel.backstack.BackStack.TransitionState.CREATED
import com.bumble.appyx.navmodel.backstack.BackStack.TransitionState.DESTROYED
import com.bumble.appyx.navmodel.backstack.BackStack.TransitionState.ACTIVE
import com.bumble.appyx.navmodel.backstack.BackStack.TransitionState.STASHED_IN_BACK_STACK
import com.bumble.appyx.navmodel.backstack.BackStackElement
import com.bumble.appyx.navmodel.backstack.operation.Routing.Routing1
import com.bumble.appyx.navmodel.backstack.operation.Routing.Routing2
import com.bumble.appyx.navmodel.backstack.operation.Routing.Routing3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

internal class NewRootTest {

    @Test
    fun `is always applicable`() {

        val elements = emptyList<BackStackElement<Routing>>()
        val operation = NewRoot<Routing>(element = Routing1)

        val applicable = operation.isApplicable(elements)

        assertEquals(applicable, true)
    }

    @Test
    fun `crashes when no element on screen`() {

        val elements = emptyList<BackStackElement<Routing>>()
        val operation = NewRoot<Routing>(element = Routing1)

        assertThrows(IllegalArgumentException::class.java) {
            operation.invoke(elements)
        }
    }

    @Test
    fun `passed element becomes the only one when currently on screen`() {

        val elements = listOf<BackStackElement<Routing>>(
            backStackElement(
                element = Routing1,
                fromState = STASHED_IN_BACK_STACK,
                targetState = STASHED_IN_BACK_STACK,
                operation = Noop()
            ),
            backStackElement(
                element = Routing2,
                fromState = ACTIVE,
                targetState = ACTIVE,
                operation = Noop()
            )
        )

        val operation = NewRoot<Routing>(element = Routing2)

        val newElements = operation.invoke(elements = elements)

        val expectedElements = listOf<BackStackElement<Routing>>(
            backStackElement(
                element = Routing2,
                fromState = ACTIVE,
                targetState = ACTIVE,
                operation = Noop()
            )
        )
        newElements.assertRoutingElementsEqual(expectedElements)
    }

    @Test
    fun `destroys current element on screen, remove the rest, and add on screen the newly created one`() {

        val elements = listOf<BackStackElement<Routing>>(
            backStackElement(
                element = Routing1,
                fromState = STASHED_IN_BACK_STACK,
                targetState = STASHED_IN_BACK_STACK,
                operation = Noop()
            ),
            backStackElement(
                element = Routing2,
                fromState = ACTIVE,
                targetState = ACTIVE,
                operation = Noop()
            )
        )

        val operation = NewRoot<Routing>(element = Routing3)

        val newElements = operation.invoke(elements = elements)

        val expectedElements = listOf<BackStackElement<Routing>>(
            backStackElement(
                element = Routing2,
                fromState = ACTIVE,
                targetState = DESTROYED,
                operation = operation
            ),
            backStackElement(
                element = Routing3,
                fromState = CREATED,
                targetState = ACTIVE,
                operation = operation
            )
        )
        newElements.assertRoutingElementsEqual(expectedElements)
    }
}
