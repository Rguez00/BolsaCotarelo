package org.example.project.core.util

import java.util.concurrent.ConcurrentHashMap

actual class ThreadSafeMap<K : Any, V : Any> actual constructor() {
    private val map = ConcurrentHashMap<K, V>()

    actual fun get(key: K): V? = map[key]

    actual fun put(key: K, value: V) {
        map[key] = value
    }

    actual fun values(): List<V> = map.values.toList()
}
