package com.example.april.hypercastandroid;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * An activity representing a single OL_Socket detail screen. This
 * activity is only used on narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link OLSocketListActivity}.
 */
public class OLSocketDetailActivity extends AppCompatActivity {

    /**
     * TAG used for OLSocketDetailActivity in Logcat debugging
     */
    private static final String TAG = "OLSocketDetailActivity";

    /**
     * Binder for bounding to the HyperCastService
     */
    private HyperCastService.LocalBinder mBinder;

    /**
     * Floating Action Button for configuration
     */
    private FloatingActionButton fab;

    /**
     * Start button for creating/ joining an overlay network
     */
    private Button Create_Join;

    /**
     * Send button for sending messages
     */
    private Button Send;

    /**
     * Stop button for stopping the socket
     */
    private Button Stop;

    /**
     * Console window used to print out messages received
     */
    private TextView console;

    /**
     * Input bar for getting input messages from users
     */
    private EditText Message;

    /**
     * True if the service is bound
     */
    private boolean isBound = false;

    /**
     * True if Bluetooth mode is selected for this socket by configuring BT xml file
     */
    private Boolean isBluetooth;

    /**
     * True if the socket is running
     */
    private Boolean isStarted = false;

    /**
     * True if the underlay in BT_Module App has been started
     */
    private Boolean Module_Started = false;

    /**
     * Position of the socket in the list in string form
     */
    private String Socket_id;

    /**
     * Position of the socket in the list in integer form
     */
    private int Position_id;

    /**
     * Name of the configuration file for TCP/UDP
     */
    private static final String ConfigFileName = "hypercast.xml" ;

    /**
     * Name of the configuration file for Bluetooth
     */
    private static final String ConfigFileNameBT = "Bluetooth_Interface_SPT.xml" ;

    /**
     * Custom action String for broadcasting indicating that BT_Module has already been
     * started as the underlay
     */
    final static String MODULE_STARTED = "com.example.dison.hypercastandroid.START_MODULE";

    /**
     * Custom action String for broadcasting indicating that the socket has been started
     * and is active
     */
    final static String STARTED = "com.example.dison.hypercastandroid.STARTED";

    /**
     * Custom action String for broadcasting indicating that a message has been received by
     * this socket and its database should be updated to contain the newly received message
     */
    final static String MSG_RECEIVED = "com.example.dison.hypercastandroid.MSG_RECEIVED";

