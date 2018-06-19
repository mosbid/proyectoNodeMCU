#include <LiquidCrystal_I2C.h>
#include <time.h>
#include <String.h>
#include <Arduino.h>
#include <ArduinoJson.h>
#include <ESP8266WebServer.h>
#include <ESP8266Wifi.h>
#include <RestClient.h>
#include <PubSubClient.h>
#include <Keypad.h>

boolean permablock = false;
int puertaRelacionada = 1;
String direccionPuerta = "Avenida_Reina_Mercedes";

LiquidCrystal_I2C lcd(0x3F,16,2);
char responseBuffer[50];

//Config KEYPAD
const byte rows = 4; //four rows
const byte cols = 4; //three columns
char keys[rows][cols] = {
  {'1','2','3', 'A'},
  {'4','5','6', 'B'},
  {'7','8','9', 'C'},
  {'*','0','#', 'D'},
};

byte rowPins[rows] = {12, 13, 1, 9}; //connect to the row pinouts of the keypad
byte colPins[cols] = {16, 0, 2, 14};//connect to the column pinouts of the keypad

Keypad keypad = Keypad( makeKeymap(keys), colPins, rowPins ,cols, rows );
char men[15];
char pass[9] = {'1', '2', '3', '4', '\0'};
char masterPass[9] = {'9', '8', '7', '6', '5', '4', '3', '2', '\0'};

int i = 0;
int blockcounter = 0;
int changepass;
int newPass = 0;


char msg[50];
//const char* ssid = "Wifi Casa";
//const char* password = "pilarluisivanruben1492";
//const char* serverIP = "192.168.0.159";
const char* ssid = "S8 Manu";
const char* password = "27121996";
const char* serverIP = "192.168.43.124";
//const char* serverIP = "192.168.43.170";

WiFiClient espClient;
PubSubClient pubsubClient(espClient);
RestClient client = RestClient(serverIP,8083);

void wifiInit(){
  
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);
  
  while(WiFi.status() != WL_CONNECTED){
    delay(1000);
    Serial.print(".");
  }
  
  Serial.println("Conexion establecida");
  Serial.print("IP asignada: ");
  Serial.println(WiFi.localIP());

  pubsubClient.setServer(serverIP, 1883);
  pubsubClient.setCallback(callback);
}

void updatePuerta(int estadoPuerta){
  const size_t capacity = 120;
  DynamicJsonBuffer jsonBuffer(capacity);
  JsonObject& newJson = jsonBuffer.createObject();

  unsigned long time = millis();
  newJson["id"] = estadoPuerta;
  newJson["doorState"] = estadoPuerta;
  newJson["doorPass"] = pass;
  newJson["doorAdmin"] = masterPass;
  char jsonStr[100];
  newJson.printTo(jsonStr);

  Serial.println(jsonStr);

  String response = "";
  int statusCode = client.post("/api/door", jsonStr,
    &response);
    Serial.print("Status code: ");
    Serial.println(statusCode);
    Serial.print("Respuesta: ");
    Serial.println(response);  
}

void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Mensaje recibido [");
  Serial.print(topic);
  Serial.print("] ");
  String message = String((char *)payload);
  Serial.print(message);

    if(message == "abrirPuerta"){
      lcd.clear();
      lcd.setCursor(0,0);
      lcd.print("Door opened.");
      digitalWrite(10, HIGH);
      delay(1000);
      digitalWrite(10, LOW);
      lcd.clear();
    }
    if(message == "blockedDoor"){
      Serial.println("Puerta permabloqueada");
      lcd.clear();
      lcd.setCursor(0,0);
      lcd.print("DOOR PERMABLOCK");
      permablock = true;
    }
    if(message.startsWith("newNormalPass")){
      String newnormalpass = message;
      newnormalpass.replace("newNormalPass","");
      char buf1[9];
      newnormalpass.toCharArray(buf1, 9);      
      memset(pass, 0, sizeof(pass));
      strcpy(pass, buf1);
      memset(buf1, 0, sizeof(buf1));
    }
    if(message.startsWith("newMasterPass")){
      String newmasterpass = message;
      newmasterpass.replace("newMasterPass","");
      char buf2[9];
      newmasterpass.toCharArray(buf2, 9);      
      memset(masterPass, 0, sizeof(masterPass));
      strcpy(masterPass, buf2);
      memset(buf2, 0, sizeof(buf2));
    }
    if(message.startsWith("no")){
      Serial.println("Puerta desbloqueada");
      lcd.clear();
      lcd.setCursor(0,0);
      lcd.print("DOOR UNLOCKED");
      permablock = false;
    }
}


void initPass(){
  String response = "";
  int statusCode = client.get("/api/doorContra/1", &response);
  Serial.println(statusCode);
  Serial.println(response);
  char buf[9];
  response.toCharArray(buf, 9);
  memset(pass, 0, sizeof(pass));
  strcpy(pass,buf);

  String response2 = "";
  int statusCode2 = client.get("/api/doorContraMaestra/1", &response2);
  Serial.println(statusCode2);
  Serial.println(response2);
  char buf2[9];
  response2.toCharArray(buf2, 9);
  memset(masterPass, 0, sizeof(masterPass));
  strcpy(masterPass,buf2);

}

