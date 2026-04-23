package com.techsolution.techmaintenance.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.adapters.EquipoAdapter;
import com.techsolution.techmaintenance.models.Equipo;
import com.techsolution.techmaintenance.models.Usuario;

import java.util.ArrayList;
import java.util.List;

public class ListaEquiposActivity extends AppCompatActivity {

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Usuario usuarioActual;

    // Vistas
    private Toolbar toolbar;
    private TextView tvContador;
    private EditText etBuscar;
    private ImageButton btnLimpiarBusqueda;
    private ChipGroup chipGroupFiltros;
    private Chip chipTodos, chipLaptops, chipDesktops, chipServidores, chipImpresoras;
    private Chip chipOperativos, chipEnMantenimiento, chipFueraServicio;
    private ProgressBar progressBar;
    private LinearLayout layoutSinEquipos;
    private RecyclerView recyclerEquipos;
    private FloatingActionButton fabAgregar;

    // Adapter y lista
    private EquipoAdapter adapter;
    private List<Equipo> listaEquipos;
    private boolean primeraVez = true;
    private boolean esAdmin = false;

    // Filtro por cliente
    private String clienteIdFiltro = null;
    private String nombreClienteFiltro = null;

    // Filtro por técnico
    private boolean filtrarPorTecnico = false;
    private String tecnicoId = null;
    private List<String> equiposDelTecnico = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_equipos);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Verificar si viene filtro por cliente
        if (getIntent().hasExtra("clienteId")) {
            clienteIdFiltro = getIntent().getStringExtra("clienteId");
            nombreClienteFiltro = getIntent().getStringExtra("nombreCliente");
            android.util.Log.d("ListaEquipos", "📌 Filtro por cliente activado: " + nombreClienteFiltro);
        }

        // Verificar si viene filtro por técnico
        if (getIntent().hasExtra("filtrarPorTecnico")) {
            filtrarPorTecnico = getIntent().getBooleanExtra("filtrarPorTecnico", false);
            tecnicoId = getIntent().getStringExtra("tecnicoId");
            android.util.Log.d("ListaEquipos", "👤 Filtro por técnico activado: " + tecnicoId);
        }

        // Inicializar vistas
        inicializarVistas();

        // Configurar toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Si hay filtro, cambiar el título
            if (clienteIdFiltro != null && nombreClienteFiltro != null) {
                getSupportActionBar().setTitle("Equipos de " + nombreClienteFiltro);
            } else if (filtrarPorTecnico) {
                getSupportActionBar().setTitle("Mis Equipos");
            }
        }

        // Verificar rol del usuario
        verificarRolUsuario();
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        tvContador = findViewById(R.id.tvContadorEquipos);
        etBuscar = findViewById(R.id.etBuscar);
        btnLimpiarBusqueda = findViewById(R.id.btnLimpiarBusqueda);
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros);
        chipTodos = findViewById(R.id.chipTodos);
        chipLaptops = findViewById(R.id.chipLaptops);
        chipDesktops = findViewById(R.id.chipDesktops);
        chipServidores = findViewById(R.id.chipServidores);
        chipImpresoras = findViewById(R.id.chipImpresoras);
        chipOperativos = findViewById(R.id.chipOperativos);
        chipEnMantenimiento = findViewById(R.id.chipEnMantenimiento);
        chipFueraServicio = findViewById(R.id.chipFueraServicio);
        progressBar = findViewById(R.id.progressBar);
        layoutSinEquipos = findViewById(R.id.layoutSinEquipos);
        recyclerEquipos = findViewById(R.id.recyclerEquipos);
        fabAgregar = findViewById(R.id.fabAgregarEquipo);
    }

    private void verificarRolUsuario() {
        String uid = auth.getCurrentUser().getUid();

        db.collection("usuarios").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        usuarioActual = documentSnapshot.toObject(Usuario.class);
                        esAdmin = usuarioActual != null && "admin".equals(usuarioActual.getRol());

                        // Mostrar/ocultar FAB según rol
                        fabAgregar.setVisibility(esAdmin ? View.VISIBLE : View.GONE);

                        // Configurar RecyclerView
                        configurarRecyclerView();

                        // Configurar listeners
                        configurarListeners();

                        // Cargar equipos
                        cargarEquipos();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al verificar usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void configurarRecyclerView() {
        listaEquipos = new ArrayList<>();
        adapter = new EquipoAdapter(this, listaEquipos, esAdmin, new EquipoAdapter.OnEquipoClickListener() {
            @Override
            public void onVerClick(Equipo equipo) {
                abrirDetalleEquipo(equipo);
            }

            @Override
            public void onEditarClick(Equipo equipo) {
                if (esAdmin) {
                    abrirEditarEquipo(equipo);
                } else {
                    Toast.makeText(ListaEquiposActivity.this, "No tienes permisos para editar equipos", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // GridLayoutManager con 2 columnas (como especifica el CLAUDE.md)
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerEquipos.setLayoutManager(gridLayoutManager);
        recyclerEquipos.setAdapter(adapter);
        // Removido setHasFixedSize(true) debido a wrap_content en scroll direction

        android.util.Log.d("ListaEquipos", "✅ RecyclerView configurado con Grid 2 columnas");
    }

    private void configurarListeners() {
        // FAB Agregar (solo para admin)
        fabAgregar.setOnClickListener(v -> {
            if (esAdmin) {
                abrirAgregarEquipo();
            }
        });

        // Barra de búsqueda
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    btnLimpiarBusqueda.setVisibility(View.VISIBLE);
                } else {
                    btnLimpiarBusqueda.setVisibility(View.GONE);
                }

                adapter.filtrar(s.toString());
                actualizarContador();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Botón limpiar búsqueda
        btnLimpiarBusqueda.setOnClickListener(v -> {
            etBuscar.setText("");
            btnLimpiarBusqueda.setVisibility(View.GONE);
        });

        // Chips de filtros
        chipGroupFiltros.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int selectedId = checkedIds.get(0);

            if (selectedId == R.id.chipTodos) {
                adapter.filtrarPorTipo("todos");
            } else if (selectedId == R.id.chipLaptops) {
                adapter.filtrarPorTipo("laptop");
            } else if (selectedId == R.id.chipDesktops) {
                adapter.filtrarPorTipo("desktop");
            } else if (selectedId == R.id.chipServidores) {
                adapter.filtrarPorTipo("servidor");
            } else if (selectedId == R.id.chipImpresoras) {
                adapter.filtrarPorTipo("impresora");
            } else if (selectedId == R.id.chipOperativos) {
                adapter.filtrarPorEstado("operativo");
            } else if (selectedId == R.id.chipEnMantenimiento) {
                adapter.filtrarPorEstado("mantenimiento");
            } else if (selectedId == R.id.chipFueraServicio) {
                adapter.filtrarPorEstado("fuera_servicio");
            }

            actualizarContador();
        });

        // Click en toolbar (volver)
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void cargarEquipos() {
        mostrarCargando(true);

        android.util.Log.d("ListaEquipos", "🔍 ========== INICIANDO CARGA DE EQUIPOS ==========");
        android.util.Log.d("ListaEquipos", "📊 Estado actual:");
        android.util.Log.d("ListaEquipos", "   - clienteIdFiltro: " + (clienteIdFiltro != null ? clienteIdFiltro : "NULL"));
        android.util.Log.d("ListaEquipos", "   - nombreClienteFiltro: " + (nombreClienteFiltro != null ? nombreClienteFiltro : "NULL"));
        android.util.Log.d("ListaEquipos", "   - filtrarPorTecnico: " + filtrarPorTecnico);
        android.util.Log.d("ListaEquipos", "   - tecnicoId: " + (tecnicoId != null ? tecnicoId : "NULL"));
        android.util.Log.d("ListaEquipos", "   - esAdmin: " + esAdmin);

        // Si es filtro por técnico, primero obtener los equipoIds de sus mantenimientos
        if (filtrarPorTecnico && tecnicoId != null) {
            android.util.Log.d("ListaEquipos", "👤 *** APLICANDO FILTRO POR TÉCNICO ***");
            android.util.Log.d("ListaEquipos", "👤 Paso 1: Obteniendo mantenimientos del técnico");

            equiposDelTecnico.clear();

            // Consultar mantenimientos donde el técnico es principal
            db.collection("mantenimientos")
                    .whereEqualTo("tecnicoPrincipalId", tecnicoId)
                    .get()
                    .addOnSuccessListener(queryDocs -> {
                        android.util.Log.d("ListaEquipos", "✅ Mantenimientos como principal: " + queryDocs.size());

                        for (QueryDocumentSnapshot doc : queryDocs) {
                            String equipoId = doc.getString("equipoId");
                            String mantenimientoId = doc.getId();
                            if (equipoId != null) {
                                if (!equiposDelTecnico.contains(equipoId)) {
                                    equiposDelTecnico.add(equipoId);
                                    android.util.Log.d("ListaEquipos", "   + Equipo agregado: " + equipoId + " (de mantenimiento: " + mantenimientoId + ")");
                                } else {
                                    android.util.Log.d("ListaEquipos", "   = Equipo ya en lista: " + equipoId + " (mantenimiento: " + mantenimientoId + ")");
                                }
                            }
                        }

                        android.util.Log.d("ListaEquipos", "📊 Equipos únicos hasta ahora: " + equiposDelTecnico.size());

                        // También consultar mantenimientos donde el técnico es de apoyo
                        consultarMantenimientosApoyo();
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("ListaEquipos", "❌ Error al consultar mantenimientos: " + e.getMessage());
                        mostrarCargando(false);
                        Toast.makeText(this, "Error al cargar equipos del técnico", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Filtrado normal (por cliente o todos)
            cargarEquiposNormal();
        }
    }

    private void consultarMantenimientosApoyo() {
        android.util.Log.d("ListaEquipos", "👤 Paso 2: Consultando mantenimientos como apoyo");

        db.collection("mantenimientos")
                .whereArrayContains("tecnicosApoyo", tecnicoId)
                .get()
                .addOnSuccessListener(queryDocs -> {
                    android.util.Log.d("ListaEquipos", "✅ Mantenimientos como apoyo: " + queryDocs.size());

                    for (QueryDocumentSnapshot doc : queryDocs) {
                        String equipoId = doc.getString("equipoId");
                        String mantenimientoId = doc.getId();
                        if (equipoId != null) {
                            if (!equiposDelTecnico.contains(equipoId)) {
                                equiposDelTecnico.add(equipoId);
                                android.util.Log.d("ListaEquipos", "   + Equipo agregado (apoyo): " + equipoId + " (de mantenimiento: " + mantenimientoId + ")");
                            } else {
                                android.util.Log.d("ListaEquipos", "   = Equipo ya en lista (apoyo): " + equipoId + " (mantenimiento: " + mantenimientoId + ")");
                            }
                        }
                    }

                    android.util.Log.d("ListaEquipos", "📊 Total equipos únicos del técnico: " + equiposDelTecnico.size());
                    android.util.Log.d("ListaEquipos", "📋 IDs de equipos: " + equiposDelTecnico.toString());

                    // Ahora cargar solo esos equipos
                    cargarEquiposPorIds();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ListaEquipos", "❌ Error al consultar mantenimientos de apoyo: " + e.getMessage());
                    // Continuar con los equipos que ya tenemos
                    cargarEquiposPorIds();
                });
    }

    private void cargarEquiposPorIds() {
        android.util.Log.d("ListaEquipos", "👤 Paso 3: Cargando equipos por IDs");

        if (equiposDelTecnico.isEmpty()) {
            android.util.Log.w("ListaEquipos", "⚠️ El técnico no tiene equipos asignados");
            listaEquipos.clear();
            adapter.actualizarLista(listaEquipos);
            actualizarContador();
            mostrarCargando(false);
            layoutSinEquipos.setVisibility(View.VISIBLE);
            recyclerEquipos.setVisibility(View.GONE);
            return;
        }

        // Firestore tiene límite de 10 elementos en whereIn, así que si hay más, hacer múltiples consultas
        List<List<String>> lotes = dividirEnLotes(equiposDelTecnico, 10);
        android.util.Log.d("ListaEquipos", "📦 Dividido en " + lotes.size() + " lote(s)");

        listaEquipos.clear();
        cargarLoteEquipos(lotes, 0);
    }

    private void cargarLoteEquipos(List<List<String>> lotes, int indice) {
        if (indice >= lotes.size()) {
            // Terminamos de cargar todos los lotes
            android.util.Log.d("ListaEquipos", "✅ Carga completa. Total equipos: " + listaEquipos.size());
            adapter.actualizarLista(listaEquipos);
            actualizarContador();
            mostrarCargando(false);

            if (listaEquipos.isEmpty()) {
                layoutSinEquipos.setVisibility(View.VISIBLE);
                recyclerEquipos.setVisibility(View.GONE);
            } else {
                layoutSinEquipos.setVisibility(View.GONE);
                recyclerEquipos.setVisibility(View.VISIBLE);
            }
            return;
        }

        List<String> loteActual = lotes.get(indice);
        android.util.Log.d("ListaEquipos", "📦 Cargando lote " + (indice + 1) + "/" + lotes.size() + " (" + loteActual.size() + " IDs)");

        db.collection("equipos")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), loteActual)
                .get()
                .addOnSuccessListener(queryDocs -> {
                    android.util.Log.d("ListaEquipos", "   📥 Documentos recibidos en este lote: " + queryDocs.size());

                    for (QueryDocumentSnapshot doc : queryDocs) {
                        try {
                            String equipoId = doc.getId();

                            // Verificar si ya existe en la lista para evitar duplicados
                            boolean yaExiste = false;
                            for (Equipo e : listaEquipos) {
                                if (e.getEquipoId() != null && e.getEquipoId().equals(equipoId)) {
                                    yaExiste = true;
                                    android.util.Log.w("ListaEquipos", "   ⚠️ DUPLICADO EVITADO: " + equipoId);
                                    break;
                                }
                            }

                            if (!yaExiste) {
                                Equipo equipo = doc.toObject(Equipo.class);
                                equipo.setEquipoId(equipoId);
                                listaEquipos.add(equipo);
                                android.util.Log.d("ListaEquipos", "   ✓ Agregado: " + equipo.getMarca() + " " + equipo.getModelo() + " (ID: " + equipoId + ")");
                            }
                        } catch (Exception e) {
                            android.util.Log.e("ListaEquipos", "❌ Error al convertir equipo: " + e.getMessage());
                        }
                    }

                    android.util.Log.d("ListaEquipos", "   📊 Total en listaEquipos ahora: " + listaEquipos.size());

                    // Cargar siguiente lote
                    cargarLoteEquipos(lotes, indice + 1);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ListaEquipos", "❌ Error al cargar lote: " + e.getMessage());
                    // Continuar con siguiente lote aunque falle este
                    cargarLoteEquipos(lotes, indice + 1);
                });
    }

    private List<List<String>> dividirEnLotes(List<String> lista, int tamanoLote) {
        List<List<String>> lotes = new ArrayList<>();
        for (int i = 0; i < lista.size(); i += tamanoLote) {
            int fin = Math.min(lista.size(), i + tamanoLote);
            lotes.add(new ArrayList<>(lista.subList(i, fin)));
        }
        return lotes;
    }

    private void cargarEquiposNormal() {
        android.util.Log.d("ListaEquipos", "🌐 Carga normal de equipos");

        // Construir la consulta base
        com.google.firebase.firestore.Query query = db.collection("equipos");

        // Si hay filtro por cliente, aplicarlo
        if (clienteIdFiltro != null && !clienteIdFiltro.isEmpty()) {
            android.util.Log.d("ListaEquipos", "📌 *** APLICANDO FILTRO POR CLIENTE ***");
            android.util.Log.d("ListaEquipos", "📌 Filtrando por clienteId = '" + clienteIdFiltro + "'");
            query = query.whereEqualTo("clienteId", clienteIdFiltro)
                    .orderBy("fechaRegistro", com.google.firebase.firestore.Query.Direction.DESCENDING);
        } else {
            android.util.Log.d("ListaEquipos", "🌐 Sin filtro - cargando TODOS los equipos");
            // Sin filtro, solo ordenar
            query = query.orderBy("fechaRegistro", com.google.firebase.firestore.Query.Direction.DESCENDING);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaEquipos.clear();

                    android.util.Log.d("ListaEquipos", "✅ *** RESPUESTA RECIBIDA DE FIRESTORE ***");
                    android.util.Log.d("ListaEquipos", "✅ Documentos encontrados: " + queryDocumentSnapshots.size());

                    if (queryDocumentSnapshots.isEmpty()) {
                        android.util.Log.w("ListaEquipos", "⚠️ ¡ATENCIÓN! La consulta no devolvió ningún documento");
                        android.util.Log.w("ListaEquipos", "⚠️ Posibles causas:");
                        android.util.Log.w("ListaEquipos", "   1. No hay equipos con clienteId = '" + clienteIdFiltro + "'");
                        android.util.Log.w("ListaEquipos", "   2. Falta índice compuesto en Firestore");
                        android.util.Log.w("ListaEquipos", "   3. El campo 'clienteId' no existe o está mal escrito en Firestore");
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Equipo equipo = doc.toObject(Equipo.class);
                            equipo.setEquipoId(doc.getId());
                            listaEquipos.add(equipo);

                            android.util.Log.d("ListaEquipos", "📄 Equipo #" + listaEquipos.size() + ":");
                            android.util.Log.d("ListaEquipos", "   - ID: " + equipo.getEquipoId());
                            android.util.Log.d("ListaEquipos", "   - Marca/Modelo: " + equipo.getMarca() + " " + equipo.getModelo());
                            android.util.Log.d("ListaEquipos", "   - ClienteId: " + equipo.getClienteId());
                        } catch (Exception e) {
                            android.util.Log.e("ListaEquipos", "❌ Error al convertir documento " + doc.getId() + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    android.util.Log.d("ListaEquipos", "📊 ========== RESUMEN ==========");
                    android.util.Log.d("ListaEquipos", "📊 Total equipos en lista: " + listaEquipos.size());

                    adapter.actualizarLista(listaEquipos);

                    int itemCount = adapter.getItemCount();
                    android.util.Log.d("ListaEquipos", "🔢 ItemCount del adapter: " + itemCount);

                    actualizarContador();
                    mostrarCargando(false);

                    if (itemCount == 0) {
                        android.util.Log.d("ListaEquipos", "⚠️ Adapter vacío, mostrando mensaje");
                        layoutSinEquipos.setVisibility(View.VISIBLE);
                        recyclerEquipos.setVisibility(View.GONE);
                    } else {
                        android.util.Log.d("ListaEquipos", "✅ Mostrando " + adapter.getItemCount() + " equipos");
                        layoutSinEquipos.setVisibility(View.GONE);
                        recyclerEquipos.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ListaEquipos", "❌ Error al cargar equipos: " + e.getMessage());
                    e.printStackTrace();
                    mostrarCargando(false);

                    // Si el error es por índice, informar al usuario
                    String mensajeError = e.getMessage();
                    if (mensajeError != null && mensajeError.contains("index")) {
                        Toast.makeText(this,
                                "Se requiere crear un índice en Firestore. Revisa los logs para el enlace.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Error al cargar equipos: " + mensajeError, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void actualizarContador() {
        int total = adapter.getItemCount();
        if (total == 0) {
            tvContador.setText("Total: 0 equipos");
        } else if (total == 1) {
            tvContador.setText("Total: 1 equipo");
        } else {
            tvContador.setText("Total: " + total + " equipos");
        }
    }

    private void mostrarCargando(boolean mostrar) {
        progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        recyclerEquipos.setVisibility(mostrar ? View.GONE : View.VISIBLE);
    }

    private void abrirAgregarEquipo() {
        Intent intent = new Intent(this, AgregarEditarEquipoActivity.class);
        startActivityForResult(intent, 100);
    }

    private void abrirDetalleEquipo(Equipo equipo) {
        Intent intent = new Intent(this, DetalleEquipoActivity.class);
        intent.putExtra("equipoId", equipo.getEquipoId());
        startActivityForResult(intent, 100);
    }

    private void abrirEditarEquipo(Equipo equipo) {
        android.util.Log.d("ListaEquipos", "✏️ Abriendo editar equipo: " + equipo.getMarca() + " " + equipo.getModelo());
        android.util.Log.d("ListaEquipos", "   - equipoId: " + equipo.getEquipoId());

        Intent intent = new Intent(this, AgregarEditarEquipoActivity.class);
        // Enviar solo el ID en lugar del objeto completo (más confiable)
        intent.putExtra("equipoId", equipo.getEquipoId());
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            cargarEquipos(); // Recargar lista
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Solo recargar si no es la primera vez (evitar carga duplicada con onCreate)
        if (!primeraVez) {
            cargarEquipos();
        }
        primeraVez = false;
    }
}
