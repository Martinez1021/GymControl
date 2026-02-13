package com.iot.sensors.controller;

import com.iot.sensors.model.SensorData;
import com.iot.sensors.service.SensorDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sensors")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SensorDataController {

    private final SensorDataService service;

    @GetMapping
    public ResponseEntity<List<SensorData>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SensorData> getById(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<SensorData> create(@RequestBody SensorData data) {
        System.out.println("📡 RECIBIDO DATOS DE (" + data.getEspIp() + "): " + data);
        return ResponseEntity.ok(service.save(data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SensorData> update(@PathVariable Long id, @RequestBody SensorData data) {
        return service.getById(id)
                .map(existing -> {
                    data.setId(id);
                    data.setTimestamp(existing.getTimestamp()); // Mantener timestamp original
                    return ResponseEntity.ok(service.save(data));
                })
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

    @GetMapping("/latest")
    public ResponseEntity<SensorData> getLatest() {
        return service.getLatest()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/range")
    public ResponseEntity<List<SensorData>> getByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(service.getByRange(start, end));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<SensorData>> getRecent(@RequestParam(defaultValue = "30") int minutes) {
        return ResponseEntity.ok(service.getRecent(minutes));
    }

    @GetMapping("/average/temperature")
    public ResponseEntity<Double> getAverageTemperature(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        Double avg = service.getAverageTemperature(since);
        return avg != null ? ResponseEntity.ok(avg) : ResponseEntity.noContent().build();
    }

    @GetMapping("/average/humidity")
    public ResponseEntity<Double> getAverageHumidity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        Double avg = service.getAverageHumidity(since);
        return avg != null ? ResponseEntity.ok(avg) : ResponseEntity.noContent().build();
    }
}
