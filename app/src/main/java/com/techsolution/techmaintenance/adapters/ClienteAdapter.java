package com.techsolution.techmaintenance.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.models.Cliente;

import java.util.ArrayList;
import java.util.List;

public class ClienteAdapter extends RecyclerView.Adapter<ClienteAdapter.ClienteViewHolder> {

    private Context context;
    private List<Cliente> listaClientes;
    private List<Cliente> listaClientesFiltrada;
    private OnClienteClickListener listener;
    private boolean esAdmin; // Control de permisos

    // Interface para manejar clicks
    public interface OnClienteClickListener {
        void onVerClick(Cliente cliente);
        void onEditarClick(Cliente cliente);
    }

    public ClienteAdapter(Context context, List<Cliente> listaClientes, boolean esAdmin, OnClienteClickListener listener) {
        this.context = context;
        this.listaClientes = new ArrayList<>(); // Lista independiente
        this.listaClientesFiltrada = new ArrayList<>(); // Lista independiente
        this.listener = listener;
        this.esAdmin = esAdmin;

        // Copiar datos iniciales
        if (listaClientes != null) {
            this.listaClientes.addAll(listaClientes);
            this.listaClientesFiltrada.addAll(listaClientes);
        }

        android.util.Log.d("ClienteAdapter", "Constructor: " + this.listaClientesFiltrada.size() + " clientes, esAdmin=" + esAdmin);
    }

    @NonNull
    @Override
    public ClienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cliente_card, parent, false);
        return new ClienteViewHolder(view);
    }

    // ✅ CORRECCIÓN PARA ClienteAdapter.java
