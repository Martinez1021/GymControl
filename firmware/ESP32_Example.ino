#include <SPI.h>
#include <MFRC522.h>
#include <WiFi.h>
#include <HTTPClient.h> 
#include "DHT.h"
#include <Wire.h>
#include <LiquidCrystal_PCF8574.h> 
#include <Keypad.h>
#include <ESP32Servo.h>

// ==========================================
// CONFIGURACIÓN DE RED Y SERVIDOR
// ==========================================
const char* ssid = "proyectoDAM";
const char* password = "20260108";
const char* serverAddress = "http://192.168.1.247:8080"; 

IPAddress staticIP(192, 168, 1, 248);
IPAddress gateway(192, 168, 1, 1);
IPAddress subnet(255, 255, 255, 0);
IPAddress primaryDNS(192, 168, 1, 1);
IPAddress secondaryDNS(8, 8, 8, 8);

// ==========================================
// PINES Y HARDWARE
// ==========================================
#define SS_PIN  21   
#define RST_PIN 22   
#define DHTPIN 4
#define DHTTYPE DHT11
#define LDRPIN 5
#define I2C_SDA 13
#define I2C_SCL 12
#define SERVO_PIN 15
#define BUZZER_PIN 2

// ==========================================
// TECLADO 4x4
// ==========================================
const byte ROWS = 4, COLS = 4;

char keys[ROWS][COLS] = {
  {'D','C','B','A'},
  {'#','9','6','3'},
  {'0','8','5','2'},
  {'*','7','4','1'}
};

byte rowPins[ROWS] = {14, 27, 26, 25};
byte colPins[COLS] = {33, 32, 17, 16};
Keypad keypad = Keypad(makeKeymap(keys), rowPins, colPins, ROWS, COLS);

MFRC522 mfrc522(SS_PIN, RST_PIN);
DHT dht(DHTPIN, DHTTYPE);
LiquidCrystal_PCF8574 lcd(0x27); 
Servo servoMotor;

unsigned long ultimaLecturaSensores = 0;
// AUMENTADO: Enviar sensores cada 15s en vez de 5s para no saturar la red
const long intervaloSensores = 15000; 
bool mostrandoMensajeRFID = false;

// ==========================================
// SISTEMA PIN Y SALAS
// ==========================================
String pinMaestro = "1234";
String pinActual = "";
int intentosFallidos = 0;
unsigned long tiempoBloqueo = 0;
bool sistemaBloqueado = false;
bool modoPINactivo = false;

// Sistema de salas
bool esperandoSala = false;
String uidActual = "";
String nombreActual = "";
bool esEntrada = false;  // NUEVO: detectar si es entrada o salida
const char* salas[] = {"Gimnasio", "Yoga", "Pilates", "Spinning"};
unsigned long tiempoEsperaSala = 0;
unsigned long tiempoScrollSala = 0;
int indiceSalaScroll = 0;

// ==========================================
// FUNCIONES AUXILIARES
// ==========================================
void beepCorto() {
  tone(BUZZER_PIN, 2000, 100);
  delay(120);
}

void beepError() {
  for(int i = 0; i < 3; i++) {
    tone(BUZZER_PIN, 1500, 150);
    delay(250);
  }
}

void abrirPuerta() {
  Serial.println("🚪 Abriendo puerta...");
  servoMotor.write(90);
  delay(3000);
  servoMotor.write(0);
  Serial.println("🚪 Puerta cerrada");
}

void enviarAccesoConSala(String uid, int numeroSala) {
  if (WiFi.status() != WL_CONNECTED) return;
  
  HTTPClient http;
  // AUMENTADO: Timeout para evitar error -11 si la red va lenta
  http.setConnectTimeout(15000); 
  http.setTimeout(15000);
  
  String url = String(serverAddress) + "/api/rfid/sala";
  
  String json = "{";
  json += "\"rfidTag\":\"" + uid + "\",";
  json += "\"espIp\":\"" + WiFi.localIP().toString() + "\",";
  json += "\"sala\":\"" + String(salas[numeroSala - 1]) + "\"";
  json += "}";
  
  Serial.println("📡 Enviando: " + json);
  
  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  int httpCode = http.POST(json);
  
  if (httpCode == 200) {
    Serial.println("✅ Sala registrada en BD");
  } else {
    Serial.println("❌ Error al registrar sala: " + String(httpCode));
  }
  
  http.end();
}

