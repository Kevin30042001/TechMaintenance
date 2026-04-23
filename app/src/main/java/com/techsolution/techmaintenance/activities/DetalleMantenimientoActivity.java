package com.techsolution.techmaintenance.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.helpers.DateUtils;
import com.techsolution.techmaintenance.models.Cliente;
import com.techsolution.techmaintenance.models.Equipo;
import com.techsolution.techmaintenance.models.Mantenimiento;
import com.techsolution.techmaintenance.models.Usuario;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DetalleMantenimientoActivity extends AppCompatActivity {

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;

    // Vistas - Header
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private Chip chipEstado, chipPrioridad;

    // Sección 1: Información del Equipo
    private ImageView ivEquipo;
    private TextView tvMarcaModelo, tvNumeroSerie, tvTipoEquipo, tvUbicacionEquipo;

    // Sección 2: Cliente y Ubicación
    private TextView tvNombreEmpresa, tvContacto, tvTelefonoCliente, tvDireccionCliente;
    private MaterialButton btnLlamar, btnEmail, btnMapa;

    // Sección 3: Detalles del Servicio
    private TextView tvTipo, tvFechaProgramada, tvDescripcion, tvFechaInicio, tvFechaFin, tvDuracion;

    // Sección 4: Equipo de Trabajo
    private TextView tvTecnicoPrincipal, tvTecnicosApoyo;

    // Sección 5: Observaciones
    private TextInputEditText etObservaciones;
    private MaterialButton btnGuardarObservaciones;

    // Sección 6: Evidencias Fotográficas
    private LinearLayout containerFotos;
    private MaterialButton btnAgregarFoto;
    private TextView tvNumFotos;

    // Sección 7: Validación
    private com.google.android.material.card.MaterialCardView layoutValidacion;
    private TextView tvCodigoValidacion, tvEstadoValidacion, tvCalificacion, tvComentarioCliente;

    // Botones de acción
    private LinearLayout layoutBotonesAdmin, layoutBotonesTecnicoPrincipal, layoutBotonesTecnicoApoyo;
    private MaterialButton btnIniciarServicio, btnCompletarServicio, btnCancelarAdmin, btnCancelarTecnico, btnEditar, btnReasignar, btnEliminarMantenimiento;

    // Datos
    private String mantenimientoId;
    private Mantenimiento mantenimiento;
    private Cliente cliente;
    private Equipo equipo;
    private Usuario tecnicoPrincipal;
    private List<Usuario> tecnicosApoyo = new ArrayList<>();

    // Usuario actual
    private String userId;
    private boolean esAdmin = false;
    private boolean esTecnicoPrincipal = false;
    private boolean esTecnicoApoyo = false;
    private boolean rolVerificado = false;

    // Fotos
    private List<String> fotosUrls = new ArrayList<>();
    private static final int PICK_IMAGE_REQUEST = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_mantenimiento);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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

        // Verificar rol del usuario
        verificarRolUsuario();

        // Configurar listeners
        configurarListeners();

        // Cargar datos del mantenimiento
        cargarMantenimiento();
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);
        chipEstado = findViewById(R.id.chipEstado);
        chipPrioridad = findViewById(R.id.chipPrioridad);

        // Sección 1
        ivEquipo = findViewById(R.id.ivEquipo);
        tvMarcaModelo = findViewById(R.id.tvMarcaModelo);
        tvNumeroSerie = findViewById(R.id.tvNumeroSerie);
        tvTipoEquipo = findViewById(R.id.tvTipoEquipo);
        tvUbicacionEquipo = findViewById(R.id.tvUbicacionEquipo);

        // Sección 2
        tvNombreEmpresa = findViewById(R.id.tvNombreEmpresa);
        tvContacto = findViewById(R.id.tvContacto);
        tvTelefonoCliente = findViewById(R.id.tvTelefonoCliente);
        tvDireccionCliente = findViewById(R.id.tvDireccionCliente);
        btnLlamar = findViewById(R.id.btnLlamar);
        btnEmail = findViewById(R.id.btnEmail);
        btnMapa = findViewById(R.id.btnMapa);

        // Sección 3
        tvTipo = findViewById(R.id.tvTipo);
        tvFechaProgramada = findViewById(R.id.tvFechaProgramada);
        tvDescripcion = findViewById(R.id.tvDescripcion);
        tvFechaInicio = findViewById(R.id.tvFechaInicio);
        tvFechaFin = findViewById(R.id.tvFechaFin);
        tvDuracion = findViewById(R.id.tvDuracion);

        // Sección 4
        tvTecnicoPrincipal = findViewById(R.id.tvTecnicoPrincipal);
        tvTecnicosApoyo = findViewById(R.id.tvTecnicosApoyo);

        // Sección 5
        etObservaciones = findViewById(R.id.etObservaciones);
        btnGuardarObservaciones = findViewById(R.id.btnGuardarObservaciones);

        // Sección 6
        containerFotos = findViewById(R.id.containerFotos);
        btnAgregarFoto = findViewById(R.id.btnAgregarFoto);
        tvNumFotos = findViewById(R.id.tvNumFotos);

        // Sección 7
        layoutValidacion = findViewById(R.id.layoutValidacion);
        tvCodigoValidacion = findViewById(R.id.tvCodigoValidacion);
        tvEstadoValidacion = findViewById(R.id.tvEstadoValidacion);
        tvCalificacion = findViewById(R.id.tvCalificacion);
        tvComentarioCliente = findViewById(R.id.tvComentarioCliente);

        // Botones
        layoutBotonesAdmin = findViewById(R.id.layoutBotonesAdmin);
        layoutBotonesTecnicoPrincipal = findViewById(R.id.layoutBotonesTecnicoPrincipal);
        layoutBotonesTecnicoApoyo = findViewById(R.id.layoutBotonesTecnicoApoyo);
        btnIniciarServicio = findViewById(R.id.btnIniciarServicio);
        btnCompletarServicio = findViewById(R.id.btnCompletarServicio);
        btnCancelarAdmin = findViewById(R.id.btnCancelarAdmin);
        btnCancelarTecnico = findViewById(R.id.btnCancelarTecnico);
        btnEditar = findViewById(R.id.btnEditar);
        btnReasignar = findViewById(R.id.btnReasignar);
        btnEliminarMantenimiento = findViewById(R.id.btnEliminarMantenimiento);
    }

    private void verificarRolUsuario() {
        db.collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String rol = documentSnapshot.getString("rol");
                        esAdmin = "admin".equals(rol);
                        rolVerificado = true;

                        // Reconfigurar botones después de obtener el rol
                        if (mantenimiento != null) {
                            configurarBotonesSegunRolYEstado();
                        }
                    }
                });
    }

    private void configurarListeners() {
        // Botón Llamar
        if (btnLlamar != null) {
            btnLlamar.setOnClickListener(v -> {
                if (cliente != null && cliente.getTelefonoContacto() != null) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + cliente.getTelefonoContacto()));
                    startActivity(intent);
                }
            });
        }

        // Botón Email
        if (btnEmail != null) {
            btnEmail.setOnClickListener(v -> {
                if (cliente != null && cliente.getEmailContacto() != null) {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:" + cliente.getEmailContacto()));
                    startActivity(intent);
                }
            });
        }

        // Botón Mapa
        if (btnMapa != null) {
            btnMapa.setOnClickListener(v -> {
                if (cliente != null && cliente.getDireccion() != null) {
                    try {
                        // Intentar abrir Google Maps
                        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(cliente.getDireccion()));
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");

                        if (mapIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(mapIntent);
                        } else {
                            // Si Google Maps no está instalado, abrir en navegador
                            Uri webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(cliente.getDireccion()));
                            Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
                            startActivity(webIntent);
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "No se pudo abrir el mapa", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Dirección no disponible", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Guardar observaciones
        if (btnGuardarObservaciones != null) {
            btnGuardarObservaciones.setOnClickListener(v -> guardarObservaciones());
        }

        // Agregar foto
        if (btnAgregarFoto != null) {
            btnAgregarFoto.setOnClickListener(v -> seleccionarFoto());
        }

        // Iniciar servicio
        if (btnIniciarServicio != null) {
            btnIniciarServicio.setOnClickListener(v -> iniciarServicio());
        }

        // Completar servicio
        if (btnCompletarServicio != null) {
            btnCompletarServicio.setOnClickListener(v -> {
                Intent intent = new Intent(this, CompletarServicioActivity.class);
                intent.putExtra("mantenimientoId", mantenimientoId);
                startActivityForResult(intent, 300);
            });
        }

        // Cancelar Admin
        if (btnCancelarAdmin != null) {
            btnCancelarAdmin.setOnClickListener(v -> mostrarDialogoCancelar());
        }

        // Cancelar Técnico
        if (btnCancelarTecnico != null) {
            btnCancelarTecnico.setOnClickListener(v -> mostrarDialogoCancelar());
        }

        // Eliminar (Admin)
        if (btnEliminarMantenimiento != null) {
            btnEliminarMantenimiento.setOnClickListener(v -> mostrarDialogoEliminar());
        }

        // Editar (Admin)
        if (btnEditar != null) {
            btnEditar.setOnClickListener(v -> mostrarDialogoEditar());
        }

        // Reasignar (Admin)
        if (btnReasignar != null) {
            btnReasignar.setOnClickListener(v -> mostrarDialogoReasignar());
        }
    }

    private void cargarMantenimiento() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("mantenimientos").document(mantenimientoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        mantenimiento = documentSnapshot.toObject(Mantenimiento.class);
                        if (mantenimiento != null) {
                            mantenimiento.setMantenimientoId(documentSnapshot.getId());

                            // Determinar rol del usuario en este mantenimiento
                            esTecnicoPrincipal = userId.equals(mantenimiento.getTecnicoPrincipalId());
                            esTecnicoApoyo = mantenimiento.getTecnicosApoyo() != null &&
                                    mantenimiento.getTecnicosApoyo().contains(userId);

                            // Cargar datos relacionados
                            cargarEquipo();
                            cargarCliente();
                            cargarTecnicoPrincipal();
                            cargarTecnicosApoyo();

                            // Mostrar datos
                            mostrarDatosMantenimiento();

                            // Solo configurar botones si el rol ya fue verificado
                            if (rolVerificado) {
                                configurarBotonesSegunRolYEstado();
                            }
                        }
                    } else {
                        Toast.makeText(this, "Mantenimiento no encontrado", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void cargarEquipo() {
        db.collection("equipos").document(mantenimiento.getEquipoId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        equipo = documentSnapshot.toObject(Equipo.class);
                        if (equipo != null) {
                            equipo.setEquipoId(documentSnapshot.getId());
                            mostrarDatosEquipo();
                        }
                    }
                });
    }

    private void cargarCliente() {
        db.collection("clientes").document(mantenimiento.getClienteId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        cliente = documentSnapshot.toObject(Cliente.class);
                        if (cliente != null) {
                            cliente.setClienteId(documentSnapshot.getId());
                            mostrarDatosCliente();
                        }
                    }
                });
    }

    private void cargarTecnicoPrincipal() {
        db.collection("usuarios").document(mantenimiento.getTecnicoPrincipalId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tecnicoPrincipal = documentSnapshot.toObject(Usuario.class);
                        if (tecnicoPrincipal != null) {
                            tvTecnicoPrincipal.setText(tecnicoPrincipal.getNombre());
                        }
                    }
                });
    }

    private void cargarTecnicosApoyo() {
        if (mantenimiento.getTecnicosApoyo() == null || mantenimiento.getTecnicosApoyo().isEmpty()) {
            tvTecnicosApoyo.setText("Sin técnicos de apoyo");
            return;
        }

        StringBuilder nombresApoyo = new StringBuilder();
        for (String tecnicoId : mantenimiento.getTecnicosApoyo()) {
            db.collection("usuarios").document(tecnicoId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String nombre = documentSnapshot.getString("nombre");
                            if (nombresApoyo.length() > 0) {
                                nombresApoyo.append(", ");
                            }
                            nombresApoyo.append(nombre);
                            tvTecnicosApoyo.setText(nombresApoyo.toString());
                        }
                    });
        }
    }

    private void mostrarDatosEquipo() {
        if (equipo == null) return;

        tvMarcaModelo.setText(equipo.getMarca() + " " + equipo.getModelo());
        tvNumeroSerie.setText("S/N: " + equipo.getNumeroSerie());
        tvTipoEquipo.setText(capitalizarPrimeraLetra(equipo.getTipo()));
        tvUbicacionEquipo.setText(equipo.getUbicacionEspecifica());

        // Cargar imagen del equipo
        if (equipo.getFotografiaURL() != null && !equipo.getFotografiaURL().isEmpty()) {
            Glide.with(this)
                    .load(equipo.getFotografiaURL())
                    .placeholder(R.drawable.ic_computer)
                    .error(R.drawable.ic_computer)
                    .into(ivEquipo);
        }
    }

    private void mostrarDatosCliente() {
        if (cliente == null) return;

        tvNombreEmpresa.setText(cliente.getNombreEmpresa());
        tvContacto.setText(cliente.getNombreContacto() + " - " + cliente.getCargoContacto());
        tvTelefonoCliente.setText(cliente.getTelefonoContacto());
        tvDireccionCliente.setText(cliente.getDireccion());
    }

    private void mostrarDatosMantenimiento() {
        if (mantenimiento == null) return;

        // Estado y prioridad
        chipEstado.setText(capitalizarPrimeraLetra(mantenimiento.getEstado()));
        chipPrioridad.setText(capitalizarPrimeraLetra(mantenimiento.getPrioridad()));

        // Configurar colores
        chipEstado.setChipBackgroundColorResource(obtenerColorEstado(mantenimiento.getEstado()));
        chipPrioridad.setChipBackgroundColorResource(obtenerColorPrioridad(mantenimiento.getPrioridad()));

        // Detalles del servicio
        tvTipo.setText(capitalizarPrimeraLetra(mantenimiento.getTipo()));
        tvFechaProgramada.setText(DateUtils.formatearFechaHora(mantenimiento.getFechaProgramada()));
        tvDescripcion.setText(mantenimiento.getDescripcionServicio());

        // Fechas de ejecución
        if (mantenimiento.getFechaInicio() != null) {
            tvFechaInicio.setText(DateUtils.formatearFechaHora(mantenimiento.getFechaInicio()));
        } else {
            tvFechaInicio.setText("No iniciado");
        }

        if (mantenimiento.getFechaFinalizacion() != null) {
            tvFechaFin.setText(DateUtils.formatearFechaHora(mantenimiento.getFechaFinalizacion()));
        } else {
            tvFechaFin.setText("No finalizado");
        }

        if (mantenimiento.getDuracionServicio() != null) {
            tvDuracion.setText(mantenimiento.getDuracionServicio());
        } else {
            tvDuracion.setText("--");
        }

        // Observaciones
        if (mantenimiento.getObservacionesTecnico() != null) {
            etObservaciones.setText(mantenimiento.getObservacionesTecnico());
        }

        // Fotos
        if (mantenimiento.getEvidenciasFotograficas() != null) {
            fotosUrls = mantenimiento.getEvidenciasFotograficas();
            mostrarFotos();
        }

        // Validación
        if (mantenimiento.getCodigoValidacion() != null) {
            layoutValidacion.setVisibility(View.VISIBLE);
            tvCodigoValidacion.setText(mantenimiento.getCodigoValidacion());

            if (mantenimiento.isValidadoPorCliente()) {
                tvEstadoValidacion.setText("✓ Validado");
                tvEstadoValidacion.setTextColor(getResources().getColor(R.color.success, null));
                tvCalificacion.setText(mantenimiento.getCalificacionCliente() + " estrellas");
                tvComentarioCliente.setText(mantenimiento.getComentarioCliente());
            } else {
                tvEstadoValidacion.setText("Pendiente de validación");
                tvEstadoValidacion.setTextColor(getResources().getColor(R.color.warning, null));
            }
        } else {
            layoutValidacion.setVisibility(View.GONE);
        }

        progressBar.setVisibility(View.GONE);
    }

    private void mostrarFotos() {
        containerFotos.removeAllViews();
        tvNumFotos.setText(fotosUrls.size() + " foto(s)");

        for (String url : fotosUrls) {
            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(300, 300);
            params.setMargins(0, 0, 16, 0);
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.ic_menu)
                    .into(imageView);

            containerFotos.addView(imageView);
        }
    }

    private void configurarBotonesSegunRolYEstado() {
        // Ocultar todos primero
        layoutBotonesAdmin.setVisibility(View.GONE);
        layoutBotonesTecnicoPrincipal.setVisibility(View.GONE);
        layoutBotonesTecnicoApoyo.setVisibility(View.GONE);
        if (btnEliminarMantenimiento != null) {
            btnEliminarMantenimiento.setVisibility(View.GONE);
        }

        String estado = mantenimiento.getEstado();

        if (esAdmin) {
            layoutBotonesAdmin.setVisibility(View.VISIBLE);
            // Mostrar botón eliminar solo para admin
            if (btnEliminarMantenimiento != null) {
                btnEliminarMantenimiento.setVisibility(View.VISIBLE);
            }
        } else if (esTecnicoPrincipal) {
            layoutBotonesTecnicoPrincipal.setVisibility(View.VISIBLE);

            // Mostrar botones según estado
            if ("programado".equals(estado)) {
                btnIniciarServicio.setVisibility(View.VISIBLE);
                btnCompletarServicio.setVisibility(View.GONE);
            } else if ("en_proceso".equals(estado)) {
                btnIniciarServicio.setVisibility(View.GONE);
                btnCompletarServicio.setVisibility(View.VISIBLE);
            } else {
                btnIniciarServicio.setVisibility(View.GONE);
                btnCompletarServicio.setVisibility(View.GONE);
            }
        } else if (esTecnicoApoyo) {
            layoutBotonesTecnicoApoyo.setVisibility(View.VISIBLE);
        }

        // Habilitar/deshabilitar observaciones
        boolean puedeEditarObservaciones = esTecnicoPrincipal || esTecnicoApoyo;
        etObservaciones.setEnabled(puedeEditarObservaciones);
        btnGuardarObservaciones.setVisibility(puedeEditarObservaciones ? View.VISIBLE : View.GONE);

        // Habilitar/deshabilitar agregar fotos
        btnAgregarFoto.setVisibility((esTecnicoPrincipal || esTecnicoApoyo) && fotosUrls.size() < 5 ? View.VISIBLE : View.GONE);
    }

    private void iniciarServicio() {
        new AlertDialog.Builder(this)
                .setTitle("Iniciar Servicio")
                .setMessage("¿Confirmas que estás iniciando el servicio de mantenimiento?")
                .setPositiveButton("Iniciar", (dialog, which) -> {
                    Timestamp ahora = Timestamp.now();
                    db.collection("mantenimientos").document(mantenimientoId)
                            .update("estado", "en_proceso", "fechaInicio", ahora)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Servicio iniciado", Toast.LENGTH_SHORT).show();
                                cargarMantenimiento();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void guardarObservaciones() {
        String observaciones = etObservaciones.getText().toString().trim();

        db.collection("mantenimientos").document(mantenimientoId)
                .update("observacionesTecnico", observaciones)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Observaciones guardadas", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void seleccionarFoto() {
        if (fotosUrls.size() >= 5) {
            Toast.makeText(this, "Máximo 5 fotos", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Seleccionar foto"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            subirFoto(imageUri);
        } else if (requestCode == 300 && resultCode == RESULT_OK) {
            // Volviendo de CompletarServicioActivity
            cargarMantenimiento();
        }
    }

    private void subirFoto(Uri imageUri) {
        // Mostrar progreso
        progressBar.setVisibility(View.VISIBLE);
        btnAgregarFoto.setEnabled(false);
        btnAgregarFoto.setText("Subiendo...");

        android.util.Log.d("DetalleMantenimiento", "📤 Iniciando subida de foto");

        String fileName = "mantenimiento_" + mantenimientoId + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child("evidencias/" + fileName);

        ref.putFile(imageUri)
                .addOnProgressListener(taskSnapshot -> {
                    // Calcular progreso
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    android.util.Log.d("DetalleMantenimiento", "📊 Progreso: " + (int) progress + "%");
                })
                .addOnSuccessListener(taskSnapshot -> {
                    android.util.Log.d("DetalleMantenimiento", "✅ Foto subida, obteniendo URL");

                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        fotosUrls.add(uri.toString());
                        android.util.Log.d("DetalleMantenimiento", "✅ URL obtenida: " + uri.toString());

                        db.collection("mantenimientos").document(mantenimientoId)
                                .update("evidenciasFotograficas", fotosUrls)
                                .addOnSuccessListener(aVoid -> {
                                    android.util.Log.d("DetalleMantenimiento", "✅ Foto guardada en Firestore");
                                    Toast.makeText(this, "Foto agregada exitosamente (" + fotosUrls.size() + "/5)", Toast.LENGTH_SHORT).show();
                                    mostrarFotos();
                                    configurarBotonesSegunRolYEstado();

                                    // Restaurar botón
                                    progressBar.setVisibility(View.GONE);
                                    btnAgregarFoto.setEnabled(true);
                                    btnAgregarFoto.setText("Agregar Foto");
                                })
                                .addOnFailureListener(e -> {
                                    android.util.Log.e("DetalleMantenimiento", "❌ Error al guardar en Firestore: " + e.getMessage());
                                    Toast.makeText(this, "Error al guardar foto en BD: " + e.getMessage(), Toast.LENGTH_LONG).show();

                                    // Restaurar botón
                                    progressBar.setVisibility(View.GONE);
                                    btnAgregarFoto.setEnabled(true);
                                    btnAgregarFoto.setText("Agregar Foto");
                                });
                    }).addOnFailureListener(e -> {
                        android.util.Log.e("DetalleMantenimiento", "❌ Error al obtener URL: " + e.getMessage());
                        Toast.makeText(this, "Error al obtener URL de foto: " + e.getMessage(), Toast.LENGTH_LONG).show();

                        // Restaurar botón
                        progressBar.setVisibility(View.GONE);
                        btnAgregarFoto.setEnabled(true);
                        btnAgregarFoto.setText("Agregar Foto");
                    });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DetalleMantenimiento", "❌ Error al subir foto: " + e.getMessage());
                    Toast.makeText(this, "Error al subir foto: " + e.getMessage(), Toast.LENGTH_LONG).show();

                    // Restaurar botón
                    progressBar.setVisibility(View.GONE);
                    btnAgregarFoto.setEnabled(true);
                    btnAgregarFoto.setText("Agregar Foto");
                });
    }

    private void mostrarDialogoCancelar() {
        new AlertDialog.Builder(this)
                .setTitle("Cancelar Mantenimiento")
                .setMessage("¿Estás seguro de cancelar este mantenimiento?")
                .setPositiveButton("Sí, cancelar", (dialog, which) -> {
                    db.collection("mantenimientos").document(mantenimientoId)
                            .update("estado", "cancelado")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Mantenimiento cancelado", Toast.LENGTH_SHORT).show();
                                cargarMantenimiento();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private int obtenerColorEstado(String estado) {
        switch (estado.toLowerCase()) {
            case "programado":
                return R.color.status_programado;
            case "en_proceso":
                return R.color.status_en_proceso;
            case "completado":
                return R.color.success;
            case "cancelado":
                return R.color.error;
            default:
                return R.color.secondary_text;
        }
    }

    private int obtenerColorPrioridad(String prioridad) {
        switch (prioridad.toLowerCase()) {
            case "urgente":
                return R.color.priority_urgent;
            case "alta":
                return R.color.priority_high;
            case "media":
                return R.color.priority_medium;
            case "baja":
            default:
                return R.color.priority_low;
        }
    }

    private String capitalizarPrimeraLetra(String texto) {
        if (texto == null || texto.isEmpty()) return texto;
        return texto.substring(0, 1).toUpperCase() + texto.substring(1).toLowerCase();
    }

    private void mostrarDialogoEliminar() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Mantenimiento")
                .setMessage("¿Estás seguro de que deseas eliminar este mantenimiento?\n\n" +
                        "ADVERTENCIA: Esta acción eliminará:\n" +
                        "• El registro del mantenimiento\n" +
                        "• Las evidencias fotográficas\n" +
                        "• El código de validación (si existe)\n\n" +
                        "Esta acción NO se puede deshacer.\n\n" +
                        "¿Continuar?")
                .setPositiveButton("Sí, eliminar", (dialog, which) -> eliminarMantenimiento())
                .setNegativeButton("Cancelar", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void eliminarMantenimiento() {
        android.util.Log.d("DetalleMantenimiento", "🗑️ Iniciando eliminación de mantenimiento: " + mantenimientoId);

        progressBar.setVisibility(View.VISIBLE);

        // Paso 1: Eliminar fotos de Storage
        if (fotosUrls != null && !fotosUrls.isEmpty()) {
            android.util.Log.d("DetalleMantenimiento", "📸 Eliminando " + fotosUrls.size() + " fotos...");
            for (String fotoUrl : fotosUrls) {
                try {
                    StorageReference fotoRef = storage.getReferenceFromUrl(fotoUrl);
                    fotoRef.delete()
                            .addOnSuccessListener(aVoid -> android.util.Log.d("DetalleMantenimiento", "✅ Foto eliminada"))
                            .addOnFailureListener(e -> android.util.Log.w("DetalleMantenimiento", "⚠️ Error al eliminar foto: " + e.getMessage()));
                } catch (Exception e) {
                    android.util.Log.w("DetalleMantenimiento", "⚠️ Error al parsear URL: " + e.getMessage());
                }
            }
        }

        // Paso 2: Eliminar código de validación si existe
        if (mantenimiento.getCodigoValidacion() != null && !mantenimiento.getCodigoValidacion().isEmpty()) {
            android.util.Log.d("DetalleMantenimiento", "🔢 Eliminando código de validación...");
            db.collection("codigos_validacion").document(mantenimiento.getCodigoValidacion())
                    .delete()
                    .addOnSuccessListener(aVoid -> android.util.Log.d("DetalleMantenimiento", "✅ Código eliminado"))
                    .addOnFailureListener(e -> android.util.Log.w("DetalleMantenimiento", "⚠️ Error al eliminar código: " + e.getMessage()));
        }

        // Paso 3: Eliminar documento de mantenimiento
        db.collection("mantenimientos").document(mantenimientoId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("DetalleMantenimiento", "✅ Mantenimiento eliminado de Firestore");

                    // Paso 4: Actualizar contador de mantenimientos del equipo
                    if (mantenimiento.getEquipoId() != null && !mantenimiento.getEquipoId().isEmpty()) {
                        db.collection("equipos").document(mantenimiento.getEquipoId())
                                .get()
                                .addOnSuccessListener(doc -> {
                                    if (doc.exists()) {
                                        Long totalMantenimientos = doc.getLong("totalMantenimientos");
                                        int nuevoTotal = (totalMantenimientos != null ? totalMantenimientos.intValue() : 0) - 1;
                                        if (nuevoTotal < 0) nuevoTotal = 0;

                                        db.collection("equipos").document(mantenimiento.getEquipoId())
                                                .update("totalMantenimientos", nuevoTotal);
                                    }
                                });
                    }

                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "✅ Mantenimiento eliminado correctamente", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    android.util.Log.e("DetalleMantenimiento", "❌ Error al eliminar mantenimiento: " + e.getMessage());
                    Toast.makeText(this, "❌ Error al eliminar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void mostrarDialogoEditar() {
        // Crear diálogo personalizado con campos editables
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_editar_mantenimiento, null);

        TextInputEditText etDescripcionEdit = dialogView.findViewById(R.id.etDescripcionEdit);
        AutoCompleteTextView actvTipoEdit = dialogView.findViewById(R.id.actvTipoEdit);
        AutoCompleteTextView actvPrioridadEdit = dialogView.findViewById(R.id.actvPrioridadEdit);
        TextInputEditText etFechaEdit = dialogView.findViewById(R.id.etFechaEdit);

        // Configurar dropdowns
        String[] tipos = {"preventivo", "correctivo", "emergencia"};
        ArrayAdapter<String> tipoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, tipos);
        actvTipoEdit.setAdapter(tipoAdapter);

        String[] prioridades = {"baja", "media", "alta", "urgente"};
        ArrayAdapter<String> prioridadAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, prioridades);
        actvPrioridadEdit.setAdapter(prioridadAdapter);

        // Pre-llenar con datos actuales
        if (mantenimiento != null) {
            etDescripcionEdit.setText(mantenimiento.getDescripcionServicio());
            actvTipoEdit.setText(capitalizarPrimeraLetra(mantenimiento.getTipo()), false);
            actvPrioridadEdit.setText(capitalizarPrimeraLetra(mantenimiento.getPrioridad()), false);
            if (mantenimiento.getFechaProgramada() != null) {
                etFechaEdit.setText(DateUtils.formatearFecha(mantenimiento.getFechaProgramada()));
            }
        }

        // DatePicker para fecha
        etFechaEdit.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            if (mantenimiento.getFechaProgramada() != null) {
                calendar.setTime(mantenimiento.getFechaProgramada().toDate());
            }

            new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                etFechaEdit.setText(sdf.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        new AlertDialog.Builder(this)
                .setTitle("Editar Mantenimiento")
                .setView(dialogView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String descripcion = etDescripcionEdit.getText().toString().trim();
                    String tipo = actvTipoEdit.getText().toString().toLowerCase();
                    String prioridad = actvPrioridadEdit.getText().toString().toLowerCase();
                    String fechaStr = etFechaEdit.getText().toString().trim();

                    if (descripcion.isEmpty()) {
                        Toast.makeText(this, "La descripción es obligatoria", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Actualizar en Firestore
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("descripcionServicio", descripcion);
                    if (!tipo.isEmpty()) updates.put("tipo", tipo);
                    if (!prioridad.isEmpty()) updates.put("prioridad", prioridad);

                    // Convertir fecha a Timestamp si fue modificada
                    if (!fechaStr.isEmpty()) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            java.util.Date date = sdf.parse(fechaStr);
                            if (date != null) {
                                Timestamp timestamp = new Timestamp(date);
                                updates.put("fechaProgramada", timestamp);
                            }
                        } catch (Exception e) {
                            android.util.Log.e("DetalleMantenimiento", "Error al parsear fecha: " + e.getMessage());
                            Toast.makeText(this, "Error en el formato de fecha", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    db.collection("mantenimientos").document(mantenimientoId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Mantenimiento actualizado", Toast.LENGTH_SHORT).show();
                                cargarMantenimiento();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoReasignar() {
        // Cargar lista de técnicos disponibles
        db.collection("usuarios")
                .whereEqualTo("rol", "tecnico")
                .whereEqualTo("estado", "activo")
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    List<Usuario> tecnicos = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshots) {
                        Usuario tecnico = doc.toObject(Usuario.class);
                        tecnico.setUserId(doc.getId());
                        tecnicos.add(tecnico);
                    }

                    if (tecnicos.isEmpty()) {
                        Toast.makeText(this, "No hay técnicos disponibles", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Crear diálogo personalizado con opciones
                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_reasignar_tecnicos, null);

                    AutoCompleteTextView actvTecnicoPrincipal = dialogView.findViewById(R.id.actvTecnicoPrincipalReasignar);
                    LinearLayout containerTecnicosApoyo = dialogView.findViewById(R.id.containerTecnicosApoyoReasignar);
                    MaterialButton btnAgregarApoyo = dialogView.findViewById(R.id.btnAgregarTecnicoApoyoReasignar);
                    TextView tvTecnicosApoyoActuales = dialogView.findViewById(R.id.tvTecnicosApoyoActuales);

                    // Configurar dropdown de técnico principal
                    String[] nombresTecnicos = new String[tecnicos.size()];
                    for (int i = 0; i < tecnicos.size(); i++) {
                        nombresTecnicos[i] = tecnicos.get(i).getNombre();
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, nombresTecnicos);
                    actvTecnicoPrincipal.setAdapter(adapter);

                    // Pre-seleccionar técnico principal actual
                    if (tecnicoPrincipal != null) {
                        actvTecnicoPrincipal.setText(tecnicoPrincipal.getNombre(), false);
                    }

                    // Mostrar técnicos de apoyo actuales
                    if (mantenimiento.getTecnicosApoyo() != null && !mantenimiento.getTecnicosApoyo().isEmpty()) {
                        StringBuilder apoyoActual = new StringBuilder("Actuales: ");
                        for (int i = 0; i < tecnicosApoyo.size(); i++) {
                            apoyoActual.append(tecnicosApoyo.get(i).getNombre());
                            if (i < tecnicosApoyo.size() - 1) apoyoActual.append(", ");
                        }
                        tvTecnicosApoyoActuales.setText(apoyoActual.toString());
                        tvTecnicosApoyoActuales.setVisibility(View.VISIBLE);
                    } else {
                        tvTecnicosApoyoActuales.setVisibility(View.GONE);
                    }

                    // Lista temporal para nuevos técnicos de apoyo
                    List<String> nuevosTecnicosApoyoIds = new ArrayList<>();
                    if (mantenimiento.getTecnicosApoyo() != null) {
                        nuevosTecnicosApoyoIds.addAll(mantenimiento.getTecnicosApoyo());
                    }

                    // Botón para agregar técnico de apoyo
                    btnAgregarApoyo.setOnClickListener(v -> {
                        if (nuevosTecnicosApoyoIds.size() >= 3) {
                            Toast.makeText(this, "Máximo 3 técnicos de apoyo", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Crear AutoCompleteTextView para nuevo técnico de apoyo
                        View itemView = getLayoutInflater().inflate(R.layout.item_tecnico_apoyo_selector, containerTecnicosApoyo, false);
                        AutoCompleteTextView actvApoyo = itemView.findViewById(R.id.actvTecnicoApoyo);
                        MaterialButton btnRemover = itemView.findViewById(R.id.btnRemoverTecnicoApoyo);

                        actvApoyo.setAdapter(adapter);

                        // Remover técnico de apoyo
                        btnRemover.setOnClickListener(vRemove -> {
                            containerTecnicosApoyo.removeView(itemView);
                            // Encontrar y remover de la lista temporal
                            String nombreSeleccionado = actvApoyo.getText().toString();
                            for (Usuario tec : tecnicos) {
                                if (tec.getNombre().equals(nombreSeleccionado)) {
                                    nuevosTecnicosApoyoIds.remove(tec.getUserId());
                                    break;
                                }
                            }
                        });

                        // Cuando selecciona un técnico de apoyo
                        actvApoyo.setOnItemClickListener((parent, view, position, id) -> {
                            String nombreSeleccionado = (String) parent.getItemAtPosition(position);
                            for (Usuario tec : tecnicos) {
                                if (tec.getNombre().equals(nombreSeleccionado)) {
                                    if (!nuevosTecnicosApoyoIds.contains(tec.getUserId())) {
                                        nuevosTecnicosApoyoIds.add(tec.getUserId());
                                    }
                                    break;
                                }
                            }
                        });

                        containerTecnicosApoyo.addView(itemView);
                    });

                    // Mostrar diálogo
                    new AlertDialog.Builder(this)
                            .setTitle("Reasignar Técnicos")
                            .setView(dialogView)
                            .setPositiveButton("Guardar Cambios", (dialog, which) -> {
                                String nombrePrincipalSeleccionado = actvTecnicoPrincipal.getText().toString().trim();

                                if (nombrePrincipalSeleccionado.isEmpty()) {
                                    Toast.makeText(this, "Debe seleccionar un técnico principal", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Encontrar ID del técnico principal seleccionado
                                String tecnicoPrincipalId = null;
                                for (Usuario tec : tecnicos) {
                                    if (tec.getNombre().equals(nombrePrincipalSeleccionado)) {
                                        tecnicoPrincipalId = tec.getUserId();
                                        break;
                                    }
                                }

                                if (tecnicoPrincipalId == null) {
                                    Toast.makeText(this, "Técnico principal no válido", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Verificar que el técnico principal no esté en la lista de apoyo
                                String finalTecnicoPrincipalId = tecnicoPrincipalId;
                                nuevosTecnicosApoyoIds.remove(finalTecnicoPrincipalId);

                                // Actualizar en Firestore
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("tecnicoPrincipalId", tecnicoPrincipalId);
                                updates.put("tecnicosApoyo", nuevosTecnicosApoyoIds);

                                db.collection("mantenimientos").document(mantenimientoId)
                                        .update(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Técnicos reasignados correctamente", Toast.LENGTH_SHORT).show();
                                            cargarMantenimiento();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar técnicos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
