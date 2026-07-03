package com.lksnext.ParkingJDorronsoro.model

import com.google.firebase.firestore.DocumentId

data class Plaza(
    @DocumentId val id: String = "",
    val tipo: String = "NORMAL",
    val estado: String = "LIBRE",
    val zona: String = "",
    val plazaBloqueadaId: String = ""
) {
    /** Convierte el String de Firestore al enum, tolerando espacios y valores desconocidos. */
    val tipoEnum: TipoPlaza
        get() = runCatching { TipoPlaza.valueOf(tipo.trim().uppercase()) }
            .getOrDefault(TipoPlaza.NORMAL)

    val estadoEnum: EstadoPlaza
        get() = runCatching { EstadoPlaza.valueOf(estado.trim().uppercase()) }
            .getOrDefault(EstadoPlaza.LIBRE)

    /**
     * Zona de la plaza. Si Firestore trae el campo "zona" se respeta;
     * en caso contrario se deduce a partir del número de plaza
     * (las del parking de pago son un conjunto conocido).
     */
    val zonaEnum: ZonaPlaza
        get() = runCatching { ZonaPlaza.valueOf(zona.trim().uppercase()) }
            .getOrElse {
                if (id.trim() in PLAZAS_PAGO) ZonaPlaza.PAGO else ZonaPlaza.OFICINA
            }

    companion object {
        /** Números de plaza que pertenecen al parking de pago. */
        private val PLAZAS_PAGO = setOf(
            "200", "201", "202", "203", "204", "62", "63", "47", "115"
        )
    }
}
