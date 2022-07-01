package com.bltucker.blog.plugins

import com.bltucker.blog.DataRepository
import io.github.resilience4j.kotlin.ratelimiter.RateLimiterConfig
import io.github.resilience4j.kotlin.ratelimiter.executeSuspendFunction
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import java.time.Duration

fun Application.configureRouting() {

    val config = RateLimiterConfig {
        timeoutDuration(Duration.ofSeconds(5))
        limitRefreshPeriod(Duration.ofMinutes(1))
        limitForPeriod(10)
    }

    val rateLimiterRegistry = RateLimiterRegistry.of(config)

    val importantResourceRateLimiter = rateLimiterRegistry.rateLimiter("importantResource")

    val repository = DataRepository()

    routing {
        get("/someImportantResource") {

            try{
                val data = importantResourceRateLimiter.executeSuspendFunction {
                    repository.getExpensiveData()
                }

                call.respond(mapOf("data" to data))
            } catch(ex: RequestNotPermitted){
                call.respond(HttpStatusCode.TooManyRequests)
            }
        }
    }
}
