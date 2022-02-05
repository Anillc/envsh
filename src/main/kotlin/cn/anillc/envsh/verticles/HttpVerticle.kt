package cn.anillc.envsh.verticles

import cn.anillc.envsh.utils.AsyncProcess
import io.vertx.core.eventbus.EventBus
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

class Result(
    val code: Int,
    val message: String,
    val data: Any? = null,
)

class HttpVerticle(
    private val port: Int
) : CoroutineVerticle() {
    private lateinit var eventBus: EventBus
    override suspend fun start() {
        eventBus = vertx.eventBus()
        val router = Router.router(vertx)
        router.route().handler {
            it.response().putHeader("content-type", "application/json")
            it.next()
        }
        router.get("/build").handler(this::query)
        router.post("/build").handler(this::add)
        router.getWithRegex("\\/build(?<packages>(\\/[^\\/]+)+)").handler {
            val packages = it.pathParam("packages")!!.split("/")
                .joinToString(",").trimStart(',')
            it.queryParams().set("packages", packages)
            it.reroute(HttpMethod.POST, "/build")
        }
        router.get("/envsh").handler(this::envsh)
        router.route().handler {
            val res = it.response()
            res.statusCode = 404
            res.end(Json.encode(Result(-1, "Not found")))
        }
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(port)
            .await()
        println("http server started")
    }

    private fun query(ctx: RoutingContext) {
        val idParam = ctx.queryParam("id")
        if (idParam.size == 0) {
            ctx.end(Json.encode(Result(-1, "missing params")))
            return
        }
        eventBus.request<Build>("builder-query", idParam[0]) {
            val buildResult = it.result().body()
            if (buildResult == null) {
                ctx.end(Json.encode(Result(-1, "build not found")))
                return@request
            }
            ctx.end(Json.encode(Result(1, "", buildResult)))
        }
    }

    private fun add(ctx: RoutingContext) {
        val packagesParam = ctx.queryParam("packages")
        if (packagesParam.size == 0) {
            ctx.end(Json.encode(Result(-1, "missing params")))
            return
        }
        eventBus.request<Build>("builder-add", packagesParam[0]) {
            val build = it.result().body()
            ctx.end(Json.encode(build))
        }
    }

    private fun envsh(ctx: RoutingContext) {
        val idParam = ctx.queryParam("id")
        if (idParam.size == 0) {
            ctx.end(Json.encode(Result(-1, "missing params")))
            return
        }
        eventBus.request<AsyncProcess>("builder-envsh", idParam[0]) {
            val process = it.result().body()
            if (process == null) {
                ctx.end(Json.encode(Result(-1, "the build is not available", null)))
                return@request
            }
            ctx.response().isChunked = true
            ctx.response().putHeader("content-type", "application/octet-stream")
            process.pipeTo(ctx.response()) { pipeResult ->
                if (pipeResult.failed()) pipeResult.cause().printStackTrace()
            }
        }
    }
}