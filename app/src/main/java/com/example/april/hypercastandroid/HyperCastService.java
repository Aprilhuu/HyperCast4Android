package com.example.april.hypercastandroid;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import hypercast.HyperCastConfig;
import hypercast.I_LogicalAddress;
import hypercast.I_OverlayMessage;
import hypercast.I_OverlaySocket;
import hypercast.I_ReceiveCallback;

/**
 * HyperCast Service is a bound service that handles all the background
 * communications between sockets. A {@link OLSocketDetailActivity}
 * will bind to HyperCastService whenever an instance of OLSocketDetailActivity
 * is created, and unbind from the service when that instance is destroyed.
 */
public class HyperCastService extends Service {

    /**
     * Tag used for HyperCast Service
     */
    private static String TAG = "HyperCast Service";

    /**
     * Local Binder for binding with MainActivity
     */
    private LocalBinder mBinder = new LocalBinder();

    /**
     * True if the service is alive
     */
    private Boolean isStarted;

    /**
     * An array used to store all the HyperCast Adapters that are created
     */
    public static final List<HyperCastAdapter> ADAPTERS = new ArrayList<>();

    /**
     * Custom action String for broadcasting indicating that a message has been received
     * for the socket
     */
    final static String PASS_DATA = "PASS_DATA";


    class LocalBinder extends Binder{
        /**
         * Function called by OLSocketDetailActivity when Create/Join button is clicked
         * @param ConfigFilePath The absolute path to the configuration file in internal storage
         * @param Position_id The position of the socket in the list
         */
        public void create_join_Overlay(String ConfigFilePath, int Position_id){
            Log.d(TAG,"create_join_Overlay() called.");

            HyperCastAdapter adapter;
            try {
                adapter = new HyperCastAdapter();
                new Thread(adapter,"HyperCast Adapter_" + Position_id).start();
                adapter.Initialize_adapter(ConfigFilePath, Position_id);
            }catch (Exception e){
                Log.d(TAG,"create_join_Overlay(): Unable to create a thread for Hypercast " +
                        "Adapter due to Exception " + e.getMessage());
                return;
            }

            // Add or replace the newly started Hypercast Adapter to the list
            try{
                ADAPTERS.set(Position_id, adapter);
            } catch (IndexOutOfBoundsException e){
                ADAPTERS.add(Position_id ,adapter);
            }
        }

        /**
         * Function called by OLSocketDetailActivity when Send button is clicked
         * @param Message The payload message to be sent
          */
        public void Send_Message (String Message, int Position_id){
            Log.d(TAG,"Send_Message() called with Message = " + Message );
            ADAPTERS.get(Position_id).send_message(Message);
        }

        /**
         * Function called by OLSocketDetailActivity when Stop button is clicked
         */
        public void Stop_Socket(int Position_id){
            Log.d(TAG,"Stop_Socket() called.");
            ADAPTERS.get(Position_id).Shutdown_adapter();
            ADAPTERS.set(Position_id, null);
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG,"onCreate() called.");
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,"onBind() called with intent: " + intent);
        int Position_id = intent.getIntExtra("Position_id", -2);

        // Invalid Position ID has been obtained, Error
        if (Position_id == -2){
            return null;
        }

        // When a new socket is added to the list, add to that position in ADAPTERS
        // a null adapter
        if(Position_id + 1 > ADAPTERS.size()){
            ADAPTERS.add(null);
        }

