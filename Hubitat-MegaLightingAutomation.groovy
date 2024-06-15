/*===============================================
=========  Mega Lighting Automation V0.1  =======
=========      Coltonton - D3ADCRU7R      =======
=================================================
    **** DEVELOPMENT BUILD DO NOT RUN ****


Hubitat Import URL:
https://raw.githubusercontent.com/Coltonton/Hubitat-MegaLightingAutomation/master/MegaLightingAutomation.groovy


ChangeLog:
V0.1 
   - First Beta Revision Release
   - Turn Scenes On With Motion!
   - - Each automation can have multiple motion detectors that can work independently or require all.
   - - Will turn back off when motion stops happening after a definable delay or instantly.
   - - Ablity to override auto off if a device is on; ex dont turn off the lights if the tv is on.
   - - Ability to set diffrent lighting scenes based on (outdoor) ambiant lighting conditons. [Day, Evening, Night]
   - - - Will work with one or multiple illuminance sensors (that will average)
   - - - Ability to set own Lux thresholds
   - - Ability to only allow lights with motion in user-chosen modes

   - Very much work in progress, although it tested working.

WishList:
    - User can choose the dim up/down time
    - Contact Sensor Support
    - Ability to "Create scenes" In App instad of requiring existing
    - Automaticlly create override device?
    - Link with button
    - Nightlight Mode
    - Add mode control into the "Lighting Mode" decision
    - Add more documention cuz documentation is good
    - Clean UP! & MAKE PRETTY
*/
///===================Start===================\\\
definition(
    name: "Mega Lighting Automation",                //AppName
    namespace: "MegaLightingAutomationNS",           //Namespace
    author: "Coltonton - D3ADCRU7R",                 //App Author
    description: "Average some illuminance sensors", //App Description
    category: "Convenience",                         //App Category
    iconUrl: "",                                     //App Icon
    iconX2Url: "")                                   //App Icon2x

preferences {
    page(name: "mainPage", title: "Setup Light/Motion", uninstall: true) //Page Ids

    def timedict = ["Minutes" : "60", "Hours" : "3600"]
    userParamMotion = [MotionSensors : [], 
                        MultiMotionMode : bool, 
                        DelaySetting  : false, 
                        OffTimeValue : 0,
                        OffTimeUnit  : "",
                        BypassMotion : false,
                        BypassDevices: ""  ]
    userParamIlum = [  UserParam_IllumSetting : bool,
                        UserParam_luxSensors : [],
                        DayEntryVar : int ,
                        EveEntryVar : int ,
                        NteEntryVar : int]
    userParamMode = [  OnlyInMode : bool,
                        SelectedOnlyInMode : []]
    userParamSetLights = [ DayLightDevice : [],
                            EveLightDevice : [],
                            NteLightDevice : []]  
}

