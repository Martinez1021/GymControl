<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/ESP32-Arduino-E7352C?style=for-the-badge&logo=arduino&logoColor=white" />
  <img src="https://img.shields.io/badge/H2-Database-0000BB?style=for-the-badge&logo=databricks&logoColor=white" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" />
</p>

<h1 align="center">🏋️ IoT Gym Access Control — Sensor API</h1>

<p align="center">
  <b>Sistema de control de acceso y monitorización ambiental para gimnasios, basado en IoT con ESP32 y API REST en Spring Boot.</b>
  <b> HECHO POR : Ernesto Martínez, Claudio Pastor, Alex Torres y Jorge Castera</b>
</p>

<p align="center">
  <a href="#-descripción">Descripción</a> •
  <a href="#-arquitectura">Arquitectura</a> •
  <a href="#-tecnologías">Tecnologías</a> •
  <a href="#-inicio-rápido">Inicio Rápido</a> •
  <a href="#-endpoints-api">API</a> •
  <a href="#-hardware">Hardware</a> •
  <a href="#-licencia">Licencia</a>
</p>

---

## 📖 Descripción

**Sensor API** es un sistema completo de **control de acceso RFID** y **monitorización ambiental** diseñado para gimnasios. Integra un backend REST con dispositivos ESP32 para ofrecer:

- ✅ **Control de acceso por tarjeta RFID** — Entrada y salida de socios con validación en tiempo real
- 🏠 **Gestión de salas** — Gimnasio, Yoga, Pilates y Spinning con control de aforo por sala
- 🌡️ **Monitorización ambiental** — Temperatura, humedad y luminosidad en tiempo real
- 🔐 **PIN de emergencia** — Acceso alternativo por teclado 4x4 con sistema de bloqueo anti-bruteforce
- 📊 **Dashboard de datos** — Historial completo de accesos y métricas de sensores

---

## 🏗️ Arquitectura

```
┌──────────────────┐        HTTP/JSON        ┌──────────────────────┐
│                  │ ◄─────────────────────► │                      │
│     ESP32        │    POST /api/sensors     │   Spring Boot API    │
│   + RFID-RC522   │    POST /api/rfid        │   (Java 17)          │
│   + DHT11        │    POST /api/rfid/sala   │                      │
│   + LCD 16x2     │                          │   ┌────────────────┐ │
│   + Keypad 4x4   │                          │   │  Controllers   │ │
│   + Servo        │                          │   │  Services      │ │
│   + Buzzer       │                          │   │  Repositories  │ │
│   + LDR          │                          │   │  H2 Database   │ │
│                  │                          │   └────────────────┘ │
└──────────────────┘                          └──────────────────────┘
```

---

## 🛠️ Tecnologías

### Backend
| Tecnología | Versión | Uso |
|:---|:---:|:---|
| **Spring Boot** | 3.2.1 | Framework principal |
| **Spring Data JPA** | — | Persistencia ORM |
| **Spring Validation** | — | Validación de datos |
| **H2 Database** | Runtime | Base de datos embebida |
| **Lombok** | 1.18.30 | Reducción de boilerplate |
| **Java** | 17 | Lenguaje principal |
| **Maven** | — | Gestión de dependencias |

### Hardware (ESP32)
| Componente | Función |
|:---|:---|
| **ESP32 DevKit** | Microcontrolador principal |
| **MFRC522** | Lector RFID (SPI) |
| **DHT11** | Sensor de temperatura y humedad |
| **LDR** | Sensor de luminosidad |
| **LCD 16x2 (I2C)** | Pantalla de información |
| **Keypad 4x4** | Entrada de PIN |
| **Servo Motor** | Control de puerta |
| **Buzzer** | Feedback auditivo |

---

## 📷 Multimedia

<img width="2048" height="1536" alt="image" src="https://github.com/user-attachments/assets/74b21103-0470-4a3c-a4ab-bed75f6053ae" />

<img width="1536" height="2048" alt="image" src="https://github.com/user-attachments/assets/478a122b-14f5-4403-b42e-64884d01e513" />




## 🚀 Inicio Rápido

### Prerrequisitos

- **Java 17+** instalado
- **Maven 3.8+** instalado
- **Git** instalado

### 1. Clonar el repositorio

```bash
git clone https://github.com/tu-usuario/sensor-api.git
cd sensor-api
```

### 2. Compilar y ejecutar la API

```bash
cd api

# Compilar el proyecto
mvn clean install

# Ejecutar la aplicación
mvn spring-boot:run
```

### 3. Verificar que funciona

```bash
# La API estará disponible en:
curl http://localhost:8080/api/sensors

# Consola H2 (explorar base de datos):
# http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:file:./data/iot_sensors_db
# User: sa | Password: password123
```

---

## 📡 Endpoints API

### 🏋️‍♂️ Gestión de Socios

