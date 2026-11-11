package com.juangarrido.practicantes.practicante;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PracticanteRepository extends JpaRepository<Practicante, Long> {

    boolean existsByCorreoIgnoreCase(String correo);

    List<Practicante> findAllByOrderByFechaRegistroDesc();

    /**
     * Filtros opcionales: un parámetro null no filtra.
     * - "member of" (subconsulta) en lugar de un join, para que el practicante conserve
     *   todas sus habilidades en la respuesta y no solo la filtrada.
     * - @EntityGraph trae las habilidades en la misma consulta y evita una consulta extra
     *   por cada practicante de la lista (problema N+1).
     */
    @EntityGraph(attributePaths = "habilidadesDetectadas")
    @Query("""
            select p from Practicante p
            where (:estado is null or p.estado = :estado)
              and (:habilidad is null or :habilidad member of p.habilidadesDetectadas)
            order by p.fechaRegistro desc
            """)
    List<Practicante> buscar(@Param("estado") EstadoPracticante estado, @Param("habilidad") String habilidad);
}
