package com.techsolution.techmaintenance.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.models.Cliente;
public class AgregarEditarClienteActivity extends AppCompatActivity {

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Vistas
    private Toolbar toolbar;
    private TextInputLayout tilNombreEmpresa, tilNombreContacto, tilCargoContacto;
    private TextInputLayout tilEmailContacto, tilTelefonoContacto, tilTelefonoAlternativo;
    private TextInputLayout tilDireccion, tilReferencia;
    private TextInputEditText etNombreEmpresa, etNombreContacto, etCargoContacto;
    private TextInputEditText etEmailContacto, etTelefonoContacto, etTelefonoAlternativo;
    private TextInputEditText etDireccion, etReferencia;
    private MaterialButton btnCancelar, btnGuardar;
    private ProgressBar progressBar;

    // Variables
    private boolean modoEditar = false;
    private Cliente clienteActual;
    private String emailOriginal; // Para validar cambios de email


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_editar_cliente);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Inicializar vistas
        inicializarVistas();

        // ⭐ CAMBIO: usar getParcelableExtra en lugar de getSerializableExtra
        if (getIntent().hasExtra("cliente")) {
            modoEditar = true;

            // Para Android 13+ (API 33+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                clienteActual = getIntent().getParcelableExtra("cliente", Cliente.class);
            } else {
                // Para versiones anteriores
                clienteActual = getIntent().getParcelableExtra("cliente");
            }

            cargarDatosCliente();
            toolbar.setTitle(R.string.editar_cliente);
        } else {
            toolbar.setTitle(R.string.agregar_cliente);
        }

        // Configurar toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Configurar listeners
        configurarListeners();
    }
    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);

        tilNombreEmpresa = findViewById(R.id.tilNombreEmpresa);
        tilNombreContacto = findViewById(R.id.tilNombreContacto);
        tilCargoContacto = findViewById(R.id.tilCargoContacto);
        tilEmailContacto = findViewById(R.id.tilEmailContacto);
        tilTelefonoContacto = findViewById(R.id.tilTelefonoContacto);
        tilTelefonoAlternativo = findViewById(R.id.tilTelefonoAlternativo);
        tilDireccion = findViewById(R.id.tilDireccion);
        tilReferencia = findViewById(R.id.tilReferenciaDireccion);

        etNombreEmpresa = findViewById(R.id.etNombreEmpresa);
        etNombreContacto = findViewById(R.id.etNombreContacto);
        etCargoContacto = findViewById(R.id.etCargoContacto);
        etEmailContacto = findViewById(R.id.etEmailContacto);
        etTelefonoContacto = findViewById(R.id.etTelefonoContacto);
        etTelefonoAlternativo = findViewById(R.id.etTelefonoAlternativo);
        etDireccion = findViewById(R.id.etDireccion);
        etReferencia = findViewById(R.id.etReferenciaDireccion);

        btnCancelar = findViewById(R.id.btnCancelar);
        btnGuardar = findViewById(R.id.btnGuardar);
        progressBar = findViewById(R.id.progressBarGuardar);
    }

    private void configurarListeners() {
        // Botón cancelar
        btnCancelar.setOnClickListener(v -> finish());

        // Botón guardar
        btnGuardar.setOnClickListener(v -> validarYGuardar());

        // Toolbar navigation
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void cargarDatosCliente() {
        if (clienteActual == null) return;

        etNombreEmpresa.setText(clienteActual.getNombreEmpresa());
        etNombreContacto.setText(clienteActual.getNombreContacto());
        etCargoContacto.setText(clienteActual.getCargoContacto());
        etEmailContacto.setText(clienteActual.getEmailContacto());
        etTelefonoContacto.setText(clienteActual.getTelefonoContacto());
        etTelefonoAlternativo.setText(clienteActual.getTelefonoAlternativo());
        etDireccion.setText(clienteActual.getDireccion());
        etReferencia.setText(clienteActual.getReferenciaDireccion());

        emailOriginal = clienteActual.getEmailContacto();
    }

    private void validarYGuardar() {
        // Limpiar errores previos
        limpiarErrores();

        // Obtener valores
        String nombreEmpresa = etNombreEmpresa.getText().toString().trim();
        String nombreContacto = etNombreContacto.getText().toString().trim();
        String cargoContacto = etCargoContacto.getText().toString().trim();
        String emailContacto = etEmailContacto.getText().toString().trim();
        String telefonoContacto = etTelefonoContacto.getText().toString().trim();
        String telefonoAlternativo = etTelefonoAlternativo.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();
        String referencia = etReferencia.getText().toString().trim();

        boolean esValido = true;

        // Validar nombre empresa
        if (TextUtils.isEmpty(nombreEmpresa)) {
            tilNombreEmpresa.setError(getString(R.string.error_nombre_empresa));
            esValido = false;
        }

        // Validar nombre contacto
        if (TextUtils.isEmpty(nombreContacto)) {
            tilNombreContacto.setError(getString(R.string.error_nombre_contacto));
            esValido = false;
        }

        // Validar email
        if (TextUtils.isEmpty(emailContacto)) {
            tilEmailContacto.setError(getString(R.string.error_email_vacio));
            esValido = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailContacto).matches()) {
            tilEmailContacto.setError(getString(R.string.error_email_invalido));
            esValido = false;
        }

        // Validar teléfono
        if (TextUtils.isEmpty(telefonoContacto)) {
            tilTelefonoContacto.setError(getString(R.string.error_telefono_vacio));
            esValido = false;
        } else if (telefonoContacto.length() < 8) {
            tilTelefonoContacto.setError(getString(R.string.error_telefono_invalido));
            esValido = false;
        }

        // Validar dirección
        if (TextUtils.isEmpty(direccion)) {
            tilDireccion.setError(getString(R.string.error_direccion_vacia));
            esValido = false;
        }

        if (!esValido) {
            Toast.makeText(this, "Por favor corrige los errores", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar si el email cambió (en modo editar)
        boolean emailCambio = modoEditar && !emailContacto.equals(emailOriginal);

        // Si el email cambió o es nuevo, verificar que no exista
        if (!modoEditar || emailCambio) {
            verificarEmailUnico(emailContacto, () -> {
                // Email es único, proceder a guardar
                guardarCliente(nombreEmpresa, nombreContacto, cargoContacto,
                        emailContacto, telefonoContacto, telefonoAlternativo,
                        direccion, referencia);
            });
        } else {
            // Email no cambió, guardar directamente
            guardarCliente(nombreEmpresa, nombreContacto, cargoContacto,
                    emailContacto, telefonoContacto, telefonoAlternativo,
                    direccion, referencia);
        }
    }

    private void verificarEmailUnico(String email, Runnable onEmailUnico) {
        mostrarCargando(true);

        db.collection("clientes")
                .whereEqualTo("emailContacto", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Email único
                        onEmailUnico.run();
                    } else {
                        // Email duplicado
                        mostrarCargando(false);
                        tilEmailContacto.setError(getString(R.string.error_email_duplicado));
                        Toast.makeText(this, R.string.error_email_duplicado, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    mostrarCargando(false);
                    Toast.makeText(this, "Error al verificar email", Toast.LENGTH_SHORT).show();
                });
    }

    private void guardarCliente(String nombreEmpresa, String nombreContacto, String cargoContacto,
                                String emailContacto, String telefonoContacto, String telefonoAlternativo,
                                String direccion, String referencia) {
        mostrarCargando(true);

        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        if (modoEditar) {
            // Actualizar cliente existente
            clienteActual.setNombreEmpresa(nombreEmpresa);
            clienteActual.setNombreContacto(nombreContacto);
            clienteActual.setCargoContacto(cargoContacto);
            clienteActual.setEmailContacto(emailContacto);
            clienteActual.setTelefonoContacto(telefonoContacto);
            clienteActual.setTelefonoAlternativo(telefonoAlternativo);
            clienteActual.setDireccion(direccion);
            clienteActual.setReferenciaDireccion(referencia);
            clienteActual.setUltimaModificacion(Timestamp.now());

            db.collection("clientes")
                    .document(clienteActual.getClienteId())
                    .set(clienteActual.toMap())
                    .addOnSuccessListener(aVoid -> {
                        mostrarCargando(false);
                        Toast.makeText(this, R.string.cliente_actualizado, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        mostrarCargando(false);
                        Toast.makeText(this,
                                getString(R.string.error_guardar_cliente) + ": " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });

        } else {
            // Crear nuevo cliente
            Cliente nuevoCliente = new Cliente();
            nuevoCliente.setNombreEmpresa(nombreEmpresa);
            nuevoCliente.setNombreContacto(nombreContacto);
            nuevoCliente.setCargoContacto(cargoContacto);
            nuevoCliente.setEmailContacto(emailContacto);
            nuevoCliente.setTelefonoContacto(telefonoContacto);
            nuevoCliente.setTelefonoAlternativo(telefonoAlternativo);
            nuevoCliente.setDireccion(direccion);
            nuevoCliente.setReferenciaDireccion(referencia);
            nuevoCliente.setFechaRegistro(Timestamp.now());
            nuevoCliente.setRegistradoPor(userId);
            nuevoCliente.setTotalEquipos(0);
            nuevoCliente.setUltimaModificacion(Timestamp.now());

            db.collection("clientes")
                    .add(nuevoCliente.toMap())
                    .addOnSuccessListener(documentReference -> {
                        // Actualizar el documento con su propio ID
                        String clienteId = documentReference.getId();
                        documentReference.update("clienteId", clienteId)
                                .addOnSuccessListener(aVoid -> {
                                    mostrarCargando(false);
                                    Toast.makeText(this, R.string.cliente_guardado, Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    mostrarCargando(false);
                                    Toast.makeText(this, R.string.cliente_guardado, Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                    })
                    .addOnFailureListener(e -> {
                        mostrarCargando(false);
                        Toast.makeText(this,
                                getString(R.string.error_guardar_cliente) + ": " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void limpiarErrores() {
        tilNombreEmpresa.setError(null);
        tilNombreContacto.setError(null);
        tilEmailContacto.setError(null);
        tilTelefonoContacto.setError(null);
        tilDireccion.setError(null);
    }

    private void mostrarCargando(boolean mostrar) {
        progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        btnGuardar.setEnabled(!mostrar);
        btnCancelar.setEnabled(!mostrar);
    }
}