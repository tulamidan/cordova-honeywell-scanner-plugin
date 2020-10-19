package com.tmdiwakara.cordova.honeywell;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

public class HoneywellScannerPlugin extends CordovaPlugin {
    private static final String TAG = "HoneywellScanner";
    private static final String ACTION_BARCODE_DATA = "com.honeywell.action.BARCODE_DATA";
    /**
     * Honeywell DataCollection Intent API
     * Claim scanner
     * Permissions:
     * "com.honeywell.decode.permission.DECODE"
     */
    private static final String ACTION_CLAIM_SCANNER = "com.honeywell.aidc.action.ACTION_CLAIM_SCANNER";
    /**
     * Honeywell DataCollection Intent API
     * Release scanner claim
     * Permissions:
     * "com.honeywell.decode.permission.DECODE"
     */
    private static final String ACTION_RELEASE_SCANNER = "com.honeywell.aidc.action.ACTION_RELEASE_SCANNER";
    /**
     * Honeywell DataCollection Intent API
     * Optional. Sets the profile to use. If profile is not available or if extra is not used,
     * the scanner will use factory default properties (not "DEFAULT" profile properties).
     * Values : String
     */
    private static final String EXTRA_PROFILE = "com.honeywell.aidc.extra.EXTRA_PROFILE";
    private static final String EXTRA_SCANNER = "dcs.scanner.imager";
    /**
     * Honeywell DataCollection Intent API
     * Optional. Overrides the profile properties (non-persistent) until the next scanner claim.
     * Values : Bundle
     */
    private static final String EXTRA_PROPERTIES = "com.honeywell.aidc.extra.EXTRA_PROPERTIES";

    private CallbackContext callbackContext;

    private BroadcastReceiver barcodeDataReceiver;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        barcodeDataReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_BARCODE_DATA.equals(intent.getAction())) {
                    int version = intent.getIntExtra("version", 0);
                    if (version >= 1) {
                        setScannedData(intent.getStringExtra("data"));
                    }
                }
            }
        };
    }

    @Override
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("listenForScans")) {

            this.callbackContext = callbackContext;
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            this.callbackContext.sendPluginResult(result);

            //getActivity().getApplicationContext().registerReceiver(barcodeDataReceiver, new IntentFilter(ACTION_BARCODE_DATA));
			IntentFilter intent=new IntentFilter();
			intent.addAction(ACTION_BARCODE_DATA);
			//TODO: Honeywell needs the Category "android.intent.category.DEFAULT"
			intent.addCategory("android.intent.category.DEFAULT");
			getActivity().getApplicationContext().registerReceiver(this.barcodeDataReceiver, intent);

            claimScanner();

        }
        return true;
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        getActivity().getApplicationContext().registerReceiver(barcodeDataReceiver, new IntentFilter(ACTION_BARCODE_DATA));
        claimScanner();
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        getActivity().getApplicationContext().unregisterReceiver(barcodeDataReceiver);
        releaseScanner();
    }

    private Activity getActivity() {
        return this.cordova.getActivity();
    }

    private void NotifyError(String error) {
        if (this.callbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, error);
            result.setKeepCallback(true);
            this.callbackContext.sendPluginResult(result);
        }
    }

    private void setScannedData(String data) {
        if (this.callbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, data);
            result.setKeepCallback(true);
            this.callbackContext.sendPluginResult(result);
        }
    }

private void claimScanner() {
        //Log.d("IntentApiSample: ", "claimScanner");
        Bundle properties = new Bundle();
        properties.putBoolean("DPR_DATA_INTENT", true);
        properties.putString("DPR_DATA_INTENT_ACTION", ACTION_BARCODE_DATA);

        properties.putInt("TRIG_AUTO_MODE_TIMEOUT", 2);
        properties.putString("TRIG_SCAN_MODE", "readOnRelease"); //This works for Hardware Trigger only! If scan is started from code, the code is responsible for a switching off the scanner before a decode

        mysendBroadcast(new Intent(ACTION_CLAIM_SCANNER)
                .putExtra(EXTRA_SCANNER, "dcs.scanner.imager")
                .putExtra(EXTRA_PROFILE, "DEFAULT")// "MyProfile1")
                .putExtra(EXTRA_PROPERTIES, properties)
        );
    }
    private void releaseScanner() {
        //Log.d("IntentApiSample: ", "releaseScanner");
        mysendBroadcast(new Intent(ACTION_RELEASE_SCANNER));
    }

    private static void sendImplicitBroadcast(Context ctxt, Intent i) {
        PackageManager pm = ctxt.getPackageManager();
        List<ResolveInfo> matches = pm.queryBroadcastReceivers(i, 0);
        if (matches.size() > 0) {
            for (ResolveInfo resolveInfo : matches) {
                Intent explicit = new Intent(i);
                ComponentName cn =
                        new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName,
                                resolveInfo.activityInfo.name);

                explicit.setComponent(cn);
                ctxt.sendBroadcast(explicit);
            }

        } else{
            // to be compatible with Android 9 and later version for dynamic receiver
            ctxt.sendBroadcast(i);
        }
    }

    private  void mysendBroadcast(Intent intent){
        if(Build.VERSION.SDK_INT<26) {
            getActivity().getApplicationContext().sendBroadcast(intent);
        }else {
            //for Android O above "gives W/BroadcastQueue: Background execution not allowed: receiving Intent"
            //either set targetSDKversion to 25 or use implicit broadcast
            sendImplicitBroadcast(getActivity().getApplicationContext(), intent);
        }
       //sendImplicitBroadcast(getActivity().getApplicationContext(), intent);
    }



}
