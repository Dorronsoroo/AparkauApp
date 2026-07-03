package com.lksnext.ParkingJDorronsoro.model

/**
 * Representa una plaza individual o un par de plazas tándem agrupadas para mostrar en la UI.
 */
sealed class GrupoPlaza {
    data class Single(val plaza: Plaza) : GrupoPlaza()
    data class Tandem(val plazaA: Plaza, val plazaB: Plaza) : GrupoPlaza()
}

/**
 * Agrupa las plazas de tipo TANDEM por pares (usando [Plaza.plazaBloqueadaId]) y devuelve
 * una lista de [GrupoPlaza] ordenada de la misma manera que la lista de entrada.
 * Las plazas que no son tándem se devuelven como [GrupoPlaza.Single].
 */
fun List<Plaza>.agruparTandem(): List<GrupoPlaza> {
    val procesadas = mutableSetOf<String>()
    val resultado = mutableListOf<GrupoPlaza>()
    val porId = associateBy { it.id }

    for (plaza in this) {
        if (plaza.id in procesadas) continue

        if (plaza.tipoEnum == TipoPlaza.TANDEM && plaza.plazaBloqueadaId.isNotBlank()) {
            val pareja = porId[plaza.plazaBloqueadaId]
            if (pareja != null) {
                resultado.add(GrupoPlaza.Tandem(plaza, pareja))
                procesadas.add(plaza.id)
                procesadas.add(pareja.id)
                continue
            }
        }

        resultado.add(GrupoPlaza.Single(plaza))
        procesadas.add(plaza.id)
    }

    return resultado
}


