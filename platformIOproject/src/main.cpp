#include <Arduino.h>

#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>

#include <NTPClient.h>
#include <WiFiUdp.h>

#include "secrets.h" // Contains your network ssid and password

#include <WebParser.h>

#include <time.h>

#define bufferMax 10000
#define queryMax 10000
#define RULE_FIRST_PARAM_INDEX_START 18
#define RULE_URL_STRING_MAX_LEN 100
#define ROOM_TEMP_MIN 19
#define ROOM_TEMP_MAX 26
#define WATER_TEMP_MIN 20
#define WATER_TEMP_MAX 80
#define MAX 100

struct wHeater
{
    char name[MAX];
    float temp;
    float room_temp;
    uint8 state;
};

struct AC
{
    char name[];
    float temp;
    uint8 state;
};

struct Heater
{
    char name[];
    float temp;
    float room_temp;
    uint8 state;
};

int bufferSize;
char queryBuffer[bufferMax];
char param_value[queryMax];
unsigned long arduinoSession = 1;

WebParser webParser;
WiFiClient client;

const char *ssid = SECRET_SSID;
const char *pass = SECRET_ROUTER_PASSWORD;

const char *user = SECRET_AUTH_USER;
const char *password = SECRET_AUTH_PASSWORD;

WiFiServer server(80);

// Needed for NTP server
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP, "hr.pool.ntp.org");

// Something for HTTP request
String header;

// Defining the IN1 pin for the relay
// 5 is actually D1 on the ESP8266-12E
int in1 = 5;

const char *deviceName = "pc";

// Defining time for the site request
// Current time
unsigned long startTime = millis();
unsigned long currentTime;
// Define timeout time in milliseconds (example: 2000ms = 2s)
const long timeoutTime = 20000;

// One second counter for the clock
unsigned long startClock = millis();
unsigned long currentTimeClock;
const long timeoutTimeClock = 1000;

// For clock
boolean getTimeFromServer = true; // true when x hours pass, then the time updates from the NTP server
int hour = 0, minute = 0, second = 0, day = 0, month = 0, year = 0;
int daysOfMonth[12] = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
NTPClient::paup date_struct; // Used to extract day, month and year

long long int startTimeMs = -1;
long long int startTimeHHmmMs = -1; // Start time without the date in ms
long long int endTimeHHmmMs = -1;   // End time without the date in ms

char queryStore[RULE_URL_STRING_MAX_LEN];
char firstParam[RULE_URL_STRING_MAX_LEN];
const int start_index = RULE_FIRST_PARAM_INDEX_START;

float tempStates[] = {0, .1, -.1};

struct wHeater water_heater[2];

String httpHeader =
    String("HTTP/1.1 200 OK\r\n") +
    "Content-Type: text/html\r\n" +
    "Connection: close\r\n";

//String water_heater = "1";
String processor();
void serverSection();
void water_heater_check_state();
void water_heater_switcher();
void handleLogin();
void secureRedirect();
void parseReceivedRequest(WiFiClient client);
boolean loggedIn();

