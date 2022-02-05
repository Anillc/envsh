package cn.anillc.envsh.utils

import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class BuildQueue<T>(
    private val process: (t: T) -> Unit,
) {
    private val queue = LinkedList<T>()
    private var processing = AtomicBoolean(false)

    fun add(t: T) {
        queue.offer(t)
        if (!processing.get()) startProcessing()
    }

    private fun startProcessing() {
        processing.set(true)
        Thread {
            while (true) {
                val t = queue.poll() ?: break
                try {
                    process(t)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            processing.set(false)
        }.start()
    }
}