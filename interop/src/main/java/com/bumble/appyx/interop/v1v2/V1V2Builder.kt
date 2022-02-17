package com.bumble.appyx.interop.v1v2

import com.badoo.ribs.builder.SimpleBuilder
import com.badoo.ribs.core.modality.BuildParams
import com.bumble.appyx.interop.v1v2.V1V2Node.Companion.V1V2NodeKey
import com.bumble.appyx.v2.core.integration.NodeFactory
import com.bumble.appyx.v2.core.modality.BuildContext
import com.bumble.appyx.v2.core.node.Node
import com.bumble.appyx.v2.core.node.build

class V1V2Builder<N : Node>(private val nodeFactory: NodeFactory<N>) :
    SimpleBuilder<V1V2Node>() {
    override fun build(buildParams: BuildParams<Nothing?>): V1V2Node {
        val bundle = buildParams.savedInstanceState?.getBundle(V1V2NodeKey)
        val stateMap = bundle?.let {
            val keys = bundle.keySet()
            val map = mutableMapOf<String, Any?>()
            keys.forEach {
                map[it] = bundle[it]
            }
            map
        }

        val node = nodeFactory.create(BuildContext.root(stateMap)).build()

        return V1V2Node(buildParams, v2Node = node)
    }
}
