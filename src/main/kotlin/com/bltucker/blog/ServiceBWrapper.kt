package com.bltucker.blog

import kotlinx.coroutines.delay
import kotlin.random.Random

class ServiceBWrapper {


    suspend fun unreliableRemoteCall() : String{
        delay(100)
        return if(Random.nextInt(100) < 33){
            throw Exception("Call Failed")
        } else {
            "data"
        }
    }

    suspend fun slowCall(): String{
        delay(8000)
        return "slow data"
    }
}