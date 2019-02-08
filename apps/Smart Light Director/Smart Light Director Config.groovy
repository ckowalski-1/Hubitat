/**
 *  Smart Light Director Config (Child App)
 *
 *  Copyright 2015 Christopher Kowalski
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
    name: "Smart Light Director Config",
    namespace: "ckowalski-1",
    author: "Christopher Kowalski",
    description: "Smart Light Director Application",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png",
	parent: "ckowalski-1:Smart Light Director",
)

preferences {
	page(name: "firstPage")
    page(name: "pageAbout")
	page(name: "secondaryEnablePage")
	page(name: "nightlightConfigPage")
    page(name: "dimmerConfigPage")
    page(name: "installPage")
}


// First page
def firstPage() {
	dynamicPage(name: "firstPage", title: "Select lights and triggers", nextPage: "secondaryEnablePage", install: false, uninstall: state.installed) {
        section("About") {
            href "pageAbout", title:"About", description:"Tap to open"
        }
        section("Control these lights (switches) ...") {
            input "lights", "capability.switch", multiple: true, required: false
        }
        section("Control these lights (dimmers) ...") {
            input "dimmers", "capability.switchLevel", multiple: true, required: false
        }
        section("Turning on if there's movement ..."){
            input "motionSensor", "capability.motionSensor", title: "Motion?", multiple: true, required: false
        }
        section("Or when a door opens ...") {
            input "doors", "capability.contactSensor", title: "Door?", multiple: true, required: false
            input "doorCloseInstead", "bool", defaultValue: "false", title: "Activate On Door closed?", required: true
			input "allDoors", "bool", defaultValue: "false", title: "All Door Must be Open or Closed?", required: true
        }
        section("Or when someone arrives") {
            input "people", "capability.presenceSensor", multiple: true, required:false
        }
        section("And then off when it's light, or there's been no movement, or all doors are closed for ..."){
            input "delayMinutes", "number", title: "Minutes?", defaultValue: 0
        }
        section("Persistent Mode?") {
        	input "perst", "bool", defaultValue: "true", required: false
        }
        section("Pick the primary method the automation will be enabled") {
            input("enableType", "enum", options: [
                "alwaysEnabledChoice":"Always Enabled",
                "lightSensorChoice":"Light Sensor",
                "modeEnableChoice":"Mode Change",
                "sunChoice": "Sun Rise/Set"], defaultValue: "alwaysEnabledChoice", submitOnChange: true)
		}
        
        // Options are based on enable type
        if (enableType == "alwaysEnabledChoice") {
			section("No additional setup for Always Enabled"){}

        } else if (enableType == "lightSensorChoice") {
            section("Pick a light sensor"){
                input "lightSensor", "capability.illuminanceMeasurement", required: true
                input "actionOnEnable", "enum", title: "What to do on enable", required: true, options: enableActionTypes()
                input "actionOndisable", "enum", title: "What to do on disable", required: true, options: enableActionTypes()
            }

        } else if (enableType == "modeEnableChoice") {
            section("Pick the enable and disable modes"){
            	input "enableMode", "mode", title: "Enable Mode", required: true, mutiple: false
            	input "disableMode", "mode", title: "Disable Mode", required: true, mutiple: false
            	input "actionOnEnable", "enum", title: "What to do on enable", required: true, options: enableActionTypes()
            	input "actionOndisable", "enum", title: "What to do on disable", required: true, options: enableActionTypes()
            }

        // Sun set/rise
        } else if (enableType == "sunChoice") {
            section ("Sunrise offset (optional)...") {
                input "sunriseOffsetValue", "text", title: "HH:MM", required: false
                input "sunriseOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
            }
            section ("Sunset offset (optional)...") {
                input "sunsetOffsetValue", "text", title: "HH:MM", required: false
                input "sunsetOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
            }
            section ("Zip code (optional, defaults to location coordinates when location services are enabled)...") {
                input "zipCode", "text", title: "Zip code", required: false
            }
            section ("What to do on enable and disable?") {
                input "actionOnEnable", "enum", title: "What should happne on enable", required: true, options: enableActionTypes()
                input "actionOndisable", "enum", title: "What should happen on disable", required: true, options: enableActionTypes()
            }
        }

        section("Enable a Nightlight mode (If Enabled Config will be on a seperate page)") {
        	input "nightlightModeConfig", "bool", defaultValue: "false", required: false
		}
    }
}

private enableActionTypes() {
     return [
            "enableNothing":"Nothing",
            "lightsOn":"On",
            "lightsOnFull":"Full On",
            "lightsOff": "Off",
            "lightsOffFull": "Full Off",
            "lightsOnThenOff": "Full on Then Full Off"
            ]
}

// Show "About" page
def pageAbout() {

    def textAbout =
        "Control when a light turns on and off with separate \"enabled\" and \"action\" conditions. " +
        "For example a light might turn on when someone arrives (action) but only when it's dark (enabled)." +
        "\r\n\r\nEnable choices are time (based on sunrise/set), light level, always, or mode change.  The " +
        "automatation can also be optionally enabled/disabled by double tapping on or off a switch." +
        "\r\nAction choices are motion, open/close, arrival, or mode change." +
        "\r\n\r\nWhen turned on the light will remain on for a set number of minutes after all actions are cleared. " +
        "For example if two different open/close sensors are used, the light won't turn off until after BOTH are closed." +
        "\r\n\r\nPersistent Mode means the light will always be \"turned on\" on a action even it has been manually turned off. " +
        "If you disable presistent mode and manually turn off the light it won't turn back on until all actions clear for the configured time" +
        "\r\n\r\nYou can also control what happens when automation is enabled.  On means dimmers set to the on value, Full On means dimmers set to 100% " +
        "Likewise Off means dimmers set to the off value and Full Off means dimmers set to 0%"

    def pageProperties = [
        name        : "pageAbout",
        title       : "About",
        nextPage    : "firstPage",
        install     : false,
        uninstall   : state.installed
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph textAbout
        }
    }
}


//
// Show Secondary Enable Config Page
//
def secondaryEnablePage() {

    def secondaryTextAbout =
        "Configure a Secondary Enable Method (optional)."

    def secondaryPageProperties = [
        name        : "secondaryEnablePage",
        title       : "Configure Secondary Enable Methods",
        nextPage    : nightlightConfigPage,
        install     : false,
        uninstall   : state.installed
    ]

    return dynamicPage(secondaryPageProperties) {
        section {
            paragraph secondaryTextAbout
        }
        
        section("Allow automation to be enabled or disabled via - A switch") {
        	input "secondaryToggle", "bool", defaultValue: "false", title: "Toggle a Switch?", required: false, submitOnChange: true
        }
        if (secondaryToggle) {
            section("Select switch to enable/disable automation with on/off toggle (optional)") {
                input "toggleSwitch", "capability.switch", multiple: false, required: true
                input "STactionOnEnable", "enum", title: "What to do on enable (On then off)", required: true, options: enableActionTypes()
                input "STactionOndisable", "enum", title: "What to do on disable (off then on)", required: true, options: enableActionTypes()
            }
        }
        
        section("Allow automation to be enabled or disabled via - A single button (toggle on and off)") {
        	input "secondaryToggleButton", "bool", defaultValue: "false", title: "Single button?", required: false, submitOnChange: true
        }
        if (secondaryToggleButton) {
        	section("Enable/disable on button press") {
            	input "toggleButtonDevice", "capability.pushableButton", multiple: false, required: true
                input "toggleButtonNumber", "number", title: "Button Number", multiple: false, required: true
				input "toggleButtonDoubleTap", "bool", defaultValue: "false", title: "Double tap instead of push?", required: true
                input "TBactionOnEnable", "enum", title: "What to do on enable", required: true, options: enableActionTypes()
                input "TBactionOndisable", "enum", title: "What to do on disable", required: true, options: enableActionTypes()
            }
        }
        
        section("Allow automation to be enabled or disabled via - Buttons (seperate on and off buttons)") {
        	input "secondarySeperateButton", "bool", defaultValue: "false", title: "Seperate Buttons?", required: false, submitOnChange: true
        }
        if (secondarySeperateButton) {
        	section("Enable with one button, disable with a different button") {
            	input "enableButtonDevice", "capability.pushableButton", title: "Enable Button", multiple: false, required: true
                input "enableButtonNumber", "number", title: "Enable Button Number", multiple: false, required: true
                input "disableButtonDevice", "capability.pushableButton", title: "Disable Button", multiple: false, required: true
                input "disableButtonNumber", "number", title: "Disable Button Number", multiple: false, required: true
				input "enableButtonDoubleTap", "bool", defaultValue: "false", title: "Double tap instead of push?", required: true
                input "SBactionOnEnable", "enum", title: "What to do on enable", required: true, options: enableActionTypes()
                input "SBactionOndisable", "enum", title: "What to do on disable", required: true, options: enableActionTypes()
            }
        }
    }
}


//
// Show "Nightlight" setup page
//
def nightlightConfigPage() {
    if (nightlightModeConfig == false) {
		return dimmerConfigPage()
	}
    
    def nightlightTextAbout =
        "Configure how nightligh mode should be enabled and disabled. " +
		"Mode change is the only primary means to enable nighlight mode at this time."

    def nightlightPageProperties = [
        name        : "nightlightConfigPage",
        title       : "Configure Nightlight Enable/Disable",
        nextPage    : dimmerConfigPage,
        install     : false,
        uninstall   : state.installed
    ]

    return dynamicPage(nightlightPageProperties) {
        section {
            paragraph nightlightTextAbout
        }
        
		section("Pick the enable and disable modes"){
           	input "nightlightEnableMode", "mode", title: "Enable Mode", required: true, mutiple: false
           	input "nightlightDisableMode", "mode", title: "Disable Mode", required: true, mutiple: false
           	input "nightlightActionOnEnable", "enum", title: "What to do on enable", required: true, options: enableActionTypes()
           	input "nightlightActionOndisable", "enum", title: "What to do on disable", required: true, options: enableActionTypes()
        }
		
		section("Set how long to wait before turning off the light in Nightlight mode"){
            input "nightlightDelayMinutes", "number", title: "Minutes?", defaultValue: 0
        }
		
		section("Allow nightlight to be enabled or disabled via - A switch") {
        	input "nightlightSecondaryToggle", "bool", defaultValue: "false", title: "Toggle a Switch?", required: false, submitOnChange: true
        }
        if (nightlightSecondaryToggle) {
            section("Select switch to enable/disable automation with on/off toggle (optional)") {
                input "nightlightToggleSwitch", "capability.switch", multiple: false, required: true
                input "nightlightSTactionOnEnable", "enum", title: "What to do on enable (On then off)", required: true, options: enableActionTypes()
                input "nightlightSTactionOndisable", "enum", title: "What to do on disable (off then on)", required: true, options: enableActionTypes()
            }
        }
        
        section("Allow nightlight to be enabled or disabled via - A single button (toggle on and off)") {
        	input "nightlightSecondaryToggleButton", "bool", defaultValue: "false", title: "Single button?", required: false, submitOnChange: true
        }
        if (nightlightSecondaryToggleButton) {
        	section("Enable/disable on button press") {
            	input "nightlightToggleButtonDevice", "capability.pushableButton", multiple: false, required: true
                input "nightlightToggleButtonNumber", "number", title: "Button Number", multiple: false, required: true
				input "nightlightToggleButtonDoubleTap", "bool", defaultValue: "false", title: "Double tap instead of push?", required: true
                input "nightlightTBactionOnEnable", "enum", title: "What to do on enable", required: true, options: enableActionTypes()
                input "nightlightTBactionOndisable", "enum", title: "What to do on disable", required: true, options: enableActionTypes()
            }
        }
        
        section("Allow automation to be enabled or disabled via - Buttons (seperate on and off buttons)") {
        	input "nightlightSecondarySeperateButton", "bool", defaultValue: "false", title: "Seperate Buttons?", required: false, submitOnChange: true
        }
        if (nightlightSecondarySeperateButton) {
        	section("Enable with one button, disable with a different button") {
            	input "nightlightEnableButtonDevice", "capability.pushableButton", title: "Enable Button", multiple: false, required: true
                input "nightlightEnableButtonNumber", "number", title: "Enable Button Number", multiple: false, required: true
                input "nightlightDisableButtonDevice", "capability.pushableButton", title: "Disable Button", multiple: false, required: true
                input "nightlightDisableButtonNumber", "number", title: "Disable Button Number", multiple: false, required: true
				input "nightlightEnableButtonDoubleTap", "bool", defaultValue: "false", title: "Double tap instead of push?", required: true
                input "nightlightSBactionOnEnable", "enum", title: "What to do on enable", required: true, options: enableActionTypes()
                input "nightlightSBactionOndisable", "enum", title: "What to do on disable", required: true, options: enableActionTypes()
            }
        }
    }
}


//
// Show "Configure Dimmers" setup page
//
def dimmerConfigPage() {
	if (dimmers == null) {
		return installPage()
	}
    
    def dimmerstTextAbout =
        "Set desired dimming levels for each dimmer. Dimming values " +
        "are between 0 (off) and 99 (full brightness)."

    def dimmersPageProperties = [
        name        : "dimmerConfigPage",
        title       : "Configure Dimmers",
        nextPage    : installPage,
        install     : false,
        uninstall   : state.installed
    ]

    return dynamicPage(dimmersPageProperties) {
        section {
            paragraph dimmerstTextAbout
        }
        
        settings.dimmers?.each() {
            def name = it as String
            section("${name} Config", hideable:true, hidden:false) {
              input "${name}_OnVal", "number", title:"${name} On Value", required:true
              input "${name}_OffVal", "number", title:"${name} Off Value", required:true
            }
			if (nightlightModeConfig) {
				section("${name} Nightlight Config", hideable:true, hidden:false) {
              		input "${name}_nightlightOnVal", "number", title:"${name} On Value", required:true
              		input "${name}_nightlightOffVal", "number", title:"${name} Off Value", required:true
				}
			}
        }
    }
}


//
// Page to configure modes and names
//
def installPage() {
	if (!overrideLabel) {
        // if the user selects to not change the label, give a default label
        def l = defaultLabel()
        log.debug "will set default label of $l"
        app.updateLabel(l)
    }
    dynamicPage(name: "installPage", title: "Name app and configure modes", install: true, uninstall: state.installed) {
        if (overrideLabel) {
            section("Automation name") {
                label title: "Enter custom name", defaultValue: app.label, required: false
            }
        } else {
            section("Automation name") {
                paragraph app.label
            }
        }
        section {
            input "overrideLabel", "bool", title: "Edit automation name", defaultValue: "false", required: "false", submitOnChange: true
        }

		section {
            input "modes", "mode", title: "Only when mode is", multiple: true, required: false
        }
    }
}

//
// A method that will set the default label of the automation.
// It uses the lights selected and action to create the automation label
//
def defaultLabel() {

	def lightsLabel = ""
	// Build label based on lights first
	if (lights != null) {
    	lightsLabel = settings.lights.size() == 1 ? lights[0].displayName : lights[0].displayName + ", etc..."
	// Dimmer next
	} else if (dimmers != null) {
    	lightsLabel = settings.dimmers.size() == 1 ? dimmers[0].displayName : dimmers[0].displayName + ", etc..."
    // Or nothing
    } else {
    	lightsLabel = "Nothing"
    }

	return "Automating $lightsLabel"
}



def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
    initChild()
}


def initChild() {

	state.installed = true
    state.lastStatus = "unknown"
    // If parentEnable is already set use it (i.e. on update)
	// Otherwise set to true
	if (state.parentEnable == null) {
		state.parentEnable = true
	}
    state.adjustLastTime = 0
	// If parentEnable is already set use it (i.e. on update)
	// Otherwise set to true
	if (state.nightlightEnabled == null) {
		state.nightlightEnabled = false
	}
    
    // if the user did not override the label, set the label to the default
    if (!overrideLabel) {
        app.updateLabel(defaultLabel())
    }

	if (motionSensor) {
		subscribe(motionSensor, "motion", motionHandler)
	}
	if (doors) {
		subscribe(doors, "contact", contactHandler)
	}
    
    if (people) {
    	subscribe(people, "presence", presenceHandler)
    }
    
    // Config based on the enable type
	if (enableType == "alwaysEnabledChoice") {
    	state.alwaysEnabled = true
        state.appEnabled = true
        def lightSensor = null
        log.debug "Initialize: Always Enabled"
    } else if (enableType == "lightSensorChoice") {
    	state.alwaysEnabled = false
        state.appEnabled = false
		subscribe(lightSensor, "illuminance", illuminanceHandler, [filterEvents: false])
        log.debug "Initialize: Using Light Sensor"
	} else if (enableType == "modeEnableChoice") {
    	subscribe(location, modeHandler)
        state.appEnabled = false
		def lightSensor = null
        log.debug "Initialize: Using Mode to Enable"
    } else if (enableType == "sunChoice") {
    	state.alwaysEnabled = false
        state.appEnabled = false
        def lightSensor = null
        state.riseTime = 0
        state.setTime = 0
		subscribe(location, "position", locationPositionChange)
		subscribe(location, "sunriseTime", sunriseSunsetTimeHandler)
		subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)
		astroCheck()
        // schedule an astro check every 1h to work around SmartThings missing scheduled events issues
        runEvery1Hour(astroCheck)
        // Set the initial enabled state (from astroCheck)
        state.appEnabled = state.astroEnabled
        log.debug "Initialize: Using Sun set/rise for enable"
	}
    
    log.debug "Initialize: Persistent mode: $perst"
    log.debug "Initialize: enabled: $state.appEnabled"
    
    log.debug "Initialize: Action on Primary Enable: $actionOnEnable"
    log.debug "Initialize: Action on Primary Disable: $actionOndisable"
    
    if (secondaryToggle) {
        subscribe(toggleSwitch, "switch", switchHandlerToggle)
        log.debug "Initialize: Toggle Switch: $toggleSwitch"  
        log.debug "Initialize: ST Action on Enable: $STactionOnEnable"
        log.debug "Initialize: ST Action on Disable: $STactionOndisable"
    }
    
    if (secondaryToggleButton) {
        subscribe(toggleButtonDevice, "pushed", buttonHandler)
		subscribe(toggleButtonDevice, "doubleTapped", buttonHandler)
        log.debug "Initialize: Using Toggle Button: $toggleButtonDevice"
        log.debug "Initialize: Using Toggle Button number: $toggleButtonNumber"
		log.debug "Initialize: Using Toggle Button: Double Tap Instead of push is $toggleButtonDoubleTap"
        log.debug "Initialize: TB Action on Enable: $TBactionOnEnable"
        log.debug "Initialize: TB Action on Disable: $TBactionOndisable"
    }
    
    if (secondarySeperateButton) {
        subscribe(enableButtonDevice, "pushed", buttonHandler)
		subscribe(enableButtonDevice, "doubleTapped", buttonHandler)
        subscribe(disableButtonDevice, "pushed", buttonHandler)
		subscribe(disableButtonDevice, "doubleTapped", buttonHandler)
        log.debug "Initialize: Using seperate Enable and Disable Buttons: $enableButtonDevice, $disableButtonDevice"
        log.debug "Initialize: Using seperate Enable and Disable Button numbers: $enableButtonNumber, $disableButtonNumber"
		log.debug "Initialize: Using seperate Enable and Disable Button: Double Tap Instead of push is $enableButtonDoubleTap"
        log.debug "Initialize: SB Action on Enable: $SBactionOnEnable"
        log.debug "Initialize: SB Action on Disable: $SBactionOndisable"
    }
	
	if (nightlightModeConfig) {
    	subscribe(location, nightlightModeHandler)
		log.debug "Initialize: Nighlight mode subscription done"
	}
	
	if (nightlightSecondaryToggle) {
        subscribe(nightlightToggleSwitch, "switch", nightlightSwitchHandlerToggle)
        log.debug "Initialize: nightlight Toggle Switch: $nightlightToggleSwitch"  
        log.debug "Initialize: nightlight ST Action on Enable: $nightlightSTactionOnEnable"
        log.debug "Initialize: nightlight ST Action on Disable: $nightlightSTactionOndisable"
    }
    
    if (nightlightSecondaryToggleButton) {
        subscribe(nightlightToggleButtonDevice, "pushed", buttonHandler)
		subscribe(nightlightToggleButtonDevice, "doubleTapped", buttonHandler)
        log.debug "Initialize: Using nightlight Toggle Button: $nightlightToggleButtonDevice"
        log.debug "Initialize: Using nightlight Toggle Button number: $nightlightToggleButtonNumber"
		log.debug "Initialize: Using nightlight Toggle Button: Double Tap Instead of push is $nightlightToggleButtonDoubleTap"
        log.debug "Initialize: nightlight TB Action on Enable: $nightlightTBactionOnEnable"
        log.debug "Initialize: nightlight TB Action on Disable: $nightlightTBactionOndisable"
    }
    
    if (nightlightSecondarySeperateButton) {
        subscribe(nightlightEnableButtonDevice, "pushed", buttonHandler)
		subscribe(nightlightEnableButtonDevice, "doubleTapped", buttonHandler)
        subscribe(nightlightDisableButtonDevice, "pushed", buttonHandler)
		subscribe(nightlightDisableButtonDevice, "doubleTapped", buttonHandler)
        log.debug "Initialize: Using seperate Enable and Disable Buttons for nightlight: $nightlightEnableButtonDevice, $nightlightDisableButtonDevice"
        log.debug "Initialize: Using seperate Enable and Disable Button numbers for nightlight: $nightlightEnableButtonNumber, $nightlightDisableButtonNumber"
		log.debug "Initialize: Using seperate Enable and Disable Button for nightlight: Double Tap Instead of push is $nightlightEnableButtonDoubleTap"
        log.debug "Initialize: nightlight SB Action on Enable: $nightlightSBactionOnEnable"
        log.debug "Initialize: nightlight SB Action on Disable: $nightlightSBactionOndisable"
    }
	

    enabled()
}

def locationPositionChange(evt) {
	log.trace "locationChange()"
	astroCheck()
}

def sunriseSunsetTimeHandler(evt) {
	state.lastSunriseSunsetEvent = now()
	log.debug "SmartNightlight.sunriseSunsetTimeHandler($app.id)"
	astroCheck()
}


//
// Check is there are any doors open
//
def checkDoorsOpen() {
	// Check for open doors if doors are used, else return false
	if (doors) {
    	// Find if any door is open (or closed)
        def listOfOpenDoors = doors.findAll { it?.latestValue("contact") == "${doorCloseInstead ? "closed" : "open"}" }
        log.debug "Anything open $listOfOpenDoors"
		// If configured for all doors, check all doors
		if (allDoors) {
			log.debug "Number of doors in the config is: ${doors.size()}"
			log.debug "Number of doors open/closed is: ${listOfOpenDoors.size()}"
			(listOfOpenDoors.size() == doors.size()) ? true : false
		} else {
			listOfOpenDoors ? true : false
		}
    } else {
    	return false
    }
}


//
// Check is there is any motion
//
def checkAnyMotion() {
	// Check for motion if montion sensors are used, else return false
	if (motionSensor) {
        def listOfActiveMotion = motionSensor.findAll { it?.latestValue("motion") == "active" }
        log.debug "Anything moving $listOfActiveMotion"
        listOfActiveMotion ? true : false
    } else {
    	return false
    }
}


//
// Motion Handler
//
def motionHandler(evt) {
	def lastStatus = state.lastStatus
	log.debug "$evt.name: $evt.value"
    log.debug "MotionHandler: perst: $perst"
    log.debug "MotionHandler: lastStatus: $lastStatus"
	if (evt.value == "active") {
		if (enabled()) {
        	if (lastStatus != "on" || perst ) {
				log.debug "turning on lights due to motion"
				adjustLights("lightsOn")
            }
		}
		state.motionStopTime = null
	}
	
	// No motion
	else {
    	// Only schedule if enabled.
        // When not enabled do not control the lights
    	if (enabled()) {
    		scheduleCheckTurnOff()
        }
	}
}


//
// Contact Handler
//
def contactHandler(evt) {
	log.debug "$evt.name: $evt.value"
    // If a door is opened (or optionally closed)
	if (evt.value == "${doorCloseInstead ? "closed" : "open"}") {
		if (enabled() && (lastStatus != "on" || perst) ) {
			log.debug "turning on lights due to door opened"
			adjustLights("lightsOn")
		}
		state.motionStopTime = null
	}
	
	// Door Closed
	else {
    	// Only schedule if enabled.
        // When not enabled no control on lights
    	if (enabled()) {
    		scheduleCheckTurnOff()
        }
	}
}


//
// Light Handler
//
def illuminanceHandler(evt) {
	log.debug "$evt.name: $evt.value, lastStatus: $state.lastStatus, motionStopTime: $state.motionStopTime"
	def lastStatus = state.lastStatus
	// Turn off enable
	if (evt.integerValue > 50 && state.appEnabled != false) {
       	log.debug "Disable automation since it's bright"
		state.appEnabled = false
		adjustLights(actionOndisable)
	} else if (evt.integerValue < 30 && state.appEnabled != true) {
       	log.debug "Enable automation since it's dark"
		state.appEnabled = true
		adjustLights(actionOnEnable)
	}
    
    // Check if it just got dark enough to turn on the lights
	if (enabled() && (checkDoorsOpen() || checkAnyMotion())) {
		log.debug "turning on lights since it's now dark and there is motion or something opened"
		adjustLights("lightsOn")
        state.motionStopTime = null
	}
}

//
// Presence Handler
//
def presenceHandler(evt) {
	log.debug "$evt.name: $evt.value"
    // Don't do anything if the off time is 0 or we'd
    // just turn the light on then off
	if (evt.value == "present" && delayMinutes != 0) {
		if (enabled() && (lastStatus != "on" || perst) ) {
			log.debug "turning on lights due to presence"
			adjustLights("lightsOn")
		}
		state.motionStopTime = null
        // Now see if an off event should be scheduled
        scheduleCheckTurnOff()
	}
}

//
// Handle location event.
//
def modeHandler(evt) {
    log.debug "modeHandler: $evt.value"

    // Enable Mode, turn on lights
    if (evt.value == enableMode && state.appEnabled != true) {
        state.appEnabled = true
        log.debug "modeHandler: Enable"
        adjustLights(actionOnEnable)
        if (enabled() && (checkDoorsOpen() || checkAnyMotion())) {
            adjustLights("lightsOn")
            state.motionStopTime = null
        }
    // Disable mode
    } else if (evt.value == disableMode && state.appEnabled != false) {
        state.appEnabled = false
        log.debug "modeHandler: Disable"
        adjustLights(actionOndisable)
    }
}

def nightlightModeHandler(evt) {
    log.debug "nightlightModeHandler: $evt.value"

    // Enable Mode, turn on lights
    if (evt.value == nightlightEnableMode && state.nightlightEnabled != true) {
        state.nightlightEnabled = true
        log.debug "nightlightModeHandler: Enable"
		if (enabled()) {
        	adjustLights(nightlightActionOnEnable)
		}
        if (enabled() && (checkDoorsOpen() || checkAnyMotion())) {
            adjustLights("lightsOn")
            state.motionStopTime = null
        }
    // Disable mode
    } else if (evt.value == nightlightDisableMode && state.nightlightEnabled != false) {
        state.nightlightEnabled = false
        log.debug "nightlightModeHandler: Disable"
		if (enabled()) {
        	adjustLights(nightlightActionOndisable)
		}
    }
}


//
// Button Handler
//
def buttonHandler(evt) {
	def buttonNumber = evt.value
	def value = evt.name  // This is the type of input (press, hold, double tap, ...)
    def device = evt.device
	log.debug "buttonHandler: device: $device, buttonNumber: $buttonNumber, value: $value"
    // Create vars
    def buttonIsToggle = false
    def buttonIsEnable = false
    def buttonIsDisable = false
	def buttonIsNightlightToggle = false
    def buttonIsNightlightEnable = false
    def buttonIsNightlightDisable = false
    
    log.debug "buttonHandler: toggleButtonDevice: $toggleButtonDevice"
    log.debug "buttonHandler: enableButtonDevice: $enableButtonDevice"
    log.debug "buttonHandler: disableButtonDevice: $disableButtonDevice"
    
    // See if this was a toggle or on or off device
    if ((toggleButtonDevice != null) && ("$evt.device" == "$toggleButtonDevice")) {
		// Now check if we got the correct type of press
		if (((value == "pushed" )&& (!toggleButtonDoubleTap)) || ((value == "doubleTapped" ) && (toggleButtonDoubleTap))) {
			buttonIsToggle = true
			log.debug "buttonHandler: Button was for a toggle"
		}
    }
    if ((enableButtonDevice != null) && ("$evt.device" == "$enableButtonDevice")) {
		log.debug "buttonHandler: Got to here for enable.  $evt.name"
    	if (((value == "pushed" ) && (!enableButtonDoubleTap)) || ((value == "doubleTapped" ) && (enableButtonDoubleTap))) {
			log.debug "buttonHandler: Button was for enable"
        	buttonIsEnable = true
		}
    } 
    if ((disableButtonDevice != null) && ("$evt.device" == "$disableButtonDevice")) {
		log.debug "buttonHandler: Got to here for disable.  $evt.name"
		if (((value == "pushed" ) && (!enableButtonDoubleTap)) || ((value == "doubleTapped" ) && (enableButtonDoubleTap))) {
    		log.debug "buttonHandler: Button was for disable"
        	buttonIsDisable = true
		}
    }

    if ((nightlightToggleButtonDevice != null) && ("$evt.device" == "$nightlightToggleButtonDevice")) {
		// Now check if we got the correct type of press
		if (((value == "pushed" )&& (!nightlightToggleButtonDoubleTap)) || ((value == "doubleTapped" ) && (nightlightToggleButtonDoubleTap))) {
			buttonIsNightlightToggle = true
			log.debug "buttonHandler: Button was for a toggle nightlight"
		}
    }
    if ((nightlightEnableButtonDevice != null) && ("$evt.device" == "$nightlightEnableButtonDevice")) {
		log.debug "buttonHandler: Got to here for nightlight enable.  $evt.name"
    	if (((value == "pushed" ) && (!nightlightEnableButtonDoubleTap)) || ((value == "doubleTapped" ) && (nightlightEnableButtonDoubleTap))) {
			log.debug "buttonHandler: Button was for enable nightlight"
        	buttonIsNightlightEnable = true
		}
    } 
    if ((nightlightDisableButtonDevice != null) && ("$evt.device" == "$nightlightDisableButtonDevice")) {
		log.debug "buttonHandler: Got to here for disable nightlight.  $evt.name"
		if (((value == "pushed" ) && (!nightlightEnableButtonDoubleTap)) || ((value == "doubleTapped" ) && (nightlightEnableButtonDoubleTap))) {
    		log.debug "buttonHandler: Button was for disable nightlight"
        	buttonIsNightlightDisable = true
		}
    }

		switch(buttonNumber) {
			case ~/.*1.*/:
				buttonEvalAndAction(1, buttonIsToggle, buttonIsEnable, buttonIsDisable, buttonIsNightlightToggle, buttonIsNightlightEnable, buttonIsNightlightDisable)
				break
			case ~/.*2.*/:
				buttonEvalAndAction(2, buttonIsToggle, buttonIsEnable, buttonIsDisable, buttonIsNightlightToggle, buttonIsNightlightEnable, buttonIsNightlightDisable)
				break
			case ~/.*3.*/:
				buttonEvalAndAction(3, buttonIsToggle, buttonIsEnable, buttonIsDisable, buttonIsNightlightToggle, buttonIsNightlightEnable, buttonIsNightlightDisable)
				break
			case ~/.*4.*/:
				buttonEvalAndAction(4, buttonIsToggle, buttonIsEnable, buttonIsDisable, buttonIsNightlightToggle, buttonIsNightlightEnable, buttonIsNightlightDisable)
				break
            case ~/.*5.*/:
				buttonEvalAndAction(5, buttonIsToggle, buttonIsEnable, buttonIsDisable, buttonIsNightlightToggle, buttonIsNightlightEnable, buttonIsNightlightDisable)
				break
            case ~/.*6.*/:
				buttonEvalAndAction(6, buttonIsToggle, buttonIsEnable, buttonIsDisable, buttonIsNightlightToggle, buttonIsNightlightEnable, buttonIsNightlightDisable)
				break
			}
}


