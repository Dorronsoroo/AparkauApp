package com.lksnext.ParkingJDorronsoro.model.service

import com.lksnext.ParkingJDorronsoro.model.Plaza

interface PlazaService {
    suspend fun getTodasLasPlazas(): List<Plaza>
    suspend fun getPlaza(plazaId: String): Plaza?
    suspend fun actualizarEstadoPlaza(plazaId: String, nuevoEstado: String)
}