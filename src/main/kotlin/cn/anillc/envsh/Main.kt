package cn.anillc.envsh

import cn.anillc.envsh.verticles.MainVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx

fun main() {
    Vertx.vertx().deployVerticle(MainVerticle::class.java, DeploymentOptions())
}
