package com.lksnext.ParkingJDorronsoro.model.service

/**
 * Encapsula la lógica de notificaciones push (FCM) reutilizable, para mantener
 * el [com.lksnext.ParkingJDorronsoro.model.service.impl.AparkauMessagingServiceImpl] lo más fino
 * posible. Sigue el patrón interfaz + impl del resto de servicios del proyecto.
 */
interface NotificacionService {

    /**
     * Muestra una notificación local en la barra de estado del dispositivo.
     * Se encarga de crear el canal y de comprobar el permiso en Android 13+.
     */
    fun mostrarNotificacion(titulo: String, cuerpo: String)

    /**
     * Registra el [token] de FCM para el usuario con sesión iniciada.
     * Si no hay usuario logueado, no hace nada.
     */
    suspend fun registrarToken(token: String)

    /**
     * Obtiene el token de FCM actual del dispositivo y lo registra para el
     * usuario con sesión iniciada. Pensado para llamarse tras el login/registro,
     * cuando el token probablemente ya existía antes de haber usuario.
     */
    suspend fun registrarTokenActual()

    /**
     * Obtiene el token de FCM actual y lo elimina del usuario con sesión
     * iniciada. Debe llamarse ANTES de cerrar sesión (mientras aún hay usuario).
     */
    suspend fun eliminarTokenActual()

    /**
     * Programa DOS notificaciones locales para una reserva:
     *  - 30 minutos antes de que COMIENCE (horaInicioMs)
     *  - 15 minutos antes de que FINALICE (horaFinMs)
     *
     * Usa AlarmManager (alarmas exactas): no requiere FCM ni Cloud Functions y
     * despierta el dispositivo a la hora justa aunque esté en modo Doze.
     * Si ya existían recordatorios para esta reserva (p.ej. tras editarla),
     * los reemplaza automáticamente.
     *
     * @param reservaId    ID de la reserva (clave única de las tareas).
     * @param horaInicioMs Marca de tiempo Unix (ms) de inicio de la reserva.
     * @param horaFinMs    Marca de tiempo Unix (ms) de fin de la reserva.
     * @param plazaId      Identificador de la plaza (p.ej. "36A") para el texto.
     */
    fun programarRecordatorios(
        reservaId: String,
        horaInicioMs: Long,
        horaFinMs: Long,
        plazaId: String
    )

    /**
     * Cancela los DOS recordatorios (inicio y fin) de la reserva indicada.
     * Llamar al eliminar o reasignar la reserva.
     */
    fun cancelarRecordatorios(reservaId: String)
}


