"use strict";

var exec = require('cordova/exec');

var Crashlytics = function(){

    var methods = [
        'log', 'setApplicationInstallationIdentifier',
        'setBool', 'setDouble', 'setFloat', 'setInt', 'setLong', 'setString', 'setUserEmail',
        'setUserIdentifier', 'setUserName', 'simulateCrash', 'logEvent'
    ];

    var execCall;
    var rippleMock = (window.parent && window.parent.ripple);
    if(rippleMock) {
        console.warn("navigator.crashlytics not defined : considering you're in dev mode and mocking it !");
        execCall = function(methodName, args){ console.log("[Crashlytics] Call to "+methodName+"("+Array.prototype.join.apply(args, [", "])+")"); }
    } else {
        execCall = function(methodName, args){ exec(null, null, "Crashlytics", methodName, args); };
    }

    var self = this;
    for(var i=0; i<methods.length; i++) {
        // Wrapping the callback definition call into a temporary function, otherwise i will always
        // be set to methods.length when calling execCall()
        (function(idx){
            var currentMethod = methods[idx];
            self[currentMethod] = function(){
                execCall(currentMethod,  Array.prototype.slice.call(arguments));
            };
        })(i);
    }
	
	this.logException = function(error) {
		
		if(error instanceof Error) {
			var parsed = ErrorStackParse.parse(error);
			execCall(null, null, "Crashlytics", "logException", [error.message, parsed]);
		} else {
			execCall(null, null, "Crashlytics", "logException", [error]);
		}
		
	}

    this.LOG_LEVELS = {
        VERBOSE: 2,
        DEBUG: 3,
        INFO: 4,
        WARN: 5,
        ERROR: 6,
        ASSERT: 7
    };
};

var crashlytics = new Crashlytics();

module.exports = crashlytics;
