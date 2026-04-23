package com.techsolution.techmaintenance.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.models.Cliente;
import com.techsolution.techmaintenance.models.Equipo;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

public class AgregarEditarEquipoActivity extends AppCompatActivity {

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    // Vistas
    private Toolbar toolbar;
    private AutoCompleteTextView actvCliente, actvTipoEquipo, actvEstado;
    private TextInputLayout tilCliente, tilTipoEquipo, tilMarca, tilModelo;
    private TextInputLayout tilNumeroSerie, tilUbicacion, tilFechaAdquisicion, tilEstado;
    private TextInputEditText etMarca, etModelo, etNumeroSerie, etUbicacion, etFechaAdquisicion;
    private ImageView ivFotoEquipo, ivCambiarFoto;
    private MaterialButton btnTomarFoto, btnSeleccionarGaleria, btnCancelar, btnGuardar;
    private ProgressBar progressBar;

    // Variables
    private boolean modoEditar = false;
    private Equipo equipoActual;
    private List<Cliente> listaClientes;
    private Map<String, String> mapaClientesNombreId; // Nombre -> ID
    private String clienteIdSeleccionado;
    private Uri fotoUri;
    private String fotoURL;
    private boolean fotoObligatoria = true;
    private Uri tempFotoUri; // Para foto de cámara
    private Date fechaAdquisicionSeleccionada;

    // Constantes
    private static final String[] TIPOS_EQUIPO = {"laptop", "desktop", "servidor", "impresora", "scanner", "router", "switch", "otro"};

    // Estados - Textos amigables para mostrar al usuario
    private static final String[] ESTADOS_MOSTRAR = {
            "✅ Funcionando Correctamente",
            "🔧 En Mantenimiento",
            "⚠️ Requiere Reparación",
            "❌ Fuera de Servicio",
            "🆕 Nuevo / Sin Estrenar",
            "📦 En Bodega / Almacenado"
    };

