package com.iot.sensors.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double temperatura;

    @Column(nullable = false)
    private Double humedad;

    @Column(nullable = false)
    private Double luminosidad;

    // --- Nuevas Métricas IoT Avanzadas ---
    private Double batteryVoltage; // Voltaje (ej. 3.3v)
    private Integer rssi;          // Calidad WiFi (ej. -60 dBm)
    private Long uptime;           // Segundos encendido
    private String status;         // "OK", "WARNING", "CRITICAL"
    
    private String espIp = "192.168.1.248";

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (espIp == null || espIp.isEmpty()) {
            espIp = "192.168.1.248";
        }
    }
}
