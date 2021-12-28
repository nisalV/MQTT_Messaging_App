#include <ESP8266WiFi.h>
#include <PubSubClient.h>

#include <DallasTemperature.h>
#include <OneWire.h>

#define ONE_WIRE_BUS 4      // D2 pin of nodeMCU (temp reading)
#define BUILTIN_LED D0      // Built in LED

OneWire oneWire(ONE_WIRE_BUS);
 
DallasTemperature sensors(&oneWire);  

const char* ssid = "Dialog 4G";
const char* password = "pathmila1996";
const char* mqtt_server = "broker.mqtt-dashboard.com";

WiFiClient espClient;
PubSubClient client(espClient);
unsigned long lastMsg = 0;
#define MSG_BUFFER_SIZE  (50)
char msg[MSG_BUFFER_SIZE];
int value = 0;

void setup_wifi() {

  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  randomSeed(micros());

  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  for (int i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
  }
  Serial.println();

  // Switch on the LED if an 1 was received as first character
  if ((char)payload[0] == '1') {
    digitalWrite(BUILTIN_LED, LOW);   // Turn the LED on
  } else {
    digitalWrite(BUILTIN_LED, HIGH);  // Turn the LED off
  }
}

void reconnect() {
  // Loop until we're reconnected
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...");
    // Create a random client ID
    String clientId = "clientId-riur7mdNie";
    clientId += String(random(0xffff), HEX);
    // Attempt to connect
    if (client.connect(clientId.c_str())) {
      Serial.println("connected");                  
      client.publish("temp/sence", "hello world");   // Publish when connected
      client.subscribe("blink/led");                 // Subscibe when connected
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      delay(5000);
    }
  }
}

void setup() {
  pinMode(BUILTIN_LED, OUTPUT);     // Initialize the BUILTIN_LED pin as an output
  Serial.begin(115200);
  sensors.begin();
  setup_wifi();
  client.setServer(mqtt_server, 1883);
}

void loop() {
  sensors.requestTemperatures();         // To get temperature values 

  if (!client.connected()) {
    reconnect();
  }
  client.loop();

  client.setCallback(callback);         // LED on/ off by command

  unsigned long now = millis();
  if (now - lastMsg > 2000) {
    lastMsg = now;
    snprintf (msg, MSG_BUFFER_SIZE, "hello, temperature is %0.2f 'C", sensors.getTempCByIndex(0));
    Serial.print("Publish message: ");
    Serial.println(msg);
    client.publish("temp/sence", msg);
  }
}
