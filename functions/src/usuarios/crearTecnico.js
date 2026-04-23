const { onCall, HttpsError } = require('firebase-functions/v2/https');
const admin = require('firebase-admin');

/**
 * Cloud Function: Crear técnico sin afectar la sesión del admin
 *
 * Esta función crea un usuario en Firebase Authentication y Firestore
 * usando el Admin SDK, que NO afecta la sesión actual del dispositivo.
 *
 * @param {Object} request.data - Datos del técnico
 * @param {string} request.data.nombre - Nombre completo
 * @param {string} request.data.email - Email (será el usuario)
 * @param {string} request.data.password - Contraseña temporal
 * @param {string} request.data.telefono - Teléfono (opcional)
 * @param {string} request.data.rol - "admin" o "tecnico"
 * @param {string} request.data.estado - "activo" o "inactivo"
 * @param {boolean} request.data.enviarEmail - Enviar credenciales por email
 *
 * @returns {Object} - { success: boolean, userId: string, message: string }
 */
exports.crearTecnico = onCall(async (request) => {
  console.log('👤 [CREAR TÉCNICO] Iniciando creación de usuario sin afectar sesión');

  const { nombre, email, password, telefono, rol, estado, enviarEmail } = request.data;

  // ============ VALIDACIONES ============
  if (!nombre || !email || !password) {
    throw new HttpsError('invalid-argument', 'nombre, email y password son requeridos');
  }

  if (!rol || (rol !== 'admin' && rol !== 'tecnico')) {
    throw new HttpsError('invalid-argument', 'rol debe ser "admin" o "tecnico"');
  }

  if (!estado || (estado !== 'activo' && estado !== 'inactivo')) {
    throw new HttpsError('invalid-argument', 'estado debe ser "activo" o "inactivo"');
  }

  if (password.length < 6) {
    throw new HttpsError('invalid-argument', 'La contraseña debe tener al menos 6 caracteres');
  }

  // Validar formato de email
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    throw new HttpsError('invalid-argument', 'Email no válido');
  }

  console.log(`📋 Datos recibidos: ${nombre} (${email}) - Rol: ${rol}`);

  try {
    // ============ PASO 1: Crear usuario en Firebase Authentication ============
    console.log('🔐 Creando usuario en Firebase Authentication...');

    let userRecord;
    try {
      userRecord = await admin.auth().createUser({
        email: email,
        password: password,
        displayName: nombre,
        emailVerified: false
      });

      console.log(`✅ Usuario creado en Authentication: ${userRecord.uid}`);
    } catch (authError) {
      console.error('❌ Error al crear en Authentication:', authError);

      // Mensajes de error más claros
      if (authError.code === 'auth/email-already-exists') {
        throw new HttpsError('already-exists', 'Ya existe una cuenta con este email');
      } else if (authError.code === 'auth/invalid-email') {
        throw new HttpsError('invalid-argument', 'El email no es válido');
      } else if (authError.code === 'auth/weak-password') {
        throw new HttpsError('invalid-argument', 'La contraseña es muy débil');
      } else {
        throw new HttpsError('internal', `Error en Authentication: ${authError.message}`);
      }
    }

    const userId = userRecord.uid;

    // ============ PASO 2: Crear documento en Firestore ============
    console.log('📄 Creando documento en Firestore...');

    const userData = {
      nombre: nombre,
      email: email,
      telefono: telefono || '',
      rol: rol,
      estado: estado,
      fechaCreacion: admin.firestore.FieldValue.serverTimestamp(),
      estadisticas: {
        serviciosCompletados: 0,
        calificacionPromedio: 0.0,
        eficiencia: 0,
        totalCalificaciones: 0,
        totalServiciosAsignados: 0
      }
    };

    await admin.firestore()
      .collection('usuarios')
      .doc(userId)
      .set(userData);

    console.log('✅ Documento creado en Firestore');

    // ============ PASO 3: Establecer custom claims (rol y estado) ============
    console.log('🏷️ Estableciendo custom claims...');

    await admin.auth().setCustomUserClaims(userId, {
      rol: rol,
      estado: estado
    });

    console.log('✅ Custom claims establecidos');

    // ============ PASO 4: Enviar email con credenciales (si se solicita) ============
    if (enviarEmail) {
      console.log('📧 Enviando credenciales por email...');

      try {
        // ✅ CORREGIDO: Usar el helper dedicado de emailHelpers.js
        const { enviarCredencialesEmail } = require('../email/emailHelpers');

        // Llamar directamente con los parámetros simples
        await enviarCredencialesEmail(nombre, email, password);

        console.log('✅ Email con credenciales enviado exitosamente');

      } catch (emailError) {
        console.error('⚠️ Error al enviar email (no crítico):', emailError);
        console.error('⚠️ Stack trace:', emailError.stack);
        // No bloquear la creación si el email falla
      }
    }

    // ============ RETORNO EXITOSO ============
    console.log(`✅ Técnico creado exitosamente: ${nombre} (${userId})`);

    return {
      success: true,
      userId: userId,
      message: 'Técnico creado correctamente',
      data: {
        nombre: nombre,
        email: email,
        rol: rol,
        estado: estado
      }
    };

  } catch (error) {
    console.error('❌ Error general al crear técnico:', error);

    // Si ya se creó en Authentication pero falló Firestore, limpiarlo
    if (userRecord && userRecord.uid) {
      console.log(`🧹 Limpiando usuario de Authentication: ${userRecord.uid}`);
      try {
        await admin.auth().deleteUser(userRecord.uid);
        console.log('✅ Usuario eliminado de Authentication (rollback)');
      } catch (deleteError) {
        console.error('❌ Error al hacer rollback:', deleteError);
      }
    }

    // Re-lanzar el error
    if (error instanceof HttpsError) {
      throw error;
    } else {
      throw new HttpsError('internal', `Error al crear técnico: ${error.message}`);
    }
  }
});
