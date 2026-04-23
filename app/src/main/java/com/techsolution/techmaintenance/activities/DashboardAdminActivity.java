package com.techsolution.techmaintenance.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.adapters.MantenimientoAdapter;
import com.techsolution.techmaintenance.helpers.FirestoreHelper;
import com.techsolution.techmaintenance.models.Mantenimiento;

import java.util.ArrayList;
import java.util.List;

public class DashboardAdminActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Vistas - Cards de Métricas
    private TextView tvCompletados, tvEnProceso, tvPendientes, tvCalificacion;

    // Vistas - Estadísticas Rápidas
    private TextView tvTecnicosActivos, tvEquiposTotal, tvClientesTotal;

    // Vistas - Lista de Mantenimientos
    private RecyclerView recyclerMantenimientos;
    private MantenimientoAdapter adapter;
    private List<Mantenimiento> listaMantenimientos;
    private ProgressBar progressBar;
    private TextView tvSinDatos;

    // Otras vistas
    private LinearLayout btnMenu;
    private ImageView imgPerfil;
    private ImageButton btnCalendario;
    private ImageButton btnNotificaciones;
    private TextView badgeNotificaciones;
    private TextView tvVerTodos;
    private BottomNavigationView bottomNavigation;
    private SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_admin);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Verificar autenticación
        if (currentUser == null) {
            // Redirigir al login si no hay usuario
            finish();
            return;
        }

        // Inicializar vistas
        inicializarVistas();

        // Configurar listeners
        configurarListeners();

        // Configurar RecyclerView
        configurarRecyclerView();

        // Cargar todos los datos
        cargarDatos();
    }

    private void inicializarVistas() {
        // Toolbar
        btnMenu = findViewById(R.id.btnMenu);
        imgPerfil = findViewById(R.id.imgPerfil);
        btnCalendario = findViewById(R.id.btnCalendario);
        btnNotificaciones = findViewById(R.id.btnNotificaciones);
        badgeNotificaciones = findViewById(R.id.badgeNotificaciones);

        // Cards de métricas
        tvCompletados = findViewById(R.id.tvCompletados);
        tvEnProceso = findViewById(R.id.tvEnProceso);
        tvPendientes = findViewById(R.id.tvPendientes);
        tvCalificacion = findViewById(R.id.tvCalificacion);

        // Estadísticas rápidas
        tvTecnicosActivos = findViewById(R.id.tvTecnicosActivos);
        tvEquiposTotal = findViewById(R.id.tvEquiposTotal);
        tvClientesTotal = findViewById(R.id.tvClientesTotal);

        // RecyclerView y estados
        recyclerMantenimientos = findViewById(R.id.recyclerMantenimientos);
        progressBar = findViewById(R.id.progressBar);
        tvSinDatos = findViewById(R.id.tvSinDatos);
        tvVerTodos = findViewById(R.id.tvVerTodos);

        // SwipeRefresh
        swipeRefresh = findViewById(R.id.swipeRefresh);

        // Bottom Navigation
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void configurarListeners() {
        // Click en menú - Abrir administración de técnicos
        btnMenu.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdministrarTecnicosActivity.class);
            startActivity(intent);
        });

        // Click en perfil
        imgPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(this, PerfilAdminActivity.class);
            startActivity(intent);
        });

        // Click en calendario
        btnCalendario.setOnClickListener(v -> {
            Intent intent = new Intent(this, CalendarioActivity.class);
            startActivity(intent);
        });

        // Click en notificaciones
        btnNotificaciones.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificacionesActivity.class);
            startActivity(intent);
        });

        // Click en "Ver todos" - Navegar a lista completa de mantenimientos
        tvVerTodos.setOnClickListener(v -> {
            Intent intent = new Intent(this, ListaMantenimientosActivity.class);
            startActivity(intent);
        });

        // SwipeRefresh - Recargar datos al deslizar hacia abajo
        swipeRefresh.setOnRefreshListener(() -> {
            cargarDatos();
            swipeRefresh.setRefreshing(false);
        });

        // ✅ CORRECCIÓN: Bottom Navigation con rutas correctas
        bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                // Ya estamos en Dashboard, no hacer nada
                return true;

            } else if (itemId == R.id.nav_clientes) {
                // Ir a Lista de Clientes
                Intent intent = new Intent(this, ListaClientesActivity.class);
                startActivity(intent);
                return true;

            } else if (itemId == R.id.nav_equipos) {
                // ⭐ Ir a Lista de Equipos (LO QUE ACABAMOS DE CREAR)
                Intent intent = new Intent(this, ListaEquiposActivity.class);
                startActivity(intent);
                return true;

            } else if (itemId == R.id.nav_mantenimientos) {
                // Ir a Lista de Mantenimientos
                Intent intent = new Intent(this, ListaMantenimientosActivity.class);
                startActivity(intent);
                return true;

            } else if (itemId == R.id.nav_reportes) {
                // Ir a ReportesActivity
                Intent intent = new Intent(this, ReportesActivity.class);
                startActivity(intent);
                return true;
            }

            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Asegurar que el item correcto esté seleccionado
        bottomNavigation.setSelectedItemId(R.id.nav_dashboard);

        // IMPORTANTE: Recargar todos los datos cuando volvemos al Dashboard
        // Esto asegura que las estadísticas estén actualizadas después de agregar
        // clientes, equipos o técnicos
        cargarDatos();
    }
    private void configurarRecyclerView() {
        listaMantenimientos = new ArrayList<>();
        adapter = MantenimientoAdapter.createLegacy(this, listaMantenimientos);
        recyclerMantenimientos.setLayoutManager(new LinearLayoutManager(this));
        recyclerMantenimientos.setAdapter(adapter);
        recyclerMantenimientos.setNestedScrollingEnabled(false);
    }

    private void cargarDatos() {
        android.util.Log.d("DashboardAdmin", "📊 Iniciando carga de datos del dashboard...");
        cargarFotoPerfil();
        cargarMetricas();
        cargarEstadisticasRapidas();
        cargarProximosMantenimientos();
        cargarContadorNotificaciones();
    }

    // ==================== CARGAR FOTO DE PERFIL ====================

    private void cargarFotoPerfil() {
        if (currentUser == null) {
            android.util.Log.w("DashboardAdmin", "⚠️ No hay usuario autenticado");
            return;
        }

        String adminId = currentUser.getUid();

        db.collection("usuarios").document(adminId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Cargar foto de perfil si existe
                        String fotoPerfilURL = documentSnapshot.getString("fotoPerfilURL");
                        if (fotoPerfilURL != null && !fotoPerfilURL.isEmpty()) {
                            com.bumptech.glide.Glide.with(DashboardAdminActivity.this)
                                    .load(fotoPerfilURL)
                                    .placeholder(R.drawable.ic_person)
                                    .error(R.drawable.ic_person)
                                    .circleCrop()
                                    .into(imgPerfil);
                            android.util.Log.d("DashboardAdmin", "✅ Foto de perfil cargada");
                        } else {
                            android.util.Log.d("DashboardAdmin", "ℹ️ No hay URL de foto de perfil");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DashboardAdmin", "❌ Error al cargar foto de perfil: " + e.getMessage());
                });
    }

    // ==================== CARGAR MÉTRICAS ====================

    private void cargarMetricas() {
        cargarMantenimientosCompletados();
        cargarMantenimientosEnProceso();
        cargarMantenimientosPendientes();
        cargarCalificacionPromedio();
    }

    // Mantenimientos completados este mes
    private void cargarMantenimientosCompletados() {
        Timestamp inicioMes = FirestoreHelper.getInicioMesActual();
        Timestamp finMes = FirestoreHelper.getFinMesActual();

        android.util.Log.d("DashboardAdmin", "📊 Cargando completados del mes: " + inicioMes.toDate() + " - " + finMes.toDate());

        db.collection("mantenimientos")
                .whereEqualTo("estado", "completado")
                .whereGreaterThanOrEqualTo("fechaFinalizacion", inicioMes)
                .whereLessThanOrEqualTo("fechaFinalizacion", finMes)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int total = queryDocumentSnapshots.size();
                    tvCompletados.setText(String.valueOf(total));
                    android.util.Log.d("DashboardAdmin", "✅ Completados del mes: " + total);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DashboardAdmin", "❌ Error al cargar completados: " + e.getMessage());
                    tvCompletados.setText("0");
                });
    }

    // Mantenimientos en proceso hoy
    private void cargarMantenimientosEnProceso() {
        android.util.Log.d("DashboardAdmin", "📊 Cargando mantenimientos en proceso...");

        db.collection("mantenimientos")
                .whereEqualTo("estado", "en_proceso")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int total = queryDocumentSnapshots.size();
                    tvEnProceso.setText(String.valueOf(total));
                    android.util.Log.d("DashboardAdmin", "✅ Mantenimientos en proceso: " + total);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DashboardAdmin", "❌ Error al cargar mantenimientos en proceso: " + e.getMessage());
                    tvEnProceso.setText("0");
                });
    }

    // Mantenimientos pendientes esta semana
    private void cargarMantenimientosPendientes() {
        Timestamp inicioSemana = FirestoreHelper.getInicioSemanaActual();
        Timestamp finSemana = FirestoreHelper.getFinSemanaActual();

        db.collection("mantenimientos")
                .whereEqualTo("estado", "programado")
                .whereGreaterThanOrEqualTo("fechaProgramada", inicioSemana)
                .whereLessThanOrEqualTo("fechaProgramada", finSemana)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int total = queryDocumentSnapshots.size();
                    tvPendientes.setText(String.valueOf(total));
                })
                .addOnFailureListener(e -> {
                    tvPendientes.setText("0");
                });
    }

    // Calificación promedio
    private void cargarCalificacionPromedio() {
        android.util.Log.d("DashboardAdmin", "📊 Cargando calificación promedio...");

        db.collection("mantenimientos")
                .whereEqualTo("estado", "completado")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        tvCalificacion.setText("0.0");
                        android.util.Log.d("DashboardAdmin", "ℹ️ No hay mantenimientos completados");
                        return;
                    }

                    double sumaCalificaciones = 0;
                    int countCalificaciones = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Long calificacion = doc.getLong("calificacionCliente");
                        if (calificacion != null && calificacion > 0) {
                            sumaCalificaciones += calificacion;
                            countCalificaciones++;
                        }
                    }

                    if (countCalificaciones > 0) {
                        double promedio = sumaCalificaciones / countCalificaciones;
                        tvCalificacion.setText(String.format(java.util.Locale.getDefault(), "%.1f", promedio));
                        android.util.Log.d("DashboardAdmin", "✅ Calificación promedio: " + promedio + " (" + countCalificaciones + " calificaciones)");
                    } else {
                        tvCalificacion.setText("0.0");
                        android.util.Log.d("DashboardAdmin", "ℹ️ No hay calificaciones disponibles");
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DashboardAdmin", "❌ Error al cargar calificación promedio: " + e.getMessage());
                    tvCalificacion.setText("0.0");
                });
    }

    // ==================== CARGAR ESTADÍSTICAS RÁPIDAS ====================

    private void cargarEstadisticasRapidas() {
        cargarTecnicosActivos();
        cargarEquiposTotal();
        cargarClientesTotal();
    }

    // Técnicos activos
    private void cargarTecnicosActivos() {
        db.collection("usuarios")
                .whereEqualTo("rol", "tecnico")
                .whereEqualTo("estado", "activo")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int total = queryDocumentSnapshots.size();
                    tvTecnicosActivos.setText(String.valueOf(total));
                })
                .addOnFailureListener(e -> {
                    tvTecnicosActivos.setText("0");
                });
    }

    // Equipos totales
    private void cargarEquiposTotal() {
        db.collection("equipos")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int total = queryDocumentSnapshots.size();
                    tvEquiposTotal.setText(String.valueOf(total));
                })
                .addOnFailureListener(e -> {
                    tvEquiposTotal.setText("0");
                });
    }

    // Clientes totales
    private void cargarClientesTotal() {
        db.collection("clientes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int total = queryDocumentSnapshots.size();
                    tvClientesTotal.setText(String.valueOf(total));
                })
                .addOnFailureListener(e -> {
                    tvClientesTotal.setText("0");
                });
    }

    // ==================== CARGAR PRÓXIMOS MANTENIMIENTOS ====================

    private void cargarProximosMantenimientos() {
        android.util.Log.d("DashboardAdmin", "🚀 INICIANDO cargarProximosMantenimientos()");

        progressBar.setVisibility(View.VISIBLE);
        recyclerMantenimientos.setVisibility(View.GONE);
        tvSinDatos.setVisibility(View.GONE);

        // IMPORTANTE: Usar el inicio del día actual para incluir TODOS los mantenimientos de hoy
        // (no solo los que están después de la hora actual)
        Timestamp inicioDiaActual = FirestoreHelper.getInicioDiaActual();

        android.util.Log.d("DashboardAdmin", "🔍 Buscando próximos mantenimientos desde: " + inicioDiaActual.toDate());
        android.util.Log.d("DashboardAdmin", "   (Incluyendo todos los de hoy, ordenados por fecha y hora)");

        db.collection("mantenimientos")
                .whereEqualTo("estado", "programado")
                .whereGreaterThanOrEqualTo("fechaProgramada", inicioDiaActual)
                .orderBy("fechaProgramada", Query.Direction.ASCENDING) // Ordena por fecha Y hora (más cercanos primero)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    android.util.Log.d("DashboardAdmin", "✅ Query completada. Documentos encontrados: " + queryDocumentSnapshots.size());

                    progressBar.setVisibility(View.GONE);

                    if (queryDocumentSnapshots.isEmpty()) {
                        android.util.Log.d("DashboardAdmin", "ℹ️ No hay próximos mantenimientos programados");
                        tvSinDatos.setVisibility(View.VISIBLE);
                        recyclerMantenimientos.setVisibility(View.GONE);
                        listaMantenimientos.clear();
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    listaMantenimientos.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Mantenimiento mantenimiento = doc.toObject(Mantenimiento.class);
                        mantenimiento.setMantenimientoId(doc.getId());
                        listaMantenimientos.add(mantenimiento);
                        android.util.Log.d("DashboardAdmin", "📅 Mantenimiento: " + doc.getId() +
                            " - Fecha: " + mantenimiento.getFechaProgramada().toDate() +
                            " - Estado: " + mantenimiento.getEstado());
                    }

                    android.util.Log.d("DashboardAdmin", "📋 Total mantenimientos en lista: " + listaMantenimientos.size());
                    android.util.Log.d("DashboardAdmin", "🔄 Llamando adapter.updateData()");
                    adapter.updateData(listaMantenimientos);

                    android.util.Log.d("DashboardAdmin", "👁️ Mostrando RecyclerView (setVisibility(View.VISIBLE))");
                    recyclerMantenimientos.setVisibility(View.VISIBLE);

                    android.util.Log.d("DashboardAdmin", "✅ RecyclerView visible: " + (recyclerMantenimientos.getVisibility() == View.VISIBLE));
                    android.util.Log.d("DashboardAdmin", "✅ Adapter item count DESPUÉS de updateData: " + adapter.getItemCount());
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvSinDatos.setVisibility(View.VISIBLE);
                    recyclerMantenimientos.setVisibility(View.GONE);
                    android.util.Log.e("DashboardAdmin", "❌ Error al cargar próximos mantenimientos: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar mantenimientos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ==================== CARGAR CONTADOR DE NOTIFICACIONES ====================

    /**
     * Carga el contador de notificaciones no leídas y actualiza el badge
     */
    private void cargarContadorNotificaciones() {
        if (currentUser == null) {
            return;
        }

        String usuarioId = currentUser.getUid();

        db.collection("notificaciones")
                .whereEqualTo("usuarioId", usuarioId)
                .whereEqualTo("leida", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        android.util.Log.e("DashboardAdmin", "Error al cargar notificaciones: " +
                                (error != null ? error.getMessage() : "null"));
                        return;
                    }

                    int noLeidas = value.size();
                    if (noLeidas > 0) {
                        badgeNotificaciones.setVisibility(View.VISIBLE);
                        badgeNotificaciones.setText(String.valueOf(noLeidas));
                        android.util.Log.d("DashboardAdmin", "🔔 Notificaciones no leídas: " + noLeidas);
                    } else {
                        badgeNotificaciones.setVisibility(View.GONE);
                        android.util.Log.d("DashboardAdmin", "✅ No hay notificaciones pendientes");
                    }
                });
    }
}