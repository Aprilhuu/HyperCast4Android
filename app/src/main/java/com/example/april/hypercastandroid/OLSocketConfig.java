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
import java.util.regex.Pattern;

/**
 * This activity is used when configuring TCP/UDP sockets is chosen. It is
 * started by a {@link ListAvailableConfigs} activity when TCP/UDP is
 * selected.
 *
 * When save button is clicked, a newly configured HyperCast 4 configuration
 * file for TCP/UDP socket will be saved to internal storage. Clicking the
 * save button will also lead the user back to the previous
 * {@link OLSocketDetailActivity} that handles this overlay socket.
 */
public class OLSocketConfig extends AppCompatActivity {

    /**
     * Tag used for OLSocketConfig
     */
    private static final String TAG = "OLSocket Configuration";

    /**
     * Name of the configuration file to be modified for TCP/UDP socket
     * DT protocol is right now supported by this configuration file
     */
    private static final String ConfigFileName = "hypercast.xml" ;

    /**
     * Pattern for validating IP address of user input
     */
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$");

    /**
     * Input bar for getting Overlay ID from users
     */
    private EditText OverlayID;

    /**
     * Input bar for getting Buddy Address from users
     */
    private EditText BuddyAdr;

    /**
     * Input bar for getting Port Number of this socket from users
     */
    private EditText PortNum;

    /**
     * Button to save the newly changed configurations to file
     */
    private Button SaveBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_olsocket_config);

        SaveBtn = (Button)findViewById(R.id.save_btn);
        OverlayID = (EditText) findViewById(R.id.OverlayID);
        BuddyAdr = (EditText) findViewById(R.id.BuddyAdr);
        PortNum = (EditText) findViewById(R.id.PortNum);

        SaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String overlay_id = OverlayID.getText().toString().trim();
                String buddy_adr = BuddyAdr.getText().toString().trim();
                String port_num = PortNum.getText().toString().trim();

                // Check User Inputs
                if (overlay_id.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Missing required field: ID.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (port_num.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Missing required field: Port " +
                                    "Number. Using default value: 9800.",
                            Toast.LENGTH_SHORT).show();
                    port_num = "9800";
                    PortNum.setText("9800");
                }

                int port;
                try{
                    port = Integer.parseInt(port_num);

                }catch (NumberFormatException e){
                    Toast.makeText(getApplicationContext(), "Invalid Port Number. " +
                                    "Please enter only numbers.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (port < 0 || port > 65535){
                    Toast.makeText(getApplicationContext(), "Port Number Out of Range " +
                            "(0 ~ 65535). Please enter a valid port number", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!buddy_adr.isEmpty()){
                    try{
                        String [] split = buddy_adr.split(":");
                        String IP_addr = split[0];
                        String Port_num = split[1];

                        if(!IPV4_PATTERN.matcher(IP_addr).matches()) {
                            Toast.makeText(getApplicationContext(), "Invalid IP address format. Please reenter.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        int portnum;
                        try{
                            portnum = Integer.parseInt(Port_num);

                        }catch (NumberFormatException e){
                            Toast.makeText(getApplicationContext(), "Invalid Port Number. " +
                                            "Please enter only numbers.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (portnum < 0 || portnum > 65535){
                            Toast.makeText(getApplicationContext(), "Port Number Out of Range " +
                                    "(0 ~ 65535). Please enter a valid port number", Toast.LENGTH_SHORT).show();
                            return;
                        }

                    }catch (ArrayIndexOutOfBoundsException e){
                        Toast.makeText(getApplicationContext(), "Please use : to separate IP " +
                                "address and Port number.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                try{
                    InitializeHyperCastConfigFile(overlay_id, buddy_adr, port_num);
                }catch (Exception e){
                    Log.d(TAG,"onCreate(): Initialize config file failed:" + e.getMessage());
                }

                // Send a broadcast message to set this socket as a TCP/UDP socket
                Intent intent = new Intent();
                intent.setAction(OLSocketConfigBT.SELECT_BT);
                intent.putExtra("SELECT_BT",false);
                sendBroadcast(intent);

                finish();
            }
        });

    }

    /**
     * This function is used to create a HyperCast 4 configuration file for TCP/UDP socket
     * from the template "hypercaset.xml"in the assets directory and save it in internal storage.
     * Note that part of this function should be modified when the template is changed.
     * @param OverlayID
     * @param BuddyAdr
     * @param PortNum
     * @return true if successful, and false otherwise
     * @throws Exception
     */
    private boolean InitializeHyperCastConfigFile (String OverlayID, String BuddyAdr, String PortNum) throws Exception{

        Log.d(TAG, "InitializeHyperCastConfigFile() called with Overlay ID: " + OverlayID +
                "Port Number: " + PortNum +"and Buddy address: " + BuddyAdr);

        String overlayAddress;
        try{
            // Call member function to get the IP address of the localhost
            overlayAddress = getIPAddr();
        }catch (SocketException e){
            throw new Exception("Please connect to a network and try again. + ", e);
        }

        // Concatenate IP Address with Port Number
        overlayAddress += ":" + PortNum;
        Log.d(TAG, "InitialHyperCastConfigFile(): Final Overlay address: " + overlayAddress);

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

                    // Set Adapter's Physical Address
                    case 47:
                        Log.d(TAG, "InitialHyperCastConfigFile(): Configuring INETV4AndOnePort");
                        line = "<INETV4AndOnePort>" + overlayAddress + "</INETV4AndOnePort>";
                        break;

                    // Set Addresses in Buddy List
                    case 62:
                        Log.d(TAG, "InitialHyperCastConfigFile(): Configuring InterfaceAddress");
                        if (BuddyAdr.isEmpty()){
                            // If no address in the buddy is provided, the host itself is a buddy
                            // in the overlay
                            BuddyAdr =  overlayAddress;
                        }
                        line = "<InterfaceAddress>udp1|" + BuddyAdr + "</InterfaceAddress>";
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
    private static String getIPAddr () throws SocketException{
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
