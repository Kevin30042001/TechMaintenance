package com.techsolution.techmaintenance.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.adapters.MantenimientoTimelineAdapter;
import com.techsolution.techmaintenance.models.Cliente;
import com.techsolution.techmaintenance.models.Equipo;
import com.techsolution.techmaintenance.models.Mantenimiento;
import com.techsolution.techmaintenance.models.Usuario;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DetalleEquipoActivity extends AppCompatActivity {

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;

    // Vistas
    private Toolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private ImageView ivFotoEquipo;
    private FloatingActionButton fabEditar;
    private MaterialButton btnEliminar;

    // Cliente
    private TextView tvNombreCliente, tvContactoCliente;

    // Datos Técnicos
    private TextView tvTipo, tvMarca, tvModelo, tvNumeroSerie;

    // Ubicación y Estado
    private TextView tvUbicacion, tvFechaAdquisicion;
    private Chip chipEstado;

    // Estadísticas
    private TextView tvTotalMantenimientos, tvUltimoMantenimiento;

    // Fechas
    private TextView tvFechaRegistro, tvUltimaActualizacion;

    // Historial de Mantenimientos
    private RecyclerView recyclerHistorial;
    private MantenimientoTimelineAdapter timelineAdapter;
    private List<Mantenimiento> listaHistorial;
    private TextView tvContadorHistorial, tvSinHistorial;
    private ProgressBar progressBarHistorial;

    // Datos
    private Equipo equipo;
    private boolean esAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_equipo);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        // Inicializar vistas
        inicializarVistas();

        // Configurar toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });

        // Obtener equipo del intent
        obtenerEquipo();

        // Verificar rol del usuario
        verificarRolUsuario();
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsingToolbar);
        ivFotoEquipo = findViewById(R.id.ivFotoEquipo);
        fabEditar = findViewById(R.id.fabEditar);
        btnEliminar = findViewById(R.id.btnEliminar);

        // Cliente
        tvNombreCliente = findViewById(R.id.tvNombreCliente);
        tvContactoCliente = findViewById(R.id.tvContactoCliente);

        // Datos Técnicos
        tvTipo = findViewById(R.id.tvTipo);
        tvMarca = findViewById(R.id.tvMarca);
        tvModelo = findViewById(R.id.tvModelo);
        tvNumeroSerie = findViewById(R.id.tvNumeroSerie);

        // Ubicación y Estado
        tvUbicacion = findViewById(R.id.tvUbicacion);
        tvFechaAdquisicion = findViewById(R.id.tvFechaAdquisicion);
        chipEstado = findViewById(R.id.chipEstado);

        // Estadísticas
        tvTotalMantenimientos = findViewById(R.id.tvTotalMantenimientos);
        tvUltimoMantenimiento = findViewById(R.id.tvUltimoMantenimiento);

        // Fechas
        tvFechaRegistro = findViewById(R.id.tvFechaRegistro);
        tvUltimaActualizacion = findViewById(R.id.tvUltimaActualizacion);

        // Historial
        recyclerHistorial = findViewById(R.id.recyclerHistorial);
        tvContadorHistorial = findViewById(R.id.tvContadorHistorial);
        tvSinHistorial = findViewById(R.id.tvSinHistorial);
        progressBarHistorial = findViewById(R.id.progressBarHistorial);

        // Configurar click en la foto para verla en pantalla completa
        ivFotoEquipo.setOnClickListener(v -> abrirVisorImagen());

        // Configurar RecyclerView del historial
        configurarRecyclerHistorial();
    }

    private void configurarRecyclerHistorial() {
        listaHistorial = new ArrayList<>();
        timelineAdapter = new MantenimientoTimelineAdapter(this, listaHistorial);
        recyclerHistorial.setLayoutManager(new LinearLayoutManager(this));
        recyclerHistorial.setAdapter(timelineAdapter);
        recyclerHistorial.setNestedScrollingEnabled(false);
    }

    private void obtenerEquipo() {
        android.util.Log.d("DetalleEquipo", "🔍 Iniciando obtenerEquipo()");

        try {
            String equipoId = getIntent().getStringExtra("equipoId");

            android.util.Log.d("DetalleEquipo", "📋 equipoId recibido: " + (equipoId != null ? equipoId : "NULL"));

            if (equipoId == null || equipoId.isEmpty()) {
                android.util.Log.e("DetalleEquipo", "❌ equipoId es NULL o vacío");
                Toast.makeText(this, "Error: No se recibió ID del equipo", Toast.LENGTH_LONG).show();
                setResult(RESULT_CANCELED);
                finish();
                return;
            }

            android.util.Log.d("DetalleEquipo", "🔄 Consultando Firestore para equipoId: " + equipoId);

            // Cargar equipo desde Firestore
            db.collection("equipos").document(equipoId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        try {
                            android.util.Log.d("DetalleEquipo", "📥 Respuesta de Firestore recibida");

                            if (documentSnapshot.exists()) {
                                android.util.Log.d("DetalleEquipo", "✅ Documento existe");

                                equipo = documentSnapshot.toObject(Equipo.class);

                                if (equipo != null) {
                                    equipo.setEquipoId(documentSnapshot.getId());
                                    android.util.Log.d("DetalleEquipo", "✅ Equipo convertido: " + equipo.getMarca() + " " + equipo.getModelo());
                                    android.util.Log.d("DetalleEquipo", "📝 Cliente ID: " + equipo.getClienteId());
                                    android.util.Log.d("DetalleEquipo", "📸 Foto URL: " + equipo.getFotografiaURL());

                                    mostrarDatosEquipo();
                                    cargarDatosCliente();
                                    cargarHistorialMantenimientos();
                                } else {
                                    android.util.Log.e("DetalleEquipo", "❌ equipo es NULL después de toObject()");
                                    Toast.makeText(this, "Error al convertir datos del equipo", Toast.LENGTH_LONG).show();
                                    setResult(RESULT_CANCELED);
                                    finish();
                                }
                            } else {
                                android.util.Log.e("DetalleEquipo", "❌ Documento NO existe en Firestore");
                                Toast.makeText(this, "Equipo no encontrado en la base de datos", Toast.LENGTH_LONG).show();
                                setResult(RESULT_CANCELED);
                                finish();
                            }
                        } catch (Exception e) {
                            android.util.Log.e("DetalleEquipo", "❌ EXCEPCIÓN en onSuccess: " + e.getMessage());
                            e.printStackTrace();
                            Toast.makeText(this, "Error al procesar equipo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            setResult(RESULT_CANCELED);
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("DetalleEquipo", "❌ ERROR en Firestore.get(): " + e.getMessage());
                        e.printStackTrace();
                        Toast.makeText(this, "Error de conexión: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        setResult(RESULT_CANCELED);
                        finish();
                    });
        } catch (Exception e) {
            android.util.Log.e("DetalleEquipo", "❌ EXCEPCIÓN CRÍTICA en obtenerEquipo(): " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error crítico: " + e.getMessage(), Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void verificarRolUsuario() {
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            db.collection("usuarios").document(uid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Usuario usuario = doc.toObject(Usuario.class);
                            esAdmin = usuario != null && "admin".equals(usuario.getRol());

                            // Mostrar/ocultar controles de admin
                            fabEditar.setVisibility(esAdmin ? View.VISIBLE : View.GONE);
                            if (btnEliminar != null) {
                                btnEliminar.setVisibility(esAdmin ? View.VISIBLE : View.GONE);
                            }

                            // Configurar listeners
                            if (esAdmin) {
                                fabEditar.setOnClickListener(v -> abrirEditarEquipo());
                                if (btnEliminar != null) {
                                    btnEliminar.setOnClickListener(v -> mostrarDialogoEliminar());
                                }
                            }
                        }
                    });
        }
    }

    private void mostrarDatosEquipo() {
        android.util.Log.d("DetalleEquipo", "🎨 Iniciando mostrarDatosEquipo()");

        try {
            // Título
            String titulo = equipo.getMarca() + " " + equipo.getModelo();
            collapsingToolbar.setTitle(titulo);
            android.util.Log.d("DetalleEquipo", "📝 Título: " + titulo);

            // Foto
            if (equipo.getFotografiaURL() != null && !equipo.getFotografiaURL().isEmpty()) {
                android.util.Log.d("DetalleEquipo", "📷 Cargando foto: " + equipo.getFotografiaURL());
                Glide.with(this)
                        .load(equipo.getFotografiaURL())
                        .placeholder(R.drawable.ic_computer)
                        .error(R.drawable.ic_computer)
                        .centerCrop()
                        .into(ivFotoEquipo);
            } else {
                android.util.Log.d("DetalleEquipo", "⚠️ Sin foto URL");
            }

            // Datos Técnicos
            android.util.Log.d("DetalleEquipo", "📋 Configurando datos técnicos");
            tvTipo.setText(equipo.getTipo() != null ? equipo.getTipo() : "No especificado");
            tvMarca.setText(equipo.getMarca() != null ? equipo.getMarca() : "No especificado");
            tvModelo.setText(equipo.getModelo() != null ? equipo.getModelo() : "No especificado");
            tvNumeroSerie.setText(equipo.getNumeroSerie() != null ? equipo.getNumeroSerie() : "No especificado");

        // Ubicación y Estado
        tvUbicacion.setText(equipo.getUbicacionEspecifica() != null ? equipo.getUbicacionEspecifica() : "No especificado");

        // Estado con chip
        chipEstado.setText(obtenerEstadoFormateado(equipo.getEstado()));
        chipEstado.setChipBackgroundColorResource(obtenerColorEstado(equipo.getEstado()));

        // Fecha de Adquisición
        if (equipo.getFechaAdquisicion() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvFechaAdquisicion.setText(sdf.format(equipo.getFechaAdquisicion().toDate()));
        } else {
            tvFechaAdquisicion.setText("No especificada");
        }

        // Estadísticas - Se cargan dinámicamente desde Firestore en cargarHistorial()
        tvTotalMantenimientos.setText("Cargando...");
        tvUltimoMantenimiento.setText("Cargando...");

        // Fechas de Registro
        if (equipo.getFechaRegistro() != null) {
            SimpleDateFormat sdfCompleto = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvFechaRegistro.setText(sdfCompleto.format(equipo.getFechaRegistro().toDate()));
        } else {
            tvFechaRegistro.setText("No disponible");
        }

            tvUltimaActualizacion.setText("N/A");

            android.util.Log.d("DetalleEquipo", "✅ mostrarDatosEquipo() completado");
        } catch (Exception e) {
            android.util.Log.e("DetalleEquipo", "❌ ERROR en mostrarDatosEquipo: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error al mostrar datos: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void cargarDatosCliente() {
        if (equipo.getClienteId() != null && !equipo.getClienteId().isEmpty()) {
            db.collection("clientes").document(equipo.getClienteId())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Cliente cliente = doc.toObject(Cliente.class);
                            if (cliente != null) {
                                tvNombreCliente.setText(cliente.getNombreEmpresa());

                                String contacto = cliente.getNombreContacto();
                                if (cliente.getTelefonoContacto() != null && !cliente.getTelefonoContacto().isEmpty()) {
                                    contacto += " • " + cliente.getTelefonoContacto();
                                }
                                tvContactoCliente.setText(contacto);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        tvNombreCliente.setText("Cliente no encontrado");
                        tvContactoCliente.setText("");
                    });
        } else {
            tvNombreCliente.setText("Sin cliente asignado");
            tvContactoCliente.setText("");
        }
    }

    private void setTextoCampo(View item, String label, String valor) {
        try {
            if (item == null) {
                android.util.Log.e("DetalleEquipo", "❌ setTextoCampo: item es NULL para label: " + label);
                return;
            }

            TextView tvLabel = item.findViewById(R.id.tvLabel);
            TextView tvValor = item.findViewById(R.id.tvValor);

            if (tvLabel == null || tvValor == null) {
                android.util.Log.e("DetalleEquipo", "❌ setTextoCampo: TextViews son NULL para label: " + label);
                return;
            }

            tvLabel.setText(label);
            tvValor.setText(valor != null && !valor.isEmpty() ? valor : "No especificado");
        } catch (Exception e) {
            android.util.Log.e("DetalleEquipo", "❌ ERROR en setTextoCampo(" + label + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String obtenerEstadoFormateado(String estado) {
        if (estado == null) return "Desconocido";
        switch (estado.toLowerCase()) {
            case "operativo":
                return "Operativo";
            case "mantenimiento":
                return "En Mantenimiento";
            case "fuera_servicio":
                return "Fuera de Servicio";
            default:
                return estado;
        }
    }

    private int obtenerColorEstado(String estado) {
        if (estado == null) return R.color.secondary_text;
        switch (estado.toLowerCase()) {
            case "operativo":
                return R.color.success;
            case "mantenimiento":
                return R.color.warning;
            case "fuera_servicio":
                return R.color.error;
            default:
                return R.color.secondary_text;
        }
    }

    private void abrirEditarEquipo() {
        Intent intent = new Intent(this, AgregarEditarEquipoActivity.class);
        intent.putExtra("equipoId", equipo.getEquipoId());
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Recargar datos del equipo
            if (equipo != null && equipo.getEquipoId() != null) {
                db.collection("equipos").document(equipo.getEquipoId())
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                equipo = doc.toObject(Equipo.class);
                                if (equipo != null) {
                                    equipo.setEquipoId(doc.getId());
                                    mostrarDatosEquipo();
                                    cargarDatosCliente();
                                    cargarHistorialMantenimientos();
                                }
                            }
                        });
            }
        }
    }

    private void mostrarDialogoEliminar() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Equipo")
                .setMessage("¿Estás seguro de que deseas eliminar este equipo?\n\n" +
                        "Esta acción no se puede deshacer.\n\n" +
                        "Equipo: " + equipo.getMarca() + " " + equipo.getModelo() + "\n" +
                        "S/N: " + equipo.getNumeroSerie())
                .setPositiveButton("Sí, eliminar", (dialog, which) -> eliminarEquipo())
                .setNegativeButton("Cancelar", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void eliminarEquipo() {
        // Verificar si tiene mantenimientos
        db.collection("mantenimientos")
                .whereEqualTo("equipoId", equipo.getEquipoId())
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (!querySnapshots.isEmpty()) {
                        // Tiene mantenimientos, no se puede eliminar
                        new AlertDialog.Builder(this)
                                .setTitle("No se puede eliminar")
                                .setMessage("Este equipo tiene " + querySnapshots.size() + " mantenimiento(s) registrado(s).\n\n" +
                                        "No es posible eliminarlo hasta que no tenga mantenimientos asociados.")
                                .setPositiveButton("Entendido", null)
                                .show();
                    } else {
                        // No tiene mantenimientos, proceder a eliminar
                        confirmarYEliminar();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al verificar mantenimientos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmarYEliminar() {
        android.util.Log.d("DetalleEquipo", "🗑️ Eliminando equipo: " + equipo.getEquipoId());

        // Primero eliminar la foto de Storage
        if (equipo.getFotografiaURL() != null && !equipo.getFotografiaURL().isEmpty()) {
            try {
                StorageReference fotoRef = storage.getReferenceFromUrl(equipo.getFotografiaURL());
                fotoRef.delete()
                        .addOnSuccessListener(aVoid -> {
                            android.util.Log.d("DetalleEquipo", "✅ Foto eliminada de Storage");
                            eliminarDocumentoFirestore();
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.w("DetalleEquipo", "⚠️ No se pudo eliminar foto: " + e.getMessage());
                            // Continuar eliminando el documento aunque falle la foto
                            eliminarDocumentoFirestore();
                        });
            } catch (Exception e) {
                android.util.Log.w("DetalleEquipo", "⚠️ Error al parsear URL de foto: " + e.getMessage());
                eliminarDocumentoFirestore();
            }
        } else {
            // No tiene foto, eliminar directamente
            eliminarDocumentoFirestore();
        }
    }

    private void eliminarDocumentoFirestore() {
        db.collection("equipos").document(equipo.getEquipoId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("DetalleEquipo", "✅ Equipo eliminado de Firestore");

                    // Actualizar contador del cliente
                    if (equipo.getClienteId() != null && !equipo.getClienteId().isEmpty()) {
                        db.collection("clientes").document(equipo.getClienteId())
                                .get()
                                .addOnSuccessListener(doc -> {
                                    if (doc.exists()) {
                                        Long totalEquipos = doc.getLong("totalEquipos");
                                        int nuevoTotal = (totalEquipos != null ? totalEquipos.intValue() : 0) - 1;
                                        if (nuevoTotal < 0) nuevoTotal = 0;

                                        db.collection("clientes").document(equipo.getClienteId())
                                                .update("totalEquipos", nuevoTotal);
                                    }
                                });
                    }

                    Toast.makeText(this, "✅ Equipo eliminado correctamente", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DetalleEquipo", "❌ Error al eliminar equipo: " + e.getMessage());
                    Toast.makeText(this, "❌ Error al eliminar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void abrirVisorImagen() {
        if (equipo != null && equipo.getFotografiaURL() != null && !equipo.getFotografiaURL().isEmpty()) {
            // Crear un diálogo personalizado para mostrar la imagen en pantalla completa
            AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

            // Crear FrameLayout como contenedor principal
            android.widget.FrameLayout frameLayout = new android.widget.FrameLayout(this);
            frameLayout.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
            frameLayout.setBackgroundColor(0xFF000000); // Fondo negro

            // Crear ImageView con tamaño fijo grande
            ImageView imageView = new ImageView(this);

            // Calcular el tamaño de la pantalla
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int screenHeight = displayMetrics.heightPixels;
            int screenWidth = displayMetrics.widthPixels;

            // Usar 80% del ancho y 70% del alto
            int imageWidth = (int) (screenWidth * 0.8);
            int imageHeight = (int) (screenHeight * 0.7);

            android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                    imageWidth,
                    imageHeight);
            params.gravity = android.view.Gravity.CENTER;
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setBackgroundColor(0x00000000); // Sin fondo
            imageView.setPadding(0, 0, 0, 0);

            // Agregar ImageView al FrameLayout
            frameLayout.addView(imageView);

            // Cargar imagen con Glide SIN placeholder que cause el marco
            Glide.with(this)
                    .load(equipo.getFotografiaURL())
                    .into(imageView);

            // Configurar el diálogo
            builder.setView(frameLayout);
            AlertDialog dialog = builder.create();

            // Configurar el diálogo para pantalla completa sin bordes
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT);
                dialog.getWindow().setFlags(
                        android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.black);
            }

            // Cerrar al hacer clic en cualquier parte
            frameLayout.setOnClickListener(v -> dialog.dismiss());
            imageView.setOnClickListener(v -> dialog.dismiss());

            // Mostrar el diálogo
            dialog.show();
        } else {
            Toast.makeText(this, "No hay imagen disponible", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Carga el historial completo de mantenimientos de este equipo
     * Ordenado por fecha (más reciente primero)
     */
    private void cargarHistorialMantenimientos() {
        android.util.Log.d("DetalleEquipo", "📋 Iniciando carga de historial de mantenimientos");
        android.util.Log.d("DetalleEquipo", "📋 Equipo ID: " + equipo.getEquipoId());

        // Mostrar progress bar
        progressBarHistorial.setVisibility(View.VISIBLE);
        recyclerHistorial.setVisibility(View.GONE);
        tvSinHistorial.setVisibility(View.GONE);

        // Query para obtener TODOS los mantenimientos de este equipo
        db.collection("mantenimientos")
                .whereEqualTo("equipoId", equipo.getEquipoId())
                .orderBy("fechaProgramada", Query.Direction.DESCENDING) // Más recientes primero
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    android.util.Log.d("DetalleEquipo", "✅ Query completada");
                    android.util.Log.d("DetalleEquipo", "📊 Mantenimientos encontrados: " + queryDocumentSnapshots.size());

                    listaHistorial.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Mantenimiento mantenimiento = doc.toObject(Mantenimiento.class);
                            mantenimiento.setMantenimientoId(doc.getId());
                            listaHistorial.add(mantenimiento);

                            android.util.Log.d("DetalleEquipo", "  ✅ Mantenimiento agregado: " + doc.getId() + " - Estado: " + mantenimiento.getEstado());
                        } catch (Exception e) {
                            android.util.Log.e("DetalleEquipo", "  ❌ Error al parsear mantenimiento: " + e.getMessage());
                        }
                    }

                    // Ocultar progress bar
                    progressBarHistorial.setVisibility(View.GONE);

                    // Actualizar contador del historial
                    tvContadorHistorial.setText(String.valueOf(listaHistorial.size()));

                    // Actualizar estadísticas con datos reales de Firestore
                    tvTotalMantenimientos.setText(String.valueOf(listaHistorial.size()));

                    // Mostrar RecyclerView o mensaje según corresponda
                    if (listaHistorial.isEmpty()) {
                        android.util.Log.d("DetalleEquipo", "📭 Sin historial - mostrando mensaje");
                        recyclerHistorial.setVisibility(View.GONE);
                        tvSinHistorial.setVisibility(View.VISIBLE);

                        // Actualizar también el campo de último mantenimiento
                        tvUltimoMantenimiento.setText("Nunca");
                    } else {
                        android.util.Log.d("DetalleEquipo", "👁️ Mostrando historial con " + listaHistorial.size() + " items");
                        tvSinHistorial.setVisibility(View.GONE);
                        recyclerHistorial.setVisibility(View.VISIBLE);
                        timelineAdapter.actualizarLista(listaHistorial);

                        // Actualizar campo de último mantenimiento (el primero de la lista porque está ordenado DESC)
                        Mantenimiento ultimo = listaHistorial.get(0);
                        if (ultimo.getFechaProgramada() != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            tvUltimoMantenimiento.setText(sdf.format(ultimo.getFechaProgramada().toDate()));
                        } else {
                            tvUltimoMantenimiento.setText("Sin fecha");
                        }
                    }

                    android.util.Log.d("DetalleEquipo", "✅ Carga de historial completada");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DetalleEquipo", "❌ Error al cargar historial: " + e.getMessage());
                    e.printStackTrace();

                    progressBarHistorial.setVisibility(View.GONE);
                    recyclerHistorial.setVisibility(View.GONE);
                    tvSinHistorial.setVisibility(View.VISIBLE);
                    tvSinHistorial.setText("Error al cargar historial: " + e.getMessage());
                    tvContadorHistorial.setText("0");
                });
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }
}
