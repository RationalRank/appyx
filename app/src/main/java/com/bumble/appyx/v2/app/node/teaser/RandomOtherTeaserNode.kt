package com.bumble.appyx.v2.app.node.teaser

import android.os.Parcelable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import androidx.lifecycle.coroutineScope
import com.bumble.appyx.v2.app.node.child.GenericChildNode
import com.bumble.appyx.v2.app.node.teaser.RandomOtherTeaserNode.Routing
import com.bumble.appyx.v2.app.node.teaser.routingsource.Promoter
import com.bumble.appyx.v2.app.node.teaser.routingsource.operation.addFirst
import com.bumble.appyx.v2.app.node.teaser.routingsource.operation.promoteAll
import com.bumble.appyx.v2.app.node.teaser.routingsource.transitionhandler.rememberPromoterTransitionHandler
import com.bumble.appyx.v2.core.composable.Children
import com.bumble.appyx.v2.core.modality.BuildContext
import com.bumble.appyx.v2.core.node.Node
import com.bumble.appyx.v2.core.node.ParentNode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlin.random.Random

@ExperimentalUnitApi
class RandomOtherTeaserNode(
    buildContext: BuildContext,
    private val promoter: Promoter<Routing> = Promoter(),
) : ParentNode<Routing>(
    buildContext = buildContext,
    routingSource = promoter
) {

    init {
        lifecycle.coroutineScope.launch {
            repeat(4) {
                promoter.addFirst(Routing.Child((it + 1) * 100))
                promoter.promoteAll()
            }
            delay(500)
            repeat(4) {
                delay(1500)
                promoter.addFirst(Routing.Child((it + 5) * 100))
                promoter.promoteAll()
            }
            finish()
        }
    }

    sealed class Routing : Parcelable {
        @Parcelize
        data class Child(val int: Int = Random.nextInt(1000)) : Routing()
    }

    override fun resolve(routing: Routing, buildContext: BuildContext): Node =
        when (routing) {
            is Routing.Child -> GenericChildNode(buildContext, routing.int)
        }

    @Composable
    override fun View(modifier: Modifier) {
        Children(
            modifier = Modifier.fillMaxSize(),
            routingSource = promoter,
            transitionHandler = rememberPromoterTransitionHandler { spring(stiffness = Spring.StiffnessVeryLow / 4) }
        ) {
            children<Routing> { child ->
                child(Modifier.size(100.dp))
            }
        }
    }
}

