package com.techsolution.techmaintenance.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.helpers.DateUtils;
import com.techsolution.techmaintenance.models.Mantenimiento;
import com.techsolution.techmaintenance.models.MantenimientoDetallado;

import java.util.ArrayList;
import java.util.List;

public class MantenimientoAdapter extends RecyclerView.Adapter<MantenimientoAdapter.MantenimientoViewHolder> {

    private Context context;
    private List<MantenimientoDetallado> listaMantenimientos;
    private FirebaseFirestore db;
    private boolean esVistaAdmin;
    private String tecnicoActualId; // Para saber si es principal o apoyo

    // Constructor para Admin con MantenimientoDetallado (preferido - sin consultas N+1)
    public MantenimientoAdapter(Context context, List<MantenimientoDetallado> listaMantenimientos) {
        this.context = context;
        this.listaMantenimientos = listaMantenimientos;
        this.db = FirebaseFirestore.getInstance();
        this.esVistaAdmin = true;
        this.tecnicoActualId = null;
    }

    // Constructor para Técnico con MantenimientoDetallado (preferido - sin consultas N+1)
    public MantenimientoAdapter(Context context, List<MantenimientoDetallado> listaMantenimientos, String tecnicoActualId) {
        this.context = context;
        this.listaMantenimientos = listaMantenimientos;
        this.db = FirebaseFirestore.getInstance();
        this.esVistaAdmin = false;
        this.tecnicoActualId = tecnicoActualId;
    }

    // Constructor legacy para Admin con Mantenimiento (DEPRECATED - causa consultas N+1)
    // Convierte automáticamente List<Mantenimiento> a List<MantenimientoDetallado> sin datos precargados
    @Deprecated
    public static MantenimientoAdapter createLegacy(Context context, List<Mantenimiento> listaMantenimientosLegacy) {
        List<MantenimientoDetallado> listaDetallada = new ArrayList<>();
        for (Mantenimiento m : listaMantenimientosLegacy) {
            listaDetallada.add(new MantenimientoDetallado(m));
        }
        return new MantenimientoAdapter(context, listaDetallada);
    }

    // Constructor legacy para Técnico con Mantenimiento (DEPRECATED - causa consultas N+1)
    @Deprecated
    public static MantenimientoAdapter createLegacy(Context context, List<Mantenimiento> listaMantenimientosLegacy, String tecnicoActualId) {
        List<MantenimientoDetallado> listaDetallada = new ArrayList<>();
        for (Mantenimiento m : listaMantenimientosLegacy) {
            listaDetallada.add(new MantenimientoDetallado(m));
        }
        return new MantenimientoAdapter(context, listaDetallada, tecnicoActualId);
    }

