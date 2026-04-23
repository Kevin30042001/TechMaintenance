package com.techsolution.techmaintenance.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.adapters.MantenimientoAdapter;
import com.techsolution.techmaintenance.models.Mantenimiento;
import com.techsolution.techmaintenance.models.MantenimientoDetallado;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ListaMantenimientosActivity extends AppCompatActivity {

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private com.google.firebase.firestore.ListenerRegistration listenerMantenimientos;

    // Vistas
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private TextView tvContador;
    private TextInputEditText etBuscar;
    private MaterialButton btnFiltros, btnLimpiarFechas;
    private com.google.android.material.card.MaterialCardView cardFiltros;
    private ChipGroup chipGroupPrioridad, chipGroupTipo;
    private TextInputEditText etFechaDesde, etFechaHasta;
    private ProgressBar progressBar;
    private LinearLayout layoutSinDatos;
    private RecyclerView recyclerMantenimientos;
    private FloatingActionButton fabAgregar;

    // Adapter y lista
    private MantenimientoAdapter adapter;
    private List<MantenimientoDetallado> listaMantenimientos;
    private List<MantenimientoDetallado> listaMantenimientosFiltrada;

    // Variables de filtro
    private String filtroEstado = "todos"; // todos/programado/en_proceso/completado/cancelado
    private String filtroPrioridad = "todas";
    private String filtroTipo = "todos";
    private String busqueda = "";
    private com.google.firebase.Timestamp fechaDesde = null;
    private com.google.firebase.Timestamp fechaHasta = null;

    // Variables de rol
    private boolean esAdmin = false;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_mantenimientos);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
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

        // Verificar rol del usuario
        verificarRolUsuario();

        // Configurar tabs
        configurarTabs();

        // Configurar RecyclerView
        configurarRecyclerView();

        // Configurar listeners
        configurarListeners();
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        tvContador = findViewById(R.id.tvContadorMantenimientos);
        etBuscar = findViewById(R.id.etBuscar);
        btnFiltros = findViewById(R.id.btnFiltros);
        cardFiltros = findViewById(R.id.cardFiltros);
        chipGroupPrioridad = findViewById(R.id.chipGroupPrioridad);
        chipGroupTipo = findViewById(R.id.chipGroupTipo);
        etFechaDesde = findViewById(R.id.etFechaDesde);
        etFechaHasta = findViewById(R.id.etFechaHasta);
        btnLimpiarFechas = findViewById(R.id.btnLimpiarFechas);
        progressBar = findViewById(R.id.progressBar);
        layoutSinDatos = findViewById(R.id.layoutSinDatos);
        recyclerMantenimientos = findViewById(R.id.recyclerMantenimientos);
        fabAgregar = findViewById(R.id.fabAgregarMantenimiento);
    }

    private void verificarRolUsuario() {
        db.collection("usuarios").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String rol = documentSnapshot.getString("rol");
                        esAdmin = "admin".equals(rol);

                        // Mostrar/ocultar FAB según rol
                        fabAgregar.setVisibility(esAdmin ? View.VISIBLE : View.GONE);

                        // Cargar mantenimientos
                        cargarMantenimientos();
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ListaMantenimientos", "Error al verificar rol: " + e.getMessage());
                    Toast.makeText(this, "Error al verificar rol", Toast.LENGTH_SHORT).show();
                });
    }

    private void configurarTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Todos"));
        tabLayout.addTab(tabLayout.newTab().setText("Programados"));
        tabLayout.addTab(tabLayout.newTab().setText("En Proceso"));
        tabLayout.addTab(tabLayout.newTab().setText("Completados"));
        tabLayout.addTab(tabLayout.newTab().setText("Cancelados"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        filtroEstado = "todos";
                        break;
                    case 1:
                        filtroEstado = "programado";
                        break;
                    case 2:
                        filtroEstado = "en_proceso";
                        break;
                    case 3:
                        filtroEstado = "completado";
                        break;
                    case 4:
                        filtroEstado = "cancelado";
                        break;
                }
                aplicarFiltros();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void configurarRecyclerView() {
        listaMantenimientos = new ArrayList<>();
        listaMantenimientosFiltrada = new ArrayList<>();

        // Crear adapter según rol
        if (esAdmin) {
            adapter = new MantenimientoAdapter(this, listaMantenimientosFiltrada);
        } else {
            String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
            adapter = new MantenimientoAdapter(this, listaMantenimientosFiltrada, userId);
        }

        recyclerMantenimientos.setLayoutManager(new LinearLayoutManager(this));
        recyclerMantenimientos.setAdapter(adapter);
    }

    private void configurarListeners() {
        // Botón de filtros - Toggle expandir/colapsar
        btnFiltros.setOnClickListener(v -> {
            if (cardFiltros.getVisibility() == View.VISIBLE) {
                cardFiltros.setVisibility(View.GONE);
                btnFiltros.setIconResource(R.drawable.ic_menu);
            } else {
                cardFiltros.setVisibility(View.VISIBLE);
                btnFiltros.setIconResource(R.drawable.ic_close);
            }
        });

        // FAB agregar
        fabAgregar.setOnClickListener(v -> {
            Intent intent = new Intent(this, CrearMantenimientoActivity.class);
            startActivityForResult(intent, 100);
        });

        // Barra de búsqueda
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                busqueda = s.toString().toLowerCase().trim();
                aplicarFiltros();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Filtros de prioridad
        chipGroupPrioridad.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipTodasPrioridades) {
                filtroPrioridad = "todas";
            } else if (checkedId == R.id.chipUrgente) {
                filtroPrioridad = "urgente";
            } else if (checkedId == R.id.chipAlta) {
                filtroPrioridad = "alta";
            } else if (checkedId == R.id.chipMedia) {
                filtroPrioridad = "media";
            } else if (checkedId == R.id.chipBaja) {
                filtroPrioridad = "baja";
            } else {
                filtroPrioridad = "todas";
            }
            aplicarFiltros();
        });

        // Filtros de tipo
        chipGroupTipo.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipTodosTipos) {
                filtroTipo = "todos";
            } else if (checkedId == R.id.chipPreventivo) {
                filtroTipo = "preventivo";
            } else if (checkedId == R.id.chipCorrectivo) {
                filtroTipo = "correctivo";
            } else if (checkedId == R.id.chipEmergencia) {
                filtroTipo = "emergencia";
            } else {
                filtroTipo = "todos";
            }
            aplicarFiltros();
        });

        // Filtro fecha desde
        etFechaDesde.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth, 0, 0, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                fechaDesde = new com.google.firebase.Timestamp(calendar.getTime());

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                etFechaDesde.setText(sdf.format(calendar.getTime()));
                aplicarFiltros();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Filtro fecha hasta
        etFechaHasta.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth, 23, 59, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                fechaHasta = new com.google.firebase.Timestamp(calendar.getTime());

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                etFechaHasta.setText(sdf.format(calendar.getTime()));
                aplicarFiltros();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Limpiar filtro de fechas
        btnLimpiarFechas.setOnClickListener(v -> {
            fechaDesde = null;
            fechaHasta = null;
            etFechaDesde.setText("");
            etFechaHasta.setText("");
            aplicarFiltros();
        });
    }

    private void cargarMantenimientos() {
        mostrarCargando(true);

        android.util.Log.d("ListaMantenimientos", "========================================");
        android.util.Log.d("ListaMantenimientos", "📥 INICIANDO CARGA DE MANTENIMIENTOS");
        android.util.Log.d("ListaMantenimientos", "👤 Usuario: " + userId);
        android.util.Log.d("ListaMantenimientos", "🔑 Es Admin: " + esAdmin);
        android.util.Log.d("ListaMantenimientos", "========================================");

        // Remover listener anterior si existe
        if (listenerMantenimientos != null) {
            listenerMantenimientos.remove();
            android.util.Log.d("ListaMantenimientos", "🔌 Listener anterior removido");
        }

        // Verificar si viene filtro por técnico desde intent
        boolean filtrarPorTecnico = getIntent().getBooleanExtra("filtrarPorTecnico", false);
        String tecnicoIdFiltro = getIntent().getStringExtra("tecnicoId");

        if (filtrarPorTecnico && tecnicoIdFiltro != null) {
            android.util.Log.d("ListaMantenimientos", "🔍 Filtro por técnico activado: " + tecnicoIdFiltro);
        }

        // IMPORTANTE: Para técnicos, la query DEBE tener filtros (reglas de Firestore)
        Query query;
        if (esAdmin) {
            // Admin puede consultar todos sin filtros
            query = db.collection("mantenimientos");
        } else {
            // Técnico DEBE filtrar por su ID (reglas de Firestore lo requieren)
            query = db.collection("mantenimientos")
                    .whereEqualTo("tecnicoPrincipalId", userId);
        }

        android.util.Log.d("ListaMantenimientos", "📡 Ejecutando consulta a Firestore...");

        // Usar addSnapshotListener para actualizaciones en tiempo real
        listenerMantenimientos = query.addSnapshotListener((queryDocumentSnapshots, error) -> {
            if (error != null) {
                android.util.Log.e("ListaMantenimientos", "========================================");
                android.util.Log.e("ListaMantenimientos", "❌ ERROR AL CARGAR MANTENIMIENTOS");
                android.util.Log.e("ListaMantenimientos", "❌ Mensaje: " + error.getMessage());
                android.util.Log.e("ListaMantenimientos", "❌ Tipo: " + error.getClass().getName());
                android.util.Log.e("ListaMantenimientos", "========================================");

                error.printStackTrace();

                mostrarCargando(false);

                String mensajeError = error.getMessage();
                if (mensajeError != null && mensajeError.contains("index")) {
                    Toast.makeText(this, "Se requiere crear un índice en Firestore. Revisa Logcat.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Error al cargar mantenimientos: " + mensajeError, Toast.LENGTH_LONG).show();
                }

                return;
            }

            if (queryDocumentSnapshots == null) {
                android.util.Log.w("ListaMantenimientos", "⚠️ queryDocumentSnapshots es NULL");
                mostrarCargando(false);
                return;
            }

            listaMantenimientos.clear();

            android.util.Log.d("ListaMantenimientos", "========================================");
            android.util.Log.d("ListaMantenimientos", "✅ RESPUESTA RECIBIDA DE FIRESTORE");
            android.util.Log.d("ListaMantenimientos", "📦 Documentos encontrados: " + queryDocumentSnapshots.size());
            android.util.Log.d("ListaMantenimientos", "========================================");

            int contador = 0;
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                try {
                    contador++;
                    Mantenimiento mantenimiento = doc.toObject(Mantenimiento.class);
                    mantenimiento.setMantenimientoId(doc.getId());

                    android.util.Log.d("ListaMantenimientos", "📄 Documento #" + contador + ":");
                    android.util.Log.d("ListaMantenimientos", "   - ID: " + doc.getId());
                    android.util.Log.d("ListaMantenimientos", "   - Estado: " + mantenimiento.getEstado());
                    android.util.Log.d("ListaMantenimientos", "   - Tipo: " + mantenimiento.getTipo());
                    android.util.Log.d("ListaMantenimientos", "   - Técnico Principal: " + mantenimiento.getTecnicoPrincipalId());

                    // Crear objeto detallado que contendrá el mantenimiento y datos relacionados
                    MantenimientoDetallado detallado = new MantenimientoDetallado(mantenimiento);

                    // Si es admin, agregar todos
                    if (esAdmin) {
                        listaMantenimientos.add(detallado);
                        android.util.Log.d("ListaMantenimientos", "   ✅ Agregado (Admin)");
                    } else {
                        // Si es técnico, verificar si es principal o de apoyo
                        boolean esPrincipal = userId.equals(mantenimiento.getTecnicoPrincipalId());
                        boolean esApoyo = mantenimiento.getTecnicosApoyo() != null &&
                                mantenimiento.getTecnicosApoyo().contains(userId);

                        if (esPrincipal || esApoyo) {
                            listaMantenimientos.add(detallado);
                            android.util.Log.d("ListaMantenimientos", "   ✅ Agregado (Técnico " + (esPrincipal ? "Principal" : "Apoyo") + ")");
                        } else {
                            android.util.Log.d("ListaMantenimientos", "   ⏭️ Omitido (no asignado a este técnico)");
                        }
                    }

                } catch (Exception e) {
                    android.util.Log.e("ListaMantenimientos", "❌ Error al convertir documento #" + contador + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            android.util.Log.d("ListaMantenimientos", "========================================");
            android.util.Log.d("ListaMantenimientos", "📊 RESUMEN DE CARGA");
            android.util.Log.d("ListaMantenimientos", "📊 Total documentos Firestore: " + queryDocumentSnapshots.size());
            android.util.Log.d("ListaMantenimientos", "📊 Total agregados a lista: " + listaMantenimientos.size());
            android.util.Log.d("ListaMantenimientos", "========================================");

            // Si es técnico, también cargar mantenimientos donde es de apoyo
            if (!esAdmin) {
                cargarMantenimientosDeApoyo();
            } else {
                // Si es admin, continuar directamente
                cargarDatosRelacionados();
            }
        });
    }

    /**
     * Carga mantenimientos donde el técnico está como apoyo (segunda consulta).
     * Solo para técnicos.
     */
    private void cargarMantenimientosDeApoyo() {
        android.util.Log.d("ListaMantenimientos", "🔄 Cargando mantenimientos de apoyo...");

        db.collection("mantenimientos")
                .whereArrayContains("tecnicosApoyo", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int contadorApoyo = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Mantenimiento mantenimiento = doc.toObject(Mantenimiento.class);
                        mantenimiento.setMantenimientoId(doc.getId());

                        // Verificar que no esté duplicado (por si es principal Y apoyo)
                        boolean yaExiste = false;
                        for (MantenimientoDetallado d : listaMantenimientos) {
                            if (d.getMantenimiento().getMantenimientoId().equals(mantenimiento.getMantenimientoId())) {
                                yaExiste = true;
                                break;
                            }
                        }

                        if (!yaExiste) {
                            MantenimientoDetallado detallado = new MantenimientoDetallado(mantenimiento);
                            listaMantenimientos.add(detallado);
                            contadorApoyo++;
                            android.util.Log.d("ListaMantenimientos", "   ✅ Agregado como apoyo: " + doc.getId());
                        }
                    }

                    android.util.Log.d("ListaMantenimientos", "📊 Mantenimientos de apoyo agregados: " + contadorApoyo);
                    android.util.Log.d("ListaMantenimientos", "📊 Total final: " + listaMantenimientos.size());

                    // Ahora sí, cargar datos relacionados
                    cargarDatosRelacionados();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ListaMantenimientos", "❌ Error al cargar mantenimientos de apoyo: " + e.getMessage());
                    // Continuar con los que ya tenemos
                    cargarDatosRelacionados();
                });
    }

    /**
     * Carga todos los datos relacionados de equipos, clientes y técnicos de una sola vez.
     * Esto evita consultas N+1 y el error "permission denied" en el adapter.
     */
    private void cargarDatosRelacionados() {
        if (listaMantenimientos.isEmpty()) {
            android.util.Log.d("ListaMantenimientos", "📭 No hay mantenimientos para cargar datos relacionados");
            aplicarFiltros();
            mostrarCargando(false);
            return;
        }

        android.util.Log.d("ListaMantenimientos", "🔄 Cargando datos relacionados...");

        // Contador para saber cuándo terminaron todas las cargas
        AtomicInteger cargasPendientes = new AtomicInteger(listaMantenimientos.size() * 3); // equipo + cliente + tecnico

        for (MantenimientoDetallado detallado : listaMantenimientos) {
            Mantenimiento m = detallado.getMantenimiento();

            // Cargar datos del equipo
            db.collection("equipos").document(m.getEquipoId())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            detallado.setEquipoMarca(doc.getString("marca"));
                            detallado.setEquipoModelo(doc.getString("modelo"));
                        }
                        verificarCargaCompleta(cargasPendientes);
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.w("ListaMantenimientos", "Error al cargar equipo: " + e.getMessage());
                        verificarCargaCompleta(cargasPendientes);
                    });

            // Cargar datos del cliente
            db.collection("clientes").document(m.getClienteId())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            detallado.setClienteNombreEmpresa(doc.getString("nombreEmpresa"));
                            detallado.setClienteDireccion(doc.getString("direccion"));
                            detallado.setClienteEmail(doc.getString("emailContacto"));
                        }
                        verificarCargaCompleta(cargasPendientes);
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.w("ListaMantenimientos", "Error al cargar cliente: " + e.getMessage());
                        verificarCargaCompleta(cargasPendientes);
                    });

            // Cargar datos del técnico
            db.collection("usuarios").document(m.getTecnicoPrincipalId())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            detallado.setTecnicoNombre(doc.getString("nombre"));
                        }
                        verificarCargaCompleta(cargasPendientes);
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.w("ListaMantenimientos", "Error al cargar técnico: " + e.getMessage());
                        verificarCargaCompleta(cargasPendientes);
                    });
        }
    }

    /**
     * Verifica si todas las cargas de datos relacionados han terminado.
     * Cuando todas terminan, aplica filtros y oculta el loading.
     */
    private void verificarCargaCompleta(AtomicInteger cargasPendientes) {
        int pendientes = cargasPendientes.decrementAndGet();

        if (pendientes == 0) {
            android.util.Log.d("ListaMantenimientos", "✅ Todos los datos relacionados cargados");

            // Aplicar filtros
            aplicarFiltros();

            mostrarCargando(false);

            android.util.Log.d("ListaMantenimientos", "✅ Carga completada exitosamente");
        }
    }

    private void aplicarFiltros() {
        listaMantenimientosFiltrada.clear();

        for (MantenimientoDetallado detallado : listaMantenimientos) {
            Mantenimiento m = detallado.getMantenimiento();
            boolean cumpleFiltros = true;

            // Filtro por estado
            if (!"todos".equals(filtroEstado)) {
                if (!filtroEstado.equals(m.getEstado())) {
                    cumpleFiltros = false;
                }
            }

            // Filtro por prioridad
            if (!"todas".equals(filtroPrioridad)) {
                if (!filtroPrioridad.equals(m.getPrioridad())) {
                    cumpleFiltros = false;
                }
            }

            // Filtro por tipo
            if (!"todos".equals(filtroTipo)) {
                if (!filtroTipo.equals(m.getTipo())) {
                    cumpleFiltros = false;
                }
            }

            // Filtro por búsqueda - ahora puede buscar en datos relacionados
            if (!busqueda.isEmpty()) {
                String descripcion = m.getDescripcionServicio() != null ? m.getDescripcionServicio().toLowerCase() : "";
                String cliente = detallado.getClienteNombreEmpresa() != null ? detallado.getClienteNombreEmpresa().toLowerCase() : "";
                String equipo = detallado.getEquipoNombreCompleto().toLowerCase();

                // Buscar en descripción, cliente o equipo
                if (!descripcion.contains(busqueda) &&
                    !cliente.contains(busqueda) &&
                    !equipo.contains(busqueda)) {
                    cumpleFiltros = false;
                }
            }

            // Filtro por rango de fechas (fecha programada)
            if (fechaDesde != null && m.getFechaProgramada() != null) {
                if (m.getFechaProgramada().compareTo(fechaDesde) < 0) {
                    cumpleFiltros = false;
                }
            }

            if (fechaHasta != null && m.getFechaProgramada() != null) {
                if (m.getFechaProgramada().compareTo(fechaHasta) > 0) {
                    cumpleFiltros = false;
                }
            }

            if (cumpleFiltros) {
                listaMantenimientosFiltrada.add(detallado);
            }
        }

        // Ordenar por fecha programada (más próximos primero)
        java.util.Collections.sort(listaMantenimientosFiltrada, (d1, d2) -> {
            Mantenimiento m1 = d1.getMantenimiento();
            Mantenimiento m2 = d2.getMantenimiento();
            if (m1.getFechaProgramada() == null && m2.getFechaProgramada() == null) return 0;
            if (m1.getFechaProgramada() == null) return 1; // Los sin fecha van al final
            if (m2.getFechaProgramada() == null) return -1;
            return m1.getFechaProgramada().compareTo(m2.getFechaProgramada()); // Orden ascendente (próximos primero)
        });

        adapter.notifyDataSetChanged();
        actualizarContador();

        // Mostrar mensaje si no hay datos
        if (listaMantenimientosFiltrada.isEmpty()) {
            layoutSinDatos.setVisibility(View.VISIBLE);
            recyclerMantenimientos.setVisibility(View.GONE);
        } else {
            layoutSinDatos.setVisibility(View.GONE);
            recyclerMantenimientos.setVisibility(View.VISIBLE);
        }
    }

    private void actualizarContador() {
        int total = listaMantenimientosFiltrada.size();
        String texto = total == 1 ? "1 mantenimiento" : total + " mantenimientos";
        tvContador.setText(texto);
    }

    private void mostrarCargando(boolean mostrar) {
        progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        recyclerMantenimientos.setVisibility(mostrar ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Recargar lista después de crear
            cargarMantenimientos();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // El listener en tiempo real se encargará de actualizar automáticamente
        // Ya no necesitamos recargar manualmente
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remover listener para evitar memory leaks
        if (listenerMantenimientos != null) {
            listenerMantenimientos.remove();
            android.util.Log.d("ListaMantenimientos", "🔌 Listener removido");
        }
    }
}
