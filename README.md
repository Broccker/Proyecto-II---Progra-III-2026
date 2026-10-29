# Sistema de Reservas de Recursos — Proyecto #2

Nueva versión **distribuida** del Sistema de Reservas de Recursos del Proyecto #1,
para el curso **EIF206 — Programación 3 (UNA, 2026-II)**.

Arquitectura: **Frontend (Swing + MVC) ↔ sockets ↔ Backend (Lógica + Datos) → MySQL**.

## Integrantes

* Brandon Bejarano Moya — 208280313
* Daniel Sequeira Vindas — 402720269

## Estructura del proyecto

```
eif206-proyecto2/                 (POM padre, packaging=pom)
├── common/                       Modelo, interfaz Service y protocolo de red
│   └── src/main/java/com/sistema/common/
│       ├── modelo/               Entidades serializables del dominio
│       ├── dto/                  Matrices, estadísticas y datos de IA
│       ├── excepciones/          Errores de dominio
│       ├── servicio/             Interfaz Service
│       └── protocolo/            Solicitud, Respuesta, Notificación y utilidades
├── backend/                      Servidor sin interfaz gráfica
│   └── src/main/java/com/sistema/backend/
│       ├── config/               Lectura de backend.properties
│       ├── data/                 DAOs JDBC + pool de conexiones (HikariCP)
│       ├── logic/                ServiceImpl (reglas de negocio) e integración con IA
│       └── server/               Servidor, ManejadorCliente y RegistroClientes
├── frontend/                     Cliente Swing (MVC)
│   └── src/main/java/com/sistema/frontend/
│       ├── config/               Lectura de frontend.properties
│       ├── controller/           Orquestación, ejecución asíncrona y errores
│       ├── model/                Estado de cada módulo, alertas y sesión activa
│       ├── proxy/                ServiceProxy y transporte por sockets
│       ├── util/                 Iconos, PDF y reportes
│       └── view/                 Ventana principal, vistas, panel de alertas y componentes
├── sql/                          schema.sql y datos_iniciales.sql
├── .run/                         Run Configurations compartidas de IntelliJ
└── pom.xml
```

### Reglas de dependencias

| Módulo | Puede depender de | Prohibido |
|---|---|---|
| `common` | nada | JDBC, Swing, backend, frontend |
| `backend` | `common`, MySQL, HikariCP, BCrypt | Swing, frontend |
| `frontend` | `common`, JFreeChart, OpenPDF | JDBC, driver MySQL, clases de `backend` |

## Requisitos

* **JDK 26** (el proyecto compila con `release 26`).
* **Maven 3.9+** (o el Maven incluido en IntelliJ IDEA).
* **MySQL 8.4 LTS** (el esquema usa `utf8mb4_0900_ai_ci` y restricciones `CHECK`).

## Compilar y probar

```bash
mvn clean verify     # compila + tests unitarios (*Test) + tests de integración (*IT)
mvn test             # solo tests unitarios (Surefire)
mvn -DskipTests package
```

Los tests unitarios se llaman `*Test` y los de integración `*IT`
(Surefire y Failsafe respectivamente).

Las pruebas de integración crean la base **`proyectoII_reservas_test`** a partir
del mismo `sql/schema.sql` (el POM del backend lo copia a los recursos de prueba),
así que siempre usan el esquema real sin tocar la base de desarrollo. Requieren
MySQL activo; si no está disponible, se **omiten** con un mensaje en el reporte
en lugar de romper la compilación de quien solo trabaja en el frontend.

Para ejecutar solo las pruebas de integración del backend:

```bash
mvn -pl backend -am verify
```

## Ejecutar en IntelliJ IDEA

1. `File → Open` y seleccionar el `pom.xml` de la raíz; importar los tres módulos.
2. Usar las configuraciones compartidas **Backend** y **Frontend** del menú de ejecución
   (están versionadas en `.run/`).
3. Alternativa por consola:

```bash
mvn -pl backend -am exec:java
mvn -pl frontend -am exec:java
```

También se generan JAR ejecutables con `mvn package`:

```bash
java -jar backend/target/sistema-backend-2.0.0-SNAPSHOT.jar
java -jar frontend/target/sistema-frontend-2.0.0-SNAPSHOT.jar
```

## Configuración y credenciales

* `backend/src/main/resources/backend.properties` — URL, usuario y clave de MySQL,
  puerto del servidor, tamaño del pool y datos del servicio de IA.
* `frontend/src/main/resources/frontend.properties` — host, puerto del backend y
  tiempo de espera del proxy. Es **opcional**: sin archivo usa `127.0.0.1:5000`
  y 15 s. Nunca contiene credenciales de base de datos ni de IA.
* Ambos archivos están en `.gitignore`; solo se versionan los `*.example`.

Para preparar el backend por primera vez:

```bash
cp backend/src/main/resources/backend.properties.example \
   backend/src/main/resources/backend.properties
```

Luego ajuste `bd.url`, `bd.usuario` y `bd.clave`. La clase
`ConfiguracionBackend` busca el archivo en este orden:

1. `-Dbackend.config=/ruta/backend.properties`
2. La variable de entorno `BACKEND_CONFIG`
3. El recurso `/backend.properties` del classpath

Además, `-Dbd.url`, `-Dbd.usuario`, `-Dbd.clave`, `-Dbd.pool.tamano`,
`-Dservidor.puerto`, `-Dllm.api.url`, `-Dllm.api.key` y `-Dllm.model`
sobrescriben el archivo, útil para equipos de integración sin editar
credenciales versionadas.

La sección `llm.*` configura el servicio de IA que interpreta frases de reserva.
La clave **solo existe en el backend**: el frontend nunca la recibe. Si
`llm.api.url` queda vacío, la función de IA se deshabilita con un mensaje claro
y el resto del sistema sigue funcionando.

## Capa de datos (Fase 3)

El módulo `backend` es el único que conoce JDBC y MySQL.

