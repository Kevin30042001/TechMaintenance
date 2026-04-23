package com.techsolution.techmaintenance.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.activities.DetalleMantenimientoActivity;
import com.techsolution.techmaintenance.helpers.DateUtils;
import com.techsolution.techmaintenance.models.Mantenimiento;

import java.util.List;

/**
 * Adapter especializado para mostrar mantenimientos en formato timeline vertical
 * Usado en DetalleEquipoActivity para mostrar el historial de mantenimientos
 */
public class MantenimientoTimelineAdapter extends RecyclerView.Adapter<MantenimientoTimelineAdapter.ViewHolder> {

    private Context context;
    private List<Mantenimiento> listaMantenimientos;
    private FirebaseFirestore db;

    public MantenimientoTimelineAdapter(Context context, List<Mantenimiento> listaMantenimientos) {
        this.context = context;
        this.listaMantenimientos = listaMantenimientos;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_mantenimiento_timeline, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Mantenimiento mantenimiento = listaMantenimientos.get(position);

        // Ocultar líneas en primer y último item
        holder.lineaTop.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
        holder.lineaBottom.setVisibility(position == listaMantenimientos.size() - 1 ? View.INVISIBLE : View.VISIBLE);

        // Configurar icono y color según estado
        configurarEstado(holder, mantenimiento.getEstado());

        // Fecha
        if (mantenimiento.getFechaProgramada() != null) {
            holder.tvFecha.setText(DateUtils.formatearFecha(mantenimiento.getFechaProgramada()));
        } else {
            holder.tvFecha.setText("Sin fecha");
        }

        // Tipo de mantenimiento
        holder.tvTipo.setText(obtenerTipoFormateado(mantenimiento.getTipo()));

        // Cargar técnico
        cargarTecnico(holder.tvTecnico, mantenimiento.getTecnicoPrincipalId());

        // Observaciones (solo si existen y no están vacías)
        if (mantenimiento.getObservacionesTecnico() != null && !mantenimiento.getObservacionesTecnico().isEmpty()) {
            holder.tvObservaciones.setVisibility(View.VISIBLE);
            holder.tvObservaciones.setText(mantenimiento.getObservacionesTecnico());
        } else {
            holder.tvObservaciones.setVisibility(View.GONE);
        }

        // Calificación (solo si está validado)
        if (mantenimiento.isValidadoPorCliente() && mantenimiento.getCalificacionCliente() > 0) {
            holder.layoutCalificacion.setVisibility(View.VISIBLE);
            holder.tvCalificacion.setText(String.format("%.1f", (double) mantenimiento.getCalificacionCliente()));

            if (mantenimiento.getComentarioCliente() != null && !mantenimiento.getComentarioCliente().isEmpty()) {
                holder.tvComentarioCliente.setText(mantenimiento.getComentarioCliente());
            } else {
                holder.tvComentarioCliente.setText("Sin comentario");
            }
        } else {
            holder.layoutCalificacion.setVisibility(View.GONE);
        }

        // Click en "Ver Detalles"
        holder.btnVerDetalles.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetalleMantenimientoActivity.class);
            intent.putExtra("mantenimientoId", mantenimiento.getMantenimientoId());
            context.startActivity(intent);
        });

        // Click en toda la card
        holder.cardMantenimiento.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetalleMantenimientoActivity.class);
            intent.putExtra("mantenimientoId", mantenimiento.getMantenimientoId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return listaMantenimientos.size();
    }

    /**
     * Configura el icono y color del estado del mantenimiento
     */
    private void configurarEstado(ViewHolder holder, String estado) {
        if (estado == null) estado = "programado";

        int colorFondo;
        int iconoResId;

        switch (estado.toLowerCase()) {
            case "completado":
                holder.chipEstado.setText("Completado");
                colorFondo = R.color.success;
                iconoResId = R.drawable.ic_check;
                holder.chipEstado.setChipBackgroundColorResource(R.color.success);
                break;

            case "en_proceso":
                holder.chipEstado.setText("En Proceso");
                colorFondo = R.color.warning;
                iconoResId = R.drawable.ic_build;
                holder.chipEstado.setChipBackgroundColorResource(R.color.warning);
                break;

            case "cancelado":
                holder.chipEstado.setText("Cancelado");
                colorFondo = R.color.error;
                iconoResId = R.drawable.ic_close;
                holder.chipEstado.setChipBackgroundColorResource(R.color.error);
                break;

            case "programado":
            default:
                holder.chipEstado.setText("Programado");
                colorFondo = R.color.primary;
                iconoResId = R.drawable.ic_calendar;
                holder.chipEstado.setChipBackgroundColorResource(R.color.primary);
                break;
        }

        // Aplicar color de fondo al card del icono
        holder.cardIcono.setCardBackgroundColor(context.getResources().getColor(colorFondo, null));

        // Aplicar icono
        holder.ivIconoEstado.setImageResource(iconoResId);
    }

    /**
     * Carga el nombre del técnico desde Firestore
     */
    private void cargarTecnico(TextView tvTecnico, String tecnicoId) {
        if (tecnicoId == null || tecnicoId.isEmpty()) {
            tvTecnico.setText("Sin técnico asignado");
            return;
        }

        db.collection("usuarios").document(tecnicoId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String nombre = doc.getString("nombre");
                        tvTecnico.setText(nombre != null ? nombre : "Técnico desconocido");
                    } else {
                        tvTecnico.setText("Técnico no encontrado");
                    }
                })
                .addOnFailureListener(e -> {
                    tvTecnico.setText("Error al cargar técnico");
                });
    }

    /**
     * Formatea el tipo de mantenimiento para mostrar
     */
    private String obtenerTipoFormateado(String tipo) {
        if (tipo == null) return "Mantenimiento General";

        switch (tipo.toLowerCase()) {
            case "preventivo":
                return "Mantenimiento Preventivo";
            case "correctivo":
                return "Mantenimiento Correctivo";
            case "emergencia":
                return "Mantenimiento de Emergencia";
            default:
                return "Mantenimiento " + tipo;
        }
    }

    /**
     * Actualiza la lista de mantenimientos y refresca el RecyclerView
     */
    public void actualizarLista(List<Mantenimiento> nuevaLista) {
        this.listaMantenimientos = nuevaLista;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder para el timeline
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        // Timeline
        View lineaTop, lineaBottom;
        MaterialCardView cardIcono;
        ImageView ivIconoEstado;

        // Card de contenido
        MaterialCardView cardMantenimiento;
        Chip chipEstado;
        TextView tvFecha;
        TextView tvTipo;
        TextView tvTecnico;
        TextView tvObservaciones;
        LinearLayout layoutCalificacion;
        TextView tvCalificacion;
        TextView tvComentarioCliente;
        MaterialButton btnVerDetalles;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // Timeline
            lineaTop = itemView.findViewById(R.id.lineaTop);
            lineaBottom = itemView.findViewById(R.id.lineaBottom);
            cardIcono = itemView.findViewById(R.id.cardIcono);
            ivIconoEstado = itemView.findViewById(R.id.ivIconoEstado);

            // Card de contenido
            cardMantenimiento = itemView.findViewById(R.id.cardMantenimiento);
            chipEstado = itemView.findViewById(R.id.chipEstado);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvTipo = itemView.findViewById(R.id.tvTipo);
            tvTecnico = itemView.findViewById(R.id.tvTecnico);
            tvObservaciones = itemView.findViewById(R.id.tvObservaciones);
            layoutCalificacion = itemView.findViewById(R.id.layoutCalificacion);
            tvCalificacion = itemView.findViewById(R.id.tvCalificacion);
            tvComentarioCliente = itemView.findViewById(R.id.tvComentarioCliente);
            btnVerDetalles = itemView.findViewById(R.id.btnVerDetalles);
        }
    }
}