///===================Pages===================\\\
def mainPage() {
    def appTitleSize = "52px"
    def appSectionSize = "40px"
    def appSubSectionSize = "30px"
    def appEnabedSize = "22px"
    def appDisabledSize = "16px"
    def onlyInModeList = []
	dynamicPage(name: "mainPage", title: "<font style='font-size:$appTitleSize; color:#330066'><b>Mega Lighting Automation</b></font>", install: true, uninstall: true) {
        
        //Name
        section(){
            input "textEntryVar", "text", title: "<font style='font-size:12px; color:#000000'><b>Name this app</b></font>", submitOnChange: true, required: true, defaultValue: "MegaLightingAutomation" //Text Entry
        }
        
        //Motion Section
        section() { 
            paragraph "<font style='font-size:$appSectionSize; color:#3300cc'><b>Enable Motion Sensors</b></font>\n"
            input name: "enbMotionMenu", type: "bool", title: "", submitOnChange: true
 
            if(enbMotionMenu) {
                userParamMotion.MotionSensors = true
                paragraph "<font style='font-size:$appEnabedSize; color:#047db5'>[ENABLED]</font>\n"
                paragraph "<font style='font-size:$appSubSectionSize; color:#0069e0'><b><u>Which motion sensor(s)</u></b></font>"
                input name: "userParam_MotionSensors", type: "capability.motionSensor", title: "When motion is detected on...", required: true, width: 4, multiple: true, submitOnChange: true
                int motionSensorListSize
                motionSensorListSize = 0  
                try {
                    motionSensorListSize = userParam_MotionSensors.size()
                } catch(Exception ex) {
                    motionSensorListSize = 0
                }
                log.debug "Selected amout of motion sensors is $motionSensorListSize"
                if(motionSensorListSize > 1) {
                    input name: "userParam_MultiMotionMode", type: "bool", title: " ", submitOnChange: true, defaultValue: false
                    
                    if(userParam_MultiMotionMode) {
                        paragraph "<font style='font-size:$appDisabledSize; color:#000000'>[ANY/<font style='font-size:$appEnabedSize; color:#047db5'><b><u>ALL</u></b></font>] of the motion sensors must be active to trigger an event</font>"
                    }
                    else {
                        userParam_MultiMotionMode = false
                        paragraph "<font style='font-size:$appDisabledSize; color:#000000'>[<font style='font-size:$appEnabedSize; color:#047db5'><b><u>ANY</u></b></font>/ALL] of the motion sensors must be active to trigger an event</font>"
                    }
                }
                
                def enumoptions = ["Minutes", "Hours"]
                paragraph "\n\n"
                paragraph "<font style='font-size:$appSubSectionSize; color:#0069e0'><b><u>When motion stops being detected...</u></b></font>"
                input name: "userParam_DelaySetting", type: "bool", title: "", submitOnChange: true, defaultValue: true
                if(userParam_DelaySetting) {
                    log.info "userParam_DelaySetting is $userParam_DelaySetting"
                    //paragraph "<font style='font-size:22px; color:#000000'>[INSTANTLY<font style='font-size:16px; color:#047db5'><b>/AFTER A DELAY OF</b></font>]</font>"
                    paragraph "<font style='font-size:$appDisabledSize; color:#000000'>[INSTANTLY/<font style='font-size:$appEnabedSize; color:#047db5'><b><u>AFTER A DELAY OF</u></b></font>]</font>"
                    input name: "userParam_OffTimeValue", type: "number", title: "", defaultValue: 1, width: 1, required: true
                    input name: "userParam_OffTimeUnit", type: "enum", title: "", options: enumoptions, defaultValue: [0], width: 2, required: true
                    paragraph "<font style='font-size:$appEnabedSize; color:#047db5'><b>turn the lights off.</b></font>"
                }
                else { //Instantly
                    userParam_OffTimeValue = false
                    userParam_OffTimeUnit = false
                    paragraph "<font style='font-size:$appDisabledSize; color:#000000'>[<font style='font-size:$appEnabedSize; color:#047db5'><b><u>INSTANTLY</u></b></font>/AFTER A DELAY OF] <font style='font-size:22px; color:#047db5'>\n<b>turn the lights off.</b></font></font>"
                }
                paragraph "\n\n"

                paragraph "<font style='font-size:$appSubSectionSize; color:#0069e0'><b><u>Override Off Settings</u></b></font>"
                paragraph "<font style='font-size:12px; color:#000000'><b>Disable motion inactivity auto-off if these devices are ON. Will resume normal seetings once off.\nEX: Don't turn off the lights in the room when the TV is On </b></font>"
                input name: "userParam_OverrideSetting", type: "bool", title: "", submitOnChange: true, defaultValue: false
                if(userParam_OverrideSetting) {
                    log.info "userParam_DelaySetting is $userParam_OverrideSetting"
                    paragraph "<font style='font-size:$appEnabedSize; color:#047db5'>[ENABLED]</font>\n"
                    input name: "userParam_BypassDevices", type: "capability.switch", title: "Switches to disable auto off", multiple: false
                }
                else { //Instantly
                    //userParam_BypassDevices = ["null"]
                    paragraph "<font style='font-size:$appDisabledSize; color:#696969'>[DISABLED]</font>"
                }
            }   
            else{
                paragraph "<font style='font-size:$appDisabledSize; color:#696969'>[DISABLED]</font>"
            }

            userParamMotion.MotionSensors = userParam_MotionSensors
            userParamMotion.MultiMotionMode = userParam_MultiMotionMode
            userParamMotion.DelaySetting = userParam_DelaySetting
            userParamMotion.OffTimeValue = userParam_OffTimeValue
            userParamMotion.OffTimeUnit = userParam_OffTimeUnit
            userParamMotion.BypassMotion = userParam_OverrideSetting
            userParamMotion.BypassDevices = userParam_BypassDevices
        }

        /*//Contact Sensors
        section() {
            paragraph "<font style='font-size:$appSectionSize; color:#3300cc'><b>Enable Contact Sensors?</b></font>"
            input name: "enbContactMenu", type: "bool", title: "", submitOnChange: true

            if(enbContactMenu) {
                paragraph "[ENABLED]"
                paragraph "<font style='font-size:22px; color:#0069e0'><b>Which contact sensor(s)...</b></font>"
                input name: "contactSensors", type: "capability.contactSensor", title: "When sensor is opened...", required: true, width: 4, multiple: true, submitOnChange: true
            }   
            else{
                paragraph "[DISABLED]"
            }
        } */

        //Illum Section
        section () {
            if (enbMotionMenu || enbContactMenu) {
                dayIlumVar = 1000
                eveIlumVar = 400
                nteIlumVar = 50
                eveEntryVarInt
                paragraph "<font style='font-size:$appSectionSize; color:#3300cc'><b>Enable Lux Sensors</b></font>\n"
                input name: "userParam_IllumSetting", type: "bool", title: "", submitOnChange: true, defaultValue: false
                if(userParam_IllumSetting) {
                    log.info "userParam_DelaySetting is $userParam_OverrideSetting"
                    paragraph "<font style='font-size:$appEnabedSize; color:#047db5'>[ENABLED]</font>\n"
                    paragraph "<font style='font-size:$appSubSectionSize; color:#0069e0'><b><u>Select Devices</u></b></font>"
                    if(textEntryVar) app.updateLabel("$textEntryVar")
                    input "userParam_luxSensors", "capability.illuminanceMeasurement", title: "Select Illuminance Devices", submitOnChange: true, required: true, multiple: true
                    if(userParam_luxSensors) paragraph "Current average is ${averageLux()} lux" //Show after devices selected
                    paragraph "<font style='font-size:$appSubSectionSize; color:#0069e0'><b><u>\nChange default values?</u></b></font>"
                    input name: "userParam_defaultIllumSetting", type: "bool", title: "", submitOnChange: true, defaultValue: false
                    
                    if(userParam_defaultIllumSetting){
                        paragraph "<font style='font-size:$appEnabedSize; color:#047db5'>[ENABLED]</font>\n"
                        paragraph "\n"
                        paragraph "<font style='font-size:20px; color:#000000'> Night Mode</font>", width: 1
                        paragraph "<font style='font-size:12px; color:#000000'>    0  --</font>", width: 1
                        input "nteEntryVar", "number", title: "", submitOnChange: true, required: true, defaultValue: nteIlumVar, width: 1
                        paragraph "<font style='font-size:20px; color:#000000'>Lux</font>", width: 1
                        paragraph "\n"

                        paragraph "<font style='font-size:20px; color:#000000'> Eve Mode</font>", width: 1
                        paragraph "<font style='font-size:12px; color:#000000'>    $nteEntryVar  --</font>", width: 1
                        input "eveEntryVar", "number", title: "", submitOnChange: true, required: true, defaultValue: nteIlumVar, width: 1
                        paragraph "<font style='font-size:20px; color:#000000'>Lux</font>", width: 1
                        paragraph "\n"

                        eveEntryVar2 = eveEntryVar + 1
                        paragraph "<font style='font-size:20px; color:#000000'> Day Mode</font>", width: 1
                        paragraph "<font style='font-size:12px; color:#000000'> $eveEntryVar2  -- </font>", width: 1
                        paragraph "<font style='font-size:12px; color:#000000'>    +  </font>", width: 1
                        paragraph "<font style='font-size:20px; color:#000000'>Lux</font>", width: 1
                        paragraph "\n"
                        
                    }
                    else{
                        paragraph "<font style='font-size:$appDisabledSize; color:#696969'>[DISABLED]</font>"
                        dayEntryVar = dayIeveEntryVar2lumVar
                        eveEntryVar = eveIlumVar
                        nteEntryVar = nteIlumVar
                    }
                }
                else { //Instantly
                    //userParam_BypassDevices = ["null"]
                    paragraph "<font style='font-size:$appDisabledSize; color:#696969'>[DISABLED]</font>"
                }
                userParamIlum.UserParam_IllumSetting = userParam_IllumSetting
                userParamIlum.UserParam_luxSensors = userParam_luxSensors
                userParamIlum.DayEntryVar = dayEntryVar 
                userParamIlum.EveEntryVar = eveEntryVar
                userParamIlum.NteEntryVar = nteEntryVar
            }
        }

        //Mode Section
        section () {
            paragraph "<font style='font-size:$appSectionSize; color:#3300cc'><b>Enable Only in Mode Control</b></font>\n"
            input name: "userParamMode_OnlyInMode", type: "bool", title: "", submitOnChange: true, defaultValue: false
            if(userParamMode_OnlyInMode) {
                log.info "userParam_DelaySetting is $userParam_OverrideSetting"
                paragraph "<font style='font-size:$appEnabedSize; color:#047db5'>[ENABLED]</font>\n"
                secmodeList = location.getModes()
                for(int i = 0;i<secmodeList.size;i++) {
                    onlyInModeList.add(secmodeList[i].toString())  
                }
                input name: "userParamMode_selectedOnlyInMode", type: "enum", title: "Allow ONLY in these modes", options: onlyInModeList, multiple: true, submitOnChange: true
            }
            else{
                paragraph "<font style='font-size:$appDisabledSize; color:#696969'>[DISABLED]</font>"
            }
            userParamMode.OnlyInMode = userParamMode_OnlyInMode
            userParamMode.SelectedOnlyInMode = userParamMode_selectedOnlyInMode
        }

        //Var Print
        section() {
            if (userParam_IllumSetting){
                DebugDumper(userParamIlum)
            }
            if (userParamMode_selectedOnlyInMode){
                DebugDumper(userParamMode)
            }
            if (userParam_MotionSensors){
                DebugDumper(userParamMotion)
            }
        }

        //Extras
        /*section() {
            paragraph "<font style='font-size:40px; color:#3300cc'><b>Enable Extra Features?</b></font>\n"
            //userParam_ExtraOptions = []
                addonListSize = 0 
                extraFeatureList = ["Overide when On", "Only while in these modes", "Link with a button/remote", "Nightligh Mode", "Trigger only when dark"]
                paragraph "<font style='font-size:22px; color:#0069e0'><b><u>Add Other Features</u></b></font>"
                //paragraph "<font style='font-size:14px; color:#0069e0'><b><u>remove by unselecting:</u></b></font>"
                //input name: "userParam_ExtraOptions", type: "bool", title: "", submitOnChange: true, defaultValue: true
                input name: "userParam_ExtraOptions", type: "enum", title: "", options: extraFeatureList, width: 4, multiple: true, submitOnChange: true
                try {
                    addonListSize = userParam_ExtraOptions.size()
                    log.debug "Selected amout of addons is $addonListSize" 
                    for (int i = 0; i < addonListSize; i++) {
                        tmp = userParam_ExtraOptions.get(i)
                        userParamMotion.AddonList.add("$tmp")
                        log.debug"Global list: $userParamMotion.AddonList"
                        if(tmp == "Overide when On"){
                            paragraph "<font style='font-size:18px; color:#0069e0'><b>* <u>$tmp</u></b></font>"
                        } else if (tmp == "Only while in these modes"){
                            paragraph "<font style='font-size:18px; color:#0069e0'><b>* <u>$tmp</u></b></font>"
                        } else if (tmp == "Link with a button/remote"){
                            paragraph "<font style='font-size:18px; color:#0069e0'><b>* <u>$tmp</u></b></font>"
                        } else if (tmp == "Nightligh Mode"){
                            paragraph "<font style='font-size:18px; color:#0069e0'><b>* <u>$tmp</u></b></font>"
                            paragraph "<font style='font-size:12px; color:#000000'>Nightlight mode allows you to keep certian lights on when you want</font>"
                        } else if (tmp == "Trigger only when dark"){
                            paragraph "<font style='font-size:18px; color:#0069e0'><b>* <u>$tmp</u></b></font>"
                        }
                    }
                }   
                catch(Exception ex) {
                    addonListSize = 0
                }
        }*/

        //Light Settings menu
        section(){
            if(userParam_IllumSetting) {
                paragraph "<font style='font-size:$appSectionSize; color:#696969'><b><s>Standard Light Automation</s></b></font>"
                paragraph "<font style='font-size:12px; color:#696969'><b>This function is disabled as you enabled illumination control\n\n\n</b></font>"
                paragraph "<font style='font-size:$appSectionSize; color:#3300cc'><b>Illuminessence Light Automation</b></font>\n"
                paragraph "<font style='font-size:12px; color:#000000'><b>Please Note: At this time, scene activators/switches are required here\n</b></font>"
                //paragraph "<font style='font-size:12px; color:#000000'><b>If you don't, thats okay, but functionality may be limited. \n\n</b></font>"
                input name: "userParamMode_UsrHasRoutine", type: "bool", title: "", defaultValue: true, width: 2, disabled: true
                if(userParamMode_UsrHasRoutine) {
                    paragraph "<font style='font-size:$appEnabedSize; color:#047db5'><b><u>[I Have Scenes]</u></b>\n\n\n</font>", width: 7
                    paragraph "<font style='font-size:$appSubSectionSize; color:#0069e0'><b><u>Select Routine to Set During Day Mode\n</u></b></font>"
                    input name: "userParam_DayLightDevices", type: "capability.switch", title: "With these scenes...", multiple: true, required: true
                    paragraph "<font style='font-size:$appSubSectionSize; color:#0069e0'><b><u>Select Routine to Set During Eve Mode\n</u></b></font>"
                    input name: "userParam_EveLightDevices", type: "capability.switch", title: "With these scenes...", multiple: true , required: true
                    paragraph "<font style='font-size:$appSubSectionSize; color:#0069e0'><b><u>Select Routine to Set During Night Mode\n</u></b></font>"
                    input name: "userParam_NightLightDevices", type: "capability.switch", title: "With these scenes...", multiple: true, required: true
                    paragraph "<font style='font-size:$appSubSectionSize; color:#0069e0'><b><u>For Hue Groups - Add the parrent group here\n</u></b></font>" //Hue group fix
                    input name: "userParam_offLightDevices", type: "capability.switch", title: "With these scenes...", multiple: true , required: false
                }
                //Function Disabled
                /*else {  
                    paragraph "<font style='font-size:$appEnabedSize; color:#047db5'><b><u>[I Do Not Have Scenes]\n\n\n</u></b></font>", width: 7
                    input name: "userParam_colorLightDevices", type: "capability.colorControl", title: "With these color lights...", submitOnChange: true, multiple: true
                    if (userParam_colorLightDevices) {
                        input "userParam_colorLightDevices_brightness", "number", title: "This Brightness", submitOnChange: true, required: true, width: 2
                        input "userParam_colorLightDevices_color", "number", title: "This Color", submitOnChange: true, required: true, width: 2
                        //input name: "userParam_colorLightDevices_brightness", type: "number", title: "This Brightness"
                    }
                    /*input name: "userParam_tempLightDevices", type: "capability.colorTemperature", title: "With these color temperature lights...", multiple: true
                    if (userParam_tempLightDevices) {
                        paragraph"Set level to"
                    }
                    input name: "userParam_dimemrLightDevices", type: "capability.switchLevel", title: "With these dimmers...", submitOnChange: true, multiple: true
                    if (userParam_dimemrLightDevices) {
                        input "userParam_colorLightDevices_brightness", "number", title: "This Brightness", submitOnChange: true, required: true
                    }
                    input name: "userParam_switchLightDevices", type: "capability.switch", title: "With these switches...", submitOnChange: true, multiple: true
                    if (userParam_switchLightDevices) {
                        paragraph"ON"
                    }
                }*/
                userParamSetLights.DayLightDevices = userParam_DayLightDevices
                userParamSetLights.EveLightDevices = userParam_EveLightDevices
                userParamSetLights.NteLightDevices = userParam_NightLightDevices
            }
            else{
                paragraph "<font style='font-size:$appSectionSize; color:#3300cc'><b>Standard Light Automation</b></font>"
                paragraph "<font style='font-size:12px; color:#000000'><b>Please Note: At this time, scene activators/switches are required here\n\n\n</b></font>"
                input name: "userParam_sceneLightDevices", type: "capability.switch", title: "Turn on these scenes when motion...", multiple: true

                paragraph "<font style='font-size:$appSectionSize; color:#696969'><b><s>\n\n\nIlluminessence Light Automation</s></b></font>\n"
                paragraph "<font style='font-size:12px; color:#696969'><b>This function is disabled as you need to enable illumination control\n\n</b></font>"
                userParamSetLights.DayLightDevices = userParam_sceneLightDevices } 
        }

        //Logging
        section() {
            paragraph "<font style='font-size:$appSectionSize; color:#3300cc'><b>Enable Logging?</b></font>\n"
            input name: "logEnable", type: "bool", title: "Enable logging?", submitOnChange: true
            //DebugDumper(userParamMotion)
            //DebugDumper(userParamIlum)
            
        }}
}
///==================Hubitat==================\\\
// Called when app first installed
def installed() {
	initialize() }