| Clase | Responsabilidad |
|---|---|
| `config.ConfiguracionBackend` | Lee y valida `backend.properties` |
| `data.FuenteDatos` | Pool **HikariCP**; entrega conexiones y las devuelve al pool |
| `data.DAOBase` | Transacciones, `PreparedStatement` y traducción de `SQLException` |
| `data.UsuarioDAO` | Usuarios, funcionarios, claves BCrypt y búsquedas |
| `data.CategoriaRecursoDAO` | CRUD de categorías y generación de `CAT-000001` |
| `data.RecursoDAO` | CRUD de recursos con su categoría |
| `data.ReservaDAO` | Reservas, disponibilidad, cancelación y conteos de estadísticas |

Decisiones de la capa:

* **HikariCP** en lugar de un pool propio: reutiliza y valida conexiones,
  limita el máximo y evita abrir una conexión por operación. Su configuración
  es mínima (URL, usuario, clave y tamaño).
* **Todas las consultas usan `PreparedStatement`** y `try-with-resources`;
  ningún valor se concatena en el SQL.
* **Crear una reserva es atómico**: se bloquean los recursos con
  `SELECT ... FOR UPDATE`, se comprueban los traslapes y se insertan la reserva
  y su detalle en una sola transacción. Si algo falla, no queda nada a medias.
* **Los errores de MySQL se traducen** a `ValidacionException`,
  `ConflictoException`, `NoEncontradoException` o `AccesoDatosException`
  (definidas en `common`), de modo que la lógica y el frontend no dependen
  del driver.
* Los conteos de estadísticas (`ConteoCategoria`, `ConteoSemana`) se calculan
  con `COUNT`/`GROUP BY` en el backend y se devuelven como DTO serializables.

## Capa lógica (Fase 4)

La interfaz `com.sistema.common.servicio.Service` define todo lo que el
frontend necesita; `backend.logic.ServiceImpl` la implementa usando los DAOs.
El frontend solo verá la interfaz (a través del Proxy), nunca las clases de
`backend`.

| Grupo | Operaciones principales |
|---|---|
| Sesión | `iniciarSesion`, `cambiarClave` (BCrypt) |
| Funcionarios | alta, modificación, eliminación, listado y búsqueda |
| Categorías y recursos | CRUD, búsquedas, filtros y comprobación de descripciones |
| Reservas | alta con recursos automáticos, cancelación, listados por funcionario y por rango |
| Matrices | actividades de la semana y calendarización por categoría |
| Estadísticas | reservas por categoría y actividades por semana |
| IA | `extraerDatosReserva` a partir de una frase libre |

Reglas y decisiones:

* **Las validaciones viven en el servidor**, aunque el frontend también las
  repita: identificadores y descripciones únicos, longitudes máximas de las
  columnas, fechas no pasadas, hora de fin posterior al inicio, pertenencia de
  la reserva y disponibilidad de recursos.
* **Claves con BCrypt**: la clave nunca se guarda ni se devuelve en texto plano.
  Un funcionario nuevo recibe su identificador como clave inicial, igual que en
  el Proyecto #1.
* **Borrados protegidos**: no se elimina un funcionario con reservas (aunque
  estén canceladas), una categoría con recursos ni un recurso incluido en
  reservas, para conservar el historial.
* **Concurrencia**: un bloqueo en proceso serializa la generación de
  `CAT-...`/`RES-...` y la elección de recursos; la transacción del DAO con
  `SELECT ... FOR UPDATE` sigue siendo la garantía frente a varias instancias.
* **Reloj inyectable**: `ServiceImpl` recibe un `Clock`, lo que permite probar
  fechas y cancelaciones de forma determinista.
* **IA en el backend**: `logic.ia.ClienteIA` llama al proveedor, interpreta el
  JSON con `AnalizadorRespuestaIA` y resuelve las categorías con
  `MapeoCategorias`. Devuelve un `DatosReservaSugerida` con las categorías
  encontradas y las que no existen en el sistema.
* **Reportes en el frontend**: PDF y gráficos se generan en el cliente a partir
  de los datos que expone `Service`; la interfaz no tiene métodos de reporte.

Pruebas de la fase:

* Unitarias con JUnit 5 + Mockito: sesión, funcionarios, categorías, recursos,
  reservas, matrices/estadísticas, IA y configuración.
* Integración (`*IT`) contra MySQL: `ServiceImplIT` recorre autenticación,
  cambio de clave, alta de funcionarios, reservas, cancelación, estadísticas,
  matrices y protección de catálogos.

## Protocolo de red (Fase 5)

El protocolo vive en `com.sistema.common.protocolo` y lo comparten ambos
extremos. Se eligió **serialización Java** (`Serializable`) porque:

* `common` no necesita dependencias externas (ni siquiera una biblioteca JSON).
* Ambos extremos usan exactamente las mismas clases, así que se conservan los
  tipos del dominio (`LocalDate`, `LocalTime`, enums, listas y modelos).
* No hay mapeo manual ni riesgo de que un campo se desincronice.

Cada mensaje viaja como **un objeto completo** en el `ObjectOutputStream`. Al
escribir se hace `flush()` y `reset()`; el lector hace `readObject()` en un
bucle. El `reset()` evita que un objeto modificado se envíe como referencia al
anterior.

### Mensajes

| Mensaje | Campos |
|---|---|
| `Solicitud` | `id` (String), `operacion` (`Operacion`), `parametros` (`List<Object>`) |
| `Respuesta` | `id` (String), `exito` (boolean), `resultado` (Object), `error` (`ErrorRemoto`) |
| `ErrorRemoto` | `codigo` (`CodigoError`), `mensaje` (String) |
| `Notificacion` | `tipo` (`TipoNotificacion`), `carga` (`CargaNotificacion`) |
| `CargaNuevaReserva` | `reservaId`, `encargado`, `actividad`, `fecha`, `horaInicio`, `horaFin`, `recursos` |

