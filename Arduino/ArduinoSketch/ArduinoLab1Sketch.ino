/********************************************************************/
// First we include the libraries
#include <OneWire.h> 
#include <DallasTemperature.h>
/********************************************************************/
// Data wire is plugged into pin 2 on the Arduino 
#define ONE_WIRE_BUS 2 
/********************************************************************/
// Setup a oneWire instance to communicate with any OneWire devices  
// (not just Maxim/Dallas temperature ICs) 
OneWire oneWire(ONE_WIRE_BUS); 
/********************************************************************/
// Pass our oneWire reference to Dallas Temperature. 
DallasTemperature sensors(&oneWire);
/********************************************************************/ 

int buttonPin = 10;
int buttonState = 0;
int incomingByte = 0;
byte ledPins[] = {9, 8, 7, 6, 5, 4, 3};
boolean buttonPress = false;

void setup(void) 
{
  pinMode(buttonPin, INPUT);

  for (int i=0; i<7; i++){
    pinMode(ledPins[i], OUTPUT);
  }

  // start serial port 
  Serial.begin(9600); 
  sensors.begin(); 
} 

void serialEvent() {
  while (Serial.available()) {
    Serial.read();
  }
  // Checks if serial info is being passed from java app {
  if (buttonPress == false){
    buttonPress = true;
  }
  else{
    buttonPress = false;
  }
}

void loop(void) 
{ 
  // Checks button status
  buttonState = digitalRead(buttonPin);

  // One second delay
  delay(500);

  // Reads values from thermometer
  sensors.requestTemperatures(); 
  float numInC = sensors.getTempCByIndex(0);
  int numConvertToInt = (int) numInC;
  byte num = numConvertToInt; 

  // Sensor is unplugged
  if (numInC < -100){
    Serial.print("2000");
      for (byte i=0; i<7; i++) {
        //byte state = bitRead(num, i);
        digitalWrite(ledPins[i], HIGH);
      }
  }
  // Sensor is plugged in
  else {
    int Kelvin = num + 273;
    String strKelvin = String(Kelvin);
    Serial.print(Kelvin);
  }

  // Button has been pressed show LED values
  if (buttonState == HIGH || buttonPress == true) {
    for (byte i=0; i<7; i++) {
      byte state = bitRead(num, i);
      digitalWrite(ledPins[i], state);
    }
  }

  // Button is not pressed LEDs off
  else {
    for(int i=0; i<7; i++){
      digitalWrite(ledPins[i], LOW);
    }
  }
}