// Called when user presses "Done" button in app
def updated() {
	unsubscribe()
	initialize() }

// Called when user presses "Done" button in app
def initialize() {  
    //InsttallDebugChildren()
    if(userParam_MotionSensors) {
        DebugLog("initialize: Subscribing to $userParam_MotionSensors motion events")
        subscribe(userParam_MotionSensors, "motion", "motionSubHandler") }//Subscribe to motion events
    if(userParam_IllumSetting) {
        def averageDev = getChildDevice("AverageLux_${app.id}") //See if the child device already exists
        if(averageDev) {
            log.debug "initialize: Child device already exists" }
        else if(!averageDev) {
            log.debug "initialize: Child device does not exist... creating $textEntryVar"
            averageDev = addChildDevice("hubitat", "Virtual Illuminance Sensor", "AverageLux_${app.id}", null, [label: textEntryVar, name: textEntryVar]) }//Add the child device
        averageDev.setLux(averageLux())                  //Set the child devices lux to the average
        log.debug "initialize: Subscribing to $userParam_luxSensors illumination events"
        subscribe(userParam_luxSensors, "illuminance", luxSubhandler) //Subscribe to illumination events 
        def globalLightModeVar = getGlobalVar("LightMode")
        if ("$globalLightModeVar" == "null") {
            setGlobalVar("LightMode", "Z")
            log.debug "initialize: The global var has been setup"}
        else {
            log.debug "initialize:The global var has already been setup" } } 
}

