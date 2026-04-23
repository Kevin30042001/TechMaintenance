package com.techsolution.techmaintenance.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.models.Usuario;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class PerfilAdminActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    // Vistas
    private Toolbar toolbar;
    private CircleImageView imgPerfil;
    private MaterialButton btnCambiarFoto;
    private TextInputLayout tilNombre, tilEmail, tilTelefono;
    private TextInputEditText etNombre, etEmail, etTelefono;
    private TextView tvFechaIngreso;
    private MaterialButton btnGuardarCambios;
    private MaterialButton btnCambiarPassword;
    private MaterialButton btnAdministrarTecnicos;
    private MaterialButton btnCerrarSesion;

    // Variables
    private Usuario usuarioActual;
    private String userId;

    // Variables para foto
    private Uri fotoUri;
    private Uri tempFotoUri; // Para foto de cámara
    private String nuevaFotoURL; // URL de la nueva foto subida

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> camaraLauncher;
    private ActivityResultLauncher<Intent> galeriaLauncher;
    private ActivityResultLauncher<String> permisoCamaraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil_admin);

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // Inicializar launchers
        inicializarLaunchers();

        // Obtener ID del usuario actual
        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
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

        // Cargar datos del usuario
        cargarDatosUsuario();
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        imgPerfil = findViewById(R.id.imgPerfil);
        btnCambiarFoto = findViewById(R.id.btnCambiarFoto);
        tilNombre = findViewById(R.id.tilNombre);
        tilEmail = findViewById(R.id.tilEmail);
        tilTelefono = findViewById(R.id.tilTelefono);
        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etTelefono = findViewById(R.id.etTelefono);
        tvFechaIngreso = findViewById(R.id.tvFechaIngreso);
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
        btnCambiarPassword = findViewById(R.id.btnCambiarPassword);
        btnAdministrarTecnicos = findViewById(R.id.btnAdministrarTecnicos);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
    }

    private void configurarListeners() {
        // Cambiar foto
        btnCambiarFoto.setOnClickListener(v -> mostrarDialogoSeleccionarFoto());

        // Guardar cambios
        btnGuardarCambios.setOnClickListener(v -> {
            if (validarCampos()) {
                guardarCambios();
            }
        });

        // Cambiar contraseña
        btnCambiarPassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, CambiarPasswordActivity.class);
            startActivity(intent);
        });

        // Administrar técnicos
        btnAdministrarTecnicos.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdministrarTecnicosActivity.class);
            startActivity(intent);
        });

        // Cerrar sesión
        btnCerrarSesion.setOnClickListener(v -> mostrarDialogoCerrarSesion());
    }

    private void cargarDatosUsuario() {
        android.util.Log.d("PerfilAdmin", "📥 Cargando datos del usuario: " + userId);

        db.collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        usuarioActual = documentSnapshot.toObject(Usuario.class);
                        if (usuarioActual != null) {
                            usuarioActual.setUserId(documentSnapshot.getId());

                            // Rellenar campos
                            etNombre.setText(usuarioActual.getNombre());
                            etEmail.setText(usuarioActual.getEmail());
                            etTelefono.setText(usuarioActual.getTelefono());

                            // Mostrar fecha de ingreso
                            if (usuarioActual.getFechaCreacion() != null) {
                                String fechaFormateada = formatearFecha(usuarioActual.getFechaCreacion());
                                tvFechaIngreso.setText(fechaFormateada);
                            }

                            // Cargar foto si existe
                            if (usuarioActual.getFotoPerfilURL() != null && !usuarioActual.getFotoPerfilURL().isEmpty()) {
                                Glide.with(PerfilAdminActivity.this)
                                        .load(usuarioActual.getFotoPerfilURL())
                                        .placeholder(R.drawable.ic_person)
                                        .error(R.drawable.ic_person)
                                        .circleCrop()
                                        .into(imgPerfil);
                                android.util.Log.d("PerfilAdmin", "📷 Foto cargada: " + usuarioActual.getFotoPerfilURL());
                            }

                            android.util.Log.d("PerfilAdmin", "✅ Datos cargados: " + usuarioActual.getNombre());
                        }
                    } else {
                        Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("PerfilAdmin", "❌ Error al cargar datos: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String formatearFecha(Timestamp timestamp) {
        if (timestamp == null) return "No disponible";

        Date fecha = timestamp.toDate();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(fecha);
    }

    private boolean validarCampos() {
        boolean valido = true;

        // Validar nombre
        String nombre = etNombre.getText().toString().trim();
        if (nombre.isEmpty()) {
            tilNombre.setError("El nombre es obligatorio");
            valido = false;
        } else {
            tilNombre.setError(null);
        }

        return valido;
    }

    private void guardarCambios() {
        btnGuardarCambios.setEnabled(false);
        btnGuardarCambios.setText("GUARDANDO...");

        String nombre = etNombre.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();

        android.util.Log.d("PerfilAdmin", "💾 Guardando cambios...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("nombre", nombre);
        updates.put("telefono", telefono);
        updates.put("ultimaModificacion", Timestamp.now());

        db.collection("usuarios").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("PerfilAdmin", "✅ Cambios guardados");
                    Toast.makeText(this, "Cambios guardados correctamente", Toast.LENGTH_SHORT).show();
                    btnGuardarCambios.setEnabled(true);
                    btnGuardarCambios.setText("GUARDAR CAMBIOS");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("PerfilAdmin", "❌ Error al guardar: " + e.getMessage());
                    Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnGuardarCambios.setEnabled(true);
                    btnGuardarCambios.setText("GUARDAR CAMBIOS");
                });
    }

    private void mostrarDialogoCerrarSesion() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que deseas cerrar sesión?")
                .setIcon(R.drawable.ic_exit)
                .setPositiveButton("Sí, cerrar sesión", (dialog, which) -> cerrarSesion())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void cerrarSesion() {
        android.util.Log.d("PerfilAdmin", "🚪 Cerrando sesión...");

        // Cerrar sesión en Firebase
        auth.signOut();

        // Limpiar preferencias (si se usa Remember Me)
        getSharedPreferences("user_prefs", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        // Redirigir a LoginActivity
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
    }

    // ==================== MÉTODOS PARA CAMBIAR FOTO ====================

    private void inicializarLaunchers() {
        // Launcher para cámara
        camaraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (tempFotoUri != null) {
                            fotoUri = tempFotoUri;
                            // Mostrar imagen en el ImageView
                            Glide.with(this)
                                    .load(fotoUri)
                                    .placeholder(R.drawable.ic_person)
                                    .error(R.drawable.ic_person)
                                    .circleCrop()
                                    .into(imgPerfil);
                            // Subir foto a Firebase
                            subirFotoAFirebase();
                        }
                    }
                }
        );

        // Launcher para galería
        galeriaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        fotoUri = result.getData().getData();
                        if (fotoUri != null) {
                            // Mostrar imagen en el ImageView
                            Glide.with(this)
                                    .load(fotoUri)
                                    .placeholder(R.drawable.ic_person)
                                    .error(R.drawable.ic_person)
                                    .circleCrop()
                                    .into(imgPerfil);
                            // Subir foto a Firebase
                            subirFotoAFirebase();
                        }
                    }
                }
        );

        // Launcher para permiso de cámara
        permisoCamaraLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        abrirCamara();
                    } else {
                        Toast.makeText(this, "Se necesita permiso de cámara para tomar fotos", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void mostrarDialogoSeleccionarFoto() {
        // Verificar si hay foto actual para mostrar opción de eliminar
        boolean tieneFoto = usuarioActual != null &&
                usuarioActual.getFotoPerfilURL() != null &&
                !usuarioActual.getFotoPerfilURL().isEmpty();

        String[] opciones;
        if (tieneFoto) {
            opciones = new String[]{"Tomar Foto", "Seleccionar de Galería", "Eliminar Foto Actual"};
        } else {
            opciones = new String[]{"Tomar Foto", "Seleccionar de Galería"};
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Cambiar Foto de Perfil")
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        // Tomar foto
                        verificarPermisoYAbrirCamara();
                    } else if (which == 1) {
                        // Seleccionar de galería
                        abrirGaleria();
                    } else if (which == 2 && tieneFoto) {
                        // Eliminar foto actual
                        mostrarDialogoConfirmarEliminarFoto();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoConfirmarEliminarFoto() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar Foto de Perfil")
                .setMessage("¿Estás seguro de que deseas eliminar tu foto de perfil?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarFotoDePerfil())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarFotoDePerfil() {
        if (usuarioActual == null || usuarioActual.getFotoPerfilURL() == null || usuarioActual.getFotoPerfilURL().isEmpty()) {
            Toast.makeText(this, "No hay foto para eliminar", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCambiarFoto.setEnabled(false);
        btnCambiarFoto.setText("Eliminando...");

        // Eliminar URL de Firestore primero
        Map<String, Object> updates = new HashMap<>();
        updates.put("fotoPerfilURL", "");

        db.collection("usuarios").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("PerfilAdmin", "✅ Foto eliminada de Firestore");

                    // Restaurar imagen por defecto
                    imgPerfil.setImageResource(R.drawable.ic_person);

                    // Actualizar objeto local
                    if (usuarioActual != null) {
                        usuarioActual.setFotoPerfilURL("");
                    }

                    btnCambiarFoto.setEnabled(true);
                    btnCambiarFoto.setText("CAMBIAR FOTO");
                    Toast.makeText(this, "Foto de perfil eliminada", Toast.LENGTH_SHORT).show();

                    // Opcional: Eliminar archivo de Storage (requiere saber la ruta exacta)
                    // Por ahora solo eliminamos la referencia en Firestore
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("PerfilAdmin", "❌ Error al eliminar foto: " + e.getMessage());
                    btnCambiarFoto.setEnabled(true);
                    btnCambiarFoto.setText("CAMBIAR FOTO");
                    Toast.makeText(this, "Error al eliminar foto", Toast.LENGTH_SHORT).show();
                });
    }

    private void verificarPermisoYAbrirCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            permisoCamaraLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirCamara() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Crear archivo temporal para la foto
        File photoFile = null;
        try {
            File storageDir = getExternalFilesDir(null);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "PERFIL_" + timeStamp + ".jpg";
            photoFile = new File(storageDir, fileName);

            tempFotoUri = FileProvider.getUriForFile(this,
                    "com.techsolution.techmaintenance.fileprovider",
                    photoFile);

            intent.putExtra(MediaStore.EXTRA_OUTPUT, tempFotoUri);
            camaraLauncher.launch(intent);
        } catch (Exception e) {
            android.util.Log.e("PerfilAdmin", "❌ Error al crear archivo de foto: " + e.getMessage());
            Toast.makeText(this, "Error al abrir cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galeriaLauncher.launch(intent);
    }

    private void subirFotoAFirebase() {
        if (fotoUri == null) {
            Toast.makeText(this, "Error: No hay imagen seleccionada", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCambiarFoto.setEnabled(false);
        btnCambiarFoto.setText("Subiendo...");

        // Crear referencia única para la foto
        String nombreArchivo = "perfil_" + userId + "_" + UUID.randomUUID().toString() + ".jpg";
        StorageReference fotoRef = storageRef.child("fotos_perfil/" + nombreArchivo);

        android.util.Log.d("PerfilAdmin", "📤 Subiendo foto: " + nombreArchivo);

        // Subir archivo
        fotoRef.putFile(fotoUri)
                .addOnSuccessListener(taskSnapshot -> {
                    android.util.Log.d("PerfilAdmin", "✅ Foto subida exitosamente");

                    // Obtener URL de descarga
                    fotoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        nuevaFotoURL = uri.toString();
                        android.util.Log.d("PerfilAdmin", "📷 URL obtenida: " + nuevaFotoURL);

                        // Actualizar URL en Firestore
                        actualizarFotoEnFirestore(nuevaFotoURL);
                    }).addOnFailureListener(e -> {
                        android.util.Log.e("PerfilAdmin", "❌ Error al obtener URL: " + e.getMessage());
                        btnCambiarFoto.setEnabled(true);
                        btnCambiarFoto.setText("CAMBIAR FOTO");
                        Toast.makeText(this, "Error al obtener URL de la foto", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("PerfilAdmin", "❌ Error al subir foto: " + e.getMessage());
                    btnCambiarFoto.setEnabled(true);
                    btnCambiarFoto.setText("CAMBIAR FOTO");
                    Toast.makeText(this, "Error al subir foto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void actualizarFotoEnFirestore(String fotoURL) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fotoPerfilURL", fotoURL);

        db.collection("usuarios").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("PerfilAdmin", "✅ Foto actualizada en Firestore");
                    btnCambiarFoto.setEnabled(true);
                    btnCambiarFoto.setText("CAMBIAR FOTO");
                    Toast.makeText(this, "Foto de perfil actualizada correctamente", Toast.LENGTH_SHORT).show();

                    // Actualizar objeto local
                    if (usuarioActual != null) {
                        usuarioActual.setFotoPerfilURL(fotoURL);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("PerfilAdmin", "❌ Error al actualizar Firestore: " + e.getMessage());
                    btnCambiarFoto.setEnabled(true);
                    btnCambiarFoto.setText("CAMBIAR FOTO");
                    Toast.makeText(this, "Error al actualizar foto en base de datos", Toast.LENGTH_SHORT).show();
                });
    }
}
