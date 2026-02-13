package com.iot.sensors.dto;

import lombok.Data;

@Data
public class RoomAccessRequest {
    private String rfidTag;
    private String espIp;
    private String sala;
}
