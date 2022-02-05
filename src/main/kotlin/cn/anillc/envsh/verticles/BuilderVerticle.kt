package cn.anillc.envsh.verticles

import cn.anillc.envsh.utils.AsyncProcess
import cn.anillc.envsh.utils.BuildQueue
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageCodec
import io.vertx.kotlin.coroutines.CoroutineVerticle
import java.util.*

enum class Status { SUCCESSFUL, FAILED, PROCESSING, QUEUING }
class Build(
    val id: String,
    val packages: String,
    val startTime: Date,
    var finishTime: Date?,
    var status: Status,
    var stderr: String,
)

class TransformCodec<T> : MessageCodec<T, T> {
    override fun encodeToWire(buffer: Buffer?, s: T) = throw Exception("message should only transfer locally")
    override fun decodeFromWire(pos: Int, buffer: Buffer?): T = throw Exception("message should only transfer locally")
    override fun transform(s: T): T = s
    override fun name(): String = UUID.randomUUID().toString()
    override fun systemCodecID(): Byte = -1
}

class BuilderVerticle : CoroutineVerticle() {
    private val map = HashMap<String, Build>()
    private val queue = BuildQueue(this::processBuild)
    override suspend fun start() {
        val eventBus = vertx.eventBus()
        eventBus.registerDefaultCodec(Array<String>::class.java, TransformCodec())
        eventBus.registerDefaultCodec(Build::class.java, TransformCodec())
        eventBus.registerDefaultCodec(AsyncProcess::class.java, TransformCodec())
        eventBus.consumer<String>("builder-add").handler(this::add)
        eventBus.consumer<String>("builder-query").handler(this::query)
        eventBus.consumer<String>("builder-envsh").handler(this::envsh)
        vertx.setPeriodic(60 * 1000, this::clean)
    }

    private fun add(message: Message<String>) {
        val packages = message.body()
        val id = UUID.randomUUID().toString()
        val build = Build(id, packages, Date(), null, Status.QUEUING, "")
        map[id] = build
        queue.add(build)
        message.reply(build)
    }

    private fun query(message: Message<String>) {
        message.reply(map[message.body()])
    }

    private fun envsh(message: Message<String>) {
        val build = map[message.body()]
        if (build == null || build.status != Status.SUCCESSFUL) {
            message.reply(null)
            return
        }
        val process = AsyncProcess("bash", "-c", "nix run .#envsh --impure > /dev/stdout")
//        val process = AsyncProcess("sudo", "ping", "-c", "100", "127.0.0.1")
            .env("PACKAGES", build.packages)
            .start()
        message.reply(process)
    }

    private fun clean(id: Long) {
        val date = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        for ((key, value) in map) {
            if (value.finishTime != null && date > value.finishTime) {
                map.remove(key)
            }
        }
    }

    private fun processBuild(build: Build) {
        build.status = Status.PROCESSING
        val process = AsyncProcess("bash", "-c", "echo 123 > /dev/stdout")
//        val process = AsyncProcess("sudo", "ping", "-c", "1", "127.0.0.1")
            .env("PACKAGES", build.packages)
            .start()
        process.waitFor()
        var stderr = ""
        var status = Status.SUCCESSFUL
        if (process.getExitCode() != 0) {
            stderr = process.getStderr()
            status = Status.FAILED
        }
        build.finishTime = Date()
        build.stderr = stderr
        build.status = status
    }
}