`Solicitud` valida que la cantidad de parámetros coincida con la operación. Los
parámetros van en el orden del método y puede haber valores `null`.

### Operaciones

| Operación | Método de `Service` | Parámetros (en orden) |
|---|---|---|
| `INICIAR_SESION` | `iniciarSesion` | `id`, `clave` |
| `CAMBIAR_CLAVE` | `cambiarClave` | `id`, `claveActual`, `nuevaClave`, `confirmacion` |
| `ALTA_FUNCIONARIO` | `altaFuncionario` | `id`, `nombre`, `telefono` |
| `MODIFICAR_FUNCIONARIO` | `modificarFuncionario` | `id`, `nombre`, `telefono` |
| `ELIMINAR_FUNCIONARIO` | `eliminarFuncionario` | `id` |
| `LISTAR_FUNCIONARIOS` | `listarFuncionarios` | — |
| `BUSCAR_FUNCIONARIOS` | `buscarFuncionarios` | `criterio` |
| `ALTA_CATEGORIA` | `altaCategoria` | `descripcion` |
| `MODIFICAR_CATEGORIA` | `modificarCategoria` | `id`, `nuevoId`, `descripcion` |
| `ELIMINAR_CATEGORIA` | `eliminarCategoria` | `id` |
| `LISTAR_CATEGORIAS` | `listarCategorias` | — |
| `BUSCAR_CATEGORIAS` | `buscarCategorias` | `criterio` |
| `EXISTE_DESCRIPCION_CATEGORIA` | `existeDescripcionCategoria` | `descripcion` |
| `ALTA_RECURSO` | `altaRecurso` | `id`, `categoriaId`, `descripcion` |
| `MODIFICAR_RECURSO` | `modificarRecurso` | `id`, `nuevoId`, `categoriaId`, `descripcion` |
| `ELIMINAR_RECURSO` | `eliminarRecurso` | `id` |
| `LISTAR_RECURSOS` | `listarRecursos` | — |
| `BUSCAR_RECURSOS` | `buscarRecursos` | `criterio` |
| `FILTRAR_RECURSOS` | `filtrarRecursos` | `categoriaId`, `criterio` |
| `CREAR_RESERVA` | `crearReserva` | `funcionarioId`, `actividad`, `fecha`, `horaInicio`, `horaFin`, `idsCategorias` |
| `CANCELAR_RESERVA` | `cancelarReserva` | `idReserva`, `funcionarioId` |
| `LISTAR_RESERVAS_DE` | `listarReservasDe` | `funcionarioId` |
| `LISTAR_TODAS_LAS_RESERVAS` | `listarTodasLasReservas` | — |
| `LISTAR_RESERVAS_ACTIVAS_ENTRE` | `listarReservasActivasEntre` | `desde`, `hasta` |
| `CONSTRUIR_MATRIZ_ACTIVIDADES` | `construirMatrizActividades` | `fecha` |
| `CONSTRUIR_MATRIZ_CALENDARIZACION` | `construirMatrizCalendarizacion` | `fecha`, `categoriaId` |
| `CATEGORIAS_RESERVADAS` | `categoriasReservadas` | `desde`, `hasta` |
| `ACTIVIDADES_POR_SEMANA` | `actividadesPorSemana` | `desde`, `hasta` |
| `EXTRAER_DATOS_RESERVA` | `extraerDatosReserva` | `frase` |

### Códigos de error

| Código | Excepción de origen |
|---|---|
| `VALIDACION` | `ValidacionException` |
| `NO_ENCONTRADO` | `NoEncontradoException` |
| `CONFLICTO` | `ConflictoException` |
| `AUTENTICACION` | `AutenticacionException` |
| `ACCESO_DATOS` | `AccesoDatosException` |
| `CONFIGURACION` | `ConfiguracionException` |
| `SERVICIO_EXTERNO` | `ServicioExternoException` |
| `INTERNO` | cualquier otra excepción (mensaje genérico) |

### Flujo de una petición

1. El Proxy genera un `id` con `Protocolo.nuevoId()` y crea `Solicitud.de(...)`.
2. Envía la solicitud y registra un `CompletableFuture` asociado a ese `id`.
3. El backend lee, ejecuta el método de `Service` y responde con
   `Respuesta.ok` o `Respuesta.error`, conservando el `id`.
4. El hilo lector del Proxy encuentra el futuro por `id` y entrega el resultado;
   si `exito` es falso, traduce el `CodigoError` a la excepción de dominio.

### Flujo de una notificación

1. El backend crea la reserva con éxito.
2. Construye `Notificacion.nuevaReserva(reserva)`.
3. La envía a todos los clientes activos excepto al creador (Fase 6).
4. El hilo lector del Proxy reconoce la `Notificacion` y la entrega al listener
   de alertas, sin bloquear la respuesta de la petición original.

### Seguridad de la deserialización

`Protocolo.crearEntrada` instala un `ObjectInputFilter` que solo admite clases
de `com.sistema.common.*` y de las librerías estándar necesarias
(`java.lang`, `java.time`, `java.util`, `java.math`). Rechaza cualquier otra
clase y limita la profundidad (100), los arreglos (1 000 000 de elementos) y las
referencias (1 000 000). El flujo de entrada corta **cada mensaje** que supere
32 MB; el contador se reinicia después de leer un mensaje completo, de modo que
el límite no se agota por acumulación en una conexión larga. Así un cliente
malicioso no puede deserializar clases arbitrarias ni enviar objetos sin
control de tamaño.

Pruebas: `common/src/test/java/.../protocolo` cubre la serialización y
deserialización de `Solicitud`, `Respuesta` (éxito y error), `Notificacion`,
el mapeo 1:1 con `Service`, los códigos de error, el filtro, el límite por
mensaje y la serialización de todos los modelos.

## Servidor multi-hilo (Fase 6)

El backend es un servidor concurrente que atiende a todos los frontends por
sockets. Se eligieron **hilos virtuales** (`Executors.newThreadPerTaskExecutor`)
porque cada conexión pasa la mayor parte del tiempo bloqueada esperando datos:
así el servidor puede atender muchos clientes con un costo mínimo.

