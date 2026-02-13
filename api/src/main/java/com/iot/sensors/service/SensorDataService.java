package com.iot.sensors.service;

import com.iot.sensors.model.SensorData;
import com.iot.sensors.repository.SensorDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SensorDataService {

    private final SensorDataRepository repository;

    public List<SensorData> getAll() {
        return repository.findAll();
    }

    public Optional<SensorData> getById(Long id) {
        return repository.findById(id);
    }

    public SensorData save(SensorData data) {
        return repository.save(data);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Optional<SensorData> getLatest() {
        return repository.findTopByOrderByTimestampDesc();
    }

    public List<SensorData> getByRange(LocalDateTime start, LocalDateTime end) {
        return repository.findByTimestampBetween(start, end);
    }

    public List<SensorData> getRecent(int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return repository.findByTimestampAfter(since);
    }

    public Double getAverageTemperature(LocalDateTime since) {
        return repository.findAverageTemperatureSince(since);
    }

    public Double getAverageHumidity(LocalDateTime since) {
        return repository.findAverageHumiditySince(since);
    }
}
