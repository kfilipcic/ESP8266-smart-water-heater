# Smart home project

## Screenshots
### Browser screenshots
![Browser login](https://github.com/kfilipcic/ESP8266-smart-water-heater/blob/master/screenshots/browser_login.jpg?raw=true)
<kbd>
  <img src="https://github.com/kfilipcic/ESP8266-smart-water-heater/blob/master/screenshots/browser_login.jpg?raw=true">
</kbd>
![Browser time](https://github.com/kfilipcic/ESP8266-smart-water-heater/blob/master/screenshots/browser_time.jpg?raw=true)
Main page when logged in through a browser, shows the current date and time (it doesn't change in real-time, you have to refresh the page)
![Browser api check](https://github.com/kfilipcic/ESP8266-smart-water-heater/blob/master/screenshots/api_check.jpg?raw=true)
When /water\_heater\_check.html is accessed, it returns the current status of the ESP pin in JSON format
### Android app screenshots
![Android login screen](https://github.com/kfilipcic/ESP8266-smart-water-heater/blob/master/screenshots/android_login.png?raw=true)
![Android boiler off](https://github.com/kfilipcic/ESP8266-smart-water-heater/blob/master/screenshots/android_off.png?raw=true)
The main screen of the app features the current time & date of the server (scraped from the main webpage). Clicking on the big red image changes toggles the ESP pin, thus changing it to the other image presented below
![Android boiler on](https://github.com/kfilipcic/ESP8266-smart-water-heater/blob/master/screenshots/android_on.png?raw=true)
Also, the menu above currently only has a "refresh" option which refreshes the main activity