| Clase | Responsabilidad |
|---|---|
| `Servidor` | Abre el `ServerSocket` en `servidor.puerto`, acepta conexiones, crea un `ManejadorCliente` por cliente y apaga todo de forma ordenada. |
| `ManejadorCliente` | Bucle leer `Solicitud` → despachar → escribir `Respuesta`; difunde la notificación cuando se crea una reserva; maneja desconexiones y errores. |
| `Despachador` | Traduce cada `Operacion` a la llamada correspondiente de `Service` y valida el tipo de los parámetros. |
| `ClienteConectado` | Abstracción de un cliente: identificador, envío y cierre. |
| `ConexionCliente` | Implementación sobre socket; serializa las escrituras con un lock por cliente. |
| `RegistroClientes` | Registro thread-safe (`ConcurrentHashMap`), difusión asincrónica y eliminación/cierre de clientes que fallen. |

### Flujo de una petición

1. `accept()` asigna un id (`cliente-N`) y entrega el socket a un hilo virtual
   con `ManejadorCliente`.
2. `ConexionCliente` crea los flujos del protocolo (salida y `flush()` antes de
   la entrada, para que ambos extremos intercambien cabeceras sin bloquearse).
3. El manejador lee una `Solicitud`, `Despachador` invoca `Service` y responde
   con el mismo `id` de correlación.
4. Si la operación es `CREAR_RESERVA` y terminó bien, `RegistroClientes` encola
   una `Notificacion.nuevaReserva(reserva)` para todos los clientes activos
   **menos la conexión que la creó**.
5. Un error de dominio viaja como `Respuesta` fallida con su `CodigoError`; un
   error interno se reporta con un mensaje genérico.
6. Al desconectarse, el cliente se elimina del registro y su socket se cierra.

### Concurrencia, notificaciones y apagado

* Una respuesta y una notificación pueden coincidir sobre el mismo socket:
  `ConexionCliente.enviar` está sincronizado, así que cada mensaje se escribe
  completo y no se mezclan los flujos.
* La comparación para excluir al creador es por **conexión**, no por usuario:
  si la misma persona abre dos frontends, la segunda sesión sí recibe la alerta.
* La difusión usa un `Executor`: el hilo que atendió la petición no espera a
  que todos los clientes escriban. Si un cliente falla, se elimina y se cierra.
* El saludo inicial tiene un tiempo máximo de 10 s para que una conexión que no
  envía la cabecera no ocupe un hilo indefinidamente.
* `Servidor.detener()` y `ServidorMain` cierran en orden el socket de
  aceptación, las conexiones activas, el ejecutor de clientes y el pool
  HikariCP. También se ejecuta desde un *shutdown hook*.
* El logging registra conexión, operación, código de error y desconexión;
  nunca parámetros, claves ni datos personales.

Para levantarlo:

```bash
mvn -pl backend -am exec:java
# o, tras mvn package:
java -jar backend/target/sistema-backend-2.0.0-SNAPSHOT.jar
```

El puerto se toma de `servidor.puerto` en `backend.properties` (5000 por
defecto en la plantilla).

Pruebas de la fase:

* `DespachadorTest`: las 29 operaciones invocan el método correcto, se validan
  los tipos y se propagan los errores de dominio.
* `RegistroClientesTest`: exclusión del origen, difusión diferida, eliminación
  del cliente que falla y cierre masivo.
* `ServidorTest`: ciclo de vida y validación del puerto.
* `ServidorIT`: dos clientes reales por socket; uno crea una reserva, el otro
  recibe la notificación y el creador no.
* `ServidorReservasIT`: la misma prueba de punta a punta con `ServiceImpl` y
  MySQL reales; se omite si la base no está disponible.

## Frontend: conexión y patrón Proxy (Fase 7)

El frontend se comunica con el backend **solo** a través de
`ServiceProxy`, que implementa la misma interfaz `Service` de `common`. Los
controladores reciben esa interfaz por inyección y no saben que hay sockets de
por medio: la Fase 8 puede cambiar el proxy por un doble de prueba sin tocar
las vistas.

| Clase | Responsabilidad |
|---|---|
| `ServiceProxy` | Implementa `Service`: arma cada `Solicitud`, espera la `Respuesta` y devuelve el resultado o lanza la excepción de dominio. |
| `LectorMensajes` | Hilo lector dedicado: separa `Respuesta` (por id) de `Notificacion` (a las escuchas). |
| `ConexionServidor` | Socket y flujos del protocolo; serializa las escrituras concurrentes. |
| `EscuchaNotificaciones` | Interfaz que implementará el controlador de alertas (Fase 9). |
| `TraductorErrores` | Convierte `ErrorRemoto` en `ValidacionException`, `ConflictoException`, etc. |
| `ExcepcionRed` | Fallo de comunicación clasificado: sin conexión, conexión perdida, timeout, cierre o respuesta inválida. |
| `ConfiguracionFrontend` | Host, puerto y timeout desde `frontend.properties`, variables de entorno o argumentos. |

### Flujo de una petición

1. Un controlador llama a un método de `Service` (por ejemplo,
   `iniciarSesion("111", "111")`).
2. `ServiceProxy` construye `Solicitud.de(...)` con un id de correlación nuevo y
   registra un `CompletableFuture` en un mapa concurrente.
3. `ConexionServidor` envía la solicitud y el hilo llamador espera la respuesta
   con el timeout configurado.
4. `LectorMensajes` recibe la `Respuesta`, y `ServiceProxy` completa el futuro
   cuyo id coincide. El resultado se valida contra el tipo esperado.
5. Si la respuesta es fallida, `TraductorErrores` lanza la excepción de dominio
   con el mensaje del backend. Si el servidor no responde, se lanza
   `ExcepcionRed` con un mensaje apto para el usuario.

