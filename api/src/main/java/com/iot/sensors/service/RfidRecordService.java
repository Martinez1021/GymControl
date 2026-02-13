package com.iot.sensors.service;

import com.iot.sensors.model.GymUser;
import com.iot.sensors.model.RfidRecord;
import com.iot.sensors.repository.GymUserRepository;
import com.iot.sensors.repository.RfidRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RfidRecordService {

    private final RfidRecordRepository recordRepository;
    private final GymUserRepository userRepository;

    // --- Lógica de Control de Acceso ---


    /**
     * Lógica de Verificación (Lectura sin cambios de estado)
     * Endpoint: /api/rfid
     */
    @Transactional
    public Map<String, Object> verifyAccess(String rfidTag, String espIp) {
        Optional<GymUser> userOpt = userRepository.findByRfidTag(rfidTag);
        
        // Creamos registro de auditoría (opcional, pero recomendado)
        RfidRecord logRecord = new RfidRecord();
        logRecord.setRfidTag(rfidTag);
        logRecord.setTimestamp(LocalDateTime.now());
        logRecord.setEspIp(espIp);

        if (userOpt.isPresent()) {
            GymUser user = userOpt.get();
            logRecord.setCardHolder(user.getFullName());

            if (!user.isActive()) {
                logRecord.setAccessGranted(false);
                recordRepository.save(logRecord);
                return Map.of("accessGranted", false, "message", "Tarjeta no autorizada (Inactivo)");
            }

            String currentSala = user.getSala();
            if (currentSala != null && !currentSala.isEmpty()) {
                // YA ESTÁ EN SALA -> MODO SALIDA
                long minutes = 0;
                String timeFormatted = "0m";
                
                if (user.getLastEntryTime() != null) {
                    Duration d = Duration.between(user.getLastEntryTime(), LocalDateTime.now());
                    minutes = d.toMinutes();
                    long hours = minutes / 60;
                    long remMin = minutes % 60;
                    if(hours > 0) timeFormatted = String.format("%dh %dm", hours, remMin);
                    else timeFormatted = String.format("%dm", remMin);
                }

                logRecord.setAccessGranted(true); // Es válido leerla
                recordRepository.save(logRecord);
                
                return Map.of(
                    "accessGranted", true,
                    "cardHolder", user.getFullName() + " (SALIDA: " + timeFormatted + ")",
                    "salaActual", currentSala
                );

            } else {
                // NO ESTÁ EN SALA -> MODO ENTRADA
                logRecord.setAccessGranted(true);
                recordRepository.save(logRecord);

                return Map.of(
                    "accessGranted", true,
                    "cardHolder", user.getFullName() + " (ENTRADA)"
                );
            }
            
        } else {
            logRecord.setCardHolder("DESCONOCIDO");
            logRecord.setAccessGranted(false);
            recordRepository.save(logRecord);
            return Map.of("accessGranted", false, "message", "Tarjeta Desconocida");
        }
    }

    /**
     * Lógica de Registro de Acceso (Cambio de estado)
     * Endpoint: /api/rfid/sala
     */
    @Transactional
    public Map<String, Object> registerRoomAccess(String rfidTag, String salaDestino) {
        Optional<GymUser> userOpt = userRepository.findByRfidTag(rfidTag);
        
        if (userOpt.isPresent()) {
            GymUser user = userOpt.get();
            String currentSala = user.getSala();

            if (currentSala != null && !currentSala.isEmpty()) {
                 // Usuario ya está en una sala
                if (currentSala.equals(salaDestino)) {
                    // Está saliendo de la MISMA sala -> OK (Salir)
                    user.setSala(null);
                    user.setInside(false); // Asumimos que sale del 'estado ocupado'
                    user.setLastEntryTime(null);
                    userRepository.save(user);

                    return Map.of(
                        "accessGranted", true, 
                        "tipo", "SALIDA",
                        "sala", salaDestino
                    );
                } else {
                    // Está en OTRA sala -> ERROR
                    // Retornamos un mapa con error explícito para que el controlador lance un 403
                    return Map.of(
                        "accessGranted", false,
                        "errorType", "WRONG_ROOM",
                        "message", "Ya estas en sala " + currentSala + ". Debes salir por esa puerta primero."
                    );
                }
            } else {
                // No está en ninguna sala -> ENTRAR
                user.setSala(salaDestino);
                user.setInside(true);
                user.setLastEntryTime(LocalDateTime.now());
                userRepository.save(user);

                return Map.of(
                    "accessGranted", true, 
                    "tipo", "ENTRADA",
                    "sala", salaDestino
                );
            }
        } 
        return Map.of("accessGranted", false, "message", "Usuario no encontrado");
    }

    // --- MANTENER MÉTODOS ANTIGUOS COMO DEPRECATED O ELIMINAR ---
    // (Sobrescribimos processAccess y updateUserRoom con las nuevas versiones si hace falta, 
    // pero he creado nuevos nombres para limpiar lógica. Ahora borraré los viejos)
    
    // --- Gestión de Usuarios del Gimnasio ---
    
    public long getPeopleInsideCount() {
        return userRepository.countByInsideTrue();
    }

    public GymUser registerUser(GymUser user) {
        if (userRepository.existsByRfidTag(user.getRfidTag())) {
            throw new RuntimeException("El Tag RFID ya está registrado");
        }
        user.setRegistrationDate(LocalDateTime.now());
        return userRepository.save(user);
    }

    public List<GymUser> getAllUsers() {
        return userRepository.findAll();
    }
    
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public Optional<GymUser> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public GymUser updateUserStatus(Long id, boolean active) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setActive(active);
                    return userRepository.save(user);
                }).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    // --- Métodos existentes de Historial ---

    public List<RfidRecord> getAll() {
        return recordRepository.findAll();
    }

    public Optional<RfidRecord> getById(Long id) {
        return recordRepository.findById(id);
    }

    public void delete(Long id) {
        recordRepository.deleteById(id);
    }

    public List<RfidRecord> getByTag(String tag) {
        return recordRepository.findByRfidTag(tag);
    }

    public Optional<RfidRecord> getLatest() {
        return recordRepository.findTopByOrderByTimestampDesc();
    }

    public List<RfidRecord> getByRange(LocalDateTime start, LocalDateTime end) {
        return recordRepository.findByTimestampBetween(start, end);
    }

    public List<RfidRecord> getByAccessGranted(Boolean granted) {
        return recordRepository.findByAccessGranted(granted);
    }
}
