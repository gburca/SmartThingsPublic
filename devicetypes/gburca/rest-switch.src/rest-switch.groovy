/**
 * Copyright 2019 Gabriel Burca
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
  log.debug("parse(${description})")
  //def msg = parseLanMessage(description)
}

def on() {
  log.debug("Called on()")
  pushButton()
  sendEvent(name: "switch", value: "on", isStateChange: true)
}

def off() {
  log.debug("Called off()")
  pushButton()
  sendEvent(name: "switch", value: "off", isStateChange: true)
}

def installed() {
  //on()
}

private pushButton() {
  // When installing a device that uses this driver, we get to fill in a few
  // things, including the mandatory "Device Network Id". We use that to
  // identify what device we want to control, and what command we want to
  // send to it.
  def devCmd = device.deviceNetworkId.split(":")
  def irDevice = devCmd[0]
  def irCommand = devCmd[1]
  def path = "/n/HomeAutomation/ir/${irDevice}/${irCommand}"

  def headers = [:]
  // Doesn't work with HTTPS, or with hostname instead of IP:
  //    ebixio.com:443 won't work.
  // We hit the internal node.js server directly (bypassing Apache).
  // The HOST header is mandatory for HTTP.
  headers.put("HOST", "192.168.1.1:8001")

  def params = [
      method: "POST", // "GET"
      path: path,
      headers: headers
  ]

  def hubAction = new physicalgraph.device.HubAction(params, null,
                                                     [callback: "cb_handler"])

  //log.debug("pushButton(${irDevice}, ${irCommand}) " + path)
  //log.debug("label: " + device.label)
  //log.debug("name: " + device.name)
  //log.debug("callback address: " + getCallBackAddress() + " ip: " + getDataValue("ip") + " port: " + getDataValue("port"))
  //log.debug("sendHubCommand(): " + hubAction.toString())

  sendHubCommand(hubAction)
}

def cb_handler(physicalgraph.device.HubResponse hubResponse) {
  //log.debug("callbackHandler() called")
  def msg = hubResponse

  def headersAsString = msg.header // => headers as a string
  def headerMap = msg.headers      // => headers as a Map
  def body = msg.body              // => request body as a string
  def status = msg.status          // => http status code of the response
  def json = msg.json              // => any JSON included in response body, as a data structure of lists and maps
  def xml = msg.xml                // => any XML included in response body, as a document tree structure
  def data = msg.data              // => either JSON or XML in response body (whichever is specified by content-type header in response)

  //log.debug("status: ${status} headers: ${headersAsString} body: ${body}")
}

// gets the address of the device
/*
private getCallBackAddress() {
  // This gives us the Hub's local IP and port
  return device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
}

private getHostAddress() {
  def ip = getDataValue("ip")
  def port = getDataValue("port")

  if (!ip || !port) {
    // Make sure the Device Network Id is actually ip_address:port before using this.
    def parts = device.deviceNetworkId.split(":")
    if (parts.length == 2) {
      ip = parts[0]
      port = parts[1]
    } else {
      log.warn "Can't figure out ip and port for device: ${device.id}"
    }
  }

  log.debug "Using IP: $ip and port: $port for device: ${device.id}"
  return convertHexToIP(ip) + ":" + convertHexToInt(port)
}

private Integer convertHexToInt(hex) {
  return Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
  return [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
  String hexport = port.toString().format( '%04x', port.toInteger() )
  return hexport
}

*/