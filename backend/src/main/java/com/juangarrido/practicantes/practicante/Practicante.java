package com.juangarrido.practicantes.practicante;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "practicante")
public class Practicante {

    /** El esquema lo define schema.sql (id INTEGER PRIMARY KEY AUTOINCREMENT), no Hibernate. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombreCompleto;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Column(nullable = false, length = 120)
    private String carrera;

    @Column(nullable = false)
    private Integer semestre;

    @Column(nullable = false)
    private String rutaHojaVida;

    @Column(nullable = false)
    private String nombreOriginalHojaVida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPracticante estado;

    @Column(length = 500)
    private String observacion;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    private LocalDateTime fechaDecision;

    protected Practicante() {
        // requerido por JPA
    }

    public Practicante(String nombreCompleto, String correo, String carrera, Integer semestre,
                       String rutaHojaVida, String nombreOriginalHojaVida) {
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.carrera = carrera;
        this.semestre = semestre;
        this.rutaHojaVida = rutaHojaVida;
        this.nombreOriginalHojaVida = nombreOriginalHojaVida;
        this.estado = EstadoPracticante.PENDIENTE;
    }

    @PrePersist
    void alCrear() {
        this.fechaRegistro = LocalDateTime.now();
    }

    /**
     * La regla vive en la entidad para que no se pueda dejar un practicante
     * decidido sin fecha de decisión, ni devolverlo a PENDIENTE.
     */
    public void cambiarEstado(EstadoPracticante nuevoEstado, String observacion) {
        if (nuevoEstado == null || nuevoEstado == EstadoPracticante.PENDIENTE) {
            throw new CambioEstadoInvalidoException("El estado debe ser VIABLE o NO_VIABLE");
        }
        this.estado = nuevoEstado;
        this.observacion = observacion;
        this.fechaDecision = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getCorreo() { return correo; }
    public String getCarrera() { return carrera; }
    public Integer getSemestre() { return semestre; }
    public String getRutaHojaVida() { return rutaHojaVida; }
    public String getNombreOriginalHojaVida() { return nombreOriginalHojaVida; }
    public EstadoPracticante getEstado() { return estado; }
    public String getObservacion() { return observacion; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public LocalDateTime getFechaDecision() { return fechaDecision; }
}