Una `Notificacion` que llega por el mismo socket no completa ningún futuro: se
reenvía a las `EscuchaNotificaciones` registradas, desde el hilo lector. El
controlador de alertas (Fase 9) la pasa al hilo de eventos de Swing.

### Configuración y ejecución

`frontend.properties` es opcional. Orden de búsqueda:
`-Dfrontend.config=...` → `FRONTEND_CONFIG` → `/frontend.properties` →
valores por defecto. Las propiedades del sistema `servidor.host`,
`servidor.puerto` y `proxy.timeout.ms` tienen prioridad sobre el archivo.

```bash
# misma máquina, puerto 5000 (por defecto)
mvn -pl frontend -am exec:java

# otro equipo
mvn -pl frontend -am exec:java -Dexec.args="192.168.1.20 5000"
# o con el JAR
java -jar frontend/target/sistema-frontend-2.0.0-SNAPSHOT.jar 192.168.1.20 5000
```

El punto de entrada abre la conexión, arranca el proxy y muestra la ventana de
login; el resto del ciclo de vida lo coordina `ControladorAplicacion` (Fase 8).

### Separación de capas

El POM del frontend no declara el driver MySQL, HikariCP ni el módulo
`backend`. `DependenciasFrontendTest` lo verifica intentando cargar esas
clases: si alguien agrega la dependencia, la prueba falla.

Pruebas de la fase:

* `ConfiguracionFrontendTest`: archivo, classpath, propiedades del sistema,
  argumentos y validación de host, puerto y timeout.
* `TraductorErroresTest`: cada `CodigoError` produce su excepción de dominio y
  conserva el mensaje.
* `ServiceProxyIT`: login, orden de parámetros, listas, emparejamiento por id
  con respuestas desordenadas, notificaciones, errores de dominio, timeout,
  servidor caído, pérdida de conexión, respuesta inválida y cierre idempotente.
  Usa un `ServidorPrueba` que habla el protocolo real, sin MySQL ni el módulo
  `backend`.
* `ProxyBackendRealIT`: aceptación de la fase. Inicia sesión (`111/111`) contra
  el backend real. Se omite si no hay un servidor escuchando; para ejecutarla,
  arranque MySQL y `mvn -pl backend -am exec:java` y luego:

```bash
mvn -pl frontend -am -Dit.test=ProxyBackendRealIT verify
```

## Frontend: MVC y funcionalidades (Fase 8)

La interfaz replica las funcionalidades del Proyecto #1 sobre la arquitectura
distribuida. El **MVC es estricto**: la vista no conoce el proxy ni las reglas de
negocio, el controlador coordina y el modelo guarda el estado del módulo.

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Vista | `frontend.view` | Solo dibuja el modelo y expone las acciones del usuario (`alBuscar`, `alGuardar`, `alCancelar`…). No llama a `Service`. |
| Controlador | `frontend.controller` | Valida lo básico, decide alta o modificación, invoca `Service` fuera del hilo de eventos y actualiza el modelo. |
| Modelo | `frontend.model` | Estado del módulo: listado, criterio de búsqueda, selección y sesión activa. |

```text
frontend/src/main/java/com/sistema/frontend/
├── controller/     ControladorAplicacion, ControladorPrincipal y un controlador por módulo
├── model/          ModeloFuncionarios, ModeloCategorias, ModeloRecursos, ModeloReservas,
│                   ModeloCalendarizacion, ModeloActividades y Sesion
├── view/           VentanaPrincipal, vistas de login y de cada módulo, Dialogos
│   └── componentes/ ModeloTabla, SelectorFecha, SelectorHora, SelectorArchivoPdf
└── util/           Iconos, GeneradorPDF y Reportes
```

### Flujo de una acción

1. El usuario pulsa **Guardar**; la vista ejecuta la acción registrada y entrega
   los textos escritos (`getId()`, `getNombre()`, …).
2. El controlador valida los campos obligatorios. Si falta algo muestra una
   advertencia y **no** consulta al backend.
3. El controlador entrega el trabajo a `EjecutorTareas`. En producción
   `EjecutorSwing` usa un `SwingWorker`: la llamada al proxy corre en un hilo de
   trabajo y los callbacks vuelven al hilo de eventos, así la ventana no se
   congela mientras se espera la respuesta.
4. Si el backend acepta, el controlador actualiza el modelo, pide `actualizar()`
   a la vista y recarga el listado. Si falla, `Errores.mensaje(...)` muestra el
   texto de la excepción de dominio y el formulario conserva los datos.

La validación del frontend es solo para dar respuesta inmediata: el backend
vuelve a validar longitudes, formato, claves y conflictos. Nunca se sustituye.

### Módulos y pestañas

| Rol | Pestañas |
|---|---|
| Administrador | Funcionarios, Categorías, Recursos, Calendarización, Actividades, Estadísticas |
| Funcionario | Reservas (nueva + mis reservas), Calendarización, Actividades, Estadísticas |

| Módulo | Controlador | Operaciones |
|---|---|---|
| Ingreso | `ControladorLogin` | Autenticación y registro de la sesión. |
| Cambio de clave | `ControladorCambiarClave` | Verifica la confirmación y llama a `cambiarClave`. |
| Funcionarios | `ControladorFuncionarios` | Buscar, crear, modificar, eliminar y PDF. |
| Categorías | `ControladorCategorias` | Buscar, crear (id asignado por el backend), modificar, eliminar y PDF. |
| Recursos | `ControladorRecursos` | Filtro por categoría y texto, CRUD y PDF. |
| Reservas | `ControladorReservas` | Alta con extracción por IA, historial del funcionario, cancelación y PDF. |
| Calendarización | `ControladorCalendarizacion` | Matriz de 24 horas por recurso de una categoría y exportación. |
| Actividades | `ControladorActividades` | Matriz semanal de 24 horas por 7 días y exportación. |
| Estadísticas | `ControladorEstadisticas` | Conteos agregados por categoría y por semana, gráfico y PDF. |

