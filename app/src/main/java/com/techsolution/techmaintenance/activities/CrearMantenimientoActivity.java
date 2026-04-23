package com.techsolution.techmaintenance.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.adapters.EquipoSeleccionAdapter;
import com.techsolution.techmaintenance.helpers.NotificationHelper;
import com.techsolution.techmaintenance.models.Cliente;
import com.techsolution.techmaintenance.models.Equipo;
import com.techsolution.techmaintenance.models.Mantenimiento;
import com.techsolution.techmaintenance.models.Usuario;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CrearMantenimientoActivity extends AppCompatActivity {

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Vistas
    private Toolbar toolbar;
    private ViewFlipper viewFlipper;
    private TextView tvPaso1, tvPaso2, tvPaso3;
    private MaterialButton btnAnterior, btnSiguiente;

    // Paso 1: Cliente y Equipos
    private TextInputLayout tilCliente;
    private AutoCompleteTextView actvCliente;
    private RecyclerView rvEquipos;
    private TextView tvHelperEquipos, tvSinEquipos, tvContadorEquipos;
    private MaterialCardView cardContador;
    private EquipoSeleccionAdapter equipoAdapter;

    // Paso 2: Detalles
    private TextInputLayout tilTipo, tilDescripcion, tilFecha, tilHora, tilPrioridad;
    private AutoCompleteTextView actvTipo, actvPrioridad;
    private TextInputEditText etDescripcion, etFecha, etHora;

    // Paso 3: Técnicos
    private TextInputLayout tilTecnicoPrincipal;
    private AutoCompleteTextView actvTecnicoPrincipal;
    private LinearLayout containerTecnicosApoyo;
    private MaterialButton btnAgregarTecnicoApoyo;

    // Datos
    private List<Cliente> listaClientes;
    private List<Equipo> listaEquipos;
    private List<Usuario> listaTecnicos;
    private Map<String, Usuario> mapaTecnicosPorNombre; // Asocia nombre con técnico
    private List<AutoCompleteTextView> listaTecnicosApoyoViews;

    // Selecciones
    private Cliente clienteSeleccionado;
    private List<Equipo> equiposSeleccionados; // Ahora soporta múltiples equipos
    private Usuario tecnicoPrincipalSeleccionado;
    private List<String> tecnicosApoyoIds;

    // Fechas
    private Calendar calendarioSeleccionado;

    // Control
    private int pasoActual = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_mantenimiento);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Inicializar listas
        listaClientes = new ArrayList<>();
        listaEquipos = new ArrayList<>();
        listaTecnicos = new ArrayList<>();
        mapaTecnicosPorNombre = new HashMap<>(); // Nuevo: mapa de técnicos
        listaTecnicosApoyoViews = new ArrayList<>();
        tecnicosApoyoIds = new ArrayList<>();
        equiposSeleccionados = new ArrayList<>(); // Lista de equipos seleccionados
        calendarioSeleccionado = Calendar.getInstance();

        // Inicializar vistas
        inicializarVistas();

        // Configurar toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Configurar dropdowns
        configurarDropdowns();

        // Configurar listeners
        configurarListeners();

        // Cargar datos
        cargarClientes();
        cargarTecnicos();

        // Mostrar primer paso
        mostrarPaso(0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Recargar equipos si ya hay un cliente seleccionado
        // Esto actualiza los indicadores (✅/🔧) después de eliminar mantenimientos
        if (clienteSeleccionado != null) {
            android.util.Log.d("CrearMantenimiento", "🔄 onResume: Recargando equipos para actualizar indicadores");
            cargarEquiposDelCliente(clienteSeleccionado.getClienteId());
        }
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        viewFlipper = findViewById(R.id.viewFlipper);
        tvPaso1 = findViewById(R.id.tvPaso1);
        tvPaso2 = findViewById(R.id.tvPaso2);
        tvPaso3 = findViewById(R.id.tvPaso3);
        btnAnterior = findViewById(R.id.btnAnterior);
        btnSiguiente = findViewById(R.id.btnSiguiente);

        // Paso 1
        tilCliente = findViewById(R.id.tilCliente);
        actvCliente = findViewById(R.id.actvCliente);
        rvEquipos = findViewById(R.id.rvEquipos);
        tvHelperEquipos = findViewById(R.id.tvHelperEquipos);
        tvSinEquipos = findViewById(R.id.tvSinEquipos);
        tvContadorEquipos = findViewById(R.id.tvContadorEquipos);
        cardContador = findViewById(R.id.cardContador);

        // Configurar RecyclerView de equipos
        rvEquipos.setLayoutManager(new LinearLayoutManager(this));
        equipoAdapter = new EquipoSeleccionAdapter(cantidadSeleccionados -> {
            // Actualizar contador cuando cambia la selección
            actualizarContadorEquipos(cantidadSeleccionados);
        });
        rvEquipos.setAdapter(equipoAdapter);

        // Paso 2
        tilTipo = findViewById(R.id.tilTipo);
        tilDescripcion = findViewById(R.id.tilDescripcion);
        tilFecha = findViewById(R.id.tilFecha);
        tilHora = findViewById(R.id.tilHora);
        tilPrioridad = findViewById(R.id.tilPrioridad);
        actvTipo = findViewById(R.id.actvTipo);
        actvPrioridad = findViewById(R.id.actvPrioridad);
        etDescripcion = findViewById(R.id.etDescripcion);
        etFecha = findViewById(R.id.etFecha);
        etHora = findViewById(R.id.etHora);

        // Paso 3
        tilTecnicoPrincipal = findViewById(R.id.tilTecnicoPrincipal);
        actvTecnicoPrincipal = findViewById(R.id.actvTecnicoPrincipal);
        containerTecnicosApoyo = findViewById(R.id.containerTecnicosApoyo);
        btnAgregarTecnicoApoyo = findViewById(R.id.btnAgregarTecnicoApoyo);
    }

    private void configurarDropdowns() {
        // Tipo
        String[] tipos = {"Preventivo", "Correctivo", "Emergencia"};
        ArrayAdapter<String> adapterTipo = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, tipos);
        actvTipo.setAdapter(adapterTipo);

        // Prioridad
        String[] prioridades = {"Baja", "Media", "Alta", "Urgente"};
        ArrayAdapter<String> adapterPrioridad = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, prioridades);
        actvPrioridad.setAdapter(adapterPrioridad);
    }

    private void configurarListeners() {
        // Botones de navegación
        btnAnterior.setOnClickListener(v -> mostrarPaso(pasoActual - 1));
        btnSiguiente.setOnClickListener(v -> {
            if (validarPasoActual()) {
                if (pasoActual == 2) {
                    crearMantenimiento();
                } else {
                    mostrarPaso(pasoActual + 1);
                }
            }
        });

        // Cliente seleccionado -> Cargar equipos
        actvCliente.setOnItemClickListener((parent, view, position, id) -> {
            clienteSeleccionado = listaClientes.get(position);
            // Limpiar selección anterior de equipos
            equipoAdapter.limpiarSeleccion();
            cargarEquiposDelCliente(clienteSeleccionado.getClienteId());
        });

        // Fecha
        etFecha.setOnClickListener(v -> mostrarDatePicker());

        // Hora
        etHora.setOnClickListener(v -> mostrarTimePicker());

        // Técnico Principal
        actvTecnicoPrincipal.setOnItemClickListener((parent, view, position, id) -> {
            // Obtener el nombre seleccionado del dropdown (con indicadores)
            String nombreSeleccionado = (String) parent.getItemAtPosition(position);

            // Buscar el técnico correspondiente en el mapa
            Usuario tecnicoTemp = mapaTecnicosPorNombre.get(nombreSeleccionado);

            if (tecnicoTemp == null) {
                Toast.makeText(this, "Error: Técnico no encontrado", Toast.LENGTH_SHORT).show();
                return;
            }

            android.util.Log.d("CrearMantenimiento", "✅ Técnico seleccionado: " + tecnicoTemp.getNombre() + " (ID: " + tecnicoTemp.getUserId() + ")");

            // Validar solo si ya hay fecha y hora seleccionadas
            if (etFecha.getText().toString().isEmpty() || etHora.getText().toString().isEmpty()) {
                // Si no hay fecha/hora, solo guardar temporalmente
                tecnicoPrincipalSeleccionado = tecnicoTemp;
            } else {
                // Si ya hay fecha/hora, validar conflicto
                validarTecnicoDisponible(tecnicoTemp);
            }
        });

        // Agregar técnico de apoyo
        btnAgregarTecnicoApoyo.setOnClickListener(v -> agregarTecnicoApoyo());
    }

    private void cargarClientes() {
        db.collection("clientes")
                .orderBy("nombreEmpresa")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaClientes.clear();
                    List<String> nombresClientes = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Cliente cliente = doc.toObject(Cliente.class);
                        cliente.setClienteId(doc.getId());
                        listaClientes.add(cliente);
                        nombresClientes.add(cliente.getNombreEmpresa());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_dropdown_item_1line, nombresClientes);
                    actvCliente.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar clientes", Toast.LENGTH_SHORT).show();
                });
    }

    private void cargarEquiposDelCliente(String clienteId) {
        android.util.Log.d("CrearMantenimiento", "📥 Cargando equipos del cliente: " + clienteId);

        db.collection("equipos")
                .whereEqualTo("clienteId", clienteId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaEquipos.clear();
                    List<String> equiposOcupados = new ArrayList<>();

                    android.util.Log.d("CrearMantenimiento", "✅ Equipos encontrados: " + queryDocumentSnapshots.size());

                    if (queryDocumentSnapshots.isEmpty()) {
                        // Sin equipos
                        tvSinEquipos.setVisibility(View.VISIBLE);
                        rvEquipos.setVisibility(View.GONE);
                        cardContador.setVisibility(View.GONE);
                        tvHelperEquipos.setText("Este cliente no tiene equipos registrados");
                        return;
                    }

                    // Mostrar RecyclerView
                    tvSinEquipos.setVisibility(View.GONE);
                    rvEquipos.setVisibility(View.VISIBLE);
                    tvHelperEquipos.setText("Selecciona uno o varios equipos (✅ disponible, 🔧 ocupado)");

                    // Contador para saber cuándo terminar de verificar todos los equipos
                    final int totalEquipos = queryDocumentSnapshots.size();
                    final java.util.concurrent.atomic.AtomicInteger contador = new java.util.concurrent.atomic.AtomicInteger(0);

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Equipo equipo = doc.toObject(Equipo.class);
                        equipo.setEquipoId(doc.getId());
                        listaEquipos.add(equipo);

                        // Verificar si tiene mantenimientos activos
                        // IMPORTANTE: Usar Source.SERVER para evitar problemas de caché
                        db.collection("mantenimientos")
                                .whereEqualTo("equipoId", equipo.getEquipoId())
                                .whereIn("estado", java.util.Arrays.asList("programado", "en_proceso"))
                                .get(com.google.firebase.firestore.Source.SERVER)
                                .addOnSuccessListener(mntDocs -> {
                                    if (!mntDocs.isEmpty()) {
                                        equiposOcupados.add(equipo.getEquipoId());
                                        android.util.Log.d("CrearMantenimiento", "  🔧 Equipo ocupado: " + equipo.getTipo() + " - " + equipo.getMarca());
                                    } else {
                                        android.util.Log.d("CrearMantenimiento", "  ✅ Equipo disponible: " + equipo.getTipo() + " - " + equipo.getMarca());
                                    }

                                    // Verificar si ya procesamos todos los equipos
                                    if (contador.incrementAndGet() == totalEquipos) {
                                        // Actualizar adapter con todos los equipos y los ocupados
                                        equipoAdapter.setEquipos(listaEquipos);
                                        equipoAdapter.setEquiposOcupados(equiposOcupados);
                                        android.util.Log.d("CrearMantenimiento", "✅ RecyclerView de equipos configurado con " + listaEquipos.size() + " equipos");
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CrearMantenimiento", "❌ Error al cargar equipos: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar equipos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Actualiza el contador de equipos seleccionados
     */
    private void actualizarContadorEquipos(int cantidad) {
        if (cantidad > 0) {
            cardContador.setVisibility(View.VISIBLE);
            String texto = cantidad + (cantidad == 1 ? " equipo seleccionado" : " equipos seleccionados");
            tvContadorEquipos.setText(texto);
        } else {
            cardContador.setVisibility(View.GONE);
        }
    }

    private void cargarTecnicos() {
        android.util.Log.d("CrearMantenimiento", "📥 Cargando técnicos...");

        db.collection("usuarios")
                .whereEqualTo("rol", "tecnico")
                .whereEqualTo("estado", "activo")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaTecnicos.clear();
                    mapaTecnicosPorNombre.clear(); // Limpiar mapa
                    List<String> nombresTecnicos = new ArrayList<>();

                    android.util.Log.d("CrearMantenimiento", "✅ Técnicos encontrados: " + queryDocumentSnapshots.size());

                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No hay técnicos disponibles", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Obtener fechas de hoy
                    com.techsolution.techmaintenance.helpers.FirestoreHelper firestoreHelper =
                            new com.techsolution.techmaintenance.helpers.FirestoreHelper();
                    Timestamp inicioDia = firestoreHelper.getInicioDiaActual();
                    Timestamp finDia = firestoreHelper.getFinDiaActual();

                    // Contador para procesar todos los técnicos
                    final int totalTecnicos = queryDocumentSnapshots.size();
                    final java.util.concurrent.atomic.AtomicInteger contador = new java.util.concurrent.atomic.AtomicInteger(0);

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Usuario tecnico = doc.toObject(Usuario.class);
                        tecnico.setUserId(doc.getId());

                        // DEBUG: Log completo del técnico
                        android.util.Log.d("CrearMantenimiento", "🔍 ========================================");
                        android.util.Log.d("CrearMantenimiento", "🔍 DEBUG TÉCNICO CARGADO:");
                        android.util.Log.d("CrearMantenimiento", "   - Document ID: " + doc.getId());
                        android.util.Log.d("CrearMantenimiento", "   - getUserId(): " + tecnico.getUserId());
                        android.util.Log.d("CrearMantenimiento", "   - getNombre(): " + tecnico.getNombre());
                        android.util.Log.d("CrearMantenimiento", "   - getEmail(): " + tecnico.getEmail());
                        android.util.Log.d("CrearMantenimiento", "   - getRol(): " + tecnico.getRol());
                        android.util.Log.d("CrearMantenimiento", "   - getEstado(): " + tecnico.getEstado());
                        android.util.Log.d("CrearMantenimiento", "   - Documento raw:");
                        for (String key : doc.getData().keySet()) {
                            android.util.Log.d("CrearMantenimiento", "     * " + key + " = " + doc.get(key));
                        }
                        android.util.Log.d("CrearMantenimiento", "🔍 ========================================");

                        listaTecnicos.add(tecnico);

                        // Consultar cuántos servicios tiene hoy
                        db.collection("mantenimientos")
                                .whereEqualTo("tecnicoPrincipalId", tecnico.getUserId())
                                .whereGreaterThanOrEqualTo("fechaProgramada", inicioDia)
                                .whereLessThanOrEqualTo("fechaProgramada", finDia)
                                .whereIn("estado", java.util.Arrays.asList("programado", "en_proceso"))
                                .get()
                                .addOnSuccessListener(mntDocs -> {
                                    int cantidadServicios = mntDocs.size();
                                    String nombreConIndicador;

                                    // IMPORTANTE: Validar que el nombre no sea null
                                    String nombreTecnico = tecnico.getNombre();
                                    if (nombreTecnico == null || nombreTecnico.trim().isEmpty()) {
                                        android.util.Log.e("CrearMantenimiento", "❌ ERROR CRÍTICO: Nombre del técnico es NULL o vacío!");
                                        android.util.Log.e("CrearMantenimiento", "   - Técnico ID: " + tecnico.getUserId());
                                        android.util.Log.e("CrearMantenimiento", "   - Email: " + tecnico.getEmail());
                                        // Usar el email como fallback
                                        nombreTecnico = tecnico.getEmail() != null ? tecnico.getEmail().split("@")[0] : "Técnico sin nombre";
                                        android.util.Log.e("CrearMantenimiento", "   - Usando fallback: " + nombreTecnico);
                                    }

                                    if (cantidadServicios == 0) {
                                        // Sin servicios - verde
                                        nombreConIndicador = "🟢 " + nombreTecnico + " (disponible)";
                                        android.util.Log.d("CrearMantenimiento", "  🟢 " + nombreTecnico + " - 0 servicios");
                                    } else if (cantidadServicios <= 2) {
                                        // 1-2 servicios - amarillo
                                        nombreConIndicador = "🟡 " + nombreTecnico + " (" + cantidadServicios + " servicios)";
                                        android.util.Log.d("CrearMantenimiento", "  🟡 " + nombreTecnico + " - " + cantidadServicios + " servicios");
                                    } else {
                                        // 3+ servicios - rojo
                                        nombreConIndicador = "🔴 " + nombreTecnico + " (" + cantidadServicios + " servicios) ⚠️";
                                        android.util.Log.d("CrearMantenimiento", "  🔴 " + nombreTecnico + " - " + cantidadServicios + " servicios");
                                    }

                                    // Agregar al mapa: nombreConIndicador -> técnico
                                    mapaTecnicosPorNombre.put(nombreConIndicador, tecnico);
                                    nombresTecnicos.add(nombreConIndicador);

                                    android.util.Log.d("CrearMantenimiento", "  📋 Mapeado: '" + nombreConIndicador + "' -> " + tecnico.getNombre() + " (ID: " + tecnico.getUserId() + ")");

                                    // Verificar si ya procesamos todos los técnicos
                                    if (contador.incrementAndGet() == totalTecnicos) {
                                        // Ordenar alfabéticamente (manteniendo indicadores)
                                        nombresTecnicos.sort(String::compareToIgnoreCase);

                                        // Configurar adapter
                                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                                android.R.layout.simple_dropdown_item_1line, nombresTecnicos);
                                        actvTecnicoPrincipal.setAdapter(adapter);

                                        android.util.Log.d("CrearMantenimiento", "✅ Adapter configurado con " + nombresTecnicos.size() + " técnicos");
                                        android.util.Log.d("CrearMantenimiento", "✅ Mapa de técnicos: " + mapaTecnicosPorNombre.size() + " entradas");
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CrearMantenimiento", "❌ Error al cargar técnicos: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar técnicos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void mostrarDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendarioSeleccionado.set(Calendar.YEAR, year);
                    calendarioSeleccionado.set(Calendar.MONTH, month);
                    calendarioSeleccionado.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    etFecha.setText(sdf.format(calendarioSeleccionado.getTime()));

                    // Re-validar técnico si ya está seleccionado y también hay hora
                    if (tecnicoPrincipalSeleccionado != null && !etHora.getText().toString().isEmpty()) {
                        validarTecnicoDisponible(tecnicoPrincipalSeleccionado);
                    }
                },
                calendarioSeleccionado.get(Calendar.YEAR),
                calendarioSeleccionado.get(Calendar.MONTH),
                calendarioSeleccionado.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void mostrarTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    calendarioSeleccionado.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendarioSeleccionado.set(Calendar.MINUTE, minute);

                    String hora = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    etHora.setText(hora);

                    // Re-validar técnico si ya está seleccionado y también hay fecha
                    if (tecnicoPrincipalSeleccionado != null && !etFecha.getText().toString().isEmpty()) {
                        validarTecnicoDisponible(tecnicoPrincipalSeleccionado);
                    }
                },
                calendarioSeleccionado.get(Calendar.HOUR_OF_DAY),
                calendarioSeleccionado.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    private void agregarTecnicoApoyo() {
        if (listaTecnicosApoyoViews.size() >= 3) {
            Toast.makeText(this, "Máximo 3 técnicos de apoyo", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear nuevo dropdown
        TextInputLayout tilApoyo = new TextInputLayout(this, null,
                com.google.android.material.R.attr.textInputFilledStyle);
        tilApoyo.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        tilApoyo.setHint("Técnico de Apoyo " + (listaTecnicosApoyoViews.size() + 1));

        AutoCompleteTextView actvApoyo = new AutoCompleteTextView(this);
        actvApoyo.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        List<String> nombresTecnicos = new ArrayList<>();
        for (Usuario tecnico : listaTecnicos) {
            nombresTecnicos.add(tecnico.getNombre());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, nombresTecnicos);
        actvApoyo.setAdapter(adapter);

        // Agregar listener para validar selección
        actvApoyo.setOnItemClickListener((parent, view, position, id) -> {
            String nombreSeleccionado = nombresTecnicos.get(position);
            validarTecnicoApoyo(nombreSeleccionado, actvApoyo, tilApoyo);
        });

        tilApoyo.addView(actvApoyo);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = 16;
        containerTecnicosApoyo.addView(tilApoyo, params);

        listaTecnicosApoyoViews.add(actvApoyo);
    }

    /**
     * Valida que el técnico de apoyo seleccionado sea válido
     */
    private void validarTecnicoApoyo(String nombreTecnico, AutoCompleteTextView actv, TextInputLayout til) {
        android.util.Log.d("CrearMantenimiento", "🔍 Validando técnico de apoyo: " + nombreTecnico);

        // 1. Verificar que no sea el técnico principal
        if (tecnicoPrincipalSeleccionado != null && tecnicoPrincipalSeleccionado.getNombre().equals(nombreTecnico)) {
            android.util.Log.d("CrearMantenimiento", "❌ Es el técnico principal");
            til.setError("No puede ser el técnico principal");
            new AlertDialog.Builder(this)
                    .setTitle("⚠️ Técnico Duplicado")
                    .setMessage("El técnico " + nombreTecnico + " ya está asignado como técnico principal.\n\n" +
                            "Un mismo técnico no puede ser principal y de apoyo simultáneamente.")
                    .setPositiveButton("Entendido", (dialog, which) -> {
                        actv.setText("");
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return;
        }

        // 2. Verificar que no esté duplicado en otros técnicos de apoyo
        for (AutoCompleteTextView otroActv : listaTecnicosApoyoViews) {
            if (otroActv != actv) { // No comparar consigo mismo
                String otroNombre = otroActv.getText().toString().trim();
                if (otroNombre.equals(nombreTecnico)) {
                    android.util.Log.d("CrearMantenimiento", "❌ Técnico de apoyo duplicado");
                    til.setError("Técnico ya agregado");
                    new AlertDialog.Builder(this)
                            .setTitle("⚠️ Técnico Duplicado")
                            .setMessage("El técnico " + nombreTecnico + " ya está agregado como técnico de apoyo.\n\n" +
                                    "No puedes seleccionar el mismo técnico dos veces.")
                            .setPositiveButton("Entendido", (dialog, which) -> {
                                actv.setText("");
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    return;
                }
            }
        }

        // 3. Buscar el usuario completo en la lista
        Usuario tecnicoSeleccionado = null;
        for (Usuario tecnico : listaTecnicos) {
            if (tecnico.getNombre().equals(nombreTecnico)) {
                tecnicoSeleccionado = tecnico;
                break;
            }
        }

        if (tecnicoSeleccionado == null) {
            android.util.Log.e("CrearMantenimiento", "❌ Técnico no encontrado en la lista");
            return;
        }

        // 4. Validar conflictos de horario (solo si hay fecha y hora)
        if (!etFecha.getText().toString().isEmpty() && !etHora.getText().toString().isEmpty()) {
            android.util.Log.d("CrearMantenimiento", "🔍 Validando conflictos de horario para técnico de apoyo");
            Usuario finalTecnicoSeleccionado = tecnicoSeleccionado;
            validarTecnicoDisponibleParaApoyo(tecnicoSeleccionado, actv, til);
        } else {
            // Sin fecha/hora, solo validar duplicados (ya hecho arriba)
            til.setError(null);
            til.setHelperText("Técnico de apoyo agregado");
            android.util.Log.d("CrearMantenimiento", "✅ Técnico de apoyo válido (sin validar horario)");
        }
    }

    /**
     * Valida conflictos de horario para técnico de apoyo
     */
    private void validarTecnicoDisponibleParaApoyo(Usuario tecnico, AutoCompleteTextView actv, TextInputLayout til) {
        // Misma lógica que validarTecnicoDisponible pero con feedback diferente
        Calendar inicioDia = (Calendar) calendarioSeleccionado.clone();
        inicioDia.set(Calendar.HOUR_OF_DAY, 0);
        inicioDia.set(Calendar.MINUTE, 0);
        inicioDia.set(Calendar.SECOND, 0);

        Calendar finDia = (Calendar) calendarioSeleccionado.clone();
        finDia.set(Calendar.HOUR_OF_DAY, 23);
        finDia.set(Calendar.MINUTE, 59);
        finDia.set(Calendar.SECOND, 59);

        Timestamp inicioTimestamp = new Timestamp(inicioDia.getTime());
        Timestamp finTimestamp = new Timestamp(finDia.getTime());

        db.collection("mantenimientos")
                .whereEqualTo("tecnicoPrincipalId", tecnico.getUserId())
                .whereGreaterThanOrEqualTo("fechaProgramada", inicioTimestamp)
                .whereLessThanOrEqualTo("fechaProgramada", finTimestamp)
                .whereIn("estado", java.util.Arrays.asList("programado", "en_proceso"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        til.setError(null);
                        til.setHelperText("Disponible");
                        android.util.Log.d("CrearMantenimiento", "✅ Técnico de apoyo disponible");
                        return;
                    }

                    // Verificar conflictos de horario
                    int horaSeleccionada = calendarioSeleccionado.get(Calendar.HOUR_OF_DAY);
                    int minutoSeleccionado = calendarioSeleccionado.get(Calendar.MINUTE);
                    int minutosTotalesSeleccionados = (horaSeleccionada * 60) + minutoSeleccionado;

                    final int MARGEN_MINUTOS = 120;

                    for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                        Mantenimiento mantenimientoExistente = queryDocumentSnapshots.getDocuments().get(i).toObject(Mantenimiento.class);

                        if (mantenimientoExistente.getFechaProgramada() != null) {
                            Calendar calExistente = Calendar.getInstance();
                            calExistente.setTime(mantenimientoExistente.getFechaProgramada().toDate());

                            int horaExistente = calExistente.get(Calendar.HOUR_OF_DAY);
                            int minutoExistente = calExistente.get(Calendar.MINUTE);
                            int minutosTotalesExistentes = (horaExistente * 60) + minutoExistente;

                            int diferenciaMinutos = Math.abs(minutosTotalesSeleccionados - minutosTotalesExistentes);

                            if (diferenciaMinutos < MARGEN_MINUTOS) {
                                // Conflicto detectado
                                SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm", Locale.getDefault());
                                String horaExistenteStr = sdfHora.format(calExistente.getTime());

                                til.setError("Conflicto de horario");
                                new AlertDialog.Builder(this)
                                        .setTitle("⚠️ Conflicto de Horario")
                                        .setMessage("El técnico de apoyo " + tecnico.getNombre() + " tiene un servicio a las " + horaExistenteStr + " ese día.\n\n" +
                                                "Diferencia: " + diferenciaMinutos + " minutos\n" +
                                                "Mínimo requerido: " + MARGEN_MINUTOS + " minutos\n\n" +
                                                "¿Deseas seleccionarlo de todas formas?")
                                        .setPositiveButton("Sí, continuar", (dialog, which) -> {
                                            til.setError(null);
                                            til.setHelperText("Advertencia: Horario ajustado");
                                        })
                                        .setNegativeButton("No, cambiar técnico", (dialog, which) -> {
                                            actv.setText("");
                                            til.setError(null);
                                        })
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .show();
                                return;
                            }
                        }
                    }

                    // Sin conflictos
                    til.setError(null);
                    til.setHelperText("Disponible (" + queryDocumentSnapshots.size() + " servicio(s) ese día)");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CrearMantenimiento", "❌ Error al validar técnico de apoyo: " + e.getMessage());
                });
    }

    private void mostrarPaso(int paso) {
        pasoActual = paso;
        viewFlipper.setDisplayedChild(paso);

        // Actualizar indicadores
        tvPaso1.setTextColor(getResources().getColor(paso == 0 ? R.color.primary : R.color.secondary_text));
        tvPaso2.setTextColor(getResources().getColor(paso == 1 ? R.color.primary : R.color.secondary_text));
        tvPaso3.setTextColor(getResources().getColor(paso == 2 ? R.color.primary : R.color.secondary_text));

        tvPaso1.setTextAppearance(paso == 0 ? android.R.style.TextAppearance_Medium : android.R.style.TextAppearance_Small);
        tvPaso2.setTextAppearance(paso == 1 ? android.R.style.TextAppearance_Medium : android.R.style.TextAppearance_Small);
        tvPaso3.setTextAppearance(paso == 2 ? android.R.style.TextAppearance_Medium : android.R.style.TextAppearance_Small);

        // Mostrar/ocultar botones
        btnAnterior.setVisibility(paso == 0 ? View.GONE : View.VISIBLE);
        btnSiguiente.setText(paso == 2 ? "CREAR MANTENIMIENTO" : "SIGUIENTE");
    }

    private boolean validarPasoActual() {
        switch (pasoActual) {
            case 0: // Cliente y Equipos (múltiples)
                if (clienteSeleccionado == null) {
                    tilCliente.setError("Selecciona un cliente");
                    return false;
                }
                tilCliente.setError(null);

                // Obtener equipos seleccionados del adapter
                equiposSeleccionados = equipoAdapter.getEquiposSeleccionados();
                if (equiposSeleccionados.isEmpty()) {
                    tvHelperEquipos.setText("⚠️ Selecciona al menos un equipo");
                    tvHelperEquipos.setTextColor(getColor(android.R.color.holo_red_dark));
                    return false;
                }
                tvHelperEquipos.setTextColor(getColor(R.color.secondary_text));
                return true;

            case 1: // Detalles
                String tipo = actvTipo.getText().toString().trim();
                if (tipo.isEmpty()) {
                    tilTipo.setError("Selecciona un tipo");
                    return false;
                }
                tilTipo.setError(null);

                String descripcion = etDescripcion.getText().toString().trim();
                if (descripcion.isEmpty()) {
                    tilDescripcion.setError("Ingresa una descripción");
                    return false;
                }
                tilDescripcion.setError(null);

                String fecha = etFecha.getText().toString().trim();
                if (fecha.isEmpty()) {
                    tilFecha.setError("Selecciona una fecha");
                    return false;
                }
                tilFecha.setError(null);

                String hora = etHora.getText().toString().trim();
                if (hora.isEmpty()) {
                    tilHora.setError("Selecciona una hora");
                    return false;
                }
                tilHora.setError(null);

                String prioridad = actvPrioridad.getText().toString().trim();
                if (prioridad.isEmpty()) {
                    tilPrioridad.setError("Selecciona una prioridad");
                    return false;
                }
                tilPrioridad.setError(null);
                return true;

            case 2: // Técnicos
                if (tecnicoPrincipalSeleccionado == null) {
                    tilTecnicoPrincipal.setError("Selecciona un técnico principal");
                    return false;
                }
                tilTecnicoPrincipal.setError(null);
                return true;

            default:
                return true;
        }
    }

    /**
     * Valida si el técnico está disponible en la fecha/hora seleccionada
     * Verifica que no tenga otro servicio dentro de un margen de 2 horas
     */
    private void validarTecnicoDisponible(Usuario tecnico) {
        android.util.Log.d("CrearMantenimiento", "🔍 Validando disponibilidad del técnico: " + tecnico.getNombre());

        // Obtener inicio y fin del día seleccionado
        Calendar inicioDia = (Calendar) calendarioSeleccionado.clone();
        inicioDia.set(Calendar.HOUR_OF_DAY, 0);
        inicioDia.set(Calendar.MINUTE, 0);
        inicioDia.set(Calendar.SECOND, 0);

        Calendar finDia = (Calendar) calendarioSeleccionado.clone();
        finDia.set(Calendar.HOUR_OF_DAY, 23);
        finDia.set(Calendar.MINUTE, 59);
        finDia.set(Calendar.SECOND, 59);

        Timestamp inicioTimestamp = new Timestamp(inicioDia.getTime());
        Timestamp finTimestamp = new Timestamp(finDia.getTime());

        android.util.Log.d("CrearMantenimiento", "📅 Buscando mantenimientos entre " + inicioDia.getTime() + " y " + finDia.getTime());

        // Query: Buscar mantenimientos del técnico en el mismo día
        db.collection("mantenimientos")
                .whereEqualTo("tecnicoPrincipalId", tecnico.getUserId())
                .whereGreaterThanOrEqualTo("fechaProgramada", inicioTimestamp)
                .whereLessThanOrEqualTo("fechaProgramada", finTimestamp)
                .whereIn("estado", java.util.Arrays.asList("programado", "en_proceso"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    android.util.Log.d("CrearMantenimiento", "✅ Mantenimientos encontrados ese día: " + queryDocumentSnapshots.size());

                    if (queryDocumentSnapshots.isEmpty()) {
                        // ✅ No hay otros servicios ese día
                        android.util.Log.d("CrearMantenimiento", "✅ Técnico completamente libre ese día");
                        tecnicoPrincipalSeleccionado = tecnico;
                        tilTecnicoPrincipal.setError(null);
                        tilTecnicoPrincipal.setHelperText("Técnico disponible");
                        return;
                    }

                    // Verificar conflictos de horario
                    int horaSeleccionada = calendarioSeleccionado.get(Calendar.HOUR_OF_DAY);
                    int minutoSeleccionado = calendarioSeleccionado.get(Calendar.MINUTE);
                    int minutosTotalesSeleccionados = (horaSeleccionada * 60) + minutoSeleccionado;

                    android.util.Log.d("CrearMantenimiento", "⏰ Hora seleccionada: " + horaSeleccionada + ":" + minutoSeleccionado + " (" + minutosTotalesSeleccionados + " minutos)");

                    // Margen de seguridad: 2 horas = 120 minutos
                    final int MARGEN_MINUTOS = 120;

                    for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                        Mantenimiento mantenimientoExistente = queryDocumentSnapshots.getDocuments().get(i).toObject(Mantenimiento.class);
                        String mantenimientoId = queryDocumentSnapshots.getDocuments().get(i).getId();

                        if (mantenimientoExistente.getFechaProgramada() != null) {
                            Calendar calExistente = Calendar.getInstance();
                            calExistente.setTime(mantenimientoExistente.getFechaProgramada().toDate());

                            int horaExistente = calExistente.get(Calendar.HOUR_OF_DAY);
                            int minutoExistente = calExistente.get(Calendar.MINUTE);
                            int minutosTotalesExistentes = (horaExistente * 60) + minutoExistente;

                            int diferenciaMinutos = Math.abs(minutosTotalesSeleccionados - minutosTotalesExistentes);

                            android.util.Log.d("CrearMantenimiento", "  📊 Servicio existente #" + (i+1) + ": " + horaExistente + ":" + minutoExistente + " (" + minutosTotalesExistentes + " minutos)");
                            android.util.Log.d("CrearMantenimiento", "  ⏱️ Diferencia: " + diferenciaMinutos + " minutos");

                            if (diferenciaMinutos < MARGEN_MINUTOS) {
                                // ❌ CONFLICTO DETECTADO
                                android.util.Log.d("CrearMantenimiento", "❌ CONFLICTO: Diferencia menor a " + MARGEN_MINUTOS + " minutos");

                                // Formatear horas
                                SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm", Locale.getDefault());
                                String horaExistenteStr = sdfHora.format(calExistente.getTime());
                                String horaSeleccionadaStr = String.format(Locale.getDefault(), "%02d:%02d", horaSeleccionada, minutoSeleccionado);

                                // Calcular siguiente hora disponible
                                Calendar siguienteDisponible = (Calendar) calExistente.clone();
                                siguienteDisponible.add(Calendar.MINUTE, MARGEN_MINUTOS);
                                String siguienteHoraStr = sdfHora.format(siguienteDisponible.getTime());

                                // Cargar datos del equipo del mantenimiento existente
                                int finalI = i;
                                db.collection("equipos").document(mantenimientoExistente.getEquipoId())
                                        .get()
                                        .addOnSuccessListener(equipoDoc -> {
                                            final String nombreEquipo = equipoDoc.exists() ?
                                                    equipoDoc.getString("marca") + " " + equipoDoc.getString("modelo") :
                                                    "Equipo desconocido";

                                            // Cargar datos del cliente
                                            db.collection("clientes").document(mantenimientoExistente.getClienteId())
                                                    .get()
                                                    .addOnSuccessListener(clienteDoc -> {
                                                        final String nombreCliente = clienteDoc.exists() ? clienteDoc.getString("nombreEmpresa") : "Cliente desconocido";

                                                        // Obtener duración estimada del servicio existente basado en prioridad
                                                        String prioridadExistente = mantenimientoExistente.getPrioridad();
                                                        String duracionEstimada = obtenerDuracionEstimadaPorPrioridad(prioridadExistente);

                                                        // Calcular hora de fin estimada
                                                        Calendar finEstimado = (Calendar) calExistente.clone();
                                                        finEstimado.add(Calendar.MINUTE, obtenerMinutosPorPrioridad(prioridadExistente));
                                                        String horaFinEstimadaStr = sdfHora.format(finEstimado.getTime());

                                                        // Mostrar diálogo de conflicto con sistema FLEXIBLE
                                                        new AlertDialog.Builder(this)
                                                                .setTitle("⚠️ Advertencia: Técnico con Servicios")
                                                                .setMessage("El técnico " + tecnico.getNombre() + " ya tiene un servicio programado cerca de la hora seleccionada:\n\n" +
                                                                        "📋 SERVICIO EXISTENTE:\n" +
                                                                        "📅 Fecha: " + etFecha.getText().toString() + "\n" +
                                                                        "⏰ Inicio: " + horaExistenteStr + "\n" +
                                                                        "⏰ Fin estimado: " + horaFinEstimadaStr + "\n" +
                                                                        "⏱️ Duración estimada: " + duracionEstimada + "\n" +
                                                                        "🚨 Prioridad: " + (prioridadExistente != null ? prioridadExistente.toUpperCase() : "MEDIA") + "\n" +
                                                                        "🏢 Cliente: " + nombreCliente + "\n" +
                                                                        "💻 Equipo: " + nombreEquipo + "\n\n" +
                                                                        "📋 TU NUEVO SERVICIO:\n" +
                                                                        "⏰ Hora seleccionada: " + horaSeleccionadaStr + "\n" +
                                                                        "⚠️ Diferencia: " + diferenciaMinutos + " minutos\n\n" +
                                                                        "━━━━━━━━━━━━━━━━━━━━\n" +
                                                                        "💡 RECOMENDACIÓN:\n" +
                                                                        "• Mínimo sugerido: " + MARGEN_MINUTOS + " minutos de separación\n" +
                                                                        "• Siguiente horario disponible: " + siguienteHoraStr + "\n\n" +
                                                                        "⚠️ ¿Deseas asignar de todas formas?\n" +
                                                                        "(A veces las empresas necesitan que el técnico maneje múltiples tareas)")
                                                                .setPositiveButton("✅ Sí, asignar de todas formas", (dialog, which) -> {
                                                                    // SISTEMA FLEXIBLE: Permite continuar con advertencia
                                                                    android.util.Log.d("CrearMantenimiento", "⚠️ Admin decidió asignar a pesar del conflicto");
                                                                    tecnicoPrincipalSeleccionado = tecnico;
                                                                    tilTecnicoPrincipal.setError(null);
                                                                    tilTecnicoPrincipal.setHelperText("⚠️ Técnico con " + queryDocumentSnapshots.size() + " servicio(s) ese día (Advertencia aceptada)");
                                                                    Toast.makeText(this, "⚠️ Técnico asignado con conflicto de horario", Toast.LENGTH_SHORT).show();
                                                                })
                                                                .setNeutralButton("📅 Ver Agenda Completa", (dialog, which) -> {
                                                                    mostrarAgendaTecnicoConDuraciones(tecnico);
                                                                })
                                                                .setNegativeButton("❌ Cambiar Técnico", (dialog, which) -> {
                                                                    actvTecnicoPrincipal.setText("");
                                                                    tecnicoPrincipalSeleccionado = null;
                                                                    tilTecnicoPrincipal.setError("Selecciona otro técnico");
                                                                })
                                                                .setIcon(android.R.drawable.ic_dialog_alert)
                                                                .setCancelable(false)
                                                                .show();
                                                    });
                                        });

                                return; // Detener validación (conflicto encontrado)
                            }
                        }
                    }

                    // ✅ No hay conflictos
                    android.util.Log.d("CrearMantenimiento", "✅ No hay conflictos - Técnico disponible");
                    tecnicoPrincipalSeleccionado = tecnico;
                    tilTecnicoPrincipal.setError(null);
                    tilTecnicoPrincipal.setHelperText("Técnico disponible (" + queryDocumentSnapshots.size() + " servicio(s) ese día)");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CrearMantenimiento", "❌ Error al validar técnico: " + e.getMessage());
                    Toast.makeText(this, "Error al validar disponibilidad: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Muestra la agenda completa del técnico para el día seleccionado
     */
    private void mostrarAgendaTecnico(Usuario tecnico) {
        // Obtener inicio y fin del día
        Calendar inicioDia = (Calendar) calendarioSeleccionado.clone();
        inicioDia.set(Calendar.HOUR_OF_DAY, 0);
        inicioDia.set(Calendar.MINUTE, 0);

        Calendar finDia = (Calendar) calendarioSeleccionado.clone();
        finDia.set(Calendar.HOUR_OF_DAY, 23);
        finDia.set(Calendar.MINUTE, 59);

        Timestamp inicioTimestamp = new Timestamp(inicioDia.getTime());
        Timestamp finTimestamp = new Timestamp(finDia.getTime());

        // Query para obtener todos los mantenimientos del día
        db.collection("mantenimientos")
                .whereEqualTo("tecnicoPrincipalId", tecnico.getUserId())
                .whereGreaterThanOrEqualTo("fechaProgramada", inicioTimestamp)
                .whereLessThanOrEqualTo("fechaProgramada", finTimestamp)
                .whereIn("estado", java.util.Arrays.asList("programado", "en_proceso"))
                .orderBy("fechaProgramada")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    StringBuilder agenda = new StringBuilder();
                    agenda.append("Agenda de ").append(tecnico.getNombre()).append("\n");
                    agenda.append("📅 ").append(etFecha.getText().toString()).append("\n\n");

                    if (queryDocumentSnapshots.isEmpty()) {
                        agenda.append("✅ Sin servicios programados");
                    } else {
                        SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm", Locale.getDefault());

                        for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                            Mantenimiento mant = queryDocumentSnapshots.getDocuments().get(i).toObject(Mantenimiento.class);

                            if (mant.getFechaProgramada() != null) {
                                String hora = sdfHora.format(mant.getFechaProgramada().toDate());
                                agenda.append("⏰ ").append(hora).append(" - ");
                                agenda.append(obtenerTipoFormateado(mant.getTipo())).append("\n");

                                if (i < queryDocumentSnapshots.size() - 1) {
                                    agenda.append("   ↓\n");
                                }
                            }
                        }
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("📅 Agenda del Técnico")
                            .setMessage(agenda.toString())
                            .setPositiveButton("Entendido", null)
                            .show();
                });
    }

    /**
     * Formatea el estado para mostrar
     */
    private String obtenerEstadoFormateado(String estado) {
        if (estado == null) return "Desconocido";
        switch (estado.toLowerCase()) {
            case "programado": return "Programado";
            case "en_proceso": return "En Proceso";
            case "completado": return "Completado";
            case "cancelado": return "Cancelado";
            default: return estado;
        }
    }

    /**
     * Formatea el tipo para mostrar
     */
    private String obtenerTipoFormateado(String tipo) {
        if (tipo == null) return "General";
        switch (tipo.toLowerCase()) {
            case "preventivo": return "Preventivo";
            case "correctivo": return "Correctivo";
            case "emergencia": return "Emergencia";
            default: return tipo;
        }
    }

    /**
     * Obtiene la duración estimada en texto basada en la prioridad
     */
    private String obtenerDuracionEstimadaPorPrioridad(String prioridad) {
        if (prioridad == null) return "2 horas";
        switch (prioridad.toLowerCase()) {
            case "baja": return "1 hora";
            case "media": return "2 horas";
            case "alta": return "3 horas";
            case "urgente": return "4 horas";
            default: return "2 horas";
        }
    }

    /**
     * Obtiene los minutos estimados basado en la prioridad
     */
    private int obtenerMinutosPorPrioridad(String prioridad) {
        if (prioridad == null) return 120;
        switch (prioridad.toLowerCase()) {
            case "baja": return 60;    // 1 hora
            case "media": return 120;  // 2 horas
            case "alta": return 180;   // 3 horas
            case "urgente": return 240;  // 4 horas
            default: return 120;
        }
    }

    /**
     * Muestra la agenda completa del técnico con duraciones estimadas
     */
    private void mostrarAgendaTecnicoConDuraciones(Usuario tecnico) {
        // Obtener inicio y fin del día
        Calendar inicioDia = (Calendar) calendarioSeleccionado.clone();
        inicioDia.set(Calendar.HOUR_OF_DAY, 0);
        inicioDia.set(Calendar.MINUTE, 0);

        Calendar finDia = (Calendar) calendarioSeleccionado.clone();
        finDia.set(Calendar.HOUR_OF_DAY, 23);
        finDia.set(Calendar.MINUTE, 59);

        Timestamp inicioTimestamp = new Timestamp(inicioDia.getTime());
        Timestamp finTimestamp = new Timestamp(finDia.getTime());

        // Query para obtener todos los mantenimientos del día
        db.collection("mantenimientos")
                .whereEqualTo("tecnicoPrincipalId", tecnico.getUserId())
                .whereGreaterThanOrEqualTo("fechaProgramada", inicioTimestamp)
                .whereLessThanOrEqualTo("fechaProgramada", finTimestamp)
                .whereIn("estado", java.util.Arrays.asList("programado", "en_proceso"))
                .orderBy("fechaProgramada")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    StringBuilder agenda = new StringBuilder();
                    agenda.append("═══════════════════════════════\n");
                    agenda.append("📅 AGENDA COMPLETA DEL TÉCNICO\n");
                    agenda.append("═══════════════════════════════\n\n");
                    agenda.append("👤 Técnico: ").append(tecnico.getNombre()).append("\n");
                    agenda.append("📅 Fecha: ").append(etFecha.getText().toString()).append("\n");
                    agenda.append("📊 Total de servicios: ").append(queryDocumentSnapshots.size()).append("\n\n");

                    if (queryDocumentSnapshots.isEmpty()) {
                        agenda.append("✅ SIN SERVICIOS PROGRAMADOS\n\n");
                        agenda.append("El técnico está completamente libre este día.");
                    } else {
                        agenda.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
                        SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm", Locale.getDefault());

                        for (int i = 0; i < queryDocumentSnapshots.size(); i++) {
                            Mantenimiento mant = queryDocumentSnapshots.getDocuments().get(i).toObject(Mantenimiento.class);

                            if (mant.getFechaProgramada() != null) {
                                String horaInicio = sdfHora.format(mant.getFechaProgramada().toDate());

                                // Calcular hora de fin estimada
                                Calendar finEstimado = Calendar.getInstance();
                                finEstimado.setTime(mant.getFechaProgramada().toDate());
                                finEstimado.add(Calendar.MINUTE, obtenerMinutosPorPrioridad(mant.getPrioridad()));
                                String horaFin = sdfHora.format(finEstimado.getTime());

                                String duracion = obtenerDuracionEstimadaPorPrioridad(mant.getPrioridad());
                                String prioridad = mant.getPrioridad() != null ? mant.getPrioridad().toUpperCase() : "MEDIA";
                                String emoji = obtenerEmojiPorPrioridad(mant.getPrioridad());

                                agenda.append("📍 SERVICIO #").append(i + 1).append("\n");
                                agenda.append("⏰ ").append(horaInicio).append(" - ").append(horaFin).append(" (").append(duracion).append(")\n");
                                agenda.append("🏷️ ").append(obtenerTipoFormateado(mant.getTipo())).append("\n");
                                agenda.append(emoji).append(" Prioridad: ").append(prioridad).append("\n");

                                if (i < queryDocumentSnapshots.size() - 1) {
                                    // Calcular tiempo libre entre servicios
                                    Mantenimiento siguienteMant = queryDocumentSnapshots.getDocuments().get(i + 1).toObject(Mantenimiento.class);
                                    if (siguienteMant.getFechaProgramada() != null) {
                                        long diferenciaMillis = siguienteMant.getFechaProgramada().toDate().getTime() - finEstimado.getTime().getTime();
                                        long diferenciaMinutos = diferenciaMillis / (1000 * 60);

                                        if (diferenciaMinutos > 0) {
                                            agenda.append("   ⏬ (").append(diferenciaMinutos).append(" min libre)\n\n");
                                        } else {
                                            agenda.append("   ⚠️ (¡POSIBLE CONFLICTO!)\n\n");
                                        }
                                    }
                                } else {
                                    agenda.append("\n");
                                }
                            }
                        }

                        agenda.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                        agenda.append("\n💡 Duraciones estimadas según prioridad:\n");
                        agenda.append("   • Baja: 1 hora\n");
                        agenda.append("   • Media: 2 horas\n");
                        agenda.append("   • Alta: 3 horas\n");
                        agenda.append("   • Urgente: 4 horas");
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("📅 Agenda Detallada")
                            .setMessage(agenda.toString())
                            .setPositiveButton("Cerrar", null)
                            .show();
                });
    }

    /**
     * Obtiene emoji basado en prioridad
     */
    private String obtenerEmojiPorPrioridad(String prioridad) {
        if (prioridad == null) return "🔵";
        switch (prioridad.toLowerCase()) {
            case "baja": return "🟢";
            case "media": return "🟡";
            case "alta": return "🟠";
            case "urgente": return "🔴";
            default: return "🔵";
        }
    }

    private void crearMantenimiento() {
        btnSiguiente.setEnabled(false);

        // Recopilar técnicos de apoyo
        tecnicosApoyoIds.clear();
        for (AutoCompleteTextView actv : listaTecnicosApoyoViews) {
            String nombre = actv.getText().toString().trim();
            for (Usuario tecnico : listaTecnicos) {
                if (tecnico.getNombre().equals(nombre)) {
                    tecnicosApoyoIds.add(tecnico.getUserId());
                    break;
                }
            }
        }

        // Convertir tipo y prioridad a minúsculas
        String tipoFinal = actvTipo.getText().toString().trim().toLowerCase();
        String prioridadTexto = actvPrioridad.getText().toString().trim();
        String prioridadFinal = prioridadTexto.equalsIgnoreCase("Baja") ? "baja" :
                prioridadTexto.equalsIgnoreCase("Media") ? "media" :
                        prioridadTexto.equalsIgnoreCase("Alta") ? "alta" : "urgente";

        // Crear Timestamp
        Timestamp fechaProgramada = new Timestamp(calendarioSeleccionado.getTime());

        // Obtener cantidad de equipos seleccionados
        final int totalEquipos = equiposSeleccionados.size();

        // Mostrar diálogo de progreso
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setTitle("Creando Mantenimientos");
        progressDialog.setMessage("Creando 0 de " + totalEquipos + "...");
        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(totalEquipos);
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Contador de mantenimientos creados
        final java.util.concurrent.atomic.AtomicInteger contadorCreados = new java.util.concurrent.atomic.AtomicInteger(0);
        final java.util.concurrent.atomic.AtomicInteger contadorErrores = new java.util.concurrent.atomic.AtomicInteger(0);

        // Crear mantenimiento para cada equipo seleccionado
        for (Equipo equipo : equiposSeleccionados) {
            Mantenimiento mantenimiento = new Mantenimiento();
            mantenimiento.setClienteId(clienteSeleccionado.getClienteId());
            mantenimiento.setEquipoId(equipo.getEquipoId());
            mantenimiento.setTecnicoPrincipalId(tecnicoPrincipalSeleccionado.getUserId());
            mantenimiento.setTecnicosApoyo(tecnicosApoyoIds);
            mantenimiento.setTipo(tipoFinal);
            mantenimiento.setDescripcionServicio(etDescripcion.getText().toString().trim());
            mantenimiento.setFechaProgramada(fechaProgramada);
            mantenimiento.setHoraProgramada(etHora.getText().toString().trim());
            mantenimiento.setPrioridad(prioridadFinal);
            mantenimiento.setEstado("programado");

            // Guardar en Firestore
            db.collection("mantenimientos")
                    .add(mantenimiento.toMap())
                    .addOnSuccessListener(documentReference -> {
                        int creados = contadorCreados.incrementAndGet();
                        progressDialog.setProgress(creados);
                        progressDialog.setMessage("Creando " + creados + " de " + totalEquipos + "...");

                        android.util.Log.d("CrearMantenimiento", "✅ Mantenimiento creado " + creados + "/" + totalEquipos + ": " + documentReference.getId());

                        // Enviar notificación al técnico para este mantenimiento específico
                        String equipoInfo = equipo.getTipo() + " - " + equipo.getMarca() + " " + equipo.getModelo();
                        String clienteInfo = clienteSeleccionado.getNombreEmpresa();
                        String fechaHora = etFecha.getText().toString() + " " + etHora.getText().toString();

                        NotificationHelper notificationHelper = new NotificationHelper(this);
                        notificationHelper.notificarNuevoMantenimiento(
                                tecnicoPrincipalSeleccionado.getUserId(),
                                documentReference.getId(), // ID correcto del mantenimiento recién creado
                                equipoInfo,
                                clienteInfo,
                                fechaHora,
                                prioridadFinal
                        );

                        // Verificar si ya terminamos todos
                        if (creados + contadorErrores.get() == totalEquipos) {
                            progressDialog.dismiss();
                            finalizarCreacion(creados, contadorErrores.get(), totalEquipos);
                        }
                    })
                    .addOnFailureListener(e -> {
                        int errores = contadorErrores.incrementAndGet();
                        android.util.Log.e("CrearMantenimiento", "❌ Error creando mantenimiento para " + equipo.getTipo() + ": " + e.getMessage());

                        // Verificar si ya terminamos todos
                        if (contadorCreados.get() + errores == totalEquipos) {
                            progressDialog.dismiss();
                            finalizarCreacion(contadorCreados.get(), errores, totalEquipos);
                        }
                    });
        }
    }

    /**
     * Finaliza el proceso de creación de mantenimientos y muestra resultado
     */
    private void finalizarCreacion(int creados, int errores, int total) {
        // Las notificaciones ya se enviaron individualmente en el success listener
        // de cada mantenimiento creado

        // Mostrar resultado
        if (errores == 0) {
            // Todo exitoso
            String mensaje = creados == 1 ?
                    "✅ Mantenimiento creado correctamente" :
                    "✅ " + creados + " mantenimientos creados correctamente";
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
            setResult(RESULT_OK);
            finish();
        } else if (creados == 0) {
            // Todos fallaron
            Toast.makeText(this, "❌ Error: No se pudo crear ningún mantenimiento", Toast.LENGTH_LONG).show();
            btnSiguiente.setEnabled(true);
            btnSiguiente.setText("CREAR MANTENIMIENTO");
        } else {
            // Algunos exitosos, otros fallaron
            String mensaje = "⚠️ Creados: " + creados + " | Errores: " + errores;
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
            setResult(RESULT_OK);
            finish();
        }
    }
}