///=================Handelers=================\\\
//Lux Event Handeler
def luxSubhandler(evt) {
    log.info "Lux Event Handeler Called"
    //Boolean setGlobalVar("LightMode", 0)
	def averageDev = getChildDevice("AverageLux_${app.id}")
    def averageVarold = averageLux()
    int averageVar = averageVarold as Integer
	averageDev.setLux(averageVar)
	log.info "Average illuminance = $averageVar lux"
    def lightMode = getGlobalVar("LightMode").value

    int dayEntryVarInt = dayEntryVar as Integer //1000 anything less then or greater thean 
    int eveEntryVarInt = eveEntryVar as Integer //400 anything less then this but not less then nte = eve 
    int nteEntryVarInt = nteEntryVar as Integer //50 anything less then this = night

    if (averageVar > eveEntryVarInt){ //&& lightMode != "D") {
        setGlobalVar("LightMode", "D")
        log.info "Global set to: D" }
    else if (averageVar > nteEntryVarInt && averageVar <= eveEntryVarInt){// && lightMode != "E"){
        setGlobalVar("LightMode", "E")
        log.info "Global set to: E" }
    else if (averageVar <= nteEntryVarInt){//  && lightMode != "N"){
        setGlobalVar("LightMode", "N")
        log.info "Global set to: N" }
    else {                             //Else if none of those throw error
        log.warn "luxSubhandler: DID NOT MATCH A SETTING" } }