void updateNTPTime()
{
    timeClient.begin();
    timeClient.update();
    // second = timeClient.getSeconds();
    minute = timeClient.getMinutes();
    hour = timeClient.getHours() + 2; // Adding 1 hour because UTC+1
    if (hour >= 24)
    {
        hour = 1;
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

void setup()
{
    Serial.begin(115200);

    for (int i = 0; i < 2; i++)
    {
        String wName = "water_heater_" + String(i + 1);
        char wname[30];
        wName.toCharArray(wname, 30);
        strcpy(water_heater[i].name, wname);
        water_heater[i].room_temp = 21;
        water_heater[i].temp = 50;
        water_heater[i].state = 1;
    }

    // Making the relay pin output
    pinMode(in1, OUTPUT);
    digitalWrite(in1, HIGH);

    delay(10);

    Serial.println("Connecting to ");
    Serial.println(ssid);

    WiFi.disconnect(); //Prevent connecting to wifi based on previous configuration

    WiFi.hostname(deviceName); // DHCP Hostname (useful for finding device for static lease)
    WiFi.config(staticIP, dns, gateway, subnet);
    WiFi.begin(ssid, pass);
    WiFi.mode(WIFI_STA);

    while (WiFi.status() != WL_CONNECTED)
    {
        delay(500);
        Serial.print(".");
    }

    Serial.println("WiFi connected");
    Serial.print("IP address:");
    Serial.println(WiFi.localIP());
    Serial.println(WiFi.subnetMask());
    Serial.println(WiFi.gatewayIP());

    //configTime(3 * 3600, 0, "pool.ntp.org", "time.nist.gov");

    server.begin();

    updateNTPTime();       // Getting the time from the NTP server for the first time
    startClock = millis(); // Starting to count our local clock
}

void relaySwitcher()
{
    if ((((hour > 0 && hour < 25) || (hour >= 0 && hour <= 7))) && digitalRead(in1) == 1)
    { //&& //second % 2 == 0) {
        // If it's between 22:00 and 7:00, switch ON the relay
        digitalWrite(in1, LOW);
    }
    else if (digitalRead(in1) == 0)
    {
        // If not, switch OFF the relay
        digitalWrite(in1, HIGH);
    }
    else
    {
        digitalWrite(in1, HIGH);
    }
}

void give_temperature(float &temp, float low, float high)
{
    temp = (temp > low && temp < high) ? temp + tempStates[random(3)] : temp;
}

void theClock()
{
    // T h e    c l o c k :
    currentTimeClock = millis();
    // If a second passed, update the time
    if ((currentTimeClock - startClock >= timeoutTimeClock))
    {
        second = ((second) + 1);
        if (!(second % 60))
        {
            second = 0;
            minute = ((minute) + 1);
            if (!(minute % 60))
            {
                minute = 0;
                hour++;
                getTimeFromServer = true;
                if (!(hour % 24))
                {
                    hour = 0;
                    day = ((day) + 1);
                    if (!(day % daysOfMonth[month]))
                    {
                        day = 0;
                        month = ((month) + 1);
                        if (!(month % 12))
                        {
                            month = 0;
                            year++;
                            if (!(year % 4))
                                daysOfMonth[1] = 29;
                            else
                                daysOfMonth[1] = 28;
                            if (!(year % 100))
                                daysOfMonth[1] = 28;
                        }
                    }
                }
            }
        }
        startClock = currentTimeClock;
    }
    if (getTimeFromServer)
    {
        updateNTPTime();
        getTimeFromServer = false;
    }
}

void loop()
{
    theClock();

    client = server.available();
    if (client)
    {
        boolean currentLineIsBlank = true;
        bufferSize = 0;

        while (client.connected())
        {
            theClock();
            if (client.available())
            {
                theClock();
                char c = client.read();

                if (bufferSize < bufferMax)
                    queryBuffer[bufferSize++] = c;
                if (c == '\n' && currentLineIsBlank)
                {
                    parseReceivedRequest(client);
                    bufferSize = 0;
                    webParser.clearBuffer(queryBuffer, bufferMax);
                    break;
                }
                if (c == '\n')
                {
                    currentLineIsBlank = true;
                }
                else if (c != '\r')
                {
                    currentLineIsBlank = false;
                }
            }
        }
        //delay(10);

        client.flush();
        client.stop();

        long long int curr_time_ms = system_mktime(year, month, day, hour, minute, second) * 1000;
        
        if (startTimeMs >= 0 && startTimeHHmmMs >= 0 && endTimeHHmmMs >= 0)
        {
            if (startTimeMs >= curr_time_ms) {
                long long int curr_daytime_ms = hour * 3600000 + minute * 60000 + second * 1000;
                if (startTimeHHmmMs < endTimeHHmmMs) {
                    if (curr_daytime_ms > startTimeHHmmMs && curr_daytime_ms < endTimeHHmmMs && digitalRead(in1)) digitalWrite(in1, LOW);
                    else if (!digitalRead(in1)) {
                        digitalWrite(in1, HIGH);
                    }
                } else {
                    long long int curr_date_ms = system_mktime(year, month, day, 0, 0, 0) * 1000;
                    if (curr_time_ms > startTimeHHmmMs && curr_daytime_ms < (curr_date_ms + endTimeHHmmMs) && digitalRead(in1))
                        digitalWrite(in1, LOW);
                    else if (!digitalRead(in1)) {
                        digitalWrite(in1, HIGH);
                    }
                }
            }
        }

        currentTime = millis();

        if ((currentTime - startTime) >= timeoutTime)
        {
            for (int i = 0; i < 2; i++)
            {
                give_temperature(water_heater[i].temp, WATER_TEMP_MIN, WATER_TEMP_MAX);
                give_temperature(water_heater[i].room_temp, ROOM_TEMP_MIN, ROOM_TEMP_MAX);
            }
        }
    }
}

void water_heater_check_state(int index)
{
    client.println("{'water_heater': '" + String(water_heater[index].state) + "'}");
}

void water_heater_switcher(int index)
{
    switch (water_heater[index].state)
    {
    case 0:
        water_heater[index].state = 1;
        digitalWrite(in1, HIGH);
        break;
    case 1:
        water_heater[index].state = 0;
        digitalWrite(in1, LOW);
        break;
    }
    client.println("{'water_heater': '" + String(water_heater[index].state) + "'}");
}

void get_current_temperature(String deviceString, int num, double tempValue)
{
    // return random number between 20 and 90 (representing degrees in Celsius) in JSON format
    client.println("{'" + deviceString + "': '" + String(tempValue) + "'}");
}

void renderHtmlPage(char *page, WiFiClient client)
{
    String pagestr = (String)page;

    //Serial.println(pagestr);

    String time = String(hour) + ":" + String(minute) + ":" + String(second);
    String date = String(day) + "." + String(month) + "." + String(year);

    theClock();

    client.println(httpHeader);

    if (pagestr.indexOf("logtut.html") != -1)
    {
        client.println(F("<!DOCTYPE html><html><head>"));
        client.println(F("<meta name='viewport' content='user-scalable=no, initial-scale=1, maximum-scale=1, minimum-scale=1, width=device-width' />"));
        client.println(F("<title>Smart home login</title>"));
        client.println(F("<style>#loginform{width: 90%;background: #EEE;box-shadow: 2px 2px 10px #999;padding: 10px;display: block;margin: 0 auto 0 auto;}"));
        client.println(F("@media screen and (min-width: 32em) {#loginform{width: 400px;}}"));
        client.println(F("</style><link rel='icon' href='data:,'></head><body><div><h1>Login</h1></div>"));
        client.println(F("<form id='loginform' action='/login' method='GET'>"));
        client.println(F("<input type='text' name='username' placeholder='Username'>"));
        client.println(F("<input type='password' name='password' autocomplete='on' placeholder='Password'>"));
        client.println(F("<br><input type='submit' value='Submit'></form><p><a href='entry.htm'>entry</a></p></body></html>"));
    }

    else if (pagestr.indexOf("entry.html") != -1)
    {
        client.println("<html><body></form><h1>Time: " + time + "</h1></br><h1>Date: " + date + "</h1></br></body></html>");
    }
    else if (pagestr.indexOf("water_heater_1_check") != -1)
    {
        water_heater_check_state(0);
    }
    else if (pagestr.indexOf("water_heater_2_check") != -1)
    {
        water_heater_check_state(1);
    }
    else if (pagestr.indexOf("water_heater_1_switch") != -1)
    {
        water_heater_switcher(0);
    }
    else if (pagestr.indexOf("updateTime") != -1)
    {
        updateNTPTime();
    }
    else if (pagestr.indexOf("water_heater_1_temperature") != -1)
    {
        get_current_temperature("water_heater", 1, water_heater[0].temp); // temperature value is random and between 20 and 90 degrees Celsius
    }
    else if (pagestr.indexOf("water_heater_2_temperature") != -1)
    {
        get_current_temperature("water_heater", 1, water_heater[1].temp); // temperature value is random and between 20 and 90 degrees Celsius
    }
    else if (pagestr.indexOf("water_heater_1_room_temperature") != -1)
    {
        get_current_temperature("water_heater", 1, water_heater[0].room_temp);
    }
    else if (pagestr.indexOf("rform") != -1)
    {
        client.println(F("<html><head></head><form id='ruleForm' action='/rule' method='get'>"));
        client.println(F("<input type='text' name='starttimems' placeholder='Start time in ms'>"));
        client.println(F("<input type='text' name='starttime' placeholder='Start time'>"));
        client.println(F("<input type='text' name='endtime' placeholder='End time'>"));
        client.println(F("<input type='submit' value='Submit'></form></html>"));
    }
}

long long atoll(const char *ptr)
{
    long long result = 0;
    while (*ptr && isdigit(*ptr))
    {
        result *= 10;
        result += *ptr++ - '0';
    }
    return result;
}

void parseReceivedRequest(WiFiClient client)
{
    theClock();
    //find query vars
    //Serial.println(" ");
    Serial.println("*************");
    Serial.println(queryBuffer);
    Serial.println("*************");

    //  GET /index.htm HTTP/1.1
    // GET / HTTP/1.1
    if (webParser.contains(queryBuffer, "GET / HTTP/1.1") || webParser.contains(queryBuffer, ".html") || webParser.contains(queryBuffer, "rule"))
    {
        theClock();
        // *********** Render HTML ***************
        // code not to render form request.
        // GET /index.htm?devicelist=1&nocache=549320.8093103021 HTTP/1.1
        if (loggedIn())
        {
            //render html pages only if you've logged in
            webParser.clearBuffer(param_value, queryMax);
            webParser.fileUrl(queryBuffer, param_value);
            // default page
            theClock();

            if (webParser.contains(queryBuffer, "rule"))
            {
                client.println(httpHeader);

                webParser.parseQuery(queryBuffer, "starttimems", param_value);

                strcpy(queryStore, param_value);
                strcpy(firstParam, param_value);

                strtok(firstParam, "&");

                String substr = String(firstParam).substring(start_index);
                substr.toCharArray(queryStore, queryMax);

                startTimeMs = atoll(queryStore);

                client.print("{\n\t'startTimeMs': '");
                client.print(queryStore);
                client.print("',\n\t");

                webParser.clearBuffer(param_value, queryMax);
                webParser.parseQuery(queryBuffer, "starttime", param_value);
                strcpy(queryStore, param_value);
                startTimeHHmmMs = atoll(queryStore);

                client.print("'startTimeHHmmMs': '");
                client.print(queryStore);
                client.print("',\n\t");

                webParser.clearBuffer(param_value, queryMax);
                webParser.parseQuery(queryBuffer, "endtime", param_value);
                strcpy(queryStore, param_value);
                endTimeHHmmMs = atoll(queryStore);

                client.print("'endTimeHHmmMs': '");
                client.print(queryStore);
                client.print("'\n}");

                return;
            }

            else if (strcmp(param_value, "/") == 0)
            {
                strcpy(param_value, "entry.html");
                client.println(F("HTTP/1.1 302 Found"));
                client.println(F("Location: /entry.html"));
            }
            //else load whatever
            //Serial.println(param_value);
            renderHtmlPage(param_value, client);
        }
        else
        {
            //loggin form
            char page[] = "logtut.html";
            //set it so it's not the same all the time.
            arduinoSession = millis();
            renderHtmlPage(page, client);

        } //login
    }
    else
    {
        webParser.clearBuffer(param_value, queryMax);

        if (webParser.contains(queryBuffer, "login"))
        {
            webParser.parseQuery(queryBuffer, "username", param_value);

            char user[30];
            strcpy(user, param_value);

            webParser.clearBuffer(param_value, queryMax);
            webParser.parseQuery(queryBuffer, "password", param_value);
            char pass[30];
            strcpy(pass, param_value);

            // ***************** LOGIN ********************
            Serial.println(user);
            Serial.println(pass);

            if (webParser.compare(SECRET_AUTH_USER, user) && webParser.compare(SECRET_AUTH_PASSWORD, pass))
            {
                arduinoSession = millis();
                //***** print out Session ID
                // Serial.println(arduinoSession);
                // successful login and redirect to a page
                client.println(F("HTTP/1.1 302 Found"));
                client.print(F("Set-cookie: ARDUINOSESSIONID="));
                client.print(arduinoSession);
                client.println(F("; HttpOnly"));
                client.println(F("Location: /entry.html"));
                client.println();
            }
            else
            {
                // redirect back to login if wrong user / pass
                client.println(F("HTTP/1.1 302 Found"));
                client.println(F("Location: /logtut.html"));
                client.println();
            } // if login
        }
        else if (webParser.contains(queryBuffer, "logout"))
        {
            // kill session ID
            arduinoSession = 1;
            // redirect back to login if wrong user / pass
            client.println(F("HTTP/1.1 302 Found"));
            client.println(F("Location: /logtut.html"));
            client.println();
        }

    } //end main else
} // end function

boolean loggedIn()
{
    webParser.clearBuffer(param_value, queryMax);
    //going to need a parse cookie function
    webParser.parseQuery(queryBuffer, "ARDUINOSESSIONID", param_value);

    if (arduinoSession == atol(param_value))
    {
        return true;
    }
    else
    {
        return false;
    }
    return false;
}
