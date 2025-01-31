package com.bumble.appyx.app.node.onboarding.screen

import android.os.Parcelable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.ExperimentalUnitApi
import com.bumble.appyx.app.composable.Page
import com.bumble.appyx.app.node.onboarding.screen.NavModelTeaserNode.Routing
import com.bumble.appyx.app.node.onboarding.screen.NavModelTeaserNode.Routing.BackStackTeaser
import com.bumble.appyx.app.node.onboarding.screen.NavModelTeaserNode.Routing.RandomOtherTeaser
import com.bumble.appyx.app.node.teaser.backstack.BackstackTeaserNode
import com.bumble.appyx.app.ui.AppyxSampleAppTheme
import com.bumble.appyx.core.composable.Children
import com.bumble.appyx.core.integration.NodeHost
import com.bumble.appyx.core.integrationpoint.IntegrationPointStub
import com.bumble.appyx.core.modality.BuildContext
import com.bumble.appyx.core.modality.BuildContext.Companion.root
import com.bumble.appyx.core.node.Node
import com.bumble.appyx.core.node.ParentNode
import com.bumble.appyx.navmodel.backstack.BackStack
import com.bumble.appyx.navmodel.backstack.activeRouting
import com.bumble.appyx.navmodel.backstack.operation.replace
import com.bumble.appyx.navmodel.backstack.transitionhandler.rememberBackstackFader
import com.bumble.appyx.app.node.teaser.promoter.PromoterTeaserNode
import kotlinx.parcelize.Parcelize

@ExperimentalUnitApi
class NavModelTeaserNode(
    buildContext: BuildContext,
    private val backStack: BackStack<Routing> = BackStack(
        initialElement = BackStackTeaser,
        savedStateMap = buildContext.savedStateMap
    ),
) : ParentNode<Routing>(
    buildContext = buildContext,
    navModel = backStack
) {

    sealed class Routing : Parcelable {
        @Parcelize
        object BackStackTeaser : Routing()

        @Parcelize
        object RandomOtherTeaser : Routing()
    }

    override fun resolve(routing: Routing, buildContext: BuildContext): Node =
        when (routing) {
            is BackStackTeaser -> BackstackTeaserNode(buildContext)
            is RandomOtherTeaser -> PromoterTeaserNode(buildContext)
        }

    override fun onChildFinished(child: Node) {
        switchToNextExample()
    }

    private fun switchToNextExample() {
        val next = when (backStack.activeRouting) {
            is BackStackTeaser -> RandomOtherTeaser
            is RandomOtherTeaser -> BackStackTeaser
            null -> null
        }
        if (next != null) backStack.replace(next)
    }

    @Composable
    override fun View(modifier: Modifier) {
        Page(
            modifier = modifier,
            title = "NavModel",
            body = "From simple switches to flows to back stacks to complex UI interactions, " +
                "NavModels are a powerful concept to drive your application tree with."
        ) {
            Children(
                modifier = Modifier.fillMaxSize(),
                navModel = backStack,
                transitionHandler = rememberBackstackFader { tween(1000) }
            )
        }
    }
}

@Preview
@Composable
@ExperimentalUnitApi
fun NavModelTeaserPreview() {
    AppyxSampleAppTheme(darkTheme = false) {
        PreviewContent()
    }
}

@Preview
@Composable
@ExperimentalUnitApi
fun NavModelTeaserPreviewDark() {
    AppyxSampleAppTheme(darkTheme = true) {
        PreviewContent()
    }
}

@Composable
@ExperimentalUnitApi
private fun PreviewContent() {
    Surface(color = MaterialTheme.colors.background) {
        Box(Modifier.fillMaxSize()) {
            NodeHost(integrationPoint = IntegrationPointStub()) {
                NavModelTeaserNode(
                    root(null)
                )
            }
        }
    }
}
