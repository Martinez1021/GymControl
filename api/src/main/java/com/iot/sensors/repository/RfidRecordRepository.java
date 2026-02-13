package com.iot.sensors.repository;

import com.iot.sensors.model.RfidRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RfidRecordRepository extends JpaRepository<RfidRecord, Long> {

    // Buscar por Tag
    List<RfidRecord> findByRfidTag(String rfidTag);

    // Último registro
    Optional<RfidRecord> findTopByOrderByTimestampDesc();

    // Rango de fechas
    List<RfidRecord> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // Filtrar por acceso concedido/denegado
    List<RfidRecord> findByAccessGranted(Boolean accessGranted);
}
