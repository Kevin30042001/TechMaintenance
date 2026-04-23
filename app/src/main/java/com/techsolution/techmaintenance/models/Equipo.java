package com.techsolution.techmaintenance.models;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Equipo implements Serializable {
    private String equipoId;
    private String clienteId;
    private String tipo; // laptop/desktop/servidor/impresora/otro
    private String marca;
    private String modelo;
    private String numeroSerie;
    private String ubicacionEspecifica;
    private Timestamp fechaAdquisicion;
    private String estado; // operativo/mantenimiento/fuera_servicio
    private String fotografiaURL;
    private Timestamp fechaRegistro;
    private String registradoPor;
    private int totalMantenimientos;

    // Constructores
    public Equipo() {
        this.totalMantenimientos = 0;
    }

    public Equipo(String clienteId, String tipo, String marca, String modelo,
                  String numeroSerie, String ubicacionEspecifica, String estado, String registradoPor) {
        this.clienteId = clienteId;
        this.tipo = tipo;
        this.marca = marca;
        this.modelo = modelo;
        this.numeroSerie = numeroSerie;
        this.ubicacionEspecifica = ubicacionEspecifica;
        this.estado = estado;
        this.registradoPor = registradoPor;
        this.fechaRegistro = Timestamp.now();
        this.totalMantenimientos = 0;
    }

    // Getters y Setters
    public String getEquipoId() { return equipoId; }
    public void setEquipoId(String equipoId) { this.equipoId = equipoId; }

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getNumeroSerie() { return numeroSerie; }
    public void setNumeroSerie(String numeroSerie) { this.numeroSerie = numeroSerie; }

    public String getUbicacionEspecifica() { return ubicacionEspecifica; }
    public void setUbicacionEspecifica(String ubicacionEspecifica) {
        this.ubicacionEspecifica = ubicacionEspecifica;
    }

    public Timestamp getFechaAdquisicion() { return fechaAdquisicion; }
    public void setFechaAdquisicion(Timestamp fechaAdquisicion) {
        this.fechaAdquisicion = fechaAdquisicion;
    }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getFotografiaURL() { return fotografiaURL; }
    public void setFotografiaURL(String fotografiaURL) { this.fotografiaURL = fotografiaURL; }

    public Timestamp getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Timestamp fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }

    public int getTotalMantenimientos() { return totalMantenimientos; }
    public void setTotalMantenimientos(int totalMantenimientos) {
        this.totalMantenimientos = totalMantenimientos;
    }

    // Métodos auxiliares
    public boolean isOperativo() {
        return "operativo".equals(estado);
    }

    public boolean enMantenimiento() {
        return "mantenimiento".equals(estado);
    }

    public boolean fueraDeServicio() {
        return "fuera_servicio".equals(estado);
    }

    // Convertir a Map para Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("equipoId", equipoId);
        map.put("clienteId", clienteId);
        map.put("tipo", tipo);
        map.put("marca", marca);
        map.put("modelo", modelo);
        map.put("numeroSerie", numeroSerie);
        map.put("ubicacionEspecifica", ubicacionEspecifica);
        map.put("fechaAdquisicion", fechaAdquisicion);
        map.put("estado", estado);
        map.put("fotografiaURL", fotografiaURL);
        map.put("fechaRegistro", fechaRegistro);
        map.put("registradoPor", registradoPor);
        map.put("totalMantenimientos", totalMantenimientos);
        return map;
    }
}