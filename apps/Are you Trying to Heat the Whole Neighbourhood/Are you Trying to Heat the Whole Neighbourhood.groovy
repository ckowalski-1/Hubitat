/**
 *  Are you Trying to Heat the Whole Neighbourhood!
 *
 *  Copyright 2019 Christopher Kowalski
 *
 *  Simple app to put a Nest thermostat in ECO mode iif a door or window is left open, and restore
 *  once all closed
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Are you Trying to Heat the Whole Neighbourhood!",
    namespace: "ckowalski-1",
    author: "Christopher Kowalski",
    description: "If doors or windows are left open turn a Nest thermostate to ECO mode, and turn if back on when things are closed",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png",
)

preferences {
    page(name: "Configure the app", title: "Configure the app", install: true, uninstall: true) {
    
		section("Which Nest Thermostat") {
            input "thermostat", "capability.thermostat", title: "Thermostat", multiple: false, required: true
		}
		
		section("Which doors or windows") {
            input "doors", "capability.contactSensor", title: "Doors and windows", multiple: true, required: true
		}
		
		section("Set to ECO if anything left open for ..."){
            input "toEcoMins", "number", title: "Minutes?", defaultValue: 0
        }

		section("Restore to last value when everything has been closed for ..."){
            input "fromEcoMins", "number", title: "Minutes?", defaultValue: 0
        }

        section("Send a message on a change?") { 
            input "notificationDevice", "capability.notification", 
                multiple: true, 
                title: "Who should get the message?", 
                required: false
        }
        
        section("Name and Mode...") {
            label title: "Assign a name", required: false
            input "modes", "mode", title: "Only when mode is", multiple: true, required: false
        }
    }
    
}

//supportedNestThermostatModes : [off, heat, cool, auto, eco]

def installed() {
	initialize()
}

def updated() {
	initialize()
}

def initialize() {
    unsubscribe()
    subscribe(doors, "contact", contactHandler)
	atomicState.stateVar = "idle"
	log.debug "initialize: back in idle state"
}


//
// Contact Handler (also the main state handler)
//
// If a door is opened we start the timer to turn to ECO mode (checkTurnEco)
// If a door is closed we
//		Check if any door is open, if doors are open do nothing
//		If all doors are closed we start the timer to restore (checkRestore)
// There are additional states to handel manual updates to the thermostate
//
def contactHandler(evt) {
	def nestThermostatMode = thermostat.currentValue("nestThermostatMode")
	log.debug "contactHandler: $evt.name: $evt.value"
	log.debug "contactHandler: In state $atomicState.stateVar"
	log.debug "contactHandler: nestThermostatMode $nestThermostatMode"
	
	
	switch(atomicState.stateVar) {
		case "idle":
			// If a door was closed we do nothing
			// If a door was opened and in eco we stay here
			// If a door was opened and we are NOT in eco mode or off mode
				// goto the ecoScheduled state and schedule a time
			if ( (evt.value == "open") && (nestThermostatMode != "off") && (nestThermostatMode != "eco") ) {
				atomicState.stateVar = "ecoScheduled"
				log.debug "contactHandler: in idle and door opened goto ecoScheduled and schedule a change to ECO"
				if(toEcoMins) {
					runIn(toEcoMins*60, checkTurnEco)
					log.debug "contactHandler: Scheduling turn to ECO"
				} else {
					checkTurnEco()
				}
			} else {
				log.debug "contactHandler: in idle and door opened or closed, but thermostat not on so do nothing"
			}
			break;
			
		case "ecoScheduled":
    		// If all doors are closed while in this state
    		// jump back to idle.  This will mean when we run "checkTurnEco"
    		// we won't actually do anything
    		// Or if a door is opened/closed and we're already in ECO/off mode
    		// Go to manualAdjust
    		if ( doorState("all", "closed") ) {
    			atomicState.stateVar = "idle"
    			log.debug "contactHandler: in ecoScheduler and all door closed, go to idle"
			} else if ( (nestThermostatMode == "off") || (nestThermostatMode == "eco") ) {
    			atomicState.stateVar = "manualAdjust"
    			log.debug "contactHandler: in ecoScheduled and eco or off mode, go to manualAdjust"
    		}
    		break;
    		
	    case "inEco":
    		// When here we go to the manual state if not in eco and not all doors are closed
    		if ( doorState("any", "open") && (nestThermostatMode != "eco") ) {
				atomicState.stateVar = "manualAdjust"
				log.debug "contactHandler: in inEco state, not in ECO mode, go to manualAdjust state"
			// Go to idle if all doors closed and not in eco
			} else if ( doorState("all", "closed") && (nestThermostatMode != "eco") ) {
				atomicState.stateVar = "idle"
				log.debug "contactHandler: in inEco state and all doors closed and not in eco, go to idle"
			// If all does are closed and we're in eco we schedule a restore and go to that state
			} else if ( doorState("all", "closed") && (nestThermostatMode == "eco") ) {
				log.debug "contactHandler: in inEco state and all doors closed and in eco mode, schedule restore"
				atomicState.stateVar = "restoreScheduled"
				if(fromEcoMins) {
					runIn(fromEcoMins*60, checkRestore)
            		log.debug "Scheduling restore"
				} else {
					checkRestore()
				}
			}
    		break;
    		
		case "restoreScheduled":
    		// Any Open and in eco mode, go back to "inEco"
    		if ( doorState("any", "open") && (nestThermostatMode == "eco") ) {
    			atomicState.stateVar = "inEco"
				log.debug "contactHandler: in restoreScheduled state, and a door opened, go to inEco state"
			// Any open and not in eco, go to manual state
			} else if ( doorState("any", "open") && (nestThermostatMode != "eco") ) {
    			atomicState.stateVar = "manualAdjust"
				log.debug "contactHandler: in restoreScheduled state, and a door opened and not in eco mode , go to manualAdjust state"
			}
    		break;
    		
	  	case "manualAdjust":
			// Go to idle if all doors closed go back to idle
			if ( doorState("all", "closed") ) {
				atomicState.stateVar = "idle"
				log.debug "contactHandler: in manualAdjust state and all doors closed, go to idle"
			}
    		break;
    		
		default:
    		log.debug "ERROR!!!! In an unknown state"
    }
}

//
// Check if we should go into ECO mode
//
def checkTurnEco() {
	def nestThermostatMode = thermostat.currentValue("nestThermostatMode")
	log.debug "checkTurnEco: In state $atomicState.stateVar"
	log.debug "checkTurnEco: nestThermostatMode $nestThermostatMode"
	
	// Only do something if in the "ecoScheduled" state
	// If not in eco or off mode, put into eco mode
	if ( (atomicState.stateVar == "ecoScheduled") && (nestThermostatMode != "off") && (nestThermostatMode != "eco") ) {
		// Get the current settings so they can be restored
		atomicState.lastNestThermostatMode = thermostat.currentValue("nestThermostatMode")
		log.debug "checkTurnEco: lastNestThermostatMode is $atomicState.lastNestThermostatMode"
		// Set to ECO mode
        log.debug "checkTurnEco: Setting to ECO"
		atomicState.stateVar = "inEco"
		thermostat.eco()
		if (notificationDevice) {notificationDevice.deviceNotification("Put thermostat into ECO mode")}
	} else if ( (atomicState.stateVar == "ecoScheduled") && ((nestThermostatMode == "off") || (nestThermostatMode == "eco")) ) {
		log.debug "checkTurnEco: Thermostat was already off or in ECO mode so did nothing, go to manualAdjust"
		atomicState.stateVar = "manualAdjust"
	} else {
		log.debug "checkTurnEco: Not in ecoScheduled state, do nothing"
	}
}


//
// Check if we should restore from Eco
//
def checkRestore() {
	def nestThermostatMode = thermostat.currentValue("nestThermostatMode")
	log.debug "checkRestore: In state $atomicState.stateVar"
	log.debug "checkRestore: nestThermostatMode $nestThermostatMode"

    // Only doo something in the restoreScheduled state
	if ( (atomicState.stateVar == "restoreScheduled") ) {
		atomicState.stateVar = "idle"
		// If Not in ECO mode, someone updated while in ECO mode and we'll respect the new setting
		// So only restore if in ECO mode
		// Restore state.lastNestThermostatMode
		if(nestThermostatMode == "eco") {
			if(atomicState.lastNestThermostatMode == "auto") {
				thermostat.auto()
				log.debug "checkRestore: Restoring to Auto"
				if (notificationDevice) {notificationDevice.deviceNotification("Restored thermostat to Auto")}
			} else if(atomicState.lastNestThermostatMode == "heat") {
				thermostat.heat()
				log.debug "checkRestore: Restoring to Heat"
				if (notificationDevice) {notificationDevice.deviceNotification("Restored thermostat to Heat")}
			} else if(atomicState.lastNestThermostatMode == "cool") {
				thermostat.cool()
				log.debug "checkRestore: Restoring to Cool"
				if (notificationDevice) {notificationDevice.deviceNotification("Restored thermostat to Cool")}
			}
		} else {
			log.debug "checkRestore: No change to thermostate, not in ECO mode"
			if (notificationDevice) {notificationDevice.deviceNotification("Did not restore thermostat, not in ECO mode")}
		}
	}
}


//
// Check door state.  Can check:
// 		1) All or any door
//		2) Open or closed
//
def doorState(allOrAny, openOrClosed) {
	// Are we checking open or closed
	def checkClosed = false
	if (openOrClosed == "closed") {
		log.debug "doorState: checking for closed doors"
		checkClosed = true
	} else {
		log.debug "doorState: checking for open doors"
	}
	
	// Are we checking all or any
	def allDoors = false
	if (allOrAny == "all") {
		log.debug "doorState: checking for all doors"
		allDoors = true
	} else {
		log.debug "doorState: checking for any doors"
	}
	
    // Find if any door is open (or closed)
    def listOfDoors = doors.findAll { it?.latestValue("contact") == "${checkClosed ? "closed" : "open"}" }
    log.debug "doorState: list of matched doors: $listOfDoors"
    
    
    // If configured for all doors, check all doors
	if (allDoors) {
		log.debug "Number of doors in the config is: ${doors.size()}"
		log.debug "Number of doors open/closed is: ${listOfDoors.size()}"
		return ((listOfDoors.size() == doors.size()) ? true : false)
	} else {
		return (listOfDoors ? true : false)
	}
}


