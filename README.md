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
│       ├── data/                 DAOs JDBC + pool de conexiones
│       ├── logic/                ServiceImpl (reglas de negocio)
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
* **MySQL 8.0+** (a partir de la Fase 2).

## Compilar y probar

```bash
mvn clean verify     # compila + tests unitarios (*Test) + tests de integración (*IT)
mvn test             # solo tests unitarios (Surefire)
mvn -DskipTests package
```

Los tests unitarios se llaman `*Test` y los de integración `*IT`
(Surefire y Failsafe respectivamente).

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
  puerto del servidor y configuración del servicio de IA.
* `frontend/src/main/resources/frontend.properties` — host y puerto del backend.
* Ambos archivos están en `.gitignore`; solo se versionan los `*.example`.

## Ramas de trabajo

* `main`: versión estable y entregable.
* `dev`: integración de las fases.
* `feature/fase-N-descripcion`: una rama por fase o funcionalidad; se integra con Pull Request.

## Estado de las fases

| Fase | Descripción | Estado |
|---|---|---|
| 1 | Estructura Maven multi-módulo | ✅ Completada |
| 2 | Base de datos MySQL | ⏳ Pendiente |
| 3 | Backend: capa de Datos (JDBC) | ⏳ Pendiente |
| 4 | Backend: capa Lógica (Service) | ⏳ Pendiente |
| 5 | Protocolo de red | ⏳ Pendiente |
| 6 | Servidor multi-hilo y notificaciones | ⏳ Pendiente |
| 7 | Frontend: conexión y patrón Proxy | ⏳ Pendiente |
| 8 | Frontend MVC (funcionalidades del P1) | ⏳ Pendiente |
| 9 | Área de Alertas | ⏳ Pendiente |
| 10 | Estadísticas | ⏳ Pendiente |
| 11 | Validación y errores transversal | ⏳ Pendiente |
| 12 | Pruebas JUnit 5 | ⏳ Pendiente |
| 13 | Entrega y defensa | ⏳ Pendiente |