//Motion Event Handeler add updates
def motionSubHandler(evt) {
    log.debug "motionSubHandeler: [Called] with $evt | $userParam_BypassDevices"
    if(!userParam_BypassDevices) overrideDeviceState = "off" else overrideDeviceState = userParam_BypassDevices.currentValue("switch") //UPDATE ME MULTI SUPPORT
    if (evt.value == "active") {
        log.debug "motionSubHandeler: calling setLights()"
        setLights() } 
    else {         
        if (userParam_DelaySetting && overrideDeviceState == "off") {
            multiplyer = 60//imedict.get("$userParam_OffTimeUnit")
            log.debug "motionSubHandeler: Motion Inactive - scheduling off in ${userParam_OffTimeValue} ${userParam_OffTimeUnit}"
            runIn(userParam_OffTimeValue*multiplyer, "delayedOffHandler") }
        else if (!userParam_DelaySetting && overrideDeviceState == "off"){
            log.debug "motionSubHandeler: Motion inactive - turning off [$userParam_offLightDevices] now"
            userParam_offLightDevices.off() }
        else if (overrideDeviceState == "on"){
            log.debug "motionSubHandeler: Motion off blocked by $userParam_BypassDevices subscribing to it & will resume normal opperation once off "
            subscribe(userParam_BypassDevices, "switch", "bypassedOffHandler") } } } //Subscribe to motion events


