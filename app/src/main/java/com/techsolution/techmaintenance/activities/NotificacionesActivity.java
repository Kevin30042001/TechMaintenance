package com.techsolution.techmaintenance.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.adapters.NotificacionAdapter;
import com.techsolution.techmaintenance.models.Notificacion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificacionesActivity extends AppCompatActivity {

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // Vistas
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout layoutVacio;
    private TextView tvMensajeVacio;

    // Adapter y lista
    private NotificacionAdapter adapter;
    private List<Notificacion> listaNotificaciones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificaciones);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Inicializar vistas
        inicializarVistas();

        // Configurar toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Configurar RecyclerView
        configurarRecyclerView();

        // Configurar SwipeRefresh
        configurarSwipeRefresh();

        // Cargar notificaciones
        cargarNotificaciones();
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        layoutVacio = findViewById(R.id.layoutVacio);
        tvMensajeVacio = findViewById(R.id.tvMensajeVacio);

        listaNotificaciones = new ArrayList<>();
    }

    private void configurarRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificacionAdapter(this, listaNotificaciones, new NotificacionAdapter.OnNotificacionClickListener() {
            @Override
            public void onNotificacionClick(Notificacion notificacion) {
                // Marcar como leída
                marcarComoLeida(notificacion);

                // Navegar según el tipo de notificación
                navegarSegunTipo(notificacion);
            }

            @Override
            public void onEliminarClick(Notificacion notificacion) {
                // Mostrar diálogo de confirmación
                mostrarDialogoEliminar(notificacion);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void configurarSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(this::cargarNotificaciones);
        swipeRefresh.setColorSchemeResources(
                R.color.primary,
                R.color.secondary,
                R.color.accent
        );
    }

    private void cargarNotificaciones() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String usuarioId = auth.getCurrentUser().getUid();
        android.util.Log.d("Notificaciones", "🔍 ========================================");
        android.util.Log.d("Notificaciones", "🔍 CARGANDO NOTIFICACIONES");
        android.util.Log.d("Notificaciones", "🔍 ========================================");
        android.util.Log.d("Notificaciones", "👤 Usuario ID actual: " + usuarioId);
        android.util.Log.d("Notificaciones", "📧 Email actual: " + auth.getCurrentUser().getEmail());

        mostrarCarga(true);

        db.collection("notificaciones")
                .whereEqualTo("usuarioId", usuarioId)
                .orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    android.util.Log.d("Notificaciones", "✅ Query exitosa");
                    android.util.Log.d("Notificaciones", "📊 Total documentos encontrados: " + queryDocumentSnapshots.size());

                    listaNotificaciones.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        android.util.Log.d("Notificaciones", "📄 Documento: " + document.getId());
                        android.util.Log.d("Notificaciones", "   - usuarioId: " + document.getString("usuarioId"));
                        android.util.Log.d("Notificaciones", "   - titulo: " + document.getString("titulo"));
                        android.util.Log.d("Notificaciones", "   - mensaje: " + document.getString("mensaje"));
                        android.util.Log.d("Notificaciones", "   - tipo: " + document.getString("tipo"));
                        android.util.Log.d("Notificaciones", "   - leida: " + document.getBoolean("leida"));

                        Notificacion notificacion = document.toObject(Notificacion.class);
                        notificacion.setNotificacionId(document.getId());
                        listaNotificaciones.add(notificacion);
                    }

                    android.util.Log.d("Notificaciones", "📋 Notificaciones agregadas a la lista: " + listaNotificaciones.size());

                    // Actualizar UI
                    if (listaNotificaciones.isEmpty()) {
                        android.util.Log.d("Notificaciones", "⚠️ Lista vacía, mostrando mensaje");
                        mostrarMensajeVacio(true);
                    } else {
                        android.util.Log.d("Notificaciones", "✅ Mostrando " + listaNotificaciones.size() + " notificaciones");
                        mostrarMensajeVacio(false);
                        adapter.actualizarLista(listaNotificaciones);
                    }

                    mostrarCarga(false);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("Notificaciones", "❌ ERROR al cargar notificaciones: " + e.getMessage());
                    android.util.Log.e("Notificaciones", "❌ Stack trace:", e);
                    Toast.makeText(this, "Error al cargar notificaciones: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    mostrarCarga(false);
                });

        // CONSULTA ADICIONAL: Ver TODAS las notificaciones sin filtro
        android.util.Log.d("Notificaciones", "🔍 Consultando TODAS las notificaciones (sin filtro) para debug...");
        db.collection("notificaciones")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    android.util.Log.d("Notificaciones", "📊 Total notificaciones en Firestore: " + querySnapshot.size());
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        android.util.Log.d("Notificaciones", "   - " + doc.getId() + " -> usuarioId: " + doc.getString("usuarioId"));
                    }
                });
    }

    private void marcarComoLeida(Notificacion notificacion) {
        if (notificacion.isLeida()) {
            return; // Ya está leída
        }

        db.collection("notificaciones")
                .document(notificacion.getNotificacionId())
                .update("leida", true)
                .addOnSuccessListener(aVoid -> {
                    // Actualizar en la lista local
                    notificacion.setLeida(true);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("Notificaciones", "Error al marcar como leída", e);
                });
    }

    private void marcarTodasComoLeidas() {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String usuarioId = auth.getCurrentUser().getUid();

        // Mostrar progreso
        progressBar.setVisibility(View.VISIBLE);

        db.collection("notificaciones")
                .whereEqualTo("usuarioId", usuarioId)
                .whereEqualTo("leida", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No hay notificaciones sin leer",
                                Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    // Actualizar todas las notificaciones no leídas
                    int total = queryDocumentSnapshots.size();
                    int[] contador = {0};

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().update("leida", true)
                                .addOnSuccessListener(aVoid -> {
                                    contador[0]++;
                                    if (contador[0] == total) {
                                        // Todas actualizadas, recargar
                                        cargarNotificaciones();
                                        Toast.makeText(this, "Todas las notificaciones marcadas como leídas",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void navegarSegunTipo(Notificacion notificacion) {
        // Navegar a la pantalla correspondiente según el tipo
        String mantenimientoId = notificacion.getMantenimientoId();

        if (mantenimientoId != null && !mantenimientoId.isEmpty()) {
            // Ir al detalle del mantenimiento
            Intent intent = new Intent(this, DetalleMantenimientoActivity.class);
            intent.putExtra("mantenimientoId", mantenimientoId);
            startActivity(intent);
        }
    }

    private void mostrarCarga(boolean mostrar) {
        if (mostrar) {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            layoutVacio.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
        }
    }

    private void mostrarMensajeVacio(boolean mostrar) {
        if (mostrar) {
            layoutVacio.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            layoutVacio.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Muestra diálogo de confirmación para eliminar notificación
     */
    private void mostrarDialogoEliminar(Notificacion notificacion) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Eliminar notificación")
                .setMessage("¿Deseas eliminar esta notificación?\n\n" + notificacion.getTitulo())
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    eliminarNotificacion(notificacion);
                })
                .setNegativeButton("Cancelar", null)
                .setIcon(android.R.drawable.ic_menu_delete)
                .show();
    }

    /**
     * Elimina una notificación de Firestore
     */
    private void eliminarNotificacion(Notificacion notificacion) {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("notificaciones")
                .document(notificacion.getNotificacionId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Eliminar de la lista local
                    adapter.eliminarNotificacion(notificacion);

                    // Verificar si la lista quedó vacía
                    if (adapter.getItemCount() == 0) {
                        mostrarMensajeVacio(true);
                    }

                    Toast.makeText(this, "Notificación eliminada", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al eliminar: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_notificaciones, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_marcar_todas_leidas) {
            marcarTodasComoLeidas();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
