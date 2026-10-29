package com.sistema.frontend.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.sistema.common.modelo.Administrador;
import com.sistema.common.modelo.Usuario;
import com.sistema.frontend.config.ConfiguracionFrontend;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Prueba de aceptación de la Fase 7 contra el backend real.
 *
 * <p>Se omite si no hay un backend escuchando en el destino configurado, para
 * que la compilación de un compañero que solo trabaja en el frontend no
 * dependa de MySQL. Para ejecutarla:</p>
 *
 * <pre>
 * 1. Inicie MySQL y cargue sql/schema.sql y sql/datos_iniciales.sql.
 * 2. Arranque el backend: mvn -pl backend -am exec:java
 * 3. Ejecute: mvn -pl frontend -am -Dit.test=ProxyBackendRealIT verify
 * </pre>
 *
 * <p>Usa el usuario administrador de los datos iniciales (111/111).</p>
 */
@Timeout(30)
class ProxyBackendRealIT {

    private static final String ID_ADMINISTRADOR = "111";
    private static final String CLAVE_ADMINISTRADOR = "111";

    @Test
    @DisplayName("El frontend inicia sesión contra el backend real por sockets")
    void iniciaSesionContraElBackendReal() {
        ConfiguracionFrontend configuracion = ConfiguracionFrontend.cargar();

        ServiceProxy proxy;
        try {
            proxy = new ServiceProxy(configuracion);
        } catch (ExcepcionRed e) {
            Assumptions.assumeTrue(false,
                    "No hay backend en " + configuracion.host() + ":"
                            + configuracion.puerto() + "; se omite la prueba de aceptación.");
            return;
        }

        try (proxy) {
            Usuario usuario = proxy.iniciarSesion(ID_ADMINISTRADOR, CLAVE_ADMINISTRADOR);

            assertEquals(ID_ADMINISTRADOR, usuario.getId());
            Administrador administrador = assertInstanceOf(Administrador.class, usuario);
            assertEquals(ID_ADMINISTRADOR, administrador.getId());
        }
    }
}