| Método | Endpoint | Descripción |
|:---:|:---|:---|
| `GET` | `/api/rfid/users` | Listar todos los socios |
| `POST` | `/api/rfid/users` | Registrar nuevo socio |
| `PUT` | `/api/rfid/users/{id}/status?active=true` | Activar/Desactivar socio |
| `DELETE` | `/api/rfid/users/{id}` | Eliminar socio |
| `GET` | `/api/rfid/users/count-inside` | Personas dentro del gimnasio |

<details>
<summary>📋 Ejemplo: Registrar un socio</summary>

```bash
curl -X POST http://localhost:8080/api/rfid/users \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Juan Pérez",
    "rfidTag": "A1B2C3D4"
  }'
```

**Respuesta:**
```json
{
  "id": 1,
  "fullName": "Juan Pérez",
  "rfidTag": "A1B2C3D4",
  "active": true,
  "inside": false,
  "sala": null,
  "lastEntryTime": null,
  "registrationDate": "2026-02-13T12:00:00"
}
```
</details>

---

### 🌡️ Sensores

| Método | Endpoint | Descripción |
|:---:|:---|:---|
| `GET` | `/api/sensors` | Historial completo |
| `GET` | `/api/sensors/latest` | Último dato registrado |
| `POST` | `/api/sensors` | Guardar nuevo dato (ESP32) |
| `GET` | `/api/sensors/{id}` | Registro por ID |
| `PUT` | `/api/sensors/{id}` | Actualizar registro |
| `DELETE` | `/api/sensors/{id}` | Eliminar registro |
| `GET` | `/api/sensors/range?start=...&end=...` | Filtrar por rango de fechas |
| `GET` | `/api/sensors/recent?minutes=30` | Datos recientes |
| `GET` | `/api/sensors/average/temperature?since=...` | Temperatura promedio |
| `GET` | `/api/sensors/average/humidity?since=...` | Humedad promedio |

<details>
<summary>📋 Ejemplo: Enviar datos de sensores</summary>

```bash
curl -X POST http://localhost:8080/api/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "temperatura": 24.5,
    "humedad": 58.0,
    "luminosidad": 320.0,
    "batteryVoltage": 3.7,
    "rssi": -65,
    "uptime": 1205,
    "status": "OK"
  }'
```

**Respuesta:**
```json
{
  "id": 1,
  "temperatura": 24.5,
  "humedad": 58.0,
  "luminosidad": 320.0,
  "batteryVoltage": 3.7,
  "rssi": -65,
  "uptime": 1205,
  "status": "OK",
  "espIp": null,
  "timestamp": "2026-02-13T12:00:00"
}
```
</details>

---

### 🔐 Control de Acceso RFID

| Método | Endpoint | Descripción |
|:---:|:---|:---|
| `POST` | `/api/rfid` | Verificar acceso (ESP32) |
| `POST` | `/api/rfid/sala` | Registrar entrada/salida en sala |
| `GET` | `/api/rfid` | Historial de accesos |
| `GET` | `/api/rfid/latest` | Último acceso |
| `GET` | `/api/rfid/tag/{tag}` | Accesos por tarjeta |
| `GET` | `/api/rfid/access/{true\|false}` | Filtrar por resultado |
| `GET` | `/api/rfid/range?start=...&end=...` | Filtrar por fechas |
| `DELETE` | `/api/rfid/{id}` | Eliminar registro |

<details>
<summary>📋 Ejemplo: Flujo completo de acceso</summary>

```bash
# 1. Verificar tarjeta RFID (el ESP32 envía esto automáticamente)
curl -X POST http://localhost:8080/api/rfid \
  -H "Content-Type: application/json" \
  -d '{"rfidTag": "A1B2C3D4", "espIp": "192.168.1.248"}'

# Respuesta si es ENTRADA:
# {"accessGranted": true, "cardHolder": "Juan Pérez (ENTRADA)"}

# 2. Registrar en sala (después de seleccionar sala en el teclado)
curl -X POST http://localhost:8080/api/rfid/sala \
  -H "Content-Type: application/json" \
  -d '{"rfidTag": "A1B2C3D4", "espIp": "192.168.1.248", "sala": "Gimnasio"}'

# Respuesta: {"accessGranted": true, "tipo": "ENTRADA", "sala": "Gimnasio"}
```
</details>

---

## 🔌 Hardware

### Esquema de conexiones ESP32

