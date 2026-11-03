package com.sistema.frontend.proxy;


public interface FuenteNotificaciones {


    void agregarEscucha(EscuchaNotificaciones escucha);


    void quitarEscucha(EscuchaNotificaciones escucha);


    boolean estaConectado();
}
