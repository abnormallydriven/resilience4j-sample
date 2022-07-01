package com.bltucker.blog

import kotlinx.coroutines.delay

class DataRepository {

    suspend fun getExpensiveData(): String {
        delay(100)
        return "data"
    }

}