//
// Proc to evaluate if the desired button was pressed and do what was asked
//
def buttonEvalAndAction (buttonNum, buttonIsToggle, buttonIsEnable, buttonIsDisable, buttonIsNightlightToggle, buttonIsNightlightEnable, buttonIsNightlightDisable) {
	log.debug "buttonEvalAndAction: inputs: $buttonNum, $buttonIsToggle, $buttonIsEnable, $buttonIsDisable, $buttonIsNightlightToggle, $buttonIsNightlightEnable, $buttonIsNightlightDisable"
    log.debug "buttonEvalAndAction: toggleButtonNumber: $toggleButtonNumber"
    log.debug "buttonEvalAndAction: enableButtonNumber: $enableButtonNumber"
    log.debug "buttonEvalAndAction: disableButtonNumber: $disableButtonNumber"
    
    // Check toggle
    if (buttonIsToggle && (buttonNum == toggleButtonNumber)) {
        // If enabled, disable.  If disabled, enable ...
        log.debug "buttonEvalAndAction: Toggle"
        if (state.appEnabled == false) {
            state.appEnabled = true
            log.debug "buttonEvalAndAction: Toggle - Enable"
            adjustLights(TBactionOnEnable)
            if (enabled() && (checkDoorsOpen() || checkAnyMotion())) {
                adjustLights("lightsOn")
                state.motionStopTime = null
            }
        // Disable mode
        } else if (state.appEnabled == true) {
            state.appEnabled = false
            log.debug "buttonEvalAndAction: Toggle - Disable"
            adjustLights(TBactionOndisable)
        }
    }

    // Check enable
    if (buttonIsEnable && (buttonNum == enableButtonNumber)) {
        // Enable
        log.debug "buttonEvalAndAction: Enable"
        if (state.appEnabled == false) {
            state.appEnabled = true
            log.debug "buttonEvalAndAction: Seperate - Enable"
            adjustLights(SBactionOnEnable)
            if (enabled() && (checkDoorsOpen() || checkAnyMotion())) {
                adjustLights("lightsOn")
                state.motionStopTime = null
            }
        }
    }

    // Check disable
    if (buttonIsDisable && (buttonNum == disableButtonNumber)) {
        // Disable
        log.debug "buttonEvalAndAction: Disable"
        if (state.appEnabled == true) {
            state.appEnabled = false
            log.debug "buttonEvalAndAction: Seperare - Disable"
            adjustLights(SBactionOndisable)
        }
    }
	
    // Check nightlight toggle
    if (buttonIsNightlightToggle && (buttonNum == nightlightToggleButtonNumber)) {
        // If enabled, disable.  If disabled, enable ...
        log.debug "buttonEvalAndAction: nightlight Toggle"
        if (state.nightlightEnabled == false) {
            state.nightlightEnabled = true
            log.debug "buttonEvalAndAction: nightlight Toggle - Enable"
			if (enabled()) {
            	adjustLights(nightlightTBactionOnEnable)
			}
            if (enabled() && (checkDoorsOpen() || checkAnyMotion())) {
                adjustLights("lightsOn")
                state.motionStopTime = null
            }
        // Disable mode
        } else if (state.nightlightEnabled == true) {
            state.nightlightEnabled = false
            log.debug "buttonEvalAndAction: nightlight Toggle - Disable"
            if (enabled()) {
				adjustLights(nightlightTBactionOndisable)
			}
        }
    }

    // Check enable
    if (buttonIsNightlightEnable && (buttonNum == nightlightEnableButtonNumber)) {
        // Enable
        log.debug "buttonEvalAndAction: Enable nightlight"
        if (state.nightlightEnabled == false) {
            state.nightlightEnabled = true
            log.debug "buttonEvalAndAction: nightlight Seperate - Enable"
            if (enabled()) {
				adjustLights(nightlightSBactionOnEnable)
			}
            if (enabled() && (checkDoorsOpen() || checkAnyMotion())) {
                adjustLights("lightsOn")
                state.motionStopTime = null
            }
        }
    }

    // Check disable
    if (buttonIsNightlightDisable && (buttonNum == nightlightDisableButtonNumber)) {
        // Disable
        log.debug "buttonEvalAndAction: Disable nightlight"
        if (state.nightlightEnabled == true) {
            state.nightlightEnabled = false
            log.debug "buttonEvalAndAction: Seperare nightlight - Disable"
            if (enabled()) {
				adjustLights(nightlightSBactionOndisable)
			}
        }
    }

}