`ControladorAplicacion` es el único que crea ventanas: muestra el login, abre la
ventana principal según el rol, gestiona el diálogo de cambio de clave, el cierre
de sesión y la salida. `ControladorPrincipal` arma las pestañas e inyecta en cada
una su modelo, su vista y el mismo `Service`.

Los reportes se generan en el frontend con OpenPDF (`GeneradorPDF` + `Reportes`);
el backend no produce archivos. Los iconos del P1 se copiaron a
`frontend/src/main/resources/Iconos/` y se cargan con `util.Iconos`, que
devuelve `null` si el recurso no está, sin romper la vista.

### Ejecución

Con el backend escuchando en el puerto 5000 y la base cargada:

```bash
mvn -pl frontend -am exec:java
# o
java -jar frontend/target/sistema-frontend-2.0.0-SNAPSHOT.jar
```

Ingrese con `111/111` (administrador) o `222/222` (funcionario). El menú
**Sesión** permite cambiar la clave o cerrar sesión.

Pruebas de la fase:

* `ControladorLoginTest`, `ControladorCambiarClaveTest`: validación local,
  registro de la sesión y fallos del backend.
* `ControladorFuncionariosTest`, `ControladorCategoriasTest`,
  `ControladorRecursosTest`, `ControladorReservasTest`,
  `ControladorCalendarizacionTest`, `ControladorActividadesTest`: acciones de la
  vista → controlador → `Service` simulado (Mockito) y vuelta al modelo y a la
  tabla.
* `ErroresTest`, `EjecutorSwingTest`, `SesionTest`: traducción de errores,
  ejecución fuera del EDT y estado de la sesión.
* `FlujoMVCIT`: flujo completo **Vista → Controlador → Proxy → Servidor →
  Controlador → Vista** contra un servidor que habla el protocolo real (sin
  MySQL): login y alta de funcionario con la tabla actualizada.

## Frontend: área de alertas (Fase 9)

El panel de alertas está al **lado derecho** de la ventana principal, fuera del
`JTabbedPane` y siempre visible. Muestra, solo de forma informativa, las reservas
que **otros** usuarios registran mientras la sesión está activa. No se persiste
nada: al cerrar sesión las alertas desaparecen.

| Clase | Responsabilidad |
|---|---|
| `ModeloAlertas` | Lista local de alertas, con deduplicación por reserva. Sincronizada porque la notificación llega por el hilo lector. |
| `Alerta` | Valor inmutable con los datos de la reserva y el texto `Encargado - Actividad`. |
| `PanelAlertas` | Tabla con columnas **Actividad**, **Ver** y **Desc.**; diálogo de detalle y confirmación de descarte. No conoce el backend. |
| `ControladorAlertas` | Implementa `EscuchaNotificaciones`: recibe la notificación, la pasa al hilo de eventos y atiende Ver/Descartar. |
| `HiloInterfaz` | Abstracción del despacho a Swing (`SwingUtilities::invokeLater`); las pruebas la sustituyen por ejecución directa. |
| `FuenteNotificaciones` | Contrato mínimo del proxy para registrar/quitar escuchas sin acoplar los controladores a los sockets. |

### Flujo de una alerta

1. El backend crea la reserva y `RegistroClientes` difunde una `Notificacion`
   con `CargaNuevaReserva` a todos los clientes **menos la conexión de origen**;
   por eso el creador no recibe su propia alerta.
2. El hilo lector del proxy entrega la notificación a `ControladorAlertas`,
   registrado por `ControladorAplicacion` al abrir la ventana principal.
3. El controlador la encola y programa **un único** volcado al hilo de eventos.
   Si llegan muchas alertas seguidas se drenan juntas en un solo refresco, sin
   bloquear ni el lector ni la interfaz.
4. El modelo agrega la alerta (ignorando reservas repetidas) y el panel
   actualiza la tabla.

`alPerderConexion` avisa una sola vez y el pie del panel queda en "Sin conexión
con el servidor"; las peticiones posteriores fallan con `ExcepcionRed` y los
controladores de módulo muestran el error.

### Ver y descartar

* **Ver** (ícono de carpeta, o doble clic en la actividad) abre un diálogo modal
  titulado con la actividad y muestra **Encargado, Fecha, Desde, Hasta y
  Recursos**, con botón **OK**.
* **Desc.** (ícono rojo) pide confirmación con el nombre de la actividad. Si se
  confirma, la alerta se quita del modelo y de la tabla. **No se envía ninguna
  petición al backend**: el descarte es solo local.
* El controlador no abre diálogos mientras procesa la notificación: el trabajo
  pesado ya está en el hilo de eventos, así que una alerta que llega con un
  diálogo abierto se repinta cuando el diálogo lo permite, sin corromper el
  estado de Swing.

Las alertas viven en el `ControladorPrincipal` de cada sesión: al cerrar sesión
se quita la escucha del proxy y se llama a `vaciar()`, de modo que la siguiente
sesión empieza sin alertas.

### Probar con dos frontends

1. Arranque MySQL y el backend: `mvn -pl backend -am exec:java`.
2. Abra dos frontends: `mvn -pl frontend -am exec:java` (dos terminales).
3. Ingrese en uno con `222/222` y en el otro con `333/333`.
4. Cree una reserva como `222`; en el frontend de `333` aparece la alerta
   `Ana Pérez - <actividad>` al instante. El de `222` no la recibe.
5. En `333`, use **Ver** y **Desc.**; la reserva no cambia en el backend.

Pruebas de la fase:

* `AlertaTest`, `ModeloAlertasTest`: formato del texto, orden, deduplicación,
  eliminación y vaciado.
* `ControladorAlertasTest`: llegada de notificaciones, ráfagas agrupadas en un
  solo refresco, Ver, Descarte confirmado/cancelado, fila inexistente y aviso
  único de desconexión.
* `PanelAlertasTest`: render de la tabla, conteo en el pie, clic en las columnas
  **Ver** y **Desc.**, texto del detalle y estado de desconexión.
