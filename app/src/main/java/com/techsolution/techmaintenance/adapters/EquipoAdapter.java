package com.techsolution.techmaintenance.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.models.Equipo;

import java.util.ArrayList;
import java.util.List;

public class EquipoAdapter extends RecyclerView.Adapter<EquipoAdapter.EquipoViewHolder> {

    private Context context;
    private List<Equipo> listaEquipos;
    private List<Equipo> listaEquiposFiltrada;
    private OnEquipoClickListener listener;
    private boolean esAdmin;

    // Interface para manejar clicks
    public interface OnEquipoClickListener {
        void onVerClick(Equipo equipo);
        void onEditarClick(Equipo equipo);
    }

    public EquipoAdapter(Context context, List<Equipo> listaEquipos, boolean esAdmin, OnEquipoClickListener listener) {
        this.context = context;
        this.listaEquipos = new ArrayList<>();
        this.listaEquiposFiltrada = new ArrayList<>();
        this.listener = listener;
        this.esAdmin = esAdmin;

        // Copiar datos iniciales
        if (listaEquipos != null) {
            this.listaEquipos.addAll(listaEquipos);
            this.listaEquiposFiltrada.addAll(listaEquipos);
        }

        android.util.Log.d("EquipoAdapter", "Constructor: " + this.listaEquiposFiltrada.size() + " equipos");
    }