// For handeling the bypass device procedure
def bypassedOffHandler(evt) {
    log.debug "bypassedOffHandler: [Called] with $evt"
    motionState = userParam_BypassDevices[0].currentValue("motion")            //Get current motion
    if (evt.value == "off" && motionState == "inactive"){                      //If the bypassed device turns off, and motion is inactive turn off lights based on user setting
        unsubscribe(userParam_BypassDevices, "switch", "bypassedOffHandler")     //Unsubscrive from this event type as no longer needed
        if (!userParam_DelaySetting){                                            //Get Delay setting mode (Delayd)
            log.debug "bypassedOffHandler: Motion Inactive - scheduled off in ${userParam_OffTimeValue} ${userParam_OffTimeUnit}"
            multiplyer = 60//imedict.get("$userParam_OffTimeUnit")
            runIn(userParam_OffTimeValue*multiplyer, "delayedOffHandler") }         //Create an event to turn off the lights
        else {                                                                     //Get Delay setting mode (Instant)
            log.debug "motionSubHandeler: Motion inactive - turning off lights now" 
            userParam_LightDevices.off()  } }                                         //Turn lights off
    else if (evt.value == "off" && motionState == "active"){                   //If the bypassed device turned off but motion is active, do nothing
        unsubscribe(userParam_BypassDevices, "switch", "bypassedOffHandler")     //Unsubscrive from this event type as no longer needed
        log.debug "bypassedOffHandler: Bypass Device now off, motion active, not doing anything" } }

