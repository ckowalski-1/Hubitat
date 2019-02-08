/**
 *  You Were Gone This Long ...
 *
 *  Copyright 2019 Christopher Kowalski
 *
 *  Simple app to send silly random messages when you leave and when you return, and also tell you how long
 *  you were gone
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
    name: "You Were Gone This Long ...",
    namespace: "ckowalski-1",
    author: "Christopher Kowalski",
    description: "Simple app to send silly random messages when you leave and when you return, and also tell you how long you were gone",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png",
)

preferences {
    page(name: "Confgiure Your Trigger", title: "Choose a leave trigger and a return trigger", install: true, uninstall: true) {
    
        section("Presence Switch") { 
            input "presence_trigger", "capability.switch", 
                multiple: false, 
                title: "Presence Switch ...", 
                required: true
        }


        section("Send to who?") { 
            input "notification_device", "capability.notification", 
                multiple: true, 
                title: "Who should get the message?", 
                required: true
        }
        
        section("Name and Mode...") {
            label title: "Assign a name", required: false
            input "modes", "mode", title: "Only when mode is", multiple: true, required: false
        }
    }
    
}

def installed() {
	initialize()
}

def updated() {
	initialize()
}

def initialize() {
    unsubscribe()
    subscribe(presence_trigger, "switch", switch_hander)
	state.leave_time = null
}

def switch_hander(evt) {
	log.debug "Got $evt.value"
	if (evt.value == "on") {
		arriver_handler()
	} else {
		leave_hander()
	}
}

def leave_hander() {
    // When the leave hander triggers we:
		// Store the time
		// Push out a message
	state.leave_time = now()
	log.debug "Left at $state.leave_time"
	def msg = generate_message("leave")
	log.debug "Left message $msg"
	notification_device.deviceNotification("$msg")
}


def arriver_handler() {
    // When the leave hander triggers we:
		// Store the time time
		// Calculate how long the house was empty
		// Push out a message
	def arrive_time = now()
	def time_gone_message = ""
	log.debug "Leave time is $state.leave_time"
	if (state.leave_time == null) {
		time_gone_message = "unknown"
	} else {
		def time_gone_ms 	= arrive_time - state.leave_time
		def time_gone_s 	= (int)(time_gone_ms / 1000) % 60
		def time_gone_min 	= (int)(time_gone_ms / (1000*60)) % 60
		def time_gone_hours = (int)(time_gone_ms / (1000*60*60)) % 24
		def time_gone_days 	= (int)(time_gone_ms / (1000*60*60*24))
	
		if (time_gone_days > 0) {
			time_gone_message = "$time_gone_days days $time_gone_hours hours and $time_gone_min mins"
		} else if (time_gone_hours > 0) {
			time_gone_message = "$time_gone_hours hours and $time_gone_min mins"
		} else {
			time_gone_message = "$time_gone_min mins"
		}
	}
	
	
	def message_to_send = generate_message("arrive") + " - You were gone for $time_gone_message."
	log.debug "Left message $message_to_send"
	notification_device.deviceNotification("$message_to_send")
}

def generate_message(leave_or_arrive) {
	def leave_messages =  ["Parting is such sweet sorrow","Gone, but not forgotten", "FINE!  You hate me!", "I miss you already ...",
						   "Come Back!","Have fun and be safe","Safe Travels","Don’t worry I’ll keep an eye on things","Truffles says bark",
						   "I hate to see you leave, but I love to watch you go ... the doorbell made me say it!","Don’t forget me when you’re gone",
						   "Good day sir... I said good day","I can’t see, it’s all black. Oh wait that just because the lights are off",
						   "What? Are you too good for your home"]
	def arrive_messages = ["Bark, bark, woof, woof ... sorry I got all caught up","Get down, get down! Oh hey nothing going on here",
						   "I’d bring you your slippers but I don’t have hands or teeth","Wow that was a great game of hide-and-go-seek",
						   "You’d better not smell like another house","You complete me ...","Long time no see","I'd hug you if I had arms",
						   "I'd kiss you if I had lips"]
	def message = ""
	Random rnd = new Random()
	
	if (leave_or_arrive == "leave") {
		message = leave_messages[rnd.nextInt(leave_messages.size)]
	} else {
		message = arrive_messages[rnd.nextInt(arrive_messages.size)]
	}
	return message
}

