package com.iot.sensors.controller;

import com.iot.sensors.model.GymUser;
import com.iot.sensors.model.RfidRecord;
import com.iot.sensors.dto.RoomAccessRequest;
import com.iot.sensors.service.RfidRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rfid")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class RfidRecordController {

    private final RfidRecordService service;

    // --- ENDPOINTS DE CONTROL DE ACCESO (ESP32) ---

    @PostMapping
    public ResponseEntity<?> processAccess(@RequestBody RfidRecord record) {
        // Usamos la nueva lógica de verificación sin cambios de estado (solo lectura)
        Map<String, Object> response = service.verifyAccess(record.getRfidTag(), record.getEspIp());
        
        String logStatus = Boolean.TRUE.equals(response.get("accessGranted")) ? "OK" : "DENEGADO";
        log.info("🔐 RFID CHECK ({}): Tag={} | Resp={}", record.getEspIp(), record.getRfidTag(), response);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sala")
    public ResponseEntity<?> processRoomAccess(@RequestBody RoomAccessRequest request) {
        // Usamos la nueva lógica de registro de sala (cambio de estado)
        Map<String, Object> response = service.registerRoomAccess(request.getRfidTag(), request.getSala());
        
        log.info("📍 ACCESO SALA: {} | Tag: {} | Resp: {}", request.getSala(), request.getRfidTag(), response);
        
        // Si el servicio indica error de sala incorrecta, devolvemos 403
        if (Boolean.FALSE.equals(response.get("accessGranted")) && response.containsKey("errorType")) {
            return ResponseEntity.status(403).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    // --- ENDPOINTS DE GESTIÓN DE USUARIOS DEL GIMNASIO ---

    @PostMapping("/users")
    public ResponseEntity<GymUser> registerUser(@RequestBody GymUser user) {
        return ResponseEntity.ok(service.registerUser(user));
    }

    @GetMapping("/users")
    public ResponseEntity<List<GymUser>> getAllUsers() {
        return ResponseEntity.ok(service.getAllUsers());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        service.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<GymUser> updateUserStatus(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(service.updateUserStatus(id, active));
    }

    @GetMapping("/users/count-inside")
    public ResponseEntity<Long> getPeopleInsideCount() {
        return ResponseEntity.ok(service.getPeopleInsideCount());
    }

    // --- HISTORIAL ---

    @GetMapping
    public ResponseEntity<List<RfidRecord>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RfidRecord> getById(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (service.getById(id).isPresent()) {
            service.delete(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/tag/{rfidTag}")
    public ResponseEntity<List<RfidRecord>> getByTag(@PathVariable String rfidTag) {
        List<RfidRecord> records = service.getByTag(rfidTag);
        return !records.isEmpty() ? ResponseEntity.ok(records) : ResponseEntity.notFound().build();
    }

    @GetMapping("/latest")
    public ResponseEntity<RfidRecord> getLatest() {
        return service.getLatest()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/range")
    public ResponseEntity<List<RfidRecord>> getByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(service.getByRange(start, end));
    }

    @GetMapping("/access/{granted}")
    public ResponseEntity<List<RfidRecord>> getByAccessGranted(@PathVariable Boolean granted) {
        return ResponseEntity.ok(service.getByAccessGranted(granted));
    }
}
