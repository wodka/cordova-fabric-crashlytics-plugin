package org.apache.cordova.crashlytics;

import android.content.Context;
import android.app.Activity;
import java.util.Iterator;

import io.fabric.sdk.android.Fabric;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import sun.org.mozilla.javascript.internal.JavaScriptException;

import javax.security.auth.callback.Callback;

public class CrashlyticsPlugin extends CordovaPlugin {

    private Activity getActivity() {
        return (Activity)this.webView.getContext();
    }

    /**
     * Gets the application context from cordova's main activity.
     * @return the application context
     */
    private Context getApplicationContext() {
        return this.getActivity().getApplicationContext();
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Fabric.with(this.getApplicationContext(), new Crashlytics(), new Answers());
    }

    private static enum BridgedMethods {
        logException(1){
            @Override
            public void call(JSONArray args) throws JSONException {
//                String stackTraceString = args.getString(0).substring(args.getString(0).indexOf("stacktrace") + 1);
//
//                if(stackTraceString.isEmpty()) {
//                    return;
//                }
//
//                StackTraceElement ste = new StackTraceElement()
//
//
//                StackTraceElement[] trace = new StackTraceElement[] {
//                        new StackTraceElement("ClassName","methodName","fileName",10),
//                        new StackTraceElement("ClassName","methodName","fileName",11),
//                        new StackTraceElement("ClassName","methodName","fileName",12)
//                };
//
//                // sets the stack trace elements
//                RuntimeException e = new RuntimeException("broken");
//                e.setStackTrace(trace);
//
//                //javascriptexception
//                JavaScriptException e = new JavaScriptException();

                Crashlytics.logException(new JavaScriptException(args.getString(0)));
            }
        },
        log(1){
            @Override
            public void call(JSONArray args) throws JSONException {
                if (argsLengthValid(3, args)) {
                    Crashlytics.log(args.getInt(0), args.getString(1), args.getString(2));
                } else {
                    Crashlytics.log(args.getString(0));
                }
            }
        },
        setApplicationInstallationIdentifier(1){
            @Override
            public void call(JSONArray args) throws JSONException {
                Crashlytics.setUserIdentifier(args.getString(0));
            }
        },
        setBool(2){
            @Override
            public void call(JSONArray args) throws JSONException {
                Crashlytics.setBool(args.getString(0), args.getBoolean(1));
            }
        },
        setDouble(2){
            @Override
            public void call(JSONArray args) throws JSONException {
                Crashlytics.setDouble(args.getString(0), args.getDouble(1));
            }
        },
        setFloat(2){
            @Override
            public void call(JSONArray args) throws JSONException {
                Crashlytics.setFloat(args.getString(0), Float.valueOf(args.getString(1)));
            }
        },
        setInt(2){
            @Override
            public void call(JSONArray args) throws JSONException {
                Crashlytics.setInt(args.getString(0), args.getInt(1));
            }
        },
        setLong(2){
            @Override
            public void call(JSONArray args) throws JSONException {
                Crashlytics.setLong(args.getString(0), args.getLong(1));
            }
        },
        setString(2){
            @Override
            public void call(JSONArray args) throws JSONException {
                Crashlytics.setString(args.getString(0), args.getString(1));
            }
        },
        setUserEmail(1){
            @Override
            public void call(JSONArray args) throws JSONException {
                Crashlytics.setUserEmail(args.getString(0));
            }
        },
        setUserIdentifier(1){
            @Override
            public void call(JSONArray args) throws JSONException {
                Crashlytics.setUserIdentifier(args.getString(0));
            }
        },
        setUserName(1){
            @Override
            public void call(JSONArray args) throws JSONException {
                Crashlytics.setUserName(args.getString(0));
            }
        },
        simulateCrash(0, true){
            @Override
            public void call(JSONArray args) throws JSONException {
                String message = args.length() == 0 ? "This is a crash":args.getString(0);
                throw new RuntimeException(message);
            }
        },
        logEvent(2){
            @Override public void call(JSONArray args) throws JSONException {
                CustomEvent event = new CustomEvent(args.getString(0));
                if(args.length() > 1) {
                    for(Iterator<String> it = args.getJSONObject(1).keys(); it.hasNext();) {
                        String key = it.next();
                        String val = args.getJSONObject(1).getString(key);
                        event.putCustomAttribute(key, val);
                    }
                }
                Answers.getInstance().logCustom(event);
            }
        };

        int minExpectedArgsLength;
        boolean throwThrowable;
        BridgedMethods(int minExpectedArgsLength) {
            this(minExpectedArgsLength, false);
        }
        BridgedMethods(int minExpectedArgsLength, boolean throwThrowable) {
            this.minExpectedArgsLength = minExpectedArgsLength;
            this.throwThrowable = throwThrowable;
        }

        public abstract void call(JSONArray args) throws JSONException;

        public static boolean argsLengthValid(int minExpectedArgsLength, JSONArray args) throws JSONException {
            return (args != null && args.length() >= minExpectedArgsLength);
        }

        public Runnable runnableFrom(final CallbackContext callbackContext, final JSONArray args) {
            final BridgedMethods method = this;
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        method.call(args);
                        callbackContext.success();
                    } catch (Throwable t) {
                        callbackContext.error(t.getMessage());
                        if (method.throwThrowable) {
                            throw new RuntimeException(t);
                        }
                    }
                }
            };
        }
    }

    @Override
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        try {
            final BridgedMethods bridgedMethods = BridgedMethods.valueOf(action);
            if (bridgedMethods != null) {
                if (!BridgedMethods.argsLengthValid(bridgedMethods.minExpectedArgsLength, args)) {
                    callbackContext.error(String.format("Unsatisfied min args length (expected=%s)", bridgedMethods.minExpectedArgsLength));
                    return true;
                }

                Runnable runnable = bridgedMethods.runnableFrom(callbackContext, args);
                cordova.getThreadPool().execute(runnable);

                return true;
            }
        }catch(IllegalArgumentException e) { // Didn't found any enum value corresponding to requested action
            return false;
        }

        return false;
    }
}
