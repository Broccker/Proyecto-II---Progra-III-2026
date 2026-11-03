package com.sistema.backend.server;

import java.io.IOException;


public interface ClienteConectado {


    String id();


    void enviar(Object mensaje) throws IOException;


    void cerrar();


    boolean estaCerrado();
}
