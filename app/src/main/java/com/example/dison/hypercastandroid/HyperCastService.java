package com.example.dison.hypercastandroid;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import hypercast.*;

public class HyperCastService extends Service {
    public HyperCastService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
