package com.techsolution.techmaintenance.activities;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.models.Usuario;

import java.util.HashMap;
import java.util.Map;

public class CrearEditarTecnicoActivity extends AppCompatActivity {

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseFunctions functions;

    // Vistas
    private Toolbar toolbar;
    private TextInputLayout tilNombre, tilEmail, tilTelefono, tilPassword, tilRol;
    private TextInputEditText etNombre, etEmail, etTelefono, etPassword;
    private AutoCompleteTextView actvRol;
    private SwitchMaterial switchEstado;
    private MaterialCheckBox cbEnviarCredenciales;
    private MaterialCardView cardPassword, cardOpciones;
    private MaterialButton btnCancelar, btnGuardar;

    // Variables
    private boolean modoEditar = false;
    private String tecnicoId = null;
    private Usuario tecnicoActual = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_editar_tecnico);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        functions = FirebaseFunctions.getInstance("us-central1");

        // Inicializar vistas
        inicializarVistas();

        // Configurar toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Configurar dropdown de rol
        configurarDropdownRol();

        // Verificar modo (crear o editar)
        verificarModo();

        // Configurar listeners
        configurarListeners();
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        tilNombre = findViewById(R.id.tilNombre);
        tilEmail = findViewById(R.id.tilEmail);
        tilTelefono = findViewById(R.id.tilTelefono);
        tilPassword = findViewById(R.id.tilPassword);
        tilRol = findViewById(R.id.tilRol);
        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etTelefono = findViewById(R.id.etTelefono);
        etPassword = findViewById(R.id.etPassword);
        actvRol = findViewById(R.id.actvRol);
        switchEstado = findViewById(R.id.switchEstado);
        cbEnviarCredenciales = findViewById(R.id.cbEnviarCredenciales);
        cardPassword = findViewById(R.id.cardPassword);
        cardOpciones = findViewById(R.id.cardOpciones);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnGuardar = findViewById(R.id.btnGuardar);
    }

    private void configurarDropdownRol() {
        String[] roles = {"Técnico", "Administrador"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, roles);
        actvRol.setAdapter(adapter);
        actvRol.setText("Técnico", false); // Valor por defecto
    }

    private void verificarModo() {
        if (getIntent().hasExtra("tecnicoId")) {
            // Modo editar
            modoEditar = true;
            tecnicoId = getIntent().getStringExtra("tecnicoId");
            toolbar.setTitle("Editar Técnico");
            btnGuardar.setText("ACTUALIZAR");

            // Ocultar campos que no se editan
            cardPassword.setVisibility(View.GONE);
            cardOpciones.setVisibility(View.GONE);

            // Cargar datos del técnico
            cargarDatosTecnico();
        } else {
            // Modo crear
            modoEditar = false;
            toolbar.setTitle("Nuevo Técnico");
            btnGuardar.setText("CREAR TÉCNICO");

            // Mostrar todos los campos
            cardPassword.setVisibility(View.VISIBLE);
            cardOpciones.setVisibility(View.VISIBLE);
        }
    }

    private void cargarDatosTecnico() {
        if (tecnicoId == null || tecnicoId.isEmpty()) {
            Toast.makeText(this, "Error: ID de técnico no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        android.util.Log.d("CrearEditarTecnico", "📥 Cargando técnico: " + tecnicoId);

        db.collection("usuarios").document(tecnicoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tecnicoActual = documentSnapshot.toObject(Usuario.class);
                        if (tecnicoActual != null) {
                            tecnicoActual.setUserId(documentSnapshot.getId());

                            // Rellenar campos
                            etNombre.setText(tecnicoActual.getNombre());
                            etEmail.setText(tecnicoActual.getEmail());
                            etTelefono.setText(tecnicoActual.getTelefono());

                            // Email no editable en modo editar
                            etEmail.setEnabled(false);
                            tilEmail.setHelperText("No se puede modificar el email");

                            // Establecer rol
                            String rolTexto = "admin".equals(tecnicoActual.getRol()) ? "Administrador" : "Técnico";
                            actvRol.setText(rolTexto, false);

                            // Establecer estado
                            switchEstado.setChecked("activo".equals(tecnicoActual.getEstado()));

                            android.util.Log.d("CrearEditarTecnico", "✅ Datos cargados: " + tecnicoActual.getNombre());
                        }
                    } else {
                        Toast.makeText(this, "Técnico no encontrado", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CrearEditarTecnico", "❌ Error al cargar técnico: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void configurarListeners() {
        // Switch de estado
        switchEstado.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Actualizar descripción (puedes agregar un TextView para esto si lo necesitas)
            android.util.Log.d("CrearEditarTecnico", "Estado: " + (isChecked ? "Activo" : "Inactivo"));
        });

        // Botón cancelar
        btnCancelar.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Cancelar")
                    .setMessage("¿Estás seguro de que deseas cancelar? Los cambios no se guardarán.")
                    .setPositiveButton("Sí, cancelar", (dialog, which) -> finish())
                    .setNegativeButton("Continuar editando", null)
                    .show();
        });

        // Botón guardar
        btnGuardar.setOnClickListener(v -> {
            if (validarCampos()) {
                if (modoEditar) {
                    actualizarTecnico();
                } else {
                    crearTecnico();
                }
            }
        });
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

        // Validar email
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            tilEmail.setError("El email es obligatorio");
            valido = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email no válido");
            valido = false;
        } else {
            tilEmail.setError(null);
        }

        // Validar contraseña (solo en modo crear)
        if (!modoEditar) {
            String password = etPassword.getText().toString().trim();
            if (password.isEmpty()) {
                tilPassword.setError("La contraseña es obligatoria");
                valido = false;
            } else if (password.length() < 6) {
                tilPassword.setError("Mínimo 6 caracteres");
                valido = false;
            } else {
                tilPassword.setError(null);
            }
        }

        // Validar rol
        String rol = actvRol.getText().toString().trim();
        if (rol.isEmpty()) {
            tilRol.setError("Selecciona un rol");
            valido = false;
        } else {
            tilRol.setError(null);
        }

        return valido;
    }

    private void crearTecnico() {
        btnGuardar.setEnabled(false);
        btnGuardar.setText("CREANDO...");

        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String rolTexto = actvRol.getText().toString().trim();
        String rol = "Administrador".equals(rolTexto) ? "admin" : "tecnico";
        String estado = switchEstado.isChecked() ? "activo" : "inactivo";
        boolean enviarEmail = cbEnviarCredenciales.isChecked();

        android.util.Log.d("CrearEditarTecnico", "🔨 Creando técnico mediante Cloud Function: " + email);

        // ✅ CORREGIDO: Usar Cloud Function en lugar de auth.createUserWithEmailAndPassword()
        // Esto evita que la sesión del admin cambie al nuevo usuario creado
        Map<String, Object> data = new HashMap<>();
        data.put("nombre", nombre);
        data.put("email", email);
        data.put("password", password);
        data.put("telefono", telefono);
        data.put("rol", rol);
        data.put("estado", estado);
        data.put("enviarEmail", enviarEmail);

        functions.getHttpsCallable("crearTecnico")
                .call(data)
                .addOnSuccessListener(result -> {
                    android.util.Log.d("CrearEditarTecnico", "✅ Técnico creado exitosamente mediante Cloud Function");

                    Map<String, Object> responseData = (Map<String, Object>) result.getData();
                    boolean success = (boolean) responseData.get("success");
                    String message = (String) responseData.get("message");

                    if (success) {
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        btnGuardar.setEnabled(true);
                        btnGuardar.setText("CREAR TÉCNICO");
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CrearEditarTecnico", "❌ Error al crear técnico: " + e.getMessage());

                    String errorMsg = "Error al crear técnico";
                    String exceptionMsg = e.getMessage().toLowerCase();

                    if (exceptionMsg.contains("email") && exceptionMsg.contains("already")) {
                        errorMsg = "Ya existe una cuenta con este email";
                    } else if (exceptionMsg.contains("weak-password") || exceptionMsg.contains("muy débil")) {
                        errorMsg = "La contraseña es muy débil (mínimo 6 caracteres)";
                    } else if (exceptionMsg.contains("invalid-email") || exceptionMsg.contains("no válido")) {
                        errorMsg = "El email no es válido";
                    } else {
                        errorMsg = e.getMessage();
                    }

                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("CREAR TÉCNICO");
                });
    }

    private void actualizarTecnico() {
        btnGuardar.setEnabled(false);
        btnGuardar.setText("ACTUALIZANDO...");

        String nombre = etNombre.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String rolTexto = actvRol.getText().toString().trim();
        String rol = "Administrador".equals(rolTexto) ? "admin" : "tecnico";
        String estado = switchEstado.isChecked() ? "activo" : "inactivo";

        android.util.Log.d("CrearEditarTecnico", "🔄 Actualizando técnico: " + tecnicoId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("nombre", nombre);
        updates.put("telefono", telefono);
        updates.put("rol", rol);
        updates.put("estado", estado);
        updates.put("ultimaModificacion", com.google.firebase.Timestamp.now());

        db.collection("usuarios").document(tecnicoId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("CrearEditarTecnico", "✅ Técnico actualizado");
                    Toast.makeText(this, "Técnico actualizado correctamente", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CrearEditarTecnico", "❌ Error al actualizar: " + e.getMessage());
                    Toast.makeText(this, "Error al actualizar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("ACTUALIZAR");
                });
    }

    @Override
    public void onBackPressed() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancelar")
                .setMessage("¿Estás seguro de que deseas salir? Los cambios no se guardarán.")
                .setPositiveButton("Sí, salir", (dialog, which) -> {
                    super.onBackPressed();
                })
                .setNegativeButton("Continuar editando", null)
                .show();
    }
}
