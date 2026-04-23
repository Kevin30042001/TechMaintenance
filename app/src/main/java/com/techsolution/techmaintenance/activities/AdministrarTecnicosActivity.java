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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.adapters.TecnicoAdapter;
import com.techsolution.techmaintenance.models.Usuario;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdministrarTecnicosActivity extends AppCompatActivity {

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Vistas
    private Toolbar toolbar;
    private TextView tvContador;
    private EditText etBuscar;
    private ImageButton btnLimpiarBusqueda;
    private ChipGroup chipGroupFiltros;
    private Chip chipTodos, chipActivos, chipInactivos;
    private ProgressBar progressBar;
    private LinearLayout layoutSinTecnicos;
    private RecyclerView recyclerTecnicos;
    private FloatingActionButton fabAgregar;

    // Adapter y lista
    private TecnicoAdapter adapter;
    private List<Usuario> listaTecnicos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("AdminTecnicos", "🟢 onCreate() - INICIO");

        try {
            setContentView(R.layout.activity_administrar_tecnicos);
            android.util.Log.d("AdminTecnicos", "✅ setContentView OK");

            // Inicializar Firebase
            db = FirebaseFirestore.getInstance();
            auth = FirebaseAuth.getInstance();
            android.util.Log.d("AdminTecnicos", "✅ Firebase inicializado");

            // Inicializar vistas
            inicializarVistas();
            android.util.Log.d("AdminTecnicos", "✅ Vistas inicializadas");

            // Configurar toolbar
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
            android.util.Log.d("AdminTecnicos", "✅ Toolbar configurado");

            // Configurar RecyclerView
            configurarRecyclerView();
            android.util.Log.d("AdminTecnicos", "✅ RecyclerView configurado");

            // Configurar listeners
            configurarListeners();
            android.util.Log.d("AdminTecnicos", "✅ Listeners configurados");

            // Cargar técnicos
            cargarTecnicos();
            android.util.Log.d("AdminTecnicos", "✅ onCreate() - FIN EXITOSO");

        } catch (Exception e) {
            android.util.Log.e("AdminTecnicos", "❌ ERROR en onCreate(): " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error al cargar la actividad: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        tvContador = findViewById(R.id.tvContadorTecnicos);
        etBuscar = findViewById(R.id.etBuscar);
        btnLimpiarBusqueda = findViewById(R.id.btnLimpiarBusqueda);
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros);
        chipTodos = findViewById(R.id.chipTodos);
        chipActivos = findViewById(R.id.chipActivos);
        chipInactivos = findViewById(R.id.chipInactivos);
        progressBar = findViewById(R.id.progressBar);
        layoutSinTecnicos = findViewById(R.id.layoutSinTecnicos);
        recyclerTecnicos = findViewById(R.id.recyclerTecnicos);
        fabAgregar = findViewById(R.id.fabAgregarTecnico);
    }

    private void configurarRecyclerView() {
        listaTecnicos = new ArrayList<>();
        adapter = new TecnicoAdapter(this, listaTecnicos, new TecnicoAdapter.OnTecnicoClickListener() {
            @Override
            public void onVerClick(Usuario tecnico) {
                mostrarEstadisticasTecnico(tecnico);
            }

            @Override
            public void onEditarClick(Usuario tecnico) {
                abrirEditarTecnico(tecnico);
            }

            @Override
            public void onCambiarEstadoClick(Usuario tecnico) {
                mostrarDialogoCambiarEstado(tecnico);
            }

            @Override
            public void onEliminarClick(Usuario tecnico) {
                mostrarDialogoEliminar(tecnico);
            }
        });

        recyclerTecnicos.setLayoutManager(new LinearLayoutManager(this));
        recyclerTecnicos.setAdapter(adapter);

    }

    private void configurarListeners() {
        // FAB Agregar
        fabAgregar.setOnClickListener(v -> abrirCrearTecnico());

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

        // Filtros de chips
        chipGroupFiltros.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipTodos) {
                adapter.filtrarPorEstado("todos");
            } else if (checkedId == R.id.chipActivos) {
                adapter.filtrarPorEstado("activos");
            } else if (checkedId == R.id.chipInactivos) {
                adapter.filtrarPorEstado("inactivos");
            }
            actualizarContador();
        });
    }

    private void cargarTecnicos() {
        mostrarCargando(true);

        android.util.Log.d("AdminTecnicos", "🔍 Cargando técnicos...");

        // Cargar solo usuarios con rol="tecnico" o "admin"
        db.collection("usuarios")
                .orderBy("nombre")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaTecnicos.clear();

                    android.util.Log.d("AdminTecnicos", "✅ Documentos encontrados: " + queryDocumentSnapshots.size());

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Usuario usuario = doc.toObject(Usuario.class);
                            usuario.setUserId(doc.getId());

                            // Filtrar solo técnicos (puedes incluir admins si quieres)
                            if ("tecnico".equals(usuario.getRol()) || "admin".equals(usuario.getRol())) {
                                listaTecnicos.add(usuario);
                                android.util.Log.d("AdminTecnicos", "👤 Usuario: " + usuario.getNombre() + " (" + usuario.getRol() + ")");
                            }
                        } catch (Exception e) {
                            android.util.Log.e("AdminTecnicos", "❌ Error al convertir usuario: " + e.getMessage());
                        }
                    }

                    android.util.Log.d("AdminTecnicos", "📊 Total técnicos/admins: " + listaTecnicos.size());

                    adapter.actualizarLista(listaTecnicos);
                    actualizarContador();
                    mostrarCargando(false);

                    if (listaTecnicos.isEmpty()) {
                        layoutSinTecnicos.setVisibility(View.VISIBLE);
                        recyclerTecnicos.setVisibility(View.GONE);
                    } else {
                        layoutSinTecnicos.setVisibility(View.GONE);
                        recyclerTecnicos.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AdminTecnicos", "❌ Error al cargar técnicos: " + e.getMessage());
                    mostrarCargando(false);
                    Toast.makeText(this, "Error al cargar técnicos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void actualizarContador() {
        int total = adapter.getItemCount();
        String texto = total == 1 ? "1 técnico" : total + " técnicos";
        tvContador.setText(texto);
    }

    private void mostrarCargando(boolean mostrar) {
        progressBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        recyclerTecnicos.setVisibility(mostrar ? View.GONE : View.VISIBLE);
    }

    /**
     * Abre la actividad para crear un nuevo técnico
     */
    private void abrirCrearTecnico() {
        Intent intent = new Intent(this, CrearEditarTecnicoActivity.class);
        startActivityForResult(intent, 100);
    }

    /**
     * Abre la actividad para editar un técnico existente
     */
    private void abrirEditarTecnico(Usuario tecnico) {
        Intent intent = new Intent(this, CrearEditarTecnicoActivity.class);
        intent.putExtra("tecnicoId", tecnico.getUserId());
        startActivityForResult(intent, 100);
    }

    /**
     * Muestra un diálogo con estadísticas detalladas del técnico
     */
    private void mostrarEstadisticasTecnico(Usuario tecnico) {
        String mensaje = "Nombre: " + tecnico.getNombre() + "\n" +
                        "Email: " + tecnico.getEmail() + "\n" +
                        "Teléfono: " + (tecnico.getTelefono() != null ? tecnico.getTelefono() : "No especificado") + "\n" +
                        "Estado: " + tecnico.getEstado() + "\n\n";

        if (tecnico.getEstadisticas() != null) {
            mensaje += "Servicios completados: " + tecnico.getEstadisticas().getServiciosCompletados() + "\n" +
                      "Calificación promedio: " + String.format("%.1f", tecnico.getEstadisticas().getCalificacionPromedio()) + "\n" +
                      "Eficiencia: " + tecnico.getEstadisticas().getEficiencia() + "%";
        } else {
            mensaje += "Sin estadísticas registradas";
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Estadísticas de " + tecnico.getNombre())
                .setMessage(mensaje)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    /**
     * Muestra un diálogo para confirmar el cambio de estado del técnico
     */
    private void mostrarDialogoCambiarEstado(Usuario tecnico) {
        boolean esActivo = "activo".equals(tecnico.getEstado());
        String nuevoEstado = esActivo ? "inactivo" : "activo";
        String accion = esActivo ? "desactivar" : "activar";

        String mensaje = "¿Estás seguro de que deseas " + accion + " a " + tecnico.getNombre() + "?\n\n";

        if (esActivo) {
            mensaje += "Al desactivar, el técnico no podrá iniciar sesión en la aplicación.";
        } else {
            mensaje += "Al activar, el técnico podrá iniciar sesión nuevamente.";
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(esActivo ? "Desactivar Técnico" : "Activar Técnico")
                .setMessage(mensaje)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(accion.toUpperCase(), (dialog, which) -> cambiarEstadoTecnico(tecnico, nuevoEstado))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Cambia el estado del técnico en Firestore
     */
    private void cambiarEstadoTecnico(Usuario tecnico, String nuevoEstado) {
        if (tecnico.getUserId() == null || tecnico.getUserId().isEmpty()) {
            Toast.makeText(this, "Error: Técnico sin ID", Toast.LENGTH_SHORT).show();
            return;
        }

        mostrarCargando(true);

        android.util.Log.d("AdminTecnicos", "🔄 Cambiando estado de " + tecnico.getNombre() + " a: " + nuevoEstado);

        db.collection("usuarios").document(tecnico.getUserId())
                .update("estado", nuevoEstado)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("AdminTecnicos", "✅ Estado actualizado correctamente");

                    String accion = "activo".equals(nuevoEstado) ? "activado" : "desactivado";
                    Toast.makeText(this, tecnico.getNombre() + " " + accion + " correctamente", Toast.LENGTH_SHORT).show();

                    // Recargar lista
                    cargarTecnicos();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AdminTecnicos", "❌ Error al cambiar estado: " + e.getMessage());
                    mostrarCargando(false);
                    Toast.makeText(this, "Error al cambiar estado: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void mostrarDialogoEliminar(Usuario tecnico) {
        String mensaje = "⚠️ ADVERTENCIA: Esta acción NO se puede deshacer.\n\n" +
                "Se eliminará:\n" +
                "• El usuario de Firebase Authentication\n" +
                "• El documento de Firestore\n" +
                "• Todas las referencias del técnico\n\n" +
                "Técnico: " + tecnico.getNombre() + "\n" +
                "Email: " + tecnico.getEmail() + "\n\n" +
                "¿Estás completamente seguro?";

        new MaterialAlertDialogBuilder(this)
                .setTitle("🗑️ Eliminar Técnico Permanentemente")
                .setMessage(mensaje)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("SÍ, ELIMINAR", (dialog, which) -> {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Confirmar Eliminación")
                            .setMessage("Esta es tu última oportunidad.\n\n¿Realmente deseas eliminar a " + tecnico.getNombre() + "?")
                            .setPositiveButton("ELIMINAR DEFINITIVAMENTE", (dialog2, which2) -> eliminarTecnico(tecnico))
                            .setNegativeButton("Cancelar", null)
                            .show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarTecnico(Usuario tecnico) {
        if (tecnico.getUserId() == null || tecnico.getUserId().isEmpty()) {
            Toast.makeText(this, "Error: Técnico sin ID", Toast.LENGTH_SHORT).show();
            return;
        }

        mostrarCargando(true);

        android.util.Log.d("AdminTecnicos", "🗑️ Iniciando eliminación de: " + tecnico.getNombre());
        android.util.Log.d("AdminTecnicos", "   UserID: " + tecnico.getUserId());
        android.util.Log.d("AdminTecnicos", "   Email: " + tecnico.getEmail());

        db.collection("mantenimientos")
                .whereEqualTo("tecnicoPrincipalId", tecnico.getUserId())
                .whereIn("estado", java.util.Arrays.asList("programado", "en_proceso"))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        mostrarCargando(false);
                        android.util.Log.w("AdminTecnicos", "⚠️ Técnico tiene " + querySnapshot.size() + " mantenimientos activos");
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("No se puede eliminar")
                                .setMessage("El técnico " + tecnico.getNombre() + " tiene " + querySnapshot.size() +
                                        " mantenimientos programados o en proceso.\n\n" +
                                        "Reasigna o completa estos mantenimientos antes de eliminarlo.")
                                .setPositiveButton("Entendido", null)
                                .show();
                        return;
                    }

                    android.util.Log.d("AdminTecnicos", "✅ No hay mantenimientos activos, procediendo...");

                    FirebaseAuth.getInstance().fetchSignInMethodsForEmail(tecnico.getEmail())
                            .addOnSuccessListener(signInMethods -> {
                                if (signInMethods.getSignInMethods() != null && !signInMethods.getSignInMethods().isEmpty()) {
                                    android.util.Log.d("AdminTecnicos", "🔐 Usuario existe en Authentication, eliminando de ambos lados...");
                                    eliminarDeAuthenticationYFirestore(tecnico);
                                } else {
                                    android.util.Log.d("AdminTecnicos", "📄 Usuario solo en Firestore, eliminando documento...");
                                    eliminarSoloDeFirestore(tecnico);
                                }
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.w("AdminTecnicos", "⚠️ Error al verificar Authentication, eliminando solo de Firestore: " + e.getMessage());
                                eliminarSoloDeFirestore(tecnico);
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AdminTecnicos", "❌ Error al verificar mantenimientos: " + e.getMessage());
                    mostrarCargando(false);
                    Toast.makeText(this, "Error al verificar mantenimientos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void eliminarDeAuthenticationYFirestore(Usuario tecnico) {
        android.util.Log.d("AdminTecnicos", "🔥 Llamando a Cloud Function para eliminar de Authentication y Firestore...");

        Map<String, Object> data = new HashMap<>();
        data.put("userId", tecnico.getUserId());

        FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("eliminarUsuario")
                .call(data)
                .addOnSuccessListener(result -> {
                    android.util.Log.d("AdminTecnicos", "✅ Usuario eliminado completamente (Authentication + Firestore)");
                    mostrarCargando(false);
                    Toast.makeText(this, "✅ " + tecnico.getNombre() + " eliminado completamente", Toast.LENGTH_LONG).show();
                    cargarTecnicos();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AdminTecnicos", "❌ Error al eliminar usuario: " + e.getMessage());
                    mostrarCargando(false);
                    Toast.makeText(this, "Error al eliminar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void eliminarSoloDeFirestore(Usuario tecnico) {
        android.util.Log.d("AdminTecnicos", "📄 Llamando a Cloud Function (usuario huérfano)...");

        Map<String, Object> data = new HashMap<>();
        data.put("userId", tecnico.getUserId());

        FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("eliminarUsuario")
                .call(data)
                .addOnSuccessListener(result -> {
                    android.util.Log.d("AdminTecnicos", "✅ Usuario huérfano eliminado de Firestore");
                    mostrarCargando(false);
                    Toast.makeText(this, "✅ " + tecnico.getNombre() + " eliminado correctamente", Toast.LENGTH_LONG).show();
                    cargarTecnicos();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AdminTecnicos", "❌ Error: " + e.getMessage());
                    mostrarCargando(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Recargar lista después de crear/editar
            cargarTecnicos();
        }
    }
}
