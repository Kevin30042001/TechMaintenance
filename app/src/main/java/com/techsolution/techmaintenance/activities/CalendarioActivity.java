package com.techsolution.techmaintenance.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.adapters.MantenimientoAdapter;
import com.techsolution.techmaintenance.models.Mantenimiento;
import com.techsolution.techmaintenance.models.MantenimientoDetallado;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class CalendarioActivity extends AppCompatActivity {

    private static final String TAG = "CalendarioActivity";

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String userId;
    private String userRol;

    // Vistas
    private Toolbar toolbar;
    private CalendarView calendarView;
    private ChipGroup chipGroupFiltros, chipGroupPrioridad;
    private Chip chipTodos, chipSoloMios, chipPorTecnico;
    private Chip chipUrgente, chipAlta, chipMedia, chipBaja;
    private MaterialCardView cardMantenimientos;
    private TextView tvFechaSeleccionada, tvSinMantenimientos;
    private RecyclerView recyclerMantenimientos;
    private ProgressBar progressBar;

    // Adaptador
    private MantenimientoAdapter adapter;
    private List<MantenimientoDetallado> listaMantenimientos;
    private List<MantenimientoDetallado> todosMantenimientos; // Cache de todos los mantenimientos

    // Variables
    private long fechaSeleccionadaMillis;
    private String filtroActual = "todos"; // todos, solo_mios, por_tecnico
    private List<String> prioridadesSeleccionadas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendario);

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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

        // Cargar rol del usuario
        cargarRolUsuario();

        // Configurar listeners
        configurarListeners();

        // NO cargar mantenimientos aquí - esperar a que se cargue el rol primero
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        calendarView = findViewById(R.id.calendarView);
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros);
        chipGroupPrioridad = findViewById(R.id.chipGroupPrioridad);
        chipTodos = findViewById(R.id.chipTodos);
        chipSoloMios = findViewById(R.id.chipSoloMios);
        chipPorTecnico = findViewById(R.id.chipPorTecnico);
        chipUrgente = findViewById(R.id.chipUrgente);
        chipAlta = findViewById(R.id.chipAlta);
        chipMedia = findViewById(R.id.chipMedia);
        chipBaja = findViewById(R.id.chipBaja);
        cardMantenimientos = findViewById(R.id.cardMantenimientos);
        tvFechaSeleccionada = findViewById(R.id.tvFechaSeleccionada);
        tvSinMantenimientos = findViewById(R.id.tvSinMantenimientos);
        recyclerMantenimientos = findViewById(R.id.recyclerMantenimientos);
        progressBar = findViewById(R.id.progressBar);

        // Configurar RecyclerView
        listaMantenimientos = new ArrayList<>();
        todosMantenimientos = new ArrayList<>();
        adapter = new MantenimientoAdapter(this, listaMantenimientos);
        recyclerMantenimientos.setLayoutManager(new LinearLayoutManager(this));
        recyclerMantenimientos.setAdapter(adapter);

        // Obtener fecha actual del calendario
        fechaSeleccionadaMillis = calendarView.getDate();
    }

    private void configurarListeners() {
        // Listener de cambio de fecha
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth, 0, 0, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            fechaSeleccionadaMillis = calendar.getTimeInMillis();

            Log.d(TAG, "📅 Fecha seleccionada: " + dayOfMonth + "/" + (month + 1) + "/" + year);

            // Actualizar UI con la fecha
            SimpleDateFormat sdf = new SimpleDateFormat("dd 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
            tvFechaSeleccionada.setText("Mantenimientos - " + sdf.format(new Date(fechaSeleccionadaMillis)));

            // Filtrar mantenimientos del día
            filtrarMantenimientosPorFecha();
        });

        // Listener de filtros principales
        chipGroupFiltros.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipTodos)) {
                filtroActual = "todos";
            } else if (checkedIds.contains(R.id.chipSoloMios)) {
                filtroActual = "solo_mios";
            } else if (checkedIds.contains(R.id.chipPorTecnico)) {
                filtroActual = "por_tecnico";
            }

            Log.d(TAG, "🔍 Filtro cambiado a: " + filtroActual);
            cargarTodosMantenimientos();
        });

        // Listener de filtros de prioridad
        chipUrgente.setOnCheckedChangeListener((buttonView, isChecked) -> {
            actualizarFiltroPrioridad("urgente", isChecked);
        });
        chipAlta.setOnCheckedChangeListener((buttonView, isChecked) -> {
            actualizarFiltroPrioridad("alta", isChecked);
        });
        chipMedia.setOnCheckedChangeListener((buttonView, isChecked) -> {
            actualizarFiltroPrioridad("media", isChecked);
        });
        chipBaja.setOnCheckedChangeListener((buttonView, isChecked) -> {
            actualizarFiltroPrioridad("baja", isChecked);
        });
    }

    private void actualizarFiltroPrioridad(String prioridad, boolean agregar) {
        if (agregar) {
            if (!prioridadesSeleccionadas.contains(prioridad)) {
                prioridadesSeleccionadas.add(prioridad);
            }
        } else {
            prioridadesSeleccionadas.remove(prioridad);
        }

        Log.d(TAG, "🎯 Prioridades seleccionadas: " + prioridadesSeleccionadas);
        filtrarMantenimientosPorFecha();
    }

    private void cargarRolUsuario() {
        Log.d(TAG, "🔐 Cargando rol del usuario: " + userId);

        db.collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userRol = documentSnapshot.getString("rol");

                        Log.d(TAG, "👤 Usuario encontrado:");
                        Log.d(TAG, "   - ID: " + userId);
                        Log.d(TAG, "   - Rol: " + userRol);
                        Log.d(TAG, "   - Nombre: " + documentSnapshot.getString("nombre"));
                        Log.d(TAG, "   - Email: " + documentSnapshot.getString("email"));

                        // Si es admin, mostrar opción "Por Técnico"
                        if ("admin".equals(userRol)) {
                            chipPorTecnico.setVisibility(View.VISIBLE);
                            Log.d(TAG, "✅ Usuario es ADMIN - mostrando todas las opciones");
                        } else {
                            // Si es técnico, establecer "Solo Míos" por defecto
                            chipSoloMios.setChecked(true);
                            filtroActual = "solo_mios";
                            Log.d(TAG, "✅ Usuario es TÉCNICO - filtrando solo asignados");
                        }

                        // IMPORTANTE: Ahora que el rol está cargado, cargar los mantenimientos
                        Log.d(TAG, "🚀 Rol cargado exitosamente, iniciando carga de mantenimientos...");
                        cargarTodosMantenimientos();
                    } else {
                        Log.e(TAG, "❌ Documento de usuario NO existe en Firestore");
                        Log.e(TAG, "   - User ID buscado: " + userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error al cargar rol: " + e.getMessage());
                    e.printStackTrace();
                });
    }

    private void cargarTodosMantenimientos() {
        progressBar.setVisibility(View.VISIBLE);
        cardMantenimientos.setVisibility(View.GONE);

        Log.d(TAG, "========================================");
        Log.d(TAG, "📥 INICIANDO CARGA DE MANTENIMIENTOS");
        Log.d(TAG, "========================================");
        Log.d(TAG, "   - Filtro actual: " + filtroActual);
        Log.d(TAG, "   - User ID: " + userId);
        Log.d(TAG, "   - Rol del usuario: " + userRol);

        // Construir query según el rol - IMPORTANTE: Para técnicos, filtrar en Firestore
        Query query;

        if ("admin".equals(userRol)) {
            // Admin puede consultar todos sin filtros
            Log.d(TAG, "🔍 Query: db.collection('mantenimientos') [SIN FILTROS]");
            query = db.collection("mantenimientos");
        } else {
            // Técnico DEBE filtrar en la query (las reglas de Firestore lo requieren)
            // Cargar mantenimientos donde es técnico principal
            Log.d(TAG, "🔍 Query: db.collection('mantenimientos').whereEqualTo('tecnicoPrincipalId', '" + userId + "')");
            query = db.collection("mantenimientos")
                    .whereEqualTo("tecnicoPrincipalId", userId);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    todosMantenimientos.clear();

                    Log.d(TAG, "📊 Query completada. Documentos encontrados: " + queryDocumentSnapshots.size());

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Mantenimiento mantenimiento = doc.toObject(Mantenimiento.class);
                        mantenimiento.setMantenimientoId(doc.getId());

                        // Log detallado de cada mantenimiento
                        Log.d(TAG, "📅 Mantenimiento ID: " + doc.getId());
                        Log.d(TAG, "   - Estado: " + mantenimiento.getEstado());
                        Log.d(TAG, "   - Fecha programada: " + (mantenimiento.getFechaProgramada() != null ? mantenimiento.getFechaProgramada().toDate() : "null"));
                        Log.d(TAG, "   - Técnico principal: " + mantenimiento.getTecnicoPrincipalId());

                        // Crear objeto detallado
                        MantenimientoDetallado detallado = new MantenimientoDetallado(mantenimiento);
                        todosMantenimientos.add(detallado);
                    }

                    // Si es técnico, también cargar los de apoyo (segunda consulta)
                    if (!"admin".equals(userRol)) {
                        cargarMantenimientosDeApoyo();
                    } else {
                        Log.d(TAG, "✅ Mantenimientos cargados (admin): " + todosMantenimientos.size());
                        cargarDatosRelacionados();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "========================================");
                    Log.e(TAG, "❌ ERROR AL CARGAR MANTENIMIENTOS");
                    Log.e(TAG, "========================================");
                    Log.e(TAG, "   - Mensaje: " + e.getMessage());
                    Log.e(TAG, "   - Tipo de excepción: " + e.getClass().getName());
                    Log.e(TAG, "   - User ID: " + userId);
                    Log.e(TAG, "   - Rol: " + userRol);
                    e.printStackTrace();

                    Toast.makeText(this, "Error al cargar mantenimientos: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    /**
     * Carga mantenimientos donde el técnico está como apoyo.
     * Solo para técnicos (no admin).
     */
    private void cargarMantenimientosDeApoyo() {
        db.collection("mantenimientos")
                .whereArrayContains("tecnicosApoyo", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Mantenimiento mantenimiento = doc.toObject(Mantenimiento.class);
                        mantenimiento.setMantenimientoId(doc.getId());

                        // Verificar que no esté duplicado (por si es principal Y apoyo)
                        boolean yaExiste = false;
                        for (MantenimientoDetallado d : todosMantenimientos) {
                            if (d.getMantenimiento().getMantenimientoId().equals(mantenimiento.getMantenimientoId())) {
                                yaExiste = true;
                                break;
                            }
                        }

                        if (!yaExiste) {
                            MantenimientoDetallado detallado = new MantenimientoDetallado(mantenimiento);
                            todosMantenimientos.add(detallado);
                        }
                    }

                    Log.d(TAG, "✅ Mantenimientos cargados (incluyendo apoyo): " + todosMantenimientos.size());
                    cargarDatosRelacionados();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error al cargar mantenimientos de apoyo: " + e.getMessage());
                    // Continuar con los que ya se cargaron
                    cargarDatosRelacionados();
                });
    }

    /**
     * Carga todos los datos relacionados de equipos, clientes y técnicos.
     */
    private void cargarDatosRelacionados() {
        if (todosMantenimientos.isEmpty()) {
            Log.d(TAG, "📭 No hay mantenimientos para cargar datos relacionados");
            progressBar.setVisibility(View.GONE);
            filtrarMantenimientosPorFecha();
            return;
        }

        Log.d(TAG, "🔄 Cargando datos relacionados...");

        // Contador para saber cuándo terminaron todas las cargas
        AtomicInteger cargasPendientes = new AtomicInteger(todosMantenimientos.size() * 3);

        for (MantenimientoDetallado detallado : todosMantenimientos) {
            Mantenimiento m = detallado.getMantenimiento();

            // Cargar datos del equipo
            db.collection("equipos").document(m.getEquipoId())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            detallado.setEquipoMarca(doc.getString("marca"));
                            detallado.setEquipoModelo(doc.getString("modelo"));
                        }
                        verificarCargaCompletaCalendario(cargasPendientes);
                    })
                    .addOnFailureListener(e -> verificarCargaCompletaCalendario(cargasPendientes));

            // Cargar datos del cliente
            db.collection("clientes").document(m.getClienteId())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            detallado.setClienteNombreEmpresa(doc.getString("nombreEmpresa"));
                            detallado.setClienteDireccion(doc.getString("direccion"));
                            detallado.setClienteEmail(doc.getString("emailContacto"));
                        }
                        verificarCargaCompletaCalendario(cargasPendientes);
                    })
                    .addOnFailureListener(e -> verificarCargaCompletaCalendario(cargasPendientes));

            // Cargar datos del técnico
            db.collection("usuarios").document(m.getTecnicoPrincipalId())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            detallado.setTecnicoNombre(doc.getString("nombre"));
                        }
                        verificarCargaCompletaCalendario(cargasPendientes);
                    })
                    .addOnFailureListener(e -> verificarCargaCompletaCalendario(cargasPendientes));
        }
    }

    /**
     * Verifica si todas las cargas de datos relacionados han terminado.
     */
    private void verificarCargaCompletaCalendario(AtomicInteger cargasPendientes) {
        int pendientes = cargasPendientes.decrementAndGet();

        if (pendientes == 0) {
            Log.d(TAG, "✅ Todos los datos relacionados cargados");
            progressBar.setVisibility(View.GONE);

            // Filtrar por fecha actual del calendario
            filtrarMantenimientosPorFecha();
        }
    }

    private boolean esTecnicoAsignado(Mantenimiento mantenimiento) {
        // Verificar si es técnico principal
        if (userId.equals(mantenimiento.getTecnicoPrincipalId())) {
            return true;
        }

        // Verificar si está en técnicos de apoyo
        if (mantenimiento.getTecnicosApoyo() != null) {
            return mantenimiento.getTecnicosApoyo().contains(userId);
        }

        return false;
    }

    private void filtrarMantenimientosPorFecha() {
        listaMantenimientos.clear();

        // Obtener inicio y fin del día seleccionado
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(fechaSeleccionadaMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date inicioDia = calendar.getTime();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date finDia = calendar.getTime();

        Log.d(TAG, "🔍 ========== INICIANDO FILTRADO ==========");
        Log.d(TAG, "🔍 Fecha seleccionada - Inicio: " + inicioDia);
        Log.d(TAG, "🔍 Fecha seleccionada - Fin: " + finDia);
        Log.d(TAG, "🔍 Total mantenimientos disponibles: " + todosMantenimientos.size());
        Log.d(TAG, "🔍 Prioridades seleccionadas: " + prioridadesSeleccionadas);

        int contador = 0;
        for (MantenimientoDetallado detallado : todosMantenimientos) {
            Mantenimiento mantenimiento = detallado.getMantenimiento();
            Timestamp fechaProgramada = mantenimiento.getFechaProgramada();

            Log.d(TAG, "🔍 Evaluando mantenimiento #" + (++contador) + " - ID: " + mantenimiento.getMantenimientoId());

            if (fechaProgramada != null) {
                Date fechaMantenimiento = fechaProgramada.toDate();
                Log.d(TAG, "   - Fecha programada: " + fechaMantenimiento);

                // Verificar si está en el día seleccionado
                boolean dentroRango = !fechaMantenimiento.before(inicioDia) && !fechaMantenimiento.after(finDia);
                Log.d(TAG, "   - Dentro del rango: " + dentroRango);

                if (dentroRango) {
                    // Aplicar filtro de prioridad si hay alguno seleccionado
                    if (prioridadesSeleccionadas.isEmpty()) {
                        listaMantenimientos.add(detallado);
                        Log.d(TAG, "   ✅ AGREGADO (sin filtro de prioridad)");
                    } else {
                        String prioridad = mantenimiento.getPrioridad();
                        Log.d(TAG, "   - Prioridad: " + prioridad);
                        if (prioridad != null && prioridadesSeleccionadas.contains(prioridad)) {
                            listaMantenimientos.add(detallado);
                            Log.d(TAG, "   ✅ AGREGADO (coincide prioridad)");
                        } else {
                            Log.d(TAG, "   ❌ NO agregado (prioridad no coincide)");
                        }
                    }
                } else {
                    Log.d(TAG, "   ❌ NO agregado (fuera de rango de fechas)");
                }
            } else {
                Log.d(TAG, "   ⚠️ Fecha programada es NULL");
            }
        }

        Log.d(TAG, "✅ ========== FILTRADO COMPLETO ==========");
        Log.d(TAG, "✅ Mantenimientos filtrados: " + listaMantenimientos.size());

        // Actualizar UI
        cardMantenimientos.setVisibility(View.VISIBLE);
        if (listaMantenimientos.isEmpty()) {
            tvSinMantenimientos.setVisibility(View.VISIBLE);
            recyclerMantenimientos.setVisibility(View.GONE);
            Log.d(TAG, "📭 Lista vacía - mostrando mensaje sin datos");
        } else {
            tvSinMantenimientos.setVisibility(View.GONE);
            recyclerMantenimientos.setVisibility(View.VISIBLE);

            Log.d(TAG, "👁️ Mostrando RecyclerView:");
            Log.d(TAG, "   - Lista size ANTES de notifyDataSetChanged: " + listaMantenimientos.size());
            Log.d(TAG, "   - Adapter itemCount ANTES: " + adapter.getItemCount());

            adapter.notifyDataSetChanged();

            // IMPORTANTE: Calcular altura dinámica del RecyclerView
            // Esto resuelve el problema de RecyclerView con wrap_content en ScrollView
            ajustarAlturaRecyclerView(listaMantenimientos.size());

            Log.d(TAG, "   - Adapter itemCount DESPUÉS: " + adapter.getItemCount());
            Log.d(TAG, "   - RecyclerView visibility: " + (recyclerMantenimientos.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
        }
    }

    /**
     * Ajusta la altura del RecyclerView basándose en el número de items
     * Soluciona el problema de RecyclerView con wrap_content dentro de ScrollView
     */
    private void ajustarAlturaRecyclerView(int numItems) {
        if (numItems == 0) {
            Log.d(TAG, "⚠️ ajustarAlturaRecyclerView: numItems = 0, no se ajusta altura");
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
        ViewGroup.LayoutParams params = recyclerMantenimientos.getLayoutParams();
        params.height = alturaTotalPx;
        recyclerMantenimientos.setLayoutParams(params);

        Log.d(TAG, "🔧 Altura RecyclerView ajustada:");
        Log.d(TAG, "   - Items: " + numItems);
        Log.d(TAG, "   - Altura por item: " + alturaPorItemDp + "dp (" + alturaPorItemPx + "px)");
        Log.d(TAG, "   - Altura total: " + (alturaTotalPx / density) + "dp (" + alturaTotalPx + "px)");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar datos al volver SOLO si el rol ya está cargado
        if (userRol != null) {
            Log.d(TAG, "🔄 onResume: Recargando mantenimientos (rol: " + userRol + ")");
            cargarTodosMantenimientos();
        } else {
            Log.d(TAG, "⏳ onResume: Esperando a que se cargue el rol primero");
        }
    }
}
