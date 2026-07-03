/**
 * Cloud Functions de Aparkau.
 *
 * Notificaciones push de plazas tándem:
 * cuando se crea una reserva sobre una plaza A que tiene `plazaBloqueadaId` = B,
 * el usuario que tenga una reserva activa sobre la plaza B ese mismo día queda
 * BLOQUEADO. Le enviamos un push a todos sus dispositivos (tokens FCM).
 */
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions/v2";
import { initializeApp } from "firebase-admin/app";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";

initializeApp();
const db = getFirestore();

// Estados de reserva que consideramos "vigentes" para el bloqueo.
const ESTADOS_ACTIVOS = ["AGENDADA", "ACTIVA"];

// La app guarda las fechas en hora local (España). Comparamos "el mismo día"
// en esa misma zona para evitar desajustes de ±1 día respecto a UTC.
const ZONA_HORARIA = "Europe/Madrid";
const formateadorDia = new Intl.DateTimeFormat("en-CA", {
  timeZone: ZONA_HORARIA,
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
});

interface ReservaData {
  usuarioId?: string;
  plazaId?: string;
  estado?: string;
  fechaReserva?: Timestamp | null;
  horaInicio?: Timestamp | null;
}

interface AvisoSalidaData {
  // uid del usuario BLOQUEADO que pide salir (nunca se comparte con nadie).
  usuarioId?: string;
  // Plaza del solicitante (la trasera del tándem, la que queda bloqueada).
  plazaBloqueadaId?: string;
}

/**
 * Devuelve la fecha (YYYY-MM-DD, en hora de España) de un Timestamp para
 * comparar "el mismo día".
 */
function claveDia(ts: Timestamp | null | undefined): string | null {
  if (!ts) return null;
  return formateadorDia.format(ts.toDate());
}

export const notificarBloqueoTandem = onDocumentCreated(
  "reservas/{reservaId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const reserva = snap.data() as ReservaData;
    const plazaOcupadaId = reserva.plazaId;
    const diaReserva = claveDia(reserva.fechaReserva ?? reserva.horaInicio);

    if (!plazaOcupadaId) {
      logger.info("Reserva sin plazaId; nada que hacer.");
      return;
    }

    // 1. Leer la plaza ocupada para saber a qué plaza bloquea (tándem).
    const plazaSnap = await db.collection("plazas").doc(plazaOcupadaId).get();
    if (!plazaSnap.exists) {
      logger.info(`Plaza ${plazaOcupadaId} no existe.`);
      return;
    }

    const plazaBloqueadaId = (plazaSnap.get("plazaBloqueadaId") as string) || "";
    if (!plazaBloqueadaId) {
      logger.info(`Plaza ${plazaOcupadaId} no bloquea ninguna (no es tándem).`);
      return;
    }

    // 2. Buscar reservas sobre la plaza bloqueada (filtramos estado/día en código
    //    para no necesitar un índice compuesto en Firestore).
    const reservasBloqueadas = await db
      .collection("reservas")
      .where("plazaId", "==", plazaBloqueadaId)
      .get();

    if (reservasBloqueadas.empty) {
      logger.info(`Nadie tiene reservada la plaza bloqueada ${plazaBloqueadaId}.`);
      return;
    }

    // 3. Filtrar por estado activo y mismo día; quedarnos con los usuarios afectados.
    const usuariosAfectados = new Set<string>();
    reservasBloqueadas.forEach((doc) => {
      const r = doc.data() as ReservaData;
      const activa = ESTADOS_ACTIVOS.includes(r.estado ?? "");
      const mismoDia = claveDia(r.fechaReserva ?? r.horaInicio) === diaReserva;
      const usuarioId = r.usuarioId;
      // No notificamos al que acaba de reservar (por si reservó ambas).
      if (activa && mismoDia && usuarioId && usuarioId !== reserva.usuarioId) {
        usuariosAfectados.add(usuarioId);
      }
    });

    if (usuariosAfectados.size === 0) {
      logger.info("No hay usuarios bloqueados ese día.");
      return;
    }

    const titulo = "Tu plaza ha quedado bloqueada";
    const cuerpo =
      `Han reservado la plaza ${plazaOcupadaId}, que forma tándem con tu plaza ` +
      `${plazaBloqueadaId}. Es posible que no puedas usarla mientras dure la reserva.`;

    // 4. Enviar el push a cada usuario afectado.
    await Promise.all(
      Array.from(usuariosAfectados).map((uid) =>
        enviarPushAUsuario(uid, titulo, cuerpo, {
          tipo: "BLOQUEO_TANDEM",
          plazaOcupadaId,
          plazaBloqueadaId,
        })
      )
    );
  }
);