// For handeling the delayed off Procedure
def delayedOffHandler() {
    log.debug "delayedOffHandler: Called"
    if (!userParam_BypassDevices){
        log.info "delayedOffHandler: Turning off [$userParam_offLightDevices] now"
        userParam_offLightDevices.off() } 
    else{
        if (userParam_BypassDevices.currentValue("switch") == "on") {           //If for any reason bypass device is on, cancel
            log.warn "delayedOffHandler: Caught Exception - bypass active, will not proceed with request." }
        else if (userParam_BypassDevices.currentValue("motion") == "active" ){  //If for any reason motion device is active, cancel
            log.warn "delayedOffHandler: Caught Exception - motion active, will not proceed with request." } } 
}

///=================FUNCTIONS=================\\\
//Function to get the average LUX
def averageLux() {
	def total = 0
	def n = userParam_luxSensors.size()
	userParam_luxSensors.each {total += it.currentIlluminance}
	return (total / n).toDouble().round(0).toInteger() }

//For setting the lights (Illumination based or static)
def setLights(){
    if (checkLocationModeValid(userParamMode_OnlyInMode) == true) {
        if (userParam_IllumSetting){
            def sceneLightLookup = ["D" : userParam_DayLightDevices, "E" : userParam_EveLightDevices, "N" : userParam_NightLightDevices]
            def globalLightModeVar = getGlobalVar("LightMode").value
            selectedLights = sceneLightLookup.get(globalLightModeVar)
            if (selectedLights){
                log.debug"setLights: Setting $selectedLights to [ON]"
                selectedLights.on() } 
            else {
               log.warn "setLights: DID NOT MATCH A SETTING" }
            unschedule("delayedOffHandler") } 
        else{
            log.info "setLights: $userParam_DayLightDevices to [ON]"
            selectedLights.on()
            unschedule("delayedOffHandler") } }
    else{
        log.debug"setLights: Mode conditon not met to execute" } }