void mostrarMenuSalas() {
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Elige sala 1-4:");
  
  indiceSalaScroll = 0;
  tiempoScrollSala = millis();
  actualizarScrollSalas();
  
  Serial.println("\n=== SELECCIONA SALA ===");
  Serial.println("1 - Gimnasio");
  Serial.println("2 - Yoga");
  Serial.println("3 - Pilates");
  Serial.println("4 - Spinning");
  Serial.println("* - Cancelar");
}

void actualizarScrollSalas() {
  lcd.setCursor(0, 1);
  
  // Mostrar sala actual rotando
  String texto = String(indiceSalaScroll + 1) + ":" + String(salas[indiceSalaScroll]);
  
  // Rellenar con espacios
  while(texto.length() < 16) {
    texto += " ";
  }
  
  lcd.print(texto);
}

void procesarSeleccionSala(char tecla) {
  if (tecla >= '1' && tecla <= '4') {
    int sala = tecla - '0';
    
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Sala: ");
    lcd.print(salas[sala - 1]);
    lcd.setCursor(0, 1);
    lcd.print("Registrado!");
    
    beepCorto();
    delay(500);
    beepCorto();
    
    Serial.println("✅ Sala seleccionada: " + String(salas[sala - 1]));
    
    // Enviar al servidor
    enviarAccesoConSala(uidActual, sala);
    
    // Abrir puerta
    abrirPuerta();
    
    esperandoSala = false;
    uidActual = "";
    nombreActual = "";
    
    delay(2000);
    lcd.clear();
    
  } else if (tecla == '*') {
    lcd.clear();
    lcd.print("Cancelado");
    beepError();
    esperandoSala = false;
    uidActual = "";
    delay(1500);
    lcd.clear();
  }
}

void setup() {
  Serial.begin(115200);
  delay(1000);

  pinMode(LDRPIN, INPUT);
  pinMode(BUZZER_PIN, OUTPUT);

  servoMotor.attach(SERVO_PIN);
  servoMotor.write(0);

  Wire.begin(I2C_SDA, I2C_SCL);
  lcd.begin(16, 2);
  lcd.setBacklight(255);
  
  lcd.clear();
  lcd.print("Iniciando...");

  Serial.println("\n=== SISTEMA GIMNASIO ===");
  
  WiFi.begin(ssid, password);
  WiFi.config(staticIP, gateway, subnet, primaryDNS, secondaryDNS);

  int intentos = 0;
  while (WiFi.status() != WL_CONNECTED && intentos < 20) {
    delay(500);
    Serial.print(".");
    intentos++;
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\n✓ WiFi OK: " + WiFi.localIP().toString());
    lcd.clear();
    lcd.print("WiFi OK");
    lcd.setCursor(0, 1);
    lcd.print(WiFi.localIP());
    beepCorto();
    delay(2000);
  } else {
    Serial.println("\n✗ Error WiFi");
    lcd.clear();
    lcd.print("Error WiFi");
    beepError();
    delay(2000);
  }

  dht.begin();
  SPI.begin();
  mfrc522.PCD_Init();
  
  lcd.clear();
  Serial.println("✓ Sistema listo");
  beepCorto();
}