* `AlertasIT`: dos frontends contra un servidor que habla el protocolo real. Una
  reserva creada en un cliente llega solo al otro; Ver y Desc. no generan
  tráfico; la caída de la conexión se informa en el panel.

## Frontend: estadísticas (Fase 10)

La pestaña **Estadísticas** está disponible para ambos roles y contiene dos
paneles independientes:

| Panel | Datos | Columnas |
|---|---|---|
| **Recursos** | Reservas activas que usaron recursos de cada categoría | Categoría, Reservas |
| **Actividades** | Reservas activas por semana (identificada por su lunes) | Semana (lunes), Actividades |

Cada panel tiene su propio rango **Desde/Hasta**, un botón **Calcular**, la tabla
de resultados, un **gráfico de barras** (JFreeChart) y la exportación a PDF con
la tabla y la imagen del gráfico.

| Clase | Responsabilidad |
|---|---|
| `ModeloEstadisticas` | Guarda los dos conteos ya agregados que devolvió el backend. |
| `PanelEstadistica` | Rango de fechas, tabla y gráfico de un panel; no calcula nada. |
| `VistaEstadisticas` | Dos pestañas internas (Recursos y Actividades) y mensajes al usuario. |
| `ControladorEstadisticas` | Valida el rango, consulta al backend fuera del hilo de eventos y vuelca el resultado en el modelo. |

### Agregación en el backend

El frontend nunca recibe la lista completa de reservas para contarlas: pide el
conteo y MySQL hace el `COUNT`/`GROUP BY`.

```sql
-- Recursos: una reserva cuenta una sola vez por categoría
SELECT c.id, c.descripcion, COUNT(DISTINCT rr.reserva_id) AS cantidad
FROM categorias c
JOIN recursos rc ON rc.categoria_id = c.id
JOIN reserva_recursos rr ON rr.recurso_id = rc.id
JOIN reservas r ON r.id = rr.reserva_id
WHERE r.estado = 'ACTIVA' AND r.fecha BETWEEN ? AND ?
GROUP BY c.id, c.descripcion
ORDER BY cantidad DESC, c.descripcion;

-- Actividades: se agrupa por el lunes de la semana de cada reserva
SELECT DATE_SUB(fecha, INTERVAL WEEKDAY(fecha) DAY) AS lunes, COUNT(*) AS cantidad
FROM reservas
WHERE estado = 'ACTIVA' AND fecha BETWEEN ? AND ?
GROUP BY lunes
ORDER BY lunes;
```

Las consultas viven en `ReservaDAO.contarReservasPorCategoria` y
`contarActividadesPorSemana`; `ServiceImpl` valida el rango con `Validaciones`
antes de llamarlas. Solo se contabilizan reservas en estado `ACTIVA`.

### Validación y errores

* Si una fecha no tiene el formato `dd/MM/yyyy`, el selector lanza un mensaje y
  no se consulta al backend.
* Si **Desde** es posterior a **Hasta**, se avisa y tampoco se consulta.
* Un error del servidor (por ejemplo, pérdida de conexión) se muestra en un
  diálogo con el texto de dominio; el panel se reactiva para reintentar.

### Pruebas de la fase

* `ModeloEstadisticasTest`: estado inicial, conteos guardados y copia inmutable.
* `VistaEstadisticasTest`: dos paneles, columnas, filas, series del gráfico,
  rango independiente por panel, botones y habilitación.
* `ControladorEstadisticasTest`: consulta con el rango elegido, rango invertido,
  fecha inválida, error del backend, exportación a PDF y cancelación.
* `EstadisticasIT`: el proxy transporta los conteos y la tabla y el gráfico
  muestran exactamente las cifras que envió el servidor.
* `GeneradorPDFTest`: el documento se cierra antes que el flujo de salida, de
  modo que el PDF no queda vacío (corrige un fallo de cierre detectado aquí).

## Validación y errores transversal (Fase 11)

La validación ocurre en dos niveles: el frontend avisa antes de enviar y el
backend vuelve a comprobar todo antes de tocar MySQL. El servidor sigue siendo
la autoridad; las reglas locales solo evitan viajes innecesarios.

### Validaciones de los formularios

`com.sistema.frontend.util.Validaciones` concentra los límites y los mensajes.
Cada controlador valida sus campos y, si algo falla, muestra el texto en un
diálogo sin llamar al backend.

| Pantalla | Campos y reglas |
|---|---|
| Ingreso | Identificador obligatorio (≤ 20 caracteres) y clave obligatoria (≤ 72 bytes). |
| Cambio de clave | Identificador, clave actual y clave nueva obligatorios; la confirmación debe coincidir y la nueva debe ser distinta de la actual. |
| Funcionarios | Identificador obligatorio (≤ 20), nombre obligatorio (≤ 100) y teléfono con 7 a 15 dígitos y `+` opcional. |
| Categorías | Descripción obligatoria (≤ 100); identificador nuevo opcional (≤ 20). |
| Recursos | Identificador obligatorio (≤ 20), categoría seleccionada y descripción obligatoria (≤ 150). |
| Nueva reserva | Actividad obligatoria (≤ 150), frase de IA obligatoria (≤ 1000), al menos una categoría, fecha no anterior a hoy y hora de fin posterior a la de inicio. |
| Mis reservas | Debe haber una fila seleccionada; la cancelación de reservas pasadas la rechaza el backend. |
| Calendarización | Categoría seleccionada y fecha con formato válido. |
| Actividades | Fecha con formato válido. |
| Estadísticas | Fechas con formato válido y **Desde** ≤ **Hasta** en cada panel. |

Los **duplicados** (identificador de funcionario, recurso o categoría) los
detecta MySQL y `DAOBase` los traduce a `ConflictoException` con un mensaje
claro, que el frontend muestra en el diálogo de error. Las restricciones de
clave foránea y de `CHECK` se traducen del mismo modo.

### Errores del servidor

