package com.techsolution.techmaintenance.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.techsolution.techmaintenance.R;

import java.util.List;
import java.util.Locale;

public class TecnicoEstadisticaAdapter extends RecyclerView.Adapter<TecnicoEstadisticaAdapter.ViewHolder> {

    public static class TecnicoEstadistica {
        private String nombre;
        private int totalServicios;
        private double calificacionPromedio;

        public TecnicoEstadistica(String nombre, int totalServicios, double calificacionPromedio) {
            this.nombre = nombre;
            this.totalServicios = totalServicios;
            this.calificacionPromedio = calificacionPromedio;
        }

        public String getNombre() {
            return nombre;
        }

        public int getTotalServicios() {
            return totalServicios;
        }

        public double getCalificacionPromedio() {
            return calificacionPromedio;
        }
    }

    private Context context;
    private List<TecnicoEstadistica> lista;

    public TecnicoEstadisticaAdapter(Context context, List<TecnicoEstadistica> lista) {
        this.context = context;
        this.lista = lista;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tecnico_estadistica, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TecnicoEstadistica tecnico = lista.get(position);

        holder.tvNombreTecnico.setText(tecnico.getNombre());
        holder.tvServiciosTecnico.setText(tecnico.getTotalServicios() + " servicios");
        holder.tvCalificacionTecnico.setText(String.format(Locale.getDefault(), "⭐ %.1f", tecnico.getCalificacionPromedio()));
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreTecnico, tvServiciosTecnico, tvCalificacionTecnico;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombreTecnico = itemView.findViewById(R.id.tvNombreTecnico);
            tvServiciosTecnico = itemView.findViewById(R.id.tvServiciosTecnico);
            tvCalificacionTecnico = itemView.findViewById(R.id.tvCalificacionTecnico);
        }
    }
}
