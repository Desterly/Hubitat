/**
 *  Copyright 2019 Stressfactor
 *
 *  Modified from Generic drivers for Smartthings and Hubitat for Ikea Tradfri Bulbs
 *
 */

metadata {
	definition (name: "Tradfri bulb", namespace: "stressfactor", author: "Desterly") {

		capability "Actuator"
		capability "Color Temperature"
		capability "Configuration"
		capability "Polling"
		capability "Refresh"
		capability "Switch"
		capability "Switch Level"
		capability "Health Check"

		attribute "colorName", "string"

		// Ikea
		fingerprint inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B05, 1000", outClusters: "0005, 0019, 0020, 1000", manufacturer: "IKEA of Sweden", model: "TRADFRI bulb E26 WS clear 950lm", deviceJoinName: "IKEA TR휚FRI White Spectrum LED Bulb"
		fingerprint inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B05, 1000", outClusters: "0005, 0019, 0020, 1000", manufacturer: "IKEA of Sweden", model: "TRADFRI bulb GU10 WS 400lm", deviceJoinName: "IKEA TR휚FRI White Spectrum LED Bulb"
		fingerprint inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B05, 1000", outClusters: "0005, 0019, 0020, 1000", manufacturer: "IKEA of Sweden", model: "TRADFRI bulb E12 WS opal 400lm", deviceJoinName: "IKEA TR휚FRI White Spectrum LED Bulb"
		fingerprint inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B05, 1000", outClusters: "0005, 0019, 0020, 1000", manufacturer: "IKEA of Sweden", model: "TRADFRI bulb E26 WS opal 980lm", deviceJoinName: "IKEA TR휚FRI White Spectrum LED Bulb"
        fingerprint inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B05, 1000", outClusters: "0005, 0019, 0020, 1000", manufacturer: "IKEA of Sweden", model: "TRADFRI bulb E26 WS opal 1000lm", deviceJoinName: "IKEA TR휚FRI White Spectrum LED Bulb"
		fingerprint profileId: "C05E", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B05, 1000", outClusters: "0005, 0019, 0020, 1000", manufacturer: "IKEA of Sweden", model: "TRADFRI bulb E27 WS clear 950lm", deviceJoinName: "IKEA TR휚FRI White Spectrum LED Bulb"
		fingerprint profileId: "C05E", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B05, 1000", outClusters: "0005, 0019, 0020, 1000", manufacturer: "IKEA of Sweden", model: "TRADFRI bulb E14 WS opal 400lm", deviceJoinName: "IKEA TR휚FRI White Spectrum LED Bulb"
		fingerprint profileId: "C05E", inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, 0B05, 1000", outClusters: "0005, 0019, 0020, 1000", manufacturer: "IKEA of Sweden", model: "TRADFRI bulb E27 WS opal 980lm", deviceJoinName: "IKEA TR휚FRI White Spectrum LED Bulb"

	}

}


// Parse incoming device messages to generate events
def parse(String description) {
	log.debug "description is $description"
	def event = zigbee.getEvent(description)
	if (event) {
		if (event.name == "colorTemperature") {
			event.unit = "K"
			setGenericName(event.value)
		}
		sendEvent(event)
	}
	else {    
		//log.warn "DID NOT PARSE MESSAGE for description : $description"
		//log.debug zigbee.parseDescriptionAsMap(description)
	}
}

def off() {
	zigbee.off() + ["delay 1500"] + zigbee.onOffRefresh()
}

def on() {
	zigbee.on() + ["delay 1500"] + zigbee.onOffRefresh()
}

def setLevel(value, rate = null) {
	zigbee.setLevel(value) + zigbee.onOffRefresh() + zigbee.levelRefresh()
}

def refresh() {
	def cmds = zigbee.onOffRefresh() + 
               zigbee.levelRefresh() + 
               zigbee.colorTemperatureRefresh()
               zigbee.onOffConfig() + 
               zigbee.levelConfig()
	

	cmds
}

def poll() {
	zigbee.onOffRefresh() + zigbee.levelRefresh() + zigbee.colorTemperatureRefresh()
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	return zigbee.levelRefresh()
}

def healthPoll() {
	log.debug "healthPoll()"
	def cmds = poll()
	cmds.each{ sendHubCommand(new hubitat.device.HubAction(it))}
}

def configureHealthCheck() {
	Integer hcIntervalMinutes = 12
	if (!state.hasConfiguredHealthCheck) {
		log.debug "Configuring Health Check, Reporting"
		unschedule("healthPoll", [forceForLocallyExecuting: true])
		runEvery5Minutes("healthPoll", [forceForLocallyExecuting: true])
		// Device-Watch allows 2 check-in misses from device
		sendEvent(name: "checkInterval", value: hcIntervalMinutes * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
		state.hasConfiguredHealthCheck = true
	}
}

def configure() {
	log.debug "configure()"
	configureHealthCheck()
	// Implementation note: for the Eaton Halo_LT01, it responds with "switch:off" to onOffConfig, so be sure this is before the call to onOffRefresh
	zigbee.onOffConfig() + zigbee.levelConfig() + zigbee.onOffRefresh() + zigbee.levelRefresh() + zigbee.colorTemperatureRefresh()
}

def updated() {
	log.debug "updated()"
	configureHealthCheck()
}

def setColorTemperature(rawValue) {
    log.debug "setColorTemperature ${rawValue}"
    def rate = transitionTime?.toInteger() ?: 1000
    // Set the values to the three possiblities - sync'd to values in Alexa
    if (rawValue <= 2200) {
        rawValue = 2200
    } else if (rawValue <=2700) {
        rawValue = 2700
    } else {
        rawValue = 4000
    }
    def value = intTo16bitUnsignedHex((1000000/rawValue).toInteger())
    def cmd = []
    def isOn = device.currentValue("switch") == "on"

    if (isOn){
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x000A {${value} ${intTo16bitUnsignedHex(rate / 100)}}",
                "delay ${rate + 400}",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0007 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0008 {}"
        ]
    } else {
        cmd = [
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x000A {${value} 0x0100}", "delay 200",
                "he cmd 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 1 {}","delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0006 0 {}","delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0007 {}", "delay 200",
                "he rattr 0x${device.deviceNetworkId} 0x${device.endpointId} 0x0300 0x0008 {}"
        ]
    }
    setGenericName(rawValue)
    state.lastCT = value
    return cmd    
}

//Naming based on the Temps in Alexa
def setGenericName(value){
	if (value != null) {
		def genericName = ""
		if (value <= 2203) {
			genericName = "Warm White"
		} else if (value <= 2703) {
			genericName = "Soft White"
		} else {
			genericName = "White"
		}
		sendEvent(name: "colorName", value: genericName, displayed: false)
	}
}

def intTo16bitUnsignedHex(value) {
    def hexStr = zigbee.convertToHexString(value.toInteger(),4)
    return new String(hexStr.substring(2, 4) + hexStr.substring(0, 2))
}

def intTo8bitUnsignedHex(value) {
    return zigbee.convertToHexString(value.toInteger(), 2)
}
