package com.iot.sensors.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "rfid_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RfidRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String rfidTag;

    private String cardHolder;

    private Boolean accessGranted;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    private String espIp = "192.168.1.248";

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
