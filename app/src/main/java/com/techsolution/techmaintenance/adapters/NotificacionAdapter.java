package com.techsolution.techmaintenance.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.models.Notificacion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NotificacionAdapter extends RecyclerView.Adapter<NotificacionAdapter.NotificacionViewHolder> {

    private Context context;
    private List<Notificacion> listaNotificaciones;
    private OnNotificacionClickListener listener;

    // Interface para manejar clicks
    public interface OnNotificacionClickListener {
        void onNotificacionClick(Notificacion notificacion);
        void onEliminarClick(Notificacion notificacion);
    }

    public NotificacionAdapter(Context context, List<Notificacion> listaNotificaciones, OnNotificacionClickListener listener) {
        this.context = context;
        this.listaNotificaciones = new ArrayList<>();
        this.listener = listener;

        // Copiar datos iniciales
        if (listaNotificaciones != null) {
            this.listaNotificaciones.addAll(listaNotificaciones);
        }
    }

    @NonNull
    @Override
    public NotificacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notificacion, parent, false);
        return new NotificacionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificacionViewHolder holder, int position) {
        Notificacion notificacion = listaNotificaciones.get(position);

        // Título
        holder.tvTitulo.setText(notificacion.getTitulo());

        // Mensaje
        holder.tvMensaje.setText(notificacion.getMensaje());

        // Tiempo transcurrido
        holder.tvTiempo.setText(getTimeAgo(notificacion.getFechaCreacion()));

        // Configurar icono y color según tipo
        configurarIconoYColor(holder, notificacion.getTipo());

        // Indicador de leída/no leída
        if (notificacion.isLeida()) {
            holder.indicadorNoLeida.setVisibility(View.GONE);
            holder.tvTitulo.setTypeface(null, Typeface.NORMAL);
            holder.tvMensaje.setAlpha(0.7f);
        } else {
            holder.indicadorNoLeida.setVisibility(View.VISIBLE);
            holder.tvTitulo.setTypeface(null, Typeface.BOLD);
            holder.tvMensaje.setAlpha(1.0f);
        }

        // Click en la notificación
        holder.cardNotificacion.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificacionClick(notificacion);
            }
        });

        // Long click para eliminar
        holder.cardNotificacion.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onEliminarClick(notificacion);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return listaNotificaciones.size();
    }

    /**
     * Configura el icono y color según el tipo de notificación
     */
    private void configurarIconoYColor(NotificacionViewHolder holder, String tipo) {
        int iconResId;
        int colorResId;

        switch (tipo) {
            case "asignacion":
                iconResId = R.drawable.ic_build;
                colorResId = R.color.primary;
                break;
            case "completado":
                iconResId = R.drawable.ic_check_circle;
                colorResId = R.color.success;
                break;
            case "urgente":
                iconResId = R.drawable.ic_warning;
                colorResId = R.color.error;
                break;
            case "validacion":
                iconResId = R.drawable.ic_star;
                colorResId = R.color.warning;
                break;
            case "general":
            default:
                iconResId = R.drawable.ic_notification;
                colorResId = R.color.info;
                break;
        }

        holder.ivIcono.setImageResource(iconResId);
        holder.cardIcono.setCardBackgroundColor(ContextCompat.getColor(context, colorResId));
    }

    /**
     * Calcula el tiempo transcurrido desde la creación de la notificación
     */
    private String getTimeAgo(Timestamp timestamp) {
        if (timestamp == null) {
            return "Ahora";
        }

        Date fechaCreacion = timestamp.toDate();
        Date ahora = new Date();

        long diff = ahora.getTime() - fechaCreacion.getTime();
        long segundos = diff / 1000;
        long minutos = segundos / 60;
        long horas = minutos / 60;
        long dias = horas / 24;

        if (segundos < 60) {
            return "Ahora";
        } else if (minutos < 60) {
            return "Hace " + minutos + (minutos == 1 ? " minuto" : " minutos");
        } else if (horas < 24) {
            return "Hace " + horas + (horas == 1 ? " hora" : " horas");
        } else if (dias < 7) {
            return "Hace " + dias + (dias == 1 ? " día" : " días");
        } else {
            return "Hace más de una semana";
        }
    }

    /**
     * Actualizar lista completa de notificaciones
     */
    public void actualizarLista(List<Notificacion> nuevaLista) {
        this.listaNotificaciones.clear();

        if (nuevaLista != null && !nuevaLista.isEmpty()) {
            this.listaNotificaciones.addAll(nuevaLista);
        }

        notifyDataSetChanged();
    }

    /**
     * Eliminar una notificación específica de la lista
     */
    public void eliminarNotificacion(Notificacion notificacion) {
        int posicion = -1;
        for (int i = 0; i < listaNotificaciones.size(); i++) {
            if (listaNotificaciones.get(i).getNotificacionId().equals(notificacion.getNotificacionId())) {
                posicion = i;
                break;
            }
        }

        if (posicion != -1) {
            listaNotificaciones.remove(posicion);
            notifyItemRemoved(posicion);
            notifyItemRangeChanged(posicion, listaNotificaciones.size());
        }
    }

    // ViewHolder
    static class NotificacionViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardNotificacion;
        MaterialCardView cardIcono;
        ImageView ivIcono;
        TextView tvTitulo;
        TextView tvMensaje;
        TextView tvTiempo;
        View indicadorNoLeida;

        public NotificacionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardNotificacion = itemView.findViewById(R.id.cardNotificacion);
            cardIcono = itemView.findViewById(R.id.cardIcono);
            ivIcono = itemView.findViewById(R.id.ivIcono);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvMensaje = itemView.findViewById(R.id.tvMensaje);
            tvTiempo = itemView.findViewById(R.id.tvTiempo);
            indicadorNoLeida = itemView.findViewById(R.id.indicadorNoLeida);
        }
    }
}
