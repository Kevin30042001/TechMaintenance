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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.adapters.ClienteAdapter;
import com.techsolution.techmaintenance.models.Cliente;

import java.util.ArrayList;
import java.util.List;

public class ListaClientesActivity extends AppCompatActivity {

    // Firebase
    private FirebaseFirestore db;
    private com.google.firebase.auth.FirebaseAuth auth;

    // Vistas
    private Toolbar toolbar;
    private TextView tvContador;
    private EditText etBuscar;
    private ImageButton btnLimpiarBusqueda;
    private ChipGroup chipGroupFiltros;
    private Chip chipTodos, chipConEquipos, chipSinEquipos, chipRecientes;
    private ProgressBar progressBar;
    private LinearLayout layoutSinClientes;
    private RecyclerView recyclerClientes;
    private FloatingActionButton fabAgregar;

    // Adapter y lista
    private ClienteAdapter adapter;
    private List<Cliente> listaClientes;
    private boolean primeraVez = true;

    // Control de acceso
    private boolean esAdmin = false;
    private com.techsolution.techmaintenance.models.Usuario usuarioActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_clientes);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        auth = com.google.firebase.auth.FirebaseAuth.getInstance();

        // Inicializar vistas
        inicializarVistas();

        // Configurar toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Verificar rol del usuario ANTES de configurar todo
        verificarRolUsuario();
    }

    /**
     * Verifica el rol del usuario y configura la interfaz según sus permisos
     */
    private void verificarRolUsuario() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("usuarios").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        usuarioActual = documentSnapshot.toObject(com.techsolution.techmaintenance.models.Usuario.class);
                        esAdmin = usuarioActual != null && "admin".equals(usuarioActual.getRol());

                        android.util.Log.d("ListaClientes", "👤 Usuario: " + usuarioActual.getNombre());
                        android.util.Log.d("ListaClientes", "🔐 Rol: " + usuarioActual.getRol());
                        android.util.Log.d("ListaClientes", "✅ Es Admin: " + esAdmin);

                        // Configurar interfaz según rol
                        configurarInterfazSegunRol();

                        // Configurar RecyclerView
                        configurarRecyclerView();

                        // Configurar listeners
                        configurarListeners();

                        // Cargar clientes
                        cargarClientes();
                    } else {
                        Toast.makeText(this, "Error: No se encontró información del usuario", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al verificar usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Configura la interfaz según el rol del usuario
     */
    private void configurarInterfazSegunRol() {
        if (esAdmin) {
            // Admin: puede crear clientes
            fabAgregar.setVisibility(View.VISIBLE);
            android.util.Log.d("ListaClientes", "✅ FAB visible (Admin)");
        } else {
            // Técnico: solo puede ver clientes
            fabAgregar.setVisibility(View.GONE);
            android.util.Log.d("ListaClientes", "🚫 FAB oculto (Técnico - solo lectura)");
        }
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        tvContador = findViewById(R.id.tvContadorClientes);
        etBuscar = findViewById(R.id.etBuscar);
        btnLimpiarBusqueda = findViewById(R.id.btnLimpiarBusqueda);
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros);
        chipTodos = findViewById(R.id.chipTodos);
        chipConEquipos = findViewById(R.id.chipConEquipos);
        chipSinEquipos = findViewById(R.id.chipSinEquipos);
        chipRecientes = findViewById(R.id.chipRecientes);
        progressBar = findViewById(R.id.progressBar);
        layoutSinClientes = findViewById(R.id.layoutSinClientes);
        recyclerClientes = findViewById(R.id.recyclerClientes);
        fabAgregar = findViewById(R.id.fabAgregarCliente);
    }

    private void configurarRecyclerView() {
        listaClientes = new ArrayList<>();
        adapter = new ClienteAdapter(this, listaClientes, esAdmin, new ClienteAdapter.OnClienteClickListener() {
            @Override
            public void onVerClick(Cliente cliente) {
                abrirDetalleCliente(cliente);
            }

            @Override
            public void onEditarClick(Cliente cliente) {
                if (esAdmin) {
                    abrirEditarCliente(cliente);
                } else {
                    Toast.makeText(ListaClientesActivity.this, "No tienes permisos para editar clientes", Toast.LENGTH_SHORT).show();
                }
            }
        });

        recyclerClientes.setLayoutManager(new LinearLayoutManager(this));
        recyclerClientes.setAdapter(adapter);
        // Removido setHasFixedSize(true) debido a wrap_content en scroll direction

        android.util.Log.d("ListaClientes", "✅ RecyclerView configurado con adapter");
    }

    private void configurarListeners() {
        // FAB Agregar
        fabAgregar.setOnClickListener(v -> {
            if (esAdmin) {
                abrirAgregarCliente();
            } else {
                Toast.makeText(this, "No tienes permisos para crear clientes", Toast.LENGTH_SHORT).show();
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
            } else if (selectedId == R.id.chipConEquipos) {
                adapter.filtrarPorTipo("con_equipos");
            } else if (selectedId == R.id.chipSinEquipos) {
                adapter.filtrarPorTipo("sin_equipos");
            } else if (selectedId == R.id.chipRecientes) {
                adapter.filtrarPorTipo("recientes");
            }

            actualizarContador();
        });

        // Click en toolbar (volver)
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void cargarClientes() {
        mostrarCargando(true);

        android.util.Log.d("ListaClientes", "🔍 Iniciando carga de clientes...");

        db.collection("clientes")
                .orderBy("fechaRegistro", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaClientes.clear();

                    android.util.Log.d("ListaClientes", "✅ Documentos encontrados: " + queryDocumentSnapshots.size());

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Cliente cliente = doc.toObject(Cliente.class);
                            cliente.setClienteId(doc.getId());
                            listaClientes.add(cliente);

                            android.util.Log.d("ListaClientes", "📄 Cliente cargado: " + cliente.getNombreEmpresa());
                        } catch (Exception e) {
                            android.util.Log.e("ListaClientes", "❌ Error al convertir documento: " + e.getMessage());
                        }
                    }

                    android.util.Log.d("ListaClientes", "📊 Total clientes en lista: " + listaClientes.size());

                    adapter.actualizarLista(listaClientes);

                    // Verificar inmediatamente después de actualizar
                    int itemCount = adapter.getItemCount();
                    android.util.Log.d("ListaClientes", "🔢 ItemCount del adapter: " + itemCount);

                    actualizarContador();
                    mostrarCargando(false);

                    // Verificar usando el adapter, no listaClientes
                    if (itemCount == 0) {
                        android.util.Log.d("ListaClientes", "⚠️ Adapter vacío, mostrando mensaje");
                        layoutSinClientes.setVisibility(View.VISIBLE);
                        recyclerClientes.setVisibility(View.GONE);
                    } else {
                        android.util.Log.d("ListaClientes", "✅ Mostrando " + adapter.getItemCount() + " clientes");
                        layoutSinClientes.setVisibility(View.GONE);
                        recyclerClientes.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ListaClientes", "❌ Error al cargar clientes: " + e.getMessage());
                    mostrarCargando(false);
                    Toast.makeText(this,
                            getString(R.string.error_cargar_clientes) + ": " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void actualizarContador() {
        int total = adapter.getItemCount();
        if (total == 0) {
            tvContador.setText("Total: 0 clientes");
        } else if (total == 1) {
            tvContador.setText("Total: 1 cliente");
        } else {
            tvContador.setText("Total: " + total + " clientes");
        }
    }

    private void mostrarCargando(boolean mostrar) {
        progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        recyclerClientes.setVisibility(mostrar ? View.GONE : View.VISIBLE);
    }

    private void abrirAgregarCliente() {
        Intent intent = new Intent(this, AgregarEditarClienteActivity.class);
        startActivity(intent);
    }

    private void abrirDetalleCliente(Cliente cliente) {
        Intent intent = new Intent(this, DetalleClienteActivity.class);
        intent.putExtra("cliente", cliente);
        startActivityForResult(intent, 100); // ← Cambio aquí
    }

    // Agregar este método:
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            cargarClientes(); // Recargar lista
        }
    }

    private void abrirEditarCliente(Cliente cliente) {
        Intent intent = new Intent(this, AgregarEditarClienteActivity.class);
        intent.putExtra("cliente", cliente);
        intent.putExtra("modo", "editar");
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Solo recargar si no es la primera vez (evitar carga duplicada con onCreate)
        if (!primeraVez) {
            cargarClientes();
        }
        primeraVez = false;
    }
}