void putLog(boolean acierto, int intento, boolean bloqueo){
  const size_t capacity = 120;
  DynamicJsonBuffer jsonBuffer(capacity);
  JsonObject& newJson = jsonBuffer.createObject();

  unsigned long time = millis();
  newJson["tryState"] = acierto;
  newJson["tryDate"] = time;
  newJson["tryNumber"] = intento;
  newJson["tryBlock"] = bloqueo;
  newJson["relatedDoor"] = puertaRelacionada;
  char jsonStr[100];
  newJson.printTo(jsonStr);

  Serial.println(jsonStr);

  String response = "";
  int statusCode = client.put("/api/terminal", jsonStr,
    &response);
    Serial.print("Status code: ");
    Serial.println(statusCode);
    Serial.print("Respuesta: ");
    Serial.println(response);  
}



void reconnect() {
  while (!pubsubClient.connected()) {
    Serial.print("Conectando al servidor MQTT");
    if (pubsubClient.connect("dadESP")) {
      Serial.println("Conectado");
      pubsubClient.subscribe("topic_2");
    } else {
      Serial.print("Error, rc=");
      Serial.print(pubsubClient.state());
      Serial.println(" Reintentando en 5 segundos");
      delay(5000);
    }
  }
}

void setup() {

  Serial.begin(115200);

  lcd.init(5,4);
  lcd.backlight();

  lcd.clear();
  lcd.setCursor(0,0);
  pinMode(10, OUTPUT);
  digitalWrite(10, LOW);

  wifiInit();

  initPass();
}


void loop() {
  char key = keypad.getKey();
if (permablock == true){
    lcd.setCursor(0,0);
    lcd.print("DOOR PERMABLOCK");
}
if (key != NO_KEY && permablock==false) {
  if (i>8){ //OVERFLOW CONTROL
    i = 0;
    lcd.clear();
    lcd.setCursor(0,0);
    lcd.print("MAX SIZE ERROR");
    memset(men, 0, sizeof(men));
    delay(1000);
    lcd.clear();
    lcd.setCursor(0,0);
  }else if(blockcounter >=3){
    lcd.clear();
    lcd.setCursor(0,0);
    lcd.print("DOOR BLOCKED");
    putLog(false, blockcounter, true);
    updatePuerta(1);
    delay(5000);
    lcd.clear();
    updatePuerta(0);
    blockcounter = 0;
  }else if (key != '*' && key != '#'){
    men[i] = key;
    if(i==0){
      lcd.clear();
      lcd.setCursor(0,0);
      lcd.print("PASSWORD:");
    }
    lcd.setCursor(i,1);
    lcd.print('*');
    i++;
  }else if (key == '*' && newPass == 1){ //NEW NORMAL PASSWORD
    i = 0;
    memset(pass, 0, sizeof(pass));
    strcpy(pass, men);
    memset(men, 0, sizeof(men));
    lcd.clear();
    lcd.setCursor(0,0);
    lcd.print("PASS CHANGED");
    updatePuerta(1);
    newPass = 2;
    delay(1000);
    lcd.clear();
    lcd.setCursor(0,0);
    lcd.print("NEW MASTERPASS");

  }else if (key == '*' && newPass == 2){ //NEW MASTER PASSWORD
    i = 0;
    memset(masterPass, 0, sizeof(masterPass));
    strcpy(masterPass, men);
    memset(men, 0, sizeof(men));

    lcd.clear();
    lcd.setCursor(0,0);
    lcd.print("NEW MASTER OK");
    updatePuerta(1);
    newPass = 0;
    delay(1000);
    lcd.clear();
    lcd.setCursor(0,0);

  }else if (key == '*'){
  if(i==4){
    i = 0;
    if(strcmp(men, pass) == 0){ //Normal Pass OK   
        lcd.clear();
        lcd.setCursor(0,0);
        lcd.print("DOOR OPENED");
        putLog(true, blockcounter, false);
        blockcounter = 0;
        digitalWrite(10, HIGH);
        delay(1000);
        digitalWrite(10, LOW);
        lcd.clear();
      }else{ // Normal Pass ERROR
        blockcounter++;
        putLog(false, blockcounter, false);  
        lcd.clear();
        lcd.setCursor(0,0);
        lcd.print("PASS ERROR");
        delay(1000);
        lcd.clear();
        lcd.setCursor(0,0);
      }
    }else if(i==8){
      i = 0;
      if(strcmp(men, masterPass) == 0){ //Master Pass OK
        putLog(false, blockcounter, false);      
        blockcounter = 0;
        lcd.clear();
        lcd.setCursor(0,0);
        lcd.print("MASTER PASS OK");
        delay(1000);
        lcd.clear();
        lcd.setCursor(0,0);
        lcd.print("INSERT NEW PASS");
        newPass = 1;
      }else{ // Master Pass ERROR
        blockcounter++;
        putLog(false, blockcounter, false);  
        lcd.clear();
        lcd.setCursor(0,0);
        lcd.print("MASTERPASS ERROR");
        delay(1000);
        lcd.clear();
        lcd.setCursor(0,0);
      }
    }else{ //SIZE ERROR
      i = 0;
      lcd.clear();
      lcd.setCursor(0,0);
      lcd.print("SIZE ERROR");
      delay(1000);
      lcd.clear();
      lcd.setCursor(0,0);
    }
    memset(men, 0, sizeof(men));
    
  }else if (key == '#'){ //Reseteo del password
    i = 0;
    lcd.clear();
    lcd.setCursor(0,0);
    lcd.print("INPUT ERASED");
    memset(men, 0, sizeof(men));
    delay(1000);
    lcd.clear();
    lcd.setCursor(0,0);
  }
}

// MQTT
  if (!pubsubClient.connected()) {
    reconnect();
  }

   pubsubClient.loop();

}