//
// Sunset Handler
//  Check for any open doors to turn lights on if they were opened before
//
def sunsetHandler() {
	log.debug "Executing sunset handler"
    state.appEnabled = true
    adjustLights(actionOnEnable)
    // Check if there is a door open to turn on the lights
    if (enabled() && (checkDoorsOpen() || checkAnyMotion())) {
        log.debug "turning on lights since it's now sunset and there is something opened"
        adjustLights("lightsOn")
        state.motionStopTime = null
    }
}
    	
//
// Sunrise Handler
//  Turn off lights at sunrise
//
def sunriseHandler() {
	log.debug "Executing sunrise handler"
    adjustLights(actionOndisable)
   	state.appEnabled = false
}

//
// Switch Handler: for toggle
//
def switchHandlerToggle(evt) {
	log.debug "switchHandlerToggle: $evt.value"
	
    // Switch was turned off
    if (evt.value == "off") {
        log.debug "Switch to off, disable automation"
        state.appEnabled = false
        adjustLights(STactionOndisable)
    // Switch was turned on
    } else if (evt.value == "on") {
        log.debug "Switch to on, enable automation"
        state.appEnabled = true
        adjustLights(STactionOnEnable)
    }
}

def nightlightSwitchHandlerToggle(evt) {
	log.debug "nightlightSwitchHandlerToggle: $evt.value"
	
    // Switch was turned off
    if (evt.value == "off") {
        log.debug "Switch to off, disable nighlight"
        state.nightlightEnabled = false
		if (enabled()) {
        	adjustLights(nightlightSTactionOndisable)
		}
    // Switch was turned on
    } else if (evt.value == "on") {
        log.debug "Switch to on, enable nightlight"
        state.nightlightEnabled = true
		if (enabled()) {
			adjustLights(nightlightSTactionOnEnable)
		}
    }
}


