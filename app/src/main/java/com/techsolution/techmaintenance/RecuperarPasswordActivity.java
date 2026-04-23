package com.techsolution.techmaintenance;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.functions.FirebaseFunctions;
import com.techsolution.techmaintenance.activities.LoginActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RecuperarPasswordActivity extends AppCompatActivity {

    private static final String TAG = "RecuperarPassword";

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseFunctions functions;

    // Vistas - Paso 1: Email
    private LinearLayout layoutPaso1;
    private EditText etEmail;
    private Button btnEnviarCodigo;
    private ProgressBar progressBarPaso1;

    // Vistas - Paso 2: Código
    private LinearLayout layoutPaso2;
    private EditText etCodigo;
    private Button btnVerificarCodigo;
    private Button btnReenviarCodigo;
    private TextView tvTiempoRestante;
    private ProgressBar progressBarPaso2;

    // Vistas - Paso 3: Nueva contraseña
    private LinearLayout layoutPaso3;
    private EditText etNuevaPassword;
    private EditText etConfirmarPassword;
    private Button btnCambiarPassword;
    private ProgressBar progressBarPaso3;

    // Variables de control
    private String emailUsuario;
    private String codigoGenerado;
    private String userIdRecuperar;
    private CountDownTimer countDownTimer;
    private boolean puedeReenviar = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar_password);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Desactivar cache para forzar consulta directa al servidor
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build();
        db.setFirestoreSettings(settings);

        functions = FirebaseFunctions.getInstance("us-central1");

        // Inicializar vistas
        inicializarVistas();

        // Configurar listeners
        configurarListeners();

        // Mostrar solo el primer paso
        mostrarPaso(1);
    }

    private void inicializarVistas() {
        // Paso 1
        layoutPaso1 = findViewById(R.id.layoutPaso1);
        etEmail = findViewById(R.id.etEmail);
        btnEnviarCodigo = findViewById(R.id.btnEnviarCodigo);
        progressBarPaso1 = findViewById(R.id.progressBarPaso1);

        // Paso 2
        layoutPaso2 = findViewById(R.id.layoutPaso2);
        etCodigo = findViewById(R.id.etCodigo);
        btnVerificarCodigo = findViewById(R.id.btnVerificarCodigo);
        btnReenviarCodigo = findViewById(R.id.btnReenviarCodigo);
        tvTiempoRestante = findViewById(R.id.tvTiempoRestante);
        progressBarPaso2 = findViewById(R.id.progressBarPaso2);

        // Paso 3
        layoutPaso3 = findViewById(R.id.layoutPaso3);
        etNuevaPassword = findViewById(R.id.etNuevaPassword);
        etConfirmarPassword = findViewById(R.id.etConfirmarPassword);
        btnCambiarPassword = findViewById(R.id.btnCambiarPassword);
        progressBarPaso3 = findViewById(R.id.progressBarPaso3);
    }

    private void configurarListeners() {
        btnEnviarCodigo.setOnClickListener(v -> enviarCodigoRecuperacion());
        btnVerificarCodigo.setOnClickListener(v -> verificarCodigo());
        btnReenviarCodigo.setOnClickListener(v -> reenviarCodigo());
        btnCambiarPassword.setOnClickListener(v -> cambiarPassword());

        // Botón volver
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());
    }

    private void mostrarPaso(int paso) {
        layoutPaso1.setVisibility(paso == 1 ? View.VISIBLE : View.GONE);
        layoutPaso2.setVisibility(paso == 2 ? View.VISIBLE : View.GONE);
        layoutPaso3.setVisibility(paso == 3 ? View.VISIBLE : View.GONE);
    }

    // ==================== PASO 1: ENVIAR CÓDIGO ====================

    private void enviarCodigoRecuperacion() {
        emailUsuario = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(emailUsuario)) {
            etEmail.setError("Ingresa tu email");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailUsuario).matches()) {
            etEmail.setError("Email inválido");
            etEmail.requestFocus();
            return;
        }

        mostrarCargando(1, true);

        // ✅ Verificar si el email existe en Firestore (no en Auth)
        db.collection("usuarios")
                .whereEqualTo("email", emailUsuario)
                .whereEqualTo("estado", "activo")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        mostrarCargando(1, false);
                        Toast.makeText(this,
                                "No existe una cuenta activa con este email",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Guardar el userId
                    userIdRecuperar = queryDocumentSnapshots.getDocuments().get(0).getId();

                    // Generar código de 6 dígitos
                    codigoGenerado = generarCodigoAleatorio();

                    // Guardar código en Firestore
                    guardarCodigoEnFirestore();
                })
                .addOnFailureListener(e -> {
                    mostrarCargando(1, false);
                    Log.e(TAG, "Error al verificar email", e);
                    Toast.makeText(this,
                            "Error de conexión. Verifica tu internet e intenta de nuevo",
                            Toast.LENGTH_LONG).show();
                });
    }

    private String generarCodigoAleatorio() {
        Random random = new Random();
        int codigo = 100000 + random.nextInt(900000);
        return String.valueOf(codigo);
    }

    private void guardarCodigoEnFirestore() {
        Map<String, Object> codigoData = new HashMap<>();
        codigoData.put("email", emailUsuario);
        codigoData.put("userId", userIdRecuperar);
        codigoData.put("codigo", codigoGenerado);
        codigoData.put("fechaCreacion", System.currentTimeMillis());
        codigoData.put("expiraEn", System.currentTimeMillis() + (15 * 60 * 1000)); // 15 minutos
        codigoData.put("usado", false);

        db.collection("codigos_recuperacion")
                .document(emailUsuario)
                .set(codigoData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Código guardado en Firestore");

                    // Enviar email con el código
                    enviarEmailConCodigo(emailUsuario, codigoGenerado);
                })
                .addOnFailureListener(e -> {
                    mostrarCargando(1, false);
                    Log.e(TAG, "Error al guardar código", e);
                    Toast.makeText(this,
                            "Error al generar código. Intenta de nuevo",
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ==================== ENVÍO DE EMAIL CON CLOUD FUNCTIONS ====================

    private void enviarEmailConCodigo(String email, String codigo) {
        Log.d(TAG, "=== Enviando email ===");
        Log.d(TAG, "Email: " + email);
        Log.d(TAG, "Código: " + codigo);

        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("codigo", codigo);

        functions
                .getHttpsCallable("enviarCodigoRecuperacion")
                .call(data)
                .addOnSuccessListener(httpsCallableResult -> {
                    mostrarCargando(1, false);

                    Log.d(TAG, "✅ Email enviado correctamente");

                    Toast.makeText(this,
                            "📧 Código enviado a tu email\nRevisa tu bandeja de entrada",
                            Toast.LENGTH_LONG).show();

                    // Pasar al paso 2
                    mostrarPaso(2);
                    iniciarTemporizador();
                })
                .addOnFailureListener(e -> {
                    mostrarCargando(1, false);

                    Log.e(TAG, "❌ Error al enviar email: " + e.getMessage());

                    // Aunque falle el email, el código ya está guardado
                    Toast.makeText(this,
                            "⚠️ Código generado pero hubo un problema al enviar el email\n" +
                                    "Código: " + codigo + "\nUsa este código para continuar",
                            Toast.LENGTH_LONG).show();

                    // Permitir continuar de todas formas
                    mostrarPaso(2);
                    iniciarTemporizador();
                });
    }

    // ==================== PASO 2: VERIFICAR CÓDIGO ====================

    private void iniciarTemporizador() {
        puedeReenviar = false;
        btnReenviarCodigo.setEnabled(false);

        countDownTimer = new CountDownTimer(120000, 1000) { // 2 minutos
            @Override
            public void onTick(long millisUntilFinished) {
                long segundos = millisUntilFinished / 1000;
                long minutos = segundos / 60;
                segundos = segundos % 60;
                tvTiempoRestante.setText(String.format("Podrás reenviar en %02d:%02d", minutos, segundos));
            }

            @Override
            public void onFinish() {
                tvTiempoRestante.setText("Puedes reenviar el código");
                btnReenviarCodigo.setEnabled(true);
                puedeReenviar = true;
            }
        }.start();
    }

    private void verificarCodigo() {
        String codigoIngresado = etCodigo.getText().toString().trim();

        if (TextUtils.isEmpty(codigoIngresado)) {
            etCodigo.setError("Ingresa el código");
            etCodigo.requestFocus();
            return;
        }

        if (codigoIngresado.length() != 6) {
            etCodigo.setError("El código debe tener 6 dígitos");
            etCodigo.requestFocus();
            return;
        }

        mostrarCargando(2, true);

        // Verificar código en Firestore
        db.collection("codigos_recuperacion")
                .document(emailUsuario)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    mostrarCargando(2, false);

                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Código no encontrado", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String codigoGuardado = documentSnapshot.getString("codigo");
                    Long expiraEn = documentSnapshot.getLong("expiraEn");
                    Boolean usado = documentSnapshot.getBoolean("usado");

                    // Validaciones
                    if (usado != null && usado) {
                        Toast.makeText(this, "Este código ya fue utilizado", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (expiraEn != null && System.currentTimeMillis() > expiraEn) {
                        Toast.makeText(this, "El código ha expirado (válido por 15 minutos)", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!codigoIngresado.equals(codigoGuardado)) {
                        Toast.makeText(this, "Código incorrecto", Toast.LENGTH_SHORT).show();
                        etCodigo.setError("Código incorrecto");
                        return;
                    }

                    // ✅ Código correcto
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }

                    Toast.makeText(this, "✅ Código verificado correctamente", Toast.LENGTH_SHORT).show();
                    mostrarPaso(3);
                })
                .addOnFailureListener(e -> {
                    mostrarCargando(2, false);
                    Log.e(TAG, "Error al verificar código", e);
                    Toast.makeText(this, "Error de conexión al verificar código", Toast.LENGTH_SHORT).show();
                });
    }

    private void reenviarCodigo() {
        if (!puedeReenviar) {
            Toast.makeText(this, "⏱️ Espera un momento antes de reenviar", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generar nuevo código
        codigoGenerado = generarCodigoAleatorio();

        // Guardar nuevo código
        guardarCodigoEnFirestore();

        // Reiniciar temporizador
        iniciarTemporizador();

        // Limpiar campo
        etCodigo.setText("");
    }

    // ==================== PASO 3: CAMBIAR CONTRASEÑA ====================

    private void cambiarPassword() {
        String nuevaPassword = etNuevaPassword.getText().toString().trim();
        String confirmarPassword = etConfirmarPassword.getText().toString().trim();

        // Validaciones
        if (TextUtils.isEmpty(nuevaPassword)) {
            etNuevaPassword.setError("Ingresa la nueva contraseña");
            etNuevaPassword.requestFocus();
            return;
        }

        if (nuevaPassword.length() < 6) {
            etNuevaPassword.setError("Mínimo 6 caracteres");
            etNuevaPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmarPassword)) {
            etConfirmarPassword.setError("Confirma la contraseña");
            etConfirmarPassword.requestFocus();
            return;
        }

        if (!nuevaPassword.equals(confirmarPassword)) {
            etConfirmarPassword.setError("Las contraseñas no coinciden");
            etConfirmarPassword.requestFocus();
            return;
        }

        mostrarCargando(3, true);
        cambiarPasswordConCloudFunction(nuevaPassword);
    }

    private void cambiarPasswordConCloudFunction(String nuevaPassword) {
        Map<String, Object> data = new HashMap<>();
        data.put("email", emailUsuario);
        data.put("nuevaPassword", nuevaPassword);
        data.put("codigo", codigoGenerado);

        functions
                .getHttpsCallable("cambiarPasswordRecuperacion")
                .call(data)
                .addOnSuccessListener(httpsCallableResult -> {
                    mostrarCargando(3, false);

                    new AlertDialog.Builder(this)
                            .setTitle("✅ Contraseña Actualizada")
                            .setMessage("Tu contraseña ha sido cambiada exitosamente.\n\n" +
                                    "Ya puedes iniciar sesión con tu nueva contraseña.")
                            .setPositiveButton("Ir al Login", (dialog, which) -> {
                                Intent intent = new Intent(RecuperarPasswordActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .setCancelable(false)
                            .show();
                })
                .addOnFailureListener(e -> {
                    mostrarCargando(3, false);
                    Log.e(TAG, "Error: " + e.getMessage());
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void mostrarCargando(int paso, boolean mostrar) {
        switch (paso) {
            case 1:
                progressBarPaso1.setVisibility(mostrar ? View.VISIBLE : View.GONE);
                btnEnviarCodigo.setEnabled(!mostrar);
                etEmail.setEnabled(!mostrar);
                break;
            case 2:
                progressBarPaso2.setVisibility(mostrar ? View.VISIBLE : View.GONE);
                btnVerificarCodigo.setEnabled(!mostrar);
                btnReenviarCodigo.setEnabled(!mostrar && puedeReenviar);
                etCodigo.setEnabled(!mostrar);
                break;
            case 3:
                progressBarPaso3.setVisibility(mostrar ? View.VISIBLE : View.GONE);
                btnCambiarPassword.setEnabled(!mostrar);
                etNuevaPassword.setEnabled(!mostrar);
                etConfirmarPassword.setEnabled(!mostrar);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
