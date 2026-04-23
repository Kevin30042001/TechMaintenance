package com.techsolution.techmaintenance.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.models.Usuario;

import java.util.ArrayList;
import java.util.List;

public class TecnicoAdapter extends RecyclerView.Adapter<TecnicoAdapter.TecnicoViewHolder> {

    private Context context;
    private List<Usuario> listaTecnicos;
    private List<Usuario> listaTecnicosFiltrada;
    private OnTecnicoClickListener listener;

    // Interface para manejar clicks
    public interface OnTecnicoClickListener {
        void onVerClick(Usuario tecnico);
        void onEditarClick(Usuario tecnico);
        void onCambiarEstadoClick(Usuario tecnico);
        void onEliminarClick(Usuario tecnico);
    }

    public TecnicoAdapter(Context context, List<Usuario> listaTecnicos, OnTecnicoClickListener listener) {
        this.context = context;
        this.listaTecnicos = new ArrayList<>();
        this.listaTecnicosFiltrada = new ArrayList<>();
        this.listener = listener;

        // Copiar datos iniciales
        if (listaTecnicos != null) {
            this.listaTecnicos.addAll(listaTecnicos);
            this.listaTecnicosFiltrada.addAll(listaTecnicos);
        }

        android.util.Log.d("TecnicoAdapter", "Constructor: " + this.listaTecnicosFiltrada.size() + " técnicos");
    }

    @NonNull
    @Override
    public TecnicoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tecnico_card, parent, false);
        return new TecnicoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TecnicoViewHolder holder, int position) {
        Usuario tecnico = listaTecnicosFiltrada.get(position);

        // Nombre
        holder.tvNombreTecnico.setText(tecnico.getNombre());

        // Email
        holder.tvEmailTecnico.setText(tecnico.getEmail());

        // Estado
        boolean esActivo = "activo".equals(tecnico.getEstado());
        if (esActivo) {
            holder.chipEstado.setText("Activo");
            holder.chipEstado.setChipBackgroundColorResource(R.color.success);
            holder.btnCambiarEstado.setText("DESACTIVAR");
            holder.btnCambiarEstado.setTextColor(context.getResources().getColor(R.color.error));
        } else {
            holder.chipEstado.setText("Inactivo");
            holder.chipEstado.setChipBackgroundColorResource(R.color.secondary_text);
            holder.btnCambiarEstado.setText("ACTIVAR");
            holder.btnCambiarEstado.setTextColor(context.getResources().getColor(R.color.success));
        }

        // Estadísticas
        if (tecnico.getEstadisticas() != null) {
            holder.tvServiciosCompletados.setText(String.valueOf(tecnico.getEstadisticas().getServiciosCompletados()));

            double calificacion = tecnico.getEstadisticas().getCalificacionPromedio();
            holder.tvCalificacion.setText(String.format("⭐ %.1f", calificacion));

            holder.tvEficiencia.setText(String.format("%.0f%%", tecnico.getEstadisticas().getEficiencia()));
        } else {
            holder.tvServiciosCompletados.setText("0");
            holder.tvCalificacion.setText("⭐ 0.0");
            holder.tvEficiencia.setText("0%");
        }

        // Listeners
        holder.btnVerEstadisticas.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVerClick(tecnico);
            }
        });

        holder.btnEditarTecnico.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditarClick(tecnico);
            }
        });

        holder.btnCambiarEstado.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCambiarEstadoClick(tecnico);
            }
        });

        holder.btnEliminarTecnico.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEliminarClick(tecnico);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaTecnicosFiltrada.size();
    }

    // Método para filtrar técnicos por texto
    public void filtrar(String query) {
        listaTecnicosFiltrada.clear();

        if (query.isEmpty()) {
            listaTecnicosFiltrada.addAll(listaTecnicos);
        } else {
            String queryLower = query.toLowerCase().trim();

            for (Usuario tecnico : listaTecnicos) {
                // Buscar en nombre
                if (tecnico.getNombre().toLowerCase().contains(queryLower)) {
                    listaTecnicosFiltrada.add(tecnico);
                    continue;
                }

                // Buscar en email
                if (tecnico.getEmail().toLowerCase().contains(queryLower)) {
                    listaTecnicosFiltrada.add(tecnico);
                }
            }
        }

        notifyDataSetChanged();
    }

    // Método para filtrar por estado
    public void filtrarPorEstado(String filtro) {
        listaTecnicosFiltrada.clear();

        if ("todos".equals(filtro)) {
            listaTecnicosFiltrada.addAll(listaTecnicos);
        } else if ("activos".equals(filtro)) {
            for (Usuario tecnico : listaTecnicos) {
                if ("activo".equals(tecnico.getEstado())) {
                    listaTecnicosFiltrada.add(tecnico);
                }
            }
        } else if ("inactivos".equals(filtro)) {
            for (Usuario tecnico : listaTecnicos) {
                if ("inactivo".equals(tecnico.getEstado())) {
                    listaTecnicosFiltrada.add(tecnico);
                }
            }
        }

        notifyDataSetChanged();
    }

    // Actualizar lista completa
    public void actualizarLista(List<Usuario> nuevaLista) {
        android.util.Log.d("TecnicoAdapter", "actualizarLista() recibió: " + (nuevaLista != null ? nuevaLista.size() : 0) + " técnicos");

        this.listaTecnicos.clear();
        this.listaTecnicosFiltrada.clear();

        if (nuevaLista != null && !nuevaLista.isEmpty()) {
            this.listaTecnicos.addAll(nuevaLista);
            this.listaTecnicosFiltrada.addAll(nuevaLista);
        }

        notifyDataSetChanged();
    }

    // ViewHolder
    static class TecnicoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFotoPerfil;
        TextView tvNombreTecnico, tvEmailTecnico;
        Chip chipEstado;
        TextView tvServiciosCompletados, tvCalificacion, tvEficiencia;
        MaterialButton btnVerEstadisticas, btnEditarTecnico, btnCambiarEstado, btnEliminarTecnico;

        public TecnicoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFotoPerfil = itemView.findViewById(R.id.ivFotoPerfil);
            tvNombreTecnico = itemView.findViewById(R.id.tvNombreTecnico);
            tvEmailTecnico = itemView.findViewById(R.id.tvEmailTecnico);
            chipEstado = itemView.findViewById(R.id.chipEstado);
            tvServiciosCompletados = itemView.findViewById(R.id.tvServiciosCompletados);
            tvCalificacion = itemView.findViewById(R.id.tvCalificacion);
            tvEficiencia = itemView.findViewById(R.id.tvEficiencia);
            btnVerEstadisticas = itemView.findViewById(R.id.btnVerEstadisticas);
            btnEditarTecnico = itemView.findViewById(R.id.btnEditarTecnico);
            btnCambiarEstado = itemView.findViewById(R.id.btnCambiarEstado);
            btnEliminarTecnico = itemView.findViewById(R.id.btnEliminarTecnico);
        }
    }
}
