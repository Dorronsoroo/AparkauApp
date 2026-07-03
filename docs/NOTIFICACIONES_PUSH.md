# Sistema de Notificaciones Push — Plazas Tándem (Aparkau)

> Documentación funcional y técnica del sistema de notificaciones push que avisa
> a un usuario cuando su plaza tándem queda **bloqueada** porque otra persona ha
> reservado la plaza asociada.

---

## 1. Objetivo

En las **plazas tándem**, una plaza A "delantera" bloquea a una plaza B "trasera":
cuando alguien reserva/ocupa A, la plaza B queda inutilizable. El objetivo es
**avisar por push** al usuario que tenía reservada la plaza B de que ha quedado
bloqueada.

---

## 2. Arquitectura general

```
┌────────────────────┐        crea reserva         ┌──────────────────────┐
│   App Android       │ ─────────────────────────▶ │   Firestore           │
│  (Kotlin, Compose)  │                            │  reservas / plazas /  │
│                     │ ◀───── push (FCM) ──────┐  │  usuarios/{uid}/tokens│
└────────────────────┘                         │  └──────────┬───────────┘
        ▲                                       │             │ onCreate(reservas)
        │ muestra notificación                  │             ▼
        │                                       │  ┌──────────────────────┐
        │                                       └──│  Cloud Function       │
        └──────────────────────────────────────── │ notificarBloqueoTandem│
                     envía FCM                     │  (Node.js 20, TS)     │
                                                   └──────────────────────┘
```

**Piezas:**
- **App Android**: registra el token FCM del dispositivo y muestra las notificaciones.
- **Firestore**: base de datos (colecciones `usuarios`, `plazas`, `reservas` + subcolección `usuarios/{uid}/tokens`).
- **Cloud Function**: al crearse una reserva, detecta al usuario bloqueado y le envía el push.

> ⚠️ FCM **no permite** enviar push de dispositivo a dispositivo de forma segura;
> por eso el envío se hace desde un backend (Cloud Functions con el Admin SDK).

---

## 3. Modelo de datos (Firestore)

### Colección `usuarios`
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` (@DocumentId) | String | UID de Firebase Auth |
| `nombre`, `apellidos`, `email` | String | Datos del usuario |
| `perfil` | String (enum) | EMPLEADO_HABITUAL, NOMADA, VIP_CLIENTE, PRIORIDAD_ESPECIAL |
| `vehiculos` | Array | Lista de vehículos |

#### Subcolección `usuarios/{uid}/tokens/{token}` — **NUEVA**
| Campo | Tipo | Descripción |
|-------|------|-------------|
| *(ID del documento)* | String | El propio token FCM (idempotente, sin duplicados) |
| `token` | String | Token FCM del dispositivo |
| `plataforma` | String | `"android"` |
| `actualizadoEn` | Timestamp | Fecha de registro/actualización |

### Colección `plazas`
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` (@DocumentId) | String | Número de plaza (p.ej. "36A") |
| `tipo` | String | NORMAL, ELECTRICA, MOTO, PRIORITARIA, TANDEM |
| `estado` | String | LIBRE, OCUPADA, BLOQUEADA_POR_TANDEM |
| `zona` | String | OFICINA, PAGO |
| `plazaBloqueadaId` | String | **Clave del tándem**: ID de la plaza que queda bloqueada al ocupar ésta |

