package com.techsolution.techmaintenance.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

/**
 * ✅ SOLUCIÓN: Usar Parcelable en lugar de Serializable
 * Esto resuelve el problema con Firebase Timestamp que no es Serializable
 */
public class Cliente implements Parcelable {
    private String clienteId;
    private String nombreEmpresa;
    private String nombreContacto;
    private String cargoContacto;
    private String emailContacto;
    private String telefonoContacto;
    private String telefonoAlternativo;
    private String direccion;
    private String referenciaDireccion;
    private Timestamp fechaRegistro;
    private String registradoPor;
    private int totalEquipos;
    private Timestamp ultimaModificacion;

    // Constructor vacío requerido por Firestore
    public Cliente() {
        this.totalEquipos = 0;
    }

    public Cliente(String nombreEmpresa, String nombreContacto, String emailContacto,
                   String telefonoContacto, String direccion, String registradoPor) {
        this.nombreEmpresa = nombreEmpresa;
        this.nombreContacto = nombreContacto;
        this.emailContacto = emailContacto;
        this.telefonoContacto = telefonoContacto;
        this.direccion = direccion;
        this.registradoPor = registradoPor;
        this.fechaRegistro = Timestamp.now();
        this.ultimaModificacion = Timestamp.now();
        this.totalEquipos = 0;
    }

    // ══════════════════════════════════════════════════════════════
    // IMPLEMENTACIÓN DE PARCELABLE
    // ══════════════════════════════════════════════════════════════

    protected Cliente(Parcel in) {
        clienteId = in.readString();
        nombreEmpresa = in.readString();
        nombreContacto = in.readString();
        cargoContacto = in.readString();
        emailContacto = in.readString();
        telefonoContacto = in.readString();
        telefonoAlternativo = in.readString();
        direccion = in.readString();
        referenciaDireccion = in.readString();
        registradoPor = in.readString();
        totalEquipos = in.readInt();

        // Leer Timestamps como segundos y nanosegundos
        long fechaRegistroSeconds = in.readLong();
        int fechaRegistroNanos = in.readInt();
        if (fechaRegistroSeconds != -1) {
            fechaRegistro = new Timestamp(fechaRegistroSeconds, fechaRegistroNanos);
        }

        long ultimaModificacionSeconds = in.readLong();
        int ultimaModificacionNanos = in.readInt();
        if (ultimaModificacionSeconds != -1) {
            ultimaModificacion = new Timestamp(ultimaModificacionSeconds, ultimaModificacionNanos);
        }
    }

    public static final Creator<Cliente> CREATOR = new Creator<Cliente>() {
        @Override
        public Cliente createFromParcel(Parcel in) {
            return new Cliente(in);
        }

        @Override
        public Cliente[] newArray(int size) {
            return new Cliente[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(clienteId);
        dest.writeString(nombreEmpresa);
        dest.writeString(nombreContacto);
        dest.writeString(cargoContacto);
        dest.writeString(emailContacto);
        dest.writeString(telefonoContacto);
        dest.writeString(telefonoAlternativo);
        dest.writeString(direccion);
        dest.writeString(referenciaDireccion);
        dest.writeString(registradoPor);
        dest.writeInt(totalEquipos);

        // Escribir Timestamps como segundos y nanosegundos
        if (fechaRegistro != null) {
            dest.writeLong(fechaRegistro.getSeconds());
            dest.writeInt(fechaRegistro.getNanoseconds());
        } else {
            dest.writeLong(-1);
            dest.writeInt(0);
        }

        if (ultimaModificacion != null) {
            dest.writeLong(ultimaModificacion.getSeconds());
            dest.writeInt(ultimaModificacion.getNanoseconds());
        } else {
            dest.writeLong(-1);
            dest.writeInt(0);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // GETTERS Y SETTERS
    // ══════════════════════════════════════════════════════════════

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public String getNombreEmpresa() { return nombreEmpresa; }
    public void setNombreEmpresa(String nombreEmpresa) { this.nombreEmpresa = nombreEmpresa; }

    public String getNombreContacto() { return nombreContacto; }
    public void setNombreContacto(String nombreContacto) { this.nombreContacto = nombreContacto; }

    public String getCargoContacto() { return cargoContacto; }
    public void setCargoContacto(String cargoContacto) { this.cargoContacto = cargoContacto; }

    public String getEmailContacto() { return emailContacto; }
    public void setEmailContacto(String emailContacto) { this.emailContacto = emailContacto; }

    public String getTelefonoContacto() { return telefonoContacto; }
    public void setTelefonoContacto(String telefonoContacto) { this.telefonoContacto = telefonoContacto; }

    public String getTelefonoAlternativo() { return telefonoAlternativo; }
    public void setTelefonoAlternativo(String telefonoAlternativo) {
        this.telefonoAlternativo = telefonoAlternativo;
    }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getReferenciaDireccion() { return referenciaDireccion; }
    public void setReferenciaDireccion(String referenciaDireccion) {
        this.referenciaDireccion = referenciaDireccion;
    }

    public Timestamp getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Timestamp fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }

    public int getTotalEquipos() { return totalEquipos; }
    public void setTotalEquipos(int totalEquipos) { this.totalEquipos = totalEquipos; }

    public Timestamp getUltimaModificacion() { return ultimaModificacion; }
    public void setUltimaModificacion(Timestamp ultimaModificacion) {
        this.ultimaModificacion = ultimaModificacion;
    }

    // ══════════════════════════════════════════════════════════════
    // MÉTODOS PARA FIREBASE
    // ══════════════════════════════════════════════════════════════

    /**
     * Convertir a Map para guardar en Firestore
     * ⭐ IMPORTANTE: Incluye clienteId
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        // ⭐ Incluir clienteId
        if (clienteId != null && !clienteId.isEmpty()) {
            map.put("clienteId", clienteId);
        }

        map.put("nombreEmpresa", nombreEmpresa);
        map.put("nombreContacto", nombreContacto);
        map.put("cargoContacto", cargoContacto);
        map.put("emailContacto", emailContacto);
        map.put("telefonoContacto", telefonoContacto);
        map.put("telefonoAlternativo", telefonoAlternativo);
        map.put("direccion", direccion);
        map.put("referenciaDireccion", referenciaDireccion);
        map.put("fechaRegistro", fechaRegistro);
        map.put("registradoPor", registradoPor);
        map.put("totalEquipos", totalEquipos);
        map.put("ultimaModificacion", ultimaModificacion);

        return map;
    }

    /**
     * Método para debugging
     */
    @Override
    public String toString() {
        return "Cliente{" +
                "clienteId='" + clienteId + '\'' +
                ", nombreEmpresa='" + nombreEmpresa + '\'' +
                ", nombreContacto='" + nombreContacto + '\'' +
                ", emailContacto='" + emailContacto + '\'' +
                ", totalEquipos=" + totalEquipos +
                '}';
    }
}