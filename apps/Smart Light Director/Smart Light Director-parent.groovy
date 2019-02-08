/**
 *  Smart Light Director
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
    name: "Smart Light Director",
    namespace: "ckowalski-1",
    author: "Christopher Kowalski",
    description: "Smart Light Director Application",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png",
    singleInstance: true)

preferences {
	page(name: "parentPage")  
    page(name: "pageAbout", nextPage: "parentPage")
    page(name: "automationsPage")
}



def parentPage() {
	return dynamicPage(name: "parentPage", title: "", install: true, uninstall: true) {
		if(!state.appInstalled) {
			section("Hit Done to install the App") {
		}
		} else {
			
       section("Create a new lighting automation.") {
            app(name: "childApps", appName: "Smart Light Director Config", namespace: "ckowalski-1", title: "New Lighting Automation", multiple: true)
        }
		section("Manage Automations:") {
		href (name: "automationsPage", title: "Automations",
            description: "Tap to see automation state and enable/disable automations",
            page: "automationsPage"
            )
		}
        section("Version Info, User's Guide") {
       	href (name: "pageAbout", title: "Smart Light Director", 
       		description: "Tap to get Smartapp Info and User's Guide.",
       		page: "pageAbout"
			)		
   		}
        }
	}
}



def automationsPage() {
	return dynamicPage(name: "automationsPage", title: "Installed Automations") {
		def children = getChildApps()
        log.debug "Childern are ${children.label}"
        //def sortedChildern = children.toSorted()
        //log.debug "Childern Sorted are ${sortedChildern.label}"
		if(children) {
        	children.each { child ->
            	def appName = "${child.label}"
                log.debug "automationsPage: child app: ${child.label}"
				def initialParentEnabledVal = child.parentEnabledVal()
				log.debug "automationsPage: $appName: initialParentEnabledVal = $initialParentEnabledVal"
                section("Details for $appName") {
					// Input to disable automation
                    input "enableAutomation_${appName}", "bool", title: "Enable $appName?", required: false, defaultValue: "$initialParentEnabledVal", submitOnChange: true
                    log.debug "automationsPage: $appName: switch value is: "+settings."enableAutomation_$appName"
                    if (settings."enableAutomation_$appName") {
                        child.parentEnable(true)
                        log.debug "automationsPage: $appName: Did parent enable true"
                    } else {
                        child.parentEnable(false)
                        log.debug "automationsPage: $appName: Did parent enable false"
                    }
                    def parentEnabled = child.parentEnabledVal()
                    log.debug "automationsPage: $appName: Current Parent Enable is: " +settings."enableAutomation_$appName" +", $parentEnabled"
					// Display Enable State
                    // Get the overall app Enable state
                    def appEnabled = child.enabled()
                    log.debug "automationsPage: $appName: appEnabled: $appEnabled"
                    // Get the child or app enable mode (for example maybe not enabled if using light and not dark)
                    def childEnabled = child.getChildEnableState()
					def childNightEnabled = child.getChildNightlightEnableState()
                    def overallEnabledText = "Foo"
                    def childEnabledText = "Foo"
                    def parentEnabledText = "Foo"
					def childNightlightEnabledText = "Foo"
                    if (appEnabled) {overallEnabledText = "Enabled"} else {overallEnabledText = "Disabled"}
                    if (childEnabled) {childEnabledText = "Enabled"} else {childEnabledText = "Disabled"}
                    if (parentEnabled) {parentEnabledText = "Enabled"} else {parentEnabledText = "Disabled"}
					if (childNightEnabled) {childNightlightEnabledText = "Enabled"} else {childNightlightEnabledText = "Disabled"}
                    log.debug   "automationsPage: The automation $appName: \n"+
                    			"App Enable State is:         $childEnabledText \n"+
                                "Parent Enable State is:      $parentEnabledText \n"+
                                "Overall Enable State is:     $overallEnabledText \n"+
								"Nighlight Enable State is:   $childNightlightEnabledText"
                    paragraph   "The automation $appName: \n"+
                    			"App Enable State is:         $childEnabledText \n"+
                                "Parent Enable State is:      $parentEnabledText \n"+
                                "Overall Enable State is:     $overallEnabledText \n"+
								"Nighlight Enable State is:   $childNightlightEnabledText"
                }
            }
 
        } else {
			section("") {
				paragraph "You haven't created any Automations yet!"
			}
		}
	}
}


// Show "About" page
private def pageAbout() {

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
        nextPage    : "parentPage",
        install     : false,
        uninstall   : state.installed
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph textAbout
        }
    }
}


def installed() { 
	state.appInstalled = true
	initialize()
}

def updated() {
	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	initParent()
}

def initParent() {
	log.debug "Parent Initialized"
}


