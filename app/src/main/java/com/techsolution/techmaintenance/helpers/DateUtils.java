package com.techsolution.techmaintenance.helpers;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class DateUtils {

    // Formato: 25/11/2025 - 10:00 AM
    public static String formatearFechaHora(Timestamp timestamp) {
        if (timestamp == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy - hh:mm a", new Locale("es", "ES"));
        return sdf.format(timestamp.toDate());
    }

    // Formato: 25/11/2025
    public static String formatearFecha(Timestamp timestamp) {
        if (timestamp == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("es", "ES"));
        return sdf.format(timestamp.toDate());
    }

    // Formato: Lunes, 25 de Noviembre 2025
    public static String formatearFechaCompleta(Timestamp timestamp) {
        if (timestamp == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd 'de' MMMM yyyy", new Locale("es", "ES"));
        String fecha = sdf.format(timestamp.toDate());
        // Capitalizar primera letra
        return fecha.substring(0, 1).toUpperCase() + fecha.substring(1);
    }

    // Formato: 10:00 AM
    public static String formatearHora(Timestamp timestamp) {
        if (timestamp == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", new Locale("es", "ES"));
        return sdf.format(timestamp.toDate());
    }

    // Formato: Mañana, Hoy, Ayer, o fecha
    public static String formatearFechaRelativa(Timestamp timestamp) {
        if (timestamp == null) return "";

        long timestampMillis = timestamp.toDate().getTime();
        long ahora = System.currentTimeMillis();

        long diferencia = timestampMillis - ahora;
        long unDia = 24 * 60 * 60 * 1000;

        if (Math.abs(diferencia) < unDia / 2) {
            return "Hoy";
        } else if (diferencia > 0 && diferencia < unDia * 1.5) {
            return "Mañana";
        } else if (diferencia < 0 && Math.abs(diferencia) < unDia * 1.5) {
            return "Ayer";
        } else {
            return formatearFecha(timestamp);
        }
    }

    // Obtener texto del día de la semana
    public static String obtenerDiaSemana(Timestamp timestamp) {
        if (timestamp == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", new Locale("es", "ES"));
        String dia = sdf.format(timestamp.toDate());
        return dia.substring(0, 1).toUpperCase() + dia.substring(1);
    }
}