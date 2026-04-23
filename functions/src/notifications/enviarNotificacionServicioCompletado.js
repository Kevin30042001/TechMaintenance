const { onCall, HttpsError } = require('firebase-functions/v2/https');
const admin = require('firebase-admin');

/**
 * Cloud Function: Enviar notificación push al admin cuando un técnico completa un servicio
 *
 * @param {Object} request.data - Datos del servicio completado
 * @param {string} request.data.adminId - ID del administrador (opcional, se buscará automáticamente)
 * @param {string} request.data.mantenimientoId - ID del mantenimiento completado
 * @param {string} request.data.tecnicoNombre - Nombre del técnico que completó el servicio
 * @param {string} request.data.equipoInfo - Información del equipo
 * @param {string} request.data.clienteInfo - Información del cliente
 * @param {number} request.data.calificacion - Calificación del cliente (opcional)
 *
 * @returns {Object} - { success: boolean, message: string, notificacionesEnviadas: number }
 */
exports.enviarNotificacionServicioCompletado = onCall(async (request) => {
  const data = request.data;
  console.log('🔔 ========================================');
  console.log('🔔 [NOTIFICACIÓN] SERVICIO COMPLETADO');
  console.log('🔔 ========================================');
  console.log('📋 Datos recibidos:', JSON.stringify(data, null, 2));

  const { adminId, mantenimientoId, tecnicoNombre, equipoInfo, clienteInfo, calificacion } = data;

  // Validaciones
  if (!mantenimientoId) {
    console.error('❌ Error: mantenimientoId no proporcionado');
    throw new HttpsError('invalid-argument', 'mantenimientoId es requerido');
  }

  if (!tecnicoNombre) {
    console.error('❌ Error: tecnicoNombre no proporcionado');
    throw new HttpsError('invalid-argument', 'tecnicoNombre es requerido');
  }

  try {
    let adminsIds = [];

    // 1. Obtener admin(s) a notificar
    if (adminId) {
      // Si se proporciona un adminId específico, usar ese
      adminsIds.push(adminId);
      console.log(`📥 Usando adminId proporcionado: ${adminId}`);
    } else {
      // Si no, buscar todos los admins activos
      console.log('📥 Buscando todos los administradores activos...');
      const adminsSnapshot = await admin.firestore()
        .collection('usuarios')
        .where('rol', '==', 'admin')
        .where('estado', '==', 'activo')
        .get();

      if (adminsSnapshot.empty) {
        console.warn('⚠️ No se encontraron administradores activos');
        return {
          success: false,
          message: 'No hay administradores activos para notificar',
          notificacionesEnviadas: 0
        };
      }

      adminsSnapshot.forEach(doc => adminsIds.push(doc.id));
      console.log(`✅ ${adminsIds.length} administrador(es) encontrado(s)`);
    }

    // 2. Construir mensaje de notificación
    let titulo = '✅ Servicio completado';
    let cuerpo = `${tecnicoNombre} completó: ${equipoInfo}`;

    if (clienteInfo) {
      cuerpo += ` - ${clienteInfo}`;
    }

    if (calificacion) {
      const estrellas = '⭐'.repeat(calificacion);
      cuerpo += `\n${estrellas} ${calificacion}/5`;
    }

    // 3. Enviar notificación a cada admin
    let notificacionesEnviadas = 0;
    let notificacionesGuardadas = 0;
    const promesas = [];

    for (const currentAdminId of adminsIds) {
      const promesa = (async () => {
        try {
          // ✅ SIEMPRE guardar notificación en Firestore PRIMERO (sin importar si hay token FCM)
          console.log(`💾 Guardando notificación en Firestore para admin ${currentAdminId}...`);
          console.log(`   - Mantenimiento ID: ${mantenimientoId}`);
          console.log(`   - Título: ${titulo}`);
          console.log(`   - Mensaje: ${cuerpo}`);

          // Construir objeto de datos (solo incluir campos con valor)
          const datosNotificacion = {
            tecnicoNombre,
            equipoInfo,
            clienteInfo
          };

          // Solo agregar calificacion si existe
          if (calificacion !== undefined && calificacion !== null) {
            datosNotificacion.calificacion = calificacion;
          }

          const notificacionData = {
            usuarioId: currentAdminId,
            tipo: 'completado',
            mantenimientoId: mantenimientoId,
            titulo: titulo,
            mensaje: cuerpo,
            leida: false,
            fechaCreacion: admin.firestore.FieldValue.serverTimestamp(),
            datos: datosNotificacion
          };

          console.log(`   - Datos a guardar:`, JSON.stringify(notificacionData, null, 2));

          const docRef = await admin.firestore().collection('notificaciones').add(notificacionData);
          console.log(`   - Documento creado con ID: ${docRef.id}`);

          notificacionesGuardadas++;
          console.log(`✅ Notificación guardada en Firestore para admin ${currentAdminId}`);

          // Obtener token FCM del admin para enviar push notification (opcional)
          const adminDoc = await admin.firestore()
            .collection('usuarios')
            .doc(currentAdminId)
            .get();

          if (!adminDoc.exists) {
            console.warn(`⚠️ Admin ${currentAdminId} no encontrado, pero notificación guardada`);
            return;
          }

          const adminData = adminDoc.data();
          const fcmToken = adminData.fcmToken;

          if (!fcmToken) {
            console.warn(`⚠️ Admin ${currentAdminId} (${adminData.nombre}) no tiene FCM token, pero notificación guardada`);
            return;
          }

          console.log(`✅ Token FCM encontrado para admin ${adminData.nombre}, enviando push...`);

          // Construir mensaje FCM
          const mensaje = {
            notification: {
              title: titulo,
              body: cuerpo
            },
            data: {
              mantenimientoId: mantenimientoId,
              tipo: 'servicio_completado',
              tecnicoNombre: tecnicoNombre,
              equipoInfo: equipoInfo || '',
              clienteInfo: clienteInfo || '',
              calificacion: calificacion ? calificacion.toString() : '0',
              timestamp: Date.now().toString()
            },
            android: {
              priority: 'high',
              notification: {
                channelId: 'techmaintenance_notifications',
                priority: 'high',
                sound: 'default',
                color: '#4CAF50'
              }
            },
            token: fcmToken
          };

          console.log(`📤 Enviando push notification a admin ${adminData.nombre}...`);

          // Enviar notificación push
          const response = await admin.messaging().send(mensaje);
          console.log(`✅ Push notification enviada a ${adminData.nombre}:`, response);

          notificacionesEnviadas++;

        } catch (error) {
          console.error(`❌ ========================================`);
          console.error(`❌ ERROR al procesar notificación para admin ${currentAdminId}`);
          console.error(`❌ ========================================`);
          console.error(`   - Tipo de error: ${error.constructor.name}`);
          console.error(`   - Código de error: ${error.code}`);
          console.error(`   - Mensaje: ${error.message}`);
          console.error(`   - Stack trace:`, error.stack);
          console.error(`   - Error completo:`, error);

          // Si el token es inválido, eliminarlo
          if (error.code === 'messaging/invalid-registration-token' ||
              error.code === 'messaging/registration-token-not-registered') {
            console.log(`🗑️ Eliminando token FCM inválido del admin ${currentAdminId}...`);
            try {
              await admin.firestore()
                .collection('usuarios')
                .doc(currentAdminId)
                .update({ fcmToken: admin.firestore.FieldValue.delete() });
            } catch (deleteError) {
              console.error(`❌ Error al eliminar token:`, deleteError);
            }
          }

          // NO re-lanzar el error para permitir que continúe con otros admins
          // El contador notificacionesGuardadas ya habrá sido incrementado si se guardó exitosamente antes del error
          console.error(`❌ Continuando con siguiente administrador...`);
        }
      })();

      promesas.push(promesa);
    }

    // Esperar a que todas las notificaciones se procesen
    await Promise.all(promesas);

    console.log(`✅ Proceso completado:`);
    console.log(`   - Notificaciones guardadas en Firestore: ${notificacionesGuardadas}`);
    console.log(`   - Push notifications enviadas: ${notificacionesEnviadas}`);

    return {
      success: notificacionesGuardadas > 0,
      message: notificacionesGuardadas > 0
        ? `${notificacionesGuardadas} notificación(es) guardada(s) en Firestore, ${notificacionesEnviadas} push enviadas`
        : 'No se guardaron notificaciones',
      notificacionesEnviadas: notificacionesEnviadas,
      notificacionesGuardadas: notificacionesGuardadas
    };

  } catch (error) {
    console.error('❌ Error general al enviar notificaciones:', error);
    throw new HttpsError('internal', 'Error al enviar notificaciones: ' + error.message);
  }
});
