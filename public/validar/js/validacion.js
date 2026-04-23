// Variables globales
let currentRating = 0;
let codigoValidacion = '';
let mantenimientoId = '';
let mantenimientoData = null;

// Inicializar la página
document.addEventListener('DOMContentLoaded', function() {
    // Obtener parámetros de URL
    const urlParams = new URLSearchParams(window.location.search);
    codigoValidacion = urlParams.get('codigo');
    mantenimientoId = urlParams.get('id');

    console.log('📋 Parámetros recibidos:', { codigo: codigoValidacion, id: mantenimientoId });

    // Contador de caracteres del comentario
    const comentarioTextarea = document.getElementById('comentario');
    const charCount = document.getElementById('charCount');

    if (comentarioTextarea) {
        comentarioTextarea.addEventListener('input', function() {
            charCount.textContent = this.value.length;
        });
    }

    // Habilitar/deshabilitar botón según checkbox
    const confirmacionCheckbox = document.getElementById('confirmacion');
    const submitBtn = document.getElementById('submitBtn');

    if (confirmacionCheckbox) {
        confirmacionCheckbox.addEventListener('change', function() {
            submitBtn.disabled = !(this.checked && currentRating > 0);
        });
    }

    // Cargar datos del servicio
    if (codigoValidacion && mantenimientoId) {
        cargarDatosServicio();
    } else {
        mostrarError('Link inválido', 'El enlace de validación no es válido. Por favor, verifica el código o contacta al técnico.');
    }
});

// Cargar datos del servicio desde Firestore
async function cargarDatosServicio() {
    mostrarEstado('loading');

    try {
        console.log('🔍 ========================================');
        console.log('🔍 BUSCANDO MANTENIMIENTO EN FIRESTORE');
        console.log('🔍 ========================================');
        console.log('   - Colección: mantenimientos');
        console.log('   - ID del documento:', mantenimientoId);
        console.log('   - Código esperado:', codigoValidacion);

        // Obtener documento del mantenimiento
        const mantenimientoDoc = await db.collection('mantenimientos').doc(mantenimientoId).get();

        console.log('   - Documento existe?:', mantenimientoDoc.exists);
        console.log('   - Documento completo:', mantenimientoDoc);

        if (!mantenimientoDoc.exists) {
            console.error('❌ DOCUMENTO NO EXISTE EN FIRESTORE');
            console.error('   - ID buscado:', mantenimientoId);
            console.error('   - Posibles causas:');
            console.error('     1. El documento fue eliminado');
            console.error('     2. El ID es incorrecto');
            console.error('     3. Hay un delay en la replicación de Firestore');
            mostrarError('Servicio no encontrado', 'No se encontró el servicio solicitado. Verifica el código o contacta al técnico.');
            return;
        }

        mantenimientoData = mantenimientoDoc.data();
        console.log('✅ Mantenimiento encontrado');
        console.log('   - Datos:', mantenimientoData);
        console.log('   - Código en BD:', mantenimientoData.codigoValidacion);
        console.log('   - Estado:', mantenimientoData.estado);
        console.log('   - Validado?:', mantenimientoData.validadoPorCliente);

        // Verificar código de validación
        if (mantenimientoData.codigoValidacion !== codigoValidacion) {
            mostrarError('Código inválido', 'El código de validación no coincide. Verifica el código o solicita uno nuevo.');
            return;
        }

        // Verificar si ya fue validado
        if (mantenimientoData.validadoPorCliente === true) {
            const fechaValidacion = mantenimientoData.fechaValidacion ?
                new Date(mantenimientoData.fechaValidacion.toDate()).toLocaleDateString('es-ES') : 'N/A';
            mostrarError('Ya validado', `Este servicio ya fue validado anteriormente el ${fechaValidacion}.`);
            return;
        }

        // Verificar expiración (24 horas)
        if (mantenimientoData.codigoExpiraEn) {
            const ahora = new Date();
            const expira = mantenimientoData.codigoExpiraEn.toDate();

            if (ahora > expira) {
                mostrarError('Código expirado', 'Este código expiró. Por favor, solicita un nuevo código al técnico.');
                return;
            }
        }

        // Cargar datos relacionados
        await cargarDatosRelacionados();

        // Mostrar contenido
        mostrarEstado('content');

    } catch (error) {
        console.error('❌ Error al cargar servicio:', error);
        mostrarError('Error de conexión', 'No se pudo conectar con el servidor. Verifica tu internet e intenta nuevamente.');
    }
}