/**
 * Envía una notificación a todos los tokens FCM de un usuario y limpia los
 * tokens que ya no son válidos.
 */
async function enviarPushAUsuario(
  uid: string,
  titulo: string,
  cuerpo: string,
  data: Record<string, string>
): Promise<void> {
  const tokensSnap = await db
    .collection("usuarios")
    .doc(uid)
    .collection("tokens")
    .get();

  if (tokensSnap.empty) {
    logger.info(`Usuario ${uid} sin tokens FCM.`);
    return;
  }

  const tokens = tokensSnap.docs.map((d) => (d.get("token") as string) || d.id);

  // Añadimos SIEMPRE el destinatario y el texto en el propio "data". Enviamos el
  // mensaje como SOLO-DATOS (sin bloque "notification") para que la app SIEMPRE
  // pase por onMessageReceived —incluso en segundo plano— y pueda descartar el
  // aviso si el usuario logueado no es `uidDestino` (evita que un token "heredado"
  // de otra cuenta muestre la notificación a quien no corresponde). Sigue siendo
  // anónimo: el destinatario solo ve su PROPIO uid, nunca el del solicitante.
  const datosConDestino = {
    ...data,
    uidDestino: uid,
    titulo,
    cuerpo,
  };

  const respuesta = await getMessaging().sendEachForMulticast({
    tokens,
    data: datosConDestino,
    android: { priority: "high" },
  });

  logger.info(
    `Push a ${uid}: ${respuesta.successCount} ok, ${respuesta.failureCount} fallos.`
  );

  // Limpieza de tokens inválidos (desinstalaciones, tokens caducados...).
  const borrados: Promise<unknown>[] = [];
  respuesta.responses.forEach((res, i) => {
    const code = res.error?.code;
    if (
      code === "messaging/registration-token-not-registered" ||
      code === "messaging/invalid-registration-token"
    ) {
      borrados.push(tokensSnap.docs[i].ref.delete());
    }
  });
  await Promise.all(borrados);
}

/**
 * Garantiza que un token FCM (= un dispositivo) pertenezca a UN solo usuario.
 *
 * Al iniciar sesión otra persona en el mismo dispositivo, el token queda
 * registrado bajo el nuevo usuario, pero puede seguir existiendo bajo el
 * usuario anterior (si no cerró sesión limpiamente). Eso provoca que la
 * notificación de "plaza bloqueada" llegue al dispositivo equivocado.
 *
 * Cuando se crea un token bajo `usuarios/{uid}/tokens/{tokenId}`, borramos ese
 * mismo token en cualquier OTRO usuario.
 */
export const deduplicarTokenFcm = onDocumentCreated(
  "usuarios/{uid}/tokens/{tokenId}",
  async (event) => {
    const uid = event.params.uid;
    const tokenId = event.params.tokenId;

    // El token también se guarda como campo; usamos el ID del documento como
    // fuente de verdad (es el propio token FCM).
    const duplicados = await db
      .collectionGroup("tokens")
      .where("token", "==", tokenId)
      .get();

    const borrados: Promise<unknown>[] = [];
    duplicados.forEach((doc) => {
      const otroUid = doc.ref.parent.parent?.id;
      if (otroUid && otroUid !== uid) {
        logger.info(
          `Token ${tokenId} también estaba en usuario ${otroUid}; se elimina (ahora es de ${uid}).`
        );
        borrados.push(doc.ref.delete());
      }
    });

    await Promise.all(borrados);
  }
);

/**
 * Aviso "SOS" ANÓNIMO para salir de una plaza tándem trasera.
 *
 * El usuario BLOQUEADO (que ocupa la plaza trasera B) pulsa "Avisar para salir"
 * y la app crea un documento en `avisosSalida/{avisoId}` con SU uid y SU plaza.
 *
 * Aquí (con permisos admin) resolvemos quién le bloquea y le enviamos un push.
 * El solicitante NUNCA conoce la identidad, tokens ni teléfono del bloqueador,
 * y el bloqueador NUNCA recibe datos del solicitante: el push es 100% anónimo.
 */