### Colección `reservas`
| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` (@DocumentId) | String | ID autogenerado |
| `usuarioId` | String | UID del usuario |
| `plazaId` | String | Plaza reservada |
| `matriculaVehiculo` | String | Matrícula |
| `estado` | String (enum) | AGENDADA, ACTIVA, FINALIZADA, CANCELADA |
| `fechaReserva`, `horaInicio`, `horaFin`, ... | Timestamp | Fechas/horas |

---

## 4. Flujo funcional

1. El **Usuario B** reserva la plaza `36B` para un día X → se crea un doc en `reservas`.
2. El **Usuario A** reserva la plaza `36A` (que tiene `plazaBloqueadaId = "36B"`) el mismo día X.
3. Al crearse la reserva de A, la **Cloud Function** se dispara:
   - Lee `plazas/36A` → `plazaBloqueadaId = "36B"`.
   - Busca reservas **activas** sobre `36B` el **mismo día**.
   - Obtiene al usuario bloqueado (excluye al que acaba de reservar).
   - Lee sus tokens en `usuarios/{uid}/tokens` y envía el push.
4. El **Usuario B** recibe la notificación en su dispositivo.

---

## 5. Implementación en la App Android

### Dependencia (`app/build.gradle.kts`)
```kotlin
implementation("com.google.firebase:firebase-messaging")   // versión vía BOM
```

### Archivos NUEVOS
| Archivo | Rol |
|---------|-----|
| `model/service/AparkauMessagingService.kt` | **Interfaz** del receptor FCM. |
| `model/service/impl/AparkauMessagingServiceImpl.kt` | Receptor FCM (hereda de `FirebaseMessagingService` e implementa `AparkauMessagingService`). Clase concreta obligatoria; **delega** toda la lógica en `NotificacionService`. Maneja `onNewToken` y `onMessageReceived`. |
| `model/service/NotificacionService.kt` | **Interfaz** del servicio de notificaciones. |
| `model/service/impl/NotificacionServiceImpl.kt` | **Implementación**: crear canal, comprobar permiso, mostrar notificación, registrar/eliminar token (obtiene el token con `FirebaseMessaging.getInstance().token`). |
| `res/drawable/ic_notification.xml` | Icono monocromo para el *small icon*. |

### Archivos MODIFICADOS
| Archivo | Cambio |
|---------|--------|
| `AndroidManifest.xml` | Registro del `<service>` FCM, permiso `POST_NOTIFICATIONS`, `meta-data` de icono y canal por defecto. |
| `res/values/strings.xml` | Strings del canal de notificaciones (`notif_channel_tandem_*`). |
| `model/service/module/ServiceModule.kt` | Binding Hilt: `provideNotificacionService`. |
| `model/service/UsuarioService.kt` + `impl/UsuarioServiceImpl.kt` | Métodos `guardarTokenFcm` / `eliminarTokenFcm` (subcolección `tokens`). |
| `screen/login/LoginViewModel.kt` | Tras autenticar → `registrarTokenActual()`. |
| `screen/sign_up/SignUpViewModel.kt` | Tras crear usuario → `registrarTokenActual()`. |
| `screen/mi_cuenta/MiCuentaViewModel.kt` | Antes de `signOut()` → `eliminarTokenActual()`. |
| `MainActivity.kt` | Solicita el permiso `POST_NOTIFICATIONS` en Android 13+. |

### Patrón de diseño
Se respeta el patrón del proyecto **interfaz + impl** enlazadas por Hilt en
`ServiceModule`. `AparkauMessagingServiceImpl` es la única clase "concreta" porque el
sistema Android la instancia directamente, pero no contiene lógica de negocio.

### Momentos en los que se registra/elimina el token
- **Registro de usuario** y **login** → se guarda el token (cubre el caso de que
  el token ya existiera antes de haber usuario logueado).
- **Cierre de sesión** → se elimina el token (para no enviar push a un dispositivo
  del que el usuario ya salió).
- **`onNewToken`** → respaldo automático cuando FCM renueva el token.

---

## 6. Cloud Function (`functions/`)

### Estructura
```
functions/
├── package.json        (deps: firebase-admin, firebase-functions; Node 20)
├── tsconfig.json
├── .gitignore
└── src/index.ts        ← lógica
firebase.json           (config de deploy de functions + firestore rules)
.firebaserc             (proyecto por defecto: aparkau-1bd28)
firestore.rules         (reglas de seguridad)
```

### Función `notificarBloqueoTandem`
- **Trigger**: `onDocumentCreated("reservas/{reservaId}")` (Firestore, 2ª gen).
- **Región**: `europe-west1` (trigger en `eur3`).
- **Lógica**:
  1. Lee la plaza reservada y su `plazaBloqueadaId`.
  2. Si no es tándem (campo vacío), termina.
  3. Busca reservas sobre la plaza bloqueada (filtra estado activo y mismo día en código, para no necesitar índice compuesto).
  4. Determina los usuarios afectados (excluye al que reserva).
  5. Envía el push a todos sus tokens con `sendEachForMulticast`.
  6. **Limpia** los tokens inválidos (desinstalados/caducados).

---

## 7. Despliegue

### Requisitos
- **Plan Blaze** activo en Firebase (Cloud Functions no funciona en Spark).
- **Node.js 20** y **Firebase CLI** instalados.

### Comandos
```powershell
# 1. Login (una vez)
firebase login

