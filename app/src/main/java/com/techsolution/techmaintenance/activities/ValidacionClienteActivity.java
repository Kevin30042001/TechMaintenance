package com.techsolution.techmaintenance.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.helpers.DateUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidacionClienteActivity extends AppCompatActivity {

    // Firebase
    private FirebaseFirestore db;

    // Vistas - Paso 1: Ingresar código
    private LinearLayout layoutIngresarCodigo;
    private TextInputEditText etCodigo;
    private MaterialButton btnValidarCodigo, btnSolicitarReenvio;

    // Vistas - Paso 2: Resumen del servicio
    private LinearLayout layoutResumenServicio;
    private TextView tvFechaServicio, tvDuracionServicio, tvEquipoAtendido, tvTecnicoResponsable;
    private TextView tvTrabajoRealizado, tvObservaciones;
    private LinearLayout containerFotosEvidencia;
    private MaterialButton btnContinuarCalificar;

    // Vistas - Paso 3: Calificar servicio
    private LinearLayout layoutCalificarServicio;
    private RatingBar ratingBar;
    private TextView tvPuntaje;
    private TextInputEditText etComentarios;
    private MaterialButton btnEnviarValidacion;

    // Vistas - Paso 4: Confirmación
    private LinearLayout layoutConfirmacion;
    private TextView tvMensajeGracias;
    private MaterialButton btnCerrar;

    // Vistas comunes
    private Toolbar toolbar;
    private ProgressBar progressBar;

    // Datos
    private String codigoIngresado;
    private String mantenimientoId;
    private Map<String, Object> datosMantenimiento;
    private int pasoActual = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_validacion_cliente);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();

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

        // Mostrar primer paso
        mostrarPaso(1);
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);

        // Paso 1
        layoutIngresarCodigo = findViewById(R.id.layoutIngresarCodigo);
        etCodigo = findViewById(R.id.etCodigo);
        btnValidarCodigo = findViewById(R.id.btnValidarCodigo);
        btnSolicitarReenvio = findViewById(R.id.btnSolicitarReenvio);

        // Paso 2
        layoutResumenServicio = findViewById(R.id.layoutResumenServicio);
        tvFechaServicio = findViewById(R.id.tvFechaServicio);
        tvDuracionServicio = findViewById(R.id.tvDuracionServicio);
        tvEquipoAtendido = findViewById(R.id.tvEquipoAtendido);
        tvTecnicoResponsable = findViewById(R.id.tvTecnicoResponsable);
        tvTrabajoRealizado = findViewById(R.id.tvTrabajoRealizado);
        tvObservaciones = findViewById(R.id.tvObservaciones);
        containerFotosEvidencia = findViewById(R.id.containerFotosEvidencia);
        btnContinuarCalificar = findViewById(R.id.btnContinuarCalificar);

        // Paso 3
        layoutCalificarServicio = findViewById(R.id.layoutCalificarServicio);
        ratingBar = findViewById(R.id.ratingBar);
        tvPuntaje = findViewById(R.id.tvPuntaje);
        etComentarios = findViewById(R.id.etComentarios);
        btnEnviarValidacion = findViewById(R.id.btnEnviarValidacion);

        // Paso 4
        layoutConfirmacion = findViewById(R.id.layoutConfirmacion);
        tvMensajeGracias = findViewById(R.id.tvMensajeGracias);
        btnCerrar = findViewById(R.id.btnCerrar);
    }

    private void configurarListeners() {
        // Validar código
        btnValidarCodigo.setOnClickListener(v -> validarCodigo());

        // Solicitar reenvío
        btnSolicitarReenvio.setOnClickListener(v -> {
            Toast.makeText(this, "Contacte al técnico para solicitar el reenvío del código", Toast.LENGTH_LONG).show();
        });

        // Continuar a calificación
        btnContinuarCalificar.setOnClickListener(v -> mostrarPaso(3));

        // Rating bar
        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            tvPuntaje.setText(String.valueOf((int) rating) + " de 5 estrellas");
        });

        // Enviar validación
        btnEnviarValidacion.setOnClickListener(v -> enviarValidacion());

        // Cerrar
        btnCerrar.setOnClickListener(v -> finish());
    }

    private void validarCodigo() {
        codigoIngresado = etCodigo.getText().toString().trim();

        if (codigoIngresado.isEmpty()) {
            etCodigo.setError("Ingresa el código");
            return;
        }

        if (codigoIngresado.length() != 6) {
            etCodigo.setError("El código debe tener 6 dígitos");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnValidarCodigo.setEnabled(false);

        // Buscar código en colección codigos_validacion
        db.collection("codigos_validacion").document(codigoIngresado)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        mostrarError("Código inválido. Verifica el código e intenta nuevamente.");
                        return;
                    }

                    // Verificar si ya fue usado
                    Boolean usado = documentSnapshot.getBoolean("usado");
                    if (Boolean.TRUE.equals(usado)) {
                        mostrarError("Este código ya fue utilizado.");
                        return;
                    }

                    // Verificar si expiró
                    Timestamp expiracion = documentSnapshot.getTimestamp("fechaExpiracion");
                    if (expiracion != null && expiracion.toDate().before(new java.util.Date())) {
                        mostrarError("Este código ha expirado. Contacte al técnico.");
                        return;
                    }

                    // Código válido, obtener ID del mantenimiento
                    mantenimientoId = documentSnapshot.getString("mantenimientoId");
                    if (mantenimientoId == null) {
                        mostrarError("Error: Mantenimiento no encontrado.");
                        return;
                    }

                    // Cargar datos del mantenimiento
                    cargarMantenimiento();
                })
                .addOnFailureListener(e -> {
                    mostrarError("Error al validar código: " + e.getMessage());
                });
    }

    private void cargarMantenimiento() {
        db.collection("mantenimientos").document(mantenimientoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        mostrarError("Mantenimiento no encontrado.");
                        return;
                    }

                    // Verificar si ya fue validado
                    Boolean validado = documentSnapshot.getBoolean("validadoPorCliente");
                    if (Boolean.TRUE.equals(validado)) {
                        mostrarError("Este servicio ya fue validado anteriormente.");
                        return;
                    }

                    datosMantenimiento = documentSnapshot.getData();

                    // Cargar datos relacionados y mostrar resumen
                    cargarDatosResumen();
                })
                .addOnFailureListener(e -> {
                    mostrarError("Error al cargar datos: " + e.getMessage());
                });
    }

    private void cargarDatosResumen() {
        // Mostrar fecha y duración
        Timestamp fechaFin = (Timestamp) datosMantenimiento.get("fechaFinalizacion");
        if (fechaFin != null) {
            tvFechaServicio.setText(DateUtils.formatearFechaHora(fechaFin));
        }

        String duracion = (String) datosMantenimiento.get("duracionServicio");
        tvDuracionServicio.setText(duracion != null ? duracion : "No disponible");

        String trabajoRealizado = (String) datosMantenimiento.get("descripcionServicio");
        tvTrabajoRealizado.setText(trabajoRealizado != null ? trabajoRealizado : "Sin descripción");

        String observaciones = (String) datosMantenimiento.get("observacionesTecnico");
        if (observaciones != null && !observaciones.isEmpty()) {
            tvObservaciones.setText(observaciones);
        } else {
            tvObservaciones.setText("Sin observaciones adicionales");
        }

        // Cargar equipo
        String equipoId = (String) datosMantenimiento.get("equipoId");
        if (equipoId != null) {
            cargarEquipo(equipoId);
        }

        // Cargar técnico
        String tecnicoId = (String) datosMantenimiento.get("tecnicoPrincipalId");
        if (tecnicoId != null) {
            cargarTecnico(tecnicoId);
        }

        // Cargar fotos de evidencia
        List<String> fotos = (List<String>) datosMantenimiento.get("evidenciasFotograficas");
        if (fotos != null && !fotos.isEmpty()) {
            cargarFotos(fotos);
        }

        progressBar.setVisibility(View.GONE);
        mostrarPaso(2);
    }

    private void cargarEquipo(String equipoId) {
        db.collection("equipos").document(equipoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String marca = documentSnapshot.getString("marca");
                        String modelo = documentSnapshot.getString("modelo");
                        String tipo = documentSnapshot.getString("tipo");
                        tvEquipoAtendido.setText(tipo + " " + marca + " " + modelo);
                    }
                });
    }

    private void cargarTecnico(String tecnicoId) {
        db.collection("usuarios").document(tecnicoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nombre = documentSnapshot.getString("nombre");
                        tvTecnicoResponsable.setText(nombre);
                    }
                });
    }

    private void cargarFotos(List<String> fotosUrls) {
        containerFotosEvidencia.removeAllViews();

        for (String url : fotosUrls) {
            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(300, 300);
            params.setMargins(0, 0, 16, 0);
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.ic_menu)
                    .error(R.drawable.ic_menu)
                    .into(imageView);

            // Click para ver foto grande (opcional)
            imageView.setOnClickListener(v -> {
                // TODO: Mostrar foto en pantalla completa
            });

            containerFotosEvidencia.addView(imageView);
        }
    }

    private void enviarValidacion() {
        float calificacion = ratingBar.getRating();

        if (calificacion == 0) {
            Toast.makeText(this, "Por favor califica el servicio", Toast.LENGTH_SHORT).show();
            return;
        }

        String comentarios = etComentarios.getText().toString().trim();

        progressBar.setVisibility(View.VISIBLE);
        btnEnviarValidacion.setEnabled(false);

        // Actualizar mantenimiento
        Map<String, Object> updates = new HashMap<>();
        updates.put("validadoPorCliente", true);
        updates.put("calificacionCliente", (int) calificacion);
        updates.put("comentarioCliente", comentarios);
        updates.put("fechaValidacion", Timestamp.now());

        db.collection("mantenimientos").document(mantenimientoId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Marcar código como usado
                    marcarCodigoComoUsado();

                    // Actualizar estadísticas del técnico
                    actualizarEstadisticasTecnico();

                    progressBar.setVisibility(View.GONE);
                    mostrarPaso(4);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                    btnEnviarValidacion.setEnabled(true);
                });
    }

    private void marcarCodigoComoUsado() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("usado", true);
        updates.put("usadoEn", Timestamp.now());

        db.collection("codigos_validacion").document(codigoIngresado)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("ValidacionCliente", "✅ Código marcado como usado");
                });
    }

    private void actualizarEstadisticasTecnico() {
        String tecnicoId = (String) datosMantenimiento.get("tecnicoPrincipalId");
        if (tecnicoId == null) return;

        db.collection("usuarios").document(tecnicoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Obtener estadísticas actuales
                        Map<String, Object> estadisticas = (Map<String, Object>) documentSnapshot.get("estadisticas");
                        if (estadisticas == null) {
                            estadisticas = new HashMap<>();
                        }

                        // Incrementar servicios completados
                        Long completados = (Long) estadisticas.get("serviciosCompletados");
                        if (completados == null) completados = 0L;
                        estadisticas.put("serviciosCompletados", completados + 1);

                        // Recalcular calificación promedio
                        Double calificacionPromedio = (Double) estadisticas.get("calificacionPromedio");
                        if (calificacionPromedio == null) calificacionPromedio = 0.0;

                        double nuevaCalificacion = ((calificacionPromedio * completados) + ratingBar.getRating()) / (completados + 1);
                        estadisticas.put("calificacionPromedio", nuevaCalificacion);

                        // Actualizar en Firestore
                        db.collection("usuarios").document(tecnicoId)
                                .update("estadisticas", estadisticas)
                                .addOnSuccessListener(aVoid -> {
                                    android.util.Log.d("ValidacionCliente", "✅ Estadísticas actualizadas");
                                });
                    }
                });
    }

    private void mostrarPaso(int paso) {
        pasoActual = paso;

        // Ocultar todos los layouts
        layoutIngresarCodigo.setVisibility(View.GONE);
        layoutResumenServicio.setVisibility(View.GONE);
        layoutCalificarServicio.setVisibility(View.GONE);
        layoutConfirmacion.setVisibility(View.GONE);

        // Mostrar el layout correspondiente
        switch (paso) {
            case 1:
                layoutIngresarCodigo.setVisibility(View.VISIBLE);
                toolbar.setTitle("Validar Servicio");
                break;
            case 2:
                layoutResumenServicio.setVisibility(View.VISIBLE);
                toolbar.setTitle("Resumen del Servicio");
                break;
            case 3:
                layoutCalificarServicio.setVisibility(View.VISIBLE);
                toolbar.setTitle("Calificar Servicio");
                break;
            case 4:
                layoutConfirmacion.setVisibility(View.VISIBLE);
                toolbar.setTitle("¡Gracias!");
                tvMensajeGracias.setText("¡Gracias por validar el servicio!\n\n" +
                        "Tu calificación de " + (int) ratingBar.getRating() + " estrellas ha sido registrada.\n\n" +
                        "Valoramos tu opinión y seguiremos trabajando para brindarte el mejor servicio.");
                break;
        }
    }

    private void mostrarError(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        progressBar.setVisibility(View.GONE);
        btnValidarCodigo.setEnabled(true);
    }

    @Override
    public void onBackPressed() {
        if (pasoActual == 4) {
            // Si ya terminó, solo cerrar
            finish();
        } else if (pasoActual > 1) {
            // Permitir regresar al paso anterior
            mostrarPaso(pasoActual - 1);
        } else {
            super.onBackPressed();
        }
    }
}
