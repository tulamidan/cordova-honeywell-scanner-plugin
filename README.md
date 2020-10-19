# cordova-honeywell-scanner-plugin
Cordova Plugin to receive input from a Honeywell scanners ( EDA51).
I uses the Intend API and will work for Android > 7

# Device Setup
- No special device setup is required.

# Usage
This plugin uses the `setKeepCallback` feature of the `PluginResult` so that you don't have to continually register to listen for scans. Wire up an scan event listener like this:

```javascript
plugins.honeywell.listenForScans(function(data) {
  // do something with 'data'
    console.log('You scanned: ' + data);
  });
```

Subsequent calls to `listenForScans` will replace the previously set callbacks.