//
// Schedule a time to run checkTurnOff()
//
def scheduleCheckTurnOff() {
	// Check if there is no motion and no doors open to schedule a time to check if lights should be turned off
	if (!checkDoorsOpen() && !checkAnyMotion()) {
		state.motionStopTime = now()
		if (state.nightlightEnabled == false) {
			if(delayMinutes) {
				runIn(delayMinutes*60, checkTurnOff)
            	log.debug "Scheduling turn off, non-nightlight mode, in $delayMinutes mins."
			} else {
				checkTurnOff()
			}
		} else {
			if(nightlightDelayMinutes) {
				runIn(nightlightDelayMinutes*60, checkTurnOff)
            	log.debug "Scheduling turn off, nightlight mode, in $nightlightDelayMinutes mins."
			} else {
				checkTurnOff()
			}
		}
	}
}


//
// Check if the light(s) should be turned off
//
def checkTurnOff() {
	log.debug "In checkTurnOff, state.motionStopTime = $state.motionStopTime, state.lastStatus = $state.lastStatus"
    // Check only if:
    	// - there is a stopTimeSet (this is set to Null when we turn on),
        // - All doors are closed and there is no motion
	if (state.motionStopTime && !checkDoorsOpen() && !checkAnyMotion() ) {
    	// Don't check the elapsed time.  If we got here it's time to turn off the lights as the schedule to get here
        // is overwritten when re-scheduled.  If there was a schedule to get here and then something happened
        // motionStopTime will be "Null"
        log.debug "Turning off lights"
		adjustLights("lightsOff")
	}
}


