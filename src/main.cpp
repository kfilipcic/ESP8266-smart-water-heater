#include <ESP8266WiFi.h>
#include <Arduino.h>
#include <NTPClient.h>
#include <WiFiUdp.h>
#include "secrets.h" // Contains your network ssid and password

const char * ssid = SECRET_SSID;
const char * pass = SECRET_PASSWORD;

// Needed for NTP server
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP, "hr.pool.ntp.org");

// Something for HTTP request
String header;

// Defining the IN1 pin for the relay
// 5 is actually D1 on the ESP8266-12E
int in1 = 5;

//Static IP address configuration
IPAddress staticIP(192, 168, 5, 20); //ESP static ip
IPAddress gateway(192, 168, 5, 1);   //IP Address of your WiFi Router (Gateway)
IPAddress subnet(255, 255, 255, 0);  //Subnet mask
IPAddress dns(192, 168, 5, 1);  //DNS
 
const char* deviceName = "psihi";

WiFiClient client;
WiFiServer server(80);

// Defining time for the site request
// Current time
unsigned long currentTime = millis();
// Previous time
unsigned long previousTime = 0; 
// Define timeout time in milliseconds (example: 2000ms = 2s)
const long timeoutTime = 2000;

// One second counter for the clock
unsigned long startClock = millis();
unsigned long currentTimeClock;
const long timeoutTimeClock = 1000;

// For clock
boolean getTimeFromServer = true; // true when x hours pass, then the time updates from the NTP server
int hour = 0, minute = 0, second = 0, day = 0, month = 0, year = 0;
int daysOfMonth[12] = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
NTPClient::paup date_struct; // Used to extract day, month and year

void updateNTPTime() {
    timeClient.begin();
    timeClient.update();
    second = timeClient.getSeconds();
    minute = timeClient.getMinutes();
    hour = timeClient.getHours()+1; // Adding 1 hour because UTC+1
    if (hour >= 24) {
        hour = 0;
        day++;
    }
    date_struct = timeClient.getFormattedDate();
    day = date_struct.day;
    month = date_struct.month;
    year = date_struct.year;
    Serial.print(date_struct.dateStr + " ");
    Serial.print(timeClient.getFormattedTime() + " ");
    Serial.println("Getting time from NTP server...");
}

void setup() {
       Serial.begin(9600);

       // Making the relay pin output
       pinMode(in1, OUTPUT);
       digitalWrite(in1, HIGH);

       delay(10);
               
       Serial.println("Connecting to ");
       Serial.println(ssid); 
      
       WiFi.disconnect();  //Prevent connecting to wifi based on previous configuration
  
       WiFi.hostname(deviceName);      // DHCP Hostname (useful for finding device for static lease)
       WiFi.config(staticIP, gateway, subnet, dns);
       WiFi.begin(ssid, pass); 
       WiFi.mode(WIFI_STA);

       while (WiFi.status() != WL_CONNECTED) 
          {
            delay(500);
            Serial.print(".");
       }
      Serial.println("");
      Serial.println("WiFi connected"); 
      Serial.print("IP address:");
      Serial.println(WiFi.localIP());

      server.begin();
      Serial.println("HTTP server started");
      updateNTPTime(); // Getting the time from the NTP server for the first time
      startClock = millis(); // Starting to count our local clock

}

void relaySwitcher() {
    if ((((hour > 0 && hour < 25) || (hour >= 0 && hour <= 7))) && digitalRead(in1) == 1 ){//&& //second % 2 == 0) { 
        // If it's between 22:00 and 7:00, switch ON the relay
        digitalWrite(in1, LOW);
    }
    else if (digitalRead(in1) == 0) {
        // If not, switch OFF the relay
        digitalWrite(in1, HIGH);
    }
    else {
        digitalWrite(in1, HIGH);
    }
}

void theClock() {
    // T h e    c l o c k :
    currentTimeClock = millis();
    // If a second passed, update the time
    if ((currentTimeClock - startClock >= timeoutTimeClock)) {
        second = ((second)+1);
        if (!(second % 60)) {
            second = 0;    
            minute = ((minute)+1);
            if (!(minute % 60)) {
                minute = 0;    
                hour++;
                getTimeFromServer = true;
                if (!(hour % 24)) {
                    hour = 0;    
                    day = ((day)+1);
                    if (!(day % daysOfMonth[month])) {
                        day = 0;    
                        month = ((month)+1);
                        if (!(month % 12)) {
                            month = 0;    
                            year++;
                            if (!(year % 4)) daysOfMonth[1] = 29;
                            else daysOfMonth[1] = 28;
                            if (!(year % 100)) daysOfMonth[1] = 28;
                        }
                    }
                }
            }
        }
        startClock = currentTimeClock;
    }
    if (getTimeFromServer) {
        updateNTPTime();
        getTimeFromServer = false;
    }
}

void wifiClient() {
    WiFiClient client = server.available();
    if (client) {                             // If a new client connects,
        Serial.println("->Request from client.");          // print a message out in the serial port
        String currentLine = "";                // make a String to hold incoming data from the client
        currentTime = millis();
        previousTime = currentTime;

        while (client.connected() && currentTime - previousTime <= timeoutTime) { // loop while the client's connected
            relaySwitcher(); // Controlling the relay pin during HTTP requests
            currentTime = millis();         
            if (client.available()) {             // if there's bytes to read from the client,
                char c = client.read();             // read a byte, then
                Serial.write(c);                    // print it out the serial monitor
                header += c;
                if (c == '\n') {                    // if the byte is a newline character
                    // if the current line is blank, you got two newline characters in a row.
                    // that's the end of the client HTTP request, so send a response:
                    if (currentLine.length() == 0) {
                        // HTTP headers always start with a response code (e.g. HTTP/1.1 200 OK)
                        // and a content-type so the client knows what's coming, then a blank line:
                        client.println("HTTP/1.1 200 OK");
                        client.println("Content-type:text/html");
                        client.println("Connection: close");
                        client.println();

                        client.print("<html><h1>Time: "+String(hour)+":"+String(minute)+":"+String(second)+"<br>Date: "+String(day)+"."+String(month)+"."+String(year)+"</h1></html>");
                        client.println();
                        Serial.println(String(hour)+":"+String(minute)+":"+String(second));
                        break;
                        // Clear the header variable
                        header = "";
                        // Close the connection
                        client.stop();
                        Serial.println("Client disconnected.");
                        Serial.println("");
                    }
                    else if (c != '\r') {  // if you got anything else but a carriage return character,
                        currentLine += c;      // add it to the end of the currentLine
                    }
                }
            }
        }
    }
}

void loop() {     
    theClock();
    relaySwitcher();
    wifiClient();
}