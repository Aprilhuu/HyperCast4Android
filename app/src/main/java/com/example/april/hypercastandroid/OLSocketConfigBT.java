package com.example.april.hypercastandroid;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

/**
 * This activity is used when configuring Bluetooth sockets is chosen. It is
 * started by a {@link ListAvailableConfigs} activity when Bluetooth is
 * selected.
 *
 * When save button is clicked, a newly configured HyperCast 4 configuration
 * file for Bluetooth socket will be saved to internal storage. Clicking the
 * save button will also lead the user back to the previous
 * {@link OLSocketDetailActivity} that handles this overlay socket.
 */
public class OLSocketConfigBT extends AppCompatActivity {

    /**
     * Tag used for OLSocketConfigBT
     */
    private static final String TAG = "OLSocketConfigurationBT";

    /**
     * Name of the configuration file to be modified for Bluetooth socket
     * SPT protocol is right now supported by this configuration file
     */
    private static final String ConfigFileName = "Bluetooth_Interface_SPT.xml" ;

    /**
     * Custom action String for broadcasting indicating that Bluetooth mode
     * has been selected for this socket when save button is clicked
     */
    final static String SELECT_BT = "SELECT_BT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_olsocket_config_bt);

        Button SaveBtn = (Button)findViewById(R.id.save_btn_BT);
        final EditText OverlayID = (EditText) findViewById(R.id.overlayID_BT);
        final EditText BT_Module = (EditText) findViewById(R.id.BT_ModuleAddr);

        SaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String overlay_id = OverlayID.getText().toString().trim();
                String bt_module = BT_Module.getText().toString().trim();

                // Check User Input
                if (overlay_id.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Missing required field: ID.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (bt_module.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Using default address and port number" +
                                    ": Device's IP Address and Port 9800",
                            Toast.LENGTH_SHORT).show();

                    try{
                        // Call member function to get the IP address of the localhost
                        bt_module = getIPAddr();
                    }catch (SocketException e){
                        Toast.makeText(getApplicationContext(), "Please connect to a network and try again." + e, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Concatenate with Port Number
                    bt_module += ":" + "9800";
                    BT_Module.setText(bt_module);
                    Log.d(TAG, "InitialHyperCastConfigFile(): Final BT_Module address: " + bt_module);
                }

                try{
                    InitializeHyperCastConfigFile(overlay_id, bt_module);
                }catch (Exception e){
                    Log.d(TAG,"onCreate(): Initialize config file:" + e.getMessage());
                }

                // Send a broadcast message to set this socket as a bluetooth socket
                Intent intent = new Intent();
                intent.setAction(SELECT_BT);

                intent.putExtra("SELECT_BT",true);
                sendBroadcast(intent);

                finish();
            }
        });

    }

    /**
     * This function is used to create a HyperCast 4 configuration file for Bluetooth socket
     * from the template "Bluetooth_Interface_SPT.xml" in the assets directory and save
     * it in internal storage. Note that part of this function should be modified when the
     * template is changed.
     * @param OverlayID
     * @param BTModuleAddr Address of the underlay BT_Module
     * @return true if successful, and false otherwise
     * @throws Exception
     */
    private boolean InitializeHyperCastConfigFile (String OverlayID, String BTModuleAddr) throws Exception{

        Log.d(TAG, "InitializeHyperCastConfigFile() called with Overlay ID: " + OverlayID +
                "BT_Module Address: " + BTModuleAddr);

        // Construct a Configuration File in Asset Folder from the template in assets Folder
        try {
            // Open an input buffer from the template config file stored in assets directory
            InputStream inputStream = getApplicationContext().getAssets().open(ConfigFileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

            // Open an output stream in Internal Storage
            FileOutputStream fos = openFileOutput(ConfigFileName, Context.MODE_PRIVATE);
            PrintStream output = new PrintStream(fos);

            // Replace OverlayID, OverlayAddress and Interface Address
            // This part of the code is template dependent
            String line;
            int lineCount = 1;
            while ((line = br.readLine()) != null){
                switch (lineCount){
                    // Set OverlayID
                    case 5:
                        Log.d(TAG, "InitialHyperCastConfigFile(): Configuring Overlay ID");
                        line = "<OverlayID>" + OverlayID + "</OverlayID>";
                        break;

                    // Set address of BT_Module for TCP connection
                    case 95:
                        Log.d(TAG, "InitialHyperCastConfigFile(): Configuring BTModuleAddress");
                        line = "<BTModuleAddress>" + BTModuleAddr + "</BTModuleAddress>";
                        break;

                    default:
                        // Skip, just go to the next line
                        break;
                }
                output.println(line);
                lineCount ++;
            }

            fos.flush();
            fos.close();
            output.close();
            br.close();

            Log.d(TAG, "InitialHyperCastConfigFile(): Configuration file modified successfully.");
            return true;

        }  catch (Exception e){
            throw new Exception("InitialHyperCastConfigFile(): Failed to modify Configuration file.", e);
        }

    }

    /**
     * Gets IP address from the first non-localhost interface. Requires INTERNET
     * permission in AndroidManifest.xml.
     * @return a string representing the IP address
     * @throws SocketException if fails to get network interfaces
     */
    private static String getIPAddr () throws SocketException {
        Log.d(TAG, "getIPAddr(): Getting IP address...");

        List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        for (NetworkInterface networkInterface : interfaces){
            List<InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
            for (InetAddress inetAddress : addresses) {
                if (inetAddress.isLoopbackAddress()) {
                    continue;
                }
                String address = inetAddress.getHostAddress();
                // IPv4 address
                if (inetAddress instanceof Inet4Address) {
                    return address;
                }
            }
        }
        throw new SocketException("Failed to find the IP address of locol host.");
    }


}

