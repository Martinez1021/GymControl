package com.iot.sensors.repository;

import com.iot.sensors.model.SensorData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    // Última lectura
    Optional<SensorData> findTopByOrderByTimestampDesc();

    // Rango de fechas
    List<SensorData> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // Datos posteriores a una fecha
    List<SensorData> findByTimestampAfter(LocalDateTime timestamp);

    // Temperatura promedio desde una fecha
    @Query("SELECT AVG(s.temperatura) FROM SensorData s WHERE s.timestamp >= :since")
    Double findAverageTemperatureSince(LocalDateTime since);

    // Humedad promedio desde una fecha
    @Query("SELECT AVG(s.humedad) FROM SensorData s WHERE s.timestamp >= :since")
    Double findAverageHumiditySince(LocalDateTime since);
}