// Cargar datos relacionados (cliente, equipo, técnico)
async function cargarDatosRelacionados() {
    try {
        console.log('🔍 Cargando datos relacionados...');
        console.log('📋 IDs del mantenimiento:', {
            clienteId: mantenimientoData.clienteId,
            equipoId: mantenimientoData.equipoId,
            tecnicoPrincipalId: mantenimientoData.tecnicoPrincipalId
        });

        // Cargar cliente
        console.log('📦 Buscando cliente:', mantenimientoData.clienteId);
        const clienteDoc = await db.collection('clientes').doc(mantenimientoData.clienteId).get();
        const clienteData = clienteDoc.exists ? clienteDoc.data() : {};
        console.log('✅ Cliente encontrado:', clienteDoc.exists, clienteData);

        // Cargar equipo
        console.log('💻 Buscando equipo:', mantenimientoData.equipoId);
        const equipoDoc = await db.collection('equipos').doc(mantenimientoData.equipoId).get();
        const equipoData = equipoDoc.exists ? equipoDoc.data() : {};
        console.log('✅ Equipo encontrado:', equipoDoc.exists, equipoData);

        // Cargar técnico
        console.log('👤 Buscando técnico:', mantenimientoData.tecnicoPrincipalId);
        const tecnicoDoc = await db.collection('usuarios').doc(mantenimientoData.tecnicoPrincipalId).get();
        const tecnicoData = tecnicoDoc.exists ? tecnicoDoc.data() : {};
        console.log('✅ Técnico encontrado:', tecnicoDoc.exists, tecnicoData);

        // Llenar datos en la interfaz
        console.log('🎨 Llenando interfaz con datos:', { clienteData, equipoData, tecnicoData });
        llenarDatosInterfaz(clienteData, equipoData, tecnicoData);

    } catch (error) {
        console.error('❌ ERROR CRÍTICO al cargar datos relacionados:', error);
        console.error('Stack trace:', error.stack);
        // Continuar de todas formas con datos limitados
        llenarDatosInterfaz({}, {}, {});
    }
}

