const { onCall } = require('firebase-functions/v2/https');
const nodemailer = require('nodemailer');

exports.enviarCodigoValidacion = onCall(async (request) => {
  console.log('📧 ========================================');
  console.log('📧 ENVIANDO CÓDIGO DE VALIDACIÓN AL CLIENTE');
  console.log('📧 ========================================');

  // En Functions V2, los datos están en request.data
  const data = request.data;

  console.log('📦 Datos recibidos (request.data):', JSON.stringify(data, null, 2));

  const emailCliente = data.emailCliente;
  const codigo = data.codigo;
  const nombreCliente = data.nombreCliente || 'Cliente';
  const equipoInfo = data.equipoInfo || 'Equipo';
  const tecnicoNombre = data.tecnicoNombre || 'Técnico';
  const linkValidacion = data.linkValidacion;

  if (!emailCliente || !codigo || !linkValidacion) {
    console.error('❌ Faltan datos requeridos');
    console.error('   - emailCliente:', emailCliente ? '✅ OK' : '❌ FALTA');
    console.error('   - codigo:', codigo ? '✅ OK' : '❌ FALTA');
    console.error('   - linkValidacion:', linkValidacion ? '✅ OK' : '❌ FALTA');
    const { HttpsError } = require('firebase-functions/v2/https');
    throw new HttpsError(
      'invalid-argument',
      `Se requieren emailCliente, codigo y linkValidacion. Faltantes: ${!emailCliente ? 'emailCliente ' : ''}${!codigo ? 'codigo ' : ''}${!linkValidacion ? 'linkValidacion' : ''}`
    );
  }

  console.log(`   - Email destino: ${emailCliente}`);
  console.log(`   - Código: ${codigo}`);
  console.log(`   - Cliente: ${nombreCliente}`);
  console.log(`   - Equipo: ${equipoInfo}`);
  console.log(`   - Link: ${linkValidacion}`);

  // Configurar transporter de Nodemailer
  const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
      user: 'techmaintenancetechmaintenance@gmail.com',
      pass: 'obsh sgcj gimh viqa', // App password de Gmail
    },
  });

  // Plantilla HTML del email
  const htmlEmail = `
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Servicio Completado - Validación Requerida</title>
</head>
<body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f6f9;">
  <table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color: #f4f6f9; padding: 20px;">
    <tr>
      <td align="center">
        <table width="600" cellpadding="0" cellspacing="0" border="0" style="background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">

          <!-- Header -->
          <tr>
            <td style="background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%); padding: 30px 20px; text-align: center;">
              <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: 600;">
                ✅ Servicio Completado
              </h1>
              <p style="margin: 10px 0 0 0; color: #E3F2FD; font-size: 16px;">
                TechMaintenance - TechSolution
              </p>
            </td>
          </tr>

          <!-- Body -->
          <tr>
            <td style="padding: 40px 30px;">
              <p style="margin: 0 0 20px 0; font-size: 16px; color: #333; line-height: 1.6;">
                Estimado/a <strong>${nombreCliente}</strong>,
              </p>

              <p style="margin: 0 0 20px 0; font-size: 16px; color: #333; line-height: 1.6;">
                Su servicio de mantenimiento ha sido <strong>completado exitosamente</strong> por nuestro técnico ${tecnicoNombre}.
              </p>

              ${equipoInfo !== 'Equipo' ? `
              <div style="background-color: #f5f5f5; padding: 15px; border-radius: 6px; margin: 20px 0;">
                <p style="margin: 0; font-size: 14px; color: #666;">
                  <strong>Equipo atendido:</strong><br>
                  ${equipoInfo}
                </p>
              </div>
              ` : ''}

              <!-- Sección del Botón Principal -->
              <div style="background: linear-gradient(135deg, #4CAF50 0%, #388E3C 100%); padding: 25px; border-radius: 8px; margin: 30px 0; text-align: center;">
                <p style="margin: 0 0 15px 0; color: #ffffff; font-size: 18px; font-weight: 600;">
                  🌐 Validar Servicio Ahora
                </p>
                <a href="${linkValidacion}"
                   style="display: inline-block; padding: 15px 40px; background-color: #ffffff; color: #4CAF50; text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: 600; box-shadow: 0 2px 6px rgba(0,0,0,0.2);">
                  HACER CLIC AQUÍ PARA VALIDAR
                </a>
                <p style="margin: 15px 0 0 0; color: #E8F5E9; font-size: 13px;">
                  ⏰ Este enlace expira en 24 horas
                </p>
              </div>

              <!-- Código de Validación -->
              <div style="background-color: #FFF3E0; border-left: 4px solid #FF9800; padding: 20px; margin: 25px 0; border-radius: 4px;">
                <p style="margin: 0 0 10px 0; font-size: 14px; color: #666;">
                  Código de validación (por si necesita ingresarlo manualmente):
                </p>
                <p style="margin: 0; font-size: 32px; font-weight: 700; color: #FF9800; letter-spacing: 4px; font-family: 'Courier New', monospace;">
                  ${codigo}
                </p>
              </div>

              <!-- Instrucciones -->
              <div style="background-color: #E3F2FD; padding: 20px; border-radius: 6px; margin: 25px 0;">
                <p style="margin: 0 0 15px 0; font-size: 15px; color: #1976D2; font-weight: 600;">
                  📱 Cómo validar el servicio:
                </p>
                <ol style="margin: 0; padding-left: 20px; color: #333; font-size: 14px; line-height: 1.8;">
                  <li>Haga clic en el botón verde de arriba</li>
                  <li>Se abrirá en su navegador web (NO necesita instalar app)</li>
                  <li>Revise el resumen del servicio realizado</li>
                  <li>Califique el trabajo con estrellas (1-5)</li>
                  <li>Opcionalmente agregue comentarios</li>
                </ol>
              </div>

              <!-- Beneficios -->
              <div style="margin: 25px 0;">
                <p style="margin: 0 0 10px 0; font-size: 14px; color: #666;">
                  ✨ <strong>Ventajas:</strong>
                </p>
                <ul style="margin: 0; padding-left: 20px; color: #666; font-size: 14px; line-height: 1.8;">
                  <li>No necesita instalar ninguna aplicación</li>
                  <li>Funciona desde cualquier navegador web</li>
                  <li>Solo toma 1 minuto completar</li>
                  <li>Puede hacerlo desde su celular, tablet o computadora</li>
                </ul>
              </div>

              <p style="margin: 30px 0 0 0; font-size: 16px; color: #333; line-height: 1.6;">
                Su opinión es muy importante para nosotros y nos ayuda a mejorar continuamente nuestro servicio.
              </p>

              <p style="margin: 20px 0 0 0; font-size: 16px; color: #333;">
                Gracias por confiar en <strong>TechSolution</strong>.
              </p>
            </td>
          </tr>

          <!-- Footer -->
          <tr>
            <td style="background-color: #f5f5f5; padding: 20px; text-align: center; border-top: 1px solid #e0e0e0;">
              <p style="margin: 0 0 10px 0; font-size: 14px; color: #666;">
                TechMaintenance System
              </p>
              <p style="margin: 0; font-size: 12px; color: #999;">
                TechSolution © 2025 - Sistema de Gestión de Mantenimientos
              </p>
              <p style="margin: 10px 0 0 0; font-size: 11px; color: #aaa;">
                Si tiene alguna pregunta, contacte a su técnico de servicio.
              </p>
            </td>
          </tr>

        </table>
      </td>
    </tr>
  </table>
</body>
</html>
  `;

  // Configurar opciones del email
  const mailOptions = {
    from: '"TechMaintenance - TechSolution" <techmaintenancetechmaintenance@gmail.com>',
    to: emailCliente,
    subject: '✅ Servicio Completado - Validación Requerida | TechMaintenance',
    html: htmlEmail,
    text: `Estimado/a ${nombreCliente},

Su servicio de mantenimiento ha sido completado exitosamente.

${equipoInfo !== 'Equipo' ? `Equipo atendido: ${equipoInfo}\n\n` : ''}

═══════════════════════════════════════
    🌐 VALIDAR SERVICIO (CLICK AQUÍ)
═══════════════════════════════════════

${linkValidacion}

═══════════════════════════════════════

⏰ Este enlace expira en 24 horas.

CÓMO VALIDAR EL SERVICIO:

1. Haga clic en el enlace de arriba (se abrirá en su navegador)
2. El código ya está incluido en el enlace
3. Revise el resumen del servicio
4. Califique el trabajo realizado (1-5 estrellas)
5. Opcionalmente agregue comentarios

✨ NO necesita instalar ninguna app
✨ Funciona desde cualquier navegador web
✨ Solo toma 1 minuto validar

Código de validación: ${codigo}
(Por si necesita ingresarlo manualmente)

Gracias por confiar en TechSolution.

---
TechMaintenance System
TechSolution © 2025`
  };

  try {
    console.log('📤 Enviando email...');
    const info = await transporter.sendMail(mailOptions);
    console.log('✅ Email enviado exitosamente');
    console.log(`   - MessageId: ${info.messageId}`);

    return {
      success: true,
      message: 'Código de validación enviado correctamente',
      messageId: info.messageId
    };
  } catch (error) {
    console.error('❌ Error al enviar email:', error);
    const { HttpsError } = require('firebase-functions/v2/https');
    throw new HttpsError(
      'internal',
      `Error al enviar email: ${error.message}`
    );
  }
});