//Check if loccation mode matches users settings
def checkLocationModeValid(input){
    if (input == false){
        log.debug"checkLocationModeValid: No mode restrictions"
        return true }
    else if(input == true){
        getCurrentMode = location.getMode()
        if (userParamMode_selectedOnlyInMode.contains(getCurrentMode)){
            log.debug"checkLocationModeValid: Mode matches"
            return true }
        else {
            return false } }
    else {
        return false } 
}

///==============DEBUG FUNCTIONS==============\\\
def DebugLog(message){
    if (logEnable) log.debug "$message" }

def DebugDumper(dict){
    log.debug"********** VAR DUMP **********"
    for (key in dict.keySet()) { 
        var = dict[key]
        log.debug"$key :-: $var" }
    log.debug"******************************" }

def InsttallDebugChildren(){
    def testLux = getChildDevice("debugLux_${app.id}") //See if the child device already exists
    def debugMotion = getChildDevice("debugMotion_${app.id}") //See if the child device already exists
    def debugOverride = getChildDevice("debugOverride_${app.id}") //See if the child device already exists
    def debugContact = getChildDevice("debugContact_${app.id}") //See if the child device already exists
    if(!debugLux) {
        log.debug "initialize: Child device [debugLux] does not exist... creating $textEntryVar"
        debugLux = addChildDevice("hubitat", "Virtual Illuminance Sensor", "debugLux_${app.id}", null, [label: "a debugLux", name: "a debugLux"] ) }
    if(!debugMotion) {
        log.debug "initialize: Child device [debugMotion] does not exist... creating $textEntryVar"
        debugMotion = addChildDevice("hubitat", "Virtual Motion Sensor", "debugMotion_${app.id}", null, [label: "a debugMotion", name: "a debugMotion"] ) }
    if(!debugOverride) {
        log.debug "initialize: Child device [debugOverride] does not exist... creating $textEntryVar"
        debugOverride = addChildDevice("hubitat", "Virtual Motion Sensor", "debugOverride_${app.id}", null, [label: "a debugOverride", name: "a debugOverride"] ) }
    if(!debugContact) {
        log.debug "initialize: Child device [debugContact] does not exist... creating $textEntryVar"
        debugContact = addChildDevice("hubitat", "Virtual Motion Sensor", "debugContact_${app.id}", null, [label: "a debugContact", name: "a debugContact"] ) }

}
/*def singlDebugDumoer(dict, key){
    var=""
    var = dict[key]
    log.debug"$key :-: $var"

}
def DebugVariable(message, value) {
    getMsg = ""
    getVar = "$message"
    if (dumpVarsLog) log.debug "Variable: message is $value"
}*/