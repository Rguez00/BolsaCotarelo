package org.example.project.core.util

expect class ThreadSafeMap<K : Any, V : Any>() {
    fun get(key: K): V?
    fun put(key: K, value: V)
    fun values(): List<V>
}
