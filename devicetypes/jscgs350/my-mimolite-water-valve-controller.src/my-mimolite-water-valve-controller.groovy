/**
    MIMOlite Water Valve Controller

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
    in compliance with the License. You may obtain a copy of the License at:

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
    on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
    for the specific language governing permissions and limitations under the License.

    Version: v2.1

    Updates:
    -------
    02-18-2016 : Initial commit
    03-05-2016 : Changed date format to MM-dd-yyyy h:mm a
    03-11-2016 : Due to ST's v2.1.0 app totally hosing up SECONDARY_CONTROL, implemented a workaround to display that info in a separate tile.

    This assumes a CR7-02 valve with 7 wires:
        - 12V-DC +  - COM
        - Black     - 12V-DC - (negative)
        - Green     - Relay's NO
        - Red       - Relay's NC
        - Yellow    - P1 / Sensor's: +
        - White     - P1 / Sensor's: -
        - Blue      - Not connected
        - Gray      - Not connected

        - The valve will short Yellow and White when it is fully closed.
        - The valve will short Blue and Gray when it is fully open. This is not used b/c MIMOlite has only 1 sensor.
*/

metadata {
    definition (name: "My MIMOlite Water Valve Controller", namespace: "jscgs350", author: "jscgs350") {
        capability "Alarm"
        capability "Polling"
        capability "Refresh"
        capability "Valve"
        capability "Contact Sensor"
        capability "Configuration"

        attribute "powered", "string"
        attribute "valveState", "string"
        attribute "sensorState", "string"
    }

    // UI tile definitions (6 x Unlimited grid)
    tiles(scale: 2) {
        multiAttributeTile(name:"valve", type: "generic", width: 6, height: 4, canChangeIcon: false, decoration: "flat"){
            tileAttribute ("device.valve", key: "PRIMARY_CONTROL") {
                attributeState "open", label: 'Open', action: "valve.close", icon: "st.valves.water.open", backgroundColor: "#53a7c0", nextState:"closingvalve"
                attributeState "closed", label: 'Closed', action: "valve.open", icon: "st.valves.water.closed", backgroundColor: "#ff0000", nextState:"openingvalve"
                attributeState "closingvalve", label:'Closing', icon:"st.valves.water.closed", backgroundColor:"#bf9700"
                attributeState "openingvalve", label:'Opening', icon:"st.valves.water.open", backgroundColor:"#bf9700"
            }
            tileAttribute ("statusText", key: "SECONDARY_CONTROL") {
                //attributeState "statusText", label:'${currentValue}'
                attributeState "statusText", label:''
            }
        }

        // The fully open/closed sensor pin (+/- on P1)
        standardTile("contact", "device.contact", width: 2, height: 2, inactiveLabel: false) {
            state "open", label: 'Open', icon: "st.contact.contact.open", backgroundColor: "#53a7c0"
            state "closed", label: 'Closed', icon: "st.contact.contact.closed", backgroundColor: "#ff0000"
        }
        standardTile("powered", "device.powered", width: 2, height: 2, inactiveLabel: false) {
            state "powerOn", label: "Power On", icon: "st.switches.switch.on", backgroundColor: "#79b821"
            state "powerOff", label: "Power Off", icon: "st.switches.switch.off", backgroundColor: "#ffa81e"
        }
        standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        standardTile("configure", "device.configure", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
        }
        valueTile("statusText", "statusText",  width: 6, height: 2, inactiveLabel: false, decoration: "flat") {
            state "statusText", label:'${currentValue}', backgroundColor:"#ffffff"
        }

        main (["valve", "contact"])
        details(["valve", "powered", "contact", "statusText", "refresh", "configure"])
    }
}

def parse(String description) {

    def events = []
    log.debug "parse: description = ${description}"
    def cmd = zwave.parse(description, [
        0x03: 1, // ?
        0x20: 1, // Basic
        0x25: 1, // Switch Binary
        0x30: 1, // Sensor Binary
        0x31: 3, // Sensor Multilevel
        0x35: 1, // ?
        0x70: 1, // Configuration
        0x71: 1, // Alarm/Notification
        0x72: 1, // Manufacturer Specific
        0x84: 1, // Wake Up
        0x85: 1, // Association
        0x86: 1, // Version
    ])
    log.debug "parse: cmd = ${cmd}"

    if (cmd) {
        if (cmd.CMD == "7105") { // MIMO sent a power loss report
            log.debug "Device lost power"
            events += createEvent(name: "powered", value: "powerOff", descriptionText: "$device.displayName lost power")
        } else { // Any other command means we have power
            events += createEvent(name: "powered", value: "powerOn", descriptionText: "$device.displayName regained power")
        }

        events += createEvent(zwaveEvent(cmd))
    }

    def timeString = new Date().format("yyyy-MM-dd h:mm:ss a", location.timeZone)
    def statusTextMsg = "Valve is ${device.currentState('valveState').value}.\n"
    statusTextMsg += "Last refreshed at "+timeString+".\n"
    statusTextMsg += "Sensor: ${device.currentState('sensorState').value}."
    events += createEvent(isStateChange: true, name: "statusText", value: statusTextMsg)

    //log.debug events.last()
    log.debug "Valve state: ${device.currentState('valveState').value}"
    log.debug "Sensor state: ${device.currentState('sensorState').value}"

    return events
}

