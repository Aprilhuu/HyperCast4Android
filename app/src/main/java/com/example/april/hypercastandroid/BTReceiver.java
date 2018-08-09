package com.example.april.hypercastandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * A manifest-declared broadcast receiver. The messages this receiver is dedicated
 * to receive are declared in AndroidManifest.xml
 */
public class BTReceiver extends BroadcastReceiver {

    /**
     * Tag used for BTReceiver
     */
    private static final String TAG = "BTReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        // When BT_Module has started the underlay
        if (action.equals("com.example.dison.hypercastandroid.START_MODULE")){
            Log.d(TAG, "Boardcast Message from BT_Module received.");
            if (intent.getBooleanExtra("MODULE_STARTED",true)){
                OLSocketListActivity.setModule_Started(true);
            }

            else{
                OLSocketListActivity.setModule_Started(false);
                // Stop all the bluetooth sockets in the list
                for (OLSocketListActivity.OLSocketItem item : OLSocketListActivity.ITEMS){
                    if (item.content == "Bluetooth Socket" && item.isStarted ){
                        HyperCastService.ADAPTERS.get(OLSocketListActivity.ITEMS.indexOf(item)).Shutdown_adapter();
                        item.isStarted = false;
                        item.details = "Status: Stopped";
                        item.msg += "Socket is stopped due to inactive BT_Module." + "\n";
                    }
                }
            }
        }

        // When users configured a bluetooth socket. Update socket type in list activity
        else if (action.equals("com.example.dison.hypercastandroid.BLUETOOTH")){
            Log.d(TAG, "Boardcast Message from Bluetooth Configuration received.");

            Bundle info_package = intent.getExtras();
            int position_id = Integer.parseInt(info_package.getString("position_id"));
            Boolean isBluetooth = info_package.getBoolean("isBluetooth");

            if (isBluetooth){
                OLSocketListActivity.ITEMS.get(position_id).content = "Bluetooth Socket";
            }

            else{
                OLSocketListActivity.ITEMS.get(position_id).content = "TCP/UDP Socket";
            }
        }

        // When users have started certain socket in the list. Update status
        // in list activity and database
        else if (action.equals("com.example.dison.hypercastandroid.STARTED")){
            Log.d(TAG, "Boardcast Message from Start Status Update received.");

            Bundle info_package = intent.getExtras();
            int position_id = Integer.parseInt(info_package.getString("position_id"));
            Boolean isStarted = info_package.getBoolean("isStarted");

            if (isStarted){
                OLSocketListActivity.ITEMS.get(position_id).details = "Status: Started";
                OLSocketListActivity.ITEMS.get(position_id).isStarted = true;
            }

            else{
                OLSocketListActivity.ITEMS.get(position_id).details = "Status: Stopped";
                OLSocketListActivity.ITEMS.get(position_id).isStarted = false;
            }
        }

        // When a message have been received by certain socket. Update message in
        // database to include the newly received message in case the socket is
        // running in background
        else if (action.equals("com.example.dison.hypercastandroid.MSG_RECEIVED")) {
            Log.d(TAG, "Boardcast Message from receive callback.");
            Bundle msg = intent.getExtras();

            String msg_received = msg.getString("msg_received");
            String src = msg.getString("src");
            int position_id = msg.getInt("Position_id");

            String msg_to_add = "Received Message <" + msg_received + "> from address " + src + "." + "\n";
            OLSocketListActivity.ITEMS.get(position_id).msg += msg_to_add;

        }
    }
}
