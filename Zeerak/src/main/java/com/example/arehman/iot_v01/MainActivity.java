package com.example.arehman.iot_v01;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity {

    private MotorAdapter motorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_device_list);
        ListView gridview = (ListView) findViewById(R.id.listview);
        motorAdapter = new MotorAdapter(this);
        gridview.setAdapter(motorAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        motorAdapter.turnOffStateRefresher();
    }

    @Override
    protected void onResume() {
        super.onResume();
        motorAdapter.turnOnStateRefresher();
    }

}
