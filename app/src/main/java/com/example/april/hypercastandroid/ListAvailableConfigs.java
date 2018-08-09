package com.example.april.hypercastandroid;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * This activity displays all the available socket configurations right now.
 * The fab button in {@link OLSocketDetailActivity} will start this
 * activity. By selecting the corresponding option in the list,
 * a {@link OLSocketConfig} for TCP/UDP, or a {@link OLSocketConfigBT}
 * for Bluetooth will be started.
 */
public class ListAvailableConfigs extends AppCompatActivity {

    /**
     * A list view used to list all the available socket configuration
     * options
     */
    ListView ConfigsListView;

    /**
     * A string array containing names of all options
     */
    String[] configs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_available_configs);

        // Names of all available socket configurations are declared in app/res/values/strings.xml
        // Retrieve all the names and store them in a string array called configs
        Resources res = getResources();
        ConfigsListView = (ListView) findViewById(R.id.ConfigList);
        configs = res.getStringArray(R.array.configs);

        ConfigsListView.setAdapter(new ArrayAdapter<String>(this,R.layout.activity_list_available_configs_detail,configs));

        ConfigsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0){
                    Intent TCP_UDP = new Intent(getApplicationContext(),OLSocketConfig.class);
                    startActivity(TCP_UDP);
                } else if (i == 1){
                    Intent Bluetooth = new Intent(getApplicationContext(),OLSocketConfigBT.class);
                    startActivity(Bluetooth);
                }
            }
        });
    }
}