def sensorReport(boolean isOpen) {
    // We send the sensorState event from here so that the state is updated before other code tries to read it.
    if (isOpen) {
        // This just tells us that it's not fully closed. It could be partially open.
        log.debug "Valve is Open"
        sendEvent(name: "sensorState", value: "open")
        return createEvent(name: "contact", value: "open", descriptionText: "$device.displayName is open")
    } else {
        log.debug "Valve is Closed"
        sendEvent(name: "sensorState", value: "fully closed")
        return createEvent(name: "contact", value: "closed", descriptionText: "$device.displayName is closed")
    }
}

def switchReport(boolean isOpen) {
    // We send the valveState event from here so that the state is updated before other code tries to read it.
    if (isOpen) {
        log.debug "Valve opening"
        sendEvent(name: "valveState", value: "flowing water (tap to close)")
        return createEvent(name: "valve", value: "open", type: "physical")
    } else {
        log.debug "Valve closing"
        sendEvent(name: "valveState", value: "NOT flowing water (tap to open)")
        return createEvent(name: "valve", value: "closed", type: "physical")
    }
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
    //log.debug "zwaveEvent: Overload 1 - BasicReport"
    return switchReport(cmd.value == 0)
}

// This is the sensor, +/- pins on P1, reporting its status unsolicited.
def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
    //log.debug "zwaveEvent: Overload 2 - BasicSet"
    return sensorReport(cmd.value == 255)
}

// This is the relay reporting its state on request (switchBinaryGet()).
def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    //log.debug "zwaveEvent: Overload 3 - SwitchBinaryReport"
    return switchReport(cmd.value == 0)
}

// This is the sensor, +/- pins on P1, reporting its status on request (sensorBinaryGet()).
def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd) {
    //log.debug "zwaveEvent: Overload 4 - SensorBinaryReport"
    return sensorReport(cmd.sensorValue == 255)
}

def zwaveEvent(physicalgraph.zwave.commands.alarmv1.AlarmReport cmd) {
    // We caught this up in the parse method. This method not used.
    log.warn "zwaveEvent: Overload 5 - We lost power"
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    // Handles all Z-Wave commands we aren't interested in, or failed to handle.
    log.warn "zwaveEvent: Overload 6 - Z-Wave command ignored"
    return [:]
}

// This is for when the the valve's valve.close() capability is called
def close() {
    log.debug "Closing Main Water Valve due to a VALVE capability condition"
    delayBetween([
        zwave.basicV1.basicSet(value: 0xFF).format(),
        zwave.switchBinaryV1.switchBinaryGet().format()
    ])
}

// This is for when the the valve's valve.open() capability is called
def open() {
    log.debug "Opening Main Water Valve due to a VALVE capability condition"
    delayBetween([
        zwave.basicV1.basicSet(value: 0x00).format(),
        zwave.switchBinaryV1.switchBinaryGet().format()
    ])
}

def poll() {
    log.debug "Executing Poll for Main Water Valve"
    delayBetween([
        zwave.switchBinaryV1.switchBinaryGet().format(),
        zwave.sensorBinaryV1.sensorBinaryGet().format(),
        zwave.basicV1.basicGet().format(),
        zwave.alarmV1.alarmGet().format()
    ],100)
}

def refresh() {
    log.debug "Executing Refresh for Main Water Valve per user request"
    delayBetween([
        zwave.switchBinaryV1.switchBinaryGet().format(),
        zwave.sensorBinaryV1.sensorBinaryGet().format(),
        zwave.basicV1.basicGet().format(),
        zwave.alarmV1.alarmGet().format()
    ],100)
}

def configure() {
    log.debug "Executing Configure for Main Water Valve per user request"
    def cmd = delayBetween([
        // Add the Hub to association group 3 so it can receive power alarms.
        zwave.associationV1.associationSet(groupingIdentifier: 3, nodeId: [zwaveHubNodeId]).format(),

        // Momentary Relay1 output.
        // 0 = disable (Default)
        // 1..255 = enable. Value sets the approximate momentary on time in 100ms increments.
        //zwave.configurationV1.configurationSet(parameterNumber: 11, configurationValue: [25], size: 1).format(),
        zwave.configurationV1.configurationSet(parameterNumber: 11, configurationValue: [0], size: 1).format(),
    ],100)
    log.debug "zwaveEvent ConfigurationReport: '${cmd}'"
}
