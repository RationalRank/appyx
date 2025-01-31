package com.bumble.appyx.core.navigation.model.combined

import androidx.activity.OnBackPressedCallback
import com.bumble.appyx.core.plugin.Destroyable
import com.bumble.appyx.core.navigation.RoutingElements
import com.bumble.appyx.core.navigation.RoutingKey
import com.bumble.appyx.core.navigation.NavModel
import com.bumble.appyx.core.navigation.NavModelAdapter
import com.bumble.appyx.core.state.MutableSavedStateMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.coroutines.EmptyCoroutineContext

class CombinedNavModel<Routing>(
    val navModels: List<NavModel<Routing, *>>,
) : NavModel<Routing, Any?>, Destroyable {

    constructor(vararg navModels: NavModel<Routing, *>) : this(navModels.toList())

    private val scope = CoroutineScope(EmptyCoroutineContext + Dispatchers.Unconfined)

    override val elements: StateFlow<RoutingElements<Routing, *>> =
        combine(navModels.map { it.elements }) { arr -> arr.reduce { acc, list -> acc + list } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val screenState: StateFlow<NavModelAdapter.ScreenState<Routing, *>> =
        combine(navModels.map { it.screenState }) { arr ->
            NavModelAdapter.ScreenState(
                onScreen = arr.flatMap { it.onScreen },
                offScreen = arr.flatMap { it.offScreen },
            )
        }
            .stateIn(scope, SharingStarted.Eagerly, NavModelAdapter.ScreenState())

    override val onBackPressedCallbackList: List<OnBackPressedCallback>
        get() = navModels.flatMap { it.onBackPressedCallbackList }

    override fun onTransitionFinished(key: RoutingKey<Routing>) {
        navModels.forEach { it.onTransitionFinished(key) }
    }

    override fun onTransitionFinished(keys: Collection<RoutingKey<Routing>>) {
        navModels.forEach { it.onTransitionFinished(keys) }
    }

    override fun saveInstanceState(state: MutableSavedStateMap) {
        navModels.forEach { it.saveInstanceState(state) }
    }

    override fun destroy() {
        scope.cancel()
        navModels.filterIsInstance<Destroyable>().forEach { it.destroy() }
    }

}