    /**
     * Custom action String for broadcasting indicating that the socket has been configured
     * to use bluetooth
     */
    final static String BLUETOOTH = "com.example.dison.hypercastandroid.BLUETOOTH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate() called.");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_olsocket_detail);

        // Setup to indicate whether the underlay Bluetooth Module has been started or not
        Module_Started = OLSocketListActivity.getModule_Started();

        // Get the Position of this socket in the list
        Intent start_intent = getIntent();
        Socket_id = start_intent.getStringExtra(OLSocketDetailFragment.ARG_ITEM_ID);
        Position_id = Integer.parseInt(Socket_id);
        Log.d(TAG,"Position_id is " + Position_id);

        // Update the console window to retrieve past messages
        console = findViewById(R.id.console);
        console.append(OLSocketListActivity.ITEMS.get(Position_id).msg);

        if (OLSocketListActivity.ITEMS.get(Position_id).content == "Bluetooth Socket"){
            isBluetooth = true;
        }else {
            isBluetooth = false;
        }

        // Setup for Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        // Setup for Floating Action Button
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent startConfig = new Intent (getApplicationContext(),ListAvailableConfigs.class);
                startActivity(startConfig);
            }
        });

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Setup for Create/Join Button
        Create_Join = (Button) findViewById(R.id.create_join_button);
        Create_Join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isBluetooth && !Module_Started){
                    Toast.makeText(getApplicationContext(), "Please start your BT_Module" +
                            " first.",Toast.LENGTH_SHORT).show();
                    return;
                }

                EnableMessage();

                // Print out configured information[Overlay ID/ Physical Address/ Buddy List] for console
                try{
                    String configFile;
                    InputStream inputStream;
                    BufferedReader br;

                    if(isBluetooth){
                        configFile = getFilesDir().getAbsolutePath() + File.separator + ConfigFileNameBT;
                        inputStream = new FileInputStream(configFile);
                        br = new BufferedReader(new InputStreamReader(inputStream));

                        console.append("Joining/ Creating overlay over Bluetooth with:" + "\n");
                        String line;
                        int lineCount = 1;
                        while ((line = br.readLine()) != null){
                            switch (lineCount){
                                // OverlayID
                                case 5:
                                    String substring = line.substring(11);
                                    String [] split = substring.split("<");
                                    String OverlayID = split[0];
                                    console.append("Overlay ID: " + OverlayID + "\n");
                                    break;

                                // BT_Module's Address
                                case 95:
                                    substring = line.substring(17);
                                    split = substring.split("<");
                                    String BT_Module = split[0];
                                    console.append("BT_Module Address: " + BT_Module + "\n");
                                    break;

                                default:
                                    // Skip, just go to the next line
                                    break;
                            }
                            lineCount ++;
                        }
                    } else {
                        configFile = getFilesDir().getAbsolutePath() + File.separator + ConfigFileName;
                        inputStream = new FileInputStream(configFile);
                        br = new BufferedReader(new InputStreamReader(inputStream));

                        console.append("Joining/ Creating overlay over TCP/UDP with:" + "\n");
                        String line;
                        int lineCount = 1;
                        while ((line = br.readLine()) != null){
                            switch (lineCount){
                                // OverlayID
                                case 5:
                                    String substring = line.substring(11);
                                    String [] split = substring.split("<");
                                    String OverlayID = split[0];
                                    console.append("Overlay ID: " + OverlayID + "\n");
                                    break;

                                // Adapter's Physical Address
                                case 47:
                                    substring = line.substring(18);
                                    split = substring.split("<");
                                    String PhysicalAddr = split[0];
                                    OLSocketListActivity.ITEMS.get(Position_id).physicaladdr = PhysicalAddr;
                                    console.append("Physical Address: " + PhysicalAddr + "\n");
                                    break;

                                // Addresses in Buddy List
                                case 62:
                                    substring = line.substring(18);
                                    split = substring.split("<");
                                    String BuddyList = split[0];
                                    console.append("Buddy List: " + BuddyList + "\n");
                                    break;

                                default:
                                    // Skip, just go to the next line
                                    break;
                            }
                            lineCount ++;
                        }
                    }

                    br.close();

                    mBinder.create_join_Overlay(configFile, Position_id);
                    isStarted = true;

                } catch (IOException e){
                    Toast.makeText(getApplicationContext(), "Cannot open the configuration " +
                            "file.",Toast.LENGTH_SHORT).show();
                }

                // Using boardcast to set the description in listview to status: started
                Intent set_start_status = new Intent();
                set_start_status.setAction(STARTED);

                Bundle Start_info = new Bundle();
                Start_info.putString("position_id",Socket_id);

                Start_info.putBoolean("isStarted",true);
                set_start_status.putExtras(Start_info);
                sendBroadcast(set_start_status);

            }
        });

        // Get Message input
        Message = (EditText) findViewById(R.id.message_input);

        // Setup for Send button
        Send = (Button) findViewById(R.id.send_button);
        Send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message_to_send = Message.getText().toString();
                mBinder.Send_Message(message_to_send, Position_id);

                if (message_to_send.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Please enter a message.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                console.append("Sending message <" + message_to_send + "> to all nodes in " +
                        "the overlay." + "\n");
                Message.getText().clear();
            }
        });

        // Set up for Stop button
        Stop = (Button) findViewById(R.id.stop_button);
        Stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBinder.Stop_Socket(Position_id);
                DisableMessage();

                if (isBluetooth){
                    console.append("Socket connecting to BT_Module is successfully stopped." + "\n");
                } else {
                    console.append("Socket at " + OLSocketListActivity.ITEMS.get(Position_id).physicaladdr
                            + " is successfully stopped." + "\n");
                }

                isBluetooth = false;
                isStarted = false;

                // Set the description in listview to status: stopped
                Intent set_start_status = new Intent();
                set_start_status.setAction(STARTED);

                Bundle Start_info = new Bundle();
                Start_info.putString("position_id",Socket_id);

                Start_info.putBoolean("isStarted",false);
                set_start_status.putExtras(Start_info);
                sendBroadcast(set_start_status);
            }
        });

        // Binding to the service defined in HyperCastService
        try {
            Intent intent = new Intent (OLSocketDetailActivity.this, HyperCastService.class);
            intent.putExtra("Position_id",Position_id);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            isBound = true;
        } catch (Exception e){
            Toast.makeText(getApplicationContext(), "Binding to the service failed. " + e,
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        // Register local intent receivers
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(HyperCastService.PASS_DATA);
        intentFilter.addAction(OLSocketConfigBT.SELECT_BT);
        intentFilter.addAction(MODULE_STARTED);
        registerReceiver(mReceiver,intentFilter);

        // Disable/ Enable buttons and input bar based on current status of the socket
        if (OLSocketListActivity.ITEMS.get(Position_id).isStarted == true){
            EnableMessage();
        } else {
            DisableMessage();
        }

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
//        if (savedInstanceState == null) {
//            // Create the detail fragment and add it to the activity
//            // using a fragment transaction.
//            Bundle arguments = new Bundle();
//            arguments.putString(OLSocketDetailFragment.ARG_ITEM_ID,
//                    getIntent().getStringExtra(OLSocketDetailFragment.ARG_ITEM_ID));
//            OLSocketDetailFragment fragment = new OLSocketDetailFragment();
//            fragment.setArguments(arguments);
//            getSupportFragmentManager().beginTransaction()
//                    .add(R.id.olsocket_detail_container, fragment)
//                    .commit();
//        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG,"onStart() called.");
        Module_Started = OLSocketListActivity.getModule_Started();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"onStop() called.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy() called.");

        // Update database for message before destroy
        OLSocketListActivity.ITEMS.get(Position_id).msg = console.getText().toString();

        if(isBound) {
            unbindService(mConnection);
        }
        unregisterReceiver(mReceiver);

        isBound = false;
        isBluetooth = false;
        isStarted = false;
        Module_Started = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"onPause() called.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume() called.");

        Module_Started = OLSocketListActivity.getModule_Started();
        if (OLSocketListActivity.ITEMS.get(Position_id).isStarted == true){
            EnableMessage();
        } else {
            DisableMessage();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpTo(this, new Intent(this, OLSocketListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     *  Methods being called when bindservice and unbindservice are called
      */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG,"onServiceConnected");
            mBinder = (HyperCastService.LocalBinder) iBinder;
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG,"onServiceDisconnected: Connection to the service is unexpectedly lost.");
            isBound = false;
        }
    };

    /**
     * Broadcast Receiver using to receive three types of local broadcast messages
      */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // If a new message has been received
            if (HyperCastService.PASS_DATA.equals(action)) {
                Log.d(TAG, "Boardcast Message from HyperCastService for Message Received " +
                        "has been received.");

                Bundle msg;
                String msg_received;
                String src;
                int Position_num;

                try {
                    msg = intent.getExtras();
                    msg_received = msg.getString("msg_received");
                    src = msg.getString("src");
                    Position_num = msg.getInt("Position_id");
                }catch (NullPointerException e){
                    Toast.makeText(getApplicationContext(), "Failed to get content of new " +
                                    "message received. " + e,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // If the message is for the current socket in the activity
                if (Position_num == Position_id){
                    String msg_to_console = "Received Message <" + msg_received + "> from address " + src + "." + "\n";
                    console.append(msg_to_console);
                }

                // If the message is for a socket running in the background
                else{
                    // Send the source address and payload to Broadcast receiver to update database
                    Intent update_database = new Intent();
                    update_database.setAction(MSG_RECEIVED);

                    update_database.putExtras(msg);
                    sendBroadcast(update_database);
                }
            }

            // If Bluetooth or TCP/UDP has been configured
            else if (OLSocketConfigBT.SELECT_BT.equals(action)){
                Log.d(TAG, "Boardcast Message from Configuration has been received.");

                Intent set_BT = new Intent();
                set_BT.setAction(BLUETOOTH);

                Bundle Config_info = new Bundle();
                Config_info.putString("position_id",Socket_id);

                if (intent.getBooleanExtra("SELECT_BT",true)){
                    isBluetooth = true;
                    Config_info.putBoolean("isBluetooth",true);
                } else{
                    isBluetooth = false;
                    Config_info.putBoolean("isBluetooth",false);
                }

                set_BT.putExtras(Config_info);
                sendBroadcast(set_BT);
            }

            else if (MODULE_STARTED.equals(action)){
                Log.d(TAG, "Boardcast Message from BT_Module received.");

                if (intent.getBooleanExtra("MODULE_STARTED",true)){
                    Module_Started = true;
                } else{
                    if (isStarted && isBluetooth){
                        mBinder.Stop_Socket(Position_id);
                        isBluetooth = false;
                        DisableMessage();
                        console.append("Socket is stopped due to inactive BT_Module." + "\n");
                        isStarted = false;
                        OLSocketListActivity.ITEMS.get(Position_id).isStarted = false;
                        OLSocketListActivity.ITEMS.get(Position_id).details = "Status: Stopped";
                    }
                    Module_Started = false;
                }
            }
        }
    };

    private void EnableMessage(){
        Log.d(TAG,"EnableMessage() called.");

        // Enable message input, stop
        Message.setEnabled(true);
        Send.setEnabled(true);
        Stop.setEnabled(true);

        // Disable Create/ Join, Configuration modification
        fab.setEnabled(false);
        Create_Join.setEnabled(false);
    }

    public void DisableMessage(){
        Log.d(TAG,"DisableMessage() called.");

        // Disable message input, stop
        Message.setEnabled(false);
        Send.setEnabled(false);
        Stop.setEnabled(false);

        // Enable Create/ Join, Configuration modification
        fab.setEnabled(true);
        Create_Join.setEnabled(true);
    }
}




