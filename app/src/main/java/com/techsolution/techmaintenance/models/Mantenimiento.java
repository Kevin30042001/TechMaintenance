package com.techsolution.techmaintenance.models;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mantenimiento implements Serializable {
    private String mantenimientoId;
    private String clienteId;
    private String equipoId;
    private String tecnicoPrincipalId;
    private List<String> tecnicosApoyo;
    private String tipo; // preventivo/correctivo/emergencia
    private String descripcionServicio;
    private Timestamp fechaProgramada;
    private String horaProgramada;
    private Timestamp fechaInicio;
    private Timestamp fechaFinalizacion;
    private String estado; // programado/en_proceso/completado/cancelado
    private String prioridad; // baja/media/alta/urgente
    private String observacionesTecnico;
    private List<String> evidenciasFotograficas;
    private String codigoValidacion;
    private Timestamp codigoGeneradoEn;
    private Timestamp codigoExpiraEn;
    private boolean validadoPorCliente;
    private int calificacionCliente;
    private String comentarioCliente;
    private Timestamp fechaValidacion;
    private String duracionServicio;
    private String linkValidacion;
    private List<Timestamp> historialReenvios;

    // Constructores
    public Mantenimiento() {
        this.tecnicosApoyo = new ArrayList<>();
        this.evidenciasFotograficas = new ArrayList<>();
        this.historialReenvios = new ArrayList<>();
        this.validadoPorCliente = false;
        this.calificacionCliente = 0;
    }

    public Mantenimiento(String clienteId, String equipoId, String tecnicoPrincipalId,
                         String tipo, String descripcionServicio, Timestamp fechaProgramada,
                         String horaProgramada, String prioridad) {
        this.clienteId = clienteId;
        this.equipoId = equipoId;
        this.tecnicoPrincipalId = tecnicoPrincipalId;
        this.tipo = tipo;
        this.descripcionServicio = descripcionServicio;
        this.fechaProgramada = fechaProgramada;
        this.horaProgramada = horaProgramada;
        this.prioridad = prioridad;
        this.estado = "programado";
        this.tecnicosApoyo = new ArrayList<>();
        this.evidenciasFotograficas = new ArrayList<>();
        this.validadoPorCliente = false;
        this.calificacionCliente = 0;
    }

    // Getters y Setters
    public String getMantenimientoId() { return mantenimientoId; }
    public void setMantenimientoId(String mantenimientoId) { this.mantenimientoId = mantenimientoId; }

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public String getEquipoId() { return equipoId; }
    public void setEquipoId(String equipoId) { this.equipoId = equipoId; }

    public String getTecnicoPrincipalId() { return tecnicoPrincipalId; }
    public void setTecnicoPrincipalId(String tecnicoPrincipalId) {
        this.tecnicoPrincipalId = tecnicoPrincipalId;
    }

    public List<String> getTecnicosApoyo() { return tecnicosApoyo; }
    public void setTecnicosApoyo(List<String> tecnicosApoyo) { this.tecnicosApoyo = tecnicosApoyo; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDescripcionServicio() { return descripcionServicio; }
    public void setDescripcionServicio(String descripcionServicio) {
        this.descripcionServicio = descripcionServicio;
    }

    public Timestamp getFechaProgramada() { return fechaProgramada; }
    public void setFechaProgramada(Timestamp fechaProgramada) {
        this.fechaProgramada = fechaProgramada;
    }

    public String getHoraProgramada() { return horaProgramada; }
    public void setHoraProgramada(String horaProgramada) { this.horaProgramada = horaProgramada; }

    public Timestamp getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(Timestamp fechaInicio) { this.fechaInicio = fechaInicio; }

    public Timestamp getFechaFinalizacion() { return fechaFinalizacion; }
    public void setFechaFinalizacion(Timestamp fechaFinalizacion) {
        this.fechaFinalizacion = fechaFinalizacion;
    }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public String getObservacionesTecnico() { return observacionesTecnico; }
    public void setObservacionesTecnico(String observacionesTecnico) {
        this.observacionesTecnico = observacionesTecnico;
    }

    public List<String> getEvidenciasFotograficas() { return evidenciasFotograficas; }
    public void setEvidenciasFotograficas(List<String> evidenciasFotograficas) {
        this.evidenciasFotograficas = evidenciasFotograficas;
    }

    public String getCodigoValidacion() { return codigoValidacion; }
    public void setCodigoValidacion(String codigoValidacion) {
        this.codigoValidacion = codigoValidacion;
    }

    public Timestamp getCodigoGeneradoEn() { return codigoGeneradoEn; }
    public void setCodigoGeneradoEn(Timestamp codigoGeneradoEn) {
        this.codigoGeneradoEn = codigoGeneradoEn;
    }

    public Timestamp getCodigoExpiraEn() { return codigoExpiraEn; }
    public void setCodigoExpiraEn(Timestamp codigoExpiraEn) {
        this.codigoExpiraEn = codigoExpiraEn;
    }

    public boolean isValidadoPorCliente() { return validadoPorCliente; }
    public void setValidadoPorCliente(boolean validadoPorCliente) {
        this.validadoPorCliente = validadoPorCliente;
    }

    public int getCalificacionCliente() { return calificacionCliente; }
    public void setCalificacionCliente(int calificacionCliente) {
        this.calificacionCliente = calificacionCliente;
    }

    public String getComentarioCliente() { return comentarioCliente; }
    public void setComentarioCliente(String comentarioCliente) {
        this.comentarioCliente = comentarioCliente;
    }

    public Timestamp getFechaValidacion() { return fechaValidacion; }
    public void setFechaValidacion(Timestamp fechaValidacion) {
        this.fechaValidacion = fechaValidacion;
    }

    public String getDuracionServicio() { return duracionServicio; }
    public void setDuracionServicio(String duracionServicio) {
        this.duracionServicio = duracionServicio;
    }

    public String getLinkValidacion() { return linkValidacion; }
    public void setLinkValidacion(String linkValidacion) {
        this.linkValidacion = linkValidacion;
    }

    public List<Timestamp> getHistorialReenvios() { return historialReenvios; }
    public void setHistorialReenvios(List<Timestamp> historialReenvios) {
        this.historialReenvios = historialReenvios;
    }

    // Métodos auxiliares
    public boolean isProgramado() {
        return "programado".equals(estado);
    }

    public boolean isEnProceso() {
        return "en_proceso".equals(estado);
    }

    public boolean isCompletado() {
        return "completado".equals(estado);
    }

    public boolean isCancelado() {
        return "cancelado".equals(estado);
    }

    // Convertir a Map para Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("mantenimientoId", mantenimientoId);
        map.put("clienteId", clienteId);
        map.put("equipoId", equipoId);
        map.put("tecnicoPrincipalId", tecnicoPrincipalId);
        map.put("tecnicosApoyo", tecnicosApoyo);
        map.put("tipo", tipo);
        map.put("descripcionServicio", descripcionServicio);
        map.put("fechaProgramada", fechaProgramada);
        map.put("horaProgramada", horaProgramada);
        map.put("fechaInicio", fechaInicio);
        map.put("fechaFinalizacion", fechaFinalizacion);
        map.put("estado", estado);
        map.put("prioridad", prioridad);
        map.put("observacionesTecnico", observacionesTecnico);
        map.put("evidenciasFotograficas", evidenciasFotograficas);
        map.put("codigoValidacion", codigoValidacion);
        map.put("codigoGeneradoEn", codigoGeneradoEn);
        map.put("codigoExpiraEn", codigoExpiraEn);
        map.put("validadoPorCliente", validadoPorCliente);
        map.put("calificacionCliente", calificacionCliente);
        map.put("comentarioCliente", comentarioCliente);
        map.put("fechaValidacion", fechaValidacion);
        map.put("duracionServicio", duracionServicio);
        map.put("linkValidacion", linkValidacion);
        map.put("historialReenvios", historialReenvios);
        return map;
    }
}