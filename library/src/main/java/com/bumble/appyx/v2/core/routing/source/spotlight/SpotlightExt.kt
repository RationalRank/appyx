package com.bumble.appyx.v2.core.routing.source.spotlight

import kotlinx.coroutines.flow.map

fun <T : Any> Spotlight<T>.hasNext() =
    elements.map { value -> value.lastIndex != elements.value.currentIndex }

fun <T : Any> Spotlight<T>.hasPrevious() =
    elements.map { value -> value.currentIndex != 0 }

fun <T : Any> Spotlight<T>.activeIndex() =
    elements.map { value -> value.currentIndex }

fun <T : Any> Spotlight<T>.elementsCount() =
    elements.value.size