// Llenar datos en la interfaz
function llenarDatosInterfaz(cliente, equipo, tecnico) {
    console.log('📝 Llenando datos en la interfaz:', { cliente, equipo, tecnico });

    // Fecha y hora del servicio
    if (mantenimientoData.fechaFin || mantenimientoData.fechaFinalizacion) {
        const fechaField = mantenimientoData.fechaFin || mantenimientoData.fechaFinalizacion;
        const fecha = fechaField.toDate ? fechaField.toDate() : new Date(fechaField);
        document.getElementById('fechaServicio').textContent =
            fecha.toLocaleDateString('es-ES', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
    } else if (mantenimientoData.fechaProgramada) {
        const fecha = mantenimientoData.fechaProgramada.toDate ?
            mantenimientoData.fechaProgramada.toDate() :
            new Date(mantenimientoData.fechaProgramada);
        document.getElementById('fechaServicio').textContent =
            fecha.toLocaleDateString('es-ES', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
    } else {
        document.getElementById('fechaServicio').textContent = 'Fecha no disponible';
    }

    // Duración
    if (mantenimientoData.duracionServicio) {
        document.getElementById('duracionServicio').textContent = mantenimientoData.duracionServicio;
    } else if (mantenimientoData.fechaInicio && (mantenimientoData.fechaFin || mantenimientoData.fechaFinalizacion)) {
        const inicio = mantenimientoData.fechaInicio.toDate ?
            mantenimientoData.fechaInicio.toDate() :
            new Date(mantenimientoData.fechaInicio);
        const finField = mantenimientoData.fechaFin || mantenimientoData.fechaFinalizacion;
        const fin = finField.toDate ? finField.toDate() : new Date(finField);
        const diff = (fin - inicio) / 1000 / 60; // minutos
        const horas = Math.floor(diff / 60);
        const minutos = Math.floor(diff % 60);
        document.getElementById('duracionServicio').textContent =
            `${horas > 0 ? horas + 'h ' : ''}${minutos}min`;
    } else {
        document.getElementById('duracionServicio').textContent = 'No disponible';
    }

    // ✅ Empresa - CORREGIDO con validación adicional
    const nombreEmpresaElement = document.getElementById('nombreEmpresa');
    if (nombreEmpresaElement) {
        const nombreEmpresa = cliente.nombreEmpresa || cliente.nombreContacto || 'No especificado';
        nombreEmpresaElement.textContent = nombreEmpresa;
        console.log('✅ Empresa mostrada:', nombreEmpresa);
    }

    // ✅ Equipo - CORREGIDO con validación adicional
    const equipoInfoElement = document.getElementById('equipoInfo');
    if (equipoInfoElement) {
        if (equipo.tipo || equipo.marca || equipo.modelo || equipo.numeroSerie) {
            const tipo = equipo.tipo || 'Equipo';
            const marca = equipo.marca || '';
            const modelo = equipo.modelo || '';
            const serie = equipo.numeroSerie || 'Sin S/N';
            const equipoInfo = `${tipo}${marca ? ' - ' + marca : ''}${modelo ? ' ' + modelo : ''} (S/N: ${serie})`;
            equipoInfoElement.textContent = equipoInfo;
            console.log('✅ Equipo mostrado:', equipoInfo);
        } else {
            equipoInfoElement.textContent = 'Información del equipo no disponible';
        }
    }

    // ✅ Técnico - CORREGIDO con validación adicional
    const nombreTecnicoElement = document.getElementById('nombreTecnico');
    if (nombreTecnicoElement) {
        const nombreTecnico = tecnico.nombre || 'No especificado';
        nombreTecnicoElement.textContent = nombreTecnico;
        console.log('✅ Técnico mostrado:', nombreTecnico);
    }

    // Trabajo realizado
    const trabajoRealizado = mantenimientoData.observacionesTecnico ||
                            mantenimientoData.descripcionServicio ||
                            'Sin descripción del trabajo realizado';
    document.getElementById('trabajoRealizado').textContent = trabajoRealizado;

    // Fotos de evidencia
    if (mantenimientoData.evidenciasFotograficas && mantenimientoData.evidenciasFotograficas.length > 0) {
        const photosContainer = document.getElementById('photosContainer');
        const photoGallery = document.getElementById('photoGallery');

        photosContainer.innerHTML = '';

        mantenimientoData.evidenciasFotograficas.forEach((url, index) => {
            const img = document.createElement('img');
            img.src = url;
            img.alt = `Evidencia ${index + 1}`;
            img.onclick = () => abrirImagen(url);
            photosContainer.appendChild(img);
        });

        photoGallery.classList.remove('hidden');
    }
}

// Sistema de calificación con estrellas
function setRating(rating) {
    currentRating = rating;

    const stars = document.querySelectorAll('.star');
    const ratingText = document.getElementById('ratingText');
    const submitBtn = document.getElementById('submitBtn');
    const confirmacionCheckbox = document.getElementById('confirmacion');

    // Actualizar estrellas
    stars.forEach((star, index) => {
        if (index < rating) {
            star.classList.add('active');
        } else {
            star.classList.remove('active');
        }
    });

    // Actualizar texto
    const textos = {
        1: '⭐ Muy malo',
        2: '⭐⭐ Malo',
        3: '⭐⭐⭐ Regular',
        4: '⭐⭐⭐⭐ Bueno',
        5: '⭐⭐⭐⭐⭐ Excelente'
    };
    ratingText.textContent = textos[rating] || '';

    // Habilitar botón si también está marcado el checkbox
    submitBtn.disabled = !(confirmacionCheckbox.checked && currentRating > 0);
}

// Enviar validación a Firestore
async function enviarValidacion() {
    if (currentRating === 0) {
        alert('Por favor, selecciona una calificación');
        return;
    }

    const confirmacion = document.getElementById('confirmacion').checked;
    if (!confirmacion) {
        alert('Debes confirmar que el servicio fue realizado');
        return;
    }

    const comentario = document.getElementById('comentario').value.trim();
    const submitBtn = document.getElementById('submitBtn');

    submitBtn.disabled = true;
    submitBtn.textContent = 'ENVIANDO...';

    try {
        console.log('📤 Enviando validación...');

        // Actualizar documento en Firestore
        await db.collection('mantenimientos').doc(mantenimientoId).update({
            validadoPorCliente: true,
            calificacionCliente: currentRating,
            comentarioCliente: comentario || '',
            fechaValidacion: firebase.firestore.FieldValue.serverTimestamp()
        });

        console.log('✅ Validación guardada exitosamente');

        // Marcar código como usado
        if (mantenimientoData.codigoValidacion) {
            await db.collection('codigos_validacion').doc(mantenimientoData.codigoValidacion).set({
                usado: true,
                usadoEn: firebase.firestore.FieldValue.serverTimestamp(),
                mantenimientoId: mantenimientoId
            }, { merge: true });
        }

        // Actualizar estadísticas del técnico (opcional, puede hacerse con Cloud Functions)
        try {
            await actualizarEstadisticasTecnico(mantenimientoData.tecnicoPrincipalId, currentRating);
        } catch (error) {
            console.warn('No se pudieron actualizar estadísticas del técnico:', error);
            // No bloquear el flujo por esto
        }

        // Mostrar estado de éxito
        mostrarEstado('success');

    } catch (error) {
        console.error('❌ Error al enviar validación:', error);
        alert('Error al enviar la validación. Intenta nuevamente.');
        submitBtn.disabled = false;
        submitBtn.textContent = 'ENVIAR VALIDACIÓN';
    }
}

// ✅ CORREGIDO: Actualizar estadísticas del técnico
async function actualizarEstadisticasTecnico(tecnicoId, nuevaCalificacion) {
    console.log(`📊 Actualizando estadísticas del técnico ${tecnicoId} con calificación ${nuevaCalificacion}`);

    try {
        const tecnicoRef = db.collection('usuarios').doc(tecnicoId);
        const tecnicoDoc = await tecnicoRef.get();

        if (!tecnicoDoc.exists) {
            console.warn(`⚠️ Técnico ${tecnicoId} no encontrado`);
            return;
        }

        const tecnicoData = tecnicoDoc.data();
        const stats = tecnicoData.estadisticas || {};

        // ✅ CORRECCIÓN CRÍTICA: Calcular nuevas estadísticas
        const serviciosCompletadosActual = (stats.serviciosCompletados || 0);
        const calificacionPromedioActual = stats.calificacionPromedio || 0;
        const totalCalificacionesActual = stats.totalCalificaciones || 0;

        // ✅ INCREMENTAR servicios completados
        const nuevoServiciosCompletados = serviciosCompletadosActual + 1;

        // Calcular nuevo promedio de calificación
        const nuevaCalificacionPromedio =
            totalCalificacionesActual > 0
                ? ((calificacionPromedioActual * totalCalificacionesActual) + nuevaCalificacion) / (totalCalificacionesActual + 1)
                : nuevaCalificacion;

        // Calcular eficiencia (servicios completados / total servicios asignados * 100)
        const totalServiciosAsignados = stats.totalServiciosAsignados || nuevoServiciosCompletados || 1;
        const eficiencia = Math.round((nuevoServiciosCompletados / totalServiciosAsignados) * 100);

        // ✅ Actualizar Firestore con valores correctos
        await tecnicoRef.update({
            'estadisticas.serviciosCompletados': nuevoServiciosCompletados, // ✅ INCREMENTADO
            'estadisticas.calificacionPromedio': parseFloat(nuevaCalificacionPromedio.toFixed(2)),
            'estadisticas.totalCalificaciones': totalCalificacionesActual + 1,
            'estadisticas.eficiencia': eficiencia,
            'estadisticas.ultimaActualizacion': firebase.firestore.FieldValue.serverTimestamp()
        });

        console.log(`✅ Estadísticas actualizadas correctamente:`, {
            serviciosCompletadosAnterior: serviciosCompletadosActual,
            serviciosCompletadosNuevo: nuevoServiciosCompletados,
            calificacionPromedioAnterior: calificacionPromedioActual,
            calificacionPromedio: nuevaCalificacionPromedio.toFixed(2),
            totalCalificaciones: totalCalificacionesActual + 1,
            eficiencia
        });

    } catch (error) {
        console.error('❌ Error al actualizar estadísticas:', error);
        throw error;
    }
}

// Mostrar diferentes estados de la página
function mostrarEstado(estado) {
    const loading = document.getElementById('loadingState');
    const error = document.getElementById('errorState');
    const content = document.getElementById('contentState');
    const success = document.getElementById('successState');

    loading.classList.add('hidden');
    error.classList.add('hidden');
    content.classList.add('hidden');
    success.classList.add('hidden');

    switch(estado) {
        case 'loading':
            loading.classList.remove('hidden');
            break;
        case 'error':
            error.classList.remove('hidden');
            break;
        case 'content':
            content.classList.remove('hidden');
            break;
        case 'success':
            success.classList.remove('hidden');
            break;
    }
}

// Mostrar mensaje de error
function mostrarError(titulo, mensaje) {
    document.getElementById('errorTitle').textContent = titulo;
    document.getElementById('errorMessage').textContent = mensaje;
    mostrarEstado('error');
}

// Solicitar reenvío de código
function solicitarReenvio() {
    alert('Por favor, contacta al técnico para solicitar un nuevo código de validación.');
}

// Abrir imagen en modal (lightbox simple)
function abrirImagen(url) {
    window.open(url, '_blank');
}
