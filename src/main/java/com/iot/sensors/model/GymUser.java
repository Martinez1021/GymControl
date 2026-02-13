package com.iot.sensors.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "gym_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GymUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    // El Tag RFID debe ser único (no pueden haber 2 socios con la misma tarjeta)
    @Column(nullable = false, unique = true)
    private String rfidTag;

    // ¿Cuota pagada?
    private boolean active = true;

    // Control de Aforo: ¿Está dentro del gimnasio?
    // Usamos Boolean (Wrapper) para evitar errores si la BBDD devuelve NULL
    @Column(name = "user_inside")
    private Boolean inside = false;

    // Sala actual en la que se encuentra el usuario (Gimnasio, Yoga, Pilates, Spinning)
    private String sala;

    private LocalDateTime lastEntryTime;

    private LocalDateTime registrationDate = LocalDateTime.now();
}
