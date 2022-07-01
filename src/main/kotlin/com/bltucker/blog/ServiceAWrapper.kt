package com.bltucker.blog

import kotlinx.coroutines.delay

class ServiceAWrapper {

    suspend fun remoteCall() : String{
        delay(100)

        return "data"
    }
}