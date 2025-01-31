package com.bumble.appyx.core.children

import androidx.lifecycle.coroutineScope
import com.bumble.appyx.core.modality.AncestryInfo
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.navigation.RoutingKey
import com.bumble.appyx.core.node.ParentNode
import com.bumble.appyx.core.node.build
import com.bumble.appyx.core.state.MutableSavedStateMap
import com.bumble.appyx.core.state.SavedStateMap
import com.bumble.appyx.utils.customisations.NodeCustomisationDirectory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

/**
 * Initializes and removes nodes based on parent node routing source.
 *
 * Lifecycle of these nodes is managed in [com.bumble.appyx.core.lifecycle.ChildNodeLifecycleManager].
 */
internal class ChildNodeCreationManager<Routing : Any>(
    private var savedStateMap: SavedStateMap?,
    private val customisations: NodeCustomisationDirectory,
    private val keepMode: ChildEntry.KeepMode,
) {
    private lateinit var parentNode: ParentNode<Routing>
    private val _children =
        MutableStateFlow<Map<RoutingKey<Routing>, ChildEntry<Routing>>>(emptyMap())
    val children: StateFlow<ChildEntryMap<Routing>> = _children.asStateFlow()

    fun launch(parentNode: ParentNode<Routing>) {
        this.parentNode = parentNode
        savedStateMap.restoreChildren()?.also { restoredMap ->
            _children.update { restoredMap }
            savedStateMap = null
        }
        syncNavModelWithChildren(parentNode)
    }

    private fun syncNavModelWithChildren(parentNode: ParentNode<Routing>) {
        parentNode.lifecycle.coroutineScope.launch {
            parentNode.navModel.screenState.collect { state ->
                val navModelKeepKeys: Set<RoutingKey<Routing>>
                val navModelSuspendKeys: Set<RoutingKey<Routing>>
                val navModelKeys: Set<RoutingKey<Routing>>
                when (keepMode) {
                    ChildEntry.KeepMode.KEEP -> {
                        navModelKeepKeys =
                            (state.onScreen + state.offScreen).mapNotNullToSet { element -> element.key }
                        navModelSuspendKeys = emptySet()
                        navModelKeys = navModelKeepKeys
                    }
                    ChildEntry.KeepMode.SUSPEND -> {
                        navModelKeepKeys =
                            state.onScreen.mapNotNullToSet { element -> element.key }
                        navModelSuspendKeys =
                            state.offScreen.mapNotNullToSet { element -> element.key }
                        navModelKeys = navModelKeepKeys + navModelSuspendKeys
                    }
                }
                updateChildren(navModelKeys, navModelKeepKeys, navModelSuspendKeys)
            }
        }
    }

    private fun updateChildren(
        navModelKeys: Set<RoutingKey<Routing>>,
        navModelKeepKeys: Set<RoutingKey<Routing>>,
        navModelSuspendKeys: Set<RoutingKey<Routing>>,
    ) {
        _children.update { map ->
            val localKeys = map.keys
            val localKeptKeys = map.entries.mapNotNullToSet { entry ->
                entry.key.takeIf { entry.value is ChildEntry.Initialized }
            }
            val localSuspendedKeys = map.entries.mapNotNullToSet { entry ->
                entry.key.takeIf { entry.value is ChildEntry.Suspended }
            }
            val newKeys = navModelKeys - localKeys
            val removedKeys = localKeys - navModelKeys
            val keepKeys = localSuspendedKeys.intersect(navModelKeepKeys)
            val suspendKeys = localKeptKeys.intersect(navModelSuspendKeys)
            val noKeysChanges = newKeys.isEmpty() && removedKeys.isEmpty()
            val noSuspendChanges = keepKeys.isEmpty() && suspendKeys.isEmpty()
            if (noKeysChanges && noSuspendChanges) {
                return@update map
            }
            val mutableMap = map.toMutableMap()
            newKeys.forEach { key ->
                val shouldSuspend =
                    keepMode == ChildEntry.KeepMode.SUSPEND && navModelSuspendKeys.contains(key)
                mutableMap[key] =
                    childEntry(
                        key = key,
                        savedState = null,
                        suspended = shouldSuspend,
                    )
            }
            removedKeys.forEach { key ->
                mutableMap.remove(key)
            }
            keepKeys.forEach { key ->
                mutableMap[key] = requireNotNull(mutableMap[key]).initialize()
            }
            suspendKeys.forEach { key ->
                mutableMap[key] = requireNotNull(mutableMap[key]).suspend()
            }
            mutableMap
        }
    }

    @Suppress("ForbiddenComment")
    fun childOrCreate(routingKey: RoutingKey<Routing>): ChildEntry.Initialized<Routing> {
        // TODO: Should not allow child creation and throw exception instead to avoid desynchronisation
        val value = _children.value
        val child = value[routingKey] ?: error(
            "Rendering and children management is out of sync: requested $routingKey but have only ${value.keys}"
        )
        return when (child) {
            is ChildEntry.Initialized ->
                child
            is ChildEntry.Suspended ->
                _children.updateAndGet { map ->
                    val updateChild = map[routingKey]
                        ?: error("Requested child $routingKey disappeared")
                    when (updateChild) {
                        is ChildEntry.Initialized -> map
                        is ChildEntry.Suspended ->
                            map.plus(routingKey to updateChild.initialize())
                    }
                }[routingKey] as ChildEntry.Initialized
        }
    }

    private fun SavedStateMap?.restoreChildren(): ChildEntryMap<Routing>? =
        (this?.get(KEY_CHILDREN_STATE) as? Map<RoutingKey<Routing>, SavedStateMap>)?.mapValues {
            // Always restore in suspended mode, they will be unsuspended or destroyed on the first sync cycle
            childEntry(it.key, it.value, true)
        }

    fun saveChildrenState(writer: MutableSavedStateMap) {
        val children = _children.value
        if (children.isNotEmpty()) {
            val childrenState =
                children
                    .mapValues { (_, entry) ->
                        when (entry) {
                            is ChildEntry.Initialized -> entry.node.saveInstanceState(writer.saverScope)
                            is ChildEntry.Suspended -> entry.savedState
                        }
                    }
            if (childrenState.isNotEmpty()) {
                writer[KEY_CHILDREN_STATE] = childrenState
            }
        }
    }

    private fun childBuildContext(savedState: SavedStateMap?): BuildContext =
        BuildContext(
            ancestryInfo = AncestryInfo.Child(parentNode),
            savedStateMap = savedState,
            customisations = customisations.getSubDirectoryOrSelf(parentNode::class),
        )

    private fun childEntry(
        key: RoutingKey<Routing>,
        savedState: SavedStateMap?,
        suspended: Boolean,
    ): ChildEntry<Routing> =
        if (suspended) {
            ChildEntry.Suspended(key, savedState)
        } else {
            ChildEntry.Initialized(
                key = key,
                node = parentNode
                    .resolve(key.routing, childBuildContext(savedState))
                    .build()
            )
        }

    private fun ChildEntry<Routing>.initialize(): ChildEntry.Initialized<Routing> =
        when (this) {
            is ChildEntry.Initialized -> this
            is ChildEntry.Suspended ->
                ChildEntry.Initialized(
                    key = key,
                    node = parentNode.resolve(
                        routing = key.routing,
                        buildContext = childBuildContext(savedState),
                    ).build()
                )
        }

    @Suppress("ForbiddenComment")
    private fun ChildEntry<Routing>.suspend(): ChildEntry.Suspended<Routing> =
        when (this) {
            is ChildEntry.Suspended -> this
            is ChildEntry.Initialized ->
                ChildEntry.Suspended(
                    key = key,
                    // TODO: Not able to get a scope from Compose here, providing fake one
                    savedState = node.saveInstanceState { true },
                )
        }

    private companion object {
        const val KEY_CHILDREN_STATE = "ChildrenState"

        private fun <T, R : Any> Collection<T>.mapNotNullToSet(mapper: (T) -> R?): Set<R> =
            mapNotNullTo(HashSet(size, 1f), mapper)
    }

}
