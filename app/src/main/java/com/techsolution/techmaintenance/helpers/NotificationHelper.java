package com.techsolution.techmaintenance.helpers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;


import java.util.HashMap;
import java.util.Map;

/**
 * Helper para gestionar notificaciones push con Firebase Cloud Messaging
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    private final FirebaseFirestore db;
    private final Context context;
    private final FirebaseFunctions functions;

    /**
     * Interfaz para callback de notificaciones
     */
    public interface NotificationCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public NotificationHelper(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.functions = FirebaseFunctions.getInstance();
    }



    /**
     * Envía notificación push al técnico cuando se le asigna un nuevo mantenimiento
     *
     * @param tecnicoId ID del técnico a notificar
     * @param mantenimientoId ID del mantenimiento asignado
     * @param equipoInfo Información del equipo (ej: "Laptop Dell Latitude 5420")
     * @param clienteInfo Información del cliente (ej: "Corporación XYZ")
     * @param fechaHora Fecha y hora programada
     * @param prioridad Prioridad del mantenimiento (baja/media/alta/urgente)
     */
    public void notificarNuevoMantenimiento(String tecnicoId, String mantenimientoId,
                                           String equipoInfo, String clienteInfo,
                                           String fechaHora, String prioridad) {
        Log.d(TAG, "🔔 Enviando notificación de nuevo mantenimiento:");
        Log.d(TAG, "   Técnico ID: " + tecnicoId);
        Log.d(TAG, "   Mantenimiento ID: " + mantenimientoId);
        Log.d(TAG, "   Equipo: " + equipoInfo);
        Log.d(TAG, "   Cliente: " + clienteInfo);
        Log.d(TAG, "   Fecha/Hora: " + fechaHora);
        Log.d(TAG, "   Prioridad: " + prioridad);

        // Llamar a Cloud Function para enviar notificación
        Map<String, Object> data = new HashMap<>();
        data.put("tecnicoId", tecnicoId);
        data.put("mantenimientoId", mantenimientoId);
        data.put("equipoInfo", equipoInfo);
        data.put("clienteInfo", clienteInfo);
        data.put("fechaHora", fechaHora);
        data.put("prioridad", prioridad);

        functions.getHttpsCallable("enviarNotificacionNuevoMantenimiento")
                .call(data)
                .addOnSuccessListener(result -> {
                    Log.d(TAG, "✅ Notificación enviada exitosamente");
                    Log.d(TAG, "   Resultado: " + result.getData());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error enviando notificación: " + e.getMessage());
                    e.printStackTrace();
                });
    }

    /**
     * Envía notificación al admin cuando un técnico completa un servicio
     *
     * @param mantenimientoId ID del mantenimiento completado
     * @param tecnicoNombre Nombre del técnico que completó
     * @param equipoInfo Información del equipo
     * @param clienteInfo Información del cliente
     */
    public void notificarServicioCompletado(String mantenimientoId, String tecnicoNombre,
                                           String equipoInfo, String clienteInfo) {
        notificarServicioCompletado(mantenimientoId, tecnicoNombre, equipoInfo, clienteInfo, null);
    }

    /**
     * Envía notificación al admin cuando un técnico completa un servicio (con callback)
     *
     * @param mantenimientoId ID del mantenimiento completado
     * @param tecnicoNombre Nombre del técnico que completó
     * @param equipoInfo Información del equipo
     * @param clienteInfo Información del cliente
     * @param callback Callback para manejar resultado
     */
    public void notificarServicioCompletado(String mantenimientoId, String tecnicoNombre,
                                           String equipoInfo, String clienteInfo, NotificationCallback callback) {
        Log.d(TAG, "🔔 ========================================");
        Log.d(TAG, "🔔 ENVIANDO NOTIFICACIÓN DE SERVICIO COMPLETADO");
        Log.d(TAG, "🔔 ========================================");
        Log.d(TAG, "   Mantenimiento ID: " + mantenimientoId);
        Log.d(TAG, "   Técnico: " + tecnicoNombre);
        Log.d(TAG, "   Equipo: " + equipoInfo);
        Log.d(TAG, "   Cliente: " + clienteInfo);

        // Llamar a Cloud Function para enviar notificación
        Map<String, Object> data = new HashMap<>();
        data.put("mantenimientoId", mantenimientoId);
        data.put("tecnicoNombre", tecnicoNombre);
        data.put("equipoInfo", equipoInfo);
        data.put("clienteInfo", clienteInfo);

        Log.d(TAG, "📡 Llamando a Cloud Function: enviarNotificacionServicioCompletado");

        functions.getHttpsCallable("enviarNotificacionServicioCompletado")
                .call(data)
                .addOnSuccessListener(result -> {
                    Log.d(TAG, "✅ ========================================");
                    Log.d(TAG, "✅ NOTIFICACIÓN ENVIADA EXITOSAMENTE");
                    Log.d(TAG, "✅ ========================================");

                    Map<String, Object> resultData = (Map<String, Object>) result.getData();
                    if (resultData != null) {
                        Log.d(TAG, "   Resultado completo: " + resultData);
                        Log.d(TAG, "   - Success: " + resultData.get("success"));
                        Log.d(TAG, "   - Message: " + resultData.get("message"));
                        Log.d(TAG, "   - Notificaciones enviadas: " + resultData.get("notificacionesEnviadas"));
                    }

                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ ========================================");
                    Log.e(TAG, "❌ ERROR AL ENVIAR NOTIFICACIÓN");
                    Log.e(TAG, "❌ ========================================");
                    Log.e(TAG, "   Tipo de error: " + e.getClass().getSimpleName());
                    Log.e(TAG, "   Mensaje: " + e.getMessage());
                    e.printStackTrace();

                    if (callback != null) {
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * Solicita permiso de notificaciones al usuario (Android 13+)
     */
    public static void solicitarPermisoNotificaciones(android.app.Activity activity) {
        Log.d(TAG, "📱 solicitarPermisoNotificaciones() llamado");
        Log.d(TAG, "📱 SDK Version: " + android.os.Build.VERSION.SDK_INT);
        Log.d(TAG, "📱 Android Version: " + android.os.Build.VERSION.RELEASE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "📱 Android 13+, verificando permisos...");

            int permissionStatus = androidx.core.content.ContextCompat.checkSelfPermission(
                    activity, android.Manifest.permission.POST_NOTIFICATIONS);

            Log.d(TAG, "📱 Permission Status: " + permissionStatus + " (GRANTED=" +
                  android.content.pm.PackageManager.PERMISSION_GRANTED + ")");

            if (permissionStatus != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "⚠️ Permiso NO concedido, solicitando...");

                // Verificar si debemos mostrar una explicación
                boolean shouldShow = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                        activity, android.Manifest.permission.POST_NOTIFICATIONS);

                Log.d(TAG, "📱 shouldShowRequestPermissionRationale: " + shouldShow);

                if (shouldShow) {
                    Log.d(TAG, "📱 Mostrando diálogo explicativo (usuario rechazó antes)");

                    // El usuario rechazó el permiso anteriormente, mostrar diálogo explicativo
                    new android.app.AlertDialog.Builder(activity)
                            .setTitle("Permisos de Notificaciones")
                            .setMessage("Esta aplicación necesita permiso para enviarte notificaciones sobre tus mantenimientos asignados. " +
                                       "Por favor, activa las notificaciones en la configuración de la aplicación.")
                            .setPositiveButton("Ir a Configuración", (dialog, which) -> {
                                Log.d(TAG, "📱 Usuario eligió ir a configuración");
                                // Abrir configuración de la app
                                android.content.Intent intent = new android.content.Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                android.net.Uri uri = android.net.Uri.fromParts("package",
                                        activity.getPackageName(), null);
                                intent.setData(uri);
                                activity.startActivity(intent);
                            })
                            .setNegativeButton("Ahora no", (dialog, which) -> {
                                Log.d(TAG, "📱 Usuario rechazó ir a configuración");
                            })
                            .show();
                } else {
                    Log.d(TAG, "📱 Solicitando permiso directamente (primera vez)");

                    // Primera vez, solicitar permiso directamente
                    androidx.core.app.ActivityCompat.requestPermissions(
                            activity,
                            new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                            1001
                    );

                    Log.d(TAG, "📱 Permiso solicitado, esperando respuesta del usuario...");
                }
            } else {
                Log.d(TAG, "✅ Permiso de notificaciones YA CONCEDIDO");
            }
        } else {
            Log.d(TAG, "✅ Android < 13 (API " + android.os.Build.VERSION.SDK_INT +
                  "), permiso de notificaciones no requerido");
        }
    }
}
