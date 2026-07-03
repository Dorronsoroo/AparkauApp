package com.lksnext.ParkingJDorronsoro.model.service

interface LogService {
    fun logNonFatalCrash(throwable: Throwable)
}

