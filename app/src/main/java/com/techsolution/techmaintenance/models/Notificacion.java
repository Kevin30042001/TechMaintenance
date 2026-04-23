package com.techsolution.techmaintenance.models;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * Modelo de Notificación para Firestore
 */
public class Notificacion {
    private String notificacionId;
    private String titulo;
    private String mensaje;
    private String tipo; // "asignacion", "completado", "urgente", "validacion", "general"
    private String usuarioId; // ID del destinatario
    private String mantenimientoId; // Referencia al mantenimiento (si aplica)
    private boolean leida;
    private Timestamp fechaCreacion;
    private Map<String, Object> datos; // Datos adicionales para navegación

    // Constructor vacío requerido por Firestore
    public Notificacion() {
        this.leida = false;
        this.fechaCreacion = Timestamp.now();
        this.datos = new HashMap<>();
    }

    public Notificacion(String titulo, String mensaje, String tipo, String usuarioId) {
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.usuarioId = usuarioId;
        this.leida = false;
        this.fechaCreacion = Timestamp.now();
        this.datos = new HashMap<>();
    }

    // Método para convertir a Map para Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("titulo", titulo);
        map.put("mensaje", mensaje);
        map.put("tipo", tipo);
        map.put("usuarioId", usuarioId);
        map.put("mantenimientoId", mantenimientoId);
        map.put("leida", leida);
        map.put("fechaCreacion", fechaCreacion);
        map.put("datos", datos);
        return map;
    }

    // Getters y Setters
    public String getNotificacionId() {
        return notificacionId;
    }

    public void setNotificacionId(String notificacionId) {
        this.notificacionId = notificacionId;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getMantenimientoId() {
        return mantenimientoId;
    }

    public void setMantenimientoId(String mantenimientoId) {
        this.mantenimientoId = mantenimientoId;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public Timestamp getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Timestamp fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Map<String, Object> getDatos() {
        return datos;
    }

    public void setDatos(Map<String, Object> datos) {
        this.datos = datos;
    }
}
