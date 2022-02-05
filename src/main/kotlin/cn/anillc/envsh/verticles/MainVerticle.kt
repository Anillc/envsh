package cn.anillc.envsh.verticles

import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

class MainVerticle : CoroutineVerticle() {
    override suspend fun start() {
        vertx.deployVerticle(BuilderVerticle()).await()
        vertx.deployVerticle(HttpVerticle(2333)).await()
        println("envsh service started.")
    }
}