void loop() {
  unsigned long tiempoActual = millis();

  // ==========================================
  // SCROLL AUTOMÁTICO DEL MENÚ DE SALAS
  // ==========================================
  if (esperandoSala && (tiempoActual - tiempoScrollSala > 2000)) {
    tiempoScrollSala = tiempoActual;
    indiceSalaScroll = (indiceSalaScroll + 1) % 4;  // Rotar 0-3
    actualizarScrollSalas();
  }

  // ==========================================
  // TIMEOUT SELECCIÓN DE SALA (30 segundos)
  // ==========================================
  if (esperandoSala && (tiempoActual - tiempoEsperaSala > 30000)) {
    lcd.clear();
    lcd.print("Tiempo agotado");
    beepError();
    esperandoSala = false;
    uidActual = "";
    delay(2000);
    lcd.clear();
  }

  // ==========================================
  // VERIFICAR BLOQUEO
  // ==========================================
  if (sistemaBloqueado) {
    if (tiempoActual - tiempoBloqueo < 60000) {
      lcd.setCursor(0, 0);
      lcd.print("BLOQUEADO       ");
      lcd.setCursor(0, 1);
      int segsRestantes = (60000 - (tiempoActual - tiempoBloqueo)) / 1000;
      lcd.print("Espera ");
      lcd.print(segsRestantes);
      lcd.print("s   ");
      delay(1000);
      return;
    } else {
      sistemaBloqueado = false;
      intentosFallidos = 0;
      lcd.clear();
      Serial.println("✓ Desbloqueado");
      beepCorto();
    }
  }

  // ==========================================
  // LEER TECLADO
  // ==========================================
  char key = keypad.getKey();
  if (key) {
    beepCorto();
    
    if (esperandoSala) {
      procesarSeleccionSala(key);
      return;
    }
    
    procesarTeclado(key);
  }

  // ==========================================
  // MOSTRAR SENSORES
  // ==========================================
  if (!mostrandoMensajeRFID && !modoPINactivo && !esperandoSala &&
      (tiempoActual - ultimaLecturaSensores >= intervaloSensores)) {
    ultimaLecturaSensores = tiempoActual;
    
    float h = dht.readHumidity();
    float t = dht.readTemperature();
    int luz = digitalRead(LDRPIN);
    
    if (!isnan(h) && !isnan(t)) {
      lcd.setCursor(0, 0);
      lcd.print("T:"); 
      lcd.print(t, 1); 
      lcd.print("C H:"); 
      lcd.print(h, 0); 
      lcd.print("%  "); 
      
      lcd.setCursor(0, 1);
      if(luz == LOW) lcd.print("Luz:SI PIN:[A]  ");
      else           lcd.print("Luz:NO PIN:[A]  ");

      if(WiFi.status() == WL_CONNECTED) {
        sendSensorData(t, h, luz);
      }
    }
  }

  // ==========================================
  // LECTURA RFID
  // ==========================================
  if (mfrc522.PICC_IsNewCardPresent() && mfrc522.PICC_ReadCardSerial()) {
    beepCorto();
    
    if (modoPINactivo) {
      modoPINactivo = false;
      pinActual = "";
    }
    
    mostrandoMensajeRFID = true;
    
    String uid = "";
    for (byte i = 0; i < mfrc522.uid.size; i++) {
       if(mfrc522.uid.uidByte[i] < 0x10) uid += "0";
       uid += String(mfrc522.uid.uidByte[i], HEX);
    }
    uid.toUpperCase();

    Serial.println("\n🔐 TAG: " + uid);
    lcd.clear();
    lcd.print("Verificando...");

    if(WiFi.status() == WL_CONNECTED) {
      checkAccess(uid);
    } else {
      lcd.clear();
      lcd.print("Sin red");
      beepError();
    }

    mfrc522.PICC_HaltA();
    mfrc522.PCD_StopCrypto1();
    
    delay(2000);
    mostrandoMensajeRFID = false;
    
    if (!esperandoSala) {
      lcd.clear();
    }
  }
}

void procesarTeclado(char key) {
  Serial.print("Tecla: ");
  Serial.println(key);
  
  if (key == 'A') {
    modoPINactivo = true;
    pinActual = "";
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("PIN emergencia:");
    lcd.setCursor(0, 1);
    Serial.println("🔑 Modo PIN activado");
    
  } else if (key == '*') {
    if (modoPINactivo) {
      modoPINactivo = false;
      pinActual = "";
      lcd.clear();
      lcd.print("PIN cancelado");
      Serial.println("❌ Cancelado");
      delay(1000);
      lcd.clear();
    }
    
  } else if (key == '#') {
    if (modoPINactivo && pinActual.length() > 0) {
      verificarPIN();
    }
    
  } else if (key >= '0' && key <= '9') {
    if (modoPINactivo) {
      pinActual += key;
      
      lcd.setCursor(0, 1);
      lcd.print("                ");
      lcd.setCursor(0, 1);
      for (int i = 0; i < pinActual.length(); i++) {
        lcd.print("*");
      }
      
      if (pinActual.length() == 4) {
        delay(300);
        verificarPIN();
      }
    }
  }
}

void verificarPIN() {
  Serial.println("🔍 Verificando PIN...");
  
  if (pinActual == pinMaestro) {
    Serial.println("✅ PIN CORRECTO");
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("PIN CORRECTO");
    
    beepCorto();
    delay(500);
    
    intentosFallidos = 0;
    
    // PIN siempre cuenta como ENTRADA
    uidActual = "PIN_EMERGENCY";
    nombreActual = "Admin";
    esEntrada = true;
    esperandoSala = true;
    tiempoEsperaSala = millis();
    mostrarMenuSalas();
    
  } else {
    intentosFallidos++;
    Serial.println("❌ PIN INCORRECTO - Intento " + String(intentosFallidos) + "/3");
    
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("PIN INCORRECTO");
    lcd.setCursor(0, 1);
    lcd.print("Intento ");
    lcd.print(intentosFallidos);
    lcd.print("/3");
    
    beepError();
    
    if (intentosFallidos >= 3) {
      sistemaBloqueado = true;
      tiempoBloqueo = millis();
      Serial.println("⚠️ BLOQUEADO 60s");
    }
    
    delay(2000);
  }
  
  pinActual = "";
  modoPINactivo = false;
  
  if (!esperandoSala) {
    lcd.clear();
  }
}

