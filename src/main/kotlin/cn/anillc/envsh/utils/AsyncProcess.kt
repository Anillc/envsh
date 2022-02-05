package cn.anillc.envsh.utils

import com.zaxxer.nuprocess.NuProcess
import com.zaxxer.nuprocess.NuProcessBuilder
import com.zaxxer.nuprocess.NuProcessHandler
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.streams.ReadStream
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock

class AsyncProcess(
    vararg commands: String
) : ReadStream<Buffer>, NuProcessHandler {
    private val exitCode = AtomicInteger(-1)
    private val stderr = AtomicReference("")
    private val pausedLock = ReentrantLock()
    private var paused = true
    private val cache = mutableListOf<Byte>()

    private val processBuilder = NuProcessBuilder(*commands)
    private lateinit var process: NuProcess
    private var handler: Handler<Buffer>? = null
    private var exceptionHandler: Handler<Throwable>? = null
    private var endHandler: Handler<Void>? = null
    private val stderrByteList = mutableListOf<Byte>()

    init {
        processBuilder.setProcessListener(this)
    }

    fun env(name: String, value: String): AsyncProcess {
        processBuilder.environment()[name] = value
        return this
    }

    fun start(): AsyncProcess {
        process = processBuilder.start()
        return this
    }

    fun waitFor() {
        process.waitFor(0, TimeUnit.SECONDS)
    }

    fun getExitCode(): Int {
        return exitCode.get()
    }

    fun getStderr(): String {
        return stderr.get()
    }

    override fun handler(handler: Handler<Buffer>?): ReadStream<Buffer> {
        this.handler = handler
        return this
    }

    override fun exceptionHandler(handler: Handler<Throwable>?): ReadStream<Buffer> {
        this.exceptionHandler = handler
        return this
    }

    override fun pause(): ReadStream<Buffer> {
        pausedLock.lock()
        paused = true
        pausedLock.unlock()
        return this
    }

    // resume after start if you want to get input stream
    override fun resume(): ReadStream<Buffer> {
        pausedLock.lock()
        handler?.handle(Buffer.buffer(cache.toByteArray()))
        cache.clear()
        paused = false
        pausedLock.unlock()
        return this
    }

    override fun fetch(amount: Long): ReadStream<Buffer> = this

    override fun endHandler(endHandler: Handler<Void>?): ReadStream<Buffer> {
        this.endHandler = endHandler
        return this
    }

    override fun onPreStart(nuProcess: NuProcess?) {}
    override fun onStart(nuProcess: NuProcess?) {}
    override fun onExit(exitCode: Int) {
        this.exitCode.set(exitCode)
        stderr.set(String(stderrByteList.toByteArray()))
        endHandler?.handle(null)
    }

    override fun onStdout(buffer: ByteBuffer?, closed: Boolean) {
        if (!closed && buffer != null) {
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            println(String(bytes))
            pausedLock.lock()
            if (paused) {
                cache.addAll(bytes.toTypedArray())
            } else {
                handler?.handle(Buffer.buffer(bytes))
            }
            pausedLock.unlock()
        }
    }

    override fun onStderr(buffer: ByteBuffer?, closed: Boolean) {
        if (!closed && buffer != null) {
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            stderrByteList.addAll(bytes.toList())
        }
    }

    override fun onStdinReady(buffer: ByteBuffer?): Boolean = true
}