package com.techsolution.techmaintenance.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.techsolution.techmaintenance.R;

import java.util.regex.Pattern;

public class CambiarPasswordActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    // Vistas
    private Toolbar toolbar;
    private TextInputLayout tilPasswordActual, tilPasswordNueva, tilPasswordConfirmar;
    private TextInputEditText etPasswordActual, etPasswordNueva, etPasswordConfirmar;
    private MaterialButton btnCancelar, btnCambiarPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cambiar_password);

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
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
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        tilPasswordActual = findViewById(R.id.tilPasswordActual);
        tilPasswordNueva = findViewById(R.id.tilPasswordNueva);
        tilPasswordConfirmar = findViewById(R.id.tilPasswordConfirmar);
        etPasswordActual = findViewById(R.id.etPasswordActual);
        etPasswordNueva = findViewById(R.id.etPasswordNueva);
        etPasswordConfirmar = findViewById(R.id.etPasswordConfirmar);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnCambiarPassword = findViewById(R.id.btnCambiarPassword);
    }

    private void configurarListeners() {
        // Botón cancelar
        btnCancelar.setOnClickListener(v -> finish());

        // Botón cambiar contraseña
        btnCambiarPassword.setOnClickListener(v -> {
            if (validarCampos()) {
                cambiarPassword();
            }
        });
    }

    private boolean validarCampos() {
        boolean valido = true;

        // Obtener valores
        String passwordActual = etPasswordActual.getText().toString().trim();
        String passwordNueva = etPasswordNueva.getText().toString().trim();
        String passwordConfirmar = etPasswordConfirmar.getText().toString().trim();

        // Validar contraseña actual
        if (passwordActual.isEmpty()) {
            tilPasswordActual.setError("Ingresa tu contraseña actual");
            valido = false;
        } else {
            tilPasswordActual.setError(null);
        }

        // Validar nueva contraseña
        if (passwordNueva.isEmpty()) {
            tilPasswordNueva.setError("Ingresa la nueva contraseña");
            valido = false;
        } else if (passwordNueva.length() < 6) {
            tilPasswordNueva.setError("Mínimo 6 caracteres");
            valido = false;
        } else if (!validarSeguridadPassword(passwordNueva)) {
            tilPasswordNueva.setError("Debe contener al menos una letra y un número");
            valido = false;
        } else if (passwordNueva.equals(passwordActual)) {
            tilPasswordNueva.setError("Debe ser diferente a la contraseña actual");
            valido = false;
        } else {
            tilPasswordNueva.setError(null);
        }

        // Validar confirmación
        if (passwordConfirmar.isEmpty()) {
            tilPasswordConfirmar.setError("Confirma la nueva contraseña");
            valido = false;
        } else if (!passwordConfirmar.equals(passwordNueva)) {
            tilPasswordConfirmar.setError("Las contraseñas no coinciden");
            valido = false;
        } else {
            tilPasswordConfirmar.setError(null);
        }

        return valido;
    }

    /**
     * Valida que la contraseña contenga al menos una letra y un número
     */
    private boolean validarSeguridadPassword(String password) {
        // Al menos una letra y un número
        Pattern pattern = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).+$");
        return pattern.matcher(password).matches();
    }

    private void cambiarPassword() {
        btnCambiarPassword.setEnabled(false);
        btnCambiarPassword.setText("CAMBIANDO...");

        String passwordActual = etPasswordActual.getText().toString().trim();
        String passwordNueva = etPasswordNueva.getText().toString().trim();

        String email = currentUser.getEmail();

        android.util.Log.d("CambiarPassword", "🔐 Iniciando cambio de contraseña para: " + email);

        // Paso 1: Re-autenticar al usuario
        AuthCredential credential = EmailAuthProvider.getCredential(email, passwordActual);

        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("CambiarPassword", "✅ Re-autenticación exitosa");

                    // Paso 2: Actualizar contraseña
                    currentUser.updatePassword(passwordNueva)
                            .addOnSuccessListener(aVoid2 -> {
                                android.util.Log.d("CambiarPassword", "✅ Contraseña actualizada");

                                mostrarDialogoExito();
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("CambiarPassword", "❌ Error al actualizar contraseña: " + e.getMessage());

                                String errorMsg = "Error al actualizar contraseña";
                                String exceptionMsg = e.getMessage().toLowerCase();

                                if (exceptionMsg.contains("weak-password")) {
                                    errorMsg = "La contraseña es muy débil";
                                } else if (exceptionMsg.contains("requires-recent-login")) {
                                    errorMsg = "Por seguridad, debes cerrar sesión y volver a iniciar";
                                }

                                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                                btnCambiarPassword.setEnabled(true);
                                btnCambiarPassword.setText("CAMBIAR CONTRASEÑA");
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CambiarPassword", "❌ Error en re-autenticación: " + e.getMessage());

                    String errorMsg = "Contraseña actual incorrecta";
                    String exceptionMsg = e.getMessage().toLowerCase();

                    if (exceptionMsg.contains("password") ||
                        exceptionMsg.contains("invalid-credential") ||
                        exceptionMsg.contains("wrong-password")) {
                        errorMsg = "La contraseña actual es incorrecta";
                        tilPasswordActual.setError(errorMsg);
                        etPasswordActual.setText("");
                        etPasswordActual.requestFocus();
                    } else if (exceptionMsg.contains("too-many-requests")) {
                        errorMsg = "Demasiados intentos. Intenta más tarde";
                    } else if (exceptionMsg.contains("network")) {
                        errorMsg = "Error de conexión. Verifica tu internet";
                    }

                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    btnCambiarPassword.setEnabled(true);
                    btnCambiarPassword.setText("CAMBIAR CONTRASEÑA");
                });
    }

    private void mostrarDialogoExito() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Contraseña Actualizada")
                .setMessage("Tu contraseña ha sido cambiada exitosamente. Por seguridad, se recomienda cerrar sesión y volver a iniciar con la nueva contraseña.")
                .setIcon(R.drawable.ic_check_circle)
                .setPositiveButton("Entendido", (dialog, which) -> {
                    setResult(RESULT_OK);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onBackPressed() {
        // Limpiar campos al salir
        etPasswordActual.setText("");
        etPasswordNueva.setText("");
        etPasswordConfirmar.setText("");
        super.onBackPressed();
    }
}