void checkAccess(String uid) {
  HTTPClient http;
  // AUMENTADO: Timeout a 15000ms para evitar error -11
  http.setConnectTimeout(15000); 
  http.setTimeout(15000);

  String url = String(serverAddress) + "/api/rfid";
  String json = "{\"rfidTag\":\"" + uid + "\", \"espIp\":\"" + WiFi.localIP().toString() + "\"}";

  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  
  int httpCode = http.POST(json);
  
  if (httpCode == 200) {
    String payload = http.getString();
    
    if (payload.indexOf("\"accessGranted\":true") >= 0) {
      String token = "\"cardHolder\":\"";
      int start = payload.indexOf(token);
      String InfoCompleta = "Socio";
      
      if (start != -1) {
        start += token.length();
        int end = payload.indexOf("\"", start);
        if (end != -1) {
          InfoCompleta = payload.substring(start, end);
        }
      }

      Serial.println("✅ " + InfoCompleta);
      
      uidActual = uid;
      nombreActual = InfoCompleta;
      
      lcd.clear();
      lcd.setCursor(0, 0);

      // DETECTAR SI ES ENTRADA O SALIDA
      if (InfoCompleta.indexOf("(ENTRADA)") >= 0) {
         // ES ENTRADA - Pedir sala
         esEntrada = true;
         
         String nombre = InfoCompleta;
         nombre.replace(" (ENTRADA)", "");
         lcd.print("Hola " + nombre.substring(0, 11));
         lcd.setCursor(0, 1);
         lcd.print(">> ENTRANDO >>");
         
         beepCorto();
         delay(2000);
         
         // MOSTRAR MENÚ DE SALAS
         esperandoSala = true;
         tiempoEsperaSala = millis();
         mostrarMenuSalas();
         
      } else if (InfoCompleta.indexOf("(SALIDA") >= 0) {
         // ES SALIDA - NO pedir sala, abrir directamente
         esEntrada = false;
         
         int parentesis = InfoCompleta.indexOf("(");
         String nombre = InfoCompleta.substring(0, parentesis);
         String tiempo = InfoCompleta.substring(parentesis);
         tiempo.replace("(SALIDA: ", "");
         tiempo.replace(")", "");
         
         if (tiempo == "SALIDA") tiempo = "";
         
         lcd.print("Adios " + nombre.substring(0, 10));
         lcd.setCursor(0, 1);
         if (tiempo.length() > 0) lcd.print("Tiempo: " + tiempo);
         else lcd.print("<< SALIENDO <<");
         
         beepCorto();
         delay(500);
         beepCorto();
         
         // ABRIR PUERTA DIRECTAMENTE SIN PEDIR SALA
         abrirPuerta();
         
         delay(2000);
         lcd.clear();
      }
      else {
         // Caso genérico (por si acaso)
         lcd.print("Bienvenido");
         lcd.setCursor(0, 1);
         lcd.print(InfoCompleta.substring(0, 16));
         beepCorto();
         delay(2000);
         abrirPuerta();
         lcd.clear();
      }
      
    } else {
      Serial.println("⛔ DENEGADO");
      lcd.clear();
      lcd.setCursor(0, 0);
      lcd.print("ACCESO DENEGADO");
      beepError();
      delay(2000);
      lcd.clear();
    }
  } else {
    lcd.clear();
    lcd.print("Error API: " + String(httpCode));
    beepError();
    delay(2000);
    lcd.clear();
  }
  
  http.end();
}

void sendSensorData(float temp, float hum, int ldrStatus) {
  HTTPClient http;
  
  // AUMENTADO: Timeout para sensores también
  http.setConnectTimeout(15000); 
  http.setTimeout(15000);
  
  String url = String(serverAddress) + "/api/sensors";
  float lux = (ldrStatus == LOW) ? 800.0 : 50.0;
  
  String json = "{";
  json += "\"temperatura\":" + String(temp, 1) + ",";
  json += "\"humedad\":" + String(hum, 1) + ",";
  json += "\"luminosidad\":" + String(lux, 1) + ",";
  json += "\"batteryVoltage\":3.3,";
  json += "\"rssi\":" + String(WiFi.RSSI()) + ",";
  json += "\"uptime\":" + String(millis()/1000) + ",";
  json += "\"status\":\"OK\"";
  json += "}";

  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  http.POST(json);
  http.end();
}
