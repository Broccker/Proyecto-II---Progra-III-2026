package com.sistema.common.protocolo;

import java.io.Serializable;


public sealed interface CargaNotificacion extends Serializable
        permits CargaNuevaReserva {
}
