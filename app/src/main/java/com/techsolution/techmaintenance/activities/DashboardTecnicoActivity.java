package com.techsolution.techmaintenance.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.adapters.MantenimientoAdapter;
import com.techsolution.techmaintenance.helpers.DateUtils;
import com.techsolution.techmaintenance.helpers.FirestoreHelper;
import com.techsolution.techmaintenance.models.Mantenimiento;
import com.techsolution.techmaintenance.models.Usuario;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DashboardTecnicoActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String tecnicoId;

    // Vistas - Header
    private ImageView imgPerfil, imgPerfilGrande;
    private android.widget.ImageButton btnNotificaciones;
    private TextView badgeNotificaciones;
    private TextView tvSaludo, tvServiciosHoy;

    // Vistas - Mis Tareas
    private RecyclerView recyclerTareas;
    private MantenimientoAdapter adapter;
    private List<Mantenimiento> listaTareas;
    private ProgressBar progressBarTareas;
    private TextView tvSinTareas;
    private TextView tvVerTodasTareas;

    // Vistas - Mis Estadísticas
    private TextView tvMisCompletados, tvMiCalificacion, tvMiEficiencia, tvATiempo;

    // Vistas - Próxima Cita
    private MaterialCardView cardProximaCita;
    private TextView tvSinProximaCita;
    private TextView tvProximaFecha, tvProximaHora, tvProximaEquipo, tvProximaCliente, tvProximaUbicacion;
    private MaterialButton btnVerDetallesCita;
    private String proximaCitaId;

    // Otras vistas
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_tecnico);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Verificar autenticación
        if (currentUser == null) {
            finish();
            return;
        }

        tecnicoId = currentUser.getUid();

        // Verificar que sea técnico (no admin)
        verificarRolTecnico();

        // Inicializar vistas
        inicializarVistas();

        // Configurar listeners
        configurarListeners();

        // Configurar RecyclerView
        configurarRecyclerView();

        // Cargar todos los datos
        cargarDatos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar datos cuando volvemos al Dashboard
        // Esto actualiza las estadísticas después de completar servicios, etc.
        cargarDatos();
    }

    private void inicializarVistas() {
        // Header
        imgPerfil = findViewById(R.id.imgPerfil);
        btnNotificaciones = findViewById(R.id.btnNotificaciones);
        badgeNotificaciones = findViewById(R.id.badgeNotificaciones);
        imgPerfilGrande = findViewById(R.id.imgPerfilGrande);
        tvSaludo = findViewById(R.id.tvSaludo);
        tvServiciosHoy = findViewById(R.id.tvServiciosHoy);

        // Mis Tareas
        recyclerTareas = findViewById(R.id.recyclerTareas);
        progressBarTareas = findViewById(R.id.progressBarTareas);
        tvSinTareas = findViewById(R.id.tvSinTareas);
        tvVerTodasTareas = findViewById(R.id.tvVerTodasTareas);

        // Estadísticas
        tvMisCompletados = findViewById(R.id.tvMisCompletados);
        tvMiCalificacion = findViewById(R.id.tvMiCalificacion);
        tvMiEficiencia = findViewById(R.id.tvMiEficiencia);
        tvATiempo = findViewById(R.id.tvATiempo);

        // Próxima Cita
        cardProximaCita = findViewById(R.id.cardProximaCita);
        tvSinProximaCita = findViewById(R.id.tvSinProximaCita);
        tvProximaFecha = findViewById(R.id.tvProximaFecha);
        tvProximaHora = findViewById(R.id.tvProximaHora);
        tvProximaEquipo = findViewById(R.id.tvProximaEquipo);
        tvProximaCliente = findViewById(R.id.tvProximaCliente);
        tvProximaUbicacion = findViewById(R.id.tvProximaUbicacion);
        btnVerDetallesCita = findViewById(R.id.btnVerDetallesCita);

        // Bottom Navigation
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void configurarListeners() {
        // Click en perfil
        imgPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(this, PerfilTecnicoActivity.class);
            startActivity(intent);
        });

        // Click en notificaciones
        btnNotificaciones.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificacionesActivity.class);
            startActivity(intent);
        });

        // Bottom Navigation
        bottomNavigation.setSelectedItemId(R.id.nav_inicio);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_inicio) {
                return true;
            } else if (itemId == R.id.nav_mis_equipos) {
                // Abrir lista de equipos (filtrada por técnico)
                Intent intent = new Intent(this, ListaEquiposActivity.class);
                intent.putExtra("filtrarPorTecnico", true);
                intent.putExtra("tecnicoId", tecnicoId);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_mis_tareas) {
                // Abrir lista de mantenimientos (filtrada por técnico)
                Intent intent = new Intent(this, ListaMantenimientosActivity.class);
                intent.putExtra("filtrarPorTecnico", true);
                intent.putExtra("tecnicoId", tecnicoId);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_calendario) {
                // Abrir calendario de mantenimientos
                Intent intent = new Intent(this, CalendarioActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_perfil) {
                // Abrir perfil del técnico
                Intent intent = new Intent(this, PerfilTecnicoActivity.class);
                startActivity(intent);
                return true;
            }

            return false;
        });

        // Click en ver detalles de próxima cita
        if (btnVerDetallesCita != null) {
            btnVerDetallesCita.setOnClickListener(v -> {
                if (proximaCitaId != null) {
                    Intent intent = new Intent(this, DetalleMantenimientoActivity.class);
                    intent.putExtra("mantenimientoId", proximaCitaId);
                    startActivity(intent);
                }
            });
        }

        // Click en "Ver todas" las tareas
        if (tvVerTodasTareas != null) {
            tvVerTodasTareas.setOnClickListener(v -> {
                // Abrir lista completa de mantenimientos del técnico
                Intent intent = new Intent(this, ListaMantenimientosActivity.class);
                intent.putExtra("filtrarPorTecnico", true);
                intent.putExtra("tecnicoId", tecnicoId);
                startActivity(intent);
            });
        }
    }

    private void configurarRecyclerView() {
        listaTareas = new ArrayList<>();
        // Constructor para técnico (pasa el tecnicoId) - modo legacy
        adapter = MantenimientoAdapter.createLegacy(this, listaTareas, tecnicoId);
        recyclerTareas.setLayoutManager(new LinearLayoutManager(this));
        recyclerTareas.setAdapter(adapter);
        recyclerTareas.setNestedScrollingEnabled(false);
    }

    // Verificar que el usuario sea técnico
    private void verificarRolTecnico() {
        db.collection("usuarios").document(tecnicoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Usuario usuario = documentSnapshot.toObject(Usuario.class);
                        if (usuario != null) {
                            if (!"tecnico".equals(usuario.getRol())) {
                                // No es técnico, redirigir
                                Toast.makeText(this, "Acceso no autorizado", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                            if (!"activo".equals(usuario.getEstado())) {
                                // Cuenta inactiva
                                Toast.makeText(this, "Tu cuenta está inactiva", Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                                finish();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al verificar usuario", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void cargarDatos() {
        cargarDatosTecnico();
        cargarTareasDeHoy();
        cargarEstadisticasMes();
        cargarProximaCita();
        cargarContadorNotificaciones();
    }

    // ==================== CARGAR DATOS DEL TÉCNICO ====================

    private void cargarDatosTecnico() {
        db.collection("usuarios").document(tecnicoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nombre = documentSnapshot.getString("nombre");
                        if (nombre != null) {
                            tvSaludo.setText("Hola, " + nombre.split(" ")[0]);
                        }

                        // Cargar foto de perfil si existe
                        String fotoPerfilURL = documentSnapshot.getString("fotoPerfilURL");
                        if (fotoPerfilURL != null && !fotoPerfilURL.isEmpty()) {
                            // Cargar foto pequeña en toolbar
                            com.bumptech.glide.Glide.with(DashboardTecnicoActivity.this)
                                    .load(fotoPerfilURL)
                                    .placeholder(R.drawable.ic_person)
                                    .error(R.drawable.ic_person)
                                    .circleCrop()
                                    .into(imgPerfil);

                            // Cargar foto grande en sección de perfil
                            com.bumptech.glide.Glide.with(DashboardTecnicoActivity.this)
                                    .load(fotoPerfilURL)
                                    .placeholder(R.drawable.ic_person)
                                    .error(R.drawable.ic_person)
                                    .circleCrop()
                                    .into(imgPerfilGrande);
                        }
                    }
                });
    }

    // ==================== CARGAR TAREAS DE HOY ====================

    private void cargarTareasDeHoy() {
        android.util.Log.d("DashboardTecnico", "========================================");
        android.util.Log.d("DashboardTecnico", "📥 INICIANDO CARGA DE TAREAS DE HOY");
        android.util.Log.d("DashboardTecnico", "========================================");

        progressBarTareas.setVisibility(View.VISIBLE);
        recyclerTareas.setVisibility(View.GONE);
        tvSinTareas.setVisibility(View.GONE);

        android.util.Log.d("DashboardTecnico", "👤 Técnico ID: " + tecnicoId);

        // Obtener fecha de HOY (inicio y fin del día actual)
        Timestamp inicioDia = FirestoreHelper.getInicioDiaActual();
        Timestamp finDia = FirestoreHelper.getFinDiaActual();

        android.util.Log.d("DashboardTecnico", "📅 Fecha inicio día: " + inicioDia.toDate());
        android.util.Log.d("DashboardTecnico", "📅 Fecha fin día: " + finDia.toDate());

        android.util.Log.d("DashboardTecnico", "🔍 Query: db.collection('mantenimientos')");
        android.util.Log.d("DashboardTecnico", "         .whereEqualTo('tecnicoPrincipalId', '" + tecnicoId + "')");
        android.util.Log.d("DashboardTecnico", "         .whereGreaterThanOrEqualTo('fechaProgramada', " + inicioDia.toDate() + ")");

        // ✅ CORRECCIÓN: Usar solo >= y filtrar en código para evitar error de índice
        // Firestore no permite dos comparadores de rango en el mismo campo sin índice compuesto
        db.collection("mantenimientos")
                .whereEqualTo("tecnicoPrincipalId", tecnicoId)
                .whereGreaterThanOrEqualTo("fechaProgramada", inicioDia)
                .orderBy("fechaProgramada", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    android.util.Log.d("DashboardTecnico", "✅ Query completada");
                    android.util.Log.d("DashboardTecnico", "📊 Documentos encontrados en Firestore: " + queryDocumentSnapshots.size());

                    listaTareas.clear();

                    int contador = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        contador++;
                        android.util.Log.d("DashboardTecnico", "");
                        android.util.Log.d("DashboardTecnico", "🔍 Evaluando mantenimiento #" + contador + " - ID: " + doc.getId());

                        try {
                            Mantenimiento mantenimiento = doc.toObject(Mantenimiento.class);
                            mantenimiento.setMantenimientoId(doc.getId());

                            // Mostrar datos del mantenimiento
                            Timestamp fechaProgramada = mantenimiento.getFechaProgramada();
                            String estado = mantenimiento.getEstado();

                            android.util.Log.d("DashboardTecnico", "   - Estado: " + estado);
                            android.util.Log.d("DashboardTecnico", "   - Fecha programada: " + (fechaProgramada != null ? fechaProgramada.toDate() : "NULL"));

                            // Filtrar manualmente por fin de día
                            if (fechaProgramada == null) {
                                android.util.Log.d("DashboardTecnico", "   ❌ Rechazado: fechaProgramada es NULL");
                                continue;
                            }

                            boolean despuesDeFinDia = fechaProgramada.compareTo(finDia) > 0;
                            android.util.Log.d("DashboardTecnico", "   - Después de fin día: " + despuesDeFinDia);

                            if (despuesDeFinDia) {
                                android.util.Log.d("DashboardTecnico", "   ❌ Rechazado: es de otro día (después del fin de hoy)");
                                continue;
                            }

                            // Solo agregar si no está cancelado ni completado
                            boolean esCanceladoOCompletado = "cancelado".equals(estado) || "completado".equals(estado);
                            android.util.Log.d("DashboardTecnico", "   - Es cancelado o completado: " + esCanceladoOCompletado);

                            if (!esCanceladoOCompletado) {
                                listaTareas.add(mantenimiento);
                                android.util.Log.d("DashboardTecnico", "   ✅ AGREGADO a listaTareas");
                            } else {
                                android.util.Log.d("DashboardTecnico", "   ❌ Rechazado: estado es " + estado);
                            }
                        } catch (Exception e) {
                            android.util.Log.e("DashboardTecnico", "   ❌ Error al parsear mantenimiento: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    android.util.Log.d("DashboardTecnico", "========================================");
                    android.util.Log.d("DashboardTecnico", "📊 RESUMEN DE FILTRADO");
                    android.util.Log.d("DashboardTecnico", "========================================");
                    android.util.Log.d("DashboardTecnico", "Total en listaTareas: " + listaTareas.size());

                    progressBarTareas.setVisibility(View.GONE);

                    if (listaTareas.isEmpty()) {
                        android.util.Log.d("DashboardTecnico", "📭 Lista vacía - mostrando mensaje 'Sin tareas'");
                        // IMPORTANTE: Ocultar RecyclerView cuando no hay tareas
                        recyclerTareas.setVisibility(View.GONE);
                        tvSinTareas.setVisibility(View.VISIBLE);
                        tvServiciosHoy.setText("No tienes servicios programados para hoy");
                        android.util.Log.d("DashboardTecnico", "   - RecyclerView: GONE");
                        android.util.Log.d("DashboardTecnico", "   - tvSinTareas: VISIBLE");
                    } else {
                        android.util.Log.d("DashboardTecnico", "👁️ Lista con datos - mostrando RecyclerView");
                        android.util.Log.d("DashboardTecnico", "   - Total tareas: " + listaTareas.size());

                        // Mostrar RecyclerView y ocultar mensaje
                        tvSinTareas.setVisibility(View.GONE);
                        recyclerTareas.setVisibility(View.VISIBLE);

                        android.util.Log.d("DashboardTecnico", "   - Adapter itemCount ANTES de actualizar: " + adapter.getItemCount());
                        // IMPORTANTE: Usar actualizarLista() en lugar de notifyDataSetChanged()
                        // Esto sincroniza la lista del adapter con listaTareas
                        adapter.actualizarLista(listaTareas);
                        android.util.Log.d("DashboardTecnico", "   - Adapter itemCount DESPUÉS de actualizar: " + adapter.getItemCount());

                        // IMPORTANTE: Calcular altura dinámica del RecyclerView
                        // Esto resuelve el problema de RecyclerView con wrap_content en ScrollView
                        ajustarAlturaRecyclerView(listaTareas.size());

                        int total = listaTareas.size();
                        tvServiciosHoy.setText("Tienes " + total + (total == 1 ? " servicio hoy" : " servicios hoy"));

                        android.util.Log.d("DashboardTecnico", "   - RecyclerView: VISIBLE");
                        android.util.Log.d("DashboardTecnico", "   - tvSinTareas: GONE");
                        android.util.Log.d("DashboardTecnico", "   - Mensaje header: " + tvServiciosHoy.getText());
                    }

                    android.util.Log.d("DashboardTecnico", "========================================");
                    android.util.Log.d("DashboardTecnico", "✅ CARGA DE TAREAS COMPLETADA");
                    android.util.Log.d("DashboardTecnico", "========================================");
                    android.util.Log.d("DashboardTecnico", "");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DashboardTecnico", "❌ ERROR al cargar tareas: " + e.getMessage());
                    e.printStackTrace();

                    progressBarTareas.setVisibility(View.GONE);
                    tvSinTareas.setVisibility(View.VISIBLE);
                    tvSinTareas.setText("Error: " + e.getMessage());
                    tvServiciosHoy.setText("Error al cargar servicios");
                });
    }

    // ==================== CARGAR ESTADÍSTICAS DEL MES ====================

    private void cargarEstadisticasMes() {
        cargarCompletadosMes();
        cargarCalificacionPromedio();
        cargarEficiencia();
    }

    // Completados este mes
    private void cargarCompletadosMes() {
        Timestamp inicioMes = FirestoreHelper.getInicioMesActual();
        Timestamp finMes = FirestoreHelper.getFinMesActual();

        android.util.Log.d("DashboardTecnico", "📊 Cargando completados del mes para: " + tecnicoId);
        android.util.Log.d("DashboardTecnico", "📊 Rango: " + inicioMes.toDate() + " hasta " + finMes.toDate());

        // ✅ CORRECCIÓN: Usar solo >= y filtrar en código
        db.collection("mantenimientos")
                .whereEqualTo("tecnicoPrincipalId", tecnicoId)
                .whereEqualTo("estado", "completado")
                .whereGreaterThanOrEqualTo("fechaFinalizacion", inicioMes)
                .orderBy("fechaFinalizacion", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int total = 0;

                    // Filtrar manualmente los que están dentro del mes
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Timestamp fechaFin = doc.getTimestamp("fechaFinalizacion");
                        if (fechaFin != null && fechaFin.compareTo(finMes) <= 0) {
                            total++;
                        }
                    }

                    tvMisCompletados.setText(String.valueOf(total));
                    android.util.Log.d("DashboardTecnico", "✅ Completados del mes: " + total);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DashboardTecnico", "❌ Error al cargar completados: " + e.getMessage());
                    tvMisCompletados.setText("0");
                });
    }

    // Calificación promedio
    private void cargarCalificacionPromedio() {
        android.util.Log.d("DashboardTecnico", "📊 Cargando calificación promedio para: " + tecnicoId);

        db.collection("mantenimientos")
                .whereEqualTo("tecnicoPrincipalId", tecnicoId)
                .whereEqualTo("validadoPorCliente", true)
                .whereGreaterThan("calificacionCliente", 0)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    android.util.Log.d("DashboardTecnico", "📊 Mantenimientos validados encontrados: " + queryDocumentSnapshots.size());

                    if (queryDocumentSnapshots.isEmpty()) {
                        tvMiCalificacion.setText("0.0");
                        android.util.Log.d("DashboardTecnico", "⚠️ No hay mantenimientos validados");
                        return;
                    }

                    double suma = 0;
                    int total = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Long calificacion = doc.getLong("calificacionCliente");
                        android.util.Log.d("DashboardTecnico", "📊 Mantenimiento " + doc.getId() + " calificación: " + calificacion);
                        if (calificacion != null) {
                            suma += calificacion;
                            total++;
                        }
                    }

                    double promedio = total > 0 ? suma / total : 0.0;
                    final String textoCalificacion = String.format("%.1f", promedio);

                    // ✅ FORZAR actualización en UI thread
                    runOnUiThread(() -> {
                        tvMiCalificacion.setText(textoCalificacion);
                        android.util.Log.d("DashboardTecnico", "✅ UI actualizada - Calificación: " + textoCalificacion);
                    });

                    android.util.Log.d("DashboardTecnico", "✅ Calificación promedio: " + textoCalificacion + " (suma=" + suma + ", total=" + total + ")");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DashboardTecnico", "❌ Error al cargar calificación: " + e.getMessage());
                    e.printStackTrace();
                    tvMiCalificacion.setText("0.0");
                });
    }

    // Eficiencia (servicios a tiempo)
    private void cargarEficiencia() {
        Timestamp inicioMes = FirestoreHelper.getInicioMesActual();
        Timestamp finMes = FirestoreHelper.getFinMesActual();

        android.util.Log.d("DashboardTecnico", "📊 Cargando eficiencia del mes");

        // ✅ CORRECCIÓN: Usar solo >= y filtrar en código
        db.collection("mantenimientos")
                .whereEqualTo("tecnicoPrincipalId", tecnicoId)
                .whereEqualTo("estado", "completado")
                .whereGreaterThanOrEqualTo("fechaFinalizacion", inicioMes)
                .orderBy("fechaFinalizacion", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalCompletados = 0;
                    int aTiempo = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Timestamp fechaProgramada = doc.getTimestamp("fechaProgramada");
                        Timestamp fechaFinalizacion = doc.getTimestamp("fechaFinalizacion");

                        // Filtrar manualmente por fin de mes
                        if (fechaFinalizacion == null || fechaFinalizacion.compareTo(finMes) > 0) {
                            continue;
                        }

                        totalCompletados++;

                        // Verificar si fue completado a tiempo (el mismo día o antes)
                        if (fechaProgramada != null && fechaFinalizacion != null) {
                            // Comparar solo la fecha (sin hora)
                            Calendar calProgramada = Calendar.getInstance();
                            calProgramada.setTime(fechaProgramada.toDate());
                            calProgramada.set(Calendar.HOUR_OF_DAY, 23);
                            calProgramada.set(Calendar.MINUTE, 59);
                            calProgramada.set(Calendar.SECOND, 59);

                            if (fechaFinalizacion.compareTo(new Timestamp(calProgramada.getTime())) <= 0) {
                                aTiempo++;
                            }
                        }
                    }

                    if (totalCompletados > 0) {
                        double eficiencia = ((double) aTiempo / totalCompletados) * 100;
                        tvMiEficiencia.setText(String.format("%.0f%%", eficiencia));
                        tvATiempo.setText(aTiempo + "/" + totalCompletados);
                        android.util.Log.d("DashboardTecnico", "✅ Eficiencia: " + String.format("%.0f%%", eficiencia) + " (" + aTiempo + "/" + totalCompletados + ")");
                    } else {
                        tvMiEficiencia.setText("0%");
                        tvATiempo.setText("0/0");
                        android.util.Log.d("DashboardTecnico", "⚠️ No hay servicios completados este mes");
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DashboardTecnico", "❌ Error al cargar eficiencia: " + e.getMessage());
                    tvMiEficiencia.setText("0%");
                    tvATiempo.setText("0/0");
                });
    }

    // ==================== CARGAR PRÓXIMA CITA ====================

    private void cargarProximaCita() {
        Timestamp ahora = FirestoreHelper.getTimestampActual();

        db.collection("mantenimientos")
                .whereEqualTo("tecnicoPrincipalId", tecnicoId)
                .whereEqualTo("estado", "programado")
                .whereGreaterThanOrEqualTo("fechaProgramada", ahora)
                .orderBy("fechaProgramada", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        tvSinProximaCita.setVisibility(View.VISIBLE);
                        cardProximaCita.setVisibility(View.GONE);
                        return;
                    }

                    QueryDocumentSnapshot doc = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                    Mantenimiento proximaCita = doc.toObject(Mantenimiento.class);
                    proximaCita.setMantenimientoId(doc.getId());
                    proximaCitaId = doc.getId(); // Guardar ID para navegación

                    // Mostrar datos de la próxima cita
                    tvSinProximaCita.setVisibility(View.GONE);
                    cardProximaCita.setVisibility(View.VISIBLE);

                    String fechaRelativa = DateUtils.formatearFechaRelativa(proximaCita.getFechaProgramada());
                    String fecha = DateUtils.formatearFecha(proximaCita.getFechaProgramada());
                    tvProximaFecha.setText(fechaRelativa + ", " + fecha);

                    String hora = DateUtils.formatearHora(proximaCita.getFechaProgramada());
                    tvProximaHora.setText("🕙 " + hora);

                    // Cargar equipo
                    db.collection("equipos").document(proximaCita.getEquipoId())
                            .get()
                            .addOnSuccessListener(equipoDoc -> {
                                if (equipoDoc.exists()) {
                                    String marca = equipoDoc.getString("marca");
                                    String modelo = equipoDoc.getString("modelo");
                                    tvProximaEquipo.setText("💻 " + marca + " " + modelo);
                                }
                            });

                    // Cargar cliente
                    db.collection("clientes").document(proximaCita.getClienteId())
                            .get()
                            .addOnSuccessListener(clienteDoc -> {
                                if (clienteDoc.exists()) {
                                    String nombreEmpresa = clienteDoc.getString("nombreEmpresa");
                                    String direccion = clienteDoc.getString("direccion");
                                    tvProximaCliente.setText("🏢 " + nombreEmpresa);
                                    tvProximaUbicacion.setText("📍 " + direccion);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    tvSinProximaCita.setVisibility(View.VISIBLE);
                    cardProximaCita.setVisibility(View.GONE);
                });
    }

    /**
     * Ajusta la altura del RecyclerView basándose en el número de items
     * Soluciona el problema de RecyclerView con wrap_content dentro de ScrollView
     */
    private void ajustarAlturaRecyclerView(int numItems) {
        if (numItems == 0) {
            android.util.Log.d("DashboardTecnico", "⚠️ ajustarAlturaRecyclerView: numItems = 0, no se ajusta altura");
            return;
        }

        // Altura estimada por item en dp (basada en item_mantenimiento_card.xml)
        // Cada tarjeta tiene aproximadamente 220dp de altura (ajustado para mostrar todo el contenido)
        int alturaPorItemDp = 220;

        // Convertir dp a pixels
        float density = getResources().getDisplayMetrics().density;
        int alturaPorItemPx = (int) (alturaPorItemDp * density);

        // Calcular altura total necesaria
        int alturaTotalPx = alturaPorItemPx * numItems;

        // Establecer altura del RecyclerView
        ViewGroup.LayoutParams params = recyclerTareas.getLayoutParams();
        params.height = alturaTotalPx;
        recyclerTareas.setLayoutParams(params);

        android.util.Log.d("DashboardTecnico", "🔧 Altura RecyclerView ajustada:");
        android.util.Log.d("DashboardTecnico", "   - Items: " + numItems);
        android.util.Log.d("DashboardTecnico", "   - Altura por item: " + alturaPorItemDp + "dp (" + alturaPorItemPx + "px)");
        android.util.Log.d("DashboardTecnico", "   - Altura total: " + (alturaTotalPx / density) + "dp (" + alturaTotalPx + "px)");
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
                        android.util.Log.e("DashboardTecnico", "Error al cargar notificaciones: " +
                                (error != null ? error.getMessage() : "null"));
                        return;
                    }

                    int noLeidas = value.size();
                    if (noLeidas > 0) {
                        badgeNotificaciones.setVisibility(View.VISIBLE);
                        badgeNotificaciones.setText(String.valueOf(noLeidas));
                        android.util.Log.d("DashboardTecnico", "🔔 Notificaciones no leídas: " + noLeidas);
                    } else {
                        badgeNotificaciones.setVisibility(View.GONE);
                        android.util.Log.d("DashboardTecnico", "✅ No hay notificaciones pendientes");
                    }
                });
    }
}