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
├── backend/                      Servidor sin interfaz gráfica
│   └── src/main/java/com/sistema/backend/
│       ├── config/               Lectura de backend.properties
│       ├── data/                 DAOs JDBC + pool de conexiones (HikariCP)
│       ├── logic/                ServiceImpl (reglas de negocio) e integración con IA
│       └── server/               ServerSocket, ClientHandler, broadcast
├── frontend/                     Cliente Swing
│   └── src/main/java/com/sistema/frontend/
│       ├── proxy/                ServiceProxy (implementa Service vía sockets)
│       ├── model/                Modelos de la vista (MVC)
│       ├── view/                 Vistas Swing (incluye panel de Alertas)
│       └── controller/           Controladores MVC
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
* `frontend/src/main/resources/frontend.properties` — host y puerto del backend (Fase 7).
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
| 5 | Protocolo de red | ⏳ Pendiente |
| 6 | Servidor multi-hilo y notificaciones | ⏳ Pendiente |
| 7 | Frontend: conexión y patrón Proxy | ⏳ Pendiente |
| 8 | Frontend MVC (funcionalidades del P1) | ⏳ Pendiente |
| 9 | Área de Alertas | ⏳ Pendiente |
| 10 | Estadísticas | ⏳ Pendiente |
| 11 | Validación y errores transversal | ⏳ Pendiente |
| 12 | Pruebas JUnit 5 | ⏳ Pendiente |
| 13 | Entrega y defensa | ⏳ Pendiente |
