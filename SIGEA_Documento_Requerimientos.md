# SIGEA: Sistema Integral de Gestión de Equipos y Activos

## Documento de Especificación de Requerimientos de Software (SRS)

**Versión:** 1.0  
**Fecha:** 13 de febrero de 2026  
**Autor:** Camilo López Romero  
**Institución:** Servicio Nacional de Aprendizaje – SENA  
**Centro:** Centro Industrial y de Desarrollo Empresarial (CIMI)

---

## Tabla de Contenido

1. [Introducción](#1-introducción)
2. [Descripción General del Proyecto](#2-descripción-general-del-proyecto)
3. [Partes Interesadas y Usuarios](#3-partes-interesadas-y-usuarios)
4. [Requerimientos Funcionales](#4-requerimientos-funcionales)
5. [Requerimientos No Funcionales](#5-requerimientos-no-funcionales)
6. [Reglas de Negocio](#6-reglas-de-negocio)
7. [Seguridad](#7-seguridad)
8. [Arquitectura y Tecnología](#8-arquitectura-y-tecnología)
9. [Datos y Almacenamiento](#9-datos-y-almacenamiento)
10. [Integraciones](#10-integraciones)
11. [Interfaz de Usuario y Diseño](#11-interfaz-de-usuario-y-diseño)
12. [Priorización de Funcionalidades (MoSCoW)](#12-priorización-de-funcionalidades-moscow)
13. [Riesgos Identificados](#13-riesgos-identificados)
14. [Restricciones Legales y Regulatorias](#14-restricciones-legales-y-regulatorias)
15. [Mantenimiento y Soporte](#15-mantenimiento-y-soporte)
16. [Criterios de Aceptación y Éxito](#16-criterios-de-aceptación-y-éxito)
17. [Glosario](#17-glosario)

---

## 1. Introducción

### 1.1 Propósito del Documento

El presente documento tiene como propósito describir de manera detallada y formal los requerimientos funcionales y no funcionales del sistema **SIGEA (Sistema Integral de Gestión de Equipos y Activos)**, con el fin de servir como referencia principal para el diseño, desarrollo, pruebas y mantenimiento del software.

### 1.2 Alcance del Sistema

SIGEA es una aplicación web diseñada para gestionar de forma integral el préstamo de herramientas y equipos en los ambientes de formación del SENA. El sistema controla el inventario, registra préstamos y devoluciones, gestiona usuarios con roles diferenciados y genera reportes para la toma de decisiones.

### 1.3 Definición del Problema

En el ambiente de formación de telecomunicaciones del SENA, se presenta una problemática recurrente: la **pérdida de herramientas y equipos prestados** sin que exista un registro formal de a quién se prestaron ni en qué fecha. Esta situación genera:

- **Descuadres de inventario**: No hay certeza sobre la cantidad real de equipos disponibles.
- **Imposibilidad de trazabilidad**: No se puede determinar quién tiene un equipo ni cuándo fue prestado.
- **Pérdida patrimonial**: Equipos que no se devuelven sin posibilidad de recuperación.
- **Confusión operativa**: Incertidumbre sobre si el equipo fue extraviado o simplemente no devuelto.

### 1.4 Objetivo del Software

Proporcionar una herramienta digital que permita llevar un **control preciso y automatizado** de los préstamos de herramientas y equipos, facilitando la trazabilidad, la recuperación oportuna y la gestión eficiente del inventario del centro de formación.

---

## 2. Descripción General del Proyecto

| Campo                     | Detalle                                                        |
| ------------------------- | -------------------------------------------------------------- |
| **Nombre del proyecto**   | SIGEA – Sistema Integral de Gestión de Equipos y Activos       |
| **Tipo de aplicación**    | Aplicación web (responsive)                                    |
| **Disponibilidad**        | 24/7, todos los días del año                                   |
| **Alojamiento**           | Servidor propio del Centro CIMI – SENA                         |
| **Desarrollador**         | Camilo López Romero (aprendiz en etapa de prácticas)           |
| **Fase inicial**          | Ambiente de formación de telecomunicaciones                    |
| **Escalabilidad futura**  | Otros ambientes del centro → otros centros → nivel nacional    |

---

## 3. Partes Interesadas y Usuarios

### 3.1 Stakeholders

| Stakeholder                              | Interés en el proyecto                                     |
| ---------------------------------------- | ---------------------------------------------------------- |
| Instructores de telecomunicaciones       | Beneficiarios directos, administradores del sistema        |
| Coordinación académica del CIMI          | Supervisión del control de activos institucionales         |
| Área de inventarios del SENA             | Alineación con los procesos de control patrimonial         |

### 3.2 Tipos de Usuarios

El sistema contempla **dos roles** con permisos claramente diferenciados:

#### 3.2.1 Administrador

- **Perfil**: Instructores encargados del ambiente de formación.
- **Permisos**:
  - Gestión completa del inventario (crear, editar, eliminar equipos).
  - Gestión completa de usuarios (crear, editar, eliminar, asignar roles).
  - Registro y gestión de préstamos y devoluciones.
  - Creación y administración de ambientes de formación.
  - Generación y exportación de reportes.
  - Visualización del dashboard con estadísticas.
  - Configuración del sistema (parámetros de notificaciones, categorías, etc.).
  - Gestión de transferencias de equipos entre ambientes.

#### 3.2.2 Usuario Estándar

- **Perfil**: Aprendices del centro, instructores de otros ambientes, empleados del centro.
- **Permisos**:
  - Consulta de equipos disponibles.
  - Solicitud de préstamos (sujeta a aprobación del administrador).
  - Visualización de su historial personal de préstamos.
  - Consulta del estado de sus préstamos activos.
  - Reserva anticipada de equipos (sujeta a disponibilidad).

### 3.3 Usuarios Concurrentes Estimados

| Escenario                                    | Usuarios simultáneos estimados |
| -------------------------------------------- | ------------------------------ |
| Actividades prácticas (reparación de redes)  | 5 – 10                        |
| Días regulares de formación teórica          | 1 – 3                         |
| **Requisito mínimo del sistema**             | **Soportar 15 usuarios simultáneos** |

---

## 4. Requerimientos Funcionales

### 4.1 Módulo de Gestión de Inventario

| ID      | Requerimiento                                                                 | Prioridad |
| ------- | ----------------------------------------------------------------------------- | --------- |
| RF-INV-01 | El sistema debe permitir registrar nuevos equipos y herramientas con los campos: nombre, descripción, código único, categoría, estado, cantidad, ubicación (ambiente), fotografía. | Must |
| RF-INV-02 | El sistema debe permitir actualizar la información de equipos existentes.    | Must      |
| RF-INV-03 | El sistema debe permitir eliminar equipos del inventario (eliminación lógica). | Must     |
| RF-INV-04 | El sistema debe mostrar el stock disponible en tiempo real.                   | Must      |
| RF-INV-05 | El sistema debe clasificar equipos por categorías: herramientas manuales, equipos de medición, dispositivos de red, cables y conectores, equipos de protección, otros. | Must |
| RF-INV-06 | El sistema debe asignar un código único e irrepetible a cada elemento.        | Must      |
| RF-INV-07 | El sistema debe permitir el registro fotográfico de cada equipo (máximo 3 fotos por equipo, formatos JPG/PNG, tamaño máximo 5 MB por imagen). | Should |
| RF-INV-08 | El sistema debe permitir la búsqueda rápida de equipos por nombre, código, categoría o estado. | Must |
| RF-INV-09 | El sistema debe permitir filtrar equipos por estado (disponible, prestado, en mantenimiento, dado de baja), categoría y ambiente. | Must |
| RF-INV-10 | El sistema debe registrar un historial de mantenimientos y reparaciones por equipo. | Could |

### 4.2 Módulo de Gestión de Usuarios

| ID      | Requerimiento                                                                 | Prioridad |
| ------- | ----------------------------------------------------------------------------- | --------- |
| RF-USR-01 | El sistema debe permitir registrar nuevos usuarios con los campos: nombre completo, número de documento, tipo de documento, correo electrónico, teléfono, programa de formación, ficha, rol. | Must |
| RF-USR-02 | El sistema debe permitir actualizar la información de usuarios existentes.    | Must      |
| RF-USR-03 | El sistema debe permitir desactivar usuarios (eliminación lógica).            | Must      |
| RF-USR-04 | El sistema debe asignar roles: Administrador o Usuario Estándar.              | Must      |
| RF-USR-05 | El sistema debe mostrar el historial individual de préstamos por usuario.     | Must      |
| RF-USR-06 | El sistema debe permitir la búsqueda rápida de usuarios por nombre, documento o correo. | Must |

### 4.3 Módulo de Gestión de Préstamos

| ID      | Requerimiento                                                                 | Prioridad |
| ------- | ----------------------------------------------------------------------------- | --------- |
| RF-PRE-01 | El sistema debe permitir al usuario estándar **solicitar** un préstamo seleccionando los equipos deseados. | Must |
| RF-PRE-02 | El sistema debe permitir al administrador **aprobar o rechazar** las solicitudes de préstamo. | Must |
| RF-PRE-03 | El sistema debe registrar por cada préstamo: fecha y hora de salida, fecha y hora estimada de devolución, usuario solicitante, administrador que aprueba, equipo(s) prestado(s), observaciones sobre el estado del equipo al prestar. | Must |
| RF-PRE-04 | El sistema debe registrar la devolución con: fecha y hora real de devolución, estado del equipo al devolver, observaciones del administrador, administrador que recibe. | Must |
| RF-PRE-05 | El sistema debe gestionar y listar los préstamos pendientes de devolución.    | Must      |
| RF-PRE-06 | El sistema debe identificar y listar los préstamos vencidos (en mora).        | Must      |
| RF-PRE-07 | El sistema debe permitir la extensión de un préstamo activo, con un máximo de **2 extensiones** por préstamo, sujeta a aprobación del administrador. | Should |
| RF-PRE-08 | El sistema debe permitir registrar un reporte de daño cuando un equipo se devuelve en mal estado, incluyendo: descripción del daño, fotografía del daño (opcional), fecha del reporte. | Should |
| RF-PRE-09 | El sistema debe impedir que un usuario con préstamos vencidos realice nuevas solicitudes hasta regularizar su situación. | Must |
| RF-PRE-10 | El sistema debe descontar automáticamente del stock disponible los equipos prestados y reincorporarlos al devolverse. | Must |

### 4.4 Módulo de Reservas Anticipadas

| ID      | Requerimiento                                                                 | Prioridad |
| ------- | ----------------------------------------------------------------------------- | --------- |
| RF-RES-01 | El sistema debe permitir al usuario reservar equipos con un máximo de **5 días hábiles de anticipación**. | Could |
| RF-RES-02 | Si el usuario no recoge el equipo reservado en las **2 horas** siguientes a la hora de inicio de la reserva, esta se cancela automáticamente. | Could |
| RF-RES-03 | El sistema debe permitir al usuario cancelar una reserva antes de la hora de inicio. | Could |
| RF-RES-04 | El sistema debe mostrar los equipos reservados como "no disponibles" para ese periodo. | Could |

### 4.5 Módulo de Gestión Multi-ambiente

| ID      | Requerimiento                                                                 | Prioridad |
| ------- | ----------------------------------------------------------------------------- | --------- |
| RF-AMB-01 | El sistema debe permitir al administrador crear ambientes de formación con: nombre, ubicación, descripción, instructor responsable. | Should |
| RF-AMB-02 | El sistema debe permitir actualizar y desactivar ambientes de formación.      | Should    |
| RF-AMB-03 | Cada ambiente debe tener su inventario de equipos de forma independiente.     | Should    |
| RF-AMB-04 | El sistema debe permitir transferir equipos entre ambientes, registrando: ambiente origen, ambiente destino, equipo transferido, fecha, administrador que autoriza, motivo. | Should |
| RF-AMB-05 | Un administrador podrá gestionar **únicamente los ambientes asignados a su cargo**. Un superadministrador (el primer administrador del sistema) podrá gestionar todos los ambientes. | Should |

### 4.6 Módulo de Reportes y Exportación

| ID      | Requerimiento                                                                 | Prioridad |
| ------- | ----------------------------------------------------------------------------- | --------- |
| RF-REP-01 | El sistema debe generar un reporte de inventario general con filtros por ambiente, categoría y estado. | Must |
| RF-REP-02 | El sistema debe generar un historial de préstamos con filtros por: usuario, equipo, rango de fechas, estado del préstamo. | Must |
| RF-REP-03 | El sistema debe generar un reporte de equipos más solicitados (ranking).      | Should    |
| RF-REP-04 | El sistema debe generar un reporte de usuarios con préstamos pendientes o vencidos. | Must |
| RF-REP-05 | El sistema debe exportar todos los reportes en formato **XLSX** (Excel).      | Must      |
| RF-REP-06 | El sistema debe exportar todos los reportes en formato **PDF**.               | Must      |
| RF-REP-07 | El sistema debe generar un reporte de transferencias entre ambientes.         | Could     |
| RF-REP-08 | El sistema debe generar un reporte de equipos reportados como dañados.        | Could     |

### 4.7 Módulo de Notificaciones y Alertas

| ID      | Requerimiento                                                                 | Prioridad |
| ------- | ----------------------------------------------------------------------------- | --------- |
| RF-NOT-01 | El sistema debe enviar notificaciones de **recordatorio antes del vencimiento** del préstamo por correo electrónico, con la siguiente lógica escalonada: | Must |
|           | • Préstamos de **5 días o más**: recordatorio con **2 días** de anticipación. | |
|           | • Préstamos de **1 a 4 días**: recordatorio con **5 horas** de anticipación.  | |
|           | • Préstamos por **horas** (menos de 1 día): recordatorio con **45 minutos** de anticipación. | |
| RF-NOT-02 | El sistema debe enviar notificaciones automáticas de **mora** al correo del usuario cuando no devuelva los elementos en la fecha establecida. | Must |
| RF-NOT-03 | El sistema debe enviar **alertas a los administradores** cuando existan equipos en mora. | Must |
| RF-NOT-04 | El sistema debe generar alertas de **stock bajo** cuando las unidades disponibles de un equipo lleguen al umbral mínimo configurado por el administrador. | Should |
| RF-NOT-05 | El sistema debe registrar un **log de todas las notificaciones enviadas** (destinatario, tipo, fecha, estado de envío) para trazabilidad. | Should |
| RF-NOT-06 | Si un usuario no tiene correo electrónico registrado, la notificación se mostrará **dentro del sistema** como alerta interna. | Should |

### 4.8 Módulo de Dashboard

| ID      | Requerimiento                                                                 | Prioridad |
| ------- | ----------------------------------------------------------------------------- | --------- |
| RF-DSH-01 | El sistema debe mostrar un panel principal con estadísticas visuales: total de equipos, equipos disponibles, equipos prestados, equipos en mora, equipos en mantenimiento. | Must |
| RF-DSH-02 | El dashboard debe mostrar gráficos de préstamos por periodo (diario, semanal, mensual). | Should |
| RF-DSH-03 | El dashboard debe mostrar los últimos préstamos realizados.                   | Should    |
| RF-DSH-04 | El dashboard debe mostrar alertas activas (equipos en mora, stock bajo).      | Must      |

---

## 5. Requerimientos No Funcionales

### 5.1 Rendimiento

| ID       | Requerimiento                                                                | Valor objetivo    |
| -------- | ---------------------------------------------------------------------------- | ----------------- |
| RNF-REN-01 | Tiempo de carga por página                                                 | < 2 segundos      |
| RNF-REN-02 | Tiempo de respuesta de búsquedas                                           | < 1 segundo       |
| RNF-REN-03 | Tiempo de generación de reportes                                           | < 5 segundos      |
| RNF-REN-04 | Usuarios simultáneos soportados                                            | Mínimo 15         |

### 5.2 Disponibilidad

| ID       | Requerimiento                                                                | Valor objetivo    |
| -------- | ---------------------------------------------------------------------------- | ----------------- |
| RNF-DIS-01 | Disponibilidad del sistema                                                 | 24/7, 99.5% uptime anual |
| RNF-DIS-02 | Tiempo máximo de inactividad planificada por mantenimiento                  | 4 horas/mes       |

### 5.3 Compatibilidad

| ID       | Requerimiento                                              |
| -------- | ---------------------------------------------------------- |
| RNF-COM-01 | Sistemas operativos: Windows, Linux, macOS, iOS, Android |
| RNF-COM-02 | Navegadores: Chrome, Firefox, Safari, Edge (últimas 2 versiones estables) |
| RNF-COM-03 | Resoluciones: desde 320px (móvil) hasta 1920px (escritorio) |

### 5.4 Escalabilidad

| ID       | Requerimiento                                                                |
| -------- | ---------------------------------------------------------------------------- |
| RNF-ESC-01 | El sistema debe soportar un crecimiento de hasta 5,000 registros sin degradación de rendimiento. |
| RNF-ESC-02 | La arquitectura debe permitir escalar horizontalmente para soportar múltiples centros de formación a futuro. |
| RNF-ESC-03 | El diseño de la base de datos debe ser multi-tenant para facilitar la expansión a otros centros. |

### 5.5 Mantenibilidad

| ID       | Requerimiento                                                                |
| -------- | ---------------------------------------------------------------------------- |
| RNF-MAN-01 | El código debe seguir estándares de codificación documentados.              |
| RNF-MAN-02 | El sistema debe contar con documentación técnica completa.                  |
| RNF-MAN-03 | El sistema debe contar con manual de usuario.                               |
| RNF-MAN-04 | Se debe utilizar control de versiones (Git) durante todo el desarrollo.     |

---

## 6. Reglas de Negocio

### 6.1 Reglas de Préstamos

| ID      | Regla                                                                         |
| ------- | ----------------------------------------------------------------------------- |
| RN-01   | Un usuario con préstamos vencidos **no puede** solicitar nuevos préstamos hasta regularizar su situación. |
| RN-02   | Cada préstamo puede ser extendido un máximo de **2 veces**, sujeto a aprobación del administrador. |
| RN-03   | Al registrar un préstamo, el administrador debe documentar el **estado del equipo** al momento de la entrega. |
| RN-04   | Al registrar una devolución, el administrador debe verificar y documentar el **estado del equipo** al momento de la recepción. |
| RN-05   | Si un equipo se devuelve dañado, se debe generar un **reporte de daño** antes de reincorporarlo al inventario. |
| RN-06   | Solo el administrador puede aprobar o rechazar solicitudes de préstamo.       |

### 6.2 Reglas de Reservas

| ID      | Regla                                                                         |
| ------- | ----------------------------------------------------------------------------- |
| RN-07   | Las reservas se pueden realizar con un máximo de **5 días hábiles** de anticipación. |
| RN-08   | Si el usuario no recoge el equipo reservado dentro de las **2 horas** siguientes al inicio de la reserva, esta se cancela automáticamente y el equipo vuelve a estar disponible. |

### 6.3 Reglas de Inventario

| ID      | Regla                                                                         |
| ------- | ----------------------------------------------------------------------------- |
| RN-09   | Los equipos eliminados no se borran físicamente de la base de datos; se marcan como **dados de baja** (eliminación lógica). |
| RN-10   | Las transferencias de equipos entre ambientes deben ser autorizadas por el administrador del ambiente de origen. |
| RN-11   | El código único de cada equipo es **irrepetible** y no se puede reasignar.    |

### 6.4 Reglas de Usuarios

| ID      | Regla                                                                         |
| ------- | ----------------------------------------------------------------------------- |
| RN-12   | La información de aprendices registrados se conservará en el sistema por un periodo máximo de **2 años** (duración máxima de un programa de nivel tecnológico), después del cual se archivará. |
| RN-13   | Un usuario desactivado no puede iniciar sesión ni realizar solicitudes.       |

### 6.5 Reglas de Notificaciones

| ID      | Regla                                                                         |
| ------- | ----------------------------------------------------------------------------- |
| RN-14   | **Préstamos ≥ 5 días**: recordatorio con 2 días de anticipación al vencimiento. |
| RN-15   | **Préstamos de 1 a 4 días**: recordatorio con 5 horas de anticipación al vencimiento. |
| RN-16   | **Préstamos por horas** (< 1 día): recordatorio con 45 minutos de anticipación al vencimiento. |
| RN-17   | Las notificaciones de mora se envían **diariamente** mientras el préstamo siga vencido. |

---

## 7. Seguridad

### 7.1 Autenticación

| ID       | Requerimiento                                                                |
| -------- | ---------------------------------------------------------------------------- |
| RS-AUT-01 | El acceso al sistema requiere autenticación mediante **usuario y contraseña**. |
| RS-AUT-02 | Las contraseñas deben cumplir la siguiente política: mínimo **8 caracteres**, al menos **1 mayúscula**, al menos **1 minúscula**, al menos **1 número**, al menos **2 caracteres especiales**. |
| RS-AUT-03 | Tras **3 intentos fallidos** consecutivos de inicio de sesión, la cuenta se bloqueará temporalmente durante **5 minutos**. Después de un segundo bloqueo consecutivo, el tiempo incrementa a **15 minutos**. | 
| RS-AUT-04 | La sesión expirará automáticamente tras **30 minutos de inactividad** continua. |
| RS-AUT-05 | El sistema mostrará una advertencia **2 minutos antes** del cierre de sesión por inactividad, permitiendo al usuario extender o cerrar la sesión. |

### 7.2 Cifrado y Protección de Datos

| ID       | Requerimiento                                                                |
| -------- | ---------------------------------------------------------------------------- |
| RS-CIF-01 | Las contraseñas deben almacenarse utilizando un algoritmo de **hash seguro** (BCryptPasswordEncoder de Spring Security), nunca en texto plano. |
| RS-CIF-02 | Toda comunicación entre cliente y servidor debe realizarse mediante **HTTPS (TLS 1.2 o superior)**. |
| RS-CIF-03 | Los datos personales sensibles (documento de identidad, correo, teléfono) deben contar con protección de cifrado en la base de datos. |

### 7.3 Autorización

| ID       | Requerimiento                                                                |
| -------- | ---------------------------------------------------------------------------- |
| RS-AUZ-01 | El sistema debe validar el rol del usuario en **cada solicitud** para garantizar que solo accede a funcionalidades autorizadas. |
| RS-AUZ-02 | Las rutas y endpoints de administración deben estar protegidos y ser inaccesibles para usuarios estándar. |

### 7.4 Auditoría

| ID       | Requerimiento                                                                |
| -------- | ---------------------------------------------------------------------------- |
| RS-AUD-01 | El sistema debe registrar un **log de auditoría** con las acciones críticas: inicio de sesión, cierre de sesión, creación/modificación/eliminación de registros, aprobación/rechazo de préstamos. |
| RS-AUD-02 | Los logs de auditoría deben incluir: usuario, acción, fecha y hora, dirección IP. |

---

## 8. Arquitectura y Tecnología

### 8.1 Tipo de Aplicación

**Aplicación web responsive** accesible desde cualquier navegador moderno en dispositivos de escritorio y móviles.

### 8.2 Stack Tecnológico

Se ha seleccionado un stack **Java + Angular** orientado a la escalabilidad empresarial y la robustez a largo plazo. Esta arquitectura sigue el estándar de la industria para aplicaciones corporativas, permite que el sistema crezca desde un ambiente local hasta una plataforma a nivel nacional, y cuenta con uno de los ecosistemas más maduros y documentados del mercado.

#### 8.2.1 Stack Principal

| Capa                     | Tecnología                                         | Justificación                                                       |
| ------------------------ | -------------------------------------------------- | ------------------------------------------------------------------- |
| **Lenguaje backend**     | Java 21 (LTS)                                      | Lenguaje empresarial por excelencia, tipado fuerte, alto rendimiento, soporte a largo plazo (LTS) |
| **Framework backend**    | Spring Boot 3.3+                                   | Framework empresarial #1 en Java, autoconfiguración, arquitectura modular, ecosistema masivo |
| **Lenguaje frontend**    | TypeScript                                         | Tipado estricto para el frontend, detección de errores en compilación |
| **Framework frontend**   | Angular 17+                                        | Framework empresarial de Google, arquitectura basada en componentes y módulos, CLI potente, inyección de dependencias nativa |
| **UI Components**        | Angular Material + Tailwind CSS                    | Componentes Material Design accesibles, diseño responsive utility-first |
| **Estado del cliente**   | NgRx o RxJS Signals                                | Gestión de estado reactiva, patrón Redux para Angular, caché de datos |
| **API**                  | REST API con documentación Swagger/OpenAPI (SpringDoc) | Estándar de la industria, autodocumentada, facilita integraciones futuras |
| **Base de datos**        | MariaDB 11+                                        | Fork open source de MySQL, alta compatibilidad, rendimiento optimizado, amplio soporte en hosting |
| **ORM / Persistencia**   | Spring Data JPA + Hibernate                        | Estándar JPA de Java, mapeo objeto-relacional maduro, consultas JPQL y nativas, migraciones con Flyway |
| **Migraciones de BD**    | Flyway                                             | Control de versiones de la base de datos, migraciones reproducibles |
| **Autenticación**        | Spring Security + JWT                              | El framework de seguridad más robusto del ecosistema Java, filtros por roles, protección CSRF, OAuth2 extensible a futuro |
| **Validación**           | Jakarta Bean Validation (Hibernate Validator)      | Validación de DTOs con anotaciones (@NotNull, @Size, @Email), estándar Java |
| **Correo**               | Spring Boot Mail (JavaMailSender)                  | Soporte SMTP nativo, plantillas HTML con Thymeleaf, envío asíncrono |
| **Reportes PDF**         | JasperReports / iText / OpenPDF                    | Estándar empresarial para generación de reportes PDF en Java        |
| **Reportes XLSX**        | Apache POI                                         | Librería estándar Java para generación y manipulación de archivos Excel |
| **Almacenamiento**       | Sistema de archivos del servidor                   | Local inicialmente, migrable a almacenamiento de objetos (MinIO/S3) a futuro |
| **Tareas programadas**   | Spring @Scheduled (Cron)                           | Notificaciones automáticas, recordatorios, limpieza de datos, backups |
| **Logging**              | SLF4J + Logback (incluido en Spring Boot)          | Logs estructurados, rotación automática, niveles configurables      |
| **Testing**              | JUnit 5 + Mockito + Spring Boot Test               | Tests unitarios e integración, mocking, testing de endpoints REST   |
| **Testing frontend**     | Karma + Jasmine (incluido en Angular)              | Tests unitarios de componentes y servicios Angular                  |
| **Build backend**        | Maven                                              | Gestión de dependencias y ciclo de vida del proyecto Java           |
| **Build frontend**       | Angular CLI + npm                                  | Compilación, bundling y optimización del frontend                   |
| **Control de versiones** | Git + GitHub/GitLab                                | Versionado, CI/CD a futuro                                          |

#### 8.2.2 Herramientas de Desarrollo y DevOps

| Herramienta                | Propósito                                                             |
| -------------------------- | --------------------------------------------------------------------- |
| **Docker + Docker Compose** | Contenerización del sistema completo (app + BD + servicios), garantiza consistencia entre desarrollo y producción |
| **IntelliJ IDEA / VS Code** | IDEs con soporte nativo para Java/Spring Boot y Angular              |
| **Swagger UI (SpringDoc)** | Documentación interactiva de la API REST generada automáticamente    |
| **DBeaver / HeidiSQL**     | Administración visual de MariaDB                                     |
| **Postman / Insomnia**     | Testing manual de endpoints de la API                                |
| **SonarLint**              | Análisis estático de calidad de código en el IDE                     |
| **Checkstyle / SpotBugs**  | Validación de estándares de código Java                              |

#### 8.2.3 Justificación de la Arquitectura

**¿Por qué Spring Boot?**
- El framework empresarial más utilizado en Java a nivel mundial.
- Arquitectura modular: cada funcionalidad (inventario, préstamos, usuarios, notificaciones) es un paquete/módulo independiente con su propio controller, service y repository.
- Inyección de dependencias nativa y autoconfiguración.
- Spring Security es el sistema de seguridad más completo: autenticación, autorización por roles, protección CSRF, rate limiting.
- Spring @Scheduled para tareas programadas (notificaciones de mora, recordatorios, backups).
- Ecosistema inmenso: más de 20 años de madurez, documentación exhaustiva, comunidad global.
- Soporte nativo para WebSockets (notificaciones en tiempo real a futuro), colas de mensajes (RabbitMQ/Kafka), caché (Redis).
- Perfilado y monitoreo con Spring Actuator.

**¿Por qué Angular?**
- Framework empresarial de Google con soporte a largo plazo (LTS).
- Arquitectura basada en módulos y componentes, ideal para aplicaciones que crecen.
- Inyección de dependencias nativa (mismo concepto que Spring Boot).
- Angular CLI genera componentes, servicios, guards y módulos con un solo comando.
- Router Guards para protección de rutas por rol (admin/usuario estándar).
- HttpInterceptors para adjuntar JWT automáticamente a cada petición.
- Lazy Loading de módulos para optimizar tiempos de carga.
- Formularios reactivos con validación integrada.

**¿Por qué Java?**
- Lenguaje #1 en desarrollo empresarial a nivel mundial.
- Tipado fuerte y compilado: errores detectados antes de ejecutar.
- Java 21 LTS: soporte hasta 2031, virtual threads para alta concurrencia.
- Es el lenguaje que más se enseña en el SENA en programas de desarrollo de software.
- Amplia comunidad en Colombia y Latinoamérica, facilitando el soporte futuro.
- Ideal para sistemas que deben escalar a nivel institucional/nacional.

**¿Por qué MariaDB?**
- Fork open source de MySQL, 100% compatible con herramientas y drivers MySQL.
- Rendimiento superior a MySQL en consultas complejas.
- Storage engines avanzados (Aria, ColumnStore para analytics a futuro).
- Licencia GPL: libre para uso institucional sin costos de licenciamiento.
- Menor consumo de recursos que PostgreSQL, ideal para un servidor local.
- Amplio soporte en hosting y servidores Linux.

**¿Por qué Docker?**
- El sistema se despliega con un solo comando (`docker compose up`).
- Reproducible en cualquier servidor (CIMI, otro centro, nube).
- Aislamiento: la BD, el backend y el frontend corren en contenedores independientes.
- Facilita la migración futura a infraestructura cloud si es necesario.

### 8.3 Infraestructura

| Aspecto                        | Detalle                                                     |
| ------------------------------ | ----------------------------------------------------------- |
| **Servidor**                   | Servidor propio del Centro CIMI – SENA                      |
| **Infraestructura existente**  | No existe actualmente; debe aprovisionarse                  |
| **Sistema operativo servidor** | Linux (Ubuntu Server 24.04 LTS)                             |
| **Contenerización**            | Docker Engine + Docker Compose                              |
| **Reverse proxy**              | Nginx (como contenedor o en el host)                        |
| **Certificado SSL**            | Let's Encrypt (HTTPS) o certificado institucional           |
| **Runtime Java**               | JDK 21 LTS (Eclipse Temurin / Amazon Corretto)              |
| **Servidor de aplicación**     | Tomcat embebido en Spring Boot (incluido)                   |
| **Base de datos**              | MariaDB 11+ en contenedor Docker con volumen persistente    |
| **Backups automáticos**        | mariadb-dump ejecutado por cron del sistema operativo       |

#### 8.3.1 Arquitectura de Despliegue

```
┌──────────────────────────────────────────────────────────────┐
│                  Servidor CIMI - SENA                         │
│                  Ubuntu Server 24.04 LTS                      │
│                                                               │
│  ┌──────────┐    ┌────────────────┐    ┌──────────────────┐  │
│  │  Nginx   │───▶│  Angular       │    │  Spring Boot     │  │
│  │ (Proxy)  │    │  (static)      │───▶│  (API REST)      │  │
│  │ :80/:443 │    │  :4200/dist    │    │  :8080           │  │
│  └──────────┘    └────────────────┘    └────────┬─────────┘  │
│                                                  │            │
│                                        ┌─────────▼─────────┐ │
│                                        │   MariaDB 11+     │ │
│                                        │   :3306            │ │
│                                        └───────────────────┘ │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │          Docker Compose (orquestación)                  │  │
│  └─────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

> **Nota:** En producción, Angular se compila a archivos estáticos (`ng build --production`) y se sirve directamente desde Nginx. Spring Boot corre como un JAR ejecutable con Tomcat embebido.

#### 8.3.2 Requisitos Mínimos del Servidor

| Recurso        | Mínimo recomendado                        |
| -------------- | ----------------------------------------- |
| **CPU**        | 2 cores                                   |
| **RAM**        | 4 GB (recomendado 8 GB para Java + BD)    |
| **Disco**      | 50 GB SSD                                 |
| **Red**        | Conexión a la red institucional del SENA  |
| **SO**         | Ubuntu Server 24.04 LTS                   |
| **JDK**        | Java 21 LTS (Eclipse Temurin / Corretto)  |
| **Docker**     | Docker Engine 24+ y Docker Compose v2     |

### 8.4 Configuración de Correo Electrónico

| Aspecto                  | Detalle                                                   |
| ------------------------ | --------------------------------------------------------- |
| **Servidor SMTP**        | Se utilizará el servidor SMTP institucional del SENA. Si no está disponible, se configurará un servicio SMTP local en el servidor (ej: Postfix). Integrado con JavaMailSender de Spring Boot. |
| **Fallback**             | Si el usuario no tiene correo registrado, las notificaciones se mostrarán como alertas internas en el sistema. |
| **Trazabilidad**         | Se registra cada envío de correo (destinatario, tipo, fecha, estado). |

---

## 9. Datos y Almacenamiento

### 9.1 Tipos de Información

El sistema manejará los siguientes tipos de datos:

- **Datos personales**: Nombre, documento de identidad, correo electrónico, teléfono, programa de formación.
- **Datos de equipos**: Nombre, código, descripción, categoría, estado, cantidad, ubicación, fotografías.
- **Datos transaccionales**: Registros de préstamos, devoluciones, reservas, transferencias.
- **Datos estadísticos**: Reportes, logs de auditoría, historial de notificaciones.

### 9.2 Volumen Estimado de Datos

| Periodo        | Registros estimados | Observación                          |
| -------------- | ------------------- | ------------------------------------ |
| Primer año     | ~800 registros      | Uso local, un ambiente               |
| Segundo año    | ~2,000 registros    | Expansión a más ambientes            |
| A futuro       | ~10,000+ registros  | Expansión a nivel municipal/nacional |

### 9.3 Almacenamiento de Imágenes

| Parámetro         | Valor                          |
| ------------------ | ------------------------------ |
| Formatos aceptados | JPG, PNG                       |
| Tamaño máximo      | 5 MB por imagen                |
| Máximo por equipo  | 3 fotografías                  |
| Almacenamiento     | Sistema de archivos del servidor (no en la BD) |

### 9.4 Respaldos (Backups)

| Tipo de respaldo     | Frecuencia        | Detalle                                      |
| -------------------- | ------------------ | -------------------------------------------- |
| **Incremental**      | Diario             | Respalda solo los datos modificados en el día |
| **Completo**         | Semanal (domingos) | Respaldo completo de toda la base de datos    |
| **Retención**        | 3 meses            | Se conservan los respaldos de los últimos 3 meses |
| **Ubicación**        | Disco externo o segunda unidad del servidor | Almacenamiento independiente |

> **Nota:** La frecuencia de respaldo quincenal propuesta inicialmente se ha mejorado a respaldos **diarios incrementales y semanales completos**, dado que el sistema opera 24/7 y la pérdida de 2 semanas de datos sería crítica para la operación.

### 9.5 Retención de Datos

| Tipo de dato             | Tiempo de retención | Acción al expirar            |
| ------------------------ | ------------------- | ---------------------------- |
| Datos de aprendices      | 2 años              | Se archivan (no se eliminan) |
| Historial de préstamos   | 5 años              | Se archivan                  |
| Logs de auditoría        | 3 años              | Se archivan                  |
| Respaldos                | 3 meses             | Se eliminan los más antiguos |

---

## 10. Integraciones

### 10.1 Integraciones Actuales

El sistema **no requiere integraciones** con otros sistemas en su versión inicial.

### 10.2 Integraciones Futuras (Planificadas)

Cuando el sistema sea adoptado por otros centros de formación del SENA a nivel nacional, se contempla la integración con:

| Sistema                | Tipo de integración    | Propósito                                    |
| ---------------------- | ---------------------- | -------------------------------------------- |
| **Sofía Plus**         | API REST               | Consulta de datos de aprendices y programas de formación |
| **Compromiso**         | API REST               | Integración con gestión administrativa       |
| **Inventario SENA**    | API REST / Archivo     | Sincronización con el sistema de inventario corporativo |

> **Nota:** Estas integraciones están fuera del alcance de la versión 1.0 y se desarrollarán bajo demanda institucional.

---

## 11. Interfaz de Usuario y Diseño

### 11.1 Lineamientos Generales

- **Diseño desde cero**: No existen mockups ni diseños previos.
- **Responsive**: La interfaz debe adaptarse completamente a cualquier dispositivo (móvil, tablet, escritorio).
- **Usabilidad**: Diseño intuitivo, con navegación clara y mínima curva de aprendizaje.
- **Accesibilidad**: En la versión inicial se seguirán principios básicos de accesibilidad. Al integrarse a nivel institucional, se aplicará de forma obligatoria la **Ley 1712 de 2014** y los estándares **WCAG 2.1 nivel AA**.

### 11.2 Identidad Visual – Lineamientos de Marca SENA

El diseño debe seguir la identidad corporativa del SENA:

| Elemento          | Especificación                                    |
| ----------------- | ------------------------------------------------- |
| **Colores primarios** | Verde institucional SENA (#39A900), Negro (#000000), Blanco (#FFFFFF) |
| **Colores secundarios** | Gris oscuro (#333333), Gris claro (#F5F5F5) |
| **Logotipo**      | Logo oficial del SENA, visible en header y reportes |
| **Tipografía**    | Según manual de identidad visual del SENA         |

> **Nota:** Los colores exactos y el logotipo deben validarse con el manual de identidad visual vigente del SENA.

### 11.3 Estructura de Navegación Propuesta

```
├── Login
├── Dashboard (inicio)
├── Inventario
│   ├── Lista de equipos
│   ├── Registrar equipo
│   ├── Detalle de equipo
│   └── Categorías
├── Préstamos
│   ├── Solicitudes pendientes (admin)
│   ├── Préstamos activos
│   ├── Historial de préstamos
│   └── Registrar devolución (admin)
├── Reservas
│   ├── Nueva reserva
│   └── Mis reservas
├── Ambientes
│   ├── Lista de ambientes
│   ├── Inventario por ambiente
│   └── Transferencias
├── Usuarios (admin)
│   ├── Lista de usuarios
│   └── Registrar usuario
├── Reportes
│   ├── Inventario
│   ├── Préstamos
│   └── Exportar
├── Notificaciones
└── Configuración (admin)
```

---

## 12. Priorización de Funcionalidades (MoSCoW)

### Must Have (Obligatorio – MVP v1.0)
Funcionalidades sin las cuales el sistema no cumple su propósito básico:

- ✅ Gestión de inventario (CRUD de equipos)
- ✅ Gestión de usuarios (CRUD con roles)
- ✅ Gestión de préstamos (solicitud, aprobación, devolución)
- ✅ Dashboard con estadísticas básicas
- ✅ Reportes básicos (inventario, préstamos)
- ✅ Exportación a XLSX y PDF
- ✅ Notificaciones de mora por correo electrónico
- ✅ Autenticación con usuario y contraseña
- ✅ Política de contraseñas y bloqueo de cuenta
- ✅ Diseño responsive

### Should Have (Debería tener – v1.1)
Funcionalidades importantes pero no bloqueantes para el lanzamiento:

- 📌 Gestión multi-ambiente
- 📌 Notificaciones de recordatorio antes del vencimiento
- 📌 Alertas de stock bajo
- 📌 Registro fotográfico de equipos
- 📌 Reporte de equipos más solicitados
- 📌 Log de notificaciones enviadas
- 📌 Gráficos en el dashboard

### Could Have (Podría tener – v2.0)
Funcionalidades deseables si el tiempo lo permite:

- 💡 Sistema de reservas anticipadas
- 💡 Historial de mantenimientos y reparaciones
- 💡 Reporte de transferencias entre ambientes
- 💡 Reporte de equipos dañados
- 💡 Búsqueda avanzada con filtros combinados

### Won't Have (No se hará en esta fase)

- ❌ Integración con Sofía Plus, Compromiso o inventario corporativo del SENA
- ❌ Accesibilidad WCAG 2.1 nivel AA completa (se aplica al integrarse institucionalmente)
- ❌ Aplicación móvil nativa (se usa la versión web responsive)

---

## 13. Riesgos Identificados

| ID   | Riesgo                               | Probabilidad | Impacto | Mitigación                                                          |
| ---- | ------------------------------------ | ------------ | ------- | ------------------------------------------------------------------- |
| R-01 | **Desarrollador único**: Todo el desarrollo depende de una sola persona (aprendiz en prácticas). Si se presenta incapacidad o retraso, no hay respaldo. | Media | Alto | Documentar todo el proceso de desarrollo. Mantener código limpio y versionado en Git. Priorizar el MVP. |
| R-02 | **Infraestructura no disponible**: El servidor del centro no existe aún. Si no se aprovisiona a tiempo, el despliegue se retrasa. | Media | Alto | Utilizar un entorno de desarrollo local y/o un servicio de hosting temporal durante el desarrollo. Gestionar la solicitud del servidor con anticipación. |
| R-03 | **Resistencia al cambio**: Los usuarios pueden resistirse a usar un sistema nuevo si están acostumbrados a prestar equipos sin registro formal. | Media | Medio | Involucrar a los instructores desde etapas tempranas. Realizar capacitación práctica. Diseñar una interfaz simple e intuitiva. |
| R-04 | **Continuidad post-prácticas**: Al terminar la etapa de prácticas del desarrollador, el mantenimiento del sistema podría quedar sin responsable. | Alta | Alto | Generar documentación técnica completa. Escribir código mantenible. Capacitar a un instructor o compañero como administrador técnico básico. |
| R-05 | **Servidor SMTP no disponible**: Si el SENA no provee un servidor SMTP institucional, las notificaciones por correo no funcionarán. | Media | Medio | Implementar notificaciones internas como alternativa. Evaluar servicios SMTP gratuitos como fallback. |
| R-06 | **Alcance excesivo**: El volumen de funcionalidades puede superar la capacidad de un solo desarrollador en el periodo de prácticas. | Alta | Alto | Aplicar priorización MoSCoW. Desarrollar primero el MVP (Must Have). Dejar funciones Could/Won't para futuras iteraciones. |

---

## 14. Restricciones Legales y Regulatorias

| Aspecto                        | Detalle                                                       |
| ------------------------------ | ------------------------------------------------------------- |
| **Propiedad intelectual**      | Todo el desarrollo y el código fuente son propiedad exclusiva del SENA. |
| **Protección de datos**        | El sistema debe cumplir con la **Ley 1581 de 2012** (Protección de Datos Personales de Colombia) para el manejo de datos personales de los usuarios. |
| **Transparencia**              | A futuro, al integrarse institucionalmente, debe cumplir con la **Ley 1712 de 2014** (Ley de Transparencia y Acceso a la Información Pública). |
| **Accesibilidad**              | Al integrarse a plataformas oficiales del SENA, debe cumplir estándares **WCAG 2.1 nivel AA**. |
| **Uso exclusivo institucional**| El sistema es de uso exclusivo del SENA. No puede ser comercializado ni distribuido fuera de la institución. |

---

## 15. Mantenimiento y Soporte

### 15.1 Responsable de Mantenimiento

| Fase                          | Responsable                                    |
| ----------------------------- | ---------------------------------------------- |
| Durante el desarrollo         | Camilo López Romero (aprendiz desarrollador)   |
| Post-lanzamiento inicial      | Camilo López Romero                            |
| A largo plazo                 | Equipo técnico asignado por el SENA            |

### 15.2 Capacitación

| Audiencia        | Tipo de capacitación                                    | Duración estimada |
| ---------------- | ------------------------------------------------------- | ----------------- |
| Administradores  | Capacitación presencial sobre todas las funcionalidades del sistema, gestión de usuarios, reportes y configuración. | 4 horas |
| Usuarios estándar | Guía rápida de uso incluida en el sistema (sección de ayuda). | Autoguiada |

### 15.3 Documentación Entregable

| Documento                    | Descripción                                              |
| ---------------------------- | -------------------------------------------------------- |
| **Documentación técnica**    | Arquitectura, modelo de datos, API endpoints, guía de instalación y despliegue, configuración del servidor. |
| **Manual de usuario**        | Guía paso a paso de todas las funcionalidades, con capturas de pantalla. |
| **README del proyecto**      | Instrucciones de instalación, configuración y ejecución para desarrolladores futuros. |

---

## 16. Criterios de Aceptación y Éxito

### 16.1 Criterios de Aceptación

| Criterio                                                                     | Umbral     |
| ---------------------------------------------------------------------------- | ---------- |
| Porcentaje de funcionalidades Must Have implementadas y funcionales          | ≥ 100%     |
| Porcentaje de funcionalidades Should Have implementadas y funcionales        | ≥ 60%      |
| Bugs críticos en producción                                                  | 0          |
| Bugs menores aceptados en producción                                         | ≤ 5        |
| Tiempo de carga por página                                                   | < 2 seg    |

### 16.2 Criterios de Éxito

El proyecto se considerará exitoso cuando:

1. **Al menos el 80% de las funcionalidades totales** funcionen correctamente sin bugs críticos.
2. Los administradores puedan registrar préstamos y llevar control del inventario de forma digital.
3. Se reduzca significativamente la pérdida de equipos por falta de trazabilidad.
4. Las notificaciones de mora lleguen correctamente a los usuarios.
5. Los reportes se generen y exporten correctamente en XLSX y PDF.

---

## 17. Glosario

| Término              | Definición                                                                |
| -------------------- | ------------------------------------------------------------------------- |
| **Ambiente**         | Espacio físico del SENA destinado a la formación (ej: laboratorio de telecomunicaciones). |
| **Aprendiz**         | Estudiante inscrito en un programa de formación del SENA.                |
| **CIMI**             | Centro Industrial y de Desarrollo Empresarial del SENA.                  |
| **Ficha**            | Identificador único del grupo de formación al que pertenece un aprendiz. |
| **Mora**             | Estado de un préstamo que no fue devuelto en la fecha establecida.       |
| **MVP**              | Producto Mínimo Viable (Minimum Viable Product). Versión con las funcionalidades esenciales. |
| **MoSCoW**           | Método de priorización: Must, Should, Could, Won't.                      |
| **Préstamo**         | Acción de entregar temporalmente un equipo o herramienta a un usuario.   |
| **SENA**             | Servicio Nacional de Aprendizaje, entidad pública de formación profesional de Colombia. |
| **Sofía Plus**       | Plataforma oficial del SENA para gestión académica.                      |
| **Stock**            | Cantidad disponible de un equipo o herramienta en inventario.            |
| **WCAG 2.1**         | Web Content Accessibility Guidelines, estándares de accesibilidad web.   |

---

## Historial de Versiones

| Versión | Fecha              | Autor                 | Descripción                          |
| ------- | ------------------ | --------------------- | ------------------------------------ |
| 1.0     | 13 de febrero de 2026 | Camilo López Romero | Creación del documento inicial     |

---

> **Nota final:** Este documento es un artefacto vivo que puede ser actualizado conforme avance el desarrollo del proyecto y se identifiquen nuevos requerimientos o cambios en los existentes. Cualquier modificación debe quedar registrada en el historial de versiones.
