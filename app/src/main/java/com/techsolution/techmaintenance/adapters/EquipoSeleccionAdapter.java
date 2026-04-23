package com.techsolution.techmaintenance.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.models.Equipo;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter para seleccionar múltiples equipos con checkboxes
 */
public class EquipoSeleccionAdapter extends RecyclerView.Adapter<EquipoSeleccionAdapter.ViewHolder> {

    private List<Equipo> listaEquipos;
    private List<String> equiposSeleccionados; // IDs de equipos seleccionados
    private List<String> equiposOcupados; // IDs de equipos ocupados
    private OnSeleccionChangeListener listener;

    public interface OnSeleccionChangeListener {
        void onSeleccionChange(int cantidadSeleccionados);
    }

    public EquipoSeleccionAdapter(OnSeleccionChangeListener listener) {
        this.listaEquipos = new ArrayList<>();
        this.equiposSeleccionados = new ArrayList<>();
        this.equiposOcupados = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_equipo_seleccion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Equipo equipo = listaEquipos.get(position);
        boolean estaOcupado = equiposOcupados.contains(equipo.getEquipoId());
        boolean estaSeleccionado = equiposSeleccionados.contains(equipo.getEquipoId());

        // Información del equipo
        String nombreEquipo = equipo.getTipo() + " - " + equipo.getMarca() + " " + equipo.getModelo();
        holder.tvNombreEquipo.setText(nombreEquipo);
        holder.tvNumeroSerie.setText("Serie: " + equipo.getNumeroSerie());

        // Ubicación (si existe)
        if (equipo.getUbicacionEspecifica() != null && !equipo.getUbicacionEspecifica().isEmpty()) {
            holder.tvUbicacion.setVisibility(View.VISIBLE);
            holder.tvUbicacion.setText("📍 " + equipo.getUbicacionEspecifica());
        } else {
            holder.tvUbicacion.setVisibility(View.GONE);
        }

        // Indicador de disponibilidad
        if (estaOcupado) {
            holder.tvEstado.setText("🔧 Ocupado");
            holder.tvEstado.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_orange_dark));
            holder.checkbox.setEnabled(false);
            holder.checkbox.setChecked(false);
        } else {
            holder.tvEstado.setText("✅ Disponible");
            holder.tvEstado.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_green_dark));
            holder.checkbox.setEnabled(true);
            holder.checkbox.setChecked(estaSeleccionado);
        }

        // Listener del checkbox
        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !estaOcupado) {
                if (!equiposSeleccionados.contains(equipo.getEquipoId())) {
                    equiposSeleccionados.add(equipo.getEquipoId());
                }
            } else {
                equiposSeleccionados.remove(equipo.getEquipoId());
            }

            // Notificar cambio
            if (listener != null) {
                listener.onSeleccionChange(equiposSeleccionados.size());
            }
        });

        // Click en el item también togglea el checkbox (si está habilitado)
        holder.itemView.setOnClickListener(v -> {
            if (!estaOcupado) {
                holder.checkbox.toggle();
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaEquipos.size();
    }

    public void setEquipos(List<Equipo> equipos) {
        this.listaEquipos = equipos;
        notifyDataSetChanged();
    }

    public void setEquiposOcupados(List<String> equiposOcupados) {
        this.equiposOcupados = equiposOcupados;
        notifyDataSetChanged();
    }

    public List<Equipo> getEquiposSeleccionados() {
        List<Equipo> seleccionados = new ArrayList<>();
        for (Equipo equipo : listaEquipos) {
            if (equiposSeleccionados.contains(equipo.getEquipoId())) {
                seleccionados.add(equipo);
            }
        }
        return seleccionados;
    }

    public int getCantidadSeleccionados() {
        return equiposSeleccionados.size();
    }

    public void limpiarSeleccion() {
        equiposSeleccionados.clear();
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSeleccionChange(0);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkbox;
        TextView tvNombreEquipo;
        TextView tvNumeroSerie;
        TextView tvUbicacion;
        TextView tvEstado;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.checkboxEquipo);
            tvNombreEquipo = itemView.findViewById(R.id.tvNombreEquipo);
            tvNumeroSerie = itemView.findViewById(R.id.tvNumeroSerie);
            tvUbicacion = itemView.findViewById(R.id.tvUbicacion);
            tvEstado = itemView.findViewById(R.id.tvEstadoEquipo);
        }
    }
}
