package com.techsolution.techmaintenance.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.RecuperarPasswordActivity;
import com.techsolution.techmaintenance.helpers.NotificationHelper;

import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER = "remember";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Vistas
    private EditText etEmail, etPassword;
    private CheckBox cbRecordarme;
    private Button btnLogin, btnBiometric;
    private TextView tvOlvidePassword;
    private ProgressBar progressBar;

    // Biométrico
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Inicializar SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Inicializar vistas
        inicializarVistas();

        // Configurar biométrico
        configurarBiometrico();

        // Cargar credenciales guardadas
        cargarCredencialesGuardadas();

        // Listeners
        btnLogin.setOnClickListener(v -> iniciarSesion());
        tvOlvidePassword.setOnClickListener(v -> abrirRecuperarPassword());
        btnBiometric.setOnClickListener(v -> mostrarDialogoBiometrico());
    }

    private void inicializarVistas() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbRecordarme = findViewById(R.id.cbRememberSession);
        btnLogin = findViewById(R.id.btnLogin);
        btnBiometric = findViewById(R.id.btnBiometric);
        tvOlvidePassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);
    }

    // ==================== BIOMÉTRICO ====================

    private void configurarBiometrico() {
        // Verificar primero si hay credenciales guardadas
        String emailGuardado = sharedPreferences.getString(KEY_EMAIL, "");
        String passwordGuardado = sharedPreferences.getString(KEY_PASSWORD, "");

        Log.d(TAG, "🔍 DEBUG - Email guardado: [" + (emailGuardado.isEmpty() ? "VACÍO" : "EXISTE") + "]");
        Log.d(TAG, "🔍 DEBUG - Password guardado: [" + (passwordGuardado.isEmpty() ? "VACÍO" : "EXISTE") + "]");

        boolean hayCredenciales = !TextUtils.isEmpty(emailGuardado) && !TextUtils.isEmpty(passwordGuardado);
        Log.d(TAG, "🔍 DEBUG - Hay credenciales: " + hayCredenciales);

        // Si no hay credenciales guardadas, ocultar el botón de huella
        if (!hayCredenciales) {
            Log.d(TAG, "⚠️ No hay credenciales guardadas - Ocultando botón biométrico");
            btnBiometric.setVisibility(View.GONE);
            return;
        }

        Log.d(TAG, "✅ Credenciales encontradas - Verificando hardware biométrico");

        // Si hay credenciales, verificar disponibilidad del hardware biométrico
        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);

        Log.d(TAG, "🔍 DEBUG - Código de autenticación biométrica: " + canAuthenticate);

        switch (canAuthenticate) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                Log.d(TAG, "✅ Biométrico disponible y credenciales guardadas - MOSTRANDO BOTÓN");
                btnBiometric.setVisibility(View.VISIBLE);
                configurarPromptBiometrico();
                break;

            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Log.d(TAG, "❌ No hay hardware biométrico - OCULTANDO BOTÓN");
                btnBiometric.setVisibility(View.GONE);
                break;

            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Log.d(TAG, "❌ Hardware biométrico no disponible - OCULTANDO BOTÓN");
                btnBiometric.setVisibility(View.GONE);
                break;

            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Log.d(TAG, "⚠️ No hay huellas registradas - OCULTANDO BOTÓN");
                btnBiometric.setVisibility(View.GONE);
                break;

            default:
                Log.d(TAG, "❓ Estado biométrico desconocido - OCULTANDO BOTÓN");
                btnBiometric.setVisibility(View.GONE);
                break;
        }
    }

    private void configurarPromptBiometrico() {
        Executor executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(LoginActivity.this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);

                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            Log.d(TAG, "Usuario canceló la autenticación");
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Error de autenticación: " + errString,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);

                        Log.d(TAG, "✅ Autenticación biométrica exitosa");
                        Toast.makeText(LoginActivity.this,
                                "✅ Huella reconocida",
                                Toast.LENGTH_SHORT).show();

                        iniciarSesionConCredencialesGuardadas();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(LoginActivity.this,
                                "❌ Huella no reconocida",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Iniciar sesión con huella")
                .setSubtitle("Usa tu huella digital para acceder")
                .setDescription("Coloca tu dedo en el sensor")
                .setNegativeButtonText("Cancelar")
                .setConfirmationRequired(false)
                .build();
    }

    private void mostrarDialogoBiometrico() {
        // El botón solo está visible si hay credenciales guardadas,
        // pero verificamos por seguridad
        String emailGuardado = sharedPreferences.getString(KEY_EMAIL, "");
        String passwordGuardado = sharedPreferences.getString(KEY_PASSWORD, "");

        if (TextUtils.isEmpty(emailGuardado) || TextUtils.isEmpty(passwordGuardado)) {
            Log.e(TAG, "❌ Error: No debería llegar aquí sin credenciales");
            btnBiometric.setVisibility(View.GONE);
            return;
        }

        biometricPrompt.authenticate(promptInfo);
    }

    private void iniciarSesionConCredencialesGuardadas() {
        String email = sharedPreferences.getString(KEY_EMAIL, "");
        String password = sharedPreferences.getString(KEY_PASSWORD, "");

        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            mostrarCargando(true);

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "✅ Autenticación biométrica completa");

                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                verificarUsuarioYRedirigir(user.getUid());
                            }
                        } else {
                            mostrarCargando(false);
                            Toast.makeText(this,
                                    "Error al iniciar sesión: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    // ==================== LOGIN TRADICIONAL ====================

    private void cargarCredencialesGuardadas() {
        boolean recordar = sharedPreferences.getBoolean(KEY_REMEMBER, false);

        if (recordar) {
            String email = sharedPreferences.getString(KEY_EMAIL, "");
            // Solo cargar el email, NO la contraseña por seguridad
            etEmail.setText(email);
            cbRecordarme.setChecked(true);

            Log.d(TAG, "✅ Email cargado (contraseña guardada de forma segura)");
        }
    }

    private void iniciarSesion() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validaciones
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Ingresa tu email");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email inválido");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Ingresa tu contraseña");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("La contraseña debe tener al menos 6 caracteres");
            etPassword.requestFocus();
            return;
        }

        mostrarCargando(true);

        // Autenticar con Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "✅ Autenticación exitosa");

                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Guardar credenciales si está marcado "Recordarme"
                                if (cbRecordarme.isChecked()) {
                                    guardarCredenciales(email, password);
                                } else {
                                    limpiarCredenciales();
                                }

                                verificarUsuarioYRedirigir(user.getUid());
                            }
                        } else {
                            mostrarCargando(false);
                            Log.e(TAG, "❌ Error de autenticación", task.getException());

                            String errorMsg = "Error desconocido al iniciar sesión";

                            if (task.getException() != null) {
                                String exceptionMsg = task.getException().getMessage();
                                Log.d(TAG, "Excepción completa: " + exceptionMsg);

                                if (exceptionMsg != null) {
                                    // Errores de contraseña
                                    if (exceptionMsg.contains("password") ||
                                        exceptionMsg.contains("INVALID_PASSWORD") ||
                                        exceptionMsg.contains("wrong-password")) {
                                        errorMsg = "Contraseña incorrecta";
                                    }
                                    // Email no registrado
                                    else if (exceptionMsg.contains("no user record") ||
                                             exceptionMsg.contains("user-not-found") ||
                                             exceptionMsg.contains("EMAIL_NOT_FOUND")) {
                                        errorMsg = "No existe una cuenta con este email";
                                    }
                                    // Errores de red
                                    else if (exceptionMsg.contains("network") ||
                                             exceptionMsg.contains("NetworkException")) {
                                        errorMsg = "Error de conexión. Verifica tu internet";
                                    }
                                    // Email mal formado
                                    else if (exceptionMsg.contains("badly formatted") ||
                                             exceptionMsg.contains("invalid-email")) {
                                        errorMsg = "El formato del email es inválido";
                                    }
                                    // Usuario deshabilitado
                                    else if (exceptionMsg.contains("disabled") ||
                                             exceptionMsg.contains("user-disabled")) {
                                        errorMsg = "Esta cuenta ha sido deshabilitada";
                                    }
                                    // Demasiados intentos
                                    else if (exceptionMsg.contains("too many") ||
                                             exceptionMsg.contains("TOO_MANY_ATTEMPTS")) {
                                        errorMsg = "Demasiados intentos fallidos. Intenta más tarde";
                                    }
                                    // Email o contraseña incorrectos (mensaje genérico de Firebase)
                                    else if (exceptionMsg.contains("INVALID_LOGIN_CREDENTIALS") ||
                                             exceptionMsg.contains("invalid-credential")) {
                                        errorMsg = "Email o contraseña incorrectos";
                                    }
                                }
                            }

                            Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();

                            // Limpiar campo de contraseña por seguridad
                            etPassword.setText("");
                            etPassword.requestFocus();
                        }
                    }
                });
    }

    private void verificarUsuarioYRedirigir(String uid) {
        db.collection("usuarios")
                .document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    mostrarCargando(false);

                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String estado = document.getString("estado");
                            String rol = document.getString("rol");

                            Log.d(TAG, "✅ Documento encontrado - Estado: " + estado + ", Rol: " + rol);

                            if ("activo".equals(estado)) {
                                Log.d(TAG, "✅ Usuario activo - Redirigiendo según rol");

                                // Solicitar permiso de notificaciones (Android 13+)
                                NotificationHelper.solicitarPermisoNotificaciones(this);


                                redirigirSegunRol(rol);
                            } else {
                                Toast.makeText(this,
                                        "Tu cuenta está inactiva. Contacta al administrador",
                                        Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                            }
                        } else {
                            Log.e(TAG, "❌ Documento no existe para UID: " + uid);
                            Toast.makeText(this,
                                    "No se encontró información del usuario",
                                    Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                        }
                    } else {
                        Log.e(TAG, "❌ Error al obtener datos del usuario", task.getException());
                        Toast.makeText(this,
                                "Error al verificar usuario: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void redirigirSegunRol(String rol) {
        Intent intent;

        if ("admin".equals(rol)) {
            Log.d(TAG, "🔀 Redirigiendo a Dashboard Admin");
            intent = new Intent(this, DashboardAdminActivity.class);
        } else if ("tecnico".equals(rol)) {
            Log.d(TAG, "🔀 Redirigiendo a Dashboard Técnico");
            intent = new Intent(this, DashboardTecnicoActivity.class);
        } else {
            Toast.makeText(this, "Rol no reconocido: " + rol, Toast.LENGTH_SHORT).show();
            mAuth.signOut();
            return;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ==================== CREDENCIALES ====================

    private void guardarCredenciales(String email, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_PASSWORD, password);
        editor.putBoolean(KEY_REMEMBER, true);
        editor.apply();

        Log.d(TAG, "✅ Credenciales guardadas");

        // Reconfigurar biométrico para que el botón aparezca si está disponible
        configurarBiometrico();
    }

    private void limpiarCredenciales() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_PASSWORD);
        editor.putBoolean(KEY_REMEMBER, false);
        editor.apply();

        Log.d(TAG, "🗑️ Credenciales eliminadas");

        // Ocultar botón biométrico ya que no hay credenciales guardadas
        btnBiometric.setVisibility(View.GONE);
    }

    // ==================== RECUPERAR PASSWORD ====================

    private void abrirRecuperarPassword() {
        Intent intent = new Intent(this, RecuperarPasswordActivity.class);
        startActivity(intent);
    }

    // ==================== UI ====================

    private void mostrarCargando(boolean mostrar) {
        progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!mostrar);
        btnBiometric.setEnabled(!mostrar);
        etEmail.setEnabled(!mostrar);
        etPassword.setEnabled(!mostrar);
        cbRecordarme.setEnabled(!mostrar);
    }
}