        isStarted = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind() called with: intent = [" + intent + "]");
        isStarted = true;
        return false;
    }

    public HyperCastService(){
        isStarted = false;
    }

    /**
     * This class is used as an interface between HyperCast Service and codes
     * in HyperCast Package. It handles all the calling of functions in HyperCast.
     * While each HyperCast Adapter is started, a new socket and a new thread are
     * created at the same time. The newly created socket will run on the new thread
     * to avoid blocking the the Main (UI) thread. The mechanism used for the thread
     * is a state machine with four states as defined above.
     */
    public class HyperCastAdapter implements I_ReceiveCallback, Runnable {

        /**
         * Tag used for HyperCast Service
         */
        private String TAG = "HyperCast Interface";

        /**
         * Overlay Socket
         */
        private I_OverlaySocket overlaySocket;

        /**
         * Configuration object built from specified configuration file
         */
        private HyperCastConfig configObject;

        /**
         * Logical Address of this socket
         */
        private I_LogicalAddress address;

        /**
         * Position of this socket in the array list
         */
        private int Position_id;

        /**
         * State variables used in thread for each socket
         */
        private static final int CONNECTING = 1;
        private static final int SENDING = 2;
        private static final int STOPPING = 3;
        private static final int WAITING = 4;

        /**
         * Initialize the state machine to WAITING
         */
        private int state = WAITING;

        /**
         * Initialize the status of the thread to be active
         */
        volatile boolean running = true;

        /**
         * Absolute path to the configuration file
         */
        private String ConfigFilePath;

        /**
         * Message to be sent
         */
        private String Message;

        @Override
        public void run() {
            while(running){
                if(state == CONNECTING){
                    try{
                        // Create a configuration Object
                        configObject = HyperCastConfig.createConfig(ConfigFilePath);

                        // Create an overlay socket with the configuration object just created
                        overlaySocket = configObject.createOverlaySocket(this);

                        address = overlaySocket.getLogicalAddress();

                        Log.d(TAG,"Logical address is:  " + address);

                        // Join the overlay
                        overlaySocket.joinOverlay();

                    }catch (Exception e){
                        Toast.makeText(getApplicationContext(), "Failed to create/ join the overlay with " +
                                "error message: " + e.getMessage(),Toast.LENGTH_SHORT).show();
                    }

                    state = WAITING;
                }

                else if (state == SENDING){
                    // Convert the string to a byte array and create a message
                    byte[] Data_to_send = Message.getBytes();
                    I_OverlayMessage MyMessage = overlaySocket.createMessage(Data_to_send);
                    overlaySocket.sendToAll(MyMessage);

                    state = WAITING;
                }

                else if (state == STOPPING){
                    // Close all sockets
                    if (overlaySocket == null){
                        Log.d(TAG,"Shutdown_adapter(): all overaly sockets have been stopped." );
                    }

                    overlaySocket.leaveOverlay();
                    overlaySocket.closeSocket();

                    overlaySocket = null;
                    configObject = null;
                    address = null;

                    running = false;
                }
            }
        }

        /**
         * Function used to access the logical address of the socket
         * @return Logical Address of this socket
         */
        public I_LogicalAddress getAddress() {
            return address;
        }

        /**
         * Function called when initializing the adapter. State is changed to CONNECTING
         * to perform the actual creating process
         * @param ConfigFilePath_passed Absolute path to the configuration file
         * @param Socket_id Position of the socket in the array list
         */
        void Initialize_adapter(String ConfigFilePath_passed, int Socket_id){
            Log.d(TAG,"Initialize_adapter() called with file: " + ConfigFilePath);
            Position_id = Socket_id;
            ConfigFilePath = ConfigFilePath_passed;
            state = CONNECTING;
        }

        /**
         * Function used to send out messages. State is changed to SENDING to perform
         * the sending process in the thread
         * @param Message_passed Payload message to be sent
         */
        public void send_message (String Message_passed){
            Log.d(TAG,"send_message() called with Message = " + Message);
            Message = Message_passed;
            state = SENDING;
        }

        public void ReceiveCallback(I_OverlayMessage msg) {
            Log.d(TAG,"ReceiveCallback() called with I_OverlayMessage: " + msg + " for " + this.getAddress());

            // Skip messages sent by this program
            if (msg.getSourceAddress().equals(this.getAddress())){
                Log.d(TAG,"ReceiveCallback(): Message is sent by own device, skip.");
                return;
            }

            // Extract the payload and the logical address of the source
            byte[] data = msg.getPayload();
            String Src = msg.getSourceAddress().toString();

            // Send the source address and payload to the OLSocketDetailActivity which this
            // service is currently binding to
            Intent intent = new Intent();
            intent.setAction(PASS_DATA);

            Bundle msg_packet = new Bundle();
            msg_packet.putString("msg_received",new String(data));
            msg_packet.putString("src",Src);
            msg_packet.putInt("Position_id", Position_id);

            intent.putExtras(msg_packet);
            sendBroadcast(intent);
        }

        /**
         * Function used to shut down the socket and its associating thread. State is changed
         * to STOPPING so that the actual work can be carried out in run()
         * @return
         */
        public boolean Shutdown_adapter(){
            Log.d(TAG,"Shutdown_adapter() called.");
            state = STOPPING;
            return false;
        }
    }
}
