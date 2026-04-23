package com.techsolution.techmaintenance.models;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Usuario implements Serializable {
    private String userId;
    private String nombre;
    private String email;
    private String rol; // "admin" o "tecnico"
    private String telefono;
    private String estado; // "activo" o "inactivo"
    private String fotoPerfilURL;
    private Timestamp fechaCreacion;
    private Estadisticas estadisticas;

    // Clase interna para estadísticas
    public static class Estadisticas implements Serializable {
        private int serviciosCompletados;
        private double calificacionPromedio;
        private double eficiencia;

        public Estadisticas() {
            this.serviciosCompletados = 0;
            this.calificacionPromedio = 0.0;
            this.eficiencia = 0.0;
        }

        public Estadisticas(int serviciosCompletados, double calificacionPromedio, double eficiencia) {
            this.serviciosCompletados = serviciosCompletados;
            this.calificacionPromedio = calificacionPromedio;
            this.eficiencia = eficiencia;
        }

        // Getters y Setters
        public int getServiciosCompletados() { return serviciosCompletados; }
        public void setServiciosCompletados(int serviciosCompletados) {
            this.serviciosCompletados = serviciosCompletados;
        }

        public double getCalificacionPromedio() { return calificacionPromedio; }
        public void setCalificacionPromedio(double calificacionPromedio) {
            this.calificacionPromedio = calificacionPromedio;
        }

        public double getEficiencia() { return eficiencia; }
        public void setEficiencia(double eficiencia) {
            this.eficiencia = eficiencia;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("serviciosCompletados", serviciosCompletados);
            map.put("calificacionPromedio", calificacionPromedio);
            map.put("eficiencia", eficiencia);
            return map;
        }
    }

    // Constructores
    public Usuario() {
        this.estadisticas = new Estadisticas();
    }

    public Usuario(String userId, String nombre, String email, String rol, String telefono, String estado) {
        this.userId = userId;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
        this.telefono = telefono;
        this.estado = estado;
        this.fechaCreacion = Timestamp.now();
        this.estadisticas = new Estadisticas();
    }

    // Getters y Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getFotoPerfilURL() { return fotoPerfilURL; }
    public void setFotoPerfilURL(String fotoPerfilURL) { this.fotoPerfilURL = fotoPerfilURL; }

    public Timestamp getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Timestamp fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Estadisticas getEstadisticas() { return estadisticas; }
    public void setEstadisticas(Estadisticas estadisticas) { this.estadisticas = estadisticas; }

    // Métodos auxiliares
    public boolean isAdmin() {
        return "admin".equals(rol);
    }

    public boolean isTecnico() {
        return "tecnico".equals(rol);
    }

    public boolean isActivo() {
        return "activo".equals(estado);
    }

    // Convertir a Map para Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("nombre", nombre);
        map.put("email", email);
        map.put("rol", rol);
        map.put("telefono", telefono);
        map.put("estado", estado);
        map.put("fotoPerfilURL", fotoPerfilURL);
        map.put("fechaCreacion", fechaCreacion);
        map.put("estadisticas", estadisticas.toMap());
        return map;
    }
}