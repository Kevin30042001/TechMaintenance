# 🔧 TechMaintenance

> **Sistema de Gestión de Mantenimiento Técnico** — Aplicación Android nativa para administrar órdenes de servicio, clientes, equipos y técnicos en tiempo real.

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Java](https://img.shields.io/badge/Language-Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com)
[![API Level](https://img.shields.io/badge/Min%20API-29%20(Android%2010)-blue?style=for-the-badge)](https://apilevels.com)

---

## 📋 Tabla de Contenidos

- [Descripción](#-descripción)
- [Características](#-características)
- [Tecnologías](#-tecnologías)
- [Arquitectura](#-arquitectura)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Requisitos Previos](#-requisitos-previos)
- [Instalación](#-instalación)
- [Configuración de Firebase](#-configuración-de-firebase)
- [Módulos del Sistema](#-módulos-del-sistema)
- [Roles de Usuario](#-roles-de-usuario)
- [Base de Datos](#-base-de-datos-firestore)
- [Cloud Functions](#-cloud-functions)
- [Contribución](#-contribución)

---

## 📖 Descripción

**TechMaintenance** es una aplicación Android empresarial diseñada para gestionar operaciones de mantenimiento técnico de forma eficiente. Permite al **Administrador** coordinar clientes, equipos y técnicos, mientras los **Técnicos** pueden ver y completar sus órdenes de trabajo desde su dispositivo móvil.

El sistema incluye un flujo completo de validación de servicios: el técnico completa el trabajo → el cliente recibe un código por email → el cliente valida el servicio desde la app o desde un link web → el administrador ve el estado en tiempo real.

---

## ✨ Características

### 🔐 Autenticación & Seguridad
- Login con email y contraseña via **Firebase Auth**
- **Autenticación biométrica** (huella digital) con opción "Recordarme"
- Recuperación de contraseña con **código de verificación por email**
- **Custom Claims** de Firebase para control de roles (`admin` / `tecnico`)
- Redirección automática al dashboard según el rol del usuario

### 👨‍💼 Panel de Administrador
- **Dashboard** con métricas en tiempo real:
  - Mantenimientos completados (mes actual)
  - Servicios en proceso
  - Servicios pendientes (semana actual)
  - Calificación promedio del servicio
- Estadísticas rápidas: técnicos activos, total de equipos, total de clientes
- Vista de próximos mantenimientos programados
- Notificaciones con badge de no leídas

### 👷 Panel de Técnico
- Dashboard personalizado con órdenes asignadas
- Flujo completo para **completar un servicio**:
  - Registro de trabajo realizado
  - Adjuntar fotos de evidencia
  - Generar código de validación único
- Historial de servicios realizados

### 📊 Gestión de Datos
| Módulo | Funcionalidades |
|--------|----------------|
| **Clientes** | Agregar, editar, ver detalle, listar con búsqueda |
| **Equipos** | Registrar con foto (cámara/galería), historial de mantenimientos |
| **Técnicos** | Crear cuentas, activar/desactivar, enviar credenciales por email |
| **Mantenimientos** | Crear órdenes, asignar técnico, programar fecha, seguimiento de estado |

### ✅ Validación de Servicios
- El cliente puede validar el servicio mediante:
  - **App Android**: pantalla de validación con código
  - **Web**: link único generado (`techmaintenance.web.app/validar?...`)
- Sistema de calificación del servicio (1-5 estrellas)
- Deep Links configurados para abrir la app desde el navegador

### 📅 Calendario
- Vista de mantenimientos programados por día/semana/mes

### 📈 Reportes (Solo Admin)
- Generación de reportes en **PDF** (iText7)
- Exportación a **Excel** (Apache POI)
- Filtros por rango de fechas, técnico, estado

### 🔔 Notificaciones Push
- **Firebase Cloud Messaging (FCM)** para notificaciones en tiempo real
- Notificación al técnico cuando se asigna un nuevo mantenimiento
- Notificación al admin cuando un técnico completa un servicio

---

## 🛠 Tecnologías

### Android / Frontend
| Librería | Versión | Uso |
|----------|---------|-----|
| `androidx.appcompat` | 1.6.1 | Compatibilidad base |
| `material` | 1.10.0 | Componentes Material Design 3 |
| `constraintlayout` | 2.1.4 | Layouts responsivos |
| `recyclerview` | 1.3.2 | Listas y grillas |
| `swiperefreshlayout` | 1.1.0 | Pull-to-refresh |
| `biometric` | 1.2.0-alpha05 | Huella digital |
| `Glide` | 4.16.0 | Carga de imágenes |
| `CircleImageView` | 3.1.0 | Fotos de perfil circulares |

### Backend / Firebase
| Servicio | Uso |
|----------|-----|
| **Firebase Auth** | Autenticación de usuarios |
| **Cloud Firestore** | Base de datos NoSQL en tiempo real |
| **Firebase Storage** | Almacenamiento de imágenes |
| **Firebase Messaging** | Notificaciones push |
| **Cloud Functions** | Lógica de servidor (Node.js) |

### Reportes
| Librería | Uso |
|----------|-----|
| `iText7` 7.2.5 | Generación de PDFs |
| `Apache POI` 5.2.3 | Generación de archivos Excel |

---

## 🏗 Arquitectura

```
TechMaintenance
├── Android App (Java - Nativo)
│   ├── Activities (Vistas y lógica de UI)
│   ├── Adapters (RecyclerView)
│   ├── Models (POJOs de datos)
│   └── Helpers (Utilidades)
│
├── Firebase Backend
│   ├── Authentication (Gestión de usuarios)
│   ├── Firestore (Base de datos)
│   ├── Storage (Imágenes)
│   └── Cloud Functions (Node.js)
│       ├── Notificaciones push (FCM)
│       ├── Emails (recuperación, credenciales, validación)
│       └── Gestión de usuarios (crear/eliminar técnicos)
│
└── Web de Validación
    └── techmaintenance.web.app/validar
```

El patrón de desarrollo es **MVC ligero**, donde las `Activities` actúan como controladores que conectan directamente con Firestore.

---

## 📁 Estructura del Proyecto

```
app/src/main/java/com/techsolution/techmaintenance/
│
├── MainActivity.java               # Punto de entrada temporal
├── RecuperarPasswordActivity.java  # Recuperación de contraseña
│
├── activities/
│   ├── SplashActivity.java              # Pantalla de inicio / verificación de sesión
│   ├── LoginActivity.java               # Login + Biométrico
│   ├── DashboardAdminActivity.java      # Dashboard del Administrador
│   ├── DashboardTecnicoActivity.java    # Dashboard del Técnico
│   │
│   ├── ListaClientesActivity.java       # Listado de clientes
│   ├── AgregarEditarClienteActivity.java
│   ├── DetalleClienteActivity.java
│   │
│   ├── ListaEquiposActivity.java        # Listado de equipos
│   ├── AgregarEditarEquipoActivity.java
│   ├── DetalleEquipoActivity.java
│   │
│   ├── AdministrarTecnicosActivity.java # Gestión de técnicos (solo admin)
│   ├── CrearEditarTecnicoActivity.java
│   │
│   ├── ListaMantenimientosActivity.java # Órdenes de servicio
│   ├── CrearMantenimientoActivity.java
│   ├── DetalleMantenimientoActivity.java
│   ├── CompletarServicioActivity.java
│   ├── ValidarServicioActivity.java     # Deep link desde web
│   ├── ValidacionClienteActivity.java
│   │
│   ├── CalendarioActivity.java          # Vista de calendario
│   ├── ReportesActivity.java            # Reportes PDF/Excel (solo admin)
│   ├── NotificacionesActivity.java      # Centro de notificaciones
│   │
│   ├── PerfilAdminActivity.java
│   ├── PerfilTecnicoActivity.java
│   └── CambiarPasswordActivity.java
│
├── adapters/
│   ├── ClienteAdapter.java
│   ├── EquipoAdapter.java
│   ├── MantenimientoAdapter.java
│   ├── MantenimientoTimelineAdapter.java
│   ├── TecnicoAdapter.java
│   ├── TecnicoEstadisticaAdapter.java
│   ├── EquipoSeleccionAdapter.java
│   └── NotificacionAdapter.java
│
├── models/
│   ├── Cliente.java
│   ├── Equipo.java
│   ├── Mantenimiento.java
│   ├── MantenimientoDetallado.java
│   ├── Notificacion.java
│   └── Usuario.java
│
└── helpers/
    ├── FirestoreHelper.java        # Utilidades para fechas/timestamps de Firestore
    ├── NotificationHelper.java     # Gestión de canales y permisos de notificación
    └── DateUtils.java              # Formateo de fechas
```

---

## ✅ Requisitos Previos

- **Android Studio** Hedgehog (2023.1.1) o superior
- **JDK 17**
- **Android SDK** API 29+ (Android 10)
- Cuenta de **Firebase** con proyecto configurado
- **Node.js 18+** (para Cloud Functions)

---

## 🚀 Instalación

1. **Clona el repositorio:**
   ```bash
   git clone https://github.com/Kevin30042001/TechMaintenance.git
   cd TechMaintenance
   ```

2. **Abre en Android Studio:**
   - `File → Open` → selecciona la carpeta del proyecto

3. **Configura Firebase** (ver sección siguiente)

4. **Sincroniza Gradle:**
   - Android Studio lo hará automáticamente, o usa: `./gradlew sync`

5. **Ejecuta la app:**
   - Selecciona un emulador (API 29+) o dispositivo físico
   - Presiona ▶ **Run**

---

## 🔥 Configuración de Firebase

1. Ve a [Firebase Console](https://console.firebase.google.com) y crea un proyecto.

2. Agrega una app Android con el package name:
   ```
   com.techsolution.techmaintenance
   ```

3. Descarga el archivo `google-services.json` y colócalo en:
   ```
   app/google-services.json
   ```

4. Habilita los siguientes servicios en Firebase Console:
   - **Authentication** → Método: Email/Contraseña
   - **Cloud Firestore** → Crear base de datos
   - **Firebase Storage** → Bucket de almacenamiento
   - **Cloud Messaging** → Habilitar FCM

5. **Despliega las Cloud Functions:**
   ```bash
   cd functions
   npm install
   firebase deploy --only functions
   ```

> ⚠️ **Nota:** El archivo `google-services.json` y `local.properties` no están incluidos en el repositorio por seguridad. Debes configurarlos con tu propio proyecto de Firebase.

---

## 🗄 Base de Datos (Firestore)

### Colecciones principales

```
firestore/
├── usuarios/           # Admins y técnicos
│   └── {uid}/
│       ├── nombre, apellido, email
│       ├── rol: "admin" | "tecnico"
│       ├── estado: "activo" | "inactivo"
│       └── fotoPerfilURL
│
├── clientes/           # Clientes del negocio
│   └── {clienteId}/
│       ├── nombre, empresa, email, telefono
│       └── direccion
│
├── equipos/            # Equipos registrados
│   └── {equipoId}/
│       ├── nombre, marca, modelo, serie
│       ├── clienteId, clienteNombre
│       └── fotoURL
│
├── mantenimientos/     # Órdenes de servicio
│   └── {mantenimientoId}/
│       ├── estado: "programado" | "en_proceso" | "completado"
│       ├── tecnicoId, clienteId, equipoId
│       ├── fechaProgramada, fechaFinalizacion
│       ├── codigoValidacion
│       └── calificacionCliente
│
└── notificaciones/     # Notificaciones por usuario
    └── {notificacionId}/
        ├── usuarioId, titulo, cuerpo
        └── leida: boolean
```

---

## ☁️ Cloud Functions

Ubicadas en `functions/src/`, las funciones cubren:

| Función | Trigger | Descripción |
|---------|---------|-------------|
| `setUserClaimsOnCreate` | Firestore onCreate | Asigna custom claims (rol/estado) al crear usuario |
| `updateUserClaimsOnUpdate` | Firestore onUpdate | Actualiza claims si cambia rol o estado |
| `eliminarUsuario` | HTTPS Callable | Elimina usuario de Auth y Firestore |
| `crearTecnico` | HTTPS Callable | Crea cuenta de técnico con credenciales |
| `enviarCodigoRecuperacion` | HTTPS Callable | Envía código OTP por email para recuperar contraseña |
| `cambiarPasswordRecuperacion` | HTTPS Callable | Cambia contraseña con el código OTP |
| `enviarCredencialesTecnico` | HTTPS Callable | Envía credenciales al nuevo técnico por email |
| `enviarCodigoValidacion` | HTTPS Callable | Envía código de validación del servicio al cliente |
| `enviarNotificacionNuevoMantenimiento` | Callable | Push notification para técnico asignado |
| `enviarNotificacionServicioCompletado` | Callable | Push notification para admin |

---

## 👤 Roles de Usuario

| Rol | Acceso |
|-----|--------|
| **Admin** | Dashboard completo, gestión de clientes/equipos/técnicos, reportes, estadísticas |
| **Técnico** | Dashboard propio, lista de órdenes asignadas, completar servicios, perfil |

El rol se determina desde Firestore y se verifica en cada login. Un usuario inactivo no puede iniciar sesión.

---

## 🤝 Contribución

Este es un proyecto académico/personal. Si deseas contribuir:

1. Haz fork del repositorio
2. Crea una rama: `git checkout -b feature/nueva-funcionalidad`
3. Haz commit de tus cambios: `git commit -m 'feat: agregar nueva funcionalidad'`
4. Push a la rama: `git push origin feature/nueva-funcionalidad`
5. Abre un Pull Request

---

## 👨‍💻 Autor

**Kevin** — [@Kevin30042001](https://github.com/Kevin30042001)

---

*Desarrollado con ❤️ usando Java nativo y Firebase*