    @NonNull
    @Override
    public MantenimientoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_mantenimiento_card, parent, false);
        return new MantenimientoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MantenimientoViewHolder holder, int position) {
        MantenimientoDetallado detallado = listaMantenimientos.get(position);
        Mantenimiento mantenimiento = detallado.getMantenimiento();

        // Formatear fecha y hora usando DateUtils
        String fechaHora = DateUtils.formatearFechaHora(mantenimiento.getFechaProgramada());
        holder.tvFechaHora.setText(fechaHora);

        // Tipo de mantenimiento
        holder.chipTipo.setText(capitalizarPrimeraLetra(mantenimiento.getTipo()));

        // Color del indicador según prioridad
        int colorPrioridad = obtenerColorPrioridad(mantenimiento.getPrioridad());
        holder.viewPrioridad.setBackgroundColor(colorPrioridad);
        holder.indicadorPrioridad.setBackgroundColor(colorPrioridad);

        // Usar datos precargados si están disponibles, sino cargar desde Firestore (legacy)
        if (detallado.getEquipoMarca() != null) {
            // Datos precargados disponibles
            holder.tvEquipo.setText(detallado.getEquipoNombreCompleto());
            holder.tvCliente.setText(detallado.getClienteNombreEmpresa() != null ?
                    detallado.getClienteNombreEmpresa() : "Cliente no disponible");
            holder.tvUbicacion.setText(detallado.getClienteDireccion() != null ?
                    detallado.getClienteDireccion() : "Ubicación no disponible");
            holder.tvTecnico.setText(detallado.getTecnicoNombre() != null ?
                    detallado.getTecnicoNombre() : "Técnico no disponible");
        } else {
            // Modo legacy: cargar datos desde Firestore (causa consultas N+1)
            // Establecer valores por defecto mientras se cargan
            holder.tvEquipo.setText("Cargando...");
            holder.tvCliente.setText("Cargando...");
            holder.tvUbicacion.setText("Cargando...");
            holder.tvTecnico.setText("Cargando...");

            cargarDatosEquipoLegacy(mantenimiento.getEquipoId(), holder);
            cargarDatosClienteLegacy(mantenimiento.getClienteId(), holder);
            cargarDatosTecnicoLegacy(mantenimiento.getTecnicoPrincipalId(), holder);
        }

        // Si es vista de técnico, mostrar badge de rol
        if (!esVistaAdmin && tecnicoActualId != null) {
            // Determinar si es técnico principal o apoyo
            if (mantenimiento.getTecnicoPrincipalId().equals(tecnicoActualId)) {
                // Es técnico principal
                agregarBadgeRol(holder, "Principal", "#2196F3");
            } else if (mantenimiento.getTecnicosApoyo() != null &&
                    mantenimiento.getTecnicosApoyo().contains(tecnicoActualId)) {
                // Es técnico de apoyo
                agregarBadgeRol(holder, "Apoyo", "#FF9800");
            }
        }

        // Click en ver detalles
        holder.btnVerDetalles.setOnClickListener(v -> {
            Intent intent = new Intent(context, com.techsolution.techmaintenance.activities.DetalleMantenimientoActivity.class);
            intent.putExtra("mantenimientoId", mantenimiento.getMantenimientoId());
            context.startActivity(intent);
        });

        // Mostrar botón "Reenviar Código" solo si:
        // 1. Estado = "completado"
        // 2. validadoPorCliente = false
        // 3. Es técnico principal (solo él puede reenviar)

        // Debug logging
        android.util.Log.d("MantenimientoAdapter", "=== Resend Button Check ===");
        android.util.Log.d("MantenimientoAdapter", "MantenimientoID: " + mantenimiento.getMantenimientoId());
        android.util.Log.d("MantenimientoAdapter", "Estado: " + mantenimiento.getEstado());
        android.util.Log.d("MantenimientoAdapter", "ValidadoPorCliente: " + mantenimiento.isValidadoPorCliente());
        android.util.Log.d("MantenimientoAdapter", "esVistaAdmin: " + esVistaAdmin);
        android.util.Log.d("MantenimientoAdapter", "tecnicoActualId: " + tecnicoActualId);
        android.util.Log.d("MantenimientoAdapter", "tecnicoPrincipalId: " + mantenimiento.getTecnicoPrincipalId());

        boolean mostrarBotonReenviar = "completado".equals(mantenimiento.getEstado()) &&
                !mantenimiento.isValidadoPorCliente() &&
                !esVistaAdmin &&
                tecnicoActualId != null &&
                mantenimiento.getTecnicoPrincipalId().equals(tecnicoActualId);

        android.util.Log.d("MantenimientoAdapter", "Mostrar boton: " + mostrarBotonReenviar);

        holder.btnReenviarCodigo.setVisibility(mostrarBotonReenviar ? View.VISIBLE : View.GONE);

        // Click en reenviar código
        holder.btnReenviarCodigo.setOnClickListener(v -> {
            mostrarDialogoReenviarCodigo(detallado);
        });
    }

    @Override
    public int getItemCount() {
        return listaMantenimientos.size();
    }

    // Método para actualizar datos en modo legacy
    public void updateData(List<Mantenimiento> nuevosMantenimientos) {
        listaMantenimientos.clear();
        for (Mantenimiento m : nuevosMantenimientos) {
            listaMantenimientos.add(new MantenimientoDetallado(m));
        }
        notifyDataSetChanged();
        android.util.Log.d("MantenimientoAdapter", "📋 updateData() llamado. Nuevos items: " + listaMantenimientos.size());
    }

    // Agregar badge visual para indicar rol (Principal/Apoyo)
    private void agregarBadgeRol(MantenimientoViewHolder holder, String rol, String colorHex) {
        holder.chipTipo.setText(holder.chipTipo.getText() + " • " + rol);
        // Opcional: Cambiar color del chip según rol
    }

    // Obtener color según prioridad
    private int obtenerColorPrioridad(String prioridad) {
        switch (prioridad.toLowerCase()) {
            case "urgente":
                return context.getResources().getColor(R.color.priority_urgent, null);
            case "alta":
                return context.getResources().getColor(R.color.priority_high, null);
            case "media":
                return context.getResources().getColor(R.color.priority_medium, null);
            case "baja":
            default:
                return context.getResources().getColor(R.color.priority_low, null);
        }
    }

    // Capitalizar primera letra
    private String capitalizarPrimeraLetra(String texto) {
        if (texto == null || texto.isEmpty()) return texto;
        return texto.substring(0, 1).toUpperCase() + texto.substring(1).toLowerCase();
    }

    // Mostrar diálogo de confirmación para reenviar código
    private void mostrarDialogoReenviarCodigo(MantenimientoDetallado detallado) {
        Mantenimiento mantenimiento = detallado.getMantenimiento();
        String emailCliente = detallado.getClienteEmail();
        String nombreEmpresa = detallado.getClienteNombreEmpresa();

        if (emailCliente != null && nombreEmpresa != null) {
            // Verificar límite de reenvíos
            db.collection("mantenimientos").document(mantenimiento.getMantenimientoId())
                    .get()
                    .addOnSuccessListener(doc -> {
                        List<Timestamp> historialReenvios = (List<Timestamp>) doc.get("historialReenvios");
                        int cantidadReenvios = historialReenvios != null ? historialReenvios.size() : 0;

                        if (cantidadReenvios >= 5) {
                            Toast.makeText(context,
                                    "Límite de reenvíos alcanzado (máximo 5)",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Mostrar diálogo de confirmación
                        new MaterialAlertDialogBuilder(context)
                                .setTitle("Reenviar Código de Validación")
                                .setMessage("Se reenviará el código de validación al email:\n\n" +
                                        emailCliente + "\n\n" +
                                        "Cliente: " + nombreEmpresa + "\n" +
                                        "Reenvíos anteriores: " + cantidadReenvios + "/5")
                                .setPositiveButton("Reenviar", (dialog, which) -> {
                                    reenviarCodigoValidacion(mantenimiento, emailCliente);
                                })
                                .setNegativeButton("Cancelar", null)
                                .show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context,
                                "Error al verificar historial de reenvíos",
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(context,
                    "No se pudo obtener el email del cliente",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Reenviar código de validación
    private void reenviarCodigoValidacion(Mantenimiento mantenimiento, String emailCliente) {
        // Primero registrar el reenvío en Firestore
        db.collection("mantenimientos").document(mantenimiento.getMantenimientoId())
                .update("historialReenvios", FieldValue.arrayUnion(Timestamp.now()))
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("MantenimientoAdapter", "✅ Reenvío registrado en Firestore");

                    // Mostrar opciones de reenvío (Email, WhatsApp, Copiar)
                    mostrarOpcionesReenvio(mantenimiento, emailCliente);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context,
                            "Error al registrar reenvío: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    android.util.Log.e("MantenimientoAdapter", "❌ Error al reenviar código", e);
                });
    }

    // Mostrar opciones de reenvío (Email, WhatsApp, Copiar)
    private void mostrarOpcionesReenvio(Mantenimiento mantenimiento, String emailCliente) {
        String codigo = mantenimiento.getCodigoValidacion();
        String linkValidacion = mantenimiento.getLinkValidacion();

        new MaterialAlertDialogBuilder(context)
                .setTitle("Reenviar Código de Validación")
                .setMessage("¿Cómo deseas enviar el código al cliente?")
                .setPositiveButton("📧 Email", (dialog, which) -> {
                    enviarReenvioPorEmail(mantenimiento, emailCliente, codigo, linkValidacion);
                })
                .setNeutralButton("💬 WhatsApp", (dialog, which) -> {
                    enviarReenvioPorWhatsApp(mantenimiento, codigo, linkValidacion);
                })
                .setNegativeButton("📋 Copiar", (dialog, which) -> {
                    copiarCodigoAlPortapapeles(codigo);
                    Toast.makeText(context,
                            "Código copiado. Compártelo manualmente con el cliente.",
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    // Enviar reenvío por Email
    private void enviarReenvioPorEmail(Mantenimiento mantenimiento, String emailCliente,
                                      String codigo, String linkValidacion) {
        // Cargar datos del cliente y equipo para personalizar el email
        db.collection("clientes").document(mantenimiento.getClienteId())
                .get()
                .addOnSuccessListener(clienteDoc -> {
                    String nombreCliente = clienteDoc.exists() ?
                            clienteDoc.getString("nombreContacto") : "Cliente";

                    db.collection("equipos").document(mantenimiento.getEquipoId())
                            .get()
                            .addOnSuccessListener(equipoDoc -> {
                                String equipoInfo = "";
                                if (equipoDoc.exists()) {
                                    String tipo = equipoDoc.getString("tipo");
                                    String marca = equipoDoc.getString("marca");
                                    String modelo = equipoDoc.getString("modelo");
                                    equipoInfo = (tipo != null ? tipo : "") + " " +
                                               (marca != null ? marca : "") + " " +
                                               (modelo != null ? modelo : "");
                                }
                                enviarEmailReenvio(nombreCliente, equipoInfo.trim(),
                                                 emailCliente, codigo, linkValidacion);
                            })
                            .addOnFailureListener(e -> {
                                enviarEmailReenvioSimple(emailCliente, codigo, linkValidacion);
                            });
                })
                .addOnFailureListener(e -> {
                    enviarEmailReenvioSimple(emailCliente, codigo, linkValidacion);
                });
    }

    // Enviar email de reenvío con datos completos
    private void enviarEmailReenvio(String nombreCliente, String equipoInfo,
                                   String emailCliente, String codigo, String linkValidacion) {
        android.content.Intent emailIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{emailCliente});
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                "🔄 Reenvío: Código de Validación - TechMaintenance");

        String mensaje = "Estimado/a " + nombreCliente + ",\n\n" +
                "Le reenviamos el código de validación para su servicio de mantenimiento.\n\n" +
                (equipoInfo.isEmpty() ? "" : "Equipo atendido: " + equipoInfo + "\n\n") +
                "═══════════════════════════════════════\n" +
                "    🌐 VALIDAR SERVICIO (CLICK AQUÍ)\n" +
                "═══════════════════════════════════════\n\n" +
                linkValidacion + "\n\n" +
                "═══════════════════════════════════════\n\n" +
                "⏰ Este enlace expira en 24 horas desde que se completó el servicio.\n\n" +
                "📱 CÓMO VALIDAR EL SERVICIO:\n\n" +
                "1. Haga click en el enlace de arriba (se abrirá en su navegador)\n" +
                "2. El código ya está incluido en el enlace\n" +
                "3. Revise el resumen del servicio\n" +
                "4. Califique el trabajo realizado (1-5 estrellas)\n" +
                "5. Opcionalmente agregue comentarios\n\n" +
                "✨ NO necesita instalar ninguna app\n" +
                "✨ Funciona desde cualquier navegador web\n" +
                "✨ Solo toma 1 minuto validar\n\n" +
                "Su opinión es muy importante para nosotros.\n\n" +
                "Código de validación: " + codigo + "\n" +
                "(Por si necesita ingresarlo manualmente)\n\n" +
                "Gracias por confiar en TechSolution.\n\n" +
                "---\n" +
                "TechMaintenance System\n" +
                "TechSolution © 2025";

        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, mensaje);

        try {
            context.startActivity(android.content.Intent.createChooser(emailIntent,
                    "Reenviar código de validación"));
            Toast.makeText(context,
                    "📧 Abre tu app de email para enviar el código",
                    Toast.LENGTH_LONG).show();
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(context,
                    "❌ No hay aplicación de email instalada",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Enviar email de reenvío simple (sin datos del equipo/cliente)
    private void enviarEmailReenvioSimple(String emailCliente, String codigo, String linkValidacion) {
        android.content.Intent emailIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{emailCliente});
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                "🔄 Reenvío: Código de Validación - TechMaintenance");

        String mensaje = "Estimado/a Cliente,\n\n" +
                "Le reenviamos el código de validación para su servicio de mantenimiento.\n\n" +
                "═══════════════════════════════════════\n" +
                "    🌐 VALIDAR SERVICIO (CLICK AQUÍ)\n" +
                "═══════════════════════════════════════\n\n" +
                linkValidacion + "\n\n" +
                "═══════════════════════════════════════\n\n" +
                "⏰ Este enlace expira en 24 horas desde que se completó el servicio.\n\n" +
                "Haga click en el enlace de arriba para validar el servicio.\n" +
                "NO necesita instalar ninguna app.\n\n" +
                "Código de validación: " + codigo + "\n\n" +
                "Gracias por confiar en TechSolution.\n\n" +
                "---\n" +
                "TechMaintenance System";

        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, mensaje);

        try {
            context.startActivity(android.content.Intent.createChooser(emailIntent,
                    "Reenviar código de validación"));
            Toast.makeText(context,
                    "📧 Abre tu app de email para enviar el código",
                    Toast.LENGTH_LONG).show();
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(context,
                    "❌ No hay aplicación de email instalada",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Enviar reenvío por WhatsApp
    private void enviarReenvioPorWhatsApp(Mantenimiento mantenimiento, String codigo, String linkValidacion) {
        // Cargar teléfono y nombre del cliente
        db.collection("clientes").document(mantenimiento.getClienteId())
                .get()
                .addOnSuccessListener(clienteDoc -> {
                    if (clienteDoc.exists()) {
                        String telefono = clienteDoc.getString("telefonoContacto");
                        String nombreCliente = clienteDoc.getString("nombreContacto");

                        if (telefono != null && !telefono.isEmpty()) {
                            enviarWhatsAppReenvio(telefono, nombreCliente, codigo, linkValidacion);
                        } else {
                            Toast.makeText(context,
                                    "⚠️ Teléfono del cliente no disponible",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context,
                            "Error al obtener datos del cliente",
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Enviar WhatsApp con datos del reenvío
    private void enviarWhatsAppReenvio(String telefono, String nombreCliente,
                                      String codigo, String linkValidacion) {
        // Limpiar teléfono (solo números)
        String telefonoLimpio = telefono.replaceAll("[^0-9]", "");

        // Si no tiene código de país, agregar 503 (El Salvador)
        if (!telefonoLimpio.startsWith("503") && telefonoLimpio.length() == 8) {
            telefonoLimpio = "503" + telefonoLimpio;
        }

        String mensaje = "Hola " + (nombreCliente != null ? nombreCliente : "Cliente") + ",%0A%0A" +
                "🔄 Le *reenviamos* el código de validación de su servicio de mantenimiento.%0A%0A" +
                "═══════════════════════════%0A" +
                "🌐 *VALIDAR SERVICIO*%0A" +
                "═══════════════════════════%0A%0A" +
                "Por favor haga click en este enlace:%0A%0A" +
                linkValidacion + "%0A%0A" +
                "═══════════════════════════%0A%0A" +
                "⏰ Este enlace expira en *24 horas* desde que se completó el servicio.%0A%0A" +
                "✨ *NO necesita instalar ninguna app*%0A" +
                "✨ Se abre en su navegador web%0A" +
                "✨ Solo toma 1 minuto%0A%0A" +
                "Por favor califique el servicio recibido.%0A%0A" +
                "Código: *" + codigo + "*%0A" +
                "(Por si necesita ingresarlo manualmente)%0A%0A" +
                "Gracias por confiar en *TechSolution*.%0A%0A" +
                "----%0A" +
                "_TechMaintenance System_";

        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse("https://api.whatsapp.com/send?phone=" +
                    telefonoLimpio + "&text=" + mensaje));
            context.startActivity(intent);
            Toast.makeText(context,
                    "💬 Abriendo WhatsApp...",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context,
                    "❌ WhatsApp no está instalado",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Copiar código al portapapeles
    private void copiarCodigoAlPortapapeles(String codigo) {
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) context.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Código de validación", codigo);
        clipboard.setPrimaryClip(clip);
    }

    // ==================== MÉTODOS LEGACY (DEPRECATED) ====================
    // Estos métodos se usan cuando los datos NO están precargados (causa consultas N+1)

    @Deprecated
    private void cargarDatosEquipoLegacy(String equipoId, MantenimientoViewHolder holder) {
        db.collection("equipos").document(equipoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String marca = documentSnapshot.getString("marca");
                        String modelo = documentSnapshot.getString("modelo");
                        holder.tvEquipo.setText(marca + " " + modelo);
                    }
                })
                .addOnFailureListener(e -> holder.tvEquipo.setText("Equipo no encontrado"));
    }

    @Deprecated
    private void cargarDatosClienteLegacy(String clienteId, MantenimientoViewHolder holder) {
        db.collection("clientes").document(clienteId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nombreEmpresa = documentSnapshot.getString("nombreEmpresa");
                        String direccion = documentSnapshot.getString("direccion");
                        holder.tvCliente.setText(nombreEmpresa);
                        holder.tvUbicacion.setText(direccion);
                    }
                })
                .addOnFailureListener(e -> holder.tvCliente.setText("Cliente no encontrado"));
    }

    @Deprecated
    private void cargarDatosTecnicoLegacy(String tecnicoId, MantenimientoViewHolder holder) {
        db.collection("usuarios").document(tecnicoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nombre = documentSnapshot.getString("nombre");
                        holder.tvTecnico.setText(nombre);
                    }
                })
                .addOnFailureListener(e -> holder.tvTecnico.setText("Técnico no encontrado"));
    }

    /**
     * Actualiza la lista de mantenimientos del adapter.
     * Convierte List<Mantenimiento> a List<MantenimientoDetallado> para compatibilidad con el adapter.
     */
    public void actualizarLista(List<Mantenimiento> nuevaLista) {
        listaMantenimientos.clear();
        for (Mantenimiento m : nuevaLista) {
            listaMantenimientos.add(new MantenimientoDetallado(m));
        }
        android.util.Log.d("MantenimientoAdapter", "🔄 Lista actualizada. Nuevos items: " + listaMantenimientos.size());
        notifyDataSetChanged();
    }

    // ViewHolder
    static class MantenimientoViewHolder extends RecyclerView.ViewHolder {
        TextView tvFechaHora, tvEquipo, tvCliente, tvUbicacion, tvTecnico;
        View viewPrioridad, indicadorPrioridad;
        Chip chipTipo;
        MaterialButton btnVerDetalles, btnReenviarCodigo;

        public MantenimientoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFechaHora = itemView.findViewById(R.id.tvFechaHora);
            tvEquipo = itemView.findViewById(R.id.tvEquipo);
            tvCliente = itemView.findViewById(R.id.tvCliente);
            tvUbicacion = itemView.findViewById(R.id.tvUbicacion);
            tvTecnico = itemView.findViewById(R.id.tvTecnico);
            viewPrioridad = itemView.findViewById(R.id.viewPrioridad);
            indicadorPrioridad = itemView.findViewById(R.id.indicadorPrioridad);
            chipTipo = itemView.findViewById(R.id.chipTipo);
            btnVerDetalles = itemView.findViewById(R.id.btnVerDetalles);
            btnReenviarCodigo = itemView.findViewById(R.id.btnReenviarCodigo);
        }
    }
}