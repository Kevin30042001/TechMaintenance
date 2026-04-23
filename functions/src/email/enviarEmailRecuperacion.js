const functions = require("firebase-functions");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");

exports.enviarCodigoRecuperacion = functions.https.onCall(
  async (data, context) => {
    try {
      console.log("📦 === INICIO FUNCIÓN ===");

      const email = data.data ? data.data.email : data.email;
      const codigo = data.data ? data.data.codigo : data.codigo;

      console.log("📧 Email:", email);
      console.log("🔢 Código:", codigo);

      if (!email || !codigo) {
        console.error("❌ Faltan datos");
        throw new functions.https.HttpsError(
          "invalid-argument",
          "Email y código son requeridos"
        );
      }

      console.log("✅ Datos validados");
      console.log("📧 Configurando email...");

      const transporter = nodemailer.createTransport({
        service: "gmail",
        auth: {
          user: "techmaintenancetechmaintenance@gmail.com",
          pass: "obsh sgcj gimh viqa",
        },
      });

      const htmlEmail = `
      <!DOCTYPE html>
      <html>
      <head>
          <style>
              body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }
              .container { background-color: white; border-radius: 10px; padding: 30px; max-width: 600px; margin: 0 auto; }
              .header { text-align: center; color: #2196F3; }
              .code-box { background-color: #f0f8ff; border: 2px dashed #2196F3; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0; }
              .code { font-size: 32px; font-weight: bold; color: #2196F3; letter-spacing: 5px; }
              .footer { text-align: center; color: #757575; font-size: 12px; margin-top: 20px; }
          </style>
      </head>
      <body>
          <div class="container">
              <div class="header">
                  <h1>🔧 TechMaintenance</h1>
                  <h2>Código de Recuperación</h2>
              </div>
              <p>Hola,</p>
              <p>Has solicitado restablecer tu contraseña.</p>
              <p>Utiliza el siguiente código en la aplicación:</p>
              <div class="code-box">
                  <div class="code">${codigo}</div>
                  <p style="color: #757575; margin-top: 10px;">Este código expira en 15 minutos</p>
              </div>
              <p>Si no solicitaste este cambio, ignora este mensaje.</p>
              <div class="footer">
                  <p>TechSolution © 2025</p>
                  <p>Sistema de Gestión de Mantenimientos</p>
              </div>
          </div>
      </body>
      </html>
      `;

      const mailOptions = {
        from: '"TechMaintenance" <techmaintenancetechmaintenance@gmail.com>',
        to: email,
        subject: "Código de Recuperación - TechMaintenance",
        html: htmlEmail,
      };

      console.log("📤 Enviando email a:", email);
      await transporter.sendMail(mailOptions);
      console.log("✅ Email enviado exitosamente!");

      return {success: true, message: "Email enviado"};

    } catch (error) {
      console.error("❌ ERROR:", error.message);
      throw new functions.https.HttpsError("internal", error.message);
    }
  }
);

exports.cambiarPasswordRecuperacion = functions.https.onCall(
  async (data, context) => {
    try {
      console.log("🔐 === CAMBIAR PASSWORD ===");

      const email = data.data ? data.data.email : data.email;
      const nuevaPassword = data.data ? data.data.nuevaPassword : data.nuevaPassword;
      const codigo = data.data ? data.data.codigo : data.codigo;

      console.log("📧 Email:", email);

      if (!email || !nuevaPassword || !codigo) {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "Faltan datos requeridos"
        );
      }

      console.log("🔍 Verificando código...");
      const codigoDoc = await admin.firestore()
        .collection("codigos_recuperacion")
        .doc(email)
        .get();

      if (!codigoDoc.exists) {
        throw new functions.https.HttpsError("not-found", "Código no encontrado");
      }

      const codigoData = codigoDoc.data();

      if (codigoData.usado) {
        throw new functions.https.HttpsError("failed-precondition", "Código ya usado");
      }

      if (Date.now() > codigoData.expiraEn) {
        throw new functions.https.HttpsError("failed-precondition", "Código expirado");
      }

      if (codigoData.codigo !== codigo) {
        throw new functions.https.HttpsError("invalid-argument", "Código incorrecto");
      }

      console.log("✅ Código válido");
      console.log("👤 Obteniendo usuario...");

      const userRecord = await admin.auth().getUserByEmail(email);

      console.log("🔐 Actualizando contraseña...");
      await admin.auth().updateUser(userRecord.uid, {password: nuevaPassword});

      console.log("✏️ Marcando código como usado...");
      await admin.firestore()
        .collection("codigos_recuperacion")
        .doc(email)
        .update({usado: true, fechaUso: Date.now()});

      console.log("✅ Contraseña actualizada exitosamente!");
      return {success: true, message: "Contraseña actualizada"};

    } catch (error) {
      console.error("❌ ERROR:", error.message);
      throw new functions.https.HttpsError("internal", error.message);
    }
  }
);

// ============ CLOUD FUNCTION: ENVIAR CREDENCIALES ============
exports.enviarCredencialesTecnico = functions.https.onCall(
  async (data, context) => {
    try {
      console.log("👨‍🔧 === CLOUD FUNCTION: ENVIAR CREDENCIALES TÉCNICO ===");

      const nombre = data.data ? data.data.nombre : data.nombre;
      const email = data.data ? data.data.email : data.email;
      const password = data.data ? data.data.password : data.password;

      // ✅ Usar el helper de emailHelpers.js
      const { enviarCredencialesEmail } = require('./emailHelpers');
      return await enviarCredencialesEmail(nombre, email, password);

    } catch (error) {
      console.error("❌ ERROR:", error.message);
      throw new functions.https.HttpsError("internal", error.message);
    }
  }
);
