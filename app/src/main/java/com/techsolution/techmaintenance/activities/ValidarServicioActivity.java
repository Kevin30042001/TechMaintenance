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

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.helpers.DateUtils;
import com.techsolution.techmaintenance.models.Cliente;
import com.techsolution.techmaintenance.models.Equipo;
import com.techsolution.techmaintenance.models.Mantenimiento;
import com.techsolution.techmaintenance.models.Usuario;

import java.util.HashMap;
import java.util.Map;

public class ValidarServicioActivity extends AppCompatActivity {

    // Firebase
    private FirebaseFirestore db;

    // Vistas - Paso 1: Ingresar código
    private LinearLayout layoutIngresarCodigo;
    private TextInputEditText etCodigo;
    private MaterialButton btnValidarCodigo, btnReenviarCodigo;
    private ProgressBar progressBar1;

    // Vistas - Paso 2: Ver resumen
    private LinearLayout layoutResumenServicio;
    private TextView tvFechaServicio, tvDuracionServicio, tvEquipoAtendido, tvTecnicoResponsable, tvTrabajoRealizado;
    private LinearLayout containerFotosEvidencia;
    private ProgressBar progressBar2;

    // Vistas - Paso 3: Calificar
    private LinearLayout layoutCalificar;
    private RatingBar ratingBar;
    private TextInputEditText etComentarios;
    private MaterialButton btnEnviarValidacion;
    private ProgressBar progressBar3;

    // Vistas - Paso 4: Confirmación
    private LinearLayout layoutConfirmacion;
    private TextView tvMensajeConfirmacion;