    // Estados - Valores técnicos para guardar en base de datos
    private static final String[] ESTADOS_VALORES = {
            "operativo",
            "mantenimiento",
            "requiere_reparacion",
            "fuera_servicio",
            "nuevo",
            "almacenado"
    };

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> camaraLauncher;
    private ActivityResultLauncher<Intent> galeriaLauncher;
    private ActivityResultLauncher<String> permisoCamaraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_editar_equipo);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // Inicializar vistas
        inicializarVistas();

        // Inicializar launchers
        inicializarActivityResultLaunchers();

        // Verificar modo (crear o editar)
        if (getIntent().hasExtra("equipo")) {
            android.util.Log.d("AgregarEquipo", "🔧 Modo EDITAR - Equipo recibido como objeto");
            modoEditar = true;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                equipoActual = getIntent().getSerializableExtra("equipo", Equipo.class);
            } else {
                equipoActual = (Equipo) getIntent().getSerializableExtra("equipo");
            }
            toolbar.setTitle("Editar Equipo");
            fotoObligatoria = false; // En modo editar, la foto ya existe
        } else if (getIntent().hasExtra("equipoId")) {
            // Modo editar alternativo: cargar equipo desde Firestore usando solo el ID
            android.util.Log.d("AgregarEquipo", "🔧 Modo EDITAR - Solo equipoId recibido, cargando desde Firestore");
            modoEditar = true;
            toolbar.setTitle("Editar Equipo");
            fotoObligatoria = false;

            String equipoId = getIntent().getStringExtra("equipoId");
            cargarEquipoDesdeFirestore(equipoId);
        } else {
            android.util.Log.d("AgregarEquipo", "➕ Modo CREAR - Nuevo equipo");
            toolbar.setTitle("Agregar Equipo");
        }

        // Configurar toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Configurar dropdowns
        configurarDropdowns();

        // Cargar clientes
        cargarClientes();

        // Configurar listeners
        configurarListeners();

        // Verificar rol del usuario para diagnóstico
        verificarRolUsuario();
    }

    private void verificarRolUsuario() {
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            db.collection("usuarios").document(uid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String rol = doc.getString("rol");
                            android.util.Log.d("AgregarEquipo", "👤 Usuario UID: " + uid);
                            android.util.Log.d("AgregarEquipo", "🔑 Rol del usuario: " + rol);
                            android.util.Log.d("AgregarEquipo", "✅ Usuario autenticado correctamente");
                        } else {
                            android.util.Log.e("AgregarEquipo", "❌ Documento de usuario no existe en Firestore");
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("AgregarEquipo", "❌ Error al verificar rol: " + e.getMessage());
                    });
        } else {
            android.util.Log.e("AgregarEquipo", "❌ No hay usuario autenticado");
        }
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);

        tilCliente = findViewById(R.id.tilCliente);
        tilTipoEquipo = findViewById(R.id.tilTipoEquipo);
        tilMarca = findViewById(R.id.tilMarca);
        tilModelo = findViewById(R.id.tilModelo);
        tilNumeroSerie = findViewById(R.id.tilNumeroSerie);
        tilUbicacion = findViewById(R.id.tilUbicacion);
        tilFechaAdquisicion = findViewById(R.id.tilFechaAdquisicion);
        tilEstado = findViewById(R.id.tilEstado);

        actvCliente = findViewById(R.id.actvCliente);
        actvTipoEquipo = findViewById(R.id.actvTipoEquipo);
        actvEstado = findViewById(R.id.actvEstado);

        etMarca = findViewById(R.id.etMarca);
        etModelo = findViewById(R.id.etModelo);
        etNumeroSerie = findViewById(R.id.etNumeroSerie);
        etUbicacion = findViewById(R.id.etUbicacion);
        etFechaAdquisicion = findViewById(R.id.etFechaAdquisicion);

        ivFotoEquipo = findViewById(R.id.ivFotoEquipo);
        ivCambiarFoto = findViewById(R.id.ivCambiarFoto);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnSeleccionarGaleria = findViewById(R.id.btnSeleccionarGaleria);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnGuardar = findViewById(R.id.btnGuardar);
        progressBar = findViewById(R.id.progressBar);
    }

    private void inicializarActivityResultLaunchers() {
        // Launcher para cámara
        camaraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        fotoUri = tempFotoUri;
                        mostrarVistaPrevia();
                    }
                });

        // Launcher para galería
        galeriaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        fotoUri = result.getData().getData();
                        mostrarVistaPrevia();
                    }
                });

        // Launcher para permiso de cámara
        permisoCamaraLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        abrirCamara();
                    } else {
                        Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void configurarDropdowns() {
        // Tipos de equipo - Permitir autocompletado Y texto personalizado
        ArrayAdapter<String> tiposAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, TIPOS_EQUIPO);
        actvTipoEquipo.setAdapter(tiposAdapter);

        // Permitir que el usuario escriba tipos personalizados
        actvTipoEquipo.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String tipoIngresado = actvTipoEquipo.getText().toString().trim();
                if (!tipoIngresado.isEmpty()) {
                    // Verificar si es un tipo personalizado (no está en la lista predefinida)
                    boolean esPersonalizado = true;
                    for (String tipo : TIPOS_EQUIPO) {
                        if (tipo.equalsIgnoreCase(tipoIngresado)) {
                            esPersonalizado = false;
                            break;
                        }
                    }

                    if (esPersonalizado) {
                        android.util.Log.d("AgregarEquipo", "📝 Tipo personalizado: " + tipoIngresado);
                        Toast.makeText(this, "Tipo personalizado: " + tipoIngresado, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // Estados - Mostrar textos amigables
        ArrayAdapter<String> estadosAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, ESTADOS_MOSTRAR);
        actvEstado.setAdapter(estadosAdapter);
        actvEstado.setText(ESTADOS_MOSTRAR[0], false); // Por defecto: "✅ Funcionando Correctamente"
    }

    /**
     * Convierte el texto amigable del estado a su valor técnico para base de datos
     */
    private String convertirEstadoAValor(String textoAmigable) {
        for (int i = 0; i < ESTADOS_MOSTRAR.length; i++) {
            if (ESTADOS_MOSTRAR[i].equals(textoAmigable)) {
                return ESTADOS_VALORES[i];
            }
        }
        // Por defecto retornar "operativo"
        return "operativo";
    }

    /**
     * Convierte el valor técnico del estado a su texto amigable para mostrar
     */
    private String convertirValorAEstado(String valorDB) {
        for (int i = 0; i < ESTADOS_VALORES.length; i++) {
            if (ESTADOS_VALORES[i].equals(valorDB)) {
                return ESTADOS_MOSTRAR[i];
            }
        }
        // Por defecto retornar el primer estado
        return ESTADOS_MOSTRAR[0];
    }

    /**
     * Carga un equipo desde Firestore usando solo su ID
     * Se usa cuando se pasa equipoId en lugar del objeto completo
     */
    private void cargarEquipoDesdeFirestore(String equipoId) {
        android.util.Log.d("AgregarEquipo", "📥 Cargando equipo con ID: " + equipoId);

        if (equipoId == null || equipoId.isEmpty()) {
            android.util.Log.e("AgregarEquipo", "❌ equipoId es NULL o vacío");
            Toast.makeText(this, "Error: ID de equipo no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mostrarCargando(true);

        db.collection("equipos").document(equipoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        equipoActual = documentSnapshot.toObject(Equipo.class);
                        if (equipoActual != null) {
                            equipoActual.setEquipoId(documentSnapshot.getId());
                            android.util.Log.d("AgregarEquipo", "✅ Equipo cargado: " + equipoActual.getMarca() + " " + equipoActual.getModelo());

                            // Después de cargar el equipo, cargar clientes y luego poblar campos
                            cargarClientes();
                        } else {
                            android.util.Log.e("AgregarEquipo", "❌ Error al convertir documento a Equipo");
                            Toast.makeText(this, "Error al cargar datos del equipo", Toast.LENGTH_SHORT).show();
                            mostrarCargando(false);
                            finish();
                        }
                    } else {
                        android.util.Log.e("AgregarEquipo", "❌ Equipo no encontrado");
                        Toast.makeText(this, "Equipo no encontrado", Toast.LENGTH_SHORT).show();
                        mostrarCargando(false);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AgregarEquipo", "❌ Error al cargar equipo: " + e.getMessage());
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    mostrarCargando(false);
                    finish();
                });
    }

    private void cargarClientes() {
        mostrarCargando(true);
        listaClientes = new ArrayList<>();
        mapaClientesNombreId = new HashMap<>();

        db.collection("clientes")
                .orderBy("nombreEmpresa")
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    List<String> nombresClientes = new ArrayList<>();

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshots) {
                        Cliente cliente = doc.toObject(Cliente.class);
                        cliente.setClienteId(doc.getId());
                        listaClientes.add(cliente);

                        String nombreCompleto = cliente.getNombreEmpresa();
                        nombresClientes.add(nombreCompleto);
                        mapaClientesNombreId.put(nombreCompleto, doc.getId());
                    }

                    // Configurar adapter de clientes
                    ArrayAdapter<String> clientesAdapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_dropdown_item_1line, nombresClientes);
                    actvCliente.setAdapter(clientesAdapter);

                    // Si es modo editar, cargar datos del equipo
                    if (modoEditar && equipoActual != null) {
                        cargarDatosEquipo();
                    }

                    mostrarCargando(false);
                })
                .addOnFailureListener(e -> {
                    mostrarCargando(false);
                    Toast.makeText(this, "Error al cargar clientes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void cargarDatosEquipo() {
        if (equipoActual == null) return;

        // Seleccionar cliente
        for (Map.Entry<String, String> entry : mapaClientesNombreId.entrySet()) {
            if (entry.getValue().equals(equipoActual.getClienteId())) {
                actvCliente.setText(entry.getKey(), false);
                clienteIdSeleccionado = entry.getValue();
                break;
            }
        }

        // Tipo de equipo
        actvTipoEquipo.setText(equipoActual.getTipo(), false);

        // Datos básicos
        etMarca.setText(equipoActual.getMarca());
        etModelo.setText(equipoActual.getModelo());
        etNumeroSerie.setText(equipoActual.getNumeroSerie());
        etUbicacion.setText(equipoActual.getUbicacionEspecifica());

        // Estado - Convertir valor DB a texto amigable
        String estadoTextoAmigable = convertirValorAEstado(equipoActual.getEstado());
        actvEstado.setText(estadoTextoAmigable, false);

        // Fecha de adquisición
        if (equipoActual.getFechaAdquisicion() != null) {
            fechaAdquisicionSeleccionada = equipoActual.getFechaAdquisicion().toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            etFechaAdquisicion.setText(sdf.format(fechaAdquisicionSeleccionada));
        }

        // Foto
        if (equipoActual.getFotografiaURL() != null && !equipoActual.getFotografiaURL().isEmpty()) {
            fotoURL = equipoActual.getFotografiaURL();
            Glide.with(this)
                    .load(fotoURL)
                    .placeholder(R.drawable.ic_computer)
                    .into(ivFotoEquipo);
            ivCambiarFoto.setVisibility(View.VISIBLE);
        }
    }

    private void configurarListeners() {
        // Toolbar
        toolbar.setNavigationOnClickListener(v -> finish());

        // Cliente seleccionado
        actvCliente.setOnItemClickListener((parent, view, position, id) -> {
            String nombreSeleccionado = (String) parent.getItemAtPosition(position);
            clienteIdSeleccionado = mapaClientesNombreId.get(nombreSeleccionado);
        });

        // Fecha de adquisición
        etFechaAdquisicion.setOnClickListener(v -> mostrarDatePicker());

        // Tomar foto
        btnTomarFoto.setOnClickListener(v -> verificarPermisoYAbrirCamara());

        // Seleccionar de galería
        btnSeleccionarGaleria.setOnClickListener(v -> abrirGaleria());

        // Cambiar foto (solo visible cuando ya hay foto)
        ivCambiarFoto.setOnClickListener(v -> mostrarOpcionesFoto());

        // Botones
        btnCancelar.setOnClickListener(v -> finish());
        btnGuardar.setOnClickListener(v -> validarYGuardar());
    }

    private void verificarPermisoYAbrirCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            permisoCamaraLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirCamara() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            // Crear archivo temporal
            File fotoFile = new File(getExternalFilesDir(null), "temp_equipo_" + System.currentTimeMillis() + ".jpg");
            tempFotoUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", fotoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, tempFotoUri);
            camaraLauncher.launch(intent);
        }
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galeriaLauncher.launch(intent);
    }

    private void mostrarOpcionesFoto() {
        new AlertDialog.Builder(this)
                .setTitle("Cambiar Foto")
                .setItems(new String[]{"Tomar Foto", "Seleccionar de Galería", "Quitar Foto"}, (dialog, which) -> {
                    if (which == 0) {
                        verificarPermisoYAbrirCamara();
                    } else if (which == 1) {
                        abrirGaleria();
                    } else if (which == 2) {
                        quitarFoto();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void quitarFoto() {
        new AlertDialog.Builder(this)
                .setTitle("Quitar Fotografía")
                .setMessage("¿Estás seguro de que deseas quitar la fotografía? Deberás tomar una nueva antes de guardar.")
                .setPositiveButton("Sí, quitar", (dialog, which) -> {
                    fotoUri = null;
                    fotoURL = null;
                    ivFotoEquipo.setImageResource(R.drawable.ic_computer);
                    ivCambiarFoto.setVisibility(View.GONE);
                    btnTomarFoto.setVisibility(View.VISIBLE);
                    btnSeleccionarGaleria.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Fotografía eliminada. Recuerda capturar una nueva.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDatePicker() {
        // Configurar la fecha inicial en UTC
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        if (fechaAdquisicionSeleccionada != null) {
            calendar.setTime(fechaAdquisicionSeleccionada);
        }

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Seleccionar Fecha de Adquisición")
                .setSelection(calendar.getTimeInMillis())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // Convertir de UTC a fecha local
            Calendar selectedCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            selectedCalendar.setTimeInMillis(selection);

            // Crear la fecha en la zona horaria local
            Calendar localCalendar = Calendar.getInstance();
            localCalendar.set(Calendar.YEAR, selectedCalendar.get(Calendar.YEAR));
            localCalendar.set(Calendar.MONTH, selectedCalendar.get(Calendar.MONTH));
            localCalendar.set(Calendar.DAY_OF_MONTH, selectedCalendar.get(Calendar.DAY_OF_MONTH));
            localCalendar.set(Calendar.HOUR_OF_DAY, 0);
            localCalendar.set(Calendar.MINUTE, 0);
            localCalendar.set(Calendar.SECOND, 0);
            localCalendar.set(Calendar.MILLISECOND, 0);

            fechaAdquisicionSeleccionada = localCalendar.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            etFechaAdquisicion.setText(sdf.format(fechaAdquisicionSeleccionada));
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void mostrarVistaPrevia() {
        if (fotoUri != null) {
            Glide.with(this)
                    .load(fotoUri)
                    .placeholder(R.drawable.ic_computer)
                    .into(ivFotoEquipo);
            ivCambiarFoto.setVisibility(View.VISIBLE);
            btnTomarFoto.setVisibility(View.GONE);
            btnSeleccionarGaleria.setVisibility(View.GONE);
        }
    }

    private void validarYGuardar() {
        // Limpiar errores
        tilCliente.setError(null);
        tilTipoEquipo.setError(null);
        tilMarca.setError(null);
        tilModelo.setError(null);
        tilNumeroSerie.setError(null);
        tilUbicacion.setError(null);

        // Obtener valores
        String cliente = actvCliente.getText().toString().trim();
        String tipo = actvTipoEquipo.getText().toString().trim();
        String marca = etMarca.getText().toString().trim();
        String modelo = etModelo.getText().toString().trim();
        String numeroSerie = etNumeroSerie.getText().toString().trim().toUpperCase();
        String ubicacion = etUbicacion.getText().toString().trim();
        String estadoTexto = actvEstado.getText().toString().trim();
        String estado = convertirEstadoAValor(estadoTexto); // Convertir a valor DB

        // Validaciones
        boolean valido = true;

        if (TextUtils.isEmpty(cliente)) {
            tilCliente.setError("Seleccione un cliente");
            valido = false;
        }

        if (TextUtils.isEmpty(tipo)) {
            tilTipoEquipo.setError("Seleccione el tipo de equipo");
            valido = false;
        }

        if (TextUtils.isEmpty(marca)) {
            tilMarca.setError("La marca es obligatoria");
            valido = false;
        }

        if (TextUtils.isEmpty(modelo)) {
            tilModelo.setError("El modelo es obligatorio");
            valido = false;
        }

        if (TextUtils.isEmpty(numeroSerie)) {
            tilNumeroSerie.setError("El número de serie es obligatorio");
            valido = false;
        }

        if (TextUtils.isEmpty(ubicacion)) {
            tilUbicacion.setError("La ubicación específica es obligatoria");
            valido = false;
        }

        // Validar foto (obligatoria solo en modo crear)
        if (fotoObligatoria && fotoUri == null && (fotoURL == null || fotoURL.isEmpty())) {
            Toast.makeText(this, "La fotografía del equipo es obligatoria", Toast.LENGTH_LONG).show();
            valido = false;
        }

        if (!valido) return;

        // Verificar número de serie único
        verificarNumeroSerieUnico(numeroSerie, esUnico -> {
            if (esUnico) {
                // Guardar equipo
                if (fotoUri != null) {
                    subirFotoYGuardarEquipo(clienteIdSeleccionado, tipo, marca, modelo, numeroSerie, ubicacion, estado);
                } else {
                    guardarEquipo(clienteIdSeleccionado, tipo, marca, modelo, numeroSerie, ubicacion, estado, fotoURL);
                }
            } else {
                tilNumeroSerie.setError("Este número de serie ya está registrado");
                Toast.makeText(this, "El número de serie debe ser único", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verificarNumeroSerieUnico(String numeroSerie, OnVerificacionListener listener) {
        db.collection("equipos")
                .whereEqualTo("numeroSerie", numeroSerie)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (modoEditar) {
                        // En modo editar, permitir si es el mismo equipo
                        boolean esUnico = querySnapshots.isEmpty() ||
                                (querySnapshots.size() == 1 &&
                                        querySnapshots.getDocuments().get(0).getId().equals(equipoActual.getEquipoId()));
                        listener.onResultado(esUnico);
                    } else {
                        // En modo crear, debe ser único
                        listener.onResultado(querySnapshots.isEmpty());
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al verificar número de serie", Toast.LENGTH_SHORT).show();
                    listener.onResultado(false);
                });
    }

    private void subirFotoYGuardarEquipo(String clienteId, String tipo, String marca, String modelo,
                                         String numeroSerie, String ubicacion, String estado) {
        mostrarCargando(true);

        // Log de información de depuración
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "NULL";
        android.util.Log.d("AgregarEquipo", "🔐 Usuario autenticado: " + userId);
        android.util.Log.d("AgregarEquipo", "📸 Intentando subir foto...");

        String nombreArchivo = "equipos/" + UUID.randomUUID().toString() + ".jpg";
        android.util.Log.d("AgregarEquipo", "📁 Ruta: " + nombreArchivo);

        StorageReference fotoRef = storageRef.child(nombreArchivo);

        fotoRef.putFile(fotoUri)
                .addOnSuccessListener(taskSnapshot -> {
                    android.util.Log.d("AgregarEquipo", "✅ Foto subida exitosamente");
                    fotoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String urlFoto = uri.toString();
                        android.util.Log.d("AgregarEquipo", "🔗 URL obtenida: " + urlFoto);
                        guardarEquipo(clienteId, tipo, marca, modelo, numeroSerie, ubicacion, estado, urlFoto);
                    });
                })
                .addOnFailureListener(e -> {
                    mostrarCargando(false);
                    android.util.Log.e("AgregarEquipo", "❌ Error al subir foto: " + e.getClass().getName());
                    android.util.Log.e("AgregarEquipo", "❌ Mensaje: " + e.getMessage());
                    android.util.Log.e("AgregarEquipo", "❌ Causa: " + (e.getCause() != null ? e.getCause().getMessage() : "null"));
                    Toast.makeText(this, "Error al subir foto: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void guardarEquipo(String clienteId, String tipo, String marca, String modelo,
                               String numeroSerie, String ubicacion, String estado, String urlFoto) {
        Map<String, Object> equipoData = new HashMap<>();
        equipoData.put("clienteId", clienteId);
        equipoData.put("tipo", tipo);
        equipoData.put("marca", marca);
        equipoData.put("modelo", modelo);
        equipoData.put("numeroSerie", numeroSerie);
        equipoData.put("ubicacionEspecifica", ubicacion);
        equipoData.put("estado", estado);
        equipoData.put("fotografiaURL", urlFoto);

        if (fechaAdquisicionSeleccionada != null) {
            equipoData.put("fechaAdquisicion", new Timestamp(fechaAdquisicionSeleccionada));
        }

        if (modoEditar) {
            // Actualizar equipo existente
            db.collection("equipos").document(equipoActual.getEquipoId())
                    .update(equipoData)
                    .addOnSuccessListener(aVoid -> {
                        mostrarCargando(false);
                        Toast.makeText(this, "✅ Equipo actualizado correctamente", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        mostrarCargando(false);
                        Toast.makeText(this, "❌ Error al actualizar equipo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Crear nuevo equipo
            equipoData.put("fechaRegistro", Timestamp.now());
            equipoData.put("registradoPor", auth.getCurrentUser().getUid());
            equipoData.put("totalMantenimientos", 0);

            db.collection("equipos")
                    .add(equipoData)
                    .addOnSuccessListener(documentReference -> {
                        // Actualizar contador de equipos del cliente
                        actualizarContadorEquiposCliente(clienteId, true);

                        mostrarCargando(false);
                        Toast.makeText(this, "✅ Equipo registrado correctamente", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        mostrarCargando(false);
                        Toast.makeText(this, "❌ Error al guardar equipo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void actualizarContadorEquiposCliente(String clienteId, boolean incrementar) {
        db.collection("clientes").document(clienteId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long totalEquipos = documentSnapshot.getLong("totalEquipos");
                        int nuevoTotal = (totalEquipos != null ? totalEquipos.intValue() : 0) + (incrementar ? 1 : -1);

                        db.collection("clientes").document(clienteId)
                                .update("totalEquipos", nuevoTotal);
                    }
                });
    }

    private void mostrarCargando(boolean mostrar) {
        progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        btnGuardar.setEnabled(!mostrar);
        btnCancelar.setEnabled(!mostrar);
    }

    // Interface para callback de verificación
    interface OnVerificacionListener {
        void onResultado(boolean esUnico);
    }
}
