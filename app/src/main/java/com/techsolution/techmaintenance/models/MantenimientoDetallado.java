package com.techsolution.techmaintenance.models;

/**
 * Modelo que contiene un Mantenimiento con sus datos relacionados precargados.
 * Esto evita consultas N+1 en el adapter y mejora el rendimiento.
 */
public class MantenimientoDetallado {
    private Mantenimiento mantenimiento;

    // Datos del equipo
    private String equipoMarca;
    private String equipoModelo;

    // Datos del cliente
    private String clienteNombreEmpresa;
    private String clienteDireccion;
    private String clienteEmail;

    // Datos del técnico principal
    private String tecnicoNombre;

    public MantenimientoDetallado(Mantenimiento mantenimiento) {
        this.mantenimiento = mantenimiento;
    }

    // Getters y Setters
    public Mantenimiento getMantenimiento() {
        return mantenimiento;
    }

    public void setMantenimiento(Mantenimiento mantenimiento) {
        this.mantenimiento = mantenimiento;
    }

    public String getEquipoMarca() {
        return equipoMarca;
    }

    public void setEquipoMarca(String equipoMarca) {
        this.equipoMarca = equipoMarca;
    }

    public String getEquipoModelo() {
        return equipoModelo;
    }

    public void setEquipoModelo(String equipoModelo) {
        this.equipoModelo = equipoModelo;
    }

    public String getClienteNombreEmpresa() {
        return clienteNombreEmpresa;
    }

    public void setClienteNombreEmpresa(String clienteNombreEmpresa) {
        this.clienteNombreEmpresa = clienteNombreEmpresa;
    }

    public String getClienteDireccion() {
        return clienteDireccion;
    }

    public void setClienteDireccion(String clienteDireccion) {
        this.clienteDireccion = clienteDireccion;
    }

    public String getClienteEmail() {
        return clienteEmail;
    }

    public void setClienteEmail(String clienteEmail) {
        this.clienteEmail = clienteEmail;
    }

    public String getTecnicoNombre() {
        return tecnicoNombre;
    }

    public void setTecnicoNombre(String tecnicoNombre) {
        this.tecnicoNombre = tecnicoNombre;
    }

    // Método helper para obtener el nombre completo del equipo
    public String getEquipoNombreCompleto() {
        if (equipoMarca != null && equipoModelo != null) {
            return equipoMarca + " " + equipoModelo;
        } else if (equipoMarca != null) {
            return equipoMarca;
        } else if (equipoModelo != null) {
            return equipoModelo;
        }
        return "Equipo no disponible";
    }
}
