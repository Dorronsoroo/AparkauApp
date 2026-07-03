package com.lksnext.ParkingJDorronsoro.model.service

interface AvisoSalidaService {
    /**
     * Crea un aviso ANÓNIMO de "salir" para el usuario que ocupa la plaza que
     * bloquea a [plazaBloqueadaId]. La app solo escribe su propio uid y su plaza;
     * es la Cloud Function quien resuelve (con permisos admin) a quién avisar,
     * sin exponer identidad, tokens ni teléfonos entre usuarios.
     */
    suspend fun solicitarSalida(usuarioId: String, plazaBloqueadaId: String)
}

