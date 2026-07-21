/*
    Sofabaton X Series
	Copyright 2025 Hubitat Inc. All Rights Reserved

	2025-03-22 maxwell
		-initial publication in github repo

    2025-03-28 Gassgs
        -Made Button Count a preferences & changed parse to just send the number from the body as the button press
	2025-03-30 SViel
		-Added recognition of on/off button
		-Added the option to store the name of an activity if included in the button press (removed - body is always numeric)

	2025-03-19 dJOS
		-Renamed driver to 'Sofabaton X Series' to reflect compatibility with X1S (minimum supported hardware) and X2
		-Fixed msg variable in parse() declared as implicit global, now correctly scoped with def
		-Added null/empty body guard in parse() to prevent NullPointerException
		-Added bounds check in push() to reject button numbers outside 1..buttonCount
		-Fixed numberOfButtons event now sent unconditionally in updated(), not gated on ip being set
		-Removed Activity attribute and event; body is always numeric so Activity name is not possible
		-Fixed IP/ip case mismatch in updated()
		-Fixed ipToHex() ignoring its parameter, now uses passed argument
		-Fixed mixed tab/space indentation in parse() else block
		-Added on() and off() command handlers for Switch capability
		-Added integer validation before firing pushed event in parse()
		-Added installed() handler to initialise device on first save
		-Restructured buttons: 10 numeric (body 1-10) with labels, 10 user definable with match string and label

	2026-07-18 dJOS
		-Fixed string activities never triggering automations: pushed is a NUMBER attribute
		 per the PushableButton capability, so the string values previously sent were invalid.
		 User definable slots 1-10 now map to button numbers 11-20
		-Added lastButtonValue and lastButtonLabel string attributes so rules can also trigger
		 on the raw string sent by the remote, or on the friendly label
		-push() now accepts 1-20 as well as a match string, resolving it to its button number
		-Trim whitespace from the request body before matching

	2026-07-22 dJOS
		-Added button numbers (11-20) to the User Definable slot labels in the UI
		-Documented the on/off switch body option in the on-screen app config instructions

	*OVERVIEW
	 This driver allows a Sofabaton X Series remote to trigger Hubitat automations.
	 When a Sofabaton activity is started or stopped on the remote, it sends a value
	 in the request body to this driver via a local HTTP PUT request. That value is mapped
	 to a button press which can then trigger any Hubitat rule or automation.
	
	 The driver supports three types of input:
	   -on/off: fires a switch event (reserved, always active)
	   -Numeric (1-10): fires a pushed event with the number; use Button Labels in
	    preferences to document what each number represents
	   -User definable (10 slots): match any string the remote sends to a named button;
	    configure the match string and a friendly label in preferences. Slot 1 fires
	    button 11, slot 2 fires button 12, and so on through slot 10 / button 20

	 Every input fires a numeric pushed event, since Hubitat's PushableButton capability
	 defines pushed as a number. To trigger on the string itself instead, use the
	 lastButtonValue attribute (the raw text from the remote) or lastButtonLabel (your
	 configured description) as a Custom Attribute trigger in Rule Machine.
	
	 NOTE: This is one-way communication - remote to Hubitat only.
	 To trigger a Sofabaton activity FROM Hubitat, enable the API in the Sofabaton mobile app.
	 This exposes a webhook URL for each activity. Copy that URL into a Hubitat Rule Machine
	 action of type "Send HTTP GET". No additional driver is required for that direction.

	*COMPATIBLE HARDWARE
	 X1S (minimum supported), X2

	*HUBITAT CONFIGURATION
	 -Set a static DHCP reservation for the Sofabaton hub
	 -Enter that reserved IP address in this driver's Remote IP Address preference

	*SOFABATON APP CONFIGURATION
	 -In the Sofabaton app, go to Devices and tap Add Device, then select Wi-Fi
	 -Tap the link at the bottom: "Create a virtual device for IP control"
	 -Enter the URL:  http://[your Hubitat IP]:39501/
	 -Set the request method to PUT
	 -Leave Content Type and Additional Headers blank
	 -In the Body field, enter either:
	    -A number (1-10) for a numeric button
	    -Any string (e.g. "watchTV") for a user definable button - must match the
	     Match String configured in this driver's preferences exactly
	 -Repeat for each activity using a unique value each time


*/

def version() {
    return "1.8"
}

