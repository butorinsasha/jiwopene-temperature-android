# temperature-android

Simple Android application that shows hardware temperatures from /sys/class/thermal and BatteryManager.

Supported languages: English and Czech

## Features
- Reading temperatures from /sys/class/thermal directory (it can use it even if you don't have permission to read contents of directory)
- Reading temperatures using Android API – BatteryManager class
- Configurable interval of reading sensor data (from 0.1 to 10 seconds)
- Logging temperatures (configurable interval from 1 to 60 minutes)
- Log can be exported as CSV

## Changelog
See [CHANGELOG file](CHANGELOG.md)

## Contribute
### Help translate to other languages
You can translate the app to other languages. Just download one of strings.xml files ([English](app/src/main/res/values/strings.xml), [Czech](app/src/main/res/values-cs/strings.xml)) and translate the text between `>` and `<`. Then submit [merge request](https://gitlab.com/jiwopene/temperature-android/merge_requests/new) or [issue](https://gitlab.com/jiwopene/temperature-android/issues/new) with translated file. When using merge requests, always adjust the language (directory `values-cs` for Czech, `values-fr` for French, etc.).

#### Example of one line from `strings.xml`
    Original
    <string name="refresh_sensors">Refresh sensor list</string>

    Translated
    <string name="refresh_sensors">Prohledat čidla</string>

#### Status of translations
<table>
 <tr>
  <th>Language</th>
  <th>
   State (Needs check/Good/Bad)
  </th>
 </tr>
 <tr>
  <td>English</td>
  <td>Good</td>
 </tr>
 <tr>
  <td>Czech</td>
  <td>Good</td>
 </tr>
 <tr>
  <td>Indonesian</td>
  <td>Good</td>
 </tr>
</table>
