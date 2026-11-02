# Reporte de cobertura de pruebas — Fase 12

Convención del proyecto:

* `*Test`: prueba unitaria, la ejecuta **Surefire** con `mvn test`.
* `*IT`: prueba de integración, la ejecuta **Failsafe** con `mvn verify`.

```bash
mvn test       # 271 pruebas unitarias; no necesita MySQL ni backend
mvn verify     # 271 unitarias + 75 de integración
```

Las pruebas de interfaz se ejecutan con `-Djava.awt.headless=true`, de modo que
no abren ventanas reales. Las de integración con MySQL recrean
`proyectoII_reservas_test` desde `sql/schema.sql`; si MySQL no está activo se
omiten con `Assumptions` y no rompen la compilación.

## Resumen por módulo

| Módulo | Unitarias (`*Test`) | Integración (`*IT`) | Total |
|---|---:|---:|---:|
| `common` | 31 | — | 31 |
| `backend` | 101 | 56 | 157 |
| `frontend` | 139 | 19 (1 omitida sin backend externo) | 158 |
| **Total** | **271** | **75** | **346** |

Resultado de `mvn clean verify` con MySQL activo: **BUILD SUCCESS**, 0 fallos,
0 errores y 1 prueba omitida (`ProxyBackendRealIT`, que requiere un backend
externo escuchando).

## Cobertura por criterio de la rúbrica

| Funcionalidad / criterio | Pruebas principales | Qué se comprueba |
|---|---|---|
| Estructura Maven y separación de capas | `DependenciasFrontendTest`, `OperacionTest` | El frontend no carga JDBC, HikariCP ni clases del backend; cada método de `Service` tiene una operación de protocolo. |
| Base de datos y esquema | `CategoriaRecursoDAOIT`, `RecursoDAOIT`, `ReservaDAOIT`, `UsuarioDAOIT`, `EsquemaPruebasTest` | CRUD real contra MySQL de prueba, claves foráneas, restricciones, transacciones y consultas agregadas. |
| Login y cambio de clave con hash | `ServiceImplSesionTest`, `ControladorLoginTest`, `ControladorCambiarClaveTest`, `ProxyBackendRealIT` | BCrypt, mensajes que no revelan si el usuario existe, validación local y flujo real por sockets. |
| Funcionarios | `ServiceImplFuncionariosTest`, `UsuarioDAOIT`, `ControladorFuncionariosTest` | Alta con clave inicial, duplicados, nombre/teléfono, modificación, baja con reservas y MVC. |
| Categorías | `ServiceImplCategoriasTest`, `CategoriaRecursoDAOIT`, `ControladorCategoriasTest` | Identificador automático, descripción única, cambio de id y baja condicionada por recursos. |
| Recursos | `ServiceImplRecursosTest`, `RecursoDAOIT`, `ControladorRecursosTest` | Pertenencia a categoría, duplicados, cambio de id, filtros y baja condicionada por reservas. |
| Reservas | `ServiceImplReservasTest`, `ReservaDAOIT`, `ControladorReservasTest`, `ServidorReservasIT` | Selección de recurso libre, conflictos de horario, fecha pasada, cancelación solo del dueño y flujo completo por sockets. |
| Calendarización | `ServiceImplMatricesEstadisticasTest`, `ControladorCalendarizacionTest` | Matriz por categoría y fecha, horas ocupadas y validación de categoría/fecha. |
| Actividades semanales | `ServiceImplMatricesEstadisticasTest`, `ControladorActividadesTest` | Matriz de 7 días desde el lunes, horas y celdas ocupadas. |
| Estadísticas con tabla y gráfico | `ServiceImplMatricesEstadisticasTest`, `ReservaDAOIT`, `ControladorEstadisticasTest`, `VistaEstadisticasTest`, `EstadisticasIT`, `GeneradorPDFTest` | `COUNT`/`GROUP BY` en SQL, rangos válidos, tabla y serie del gráfico, PDF no vacío. |
| Notificación asincrónica a los demás | `RegistroClientesTest`, `ServidorIT`, `ServidorReservasIT`, `AlertasIT` | El creador recibe su respuesta y **no** su alerta; los demás clientes sí; la difusión sale del hilo del manejador. |
| Área de alertas, Ver y Descartar | `AlertaTest`, `ModeloAlertasTest`, `ControladorAlertasTest`, `PanelAlertasTest`, `AlertasIT` | Deduplicación por reserva, detalle modal, descarte local sin tráfico y estado de desconexión. |
| Patrón Proxy y protocolo | `ServiceProxyIT`, `SolicitudTest`, `RespuestaTest`, `NotificacionTest`, `ProtocoloTest`, `SerializacionModelosTest`, `TraductorErroresTest` | Correlación de respuestas, mensajes completos, límites de bytes, serialización y traducción de errores. |
| Servidor multi-hilo | `ServidorTest`, `ServidorIT`, `RegistroClientesTest`, `DespachadorTest`, `EjecutorSwingTest` | Un manejador por cliente, arranque/apagado, despacho por operación y trabajo fuera del EDT. |
| Validación y errores | `ValidacionesTest` (backend y frontend), pruebas de controladores, `ErroresTest`, `RespuestaTest` | Campos vacíos, longitudes, formatos, rangos, duplicados y mensajes sin *stack traces*. |
| Concurrencia de reservas | `ServiceImplIT.dosPeticionesSimultaneasNoDuplicanLaReserva` | Dos instancias simultáneas sobre el mismo recurso: una sola reserva y un solo detalle. |
| Caída y reconexión del backend | `ServiceProxyIT`, `ControladorConexionTest`, `ControladorAlertasTest` | Corte del socket, peticiones pendientes falladas, reintentos limitados y cierre ordenado. |
| Configuración | `ConfiguracionBackendTest`, `ConfiguracionFrontendTest` | Valores por defecto, archivos `.properties`, argumentos y validación de host/puerto. |
| MVC estricto | Pruebas de los diez controladores, `FlujoMVCIT` | Vista → controlador → modelo → servicio simulado; flujo completo con proxy y servidor de prueba. |

## Escenario multi-cliente

`ServidorIT` abre dos clientes reales por socket contra el `Servidor` y un
`Service` simulado. Comprueba que la respuesta vuelve al creador con el mismo
`id` de correlación y que solo el otro cliente recibe la `Notificacion`.
`ServidorReservasIT` repite el escenario con `ServiceImpl` real y MySQL:
la reserva queda guardada y la alerta llega únicamente al segundo cliente.
`AlertasIT` hace lo mismo en el frontend con dos `ServiceProxy` reales.

## Requisitos externos y pruebas omitidas

* `ProxyBackendRealIT` necesita un backend escuchando en `127.0.0.1:5000` y los
  datos de `sql/datos_iniciales.sql`. Sin él se marca como omitida.
* Las `*IT` del backend necesitan MySQL en `127.0.0.1:3306`. Para levantarlo en
  este equipo:

  ```bat
  C:\Users\Broccker\mysql-8.4.13-winx64\iniciar.bat
  ```

* `VentanaPrincipal` es un `JFrame` y no se instancia en modo *headless*; su
  lógica de sesión y pestañas se cubre desde los controladores y las vistas
  internas.

## Cómo ejecutar una prueba concreta

```bash
# Una unitaria
mvn -pl backend -am -Dtest=ServiceImplReservasTest test

# Una de integración
mvn -pl frontend -am -Dit.test=AlertasIT -Dfailsafe.failIfNoSpecifiedTests=false verify
```
