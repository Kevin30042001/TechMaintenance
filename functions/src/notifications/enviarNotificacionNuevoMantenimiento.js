const { onCall, HttpsError } = require('firebase-functions/v2/https');
const admin = require('firebase-admin');

/**
 * Cloud Function: Enviar notificación push al técnico cuando se le asigna un nuevo mantenimiento
 *
 * @param {Object} request.data - Datos del mantenimiento
 * @param {string} request.data.tecnicoId - ID del técnico asignado
 * @param {string} request.data.mantenimientoId - ID del mantenimiento creado
 * @param {string} request.data.equipoInfo - Información del equipo (ej: "Laptop - Dell Latitude 5420")
 * @param {string} request.data.clienteInfo - Información del cliente (nombre de la empresa)
 * @param {string} request.data.fechaHora - Fecha y hora programada
 * @param {string} request.data.prioridad - Prioridad del mantenimiento
 *
 * @returns {Object} - { success: boolean, message: string }
 */
exports.enviarNotificacionNuevoMantenimiento = onCall(async (request) => {
  const data = request.data;
  console.log('🔔 [NOTIFICACIÓN] Iniciando envío de notificación de nuevo mantenimiento');
  console.log('📋 Datos recibidos:', data);

  const { tecnicoId, mantenimientoId, equipoInfo, clienteInfo, fechaHora, prioridad } = data;

  // Validaciones
  if (!tecnicoId) {
    console.error('❌ Error: tecnicoId no proporcionado');
    throw new HttpsError('invalid-argument', 'tecnicoId es requerido');
  }

  if (!mantenimientoId) {
    console.error('❌ Error: mantenimientoId no proporcionado');
    throw new HttpsError('invalid-argument', 'mantenimientoId es requerido');
  }

  try {
    // 1. Obtener FCM token del técnico desde Firestore
    console.log(`📥 Obteniendo token FCM del técnico ${tecnicoId}...`);
    const tecnicoDoc = await admin.firestore()
      .collection('usuarios')
      .doc(tecnicoId)
      .get();

    if (!tecnicoDoc.exists) {
      console.error(`❌ Técnico ${tecnicoId} no encontrado en Firestore`);
      throw new HttpsError('not-found', 'Técnico no encontrado');
    }

    const tecnicoData = tecnicoDoc.data();
    const fcmToken = tecnicoData.fcmToken;

    if (!fcmToken) {
      console.warn(`⚠️ Técnico ${tecnicoId} (${tecnicoData.nombre}) no tiene FCM token registrado`);
      return {
        success: false,
        message: 'Técnico no tiene dispositivo registrado para notificaciones'
      };
    }

    console.log(`✅ Token FCM encontrado para ${tecnicoData.nombre}`);

    // 2. Determinar emoji de prioridad
    let emojiPrioridad = '🔧';
    if (prioridad === 'urgente') emojiPrioridad = '🚨';
    else if (prioridad === 'alta') emojiPrioridad = '⚠️';
    else if (prioridad === 'media') emojiPrioridad = '🔔';

    // 3. Construir mensaje de notificación
    const titulo = `${emojiPrioridad} Nuevo mantenimiento asignado`;
    const cuerpo = `${equipoInfo} - ${clienteInfo}${fechaHora ? '\n📅 ' + fechaHora : ''}`;

    const mensaje = {
      notification: {
        title: titulo,
        body: cuerpo
      },
      data: {
        mantenimientoId: mantenimientoId,
        tipo: 'nuevo_mantenimiento',
        equipoInfo: equipoInfo || '',
        clienteInfo: clienteInfo || '',
        prioridad: prioridad || 'media',
        timestamp: Date.now().toString()
      },
      android: {
        priority: 'high',
        notification: {
          channelId: 'techmaintenance_notifications',
          priority: prioridad === 'urgente' ? 'max' : 'high',
          sound: 'default',
          color: '#2196F3'
        }
      },
      token: fcmToken
    };

    console.log('📤 Enviando notificación FCM...');
    console.log('📋 Mensaje:', JSON.stringify(mensaje, null, 2));

    // 4. Enviar notificación push
    const response = await admin.messaging().send(mensaje);

    console.log('✅ Notificación enviada exitosamente:', response);

    // 5. Registrar notificación en Firestore (opcional, para historial)
    console.log('💾 Guardando notificación en Firestore para técnico...');

    // Construir objeto de datos (solo incluir campos con valor)
    const datosNotificacion = {};
    if (equipoInfo !== undefined && equipoInfo !== null) {
      datosNotificacion.equipoInfo = equipoInfo;
    }
    if (clienteInfo !== undefined && clienteInfo !== null) {
      datosNotificacion.clienteInfo = clienteInfo;
    }
    if (prioridad !== undefined && prioridad !== null) {
      datosNotificacion.prioridad = prioridad;
    }
    if (fechaHora !== undefined && fechaHora !== null) {
      datosNotificacion.fechaHora = fechaHora;
    }

    console.log('   - Datos a guardar:', JSON.stringify(datosNotificacion, null, 2));

    const notificationDoc = await admin.firestore().collection('notificaciones').add({
      usuarioId: tecnicoId,
      tipo: 'asignacion',
      mantenimientoId: mantenimientoId,
      titulo: titulo,
      mensaje: cuerpo,
      leida: false,
      fechaCreacion: admin.firestore.FieldValue.serverTimestamp(),
      datos: datosNotificacion
    });

    console.log(`✅ Notificación registrada en Firestore con ID: ${notificationDoc.id}`);

    return {
      success: true,
      message: 'Notificación enviada correctamente',
      messageId: response
    };

  } catch (error) {
    console.error('❌ ========================================');
    console.error('❌ ERROR AL ENVIAR NOTIFICACIÓN');
    console.error('❌ ========================================');
    console.error('   - Tipo de error:', error.constructor.name);
    console.error('   - Código de error:', error.code);
    console.error('   - Mensaje:', error.message);
    console.error('   - Stack trace:', error.stack);
    console.error('   - Error completo:', error);

    // Si el token es inválido, eliminarlo del usuario
    if (error.code === 'messaging/invalid-registration-token' ||
        error.code === 'messaging/registration-token-not-registered') {
      console.log('🗑️ Eliminando token FCM inválido del usuario...');
      try {
        await admin.firestore()
          .collection('usuarios')
          .doc(tecnicoId)
          .update({ fcmToken: admin.firestore.FieldValue.delete() });
        console.log('✅ Token FCM eliminado correctamente');
      } catch (deleteError) {
        console.error('❌ Error al eliminar token FCM:', deleteError);
      }

      return {
        success: false,
        message: 'Token FCM inválido, se ha eliminado'
      };
    }

    throw new HttpsError('internal', 'Error al enviar notificación: ' + error.message);
  }
});
