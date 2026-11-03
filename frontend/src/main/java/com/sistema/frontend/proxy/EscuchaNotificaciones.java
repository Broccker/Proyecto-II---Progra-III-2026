package com.sistema.frontend.proxy;

import com.sistema.common.protocolo.Notificacion;


public interface EscuchaNotificaciones {


    void alRecibir(Notificacion notificacion);


    default void alPerderConexion(Throwable causa) {

    }
}