//
// Turn on Dimmers
//
def turnOnDimmers() {

    if (settings.dimmers != null) {
        settings.dimmers?.each() {
            def name = it as String
			def fullName = "foo"
			if (state.nightlightEnabled) {
            	fullName = "${name}_nightlightOnVal"
			} else {
				fullName = "${name}_OnVal"
			}
			log.debug("turnOnDimmers: fullName: $fullName")
            def value = settings[fullName]
            log.debug("turnOnDimmers: value: $value")
            value = value.toInteger()
            if (value > 99) value = 99
            it.setLevel(value)
            log.debug("turnOnDimmers: Set $it to $value")
        }
    }
}


//
// Turn off Dimmers
//
def turnOffDimmers() {
    
    if (settings.dimmers != null) {
        settings.dimmers?.each() {
            def name = it as String
			def fullName = "foo"
			if (state.nightlightEnabled) {
            	fullName = "${name}_nightlightOffVal"
			} else {
				fullName = "${name}_OffVal"
			}
			log.debug("turnOffDimmers: fullName: $fullName")
            def value = settings[fullName]
            log.debug("turnOffDimmers: value: $value")
            value = value.toInteger()
            if (value > 99) value = 99
            it.setLevel(value)
            log.debug("turnOffDimmers: Set $it to $value")
        }
    }
}


