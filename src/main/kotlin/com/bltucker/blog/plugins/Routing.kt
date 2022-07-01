package com.bltucker.blog.plugins

import com.bltucker.blog.DataRepository
import com.bltucker.blog.ServiceBWrapper
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.kotlin.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
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

    val rateLimiterConfig = RateLimiterConfig {
        timeoutDuration(Duration.ofSeconds(5))
        limitRefreshPeriod(Duration.ofMinutes(1))
        limitForPeriod(10)
    }

    val rateLimiterRegistry = RateLimiterRegistry.of(rateLimiterConfig)

    val importantResourceRateLimiter = rateLimiterRegistry.rateLimiter("importantResource")

    val repository = DataRepository()

    val circuitBreakerConfig = CircuitBreakerConfig {
        failureRateThreshold(25F)
        permittedNumberOfCallsInHalfOpenState(5)
        slidingWindowType(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
        slidingWindowSize(5)
        minimumNumberOfCalls(5)
    }

    val circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig)

    val serviceBCircuitBreaker = circuitBreakerRegistry.circuitBreaker("ServiceB")

    val serviceBWrapper = ServiceBWrapper()


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

        get("/someUnreliableResource"){
            try{
                val data = serviceBCircuitBreaker.executeSuspendFunction {
                    serviceBWrapper.unreliableRemoteCall()
                }

                call.respond(mapOf("data" to data))
            } catch(ex: CallNotPermittedException){
                call.respond(HttpStatusCode.ServiceUnavailable)
            }
        }
    }
}