metadata {
    definition (name: "Sofabaton X Series", namespace: "hubitat", author: "Mike Maxwell", importUrl: "https://raw.githubusercontent.com/dJOS1475/Hubitat-Sofabaton-X-Series/refs/heads/main/Sofabaton_Driver.groovy") {
        capability "Actuator"
        capability "PushableButton"
        capability "Switch"
        attribute "lastButtonValue", "string"
        attribute "lastButtonLabel", "string"
        preferences {
            input name: "deviceInfo", type: "paragraph", element: "paragraph", title: "Sofabaton X Series", description: "Driver Version: ${version()}<br>Compatible Hardware: X1S and above"
            input name: "appConfig", type: "paragraph", element: "paragraph", title: "Sofabaton App Configuration", description: "1. In the Sofabaton app, go to Devices and tap Add Device, then select Wi-Fi<br>2. Tap the link at the bottom: 'Create a virtual device for IP control'<br>3. Enter the URL: http://[your Hubitat IP]:39501/<br>4. Set the request method to PUT<br>5. Leave Content Type and Additional Headers blank<br>6. In the Body field enter either:<br>&nbsp;&nbsp;&nbsp;- A number (1-10) for a numeric button<br>&nbsp;&nbsp;&nbsp;- Any string (e.g. watchTV) for a user definable button<br>&nbsp;&nbsp;&nbsp;- on or off to set this device's switch state<br>7. Repeat for each activity using a unique value each time"
            input name:"ip", type:"text", title: "Remote IP Address"
            input name: "userInfo", type: "paragraph", element: "paragraph", title: "User Definable Buttons", description: "Enter the match string the remote sends. Optionally add a pipe | followed by a description e.g. watchTV|Watch TV. The match string must match what you entered in the remote app.<br>These fire button numbers 11-20 (User 1 = button 11, User 10 = button 20). You can also trigger rules on the lastButtonValue or lastButtonLabel custom attributes if you prefer matching the string itself."
            input name:"usrBtn1", type:"text", title:"User 1 (11):", description:"matchString|Description", required:false
            input name:"usrBtn2", type:"text", title:"User 2 (12):", description:"matchString|Description", required:false
            input name:"usrBtn3", type:"text", title:"User 3 (13):", description:"matchString|Description", required:false
            input name:"usrBtn4", type:"text", title:"User 4 (14):", description:"matchString|Description", required:false
            input name:"usrBtn5", type:"text", title:"User 5 (15):", description:"matchString|Description", required:false
            input name:"usrBtn6", type:"text", title:"User 6 (16):", description:"matchString|Description", required:false
            input name:"usrBtn7", type:"text", title:"User 7 (17):", description:"matchString|Description", required:false
            input name:"usrBtn8", type:"text", title:"User 8 (18):", description:"matchString|Description", required:false
            input name:"usrBtn9", type:"text", title:"User 9 (19):", description:"matchString|Description", required:false
            input name:"usrBtn10", type:"text", title:"User 10 (20):", description:"matchString|Description", required:false
            input name: "numericInfo", type: "paragraph", element: "paragraph", title: "Numeric Buttons", description: "Labels for buttons triggered by a number (1-10) in the request body."
            input name:"btnLabel1", type:"text", title:"1:", description:"Button 1 label", required:false
            input name:"btnLabel2", type:"text", title:"2:", description:"Button 2 label", required:false
            input name:"btnLabel3", type:"text", title:"3:", description:"Button 3 label", required:false
            input name:"btnLabel4", type:"text", title:"4:", description:"Button 4 label", required:false
            input name:"btnLabel5", type:"text", title:"5:", description:"Button 5 label", required:false
            input name:"btnLabel6", type:"text", title:"6:", description:"Button 6 label", required:false
            input name:"btnLabel7", type:"text", title:"7:", description:"Button 7 label", required:false
            input name:"btnLabel8", type:"text", title:"8:", description:"Button 8 label", required:false
            input name:"btnLabel9", type:"text", title:"9:", description:"Button 9 label", required:false
            input name:"btnLabel10", type:"text", title:"10:", description:"Button 10 label", required:false
            input name:"logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
            input name:"txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
        }
    }
}

void logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

void installed(){
    log.info "installed..."
    updated()
}