//
// Set the lights
//
def adjustLights(var) {
    log.debug "adjustLights: passed in $var"
    // Set the time this happened
    state.adjustLastTime = now()
    log.debug "adjustLights: set time to $state.adjustLastTime"

	// Only do things if parent is enabled
    if (state.parentEnable) {
        switch(var) {
          case "lightsOff" :
            log.debug "adjustLights:doing lightsOff"
            if (lights != null) {lights.off()}
            if (dimmers != null) {turnOffDimmers()}
            state.lastStatus = "off"
            break
          case "lightsOffFull" :
            log.debug "adjustLights:doing lightsOffFull"
            if (lights != null) {lights.off()}
            if (dimmers != null) {dimmers.setLevel(0)}
            state.lastStatus = "off"
            break
          case "lightsOn" :
            log.debug "adjustLights:doing lightsOn"
            if (lights != null) {lights.on()}
            if (dimmers != null) {turnOnDimmers()}
            state.lastStatus = "on"
            if (dimmers != null) {turnOnDimmers()}; // Double set to work around switch oddness
            break
          case "lightsOnFull" :
            log.debug "adjustLights:doing lightsOnFull"
            if (lights != null) {lights.on()}
            if (dimmers != null) {dimmers.setLevel(99)}
            state.lastStatus = "on"
            break
        case "lightsOnThenOff" :
            log.debug "adjustLights:doing lightsOnThenOff"
            if (lights != null) {lights.on()}
            if (dimmers != null) {dimmers.setLevel(99)}
            if (lights != null) {lights.off()}
            if (dimmers != null) {dimmers.setLevel(0)}
            state.lastStatus = "off"
            break
        }
    }
}


