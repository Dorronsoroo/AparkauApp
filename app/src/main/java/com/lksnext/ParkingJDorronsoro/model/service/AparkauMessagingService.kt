package com.lksnext.ParkingJDorronsoro.model.service

import com.google.firebase.messaging.RemoteMessage

/**
 * Contrato del receptor de Firebase Cloud Messaging de la app. Sigue el patrón
 * interfaz + impl del resto de servicios del proyecto.
 *
 * La implementación concreta
 * ([com.lksnext.ParkingJDorronsoro.model.service.impl.AparkauMessagingServiceImpl])
 * hereda además de `FirebaseMessagingService`, ya que el sistema Android exige
 * una clase concreta registrada en el manifest para recibir los eventos FCM.
 */
interface AparkauMessagingService {

    /**
     * Se invoca cuando FCM genera un token nuevo o lo renueva. La implementación
     * debe persistirlo (solo si hay usuario con sesión iniciada).
     */
    fun onNewToken(token: String)

    /**
     * Se invoca al recibir un mensaje con la app en primer plano o cuando el
     * mensaje contiene un payload de tipo "data".
     */
    fun onMessageReceived(message: RemoteMessage)
}