    // Datos
    private String codigoIngresado;
    private Mantenimiento mantenimiento;
    private Cliente cliente;
    private Equipo equipo;
    private Usuario tecnico;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_validar_servicio);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();

        // Inicializar vistas
        inicializarVistas();

        // Configurar listeners
        configurarListeners();

        // Verificar si viene código por parámetro (desde email)
        String codigoParam = getIntent().getStringExtra("codigo");
        if (codigoParam != null && !codigoParam.isEmpty()) {
            etCodigo.setText(codigoParam);
            validarCodigo();
        }
    }

    private void inicializarVistas() {
        // Paso 1
        layoutIngresarCodigo = findViewById(R.id.layoutIngresarCodigo);
        etCodigo = findViewById(R.id.etCodigoValidacion);
        btnValidarCodigo = findViewById(R.id.btnValidarCodigo);
        btnReenviarCodigo = findViewById(R.id.btnReenviarCodigo);
        progressBar1 = findViewById(R.id.progressBar1);

        // Paso 2
        layoutResumenServicio = findViewById(R.id.layoutResumenServicio);
        tvFechaServicio = findViewById(R.id.tvFechaServicio);
        tvDuracionServicio = findViewById(R.id.tvDuracionServicio);
        tvEquipoAtendido = findViewById(R.id.tvEquipoAtendido);
        tvTecnicoResponsable = findViewById(R.id.tvTecnicoResponsable);
        tvTrabajoRealizado = findViewById(R.id.tvTrabajoRealizado);
        containerFotosEvidencia = findViewById(R.id.containerFotosEvidencia);
        progressBar2 = findViewById(R.id.progressBar2);

        // Paso 3
        layoutCalificar = findViewById(R.id.layoutCalificar);
        ratingBar = findViewById(R.id.ratingBar);
        etComentarios = findViewById(R.id.etComentarios);
        btnEnviarValidacion = findViewById(R.id.btnEnviarValidacion);
        progressBar3 = findViewById(R.id.progressBar3);

        // Paso 4
        layoutConfirmacion = findViewById(R.id.layoutConfirmacion);
        tvMensajeConfirmacion = findViewById(R.id.tvMensajeConfirmacion);
    }

    private void configurarListeners() {
        btnValidarCodigo.setOnClickListener(v -> validarCodigo());
        btnReenviarCodigo.setOnClickListener(v -> reenviarCodigo());
        btnEnviarValidacion.setOnClickListener(v -> enviarValidacion());
    }

    private void validarCodigo() {
        codigoIngresado = etCodigo.getText().toString().trim();

        if (codigoIngresado.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa el código", Toast.LENGTH_SHORT).show();
            return;
        }

        if (codigoIngresado.length() != 6) {
            Toast.makeText(this, "El código debe tener 6 dígitos", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar1.setVisibility(View.VISIBLE);
        btnValidarCodigo.setEnabled(false);

        // Buscar mantenimiento por código
        db.collection("mantenimientos")
                .whereEqualTo("codigoValidacion", codigoIngresado)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        progressBar1.setVisibility(View.GONE);
                        btnValidarCodigo.setEnabled(true);
                        Toast.makeText(this, "Código inválido o no encontrado", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Obtener mantenimiento
                    mantenimiento = querySnapshot.getDocuments().get(0).toObject(Mantenimiento.class);
                    mantenimiento.setMantenimientoId(querySnapshot.getDocuments().get(0).getId());

                    // Verificar si ya fue validado
                    if (mantenimiento.isValidadoPorCliente()) {
                        progressBar1.setVisibility(View.GONE);
                        btnValidarCodigo.setEnabled(true);
                        Toast.makeText(this, "Este servicio ya fue validado anteriormente", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Verificar expiración (24 horas)
                    if (mantenimiento.getCodigoExpiraEn() != null) {
                        Timestamp ahora = Timestamp.now();
                        if (ahora.compareTo(mantenimiento.getCodigoExpiraEn()) > 0) {
                            progressBar1.setVisibility(View.GONE);
                            btnValidarCodigo.setEnabled(true);
                            Toast.makeText(this, "El código ha expirado (válido por 24 horas)", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    // Código válido, cargar datos relacionados
                    cargarDatosRelacionados();
                })
                .addOnFailureListener(e -> {
                    progressBar1.setVisibility(View.GONE);
                    btnValidarCodigo.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void cargarDatosRelacionados() {
        progressBar2.setVisibility(View.VISIBLE);

        // Cargar equipo
        db.collection("equipos").document(mantenimiento.getEquipoId())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        equipo = doc.toObject(Equipo.class);
                        equipo.setEquipoId(doc.getId());
                    }

                    // Cargar cliente
                    db.collection("clientes").document(mantenimiento.getClienteId())
                            .get()
                            .addOnSuccessListener(docCliente -> {
                                if (docCliente.exists()) {
                                    cliente = docCliente.toObject(Cliente.class);
                                    cliente.setClienteId(docCliente.getId());
                                }

                                // Cargar técnico
                                db.collection("usuarios").document(mantenimiento.getTecnicoPrincipalId())
                                        .get()
                                        .addOnSuccessListener(docTecnico -> {
                                            if (docTecnico.exists()) {
                                                tecnico = docTecnico.toObject(Usuario.class);
                                            }

                                            // Mostrar resumen
                                            mostrarResumen();
                                        });
                            });
                });
    }

    private void mostrarResumen() {
        progressBar1.setVisibility(View.GONE);
        progressBar2.setVisibility(View.GONE);

        // Ocultar paso 1, mostrar paso 2 y 3
        layoutIngresarCodigo.setVisibility(View.GONE);
        layoutResumenServicio.setVisibility(View.VISIBLE);
        layoutCalificar.setVisibility(View.VISIBLE);

        // Llenar datos del resumen
        if (mantenimiento.getFechaFinalizacion() != null) {
            tvFechaServicio.setText(DateUtils.formatearFechaHora(mantenimiento.getFechaFinalizacion()));
        }

        if (mantenimiento.getDuracionServicio() != null) {
            tvDuracionServicio.setText(mantenimiento.getDuracionServicio());
        } else {
            tvDuracionServicio.setText("No especificado");
        }

        if (equipo != null) {
            tvEquipoAtendido.setText(equipo.getMarca() + " " + equipo.getModelo() + " (S/N: " + equipo.getNumeroSerie() + ")");
        }

        if (tecnico != null) {
            tvTecnicoResponsable.setText(tecnico.getNombre());
        }

        if (mantenimiento.getObservacionesTecnico() != null && !mantenimiento.getObservacionesTecnico().isEmpty()) {
            tvTrabajoRealizado.setText(mantenimiento.getObservacionesTecnico());
        } else {
            tvTrabajoRealizado.setText(mantenimiento.getDescripcionServicio());
        }

        // Mostrar fotos de evidencia
        if (mantenimiento.getEvidenciasFotograficas() != null && !mantenimiento.getEvidenciasFotograficas().isEmpty()) {
            containerFotosEvidencia.removeAllViews();
            for (String fotoUrl : mantenimiento.getEvidenciasFotograficas()) {
                ImageView imageView = new ImageView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(300, 300);
                params.setMargins(0, 0, 16, 0);
                imageView.setLayoutParams(params);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                Glide.with(this)
                        .load(fotoUrl)
                        .placeholder(R.drawable.ic_menu)
                        .into(imageView);

                containerFotosEvidencia.addView(imageView);
            }
        }
    }

    private void enviarValidacion() {
        float calificacion = ratingBar.getRating();

        if (calificacion == 0) {
            Toast.makeText(this, "Por favor califica el servicio", Toast.LENGTH_SHORT).show();
            return;
        }

        String comentarios = etComentarios.getText().toString().trim();

        progressBar3.setVisibility(View.VISIBLE);
        btnEnviarValidacion.setEnabled(false);

        // Actualizar mantenimiento
        Map<String, Object> updates = new HashMap<>();
        updates.put("validadoPorCliente", true);
        updates.put("calificacionCliente", (int) calificacion);
        updates.put("comentarioCliente", comentarios);
        updates.put("fechaValidacion", Timestamp.now());

        db.collection("mantenimientos").document(mantenimiento.getMantenimientoId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Actualizar estadísticas del técnico
                    actualizarEstadisticasTecnico((int) calificacion);

                    progressBar3.setVisibility(View.GONE);

                    // Mostrar confirmación
                    layoutResumenServicio.setVisibility(View.GONE);
                    layoutCalificar.setVisibility(View.GONE);
                    layoutConfirmacion.setVisibility(View.VISIBLE);

                    tvMensajeConfirmacion.setText("¡Gracias por tu validación!\n\n" +
                            "Tu calificación de " + (int) calificacion + " estrellas ha sido registrada.\n\n" +
                            "Puedes cerrar esta ventana.");
                })
                .addOnFailureListener(e -> {
                    progressBar3.setVisibility(View.GONE);
                    btnEnviarValidacion.setEnabled(true);
                    Toast.makeText(this, "Error al enviar validación: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void actualizarEstadisticasTecnico(int calificacion) {
        db.collection("usuarios").document(mantenimiento.getTecnicoPrincipalId())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Map<String, Object> estadisticas = (Map<String, Object>) doc.get("estadisticas");
                        if (estadisticas == null) {
                            estadisticas = new HashMap<>();
                        }

                        // Calcular nueva calificación promedio
                        Object serviciosCompletadosObj = estadisticas.get("serviciosCompletados");
                        int serviciosCompletados = serviciosCompletadosObj != null ?
                                ((Number) serviciosCompletadosObj).intValue() : 0;

                        Object calificacionPromedioObj = estadisticas.get("calificacionPromedio");
                        double calificacionPromedio = calificacionPromedioObj != null ?
                                ((Number) calificacionPromedioObj).doubleValue() : 0;

                        // Nueva calificación promedio
                        double nuevaCalificacion = ((calificacionPromedio * serviciosCompletados) + calificacion) / (serviciosCompletados + 1);

                        estadisticas.put("calificacionPromedio", nuevaCalificacion);

                        db.collection("usuarios").document(mantenimiento.getTecnicoPrincipalId())
                                .update("estadisticas", estadisticas);
                    }
                });
    }

    private void reenviarCodigo() {
        Toast.makeText(this, "Contacta con tu proveedor de servicio para reenviar el código", Toast.LENGTH_LONG).show();
    }
}