* Toda petición devuelve una `Respuesta`; si falla, trae un `ErrorRemoto` con
  código y mensaje de dominio. `ErrorRemoto.de` reemplaza cualquier mensaje
  técnico por uno genérico, de modo que nunca se filtra un *stack trace*.
* `TraductorErrores` convierte el error remoto en la excepción de dominio
  correspondiente y `Errores.mensaje` obtiene el texto para el usuario.
* Las vistas muestran ese texto con `mostrarError`/`mostrarAdvertencia`; los
  fallos inesperados se registran solo en el log del proceso.

### Concurrencia al reservar

Dos usuarios pueden intentar reservar el mismo recurso en el mismo horario.
`ReservaDAO.insertar` ejecuta todo dentro de una transacción:

1. `SELECT id FROM recursos ... ORDER BY id FOR UPDATE` bloquea las filas de los
   recursos implicados (el orden evita interbloqueos entre peticiones).
2. Con el bloqueo tomado se vuelve a consultar el traslape con
   `recursosOcupados`; si ya hay una reserva activa, se lanza
   `ConflictoException`.
3. Solo entonces inserta la reserva y su detalle en `reserva_recursos` y hace
   `commit`.

La segunda petición espera el bloqueo, vuelve a comprobar y recibe el conflicto;
nunca se crean dos reservas para el mismo recurso y horario.
`ServiceImplIT.dosPeticionesSimultaneasNoDuplicanLaReserva` lo comprueba con dos
instancias de `ServiceImpl` y una `CyclicBarrier`: exactamente una petición
tiene éxito, la otra recibe `ConflictoException`, y quedan una reserva y un
detalle.

### Caída, reconexión y cierre ordenado

* `ServiceProxy` implementa `Reconectable`. Al cortarse el socket falla las
  peticiones pendientes, marca el proxy como desconectado y avisa a las
  escuchas.
* `ControladorConexion` (registrado por `ControladorAplicacion`) muestra un
  diálogo y ofrece reconectar hasta **3 veces**. La reconexión corre fuera del
  hilo de eventos.
* `ServiceProxy.reconectar()` cierra la conexión anterior y abre una nueva;
  cada conexión tiene una **generación** para que un lector viejo no marque
  como caída la conexión nueva. Las escuchas y la sesión se conservan.
* Si el usuario declina o los intentos se agotan, la aplicación se cierra de
  forma ordenada: quita escuchas, cierra la ventana principal, cierra la sesión
  y el socket, y termina el proceso.
* Cuando la conexión vuelve, `ControladorAlertas.conexionRestablecida()`
  restablece el pie del panel de alertas.

Para probarlo manualmente:

1. Abra el backend y dos frontends (`mvn -pl backend -am exec:java` y
   `mvn -pl frontend -am exec:java`).
2. Inicie sesión y cree una reserva en un frontend: el otro recibe la alerta.
3. Detenga el backend (`taskkill /PID <pid> /F`).
4. En el frontend aparece el diálogo de conexión perdida; elija reintentar. Si
   el backend sigue apagado se agotan los 3 intentos y la aplicación se cierra;
   si se vuelve a iniciar el backend, la reconexión tiene éxito y el panel de
   alertas vuelve a su estado normal.

### Pruebas de la fase

* `ValidacionesTest`: límites de longitud, texto en blanco, identificadores,
  teléfono, clave por bytes y campos opcionales.
* `ControladorConexionTest`: reconexión exitosa, máximo de intentos con cierre
  ordenado y notificaciones que no le competen.
* `ControladorAlertasTest`: el aviso de desconexión se restablece tras
  reconectar y vuelve a mostrarse si cae otra vez.
* `ServiceProxyIT`: reconexión tras una caída y fallo de reconexión cuando el
  backend sigue apagado.
* `ServiceImplIT`: dos altas simultáneas sobre el mismo recurso crean una sola
  reserva.

## Base de datos

Nombre de la base: **`proyectoII_reservas`** (MySQL 8.4 LTS, `utf8mb4`).

```bash
mysql -u root -p --default-character-set=utf8mb4 < sql/schema.sql
mysql -u root -p --default-character-set=utf8mb4 < sql/datos_iniciales.sql
```

`schema.sql` crea la base desde cero (elimina la anterior si existe) y
`datos_iniciales.sql` carga categorías, recursos, reservas de ejemplo y cuatro
usuarios de prueba con clave hasheada (BCrypt):

| Identificador | Clave | Rol |
|---|---|---|
| `111` | `111` | Administrador |
| `222` | `222` | Funcionario |
| `333` | `333` | Funcionario |
| `444` | `444` | Funcionario |

El detalle del modelo, el diagrama ER (Mermaid), las decisiones de diseño y las
consultas de estadísticas están en [`sql/README.md`](sql/README.md).

## Ramas de trabajo

* `main`: versión estable y entregable.
* `dev`: integración de las fases.
* `feature/fase-N-descripcion`: una rama por fase o funcionalidad; se integra con Pull Request.

## Estado de las fases

| Fase | Descripción | Estado |
|---|---|---|
| 1 | Estructura Maven multi-módulo | ✅ Completada |
| 2 | Base de datos MySQL | ✅ Completada |
| 3 | Backend: capa de Datos (JDBC) | ✅ Completada |
| 4 | Backend: capa Lógica (Service) | ✅ Completada |
| 5 | Protocolo de red | ✅ Completada |
| 6 | Servidor multi-hilo y notificaciones | ✅ Completada |
| 7 | Frontend: conexión y patrón Proxy | ✅ Completada |
| 8 | Frontend MVC (funcionalidades del P1) | ✅ Completada |
| 9 | Área de Alertas | ✅ Completada |
| 10 | Estadísticas | ✅ Completada |
| 11 | Validación y errores transversal | ✅ Completada |
| 12 | Pruebas JUnit 5 | ⏳ Pendiente |
| 13 | Entrega y defensa | ⏳ Pendiente |