export const notificarAvisoSalidaTandem = onDocumentCreated(
  "avisosSalida/{avisoId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const aviso = snap.data() as AvisoSalidaData;
    const solicitanteId = aviso.usuarioId;
    const plazaSolicitante = aviso.plazaBloqueadaId;

    if (!plazaSolicitante) {
      logger.info("Aviso de salida sin plazaBloqueadaId; nada que hacer.");
      return;
    }

    logger.info(
      `Aviso de salida: solicitante=${solicitanteId}, plaza=${plazaSolicitante}.`
    );

    // 1. Encontrar la(s) plaza(s) A que bloquean la plaza del solicitante
    //    (relación inversa: A.plazaBloqueadaId == plazaDelSolicitante).
    const plazasBloqueadoras = await db
      .collection("plazas")
      .where("plazaBloqueadaId", "==", plazaSolicitante)
      .get();

    if (plazasBloqueadoras.empty) {
      logger.info(`Nadie bloquea la plaza ${plazaSolicitante} (no es trasera de tándem).`);
      return;
    }

    logger.info(
      `Plazas que bloquean a ${plazaSolicitante}: ` +
      `${plazasBloqueadoras.docs.map((d) => d.id).join(", ")}.`
    );

    // 2. Para cada plaza bloqueadora, buscar reservas activas HOY y quedarnos
    //    con los usuarios que la ocupan (los "bloqueadores").
    const hoy = claveDia(Timestamp.now());
    const bloqueadores = new Set<string>();
    // Referencias a las reservas de los bloqueadores: les marcamos un campo para
    // que su app muestre el aviso "alguien necesita salir" en la tarjeta.
    const reservasBloqueadoras: FirebaseFirestore.DocumentReference[] = [];

    await Promise.all(
      plazasBloqueadoras.docs.map(async (plazaDoc) => {
        const reservasPlaza = await db
          .collection("reservas")
          .where("plazaId", "==", plazaDoc.id)
          .get();

        reservasPlaza.forEach((doc) => {
          const r = doc.data() as ReservaData;
          const activa = ESTADOS_ACTIVOS.includes(r.estado ?? "");
          const mismoDia = claveDia(r.fechaReserva ?? r.horaInicio) === hoy;
          // Nunca nos notificamos a nosotros mismos.
          if (activa && mismoDia && r.usuarioId && r.usuarioId !== solicitanteId) {
            bloqueadores.add(r.usuarioId);
            reservasBloqueadoras.push(doc.ref);
          }
        });
      })
    );

    if (bloqueadores.size === 0) {
      logger.info(`Nadie ocupa hoy (${hoy}) la plaza que bloquea a ${plazaSolicitante}.`);
      // Limpiamos el aviso igualmente para no dejar basura.
      await snap.ref.delete().catch(() => undefined);
      return;
    }

    logger.info(
      `Bloqueadores a avisar (${bloqueadores.size}): ${Array.from(bloqueadores).join(", ")}.`
    );

    // 3a. Marcar la reserva de cada bloqueador para que su app muestre el aviso
    //     "alguien necesita salir" en la tarjeta (además del push). Anónimo: no
    //     guardamos ningún dato del solicitante.
    await Promise.all(
      reservasBloqueadoras.map((ref) =>
        ref.update({ avisoSalidaEn: Timestamp.now() }).catch(() => undefined)
      )
    );

    // 3b. Push ANÓNIMO: sin nombre, uid, tokens ni teléfono del solicitante.
    const titulo = "Un compañero necesita salir";
    const cuerpo =
      "Alguien de tu plaza tándem necesita salir. " +
      "¿Puedes mover tu coche un momento? 🚗";

    await Promise.all(
      Array.from(bloqueadores).map((uid) =>
        enviarPushAUsuario(uid, titulo, cuerpo, {
          tipo: "AVISO_SALIDA_TANDEM",
        })
      )
    );

    // 4. El aviso ya está entregado: borramos el documento (evita reenvíos y
    //    no deja rastro del solicitante en la base de datos).
    await snap.ref.delete().catch(() => undefined);
  }
);