//
// Get the Sun rise and set times
//
def astroCheck() {
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)    
    def current = new Date()
	def riseTime = s.sunrise
	def setTime = s.sunset
    def result

	// If the riseTime is before now,
    // set to the next riseTime
	if(riseTime.before(current)) {
		riseTime = riseTime.next()
        log.debug "Setting to the next rise time"
	}

	// If the current stored rise time is not the next rise time
    // Update the schedule
	if (state.riseTime != riseTime.time) {
		unschedule("sunriseHandler")
		state.riseTime = riseTime.time

		log.info "scheduling sunrise handler for $riseTime"
		schedule(riseTime, sunriseHandler)
	}

	// If the setTime is before now,
    // set to the next setTime
	if(setTime.before(current)) {
		setTime = setTime.next()
	}
    
    // If the current stored set time is not the next set time
    // Update the schedule
	if (state.setTime != setTime.time) {
		unschedule("sunsetHandler")

		state.setTime = setTime.time

		log.info "scheduling sunset handler for $setTime"
	    schedule(setTime, sunsetHandler)
	}
    
    log.debug "Current: $current"
    log.debug "riseTime: $riseTime"
	log.debug "setTime: $setTime"

    // Code to set the enable state
    // Also done on the sunset and sunrise handlers
    // But done here incase the app is loaded after sunset
    // Sets a special state variable only used at init
	def t = now()
    log.debug "Time is $t"
    // If riseTime is greater than setTime we can just check if after setTime
    // the check is after set AND before rise
    if (state.riseTime > state.setTime) {
	    result = t < state.riseTime && t > state.setTime

		// Else (this means rise time is LESS then set time)
        // This can happen if the set time is updated before the current
        // day's rise time.
        // In this case we just check if we're before the rise time
    } else {
       	result = t < state.riseTime
    }
    state.astroEnabled = result
}

//
// Determine if the app is enabled and lights could be turned on
//
def enabled() {
	def result
    log.debug "Enabled: Parent enable: $state.parentEnable"
    if (state.parentEnable) {
    	state.enabled = state.appEnabled
    	result = state.appEnabled
    } else {
    	// If the Parent App state is disabled, so is the whole app
    	state.enabled = false
        result = false
    }
    log.debug "Enabled: $result"
	return result
}

//
// Parent Enable
// Parent App calls this when it's enable state changes
// This can trigger turning on the lights
//
def parentEnable(enable) {
    log.debug "parentEnable: Parent Enable is $enable"
    state.parentEnable = enable
    enabled()
    if (enabled() && (checkDoorsOpen() || checkAnyMotion())) {
        adjustLights("lightsOn")
        state.motionStopTime = null
    }
}

//
// Return Parent Enable to the parent app since
// The child app owns the state
//
def parentEnabledVal() {
    return state.parentEnable
}

//
// Return the app enable state to the parent app
//
def getChildEnableState() {
	return state.appEnabled
}


//
// Return the app enable state to the parent app
//
def getChildNightlightEnableState() {
	return state.nightlightEnabled
}


private getSunriseOffset() {
	sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() {
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}