// Método onBindViewHolder mejorado con logging

    @Override
    public void onBindViewHolder(@NonNull ClienteViewHolder holder, int position) {
        Cliente cliente = listaClientesFiltrada.get(position);

        // ⭐ LOGGING: Verificar que clienteId existe
        android.util.Log.d("ClienteAdapter", "📋 Binding cliente: " + cliente.getNombreEmpresa() +
                ", ID: " + cliente.getClienteId());

        // Nombre de la empresa
        holder.tvNombreEmpresa.setText(cliente.getNombreEmpresa());

        // Nombre y cargo del contacto
        holder.tvNombreContacto.setText(cliente.getNombreContacto());

        if (cliente.getCargoContacto() != null && !cliente.getCargoContacto().isEmpty()) {
            holder.tvCargoContacto.setVisibility(View.VISIBLE);
            holder.tvCargoContacto.setText(" - " + cliente.getCargoContacto());
        } else {
            holder.tvCargoContacto.setVisibility(View.GONE);
        }

        // Email
        holder.tvEmailContacto.setText(cliente.getEmailContacto());

        // Teléfono
        holder.tvTelefonoContacto.setText(cliente.getTelefonoContacto());

        // Dirección
        holder.tvDireccion.setText(cliente.getDireccion());

        // Total de equipos
        int totalEquipos = cliente.getTotalEquipos();
        if (totalEquipos == 0) {
            holder.tvTotalEquipos.setText("Sin equipos");
        } else if (totalEquipos == 1) {
            holder.tvTotalEquipos.setText("1 equipo");
        } else {
            holder.tvTotalEquipos.setText(totalEquipos + " equipos");
        }

        // ✅ CORRECCIÓN: Click en Ver con validación
        holder.btnVer.setOnClickListener(v -> {
            android.util.Log.d("ClienteAdapter", "🔍 Click en VER - Cliente: " +
                    cliente.getNombreEmpresa() + ", ID: " + cliente.getClienteId());

            if (listener != null) {
                // Verificar que el cliente tenga ID antes de pasar
                if (cliente.getClienteId() == null || cliente.getClienteId().isEmpty()) {
                    android.util.Log.e("ClienteAdapter", "❌ ERROR: Cliente sin ID!");
                    Toast.makeText(context, "Error: Cliente sin ID", Toast.LENGTH_SHORT).show();
                    return;
                }
                listener.onVerClick(cliente);
            }
        });

        // ✅ CORRECCIÓN: Click en Editar con validación + control de permisos
        if (esAdmin) {
            holder.btnEditar.setVisibility(View.VISIBLE);
            holder.btnEditar.setOnClickListener(v -> {
                android.util.Log.d("ClienteAdapter", "✏️ Click en EDITAR - Cliente: " +
                        cliente.getNombreEmpresa() + ", ID: " + cliente.getClienteId());

                if (listener != null) {
                    // Verificar que el cliente tenga ID antes de pasar
                    if (cliente.getClienteId() == null || cliente.getClienteId().isEmpty()) {
                        android.util.Log.e("ClienteAdapter", "❌ ERROR: Cliente sin ID!");
                        Toast.makeText(context, "Error: Cliente sin ID", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    listener.onEditarClick(cliente);
                }
            });
        } else {
            // Técnico: ocultar botón editar
            holder.btnEditar.setVisibility(View.GONE);
        }

        // ✅ CORRECCIÓN: Click en la card completa con validación
        holder.itemView.setOnClickListener(v -> {
            android.util.Log.d("ClienteAdapter", "👆 Click en CARD - Cliente: " +
                    cliente.getNombreEmpresa());

            if (listener != null) {
                if (cliente.getClienteId() == null || cliente.getClienteId().isEmpty()) {
                    android.util.Log.e("ClienteAdapter", "❌ ERROR: Cliente sin ID!");
                    Toast.makeText(context, "Error: Cliente sin ID", Toast.LENGTH_SHORT).show();
                    return;
                }
                listener.onVerClick(cliente);
            }
        });
    }

    @Override
    public int getItemCount() {
        int count = listaClientesFiltrada.size();
        android.util.Log.d("ClienteAdapter", "getItemCount() = " + count);
        return count;
    }

    // Método para filtrar clientes
    public void filtrar(String query) {
        listaClientesFiltrada.clear();

        if (query.isEmpty()) {
            listaClientesFiltrada.addAll(listaClientes);
        } else {
            String queryLower = query.toLowerCase().trim();

            for (Cliente cliente : listaClientes) {
                // Buscar en nombre de empresa
                if (cliente.getNombreEmpresa().toLowerCase().contains(queryLower)) {
                    listaClientesFiltrada.add(cliente);
                    continue;
                }

                // Buscar en nombre de contacto
                if (cliente.getNombreContacto().toLowerCase().contains(queryLower)) {
                    listaClientesFiltrada.add(cliente);
                    continue;
                }

                // Buscar en email
                if (cliente.getEmailContacto().toLowerCase().contains(queryLower)) {
                    listaClientesFiltrada.add(cliente);
                }
            }
        }

        notifyDataSetChanged();
    }

    // Método para filtrar por tipo
    public void filtrarPorTipo(String filtro) {
        listaClientesFiltrada.clear();

        switch (filtro) {
            case "todos":
                listaClientesFiltrada.addAll(listaClientes);
                break;

            case "con_equipos":
                for (Cliente cliente : listaClientes) {
                    if (cliente.getTotalEquipos() > 0) {
                        listaClientesFiltrada.add(cliente);
                    }
                }
                break;

            case "sin_equipos":
                for (Cliente cliente : listaClientes) {
                    if (cliente.getTotalEquipos() == 0) {
                        listaClientesFiltrada.add(cliente);
                    }
                }
                break;

            case "recientes":
                // Ordenar por fecha de registro (más recientes primero)
                List<Cliente> clientesOrdenados = new ArrayList<>(listaClientes);
                clientesOrdenados.sort((c1, c2) -> {
                    if (c1.getFechaRegistro() == null || c2.getFechaRegistro() == null) {
                        return 0;
                    }
                    return c2.getFechaRegistro().compareTo(c1.getFechaRegistro());
                });
                listaClientesFiltrada.addAll(clientesOrdenados);
                break;
        }

        notifyDataSetChanged();
    }

    // Actualizar lista completa
    public void actualizarLista(List<Cliente> nuevaLista) {
        android.util.Log.d("ClienteAdapter", "actualizarLista() recibió: " + (nuevaLista != null ? nuevaLista.size() : 0) + " clientes");

        this.listaClientes.clear();
        this.listaClientesFiltrada.clear();

        if (nuevaLista != null && !nuevaLista.isEmpty()) {
            this.listaClientes.addAll(nuevaLista);
            this.listaClientesFiltrada.addAll(nuevaLista);
        }

        android.util.Log.d("ClienteAdapter", "Después de actualizar - listaClientes: " + this.listaClientes.size());
        android.util.Log.d("ClienteAdapter", "Después de actualizar - listaClientesFiltrada: " + this.listaClientesFiltrada.size());

        notifyDataSetChanged();
    }

    // ViewHolder
    static class ClienteViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreEmpresa, tvNombreContacto, tvCargoContacto;
        TextView tvEmailContacto, tvTelefonoContacto, tvDireccion;
        TextView tvTotalEquipos;
        MaterialButton btnVer, btnEditar;

        public ClienteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreEmpresa = itemView.findViewById(R.id.tvNombreEmpresa);
            tvNombreContacto = itemView.findViewById(R.id.tvNombreContacto);
            tvCargoContacto = itemView.findViewById(R.id.tvCargoContacto);
            tvEmailContacto = itemView.findViewById(R.id.tvEmailContacto);
            tvTelefonoContacto = itemView.findViewById(R.id.tvTelefonoContacto);
            tvDireccion = itemView.findViewById(R.id.tvDireccion);
            tvTotalEquipos = itemView.findViewById(R.id.tvTotalEquipos);
            btnVer = itemView.findViewById(R.id.btnVerCliente);
            btnEditar = itemView.findViewById(R.id.btnEditarCliente);
        }
    }
}