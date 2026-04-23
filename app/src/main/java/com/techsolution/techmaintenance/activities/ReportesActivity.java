package com.techsolution.techmaintenance.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.techsolution.techmaintenance.R;
import com.techsolution.techmaintenance.adapters.TecnicoEstadisticaAdapter;
import com.techsolution.techmaintenance.helpers.FirestoreHelper;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportesActivity extends AppCompatActivity {

    private static final String TAG = "ReportesActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    // Firebase
    private FirebaseFirestore db;

    // Vistas
    private Toolbar toolbar;
    private ChipGroup chipGroupPeriodo;
    private Chip chipHoy, chipEstaSemana, chipEsteMes, chipUltimosTresMeses, chipEsteAno, chipPersonalizado;

    // Filtros personalizados
    private LinearLayout layoutFechasPersonalizadas;
    private TextInputEditText etFechaInicio, etFechaFin;
    private MaterialButton btnAplicarFechas, btnLimpiarFiltros;
    private AutoCompleteTextView spinnerTecnico;

    // Métricas
    private TextView tvTotalMantenimientos, tvCompletados, tvEnProceso, tvCancelados;
    private TextView tvPorcentajeATiempo, tvCalificacionPromedio;

    // Distribución por tipo
    private TextView tvPreventivos, tvCorrectivos, tvEmergencias;

    // Top equipos
    private TextView tvTopEquipos;

    // Técnicos
    private RecyclerView recyclerTecnicos;
    private TextView tvSinTecnicos;
    private TecnicoEstadisticaAdapter tecnicoAdapter;
    private List<TecnicoEstadisticaAdapter.TecnicoEstadistica> listaTecnicos;

    // Botones
    private MaterialButton btnExportarPDF, btnExportarExcel;
    private ProgressBar progressBar;

    // Variables
    private Timestamp fechaInicio, fechaFin;
    private String periodoActual = "este_mes";
    private String tecnicoIdFiltro = null;  // null = todos los técnicos
    private String tecnicoNombreFiltro = "Todos los técnicos";
    private List<Map<String, String>> listaTecnicosDisponibles;  // {id, nombre}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();

        // Inicializar vistas
        inicializarVistas();

        // Configurar toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Configurar listeners
        configurarListeners();

        // Cargar datos iniciales (mes actual)
        establecerPeriodo("este_mes");
        cargarDatos();
    }

    private void inicializarVistas() {
        toolbar = findViewById(R.id.toolbar);
        chipGroupPeriodo = findViewById(R.id.chipGroupPeriodo);
        chipHoy = findViewById(R.id.chipHoy);
        chipEstaSemana = findViewById(R.id.chipEstaSemana);
        chipEsteMes = findViewById(R.id.chipEsteMes);
        chipUltimosTresMeses = findViewById(R.id.chipUltimosTresMeses);
        chipEsteAno = findViewById(R.id.chipEsteAno);
        chipPersonalizado = findViewById(R.id.chipPersonalizado);

        // Filtros personalizados
        layoutFechasPersonalizadas = findViewById(R.id.layoutFechasPersonalizadas);
        etFechaInicio = findViewById(R.id.etFechaInicio);
        etFechaFin = findViewById(R.id.etFechaFin);
        btnAplicarFechas = findViewById(R.id.btnAplicarFechas);
        btnLimpiarFiltros = findViewById(R.id.btnLimpiarFiltros);
        spinnerTecnico = findViewById(R.id.spinnerTecnico);

        // Métricas
        tvTotalMantenimientos = findViewById(R.id.tvTotalMantenimientos);
        tvCompletados = findViewById(R.id.tvCompletados);
        tvEnProceso = findViewById(R.id.tvEnProceso);
        tvCancelados = findViewById(R.id.tvCancelados);
        tvPorcentajeATiempo = findViewById(R.id.tvPorcentajeATiempo);
        tvCalificacionPromedio = findViewById(R.id.tvCalificacionPromedio);

        // Tipos
        tvPreventivos = findViewById(R.id.tvPreventivos);
        tvCorrectivos = findViewById(R.id.tvCorrectivos);
        tvEmergencias = findViewById(R.id.tvEmergencias);

        // Top equipos
        tvTopEquipos = findViewById(R.id.tvTopEquipos);

        // Técnicos
        recyclerTecnicos = findViewById(R.id.recyclerTecnicos);
        tvSinTecnicos = findViewById(R.id.tvSinTecnicos);
        listaTecnicos = new ArrayList<>();
        tecnicoAdapter = new TecnicoEstadisticaAdapter(this, listaTecnicos);
        recyclerTecnicos.setLayoutManager(new LinearLayoutManager(this));
        recyclerTecnicos.setAdapter(tecnicoAdapter);

        // Botones
        btnExportarPDF = findViewById(R.id.btnExportarPDF);
        btnExportarExcel = findViewById(R.id.btnExportarExcel);
        progressBar = findViewById(R.id.progressBar);

        // Inicializar lista de técnicos disponibles
        listaTecnicosDisponibles = new ArrayList<>();
        cargarTecnicosDisponibles();
    }

    private void configurarListeners() {
        chipGroupPeriodo.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipHoy)) {
                layoutFechasPersonalizadas.setVisibility(View.GONE);
                establecerPeriodo("hoy");
                cargarDatos();
            } else if (checkedIds.contains(R.id.chipEstaSemana)) {
                layoutFechasPersonalizadas.setVisibility(View.GONE);
                establecerPeriodo("esta_semana");
                cargarDatos();
            } else if (checkedIds.contains(R.id.chipEsteMes)) {
                layoutFechasPersonalizadas.setVisibility(View.GONE);
                establecerPeriodo("este_mes");
                cargarDatos();
            } else if (checkedIds.contains(R.id.chipUltimosTresMeses)) {
                layoutFechasPersonalizadas.setVisibility(View.GONE);
                establecerPeriodo("ultimos_3_meses");
                cargarDatos();
            } else if (checkedIds.contains(R.id.chipEsteAno)) {
                layoutFechasPersonalizadas.setVisibility(View.GONE);
                establecerPeriodo("este_ano");
                cargarDatos();
            } else if (checkedIds.contains(R.id.chipPersonalizado)) {
                layoutFechasPersonalizadas.setVisibility(View.VISIBLE);
                periodoActual = "personalizado";
            }
        });

        // DatePickers para fechas personalizadas
        etFechaInicio.setOnClickListener(v -> mostrarDatePicker(true));
        etFechaFin.setOnClickListener(v -> mostrarDatePicker(false));

        // Aplicar fechas personalizadas
        btnAplicarFechas.setOnClickListener(v -> {
            if (fechaInicio != null && fechaFin != null) {
                if (fechaInicio.toDate().after(fechaFin.toDate())) {
                    Toast.makeText(this, "La fecha de inicio no puede ser posterior a la fecha de fin", Toast.LENGTH_SHORT).show();
                    return;
                }
                cargarDatos();
                Toast.makeText(this, "Filtro de fechas aplicado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Selecciona ambas fechas", Toast.LENGTH_SHORT).show();
            }
        });

        // Filtro por técnico
        spinnerTecnico.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, String> tecnicoSeleccionado = listaTecnicosDisponibles.get(position);
            tecnicoIdFiltro = tecnicoSeleccionado.get("id");
            tecnicoNombreFiltro = tecnicoSeleccionado.get("nombre");
            cargarDatos();
            Toast.makeText(this, "Filtrando por: " + tecnicoNombreFiltro, Toast.LENGTH_SHORT).show();
        });

        // Limpiar filtros
        btnLimpiarFiltros.setOnClickListener(v -> {
            tecnicoIdFiltro = null;
            tecnicoNombreFiltro = "Todos los técnicos";
            spinnerTecnico.setText(tecnicoNombreFiltro, false);
            cargarDatos();
            Toast.makeText(this, "Filtros limpiados", Toast.LENGTH_SHORT).show();
        });

        btnExportarPDF.setOnClickListener(v -> verificarPermisosYExportarPDF());
        btnExportarExcel.setOnClickListener(v -> verificarPermisosYExportarExcel());
    }

    private void establecerPeriodo(String periodo) {
        periodoActual = periodo;
        Calendar cal = Calendar.getInstance();

        switch (periodo) {
            case "hoy":
                fechaInicio = FirestoreHelper.getInicioDiaActual();
                fechaFin = FirestoreHelper.getFinDiaActual();
                break;

            case "esta_semana":
                fechaInicio = FirestoreHelper.getInicioSemanaActual();
                fechaFin = FirestoreHelper.getFinSemanaActual();
                break;

            case "este_mes":
                fechaInicio = FirestoreHelper.getInicioMesActual();
                fechaFin = FirestoreHelper.getFinMesActual();
                break;

            case "ultimos_3_meses":
                cal.add(Calendar.MONTH, -3);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                fechaInicio = new Timestamp(cal.getTime());
                fechaFin = FirestoreHelper.getFinMesActual();
                break;

            case "este_ano":
                cal.set(Calendar.MONTH, Calendar.JANUARY);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                fechaInicio = new Timestamp(cal.getTime());
                fechaFin = FirestoreHelper.getFinMesActual();
                break;
        }

        Log.d(TAG, "📅 Período establecido: " + periodo + " desde " + fechaInicio.toDate() + " hasta " + fechaFin.toDate());
    }

    private void mostrarDatePicker(boolean esFechaInicio) {
        Calendar cal = Calendar.getInstance();

        // Si ya hay una fecha seleccionada, usar esa como inicial
        if (esFechaInicio && fechaInicio != null) {
            cal.setTime(fechaInicio.toDate());
        } else if (!esFechaInicio && fechaFin != null) {
            cal.setTime(fechaFin.toDate());
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);

                    if (esFechaInicio) {
                        // Fecha inicio: establecer a las 00:00:00
                        selectedDate.set(Calendar.HOUR_OF_DAY, 0);
                        selectedDate.set(Calendar.MINUTE, 0);
                        selectedDate.set(Calendar.SECOND, 0);
                        selectedDate.set(Calendar.MILLISECOND, 0);
                        fechaInicio = new Timestamp(selectedDate.getTime());

                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        etFechaInicio.setText(sdf.format(fechaInicio.toDate()));

                        Log.d(TAG, "📅 Fecha inicio seleccionada: " + fechaInicio.toDate());
                    } else {
                        // Fecha fin: establecer a las 23:59:59
                        selectedDate.set(Calendar.HOUR_OF_DAY, 23);
                        selectedDate.set(Calendar.MINUTE, 59);
                        selectedDate.set(Calendar.SECOND, 59);
                        selectedDate.set(Calendar.MILLISECOND, 999);
                        fechaFin = new Timestamp(selectedDate.getTime());

                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        etFechaFin.setText(sdf.format(fechaFin.toDate()));

                        Log.d(TAG, "📅 Fecha fin seleccionada: " + fechaFin.toDate());
                    }
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void cargarTecnicosDisponibles() {
        db.collection("usuarios")
                .whereEqualTo("rol", "tecnico")
                .whereEqualTo("estado", "activo")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaTecnicosDisponibles.clear();

                    // Agregar opción "Todos los técnicos"
                    Map<String, String> todos = new HashMap<>();
                    todos.put("id", null);
                    todos.put("nombre", "Todos los técnicos");
                    listaTecnicosDisponibles.add(todos);

                    // Agregar técnicos
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, String> tecnico = new HashMap<>();
                        tecnico.put("id", doc.getId());
                        tecnico.put("nombre", doc.getString("nombre"));
                        listaTecnicosDisponibles.add(tecnico);
                    }

                    // Configurar adapter del spinner
                    List<String> nombresTecnicos = new ArrayList<>();
                    for (Map<String, String> tec : listaTecnicosDisponibles) {
                        nombresTecnicos.add(tec.get("nombre"));
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_dropdown_item_1line,
                            nombresTecnicos
                    );
                    spinnerTecnico.setAdapter(adapter);

                    Log.d(TAG, "✅ Técnicos cargados: " + listaTecnicosDisponibles.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error al cargar técnicos: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar técnicos", Toast.LENGTH_SHORT).show();
                });
    }

    private void cargarDatos() {
        progressBar.setVisibility(View.VISIBLE);

        // Construir query con filtros
        com.google.firebase.firestore.Query query = db.collection("mantenimientos")
                .whereGreaterThanOrEqualTo("fechaProgramada", fechaInicio)
                .whereLessThanOrEqualTo("fechaProgramada", fechaFin);

        // Aplicar filtro por técnico si está seleccionado
        if (tecnicoIdFiltro != null && !tecnicoIdFiltro.isEmpty()) {
            query = query.whereEqualTo("tecnicoPrincipalId", tecnicoIdFiltro);
            Log.d(TAG, "🔍 Filtrando por técnico: " + tecnicoNombreFiltro + " (ID: " + tecnicoIdFiltro + ")");
        }

        query.get()
                .addOnSuccessListener(querySnapshot -> {
                    int total = 0;
                    int completados = 0;
                    int enProceso = 0;
                    int cancelados = 0;
                    int aTiempo = 0;
                    double sumaCalificaciones = 0;
                    int countCalificaciones = 0;

                    int preventivos = 0;
                    int correctivos = 0;
                    int emergencias = 0;

                    Map<String, Integer> equipoCount = new HashMap<>();
                    Map<String, TecnicoStats> tecnicoStats = new HashMap<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        total++;

                        String estado = doc.getString("estado");
                        String tipo = doc.getString("tipo");
                        String equipoId = doc.getString("equipoId");
                        String tecnicoId = doc.getString("tecnicoPrincipalId");

                        // Contar estados
                        if ("completado".equals(estado)) {
                            completados++;

                            // Verificar si se completó a tiempo
                            Timestamp fechaProgramada = doc.getTimestamp("fechaProgramada");
                            Timestamp fechaFinalizacion = doc.getTimestamp("fechaFinalizacion");
                            if (fechaProgramada != null && fechaFinalizacion != null) {
                                if (!fechaFinalizacion.toDate().after(fechaProgramada.toDate())) {
                                    aTiempo++;
                                }
                            }

                            // Calificación
                            Long calificacion = doc.getLong("calificacionCliente");
                            if (calificacion != null && calificacion > 0) {
                                sumaCalificaciones += calificacion;
                                countCalificaciones++;
                            }

                            // Estadísticas de técnico
                            if (tecnicoId != null) {
                                if (!tecnicoStats.containsKey(tecnicoId)) {
                                    tecnicoStats.put(tecnicoId, new TecnicoStats());
                                }
                                TecnicoStats stats = tecnicoStats.get(tecnicoId);
                                stats.totalServicios++;
                                if (calificacion != null && calificacion > 0) {
                                    stats.sumaCalificaciones += calificacion;
                                    stats.countCalificaciones++;
                                }
                            }
                        } else if ("en_proceso".equals(estado)) {
                            enProceso++;
                        } else if ("cancelado".equals(estado)) {
                            cancelados++;
                        }

                        // Contar tipos
                        if ("preventivo".equals(tipo)) {
                            preventivos++;
                        } else if ("correctivo".equals(tipo)) {
                            correctivos++;
                        } else if ("emergencia".equals(tipo)) {
                            emergencias++;
                        }

                        // Contar equipos
                        if (equipoId != null) {
                            equipoCount.put(equipoId, equipoCount.getOrDefault(equipoId, 0) + 1);
                        }
                    }

                    // Actualizar UI con métricas
                    tvTotalMantenimientos.setText(String.valueOf(total));
                    tvCompletados.setText(String.valueOf(completados));
                    tvEnProceso.setText(String.valueOf(enProceso));
                    tvCancelados.setText(String.valueOf(cancelados));

                    if (completados > 0) {
                        int porcentaje = (int) ((aTiempo * 100.0) / completados);
                        tvPorcentajeATiempo.setText(porcentaje + "%");
                    } else {
                        tvPorcentajeATiempo.setText("0%");
                    }

                    if (countCalificaciones > 0) {
                        double promedio = sumaCalificaciones / countCalificaciones;
                        tvCalificacionPromedio.setText(String.format(Locale.getDefault(), "%.1f", promedio));
                    } else {
                        tvCalificacionPromedio.setText("0.0");
                    }

                    tvPreventivos.setText(String.valueOf(preventivos));
                    tvCorrectivos.setText(String.valueOf(correctivos));
                    tvEmergencias.setText(String.valueOf(emergencias));

                    // Top equipos
                    cargarTopEquipos(equipoCount);

                    // Técnicos
                    cargarEstadisticasTecnicos(tecnicoStats);

                    progressBar.setVisibility(View.GONE);
                    Log.d(TAG, "✅ Datos cargados: " + total + " mantenimientos");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error al cargar datos: " + e.getMessage());
                    Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void cargarTopEquipos(Map<String, Integer> equipoCount) {
        if (equipoCount.isEmpty()) {
            tvTopEquipos.setText("Sin datos");
            return;
        }

        // Obtener top 5
        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(equipoCount.entrySet());
        sortedList.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(5, sortedList.size()); i++) {
            Map.Entry<String, Integer> entry = sortedList.get(i);
            String equipoId = entry.getKey();
            int total = entry.getValue();
            final int index = i + 1;

            // Cargar nombre del equipo
            db.collection("equipos").document(equipoId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String marca = doc.getString("marca");
                            String modelo = doc.getString("modelo");
                            String texto = index + ". " + marca + " " + modelo + " (" + total + " mantenimientos)\n";
                            sb.append(texto);
                            tvTopEquipos.setText(sb.toString());
                        }
                    });
        }
    }

    private void cargarEstadisticasTecnicos(Map<String, TecnicoStats> tecnicoStats) {
        if (tecnicoStats.isEmpty()) {
            tvSinTecnicos.setVisibility(View.VISIBLE);
            recyclerTecnicos.setVisibility(View.GONE);
            return;
        }

        listaTecnicos.clear();

        for (Map.Entry<String, TecnicoStats> entry : tecnicoStats.entrySet()) {
            String tecnicoId = entry.getKey();
            TecnicoStats stats = entry.getValue();

            // Cargar nombre del técnico
            db.collection("usuarios").document(tecnicoId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String nombre = doc.getString("nombre");
                            double promedio = stats.countCalificaciones > 0 ?
                                    stats.sumaCalificaciones / stats.countCalificaciones : 0;

                            listaTecnicos.add(new TecnicoEstadisticaAdapter.TecnicoEstadistica(
                                    nombre, stats.totalServicios, promedio
                            ));

                            // Ordenar por total de servicios
                            listaTecnicos.sort((a, b) -> Integer.compare(b.getTotalServicios(), a.getTotalServicios()));

                            tvSinTecnicos.setVisibility(View.GONE);
                            recyclerTecnicos.setVisibility(View.VISIBLE);
                            tecnicoAdapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    // Clase auxiliar para estadísticas de técnicos
    private static class TecnicoStats {
        int totalServicios = 0;
        double sumaCalificaciones = 0;
        int countCalificaciones = 0;
    }

    // ========== EXPORTACIÓN PDF ==========

    private void verificarPermisosYExportarPDF() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ no requiere permisos para archivos propios
            exportarPDF();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                exportarPDF();
            }
        } else {
            exportarPDF();
        }
    }

    private void exportarPDF() {
        try {
            // Crear directorio
            File directorio = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (directorio == null || (!directorio.exists() && !directorio.mkdirs())) {
                Toast.makeText(this, "Error al crear directorio", Toast.LENGTH_SHORT).show();
                return;
            }

            SimpleDateFormat sdfArchivo = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String nombreArchivo = "Reporte_TechMaintenance_" + sdfArchivo.format(new Date()) + ".pdf";
            File archivo = new File(directorio, nombreArchivo);

            PdfWriter writer = new PdfWriter(archivo);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Colores corporativos
            com.itextpdf.kernel.colors.DeviceRgb colorPrimary = new com.itextpdf.kernel.colors.DeviceRgb(33, 150, 243); // #2196F3
            com.itextpdf.kernel.colors.DeviceRgb colorSuccess = new com.itextpdf.kernel.colors.DeviceRgb(76, 175, 80); // #4CAF50
            com.itextpdf.kernel.colors.DeviceRgb colorGris = new com.itextpdf.kernel.colors.DeviceRgb(245, 245, 245);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm", Locale.getDefault());

            // ==================== PORTADA ====================
            document.add(new Paragraph("\n\n\n"));
            document.add(new Paragraph("TECHMAINTENANCE")
                    .setFontSize(28)
                    .setBold()
                    .setFontColor(colorPrimary)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            document.add(new Paragraph("Sistema de Gestión de Mantenimientos")
                    .setFontSize(14)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            document.add(new Paragraph("\n\n"));

            document.add(new Paragraph("REPORTE DE MANTENIMIENTOS")
                    .setFontSize(22)
                    .setBold()
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            document.add(new Paragraph("\n"));

            // Información del período
            String periodoTexto = getPeriodoTexto();
            document.add(new Paragraph("Período: " + periodoTexto)
                    .setFontSize(14)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            document.add(new Paragraph(sdf.format(fechaInicio.toDate()) + " - " + sdf.format(fechaFin.toDate()))
                    .setFontSize(12)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Generado: " + sdf.format(new Date()) + " " + sdfHora.format(new Date()))
                    .setFontSize(10)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            document.add(new Paragraph("\n\n"));

            // Línea separadora
            com.itextpdf.layout.element.LineSeparator ls = new com.itextpdf.layout.element.LineSeparator(
                    new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f));
            ls.setStrokeColor(colorPrimary);
            document.add(ls);

            document.add(new Paragraph("\n\n"));

            // ==================== RESUMEN EJECUTIVO ====================
            document.add(new Paragraph("RESUMEN EJECUTIVO")
                    .setFontSize(16)
                    .setBold()
                    .setFontColor(colorPrimary));

            document.add(new Paragraph("\n"));

            // Tabla de métricas principales (2x3)
            float[] columnWidthsMetricas = {1, 1, 1};
            Table tableMetricas = new Table(columnWidthsMetricas);
            tableMetricas.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

            // Fila 1
            agregarCeldaMetrica(tableMetricas, "Total Mantenimientos", tvTotalMantenimientos.getText().toString(), colorPrimary);
            agregarCeldaMetrica(tableMetricas, "Completados", tvCompletados.getText().toString(), colorSuccess);
            agregarCeldaMetrica(tableMetricas, "En Proceso", tvEnProceso.getText().toString(), new com.itextpdf.kernel.colors.DeviceRgb(255, 152, 0));

            // Fila 2
            agregarCeldaMetrica(tableMetricas, "Cancelados", tvCancelados.getText().toString(), new com.itextpdf.kernel.colors.DeviceRgb(244, 67, 54));
            agregarCeldaMetrica(tableMetricas, "% A Tiempo", tvPorcentajeATiempo.getText().toString(), colorSuccess);
            agregarCeldaMetrica(tableMetricas, "Calificación", tvCalificacionPromedio.getText().toString() + " ⭐", new com.itextpdf.kernel.colors.DeviceRgb(255, 193, 7));

            document.add(tableMetricas);
            document.add(new Paragraph("\n\n"));

            // ==================== DISTRIBUCIÓN POR TIPO ====================
            document.add(new Paragraph("DISTRIBUCIÓN POR TIPO DE MANTENIMIENTO")
                    .setFontSize(16)
                    .setBold()
                    .setFontColor(colorPrimary));

            document.add(new Paragraph("\n"));

            float[] columnWidthsTipo = {3, 1, 2};
            Table tableTipo = new Table(columnWidthsTipo);
            tableTipo.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

            // Header
            tableTipo.addHeaderCell(crearCeldaHeader("Tipo"));
            tableTipo.addHeaderCell(crearCeldaHeader("Cantidad"));
            tableTipo.addHeaderCell(crearCeldaHeader("Porcentaje"));

            // Calcular porcentajes
            int totalTipos = Integer.parseInt(tvPreventivos.getText().toString()) +
                    Integer.parseInt(tvCorrectivos.getText().toString()) +
                    Integer.parseInt(tvEmergencias.getText().toString());

            int preventivos = Integer.parseInt(tvPreventivos.getText().toString());
            int correctivos = Integer.parseInt(tvCorrectivos.getText().toString());
            int emergencias = Integer.parseInt(tvEmergencias.getText().toString());

            tableTipo.addCell(crearCelda("Preventivo"));
            tableTipo.addCell(crearCeldaCentrada(String.valueOf(preventivos)));
            tableTipo.addCell(crearCeldaCentrada(totalTipos > 0 ? String.format("%.1f%%", (preventivos * 100.0 / totalTipos)) : "0%"));

            tableTipo.addCell(crearCelda("Correctivo"));
            tableTipo.addCell(crearCeldaCentrada(String.valueOf(correctivos)));
            tableTipo.addCell(crearCeldaCentrada(totalTipos > 0 ? String.format("%.1f%%", (correctivos * 100.0 / totalTipos)) : "0%"));

            tableTipo.addCell(crearCelda("Emergencia"));
            tableTipo.addCell(crearCeldaCentrada(String.valueOf(emergencias)));
            tableTipo.addCell(crearCeldaCentrada(totalTipos > 0 ? String.format("%.1f%%", (emergencias * 100.0 / totalTipos)) : "0%"));

            document.add(tableTipo);
            document.add(new Paragraph("\n\n"));

            // ==================== DESEMPEÑO DE TÉCNICOS ====================
            if (!listaTecnicos.isEmpty()) {
                document.add(new Paragraph("DESEMPEÑO DEL EQUIPO TÉCNICO")
                        .setFontSize(16)
                        .setBold()
                        .setFontColor(colorPrimary));

                document.add(new Paragraph("\n"));

                float[] columnWidthsTecnicos = {3, 1, 1, 1};
                Table tableTecnicos = new Table(columnWidthsTecnicos);
                tableTecnicos.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

                // Header
                tableTecnicos.addHeaderCell(crearCeldaHeader("Técnico"));
                tableTecnicos.addHeaderCell(crearCeldaHeader("Servicios"));
                tableTecnicos.addHeaderCell(crearCeldaHeader("Calificación"));
                tableTecnicos.addHeaderCell(crearCeldaHeader("Desempeño"));

                // Datos
                for (TecnicoEstadisticaAdapter.TecnicoEstadistica tecnico : listaTecnicos) {
                    tableTecnicos.addCell(crearCelda(tecnico.getNombre()));
                    tableTecnicos.addCell(crearCeldaCentrada(String.valueOf(tecnico.getTotalServicios())));
                    tableTecnicos.addCell(crearCeldaCentrada(String.format("%.1f ⭐", tecnico.getCalificacionPromedio())));

                    String desempeno = "";
                    double calificacion = tecnico.getCalificacionPromedio();
                    if (calificacion >= 4.5) desempeno = "⭐ Excelente";
                    else if (calificacion >= 4.0) desempeno = "✅ Muy Bueno";
                    else if (calificacion >= 3.5) desempeno = "👍 Bueno";
                    else if (calificacion >= 3.0) desempeno = "⚠️ Regular";
                    else desempeno = "❌ Necesita Mejorar";

                    tableTecnicos.addCell(crearCeldaCentrada(desempeno));
                }

                document.add(tableTecnicos);
            }

            document.add(new Paragraph("\n\n"));

            // ==================== FOOTER ====================
            document.add(ls);
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("TechSolution © 2025 - Sistema de Gestión de Mantenimientos")
                    .setFontSize(10)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            document.close();

            Toast.makeText(this, "✅ PDF generado exitosamente", Toast.LENGTH_LONG).show();
            compartirArchivo(archivo, "application/pdf");
            Log.d(TAG, "✅ PDF profesional creado: " + archivo.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "❌ Error al crear PDF: " + e.getMessage(), e);
            Toast.makeText(this, "Error al crear PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Métodos auxiliares para PDF
    private String getPeriodoTexto() {
        switch (periodoActual) {
            case "hoy": return "Hoy";
            case "esta_semana": return "Esta Semana";
            case "este_mes": return "Este Mes";
            case "ultimos_3_meses": return "Últimos 3 Meses";
            case "este_ano": return "Este Año";
            default: return "Personalizado";
        }
    }

    private void agregarCeldaMetrica(Table table, String label, String value, com.itextpdf.kernel.colors.DeviceRgb color) {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell();
        cell.setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(245, 245, 245));
        cell.setPadding(10);
        cell.setBorder(new com.itextpdf.layout.borders.SolidBorder(color, 2));

        Paragraph labelPara = new Paragraph(label)
                .setFontSize(10)
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY);

        Paragraph valuePara = new Paragraph(value)
                .setFontSize(20)
                .setBold()
                .setFontColor(color);

        cell.add(labelPara);
        cell.add(valuePara);
        table.addCell(cell);
    }

    private com.itextpdf.layout.element.Cell crearCeldaHeader(String texto) {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(texto).setBold().setFontSize(11))
                .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(33, 150, 243))
                .setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE)
                .setPadding(8)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
        return cell;
    }

    private com.itextpdf.layout.element.Cell crearCelda(String texto) {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(texto).setFontSize(10))
                .setPadding(6);
        return cell;
    }

    private com.itextpdf.layout.element.Cell crearCeldaCentrada(String texto) {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(texto).setFontSize(10))
                .setPadding(6)
                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
        return cell;
    }

    // ========== EXPORTACIÓN EXCEL ==========

    private void verificarPermisosYExportarExcel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            exportarExcel();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                exportarExcel();
            }
        } else {
            exportarExcel();
        }
    }

    private void exportarExcel() {
        try {
            File directorio = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (directorio == null || (!directorio.exists() && !directorio.mkdirs())) {
                Toast.makeText(this, "Error al crear directorio", Toast.LENGTH_SHORT).show();
                return;
            }

            String nombreArchivo = "Reporte_TechMaintenance_" + periodoActual + "_" +
                    new SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault()).format(new Date()) + ".xlsx";
            File archivo = new File(directorio, nombreArchivo);

            XSSFWorkbook workbook = new XSSFWorkbook();

            // ===== ESTILOS =====
            // Estilo título principal
            XSSFCellStyle estiloTitulo = workbook.createCellStyle();
            XSSFFont fuenteTitulo = workbook.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 16);
            fuenteTitulo.setColor(IndexedColors.WHITE.getIndex());
            estiloTitulo.setFont(fuenteTitulo);
            estiloTitulo.setFillForegroundColor(IndexedColors.BLUE.getIndex()); // Azul
            estiloTitulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloTitulo.setAlignment(HorizontalAlignment.CENTER);
            estiloTitulo.setVerticalAlignment(VerticalAlignment.CENTER);

            // Estilo subtítulo
            XSSFCellStyle estiloSubtitulo = workbook.createCellStyle();
            XSSFFont fuenteSubtitulo = workbook.createFont();
            fuenteSubtitulo.setBold(true);
            fuenteSubtitulo.setFontHeightInPoints((short) 12);
            fuenteSubtitulo.setColor(IndexedColors.WHITE.getIndex());
            estiloSubtitulo.setFont(fuenteSubtitulo);
            estiloSubtitulo.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            estiloSubtitulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloSubtitulo.setAlignment(HorizontalAlignment.CENTER);
            estiloSubtitulo.setVerticalAlignment(VerticalAlignment.CENTER);

            // Estilo encabezado tabla
            XSSFCellStyle estiloHeader = workbook.createCellStyle();
            XSSFFont fuenteHeader = workbook.createFont();
            fuenteHeader.setBold(true);
            fuenteHeader.setColor(IndexedColors.WHITE.getIndex());
            estiloHeader.setFont(fuenteHeader);
            estiloHeader.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            estiloHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloHeader.setAlignment(HorizontalAlignment.CENTER);
            estiloHeader.setBorderBottom(BorderStyle.THIN);
            estiloHeader.setBorderTop(BorderStyle.THIN);
            estiloHeader.setBorderLeft(BorderStyle.THIN);
            estiloHeader.setBorderRight(BorderStyle.THIN);

            // Estilo celda métrica (label)
            XSSFCellStyle estiloMetricaLabel = workbook.createCellStyle();
            XSSFFont fuenteMetrica = workbook.createFont();
            fuenteMetrica.setBold(true);
            estiloMetricaLabel.setFont(fuenteMetrica);
            estiloMetricaLabel.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            estiloMetricaLabel.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloMetricaLabel.setBorderBottom(BorderStyle.THIN);
            estiloMetricaLabel.setBorderTop(BorderStyle.THIN);
            estiloMetricaLabel.setBorderLeft(BorderStyle.THIN);
            estiloMetricaLabel.setBorderRight(BorderStyle.THIN);

            // Estilo celda métrica (valor)
            XSSFCellStyle estiloMetricaValor = workbook.createCellStyle();
            XSSFFont fuenteValor = workbook.createFont();
            fuenteValor.setFontHeightInPoints((short) 14);
            fuenteValor.setBold(true);
            estiloMetricaValor.setFont(fuenteValor);
            estiloMetricaValor.setAlignment(HorizontalAlignment.CENTER);
            estiloMetricaValor.setBorderBottom(BorderStyle.THIN);
            estiloMetricaValor.setBorderTop(BorderStyle.THIN);
            estiloMetricaValor.setBorderLeft(BorderStyle.THIN);
            estiloMetricaValor.setBorderRight(BorderStyle.THIN);

            // Estilo celda normal
            XSSFCellStyle estiloCelda = workbook.createCellStyle();
            estiloCelda.setBorderBottom(BorderStyle.THIN);
            estiloCelda.setBorderTop(BorderStyle.THIN);
            estiloCelda.setBorderLeft(BorderStyle.THIN);
            estiloCelda.setBorderRight(BorderStyle.THIN);
            estiloCelda.setAlignment(HorizontalAlignment.CENTER);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

            // ===== HOJA 1: RESUMEN EJECUTIVO =====
            XSSFSheet hojaResumen = workbook.createSheet("📊 Resumen Ejecutivo");

            int rowNum = 0;

            // Título principal
            Row titulo = hojaResumen.createRow(rowNum++);
            titulo.setHeightInPoints(30);
            Cell cellTitulo = titulo.createCell(0);
            cellTitulo.setCellValue("REPORTE DE MANTENIMIENTOS - TECHMAINTENANCE");
            cellTitulo.setCellStyle(estiloTitulo);
            hojaResumen.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 5));

            // Período
            Row periodoRow = hojaResumen.createRow(rowNum++);
            periodoRow.setHeightInPoints(25);
            Cell cellPeriodo = periodoRow.createCell(0);
            cellPeriodo.setCellValue("Período: " + getPeriodoTexto() + " (" +
                    sdf.format(fechaInicio.toDate()) + " - " + sdf.format(fechaFin.toDate()) + ")");
            cellPeriodo.setCellStyle(estiloSubtitulo);
            hojaResumen.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 5));

            rowNum++; // Espacio

            // MÉTRICAS PRINCIPALES (tabla 2x3)
            Row headerMetricas = hojaResumen.createRow(rowNum++);
            headerMetricas.setHeightInPoints(25);
            Cell cellHeaderMetricas = headerMetricas.createCell(0);
            cellHeaderMetricas.setCellValue("📈 MÉTRICAS PRINCIPALES");
            cellHeaderMetricas.setCellStyle(estiloHeader);
            hojaResumen.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 5));

            // Fila 1 de métricas
            Row metrica1 = hojaResumen.createRow(rowNum++);
            crearCeldaMetricaExcel(hojaResumen, metrica1, 0, "Total Mantenimientos", tvTotalMantenimientos.getText().toString(), estiloMetricaLabel, estiloMetricaValor);
            crearCeldaMetricaExcel(hojaResumen, metrica1, 2, "Completados", tvCompletados.getText().toString(), estiloMetricaLabel, estiloMetricaValor);
            crearCeldaMetricaExcel(hojaResumen, metrica1, 4, "En Proceso", tvEnProceso.getText().toString(), estiloMetricaLabel, estiloMetricaValor);

            // Fila 2 de métricas
            Row metrica2 = hojaResumen.createRow(rowNum++);
            crearCeldaMetricaExcel(hojaResumen, metrica2, 0, "Cancelados", tvCancelados.getText().toString(), estiloMetricaLabel, estiloMetricaValor);
            crearCeldaMetricaExcel(hojaResumen, metrica2, 2, "% A Tiempo", tvPorcentajeATiempo.getText().toString(), estiloMetricaLabel, estiloMetricaValor);
            crearCeldaMetricaExcel(hojaResumen, metrica2, 4, "Calificación Promedio", tvCalificacionPromedio.getText().toString(), estiloMetricaLabel, estiloMetricaValor);

            rowNum++; // Espacio

            // DISTRIBUCIÓN POR TIPO
            Row headerTipo = hojaResumen.createRow(rowNum++);
            headerTipo.setHeightInPoints(25);
            Cell cellHeaderTipo = headerTipo.createCell(0);
            cellHeaderTipo.setCellValue("🔧 DISTRIBUCIÓN POR TIPO");
            cellHeaderTipo.setCellStyle(estiloHeader);
            hojaResumen.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));

            // Headers de tabla
            Row headerTabla = hojaResumen.createRow(rowNum++);
            crearCeldaConEstilo(headerTabla, 0, "Tipo", estiloHeader);
            crearCeldaConEstilo(headerTabla, 1, "Cantidad", estiloHeader);
            crearCeldaConEstilo(headerTabla, 2, "Porcentaje", estiloHeader);

            // Datos
            int total = Integer.parseInt(tvTotalMantenimientos.getText().toString());
            agregarFilaTipoExcel(hojaResumen, rowNum++, "Preventivo", tvPreventivos.getText().toString(), total, estiloCelda);
            agregarFilaTipoExcel(hojaResumen, rowNum++, "Correctivo", tvCorrectivos.getText().toString(), total, estiloCelda);
            agregarFilaTipoExcel(hojaResumen, rowNum++, "Emergencia", tvEmergencias.getText().toString(), total, estiloCelda);

            rowNum++; // Espacio

            // RENDIMIENTO DE TÉCNICOS (si hay datos)
            if (listaTecnicos != null && !listaTecnicos.isEmpty()) {
                Row headerTecnicos = hojaResumen.createRow(rowNum++);
                headerTecnicos.setHeightInPoints(25);
                Cell cellHeaderTecnicos = headerTecnicos.createCell(0);
                cellHeaderTecnicos.setCellValue("👥 RENDIMIENTO DE TÉCNICOS");
                cellHeaderTecnicos.setCellStyle(estiloHeader);
                hojaResumen.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 4));

                // Headers
                Row headerTablaTec = hojaResumen.createRow(rowNum++);
                crearCeldaConEstilo(headerTablaTec, 0, "Técnico", estiloHeader);
                crearCeldaConEstilo(headerTablaTec, 1, "Servicios", estiloHeader);
                crearCeldaConEstilo(headerTablaTec, 2, "Calificación", estiloHeader);
                crearCeldaConEstilo(headerTablaTec, 3, "Desempeño", estiloHeader);

                // Datos de técnicos
                for (TecnicoEstadisticaAdapter.TecnicoEstadistica tecnico : listaTecnicos) {
                    String nombre = tecnico.getNombre();
                    int servicios = tecnico.getTotalServicios();
                    double calificacion = tecnico.getCalificacionPromedio();

                    String desempeno = "";
                    if (calificacion >= 4.5) desempeno = "⭐ Excelente";
                    else if (calificacion >= 4.0) desempeno = "✅ Muy Bueno";
                    else if (calificacion >= 3.5) desempeno = "👍 Bueno";
                    else if (calificacion >= 3.0) desempeno = "⚠️ Regular";
                    else desempeno = "❌ Necesita Mejorar";

                    Row filaTec = hojaResumen.createRow(rowNum++);
                    crearCeldaConEstilo(filaTec, 0, nombre, estiloCelda);
                    crearCeldaConEstilo(filaTec, 1, String.valueOf(servicios), estiloCelda);
                    crearCeldaConEstilo(filaTec, 2, String.format("%.1f", calificacion), estiloCelda);
                    crearCeldaConEstilo(filaTec, 3, desempeno, estiloCelda);
                }
            }

            // Ajustar anchos de columna manualmente (autoSizeColumn no funciona en Android)
            hojaResumen.setColumnWidth(0, 6000);  // Col A - Labels/Técnico
            hojaResumen.setColumnWidth(1, 4000);  // Col B - Valores/Servicios
            hojaResumen.setColumnWidth(2, 6000);  // Col C - Labels/Calificación
            hojaResumen.setColumnWidth(3, 4000);  // Col D - Valores/Desempeño
            hojaResumen.setColumnWidth(4, 6000);  // Col E - Labels
            hojaResumen.setColumnWidth(5, 4000);  // Col F - Valores

            // ===== HOJA 2: DETALLE DE MANTENIMIENTOS =====
            XSSFSheet hojaDetalle = workbook.createSheet("📋 Detalle Mantenimientos");
            int rowDetalle = 0;

            // Título
            Row tituloDetalle = hojaDetalle.createRow(rowDetalle++);
            tituloDetalle.setHeightInPoints(25);
            Cell cellTituloDetalle = tituloDetalle.createCell(0);
            cellTituloDetalle.setCellValue("DETALLE COMPLETO DE MANTENIMIENTOS");
            cellTituloDetalle.setCellStyle(estiloTitulo);
            hojaDetalle.addMergedRegion(new CellRangeAddress(rowDetalle - 1, rowDetalle - 1, 0, 7));

            rowDetalle++; // Espacio

            // Headers
            Row headerDetalle = hojaDetalle.createRow(rowDetalle++);
            crearCeldaConEstilo(headerDetalle, 0, "Fecha", estiloHeader);
            crearCeldaConEstilo(headerDetalle, 1, "Cliente", estiloHeader);
            crearCeldaConEstilo(headerDetalle, 2, "Equipo", estiloHeader);
            crearCeldaConEstilo(headerDetalle, 3, "Técnico", estiloHeader);
            crearCeldaConEstilo(headerDetalle, 4, "Tipo", estiloHeader);
            crearCeldaConEstilo(headerDetalle, 5, "Estado", estiloHeader);
            crearCeldaConEstilo(headerDetalle, 6, "Prioridad", estiloHeader);
            crearCeldaConEstilo(headerDetalle, 7, "Calificación", estiloHeader);

            // Datos de mantenimientos (ejemplo - aquí deberías cargar desde Firestore)
            // Como ya tienes los datos en memoria, podrías guardarlos y usarlos aquí
            Row filaEjemplo = hojaDetalle.createRow(rowDetalle++);
            crearCeldaConEstilo(filaEjemplo, 0, sdf.format(new Date()), estiloCelda);
            crearCeldaConEstilo(filaEjemplo, 1, "Datos desde Firestore", estiloCelda);
            crearCeldaConEstilo(filaEjemplo, 2, "(implementar carga)", estiloCelda);
            crearCeldaConEstilo(filaEjemplo, 3, "", estiloCelda);
            crearCeldaConEstilo(filaEjemplo, 4, "", estiloCelda);
            crearCeldaConEstilo(filaEjemplo, 5, "", estiloCelda);
            crearCeldaConEstilo(filaEjemplo, 6, "", estiloCelda);
            crearCeldaConEstilo(filaEjemplo, 7, "", estiloCelda);

            // Ajustar anchos manualmente (autoSizeColumn no funciona en Android)
            hojaDetalle.setColumnWidth(0, 3500);  // Fecha
            hojaDetalle.setColumnWidth(1, 6000);  // Cliente
            hojaDetalle.setColumnWidth(2, 5000);  // Equipo
            hojaDetalle.setColumnWidth(3, 5000);  // Técnico
            hojaDetalle.setColumnWidth(4, 3500);  // Tipo
            hojaDetalle.setColumnWidth(5, 3500);  // Estado
            hojaDetalle.setColumnWidth(6, 3500);  // Prioridad
            hojaDetalle.setColumnWidth(7, 3500);  // Calificación

            // ===== GUARDAR ARCHIVO =====
            FileOutputStream outputStream = new FileOutputStream(archivo);
            workbook.write(outputStream);
            workbook.close();
            outputStream.close();

            Toast.makeText(this, "✅ Excel exportado: " + nombreArchivo, Toast.LENGTH_LONG).show();

            // Compartir archivo
            compartirArchivo(archivo, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            Log.d(TAG, "✅ Excel profesional creado: " + archivo.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "❌ Error al crear Excel: " + e.getMessage(), e);
            Toast.makeText(this, "Error al crear Excel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Métodos helper para Excel
    private void crearCeldaMetricaExcel(XSSFSheet sheet, Row row, int colIndex, String label, String value,
                                       XSSFCellStyle estiloLabel, XSSFCellStyle estiloValor) {
        Cell cellLabel = row.createCell(colIndex);
        cellLabel.setCellValue(label);
        cellLabel.setCellStyle(estiloLabel);

        Cell cellValor = row.createCell(colIndex + 1);
        cellValor.setCellValue(value);
        cellValor.setCellStyle(estiloValor);
    }

    private void crearCeldaConEstilo(Row row, int colIndex, String value, XSSFCellStyle estilo) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value);
        cell.setCellStyle(estilo);
    }

    private void agregarFilaTipoExcel(XSSFSheet sheet, int rowNum, String tipo, String cantidad,
                                     int total, XSSFCellStyle estilo) {
        Row row = sheet.createRow(rowNum);
        int cant = Integer.parseInt(cantidad);
        double porcentaje = total > 0 ? (cant * 100.0 / total) : 0;

        crearCeldaConEstilo(row, 0, tipo, estilo);
        crearCeldaConEstilo(row, 1, cantidad, estilo);
        crearCeldaConEstilo(row, 2, String.format("%.1f%%", porcentaje), estilo);
    }

    private void compartirArchivo(File archivo, String mimeType) {
        Uri uri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider",
                archivo);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(mimeType);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Compartir archivo"));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
