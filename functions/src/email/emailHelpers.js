const nodemailer = require("nodemailer");

/**
 * Helper para enviar credenciales por email a un nuevo técnico
 * Esta función puede ser llamada desde cualquier Cloud Function
 *
 * @param {string} nombre - Nombre del técnico
 * @param {string} email - Email del técnico
 * @param {string} password - Contraseña temporal
 * @returns {Promise<Object>} - { success: true, message: string }
 */
async function enviarCredencialesEmail(nombre, email, password) {
  console.log("👨‍🔧 === HELPER: ENVIAR CREDENCIALES TÉCNICO ===");
  console.log("📧 Email:", email);
  console.log("👤 Nombre:", nombre);

  if (!nombre || !email || !password) {
    throw new Error("Nombre, email y password son requeridos");
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
          .container { background-color: white; border-radius: 10px; padding: 30px; max-width: 600px; margin: 0 auto; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
          .header { text-align: center; color: #2196F3; border-bottom: 3px solid #2196F3; padding-bottom: 20px; margin-bottom: 30px; }
          .credentials-box { background-color: #E3F2FD; border: 2px solid #2196F3; border-radius: 8px; padding: 25px; margin: 25px 0; }
          .credential-row { display: flex; margin: 15px 0; font-size: 16px; }
          .credential-label { font-weight: bold; color: #1976D2; margin-right: 10px; }
          .credential-value { color: #333; font-family: monospace; background: white; padding: 8px 12px; border-radius: 4px; flex: 1; }
          .warning-box { background-color: #FFF3E0; border-left: 4px solid #FF9800; padding: 15px; margin: 20px 0; border-radius: 4px; }
          .steps { background-color: #F5F5F5; padding: 20px; border-radius: 8px; margin: 20px 0; }
          .step { margin: 10px 0; padding-left: 25px; position: relative; }
          .step:before { content: "✓"; position: absolute; left: 0; color: #4CAF50; font-weight: bold; }
          .footer { text-align: center; color: #757575; font-size: 12px; margin-top: 30px; padding-top: 20px; border-top: 1px solid #E0E0E0; }
      </style>
  </head>
  <body>
      <div class="container">
          <div class="header">
              <h1>🔧 TechMaintenance</h1>
              <h2>¡Bienvenido al Equipo!</h2>
          </div>

          <p style="font-size: 16px;">Hola <strong>${nombre}</strong>,</p>

          <p>¡Bienvenido a TechSolution! Tu cuenta en TechMaintenance ha sido creada exitosamente.</p>

          <div class="credentials-box">
              <h3 style="margin-top: 0; color: #1976D2; text-align: center;">🔑 TUS CREDENCIALES DE ACCESO</h3>
              <div class="credential-row">
                  <span class="credential-label">📧 Email:</span>
                  <span class="credential-value">${email}</span>
              </div>
              <div class="credential-row">
                  <span class="credential-label">🔐 Contraseña:</span>
                  <span class="credential-value">${password}</span>
              </div>
          </div>

          <div class="warning-box">
              <strong>⚠️ IMPORTANTE:</strong> Por seguridad, te recomendamos cambiar tu contraseña después del primer inicio de sesión.
          </div>

          <div class="steps">
              <h3 style="margin-top: 0; color: #333;">📱 CÓMO INICIAR SESIÓN:</h3>
              <div class="step">Descarga la app TechMaintenance en tu dispositivo Android</div>
              <div class="step">Ingresa tu email y contraseña</div>
              <div class="step">Presiona "INICIAR SESIÓN"</div>
              <div class="step">Cambia tu contraseña en la sección "Perfil"</div>
          </div>

          <p style="margin-top: 30px;">Si tienes problemas para acceder, contacta a tu administrador.</p>

          <p style="font-weight: bold; color: #2196F3; text-align: center; font-size: 18px; margin-top: 30px;">¡Bienvenido al equipo TechSolution!</p>

          <div class="footer">
              <p>TechMaintenance System</p>
              <p>TechSolution © 2025</p>
              <p style="margin-top: 15px; color: #999; font-size: 11px;">
                  Este es un correo automático, por favor no respondas a este mensaje.
              </p>
          </div>
      </div>
  </body>
  </html>
  `;

  const mailOptions = {
    from: '"TechMaintenance" <techmaintenancetechmaintenance@gmail.com>',
    to: email,
    subject: "🔧 Bienvenido a TechMaintenance - Tus credenciales de acceso",
    html: htmlEmail,
  };

  console.log("📤 Enviando email a:", email);
  await transporter.sendMail(mailOptions);
  console.log("✅ Email enviado exitosamente!");

  return { success: true, message: "Credenciales enviadas por email" };
}

module.exports = {
  enviarCredencialesEmail
};
