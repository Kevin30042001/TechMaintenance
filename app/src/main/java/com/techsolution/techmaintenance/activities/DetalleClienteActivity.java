package com.techsolution.techmaintenance.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.models.Cliente;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class DetalleClienteActivity extends AppCompatActivity {

    // Firebase
    private FirebaseFirestore db;
    private com.google.firebase.auth.FirebaseAuth auth;

    // Vistas
    private Toolbar toolbar;
    private TextView tvNombreEmpresa, tvNombreContacto, tvCargoContacto;
    private TextView tvEmailContacto, tvTelefonoContacto, tvTelefonoAlternativo;
    private TextView tvDireccion, tvReferenciaDireccion;
    private TextView tvTotalEquipos, tvFechaRegistro;
    private LinearLayout layoutTelefonoAlternativo, layoutReferencia;
    private MaterialButton btnEditarCliente, btnVerEquipos, btnEliminarCliente;
    private ProgressBar progressBar;

    private Cliente cliente;
    private boolean esAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_cliente);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        auth = com.google.firebase.auth.FirebaseAuth.getInstance();

        // Obtener cliente del Intent usando Parcelable
        if (getIntent().hasExtra("cliente")) {
            // Para Android 13+ (API 33+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                cliente = getIntent().getParcelableExtra("cliente", Cliente.class);
            } else {
                // Para versiones anteriores
                cliente = getIntent().getParcelableExtra("cliente");
            }
        } else {
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

        // Mostrar datos
        mostrarDatos();

        // Verificar rol y configurar permisos
        verificarRolUsuario();

        // Configurar listeners
        configurarListeners();
    }

    /**
     * Verifica el rol del usuario y muestra/oculta botones según permisos
     */
    private void verificarRolUsuario() {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("usuarios").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        com.techsolution.techmaintenance.models.Usuario usuario = documentSnapshot.toObject(com.techsolution.techmaintenance.models.Usuario.class);
                        esAdmin = usuario != null && "admin".equals(usuario.getRol());

                        // Configurar visibilidad de botones según rol
                        if (esAdmin) {
                            // Admin: puede editar y eliminar
                            btnEditarCliente.setVisibility(View.VISIBLE);
                            btnEliminarCliente.setVisibility(View.VISIBLE);
                            android.util.Log.d("DetalleCliente", "✅ Admin: botones editar/eliminar visibles");
                        } else {
                            // Técnico: solo lectura
                            btnEditarCliente.setVisibility(View.GONE);
                            btnEliminarCliente.setVisibility(View.GONE);
                            android.util.Log.d("DetalleCliente", "🚫 Técnico: botones editar/eliminar ocultos");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DetalleCliente", "Error al verificar rol: " + e.getMessage());
                });
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        tvNombreEmpresa = findViewById(R.id.tvNombreEmpresa);
        tvNombreContacto = findViewById(R.id.tvNombreContacto);
        tvCargoContacto = findViewById(R.id.tvCargoContacto);
        tvEmailContacto = findViewById(R.id.tvEmailContacto);
        tvTelefonoContacto = findViewById(R.id.tvTelefonoContacto);
        tvTelefonoAlternativo = findViewById(R.id.tvTelefonoAlternativo);
        tvDireccion = findViewById(R.id.tvDireccion);
        tvReferenciaDireccion = findViewById(R.id.tvReferenciaDireccion);
        tvTotalEquipos = findViewById(R.id.tvTotalEquipos);
        tvFechaRegistro = findViewById(R.id.tvFechaRegistro);
        layoutTelefonoAlternativo = findViewById(R.id.layoutTelefonoAlternativo);
        layoutReferencia = findViewById(R.id.layoutReferencia);
        btnEditarCliente = findViewById(R.id.btnEditarCliente);
        btnVerEquipos = findViewById(R.id.btnVerEquipos);
        btnEliminarCliente = findViewById(R.id.btnEliminarCliente);
    }

    private void mostrarDatos() {
        // Nombre empresa
        tvNombreEmpresa.setText(cliente.getNombreEmpresa());

        // Contacto
        tvNombreContacto.setText(cliente.getNombreContacto());

        if (cliente.getCargoContacto() != null && !cliente.getCargoContacto().isEmpty()) {
            tvCargoContacto.setText(cliente.getCargoContacto());
            tvCargoContacto.setVisibility(View.VISIBLE);
        } else {
            tvCargoContacto.setVisibility(View.GONE);
        }

        // Email y teléfonos
        tvEmailContacto.setText(cliente.getEmailContacto());
        tvTelefonoContacto.setText(cliente.getTelefonoContacto());

        // Teléfono alternativo (opcional)
        if (cliente.getTelefonoAlternativo() != null && !cliente.getTelefonoAlternativo().isEmpty()) {
            tvTelefonoAlternativo.setText(cliente.getTelefonoAlternativo());
            layoutTelefonoAlternativo.setVisibility(View.VISIBLE);
        } else {
            layoutTelefonoAlternativo.setVisibility(View.GONE);
        }

        // Dirección
        tvDireccion.setText(cliente.getDireccion());

        // Referencias (opcional)
        if (cliente.getReferenciaDireccion() != null && !cliente.getReferenciaDireccion().isEmpty()) {
            tvReferenciaDireccion.setText(cliente.getReferenciaDireccion());
            layoutReferencia.setVisibility(View.VISIBLE);
        } else {
            layoutReferencia.setVisibility(View.GONE);
        }

        // Estadísticas
        int totalEquipos = cliente.getTotalEquipos();
        if (totalEquipos == 0) {
            tvTotalEquipos.setText("Sin equipos registrados");
        } else if (totalEquipos == 1) {
            tvTotalEquipos.setText("1 equipo registrado");
        } else {
            tvTotalEquipos.setText(totalEquipos + " equipos registrados");
        }

        // Fecha de registro
        if (cliente.getFechaRegistro() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String fecha = sdf.format(cliente.getFechaRegistro().toDate());
            tvFechaRegistro.setText("Registrado: " + fecha);
        }
    }

    private void configurarListeners() {
        // Botón editar
        btnEditarCliente.setOnClickListener(v -> {
            Intent intent = new Intent(this, AgregarEditarClienteActivity.class);
            intent.putExtra("cliente", cliente);
            intent.putExtra("modo", "editar");
            startActivity(intent);
        });

        // Botón ver equipos
        btnVerEquipos.setOnClickListener(v -> abrirEquiposDelCliente());

        // ⭐ NUEVO: Botón eliminar cliente
        btnEliminarCliente.setOnClickListener(v -> mostrarDialogoEliminar());

        // Toolbar navigation
        toolbar.setNavigationOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });
    }

    /**
     * ⭐ Muestra un diálogo de confirmación antes de eliminar
     */
    private void mostrarDialogoEliminar() {
        // Verificar si el cliente tiene equipos registrados
        if (cliente.getTotalEquipos() > 0) {
            mostrarDialogoClienteConEquipos();
            return;
        }

        // Diálogo de confirmación
        new MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar Cliente")
                .setMessage("¿Estás seguro de que deseas eliminar a " +
                        cliente.getNombreEmpresa() + "?\n\nEsta acción no se puede deshacer.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarCliente())
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * ⭐ Muestra advertencia si el cliente tiene equipos registrados
     */
    private void mostrarDialogoClienteConEquipos() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("No se puede eliminar")
                .setMessage("Este cliente tiene " + cliente.getTotalEquipos() +
                        " equipo(s) registrado(s).\n\n" +
                        "Primero debes eliminar o reasignar los equipos antes de eliminar el cliente.")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("Entendido", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("Ver Equipos", (dialog, which) -> abrirEquiposDelCliente())
                .show();
    }

    /**
     * ⭐ Elimina el cliente de Firestore
     */
    private void eliminarCliente() {
        if (cliente.getClienteId() == null || cliente.getClienteId().isEmpty()) {
            Toast.makeText(this, "Error: Cliente sin ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Deshabilitar botones durante la eliminación
        btnEliminarCliente.setEnabled(false);
        btnEditarCliente.setEnabled(false);
        btnVerEquipos.setEnabled(false);

        android.util.Log.d("DetalleCliente", "🗑️ Eliminando cliente: " + cliente.getClienteId());

        db.collection("clientes")
                .document(cliente.getClienteId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("DetalleCliente", "✅ Cliente eliminado exitosamente");

                    Toast.makeText(this,
                            "Cliente " + cliente.getNombreEmpresa() + " eliminado",
                            Toast.LENGTH_SHORT).show();

                    // Cerrar activity y volver a la lista
                    setResult(RESULT_OK); // Notificar que se eliminó el cliente
                    finish();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DetalleCliente", "❌ Error al eliminar: " + e.getMessage());

                    // Reactivar botones
                    btnEliminarCliente.setEnabled(true);
                    btnEditarCliente.setEnabled(true);
                    btnVerEquipos.setEnabled(true);

                    Toast.makeText(this,
                            "Error al eliminar: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar datos por si fueron editados
        // TODO: Recargar desde Firestore si es necesario
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }

    /**
     * Abre la actividad de lista de equipos filtrada por este cliente
     */
    private void abrirEquiposDelCliente() {
        android.util.Log.d("DetalleCliente", "🔍 ========== ABRIENDO EQUIPOS DEL CLIENTE ==========");

        if (cliente == null) {
            android.util.Log.e("DetalleCliente", "❌ ERROR: cliente es NULL");
            Toast.makeText(this, "Error: Cliente no válido", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d("DetalleCliente", "📋 Cliente:");
        android.util.Log.d("DetalleCliente", "   - Nombre: " + cliente.getNombreEmpresa());
        android.util.Log.d("DetalleCliente", "   - ClienteId: " + (cliente.getClienteId() != null ? cliente.getClienteId() : "NULL"));
        android.util.Log.d("DetalleCliente", "   - Total equipos: " + cliente.getTotalEquipos());

        if (cliente.getClienteId() == null || cliente.getClienteId().isEmpty()) {
            android.util.Log.e("DetalleCliente", "❌ ERROR: clienteId es NULL o vacío");
            Toast.makeText(this, "Error: Cliente sin ID asignado", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ListaEquiposActivity.class);
        intent.putExtra("clienteId", cliente.getClienteId());
        intent.putExtra("nombreCliente", cliente.getNombreEmpresa());

        android.util.Log.d("DetalleCliente", "✅ Intent creado con extras:");
        android.util.Log.d("DetalleCliente", "   - clienteId = '" + cliente.getClienteId() + "'");
        android.util.Log.d("DetalleCliente", "   - nombreCliente = '" + cliente.getNombreEmpresa() + "'");
        android.util.Log.d("DetalleCliente", "🚀 Iniciando ListaEquiposActivity...");

        startActivity(intent);
    }
}