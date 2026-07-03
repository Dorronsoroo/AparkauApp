package com.lksnext.ParkingJDorronsoro.model

enum class PerfilUsuario {
    EMPLEADO_HABITUAL, NOMADA, VIP_CLIENTE, PRIORIDAD_ESPECIAL
}

enum class EstadoReserva {
    AGENDADA,  // El usuario ha reservado para el futuro, pero aún no ha llegado
    ACTIVA,    // El usuario ya ha hecho Check-in y su coche está físicamente ahí
    FINALIZADA,// El usuario ha hecho Check-out y se ha ido
    CANCELADA  // El usuario anuló la reserva antes de ir
}

enum class TipoPlaza {
    NORMAL, ELECTRICA, MOTO, PRIORITARIA, TANDEM
}

enum class EstadoPlaza {
    LIBRE, OCUPADA, BLOQUEADA_POR_TANDEM
}

enum class ZonaPlaza {
    OFICINA, PAGO
}


