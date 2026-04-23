package com.techsolution.techmaintenance.activities;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.helpers.NotificationHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class CompletarServicioActivity extends AppCompatActivity {

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Vistas - Paso 1: Confirmación
    private LinearLayout layoutConfirmacion;
    private TextView tvResumen;
    private MaterialButton btnConfirmarCompletar, btnCancelar;

    // Vistas - Paso 2: Código generado
    private LinearLayout layoutCodigoGenerado;
    private TextView tvCodigoGrande, tvFechaExpiracion, tvEmailCliente;
    private MaterialButton btnCopiarCodigo, btnReenviarEmail, btnVolver;

    // Vistas comunes
    private Toolbar toolbar;
    private ProgressBar progressBar;

    // Datos
    private String mantenimientoId;
    private String codigoValidacion;
    private String emailCliente;
    private Timestamp fechaInicio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completar_servicio);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Obtener ID del mantenimiento
        mantenimientoId = getIntent().getStringExtra("mantenimientoId");
        if (mantenimientoId == null) {
            Toast.makeText(this, "Error: Mantenimiento no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Inicializar vistas
        inicializarVistas();

        // Configurar toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Configurar listeners
        configurarListeners();

        // Cargar datos del mantenimiento
        cargarDatosMantenimiento();

        // Mostrar paso 1 por defecto
        mostrarPasoConfirmacion();
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);

        // Paso 1
        layoutConfirmacion = findViewById(R.id.layoutConfirmacion);
        tvResumen = findViewById(R.id.tvResumen);
        btnConfirmarCompletar = findViewById(R.id.btnConfirmarCompletar);
        btnCancelar = findViewById(R.id.btnCancelar);

        // Paso 2
        layoutCodigoGenerado = findViewById(R.id.layoutCodigoGenerado);
        tvCodigoGrande = findViewById(R.id.tvCodigoGrande);
        tvFechaExpiracion = findViewById(R.id.tvFechaExpiracion);
        tvEmailCliente = findViewById(R.id.tvEmailCliente);
        btnCopiarCodigo = findViewById(R.id.btnCopiarCodigo);
        btnReenviarEmail = findViewById(R.id.btnReenviarEmail);
        btnVolver = findViewById(R.id.btnVolver);
    }

    private void configurarListeners() {
        btnConfirmarCompletar.setOnClickListener(v -> completarServicio());
        btnCancelar.setOnClickListener(v -> finish());
        btnCopiarCodigo.setOnClickListener(v -> copiarCodigo());
        btnReenviarEmail.setOnClickListener(v -> reenviarEmail());
        btnVolver.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });
    }

    private void cargarDatosMantenimiento() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("mantenimientos").document(mantenimientoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String estado = documentSnapshot.getString("estado");
                        fechaInicio = documentSnapshot.getTimestamp("fechaInicio");
                        String clienteId = documentSnapshot.getString("clienteId");

                        // Verificar que esté en proceso
                        if (!"en_proceso".equals(estado)) {
                            Toast.makeText(this, "El mantenimiento no está en proceso", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        // Cargar email del cliente
                        cargarEmailCliente(clienteId);

                        // Calcular duración
                        String duracion = calcularDuracion(fechaInicio);
                        tvResumen.setText("Duración aproximada: " + duracion + "\n\n" +
                                "Al confirmar, se marcará como completado y se generará un código de validación de 6 dígitos que será enviado al cliente por email.\n\n" +
                                "El código expirará en 24 horas.");

                        progressBar.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(this, "Mantenimiento no encontrado", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void cargarEmailCliente(String clienteId) {
        db.collection("clientes").document(clienteId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        emailCliente = documentSnapshot.getString("emailContacto");
                        android.util.Log.d("CompletarServicio", "✅ Email del cliente cargado: " + emailCliente);
                    } else {
                        android.util.Log.w("CompletarServicio", "⚠️ Cliente no encontrado");
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CompletarServicio", "❌ Error al cargar email del cliente: " + e.getMessage());
                });
    }

    private String calcularDuracion(Timestamp inicio) {
        if (inicio == null) return "No disponible";

        long inicioMillis = inicio.toDate().getTime();
        long finMillis = System.currentTimeMillis();
        long diffMillis = finMillis - inicioMillis;

        long horas = diffMillis / (1000 * 60 * 60);
        long minutos = (diffMillis / (1000 * 60)) % 60;

        if (horas > 0) {
            return horas + " hora(s) " + minutos + " minuto(s)";
        } else {
            return minutos + " minuto(s)";
        }
    }

    private void completarServicio() {
        // Primero verificar que el mantenimiento tenga observaciones O fotos
        verificarRequisitosYCompletar();
    }

    private void verificarRequisitosYCompletar() {
        android.util.Log.d("CompletarServicio", "🔍 Verificando requisitos antes de completar");

        db.collection("mantenimientos").document(mantenimientoId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String observaciones = doc.getString("observacionesTecnico");
                        java.util.List<String> fotos = (java.util.List<String>) doc.get("evidenciasFotograficas");

                        boolean tieneObservaciones = observaciones != null && !observaciones.trim().isEmpty();
                        boolean tieneFotos = fotos != null && !fotos.isEmpty();

                        android.util.Log.d("CompletarServicio", "   - Tiene observaciones: " + tieneObservaciones);
                        android.util.Log.d("CompletarServicio", "   - Tiene fotos: " + tieneFotos + (tieneFotos ? " (" + fotos.size() + ")" : ""));

                        if (!tieneObservaciones && !tieneFotos) {
                            // No tiene ni observaciones ni fotos
                            new AlertDialog.Builder(this)
                                    .setTitle("Falta información")
                                    .setMessage("Para completar el servicio debes agregar:\n\n" +
                                            "• Al menos 1 observación sobre el trabajo realizado\n" +
                                            "  O\n" +
                                            "• Al menos 1 foto de evidencia\n\n" +
                                            "¿Deseas regresar para agregar esta información?")
                                    .setPositiveButton("Sí, regresar", (dialog, which) -> finish())
                                    .setNegativeButton("Completar sin evidencias", (dialog, which) -> {
                                        // Permitir completar pero advertir
                                        mostrarAdvertenciaYCompletar();
                                    })
                                    .show();
                        } else {
                            // Todo OK, completar
                            android.util.Log.d("CompletarServicio", "✅ Requisitos cumplidos, completando servicio");
                            ejecutarCompletarServicio();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CompletarServicio", "❌ Error al verificar requisitos: " + e.getMessage());
                    Toast.makeText(this, "Error al verificar requisitos", Toast.LENGTH_SHORT).show();
                });
    }

    private void mostrarAdvertenciaYCompletar() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Advertencia")
                .setMessage("Estás completando el servicio SIN observaciones ni fotos de evidencia.\n\n" +
                        "Esto puede afectar negativamente tu calificación.\n\n" +
                        "¿Confirmas que deseas continuar?")
                .setPositiveButton("Sí, completar", (dialog, which) -> ejecutarCompletarServicio())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ejecutarCompletarServicio() {
        android.util.Log.d("CompletarServicio", "🚀 ========================================");
        android.util.Log.d("CompletarServicio", "🚀 INICIANDO PROCESO DE COMPLETAR SERVICIO");
        android.util.Log.d("CompletarServicio", "🚀 ========================================");

        btnConfirmarCompletar.setEnabled(false);
        btnConfirmarCompletar.setText("PROCESANDO...");
        progressBar.setVisibility(View.VISIBLE);

        // Generar código de 6 dígitos
        codigoValidacion = generarCodigoAleatorio();
        android.util.Log.d("CompletarServicio", "🔢 Código generado: " + codigoValidacion);

        // Generar link de validación web
        String linkValidacion = "https://techmaintenance-798e9.web.app/validar?codigo=" + codigoValidacion + "&id=" + mantenimientoId;
        android.util.Log.d("CompletarServicio", "🔗 Link de validación: " + linkValidacion);

        // Calcular fechas
        Timestamp ahora = Timestamp.now();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 24); // Expira en 24 horas
        Timestamp fechaExpiracion = new Timestamp(cal.getTime());

        // Calcular duración final
        String duracionFinal = calcularDuracion(fechaInicio);

        // Actualizar mantenimiento
        Map<String, Object> updates = new HashMap<>();
        updates.put("estado", "completado");
        updates.put("fechaFinalizacion", ahora);
        updates.put("duracionServicio", duracionFinal);
        updates.put("codigoValidacion", codigoValidacion);
        updates.put("linkValidacion", linkValidacion);
        updates.put("codigoGeneradoEn", ahora);
        updates.put("codigoExpiraEn", fechaExpiracion);
        updates.put("validadoPorCliente", false);

        android.util.Log.d("CompletarServicio", "💾 Guardando en Firestore...");

        db.collection("mantenimientos").document(mantenimientoId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("CompletarServicio", "✅ Mantenimiento completado");

                    // Guardar código en colección separada para validación
                    guardarCodigoValidacion(codigoValidacion, fechaExpiracion);

                    // Enviar email al cliente
                    enviarEmailCliente();

                    // Notificar al administrador
                    notificarAdmin();

                    // Mostrar pantalla de código generado
                    mostrarPasoCodigoGenerado(fechaExpiracion);

                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CompletarServicio", "❌ Error: " + e.getMessage());
                    Toast.makeText(this, "Error al completar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnConfirmarCompletar.setEnabled(true);
                    btnConfirmarCompletar.setText("CONFIRMAR Y COMPLETAR");
                    progressBar.setVisibility(View.GONE);
                });
    }

    private String generarCodigoAleatorio() {
        Random random = new Random();
        int codigo = 100000 + random.nextInt(900000); // Genera número entre 100000 y 999999
        return String.valueOf(codigo);
    }

    private void guardarCodigoValidacion(String codigo, Timestamp expiracion) {
        Map<String, Object> codigoData = new HashMap<>();
        codigoData.put("mantenimientoId", mantenimientoId);
        codigoData.put("emailCliente", emailCliente);
        codigoData.put("fechaGeneracion", Timestamp.now());
        codigoData.put("fechaExpiracion", expiracion);
        codigoData.put("usado", false);
        codigoData.put("usadoEn", null);

        db.collection("codigos_validacion").document(codigo)
                .set(codigoData)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("CompletarServicio", "✅ Código guardado en BD");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CompletarServicio", "❌ Error al guardar código: " + e.getMessage());
                });
    }

    private void notificarAdmin() {
        android.util.Log.d("CompletarServicio", "📢 ========================================");
        android.util.Log.d("CompletarServicio", "📢 INICIANDO NOTIFICACIÓN AL ADMINISTRADOR");
        android.util.Log.d("CompletarServicio", "📢 ========================================");

        // Obtener información del mantenimiento para la notificación
        db.collection("mantenimientos").document(mantenimientoId)
                .get()
                .addOnSuccessListener(mantenimientoDoc -> {
                    if (!mantenimientoDoc.exists()) {
                        android.util.Log.e("CompletarServicio", "❌ Mantenimiento no encontrado para notificar");
                        return;
                    }

                    String equipoId = mantenimientoDoc.getString("equipoId");
                    String clienteId = mantenimientoDoc.getString("clienteId");

                    android.util.Log.d("CompletarServicio", "📋 Cargando datos de equipo y cliente...");

                    // Obtener datos del equipo y cliente
                    db.collection("equipos").document(equipoId)
                            .get()
                            .addOnSuccessListener(equipoDoc -> {
                                String equipoInfo = equipoDoc.exists() ?
                                        equipoDoc.getString("tipo") + " - " +
                                                equipoDoc.getString("marca") + " " +
                                                equipoDoc.getString("modelo") :
                                        "Equipo desconocido";

                                android.util.Log.d("CompletarServicio", "   - Equipo: " + equipoInfo);

                                db.collection("clientes").document(clienteId)
                                        .get()
                                        .addOnSuccessListener(clienteDoc -> {
                                            String clienteInfo = clienteDoc.exists() ?
                                                    clienteDoc.getString("nombreEmpresa") :
                                                    "Cliente desconocido";

                                            android.util.Log.d("CompletarServicio", "   - Cliente: " + clienteInfo);

                                            // Obtener nombre del técnico actual
                                            db.collection("usuarios").document(auth.getCurrentUser().getUid())
                                                    .get()
                                                    .addOnSuccessListener(tecnicoDoc -> {
                                                        String tecnicoNombre = tecnicoDoc.exists() ?
                                                                tecnicoDoc.getString("nombre") :
                                                                "Técnico";

                                                        android.util.Log.d("CompletarServicio", "   - Técnico: " + tecnicoNombre);
                                                        android.util.Log.d("CompletarServicio", "📤 Enviando notificación al administrador...");

                                                        // Enviar notificación a todos los admins activos
                                                        // La Cloud Function busca automáticamente todos los admins
                                                        NotificationHelper notificationHelper = new NotificationHelper(this);
                                                        notificationHelper.notificarServicioCompletado(
                                                                mantenimientoId, // Primer parámetro: mantenimientoId
                                                                tecnicoNombre,
                                                                equipoInfo,
                                                                clienteInfo,
                                                                new NotificationHelper.NotificationCallback() {
                                                                    @Override
                                                                    public void onSuccess() {
                                                                        android.util.Log.d("CompletarServicio", "✅ Notificación enviada correctamente al admin");
                                                                    }

                                                                    @Override
                                                                    public void onFailure(String error) {
                                                                        android.util.Log.e("CompletarServicio", "❌ Error al notificar admin: " + error);
                                                                        // No mostramos Toast para no interrumpir al técnico
                                                                        // El admin recibirá la notificación en su dashboard de todas formas
                                                                    }
                                                                }
                                                        );
                                                    });
                                        });
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CompletarServicio", "❌ Error al cargar datos para notificar: " + e.getMessage());
                });
    }

    private void enviarEmailCliente() {
        android.util.Log.d("CompletarServicio", "📧 Preparando envío de email al cliente...");
        android.util.Log.d("CompletarServicio", "📋 Email cliente actual: " + emailCliente);

        // Si el email ya está cargado, enviar directamente
        if (emailCliente != null && !emailCliente.isEmpty()) {
            android.util.Log.d("CompletarServicio", "✅ Email ya disponible, enviando...");
            enviarPorEmailAutomatico();
            return;
        }

        // Si no está cargado, cargar primero desde Firestore
        android.util.Log.d("CompletarServicio", "⏳ Email no disponible, cargando desde Firestore...");

        db.collection("mantenimientos").document(mantenimientoId)
                .get()
                .addOnSuccessListener(mantenimientoDoc -> {
                    if (mantenimientoDoc.exists()) {
                        String clienteId = mantenimientoDoc.getString("clienteId");

                        if (clienteId != null) {
                            // Cargar email del cliente
                            db.collection("clientes").document(clienteId)
                                    .get()
                                    .addOnSuccessListener(clienteDoc -> {
                                        if (clienteDoc.exists()) {
                                            emailCliente = clienteDoc.getString("emailContacto");
                                            android.util.Log.d("CompletarServicio", "✅ Email cargado: " + emailCliente);

                                            if (emailCliente != null && !emailCliente.isEmpty()) {
                                                // Ahora sí enviar el email
                                                enviarPorEmailAutomatico();
                                            } else {
                                                android.util.Log.w("CompletarServicio", "⚠️ Email del cliente está vacío");
                                                Toast.makeText(this, "Email del cliente no disponible. Usa el botón Reenviar.", Toast.LENGTH_LONG).show();
                                            }
                                        } else {
                                            android.util.Log.w("CompletarServicio", "⚠️ Cliente no encontrado");
                                            Toast.makeText(this, "Cliente no encontrado. Usa el botón Reenviar.", Toast.LENGTH_LONG).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        android.util.Log.e("CompletarServicio", "❌ Error al cargar cliente: " + e.getMessage());
                                        Toast.makeText(this, "Error al cargar datos del cliente.", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CompletarServicio", "❌ Error al cargar mantenimiento: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar datos.", Toast.LENGTH_SHORT).show();
                });
    }

    private void mostrarOpcionesEnvio() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reenviar Código de Validación");
        builder.setMessage("¿Cómo deseas reenviar el código al cliente?");

        builder.setPositiveButton("📧 Gmail", (dialog, which) -> enviarPorEmailAutomatico());
        builder.setNegativeButton("💬 WhatsApp", (dialog, which) -> enviarPorWhatsApp());
        builder.setNeutralButton("Cancelar", null);

        builder.show();
    }

    private void enviarPorEmailAutomatico() {
        android.util.Log.d("CompletarServicio", "📧 ========================================");
        android.util.Log.d("CompletarServicio", "📧 ENVIANDO EMAIL AUTOMÁTICO VÍA CLOUD FUNCTION");
        android.util.Log.d("CompletarServicio", "📧 ========================================");

        // SOLUCIÓN ROBUSTA: Cargar TODOS los datos desde el documento del mantenimiento
        // que ya fue guardado con código y link
        db.collection("mantenimientos").document(mantenimientoId)
                .get()
                .addOnSuccessListener(mantenimientoDoc -> {
                    if (!mantenimientoDoc.exists()) {
                        android.util.Log.e("CompletarServicio", "❌ Mantenimiento no encontrado");
                        Toast.makeText(this, "Error: Mantenimiento no encontrado", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // OBTENER DATOS DEL MANTENIMIENTO (ya guardados)
                    String codigoFromDB = mantenimientoDoc.getString("codigoValidacion");
                    String linkFromDB = mantenimientoDoc.getString("linkValidacion");
                    String clienteId = mantenimientoDoc.getString("clienteId");
                    String equipoId = mantenimientoDoc.getString("equipoId");

                    android.util.Log.d("CompletarServicio", "📋 Datos del mantenimiento:");
                    android.util.Log.d("CompletarServicio", "   - codigo: " + codigoFromDB);
                    android.util.Log.d("CompletarServicio", "   - link: " + linkFromDB);
                    android.util.Log.d("CompletarServicio", "   - clienteId: " + clienteId);
                    android.util.Log.d("CompletarServicio", "   - equipoId: " + equipoId);

                    // VALIDACIÓN CRÍTICA: Verificar que los datos principales existen
                    if (codigoFromDB == null || codigoFromDB.isEmpty()) {
                        android.util.Log.e("CompletarServicio", "❌ ERROR: Código no encontrado en BD");
                        Toast.makeText(this, "Error: Código no disponible", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (linkFromDB == null || linkFromDB.isEmpty()) {
                        android.util.Log.e("CompletarServicio", "❌ ERROR: Link no encontrado en BD");
                        // Generar link como fallback
                        linkFromDB = "https://techmaintenance-798e9.web.app/validar?codigo=" +
                                codigoFromDB + "&id=" + mantenimientoId;
                        android.util.Log.d("CompletarServicio", "⚠️ Link generado como fallback: " + linkFromDB);
                    }

                    if (clienteId == null || clienteId.isEmpty()) {
                        android.util.Log.e("CompletarServicio", "❌ ERROR: ClienteId no encontrado");
                        Toast.makeText(this, "Error: Cliente no encontrado", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Variables finales para usar en callbacks internos
                    final String finalCodigo = codigoFromDB;
                    final String finalLink = linkFromDB;

                    // Cargar datos del CLIENTE
                    db.collection("clientes").document(clienteId)
                            .get()
                            .addOnSuccessListener(clienteDoc -> {
                                if (!clienteDoc.exists()) {
                                    android.util.Log.e("CompletarServicio", "❌ ERROR: Cliente no encontrado");
                                    Toast.makeText(this, "Error: Cliente no encontrado", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                String emailClienteFromDB = clienteDoc.getString("emailContacto");
                                String nombreCliente = clienteDoc.getString("nombreContacto");

                                android.util.Log.d("CompletarServicio", "📋 Datos del cliente:");
                                android.util.Log.d("CompletarServicio", "   - email: " + emailClienteFromDB);
                                android.util.Log.d("CompletarServicio", "   - nombre: " + nombreCliente);

                                // VALIDACIÓN: Email del cliente
                                if (emailClienteFromDB == null || emailClienteFromDB.isEmpty()) {
                                    android.util.Log.e("CompletarServicio", "❌ ERROR: Email del cliente vacío");
                                    Toast.makeText(this, "⚠️ Error: Email del cliente no disponible. Usa el botón Reenviar.", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                // Usar valores por defecto si faltan
                                final String finalNombreCliente = (nombreCliente != null && !nombreCliente.isEmpty()) ?
                                        nombreCliente : "Cliente";

                                // Cargar datos del EQUIPO
                                if (equipoId != null && !equipoId.isEmpty()) {
                                    db.collection("equipos").document(equipoId)
                                            .get()
                                            .addOnSuccessListener(equipoDoc -> {
                                                // Construir información del equipo
                                                String equipoInfo = "Equipo";
                                                if (equipoDoc.exists()) {
                                                    String tipo = equipoDoc.getString("tipo");
                                                    String marca = equipoDoc.getString("marca");
                                                    String modelo = equipoDoc.getString("modelo");
                                                    equipoInfo = ((tipo != null ? tipo : "") + " " +
                                                            (marca != null ? marca : "") + " " +
                                                            (modelo != null ? modelo : "")).trim();
                                                    if (equipoInfo.isEmpty()) equipoInfo = "Equipo";
                                                }

                                                final String finalEquipoInfo = equipoInfo;
                                                android.util.Log.d("CompletarServicio", "📋 Equipo: " + finalEquipoInfo);

                                                // Cargar nombre del TÉCNICO
                                                db.collection("usuarios").document(auth.getCurrentUser().getUid())
                                                        .get()
                                                        .addOnSuccessListener(tecnicoDoc -> {
                                                            String tecnicoNombre = tecnicoDoc.exists() ?
                                                                    tecnicoDoc.getString("nombre") : "Técnico";
                                                            if (tecnicoNombre == null || tecnicoNombre.isEmpty()) {
                                                                tecnicoNombre = "Técnico";
                                                            }

                                                            android.util.Log.d("CompletarServicio", "📋 Técnico: " + tecnicoNombre);

                                                            // PREPARAR DATOS PARA CLOUD FUNCTION
                                                            Map<String, Object> data = new HashMap<>();
                                                            data.put("emailCliente", emailClienteFromDB);
                                                            data.put("codigo", finalCodigo);
                                                            data.put("nombreCliente", finalNombreCliente);
                                                            data.put("equipoInfo", finalEquipoInfo);
                                                            data.put("tecnicoNombre", tecnicoNombre);
                                                            data.put("linkValidacion", finalLink);

                                                            android.util.Log.d("CompletarServicio", "📤 ========================================");
                                                            android.util.Log.d("CompletarServicio", "📤 LLAMANDO A CLOUD FUNCTION");
                                                            android.util.Log.d("CompletarServicio", "📤 Datos que se envían:");
                                                            android.util.Log.d("CompletarServicio", "   - emailCliente: " + emailClienteFromDB);
                                                            android.util.Log.d("CompletarServicio", "   - codigo: " + finalCodigo);
                                                            android.util.Log.d("CompletarServicio", "   - nombreCliente: " + finalNombreCliente);
                                                            android.util.Log.d("CompletarServicio", "   - equipoInfo: " + finalEquipoInfo);
                                                            android.util.Log.d("CompletarServicio", "   - tecnicoNombre: " + tecnicoNombre);
                                                            android.util.Log.d("CompletarServicio", "   - linkValidacion: " + finalLink);
                                                            android.util.Log.d("CompletarServicio", "📤 ========================================");

                                                            // LLAMAR A CLOUD FUNCTION
                                                            FirebaseFunctions.getInstance("us-central1")
                                                                    .getHttpsCallable("enviarCodigoValidacion")
                                                                    .call(data)
                                                                    .addOnSuccessListener(result -> {
                                                                        android.util.Log.d("CompletarServicio", "✅ ========================================");
                                                                        android.util.Log.d("CompletarServicio", "✅ EMAIL ENVIADO EXITOSAMENTE");
                                                                        android.util.Log.d("CompletarServicio", "✅ ========================================");
                                                                        Toast.makeText(this, "📧 Código enviado por email a " + emailClienteFromDB, Toast.LENGTH_LONG).show();
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        android.util.Log.e("CompletarServicio", "❌ ========================================");
                                                                        android.util.Log.e("CompletarServicio", "❌ ERROR AL ENVIAR EMAIL");
                                                                        android.util.Log.e("CompletarServicio", "❌ " + e.getMessage());
                                                                        android.util.Log.e("CompletarServicio", "❌ ========================================");
                                                                        Toast.makeText(this, "⚠️ Error al enviar email: " + e.getMessage() + "\nUsa el botón Reenviar.", Toast.LENGTH_LONG).show();
                                                                    });
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            android.util.Log.e("CompletarServicio", "❌ Error al cargar técnico: " + e.getMessage());
                                                            Toast.makeText(this, "Error al cargar datos del técnico", Toast.LENGTH_SHORT).show();
                                                        });
                                            })
                                            .addOnFailureListener(e -> {
                                                android.util.Log.e("CompletarServicio", "❌ Error al cargar equipo: " + e.getMessage());
                                                Toast.makeText(this, "Error al cargar datos del equipo", Toast.LENGTH_SHORT).show();
                                            });
                                } else {
                                    android.util.Log.e("CompletarServicio", "❌ ERROR: EquipoId vacío");
                                    Toast.makeText(this, "Error: Equipo no encontrado", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("CompletarServicio", "❌ Error al cargar cliente: " + e.getMessage());
                                Toast.makeText(this, "Error al cargar datos del cliente", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CompletarServicio", "❌ Error al cargar mantenimiento: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar datos del mantenimiento", Toast.LENGTH_SHORT).show();
                });
    }

    private void enviarPorWhatsApp() {
        // Cargar teléfono del cliente
        db.collection("mantenimientos").document(mantenimientoId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String clienteId = doc.getString("clienteId");

                        db.collection("clientes").document(clienteId)
                                .get()
                                .addOnSuccessListener(clienteDoc -> {
                                    if (clienteDoc.exists()) {
                                        String telefono = clienteDoc.getString("telefonoContacto");
                                        String nombreCliente = clienteDoc.getString("nombreContacto");

                                        if (telefono != null && !telefono.isEmpty()) {
                                            enviarWhatsAppConDatos(telefono, nombreCliente);
                                        } else {
                                            Toast.makeText(this, "⚠️ Teléfono del cliente no disponible", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al obtener datos del cliente", Toast.LENGTH_SHORT).show();
                });
    }

    private void enviarWhatsAppConDatos(String telefono, String nombreCliente) {
        // Generar link de validación
        String linkValidacion = "https://techmaintenance-798e9.web.app/validar?codigo=" + codigoValidacion + "&id=" + mantenimientoId;

        // Limpiar teléfono (solo números)
        String telefonoLimpio = telefono.replaceAll("[^0-9]", "");

        // Si no tiene código de país, agregar 503 (El Salvador)
        if (!telefonoLimpio.startsWith("503") && telefonoLimpio.length() == 8) {
            telefonoLimpio = "503" + telefonoLimpio;
        }

        String mensaje = "Hola " + (nombreCliente != null ? nombreCliente : "Cliente") + ",%0A%0A" +
                "✅ Su servicio de mantenimiento ha sido *completado exitosamente*.%0A%0A" +
                "═══════════════════════════%0A" +
                "🌐 *VALIDAR SERVICIO*%0A" +
                "═══════════════════════════%0A%0A" +
                "Por favor haga click en este enlace:%0A%0A" +
                linkValidacion + "%0A%0A" +
                "═══════════════════════════%0A%0A" +
                "⏰ Este enlace expira en *24 horas*.%0A%0A" +
                "✨ *NO necesita instalar ninguna app*%0A" +
                "✨ Se abre en su navegador web%0A" +
                "✨ Solo toma 1 minuto%0A%0A" +
                "Por favor califique el servicio recibido.%0A%0A" +
                "Código: *" + codigoValidacion + "*%0A" +
                "(Por si necesita ingresarlo manualmente)%0A%0A" +
                "Gracias por confiar en *TechSolution*.%0A%0A" +
                "----%0A" +
                "_TechMaintenance System_";

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://api.whatsapp.com/send?phone=" + telefonoLimpio + "&text=" + mensaje));
            startActivity(intent);
            android.util.Log.d("CompletarServicio", "✅ Abriendo WhatsApp con teléfono: " + telefonoLimpio);
            android.util.Log.d("CompletarServicio", "✅ Link de validación incluido: " + linkValidacion);
            Toast.makeText(this, "💬 Abriendo WhatsApp...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("CompletarServicio", "❌ Error al abrir WhatsApp: " + e.getMessage());
            Toast.makeText(this, "❌ WhatsApp no está instalado", Toast.LENGTH_SHORT).show();
        }
    }


    private void copiarCodigo() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Código de validación", codigoValidacion);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Código copiado al portapapeles", Toast.LENGTH_SHORT).show();
    }

    private void reenviarEmail() {
        // Mostrar opciones: Gmail o WhatsApp
        mostrarOpcionesEnvio();
    }

    private void mostrarPasoConfirmacion() {
        layoutConfirmacion.setVisibility(View.VISIBLE);
        layoutCodigoGenerado.setVisibility(View.GONE);
    }

    private void mostrarPasoCodigoGenerado(Timestamp expiracion) {
        layoutConfirmacion.setVisibility(View.GONE);
        layoutCodigoGenerado.setVisibility(View.VISIBLE);

        tvCodigoGrande.setText(codigoValidacion);
        tvEmailCliente.setText("Código enviado a: " + (emailCliente != null ? emailCliente : "Email no disponible"));

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        tvFechaExpiracion.setText("Expira: " + sdf.format(expiracion.toDate()));
    }

    @Override
    public void onBackPressed() {
        if (layoutCodigoGenerado.getVisibility() == View.VISIBLE) {
            // Si ya completó, regresar con resultado OK
            setResult(RESULT_OK);
            finish();
        } else {
            // Si está en confirmación, permitir cancelar
            super.onBackPressed();
        }
    }
}
