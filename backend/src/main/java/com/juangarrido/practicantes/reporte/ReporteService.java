package com.juangarrido.practicantes.reporte;

import com.juangarrido.practicantes.practicante.EstadoPracticante;
import com.juangarrido.practicantes.practicante.Practicante;
import com.juangarrido.practicantes.practicante.PracticanteRepository;
import com.juangarrido.practicantes.reporte.dto.FilaDatasetResponse;
import com.juangarrido.practicantes.reporte.dto.ResumenResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Los cálculos se hacen en memoria: el volumen de un proceso de selección es pequeño
 * y así la lógica queda en Java, legible y fácil de probar. Con miles de registros
 * convendría pasar a consultas agregadas (GROUP BY) en el repositorio.
 */
@Service
@Transactional(readOnly = true)
public class ReporteService {

    private static final String SEPARADOR_CSV = ";";

    private final PracticanteRepository repository;

    public ReporteService(PracticanteRepository repository) {
        this.repository = repository;
    }

    public ResumenResponse resumen() {
        List<Practicante> todos = repository.findAll();

        long pendientes = contar(todos, EstadoPracticante.PENDIENTE);
        long viables = contar(todos, EstadoPracticante.VIABLE);
        long noViables = contar(todos, EstadoPracticante.NO_VIABLE);
        long decididos = viables + noViables;

        Double tasaViabilidad = decididos == 0 ? null : redondear(viables * 100.0 / decididos);
        OptionalDouble promedio = todos.stream()
                .map(ReporteService::diasHastaDecision)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average();
        Double promedioDias = promedio.isPresent() ? redondear(promedio.getAsDouble()) : null;

        return new ResumenResponse(
                todos.size(),
                pendientes,
                viables,
                noViables,
                tasaViabilidad,
                promedioDias,
                agrupar(todos, Practicante::getCarrera),
                agrupar(todos, Practicante::getSemestre),
                agrupar(todos, p -> p.getFechaRegistro().toLocalDate().toString())
        );
    }

    public List<FilaDatasetResponse> dataset() {
        return repository.findAllByOrderByFechaRegistroDesc().stream()
                .map(p -> new FilaDatasetResponse(
                        p.getId(),
                        p.getCarrera(),
                        p.getSemestre(),
                        p.getEstado(),
                        p.getFechaRegistro(),
                        p.getFechaDecision(),
                        diasHastaDecision(p)))
                .toList();
    }

    /** Separador ";" porque Excel en configuración regional es-CO usa la coma como decimal. */
    public String csv() {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(SEPARADOR_CSV, "id", "nombre_completo", "correo", "carrera", "semestre",
                "estado", "observacion", "fecha_registro", "fecha_decision")).append("\r\n");

        for (Practicante p : repository.findAllByOrderByFechaRegistroDesc()) {
            csv.append(String.join(SEPARADOR_CSV,
                    String.valueOf(p.getId()),
                    escapar(p.getNombreCompleto()),
                    escapar(p.getCorreo()),
                    escapar(p.getCarrera()),
                    String.valueOf(p.getSemestre()),
                    p.getEstado().name(),
                    escapar(p.getObservacion()),
                    p.getFechaRegistro().format(formato),
                    p.getFechaDecision() == null ? "" : p.getFechaDecision().format(formato)
            )).append("\r\n");
        }
        return csv.toString();
    }

    private static long contar(List<Practicante> practicantes, EstadoPracticante estado) {
        return practicantes.stream().filter(p -> p.getEstado() == estado).count();
    }

    private static <K> Map<K, Long> agrupar(List<Practicante> practicantes, Function<Practicante, K> clave) {
        return practicantes.stream()
                .collect(Collectors.groupingBy(clave, TreeMap::new, Collectors.counting()));
    }

    private static Double diasHastaDecision(Practicante p) {
        if (p.getFechaDecision() == null) {
            return null;
        }
        long minutos = Duration.between(p.getFechaRegistro(), p.getFechaDecision()).toMinutes();
        return redondear(minutos / (60.0 * 24));
    }

    private static Double redondear(double valor) {
        return Math.round(valor * 100) / 100.0;
    }

    /** Escapa comillas y evita inyección de fórmulas en Excel (celdas que empiezan por = + - @). */
    private static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        String seguro = valor.matches("^[=+\\-@].*") ? "'" + valor : valor;
        return "\"" + seguro.replace("\"", "\"\"") + "\"";
    }
}
