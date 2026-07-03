package com.lksnext.ParkingJDorronsoro.screen.reserva

import com.lksnext.ParkingJDorronsoro.model.GrupoPlaza
import com.lksnext.ParkingJDorronsoro.model.Plaza
import com.lksnext.ParkingJDorronsoro.model.Vehiculo
import com.lksnext.ParkingJDorronsoro.model.ZonaPlaza
import com.lksnext.ParkingJDorronsoro.model.agruparTandem
import java.time.LocalDate
import java.time.LocalTime

data class ReservaUiState(
    val plazas: List<Plaza> = emptyList(),
    val vehiculos: List<Vehiculo> = emptyList(),
    val matriculaSeleccionada: String = "",
    val fecha: LocalDate = LocalDate.now(),
    val horaInicio: LocalTime = LocalTime.of(8, 0),
    val horaFin: LocalTime = LocalTime.of(17, 0),
    val isLoading: Boolean = false,
    /** Mapa de plazaId -> nombre completo del ocupante (solo plazas OCUPADAS) */
    val ocupantePorPlaza: Map<String, String> = emptyMap()
) {
    val plazasOficina: List<Plaza>
        get() = plazas.filter { it.zonaEnum == ZonaPlaza.OFICINA }

    val plazasPago: List<Plaza>
        get() = plazas.filter { it.zonaEnum == ZonaPlaza.PAGO }

    val gruposOficina: List<GrupoPlaza>
        get() = plazasOficina.agruparTandem()

    val gruposPago: List<GrupoPlaza>
        get() = plazasPago.agruparTandem()
}