void updated(){
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    log.warn "description logging is: ${txtEnable == true}"
    if (logEnable) runIn(1800,logsOff)
    sendEvent(name:"numberOfButtons", value:20)
    if (ip) {
        device.deviceNetworkId = ipToHex(ip)
    }
    // Truncate numeric button labels to 40 chars
    for (int i = 1; i <= 10; i++) {
        def lbl = settings["btnLabel${i}"] ?: ""
        if (lbl.length() > 40) {
            device.updateSetting("btnLabel${i}", [value:lbl.take(40), type:"text"])
        }
    }
    // Truncate user definable button entries to 80 chars (match + pipe + description)
    for (int i = 1; i <= 10; i++) {
        def val = settings["usrBtn${i}"] ?: ""
        if (val.length() > 80) {
            device.updateSetting("usrBtn${i}", [value:val.take(80), type:"text"])
        }
    }
}

void parse(String description) {
    def msg = parseLanMessage(description)
    if (logEnable) log.debug "String is: $msg"
    if (logEnable) log.debug "String Header is: $msg.header"
    if (logEnable) log.debug "String Body is: $msg.body"
    def data = msg.body?.trim()
    if (!data) {
        if (logEnable) log.warn "$device.label Empty body received, ignoring"
        return
    }

    // 1. Check for on/off switch commands
    if (data.equalsIgnoreCase("on")) {
        sendEvent(name:"switch", value:"on")
        return
    } else if (data.equalsIgnoreCase("off")) {
        sendEvent(name:"switch", value:"off")
        return
    }

    // 2. Check if body is a numeric button (1-10)
    if (data.isInteger()) {
        def btn = data.toInteger()
        if (btn >= 1 && btn <= 10) {
            firePushed(btn, settings["btnLabel${btn}"] ?: "", data)
            return
        }
    }

    // 3. Check against user definable match strings -> buttons 11-20
    def hit = matchUserButton(data)
    if (hit) {
        firePushed(hit[0] as Integer, hit[1] as String, data)
        return
    }

    // 4. No match found
    log.warn "$device.label No match found for received body value: $data"
}

// Fires the numeric pushed event required by PushableButton, plus the string
// attributes so rules can trigger on the raw value or the friendly label.
private void firePushed(Integer btn, String lbl, String raw) {
    if (txtEnable) log.info "$device.label Button $btn${lbl ? ' (' + lbl + ')' : ''} Pushed"
    sendEvent(name:"pushed", value:btn, isStateChange: true, descriptionText:"$device.label button $btn was pushed")
    sendEvent(name:"lastButtonValue", value:raw, isStateChange: true)
    sendEvent(name:"lastButtonLabel", value:(lbl ?: raw), isStateChange: true)
}

// Returns [button, label] for a user definable slot matching str, or null.
private List matchUserButton(String str) {
    for (int i = 1; i <= 10; i++) {
        def val = settings["usrBtn${i}"] ?: ""
        if (!val) continue
        def parts = val.split(/\|/, 2)
        def match = parts[0].trim()
        if (match && match.equalsIgnoreCase(str)) {
            return [i + 10, parts.size() > 1 ? parts[1].trim() : match]
        }
    }
    return null
}

void push(data) {
    def str = data.toString().trim()

    // Numeric buttons: 1-10 are the numeric slots, 11-20 the user definable ones
    if (str.isInteger()) {
        def btn = str.toInteger()
        if (btn < 1 || btn > 20) {
            log.warn "$device.label Button $btn is out of range (1-20), ignoring"
            return
        }
        def lbl
        if (btn <= 10) {
            lbl = settings["btnLabel${btn}"] ?: ""
        } else {
            def val = settings["usrBtn${btn - 10}"] ?: ""
            def parts = val ? val.split(/\|/, 2) : []
            lbl = parts.size() > 1 ? parts[1].trim() : (parts ? parts[0].trim() : "")
        }
        firePushed(btn, lbl, str)
        return
    }

    // String input: resolve it to its user definable button number
    def hit = matchUserButton(str)
    if (hit) {
        firePushed(hit[0] as Integer, hit[1] as String, str)
    } else {
        log.warn "$device.label No user definable button matches '$str', ignoring"
    }
}

void on() {
    if (txtEnable) log.info "$device.label Switch On"
    sendEvent(name:"switch", value:"on")
}

void off() {
    if (txtEnable) log.info "$device.label Switch Off"
    sendEvent(name:"switch", value:"off")
}

String ipToHex(String ipAddress) {
    List<String> quad = ipAddress.split(/\./)
    String hexIP = ""
    quad.each {
        hexIP += Integer.toHexString(it.toInteger()).padLeft(2,"0").toUpperCase()
    }
    return hexIP
}