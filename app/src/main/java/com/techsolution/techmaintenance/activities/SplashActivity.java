package com.techsolution.techmaintenance.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techsolution.techmaintenance.R;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 segundos
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Launcher para solicitar permisos de notificaciones
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // Variables para almacenar el destino después de la verificación de permisos
    private String nextDestination = "LOGIN"; // LOGIN, ADMIN, TECNICO

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Respetar el tema del sistema (claro/oscuro)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Pantalla completa
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Inicializar el launcher para permisos de notificaciones
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    // El permiso fue concedido o denegado, continuar con la navegación
                    // No es necesario bloquear si se deniega, las notificaciones simplemente no funcionarán
                    proceedToNextScreen();
                }
        );

        // Esperar 2 segundos y verificar sesión
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkUserSession();
            }
        }, SPLASH_DURATION);
    }

    private void checkUserSession() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // Usuario ya tiene sesión activa, verificar su rol
            db.collection("usuarios").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String rol = documentSnapshot.getString("rol");
                            String estado = documentSnapshot.getString("estado");

                            // Verificar que el usuario esté activo
                            if ("activo".equals(estado)) {
                                // Establecer destino según el rol
                                if ("admin".equals(rol)) {
                                    nextDestination = "ADMIN";
                                } else if ("tecnico".equals(rol)) {
                                    nextDestination = "TECNICO";
                                } else {
                                    nextDestination = "LOGIN";
                                }
                            } else {
                                // Usuario inactivo, cerrar sesión y redirigir a login
                                mAuth.signOut();
                                nextDestination = "LOGIN";
                            }
                        } else {
                            // Usuario no existe en Firestore, cerrar sesión
                            mAuth.signOut();
                            nextDestination = "LOGIN";
                        }
                        // Verificar y solicitar permisos de notificaciones
                        checkNotificationPermission();
                    })
                    .addOnFailureListener(e -> {
                        // Error al obtener datos, ir al login
                        nextDestination = "LOGIN";
                        checkNotificationPermission();
                    });
        } else {
            // No hay sesión, ir al login
            nextDestination = "LOGIN";
            checkNotificationPermission();
        }
    }

    /**
     * Verifica si se necesita solicitar permisos de notificaciones (Android 13+)
     * Si no es necesario, procede directamente a la siguiente pantalla
     */
    private void checkNotificationPermission() {
        // Solo solicitar permisos en Android 13 (API 33) o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Solicitar permiso
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                // Permiso ya concedido, continuar
                proceedToNextScreen();
            }
        } else {
            // Android 12 o inferior, no requiere permiso en tiempo de ejecución
            proceedToNextScreen();
        }
    }

    /**
     * Navega a la pantalla correspondiente según el destino establecido
     */
    private void proceedToNextScreen() {
        switch (nextDestination) {
            case "ADMIN":
                goToDashboardAdmin();
                break;
            case "TECNICO":
                goToDashboardTecnico();
                break;
            case "LOGIN":
            default:
                goToLogin();
                break;
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToDashboardAdmin() {
        Intent intent = new Intent(SplashActivity.this, DashboardAdminActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToDashboardTecnico() {
        Intent intent = new Intent(SplashActivity.this, DashboardTecnicoActivity.class);
        startActivity(intent);
        finish();
    }
}