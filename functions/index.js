const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

// ==================== IMPORTAR FUNCIONES DE NOTIFICACIONES ====================
const { enviarNotificacionNuevoMantenimiento } = require('./src/notifications/enviarNotificacionNuevoMantenimiento');
const { enviarNotificacionServicioCompletado } = require('./src/notifications/enviarNotificacionServicioCompletado');

// Exportar funciones de notificaciones
exports.enviarNotificacionNuevoMantenimiento = enviarNotificacionNuevoMantenimiento;
exports.enviarNotificacionServicioCompletado = enviarNotificacionServicioCompletado;

// ==================== IMPORTAR FUNCIONES DE EMAIL ====================
const { enviarCodigoRecuperacion, cambiarPasswordRecuperacion, enviarCredencialesTecnico } = require('./src/email/enviarEmailRecuperacion');
const { enviarCodigoValidacion } = require('./src/email/enviarCodigoValidacion');

// Exportar funciones de email
exports.enviarCodigoRecuperacion = enviarCodigoRecuperacion;
exports.cambiarPasswordRecuperacion = cambiarPasswordRecuperacion;
exports.enviarCredencialesTecnico = enviarCredencialesTecnico;
exports.enviarCodigoValidacion = enviarCodigoValidacion;

// ==================== IMPORTAR FUNCIONES DE USUARIOS ====================
const { crearTecnico } = require('./src/usuarios/crearTecnico');

// Exportar funciones de usuarios
exports.crearTecnico = crearTecnico;

// ==================== FUNCIÓN: ESTABLECER CUSTOM CLAIMS AL CREAR USUARIO ====================

const { onDocumentCreated } = require('firebase-functions/v2/firestore');

exports.setUserClaimsOnCreate = onDocumentCreated('usuarios/{userId}', async (event) => {
  const snap = event.data;
  const userData = snap.data();
  const userId = event.params.userId;

  try {
    // Establecer custom claims con rol y estado
    await admin.auth().setCustomUserClaims(userId, {
      rol: userData.rol,
      estado: userData.estado
    });

    console.log(`✅ Custom claims establecidos para usuario ${userId}:`);
    console.log(`   - Rol: ${userData.rol}`);
    console.log(`   - Estado: ${userData.estado}`);

    return null;
  } catch (error) {
    console.error(`❌ Error al establecer custom claims para ${userId}:`, error);
    return null;
  }
});

// ==================== FUNCIÓN: ACTUALIZAR CUSTOM CLAIMS AL MODIFICAR USUARIO ====================

const { onDocumentUpdated } = require('firebase-functions/v2/firestore');

exports.updateUserClaimsOnUpdate = onDocumentUpdated('usuarios/{userId}', async (event) => {
  const newData = event.data.after.data();
  const oldData = event.data.before.data();
  const userId = event.params.userId;

  // Solo actualizar si cambió el rol o estado
  if (newData.rol === oldData.rol && newData.estado === oldData.estado) {
    console.log(`ℹ️ No hay cambios en rol/estado para ${userId}, omitiendo actualización`);
    return null;
  }

  try {
    // Actualizar custom claims
    await admin.auth().setCustomUserClaims(userId, {
      rol: newData.rol,
      estado: newData.estado
    });

    console.log(`✅ Custom claims actualizados para usuario ${userId}:`);
    console.log(`   - Rol: ${oldData.rol} → ${newData.rol}`);
    console.log(`   - Estado: ${oldData.estado} → ${newData.estado}`);

    return null;
  } catch (error) {
    console.error(`❌ Error al actualizar custom claims para ${userId}:`, error);
    return null;
  }
});

// ==================== FUNCIÓN: ELIMINAR USUARIO DE AUTHENTICATION Y FIRESTORE ====================

const { onCall } = require('firebase-functions/v2/https');

exports.eliminarUsuario = onCall(async (request) => {
  const { userId } = request.data;

  if (!userId) {
    const { HttpsError } = require('firebase-functions/v2/https');
    throw new HttpsError('invalid-argument', 'userId es requerido');
  }

  console.log(`🗑️ Iniciando eliminación de usuario: ${userId}`);

  try {
    // 1. Eliminar de Authentication
    try {
      await admin.auth().deleteUser(userId);
      console.log(`✅ Usuario ${userId} eliminado de Authentication`);
    } catch (authError) {
      // Si el usuario no existe en Authentication, solo log warning
      if (authError.code === 'auth/user-not-found') {
        console.log(`⚠️ Usuario ${userId} no existe en Authentication, solo eliminando de Firestore`);
      } else {
        throw authError;
      }
    }

    // 2. Eliminar de Firestore
    await admin.firestore().collection('usuarios').doc(userId).delete();
    console.log(`✅ Usuario ${userId} eliminado de Firestore`);

    return {
      success: true,
      message: 'Usuario eliminado completamente'
    };
  } catch (error) {
    console.error(`❌ Error al eliminar usuario ${userId}:`, error);
    const { HttpsError } = require('firebase-functions/v2/https');
    throw new HttpsError('internal', `Error al eliminar usuario: ${error.message}`);
  }
});

// ==================== FUNCIÓN OPCIONAL: ENVIAR EMAIL DE BIENVENIDA ====================

exports.sendWelcomeEmail = onDocumentCreated('usuarios/{userId}', async (event) => {
  const snap = event.data;
  const userData = snap.data();
  const userId = event.params.userId;

  // Esta es una función de ejemplo
  // Para enviar emails necesitas configurar un servicio como SendGrid o usar Firebase Extensions

  console.log(`📧 Email de bienvenida para ${userData.email}`);
  console.log(`   Nombre: ${userData.nombre}`);
  console.log(`   Rol: ${userData.rol}`);

  // TODO: Implementar envío de email real si lo necesitas

  return null;
});