    @NonNull
    @Override
    public EquipoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_equipo_card, parent, false);
        return new EquipoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipoViewHolder holder, int position) {
        Equipo equipo = listaEquiposFiltrada.get(position);

        android.util.Log.d("EquipoAdapter", "📋 Binding equipo: " + equipo.getMarca() + " " + equipo.getModelo() +
                ", ID: " + equipo.getEquipoId());

        // Cargar imagen del equipo con Glide
        if (equipo.getFotografiaURL() != null && !equipo.getFotografiaURL().isEmpty()) {
            Glide.with(context)
                    .load(equipo.getFotografiaURL())
                    .placeholder(R.drawable.ic_computer)
                    .error(R.drawable.ic_computer)
                    .centerCrop()
                    .into(holder.ivFotoEquipo);
        } else {
            holder.ivFotoEquipo.setImageResource(R.drawable.ic_computer);
        }

        // Marca y modelo
        String nombreEquipo = equipo.getMarca() + " " + equipo.getModelo();
        holder.tvNombreEquipo.setText(nombreEquipo);

        // Número de serie
        holder.tvNumeroSerie.setText("S/N: " + equipo.getNumeroSerie());

        // Cliente propietario
        // TODO: Cargar nombre del cliente desde Firestore (evitar N+1)
        holder.tvCliente.setText("Cliente"); // Placeholder

        // Ubicación específica
        holder.tvUbicacion.setText(equipo.getUbicacionEspecifica());

        // Estado del equipo con indicador visual
        configurarEstado(holder, equipo.getEstado());

        // Total de mantenimientos
        int totalMant = equipo.getTotalMantenimientos();
        if (totalMant == 0) {
            holder.tvTotalMantenimientos.setText("Sin mantenimientos");
        } else if (totalMant == 1) {
            holder.tvTotalMantenimientos.setText("1 mantenimiento");
        } else {
            holder.tvTotalMantenimientos.setText(totalMant + " mantenimientos");
        }

        // Último mantenimiento (si existe)
        // TODO: Mostrar fecha del último mantenimiento

        // Click en Ver
        holder.btnVer.setOnClickListener(v -> {
            android.util.Log.d("EquipoAdapter", "🔍 Click en VER - Equipo: " +
                    equipo.getMarca() + " " + equipo.getModelo() + ", ID: " + equipo.getEquipoId());

            if (listener != null) {
                if (equipo.getEquipoId() == null || equipo.getEquipoId().isEmpty()) {
                    android.util.Log.e("EquipoAdapter", "❌ ERROR: Equipo sin ID!");
                    Toast.makeText(context, "Error: Equipo sin ID", Toast.LENGTH_SHORT).show();
                    return;
                }
                listener.onVerClick(equipo);
            }
        });

        // Click en Editar (solo visible para admin)
        if (esAdmin) {
            holder.btnEditar.setVisibility(View.VISIBLE);
            holder.btnEditar.setOnClickListener(v -> {
                android.util.Log.d("EquipoAdapter", "✏️ Click en EDITAR - Equipo: " +
                        equipo.getMarca() + " " + equipo.getModelo());

                if (listener != null) {
                    if (equipo.getEquipoId() == null || equipo.getEquipoId().isEmpty()) {
                        android.util.Log.e("EquipoAdapter", "❌ ERROR: Equipo sin ID!");
                        Toast.makeText(context, "Error: Equipo sin ID", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    listener.onEditarClick(equipo);
                }
            });
        } else {
            holder.btnEditar.setVisibility(View.GONE);
        }

        // Click en la card completa
        holder.itemView.setOnClickListener(v -> {
            android.util.Log.d("EquipoAdapter", "👆 Click en CARD - Equipo: " +
                    equipo.getMarca() + " " + equipo.getModelo());

            if (listener != null) {
                if (equipo.getEquipoId() == null || equipo.getEquipoId().isEmpty()) {
                    android.util.Log.e("EquipoAdapter", "❌ ERROR: Equipo sin ID!");
                    Toast.makeText(context, "Error: Equipo sin ID", Toast.LENGTH_SHORT).show();
                    return;
                }
                listener.onVerClick(equipo);
            }
        });
    }

    private void configurarEstado(EquipoViewHolder holder, String estado) {
        switch (estado) {
            case "operativo":
                holder.tvEstado.setText("🟢 Operativo");
                holder.tvEstado.setTextColor(context.getResources().getColor(R.color.success));
                holder.cardView.setStrokeColor(context.getResources().getColor(R.color.success));
                break;
            case "mantenimiento":
                holder.tvEstado.setText("🟡 En Mantenimiento");
                holder.tvEstado.setTextColor(context.getResources().getColor(R.color.warning));
                holder.cardView.setStrokeColor(context.getResources().getColor(R.color.warning));
                break;
            case "fuera_servicio":
                holder.tvEstado.setText("🔴 Fuera de Servicio");
                holder.tvEstado.setTextColor(context.getResources().getColor(R.color.error));
                holder.cardView.setStrokeColor(context.getResources().getColor(R.color.error));
                break;
            default:
                holder.tvEstado.setText("⚪ Estado Desconocido");
                holder.tvEstado.setTextColor(context.getResources().getColor(R.color.secondary_text));
                holder.cardView.setStrokeColor(context.getResources().getColor(R.color.divider_color));
                break;
        }
        holder.cardView.setStrokeWidth(3);
    }

    @Override
    public int getItemCount() {
        int count = listaEquiposFiltrada.size();
        android.util.Log.d("EquipoAdapter", "getItemCount() = " + count);
        return count;
    }

    // Método para filtrar equipos por texto
    public void filtrar(String query) {
        listaEquiposFiltrada.clear();

        if (query.isEmpty()) {
            listaEquiposFiltrada.addAll(listaEquipos);
        } else {
            String queryLower = query.toLowerCase().trim();

            for (Equipo equipo : listaEquipos) {
                // Buscar en marca
                if (equipo.getMarca().toLowerCase().contains(queryLower)) {
                    listaEquiposFiltrada.add(equipo);
                    continue;
                }

                // Buscar en modelo
                if (equipo.getModelo().toLowerCase().contains(queryLower)) {
                    listaEquiposFiltrada.add(equipo);
                    continue;
                }

                // Buscar en número de serie
                if (equipo.getNumeroSerie().toLowerCase().contains(queryLower)) {
                    listaEquiposFiltrada.add(equipo);
                }
            }
        }

        notifyDataSetChanged();
    }

    // Método para filtrar por tipo
    public void filtrarPorTipo(String filtro) {
        listaEquiposFiltrada.clear();

        if ("todos".equals(filtro)) {
            listaEquiposFiltrada.addAll(listaEquipos);
        } else {
            for (Equipo equipo : listaEquipos) {
                if (filtro.equalsIgnoreCase(equipo.getTipo())) {
                    listaEquiposFiltrada.add(equipo);
                }
            }
        }

        notifyDataSetChanged();
    }

    // Método para filtrar por estado
    public void filtrarPorEstado(String estado) {
        listaEquiposFiltrada.clear();

        for (Equipo equipo : listaEquipos) {
            if (estado.equalsIgnoreCase(equipo.getEstado())) {
                listaEquiposFiltrada.add(equipo);
            }
        }

        notifyDataSetChanged();
    }

    // Actualizar lista completa
    public void actualizarLista(List<Equipo> nuevaLista) {
        android.util.Log.d("EquipoAdapter", "actualizarLista() recibió: " + (nuevaLista != null ? nuevaLista.size() : 0) + " equipos");

        this.listaEquipos.clear();
        this.listaEquiposFiltrada.clear();

        if (nuevaLista != null && !nuevaLista.isEmpty()) {
            this.listaEquipos.addAll(nuevaLista);
            this.listaEquiposFiltrada.addAll(nuevaLista);
        }

        android.util.Log.d("EquipoAdapter", "Después de actualizar - listaEquipos: " + this.listaEquipos.size());
        android.util.Log.d("EquipoAdapter", "Después de actualizar - listaEquiposFiltrada: " + this.listaEquiposFiltrada.size());

        notifyDataSetChanged();
    }

    // ViewHolder
    static class EquipoViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView ivFotoEquipo;
        TextView tvNombreEquipo, tvNumeroSerie, tvCliente, tvUbicacion;
        TextView tvEstado, tvTotalMantenimientos;
        MaterialButton btnVer, btnEditar;

        public EquipoViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            ivFotoEquipo = itemView.findViewById(R.id.ivFotoEquipo);
            tvNombreEquipo = itemView.findViewById(R.id.tvNombreEquipo);
            tvNumeroSerie = itemView.findViewById(R.id.tvNumeroSerie);
            tvCliente = itemView.findViewById(R.id.tvCliente);
            tvUbicacion = itemView.findViewById(R.id.tvUbicacion);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            tvTotalMantenimientos = itemView.findViewById(R.id.tvTotalMantenimientos);
            btnVer = itemView.findViewById(R.id.btnVerEquipo);
            btnEditar = itemView.findViewById(R.id.btnEditarEquipo);
        }
    }
}
