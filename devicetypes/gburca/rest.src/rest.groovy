/**
 * Copyright 2019 GBurca
 */

metadata {
    definition (name: "REST Switch", namespace: "gburca", author: "Gabriel Burca", runLocally: true, minHubCoreVersion: '000.021.00001', executeCommandsLocally: true, mnmn: "SmartThings", vid: "generic-switch") {
        capability "Actuator"
        capability "Sensor"
        capability "Switch"
    }

    preferences {}

    tiles(scale: 2) {
        multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.Home.home30", backgroundColor:"#00A0DC", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.Home.home30", backgroundColor:"#FFFFFF", nextState:"turningOn", defaultState: true
                attributeState "turningOn", label:'Turning On', action:"switch.off", icon:"st.Home.home30", backgroundColor:"#00A0DC", nextState:"turningOn"
                attributeState "turningOff", label:'Turning Off', action:"switch.on", icon:"st.Home.home30", backgroundColor:"#FFFFFF", nextState:"turningOff"
            }
        }

        standardTile("explicitOn", "device.switch", width: 2, height: 2, decoration: "flat") {
            state "default", label: "On", action: "switch.on", icon: "st.Home.home30", backgroundColor: "#ffffff"
        }
        standardTile("explicitOff", "device.switch", width: 2, height: 2, decoration: "flat") {
            state "default", label: "Off", action: "switch.off", icon: "st.Home.home30", backgroundColor: "#ffffff"
        }

        main(["switch"])
        details(["switch", "explicitOn", "explicitOff"])

    }
}

def parse(String description) {
    log.debug "parse(${description})"
}

def on() {
    log.debug "Called on()"
    sendCmd("Lights", "Lights1")
    //sendEvent(name: "switch", value: "on", isStateChange: true)
}

def off() {
    log.debug "Called off()"
    sendCmd("Lights", "Lights1")
    //sendEvent(name: "switch", value: "off", isStateChange: true)
}

def installed() {
    on()
}

private sendCmd(device, command) {
    log.debug "sendCmd(${device}, ${command}) " + getHostAddress()
    //def host = getHostAddress()
    //def port = host.split(":")[1]
    def path = "https://ebixio.com/n/HomeAutomation/ir/${device}/${command}""
    def headers = [:]
    headers.put("HOST", getHostAddress())

    def hubAction = new physicalgraph.device.HubAction(
        method: "POST",
        path: path,
        headers: headers
    )
    return hubAction
}

private getHostAddress() {
    def parts = device.deviceNetworkId.split(":")
    def ip = convertHexToIP(parts[0])
    def port = convertHexToInt(parts[1])
    return ip + ":" + port
}

private Integer convertHexToInt(hex) {
    Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