```
ESP32 Pin       Componente          Función
─────────       ──────────          ────────
GPIO 21  ───►   MFRC522 (SDA)      Lector RFID
GPIO 22  ───►   MFRC522 (RST)      Reset RFID
GPIO 4   ───►   DHT11 (Data)       Temperatura/Humedad
GPIO 5   ───►   LDR (Digital)      Sensor de luz
GPIO 13  ───►   LCD (SDA)          Pantalla I2C
GPIO 12  ───►   LCD (SCL)          Pantalla I2C
GPIO 15  ───►   Servo (Signal)     Motor puerta
GPIO 2   ───►   Buzzer             Feedback sonoro
GPIO 14  ───►   Keypad Row 1       ┐
GPIO 27  ───►   Keypad Row 2       │ Teclado
GPIO 26  ───►   Keypad Row 3       │ 4x4
GPIO 25  ───►   Keypad Row 4       ┘
GPIO 33  ───►   Keypad Col 1       ┐
GPIO 32  ───►   Keypad Col 2       │ Teclado
GPIO 17  ───►   Keypad Col 3       │ 4x4
GPIO 16  ───►   Keypad Col 4       ┘
```

### Configurar el ESP32

1. Abre `firmware/ESP32_Example.ino` en el **Arduino IDE**
2. Configura tu red WiFi y la IP del servidor:
   ```cpp
   const char* ssid = "TU_RED_WIFI";
   const char* password = "TU_PASSWORD";
   const char* serverAddress = "http://IP_DEL_SERVIDOR:8080";
   ```
3. Instala las librerías necesarias:
   - `MFRC522` — Lector RFID
   - `DHT sensor library` — Sensor DHT11
   - `LiquidCrystal_PCF8574` — Pantalla LCD I2C
   - `Keypad` — Teclado matricial
   - `ESP32Servo` — Control de servomotor
4. Sube el firmware al ESP32

---

## 📁 Estructura del Proyecto

```
sensor-api/
├── api/                                         # 🖥️ Backend Spring Boot
│   ├── src/
│   │   └── main/
│   │       ├── java/com/iot/sensors/
│   │       │   ├── SensorApiApplication.java       # Punto de entrada
│   │       │   ├── controller/
│   │       │   │   ├── RfidRecordController.java   # Endpoints RFID + Usuarios
│   │       │   │   └── SensorDataController.java   # Endpoints Sensores
│   │       │   ├── dto/
│   │       │   │   └── RoomAccessRequest.java       # DTO acceso por sala
│   │       │   ├── model/
│   │       │   │   ├── GymUser.java                 # Entidad Socio
│   │       │   │   ├── RfidRecord.java              # Entidad Registro RFID
│   │       │   │   └── SensorData.java              # Entidad Datos Sensor
│   │       │   ├── repository/
│   │       │   │   ├── GymUserRepository.java       # Repo Socios
│   │       │   │   ├── RfidRecordRepository.java    # Repo RFID
│   │       │   │   └── SensorDataRepository.java    # Repo Sensores
│   │       │   └── service/
│   │       │       ├── RfidRecordService.java       # Lógica acceso + salas
│   │       │       └── SensorDataService.java       # Lógica sensores
│   │       └── resources/
│   │           └── application.properties           # Configuración
│   ├── data/                                        # Base de datos H2
│   └── pom.xml                                      # Dependencias Maven
│
├── firmware/                                    # 🔌 Firmware ESP32
│   └── ESP32_Example.ino                           # Código Arduino
│
├── .gitignore
├── LICENSE
└── README.md
```

---

## ⚙️ Configuración

### Base de Datos (H2 Console)

| Parámetro | Valor |
|:---|:---|
| URL Console | `http://localhost:8080/h2-console` |
| JDBC URL | `jdbc:h2:file:./data/iot_sensors_db` |
| Usuario | `sa` |
| Password | `password123` |

### Acceso Remoto

Para acceder desde fuera de tu red local (ej. para conectar un ESP32 remoto):

```bash
# Usando VS Code Dev Tunnels
# Reemplaza localhost:8080 por tu URL pública:
# https://xxxx.devtunnels.ms/api/sensors
```

---

## 🧪 Pruebas Rápidas

Puedes probar la API sin un ESP32 usando `curl` o PowerShell:

```powershell
# PowerShell — Enviar datos de sensor simulados
Invoke-RestMethod -Uri "http://localhost:8080/api/sensors" `
  -Method Post -ContentType "application/json" `
  -Body '{"temperatura": 22.0, "humedad": 50.0, "luminosidad": 100.0, "status": "TEST"}'

# PowerShell — Registrar un socio
Invoke-RestMethod -Uri "http://localhost:8080/api/rfid/users" `
  -Method Post -ContentType "application/json" `
  -Body '{"fullName": "Test User", "rfidTag": "AABBCCDD"}'
```

---

## 🤝 Contribuir

1. Fork del repositorio
2. Crea tu feature branch (`git checkout -b feature/nueva-funcion`)
3. Commit de tus cambios (`git commit -m 'feat: añadir nueva función'`)
4. Push a la branch (`git push origin feature/nueva-funcion`)
5. Abre un Pull Request

---

## 📄 Licencia

Este proyecto está bajo la licencia **MIT**. Consulta el archivo [LICENSE](LICENSE) para más detalles.

---

<p align="center">
  Desarrollado con ❤️ usando <b>Spring Boot</b> + <b>ESP32</b>
</p>