# 2. Instalar dependencias de functions (una vez)
cd functions
npm install

# 3. Desplegar la función
npm run deploy         # equivale a: firebase deploy --only functions

# 4. Desplegar las reglas de Firestore
firebase deploy --only firestore:rules
```

### Notas de despliegue
- La **primera vez** con funciones de 2ª gen puede fallar con un error de
  *"Eventarc Service Agent... permission denied"*. Es normal: esperar unos minutos
  y reintentar.
- Si aparece *"Changing from an HTTPS function to a background triggered function
  is not allowed"*, borrar la función residual y redeplegar:
  ```powershell
  firebase functions:delete notificarBloqueoTandem --region europe-west1 --force
  firebase deploy --only functions
  ```
- Política de limpieza de artefactos (evita coste de imágenes acumuladas):
  ```powershell
  firebase functions:artifacts:setpolicy --location europe-west1 --days 3 --force
  ```

### Ver logs
```powershell
firebase functions:log --only notificarBloqueoTandem
```

---

## 8. Reglas de seguridad (`firestore.rules`)

- `usuarios`: lectura para cualquier autenticado (la app muestra el nombre del
  ocupante); escritura solo del propio dueño.
- `usuarios/{uid}/tokens`: privados de cada usuario.
- `plazas`: solo lectura desde la app (escritura solo admin/consola).
- `reservas`: lectura y creación para autenticados; modificar/borrar solo el dueño.

> Tras desplegar las reglas, validar en la app: login, ver plazas y reservar.

---

## 9. Cómo probar (end‑to‑end)

Se necesitan **dos usuarios logueados a la vez** (idealmente **dos dispositivos/
emuladores**), porque al cerrar sesión se elimina el token.

1. **Dispositivo 1**: login como Usuario B (queda logueado) → reserva la plaza `36B`.
   - Verificar en Firestore que `usuarios/{uidB}/tokens` tiene un token.
2. **Dispositivo 2**: login como Usuario A → reserva la plaza `36A` el **mismo día**.
3. El Usuario B recibe la notificación *"Tu plaza ha quedado bloqueada"*.
4. Comprobar en logs: `Push a {uid}: 1 ok, 0 fallos.`

### Prueba de recepción simple (sin backend)
Firebase Console → **Messaging** → *Enviar mensaje de prueba* → pegar el token
FCM (campo `token` del documento) → *Probar*.

---

## 10. Problemas frecuentes / gotchas

| Síntoma | Causa | Solución |
|---------|-------|----------|
| No llega la notificación (ni abierta ni cerrada) | Permiso `POST_NOTIFICATIONS` denegado | Activar notificaciones en Ajustes de la app |
| Log `sin tokens FCM` | El destinatario cerró sesión (se borró su token) | Probar con el usuario bloqueado **logueado** (dos dispositivos) |
| No se genera token | Emulador sin Google Play Services | Usar imagen *Google APIs/Play Store* o móvil real |
| Deploy falla la 1ª vez (Eventarc) | Permisos aún propagándose | Esperar y reintentar |

---

## 11. Mejoras futuras (pendientes / opcionales)

- **Notificar el desbloqueo** cuando el de la plaza A cancela/finaliza (trigger
  `onUpdate`/`onDelete` de `reservas`).
- **Bandeja in‑app**: guardar avisos en una colección `notificaciones` (historial).
- **UI de permiso**: mensaje explicativo si el usuario rechaza el permiso.
- **Upgrade de runtime**: Node.js 20 se retira el **2026‑10‑30**; subir a Node 22.
- **Quitar logs de depuración** (`Log.d`) de `AparkauMessagingServiceImpl` y
  `NotificacionServiceImpl` antes de producción.

---

## 12. Resumen del estado

| Paso | Descripción | Estado |
|------|-------------|--------|
| 1 | Dependencia `firebase-messaging` | ✅ |
| 2 | Servicio FCM (interfaz + impl) | ✅ |
| 3 | Permiso + token en login/registro/signOut | ✅ |
| 4 | Detección del usuario bloqueado | ✅ |
| 5 | Cloud Function desplegada (europe-west1) | ✅ |
| 6 | Reglas de seguridad Firestore | ✅ escritas · ⏳ desplegar |
| 7 | Prueba end‑to‑end real | ✅ validada en 2 dispositivos |

