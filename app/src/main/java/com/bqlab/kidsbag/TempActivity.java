package com.bqlab.kidsbag;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class TempActivity extends AppCompatActivity implements Runnable {

    boolean isConnected;
    Thread tempThread;
    TextView tempText;

    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference = firebaseDatabase.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tempThread.stop();
    }

    @Override
    public void run() {
        while (isConnected) {
            try {
                Thread.sleep(500);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        checkInternetState();
                        setTemperature();
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void init() {
        isConnected = getIntent().getBooleanExtra("login", false);
        tempThread = new Thread(TempActivity.this);
        tempThread.start();
        tempText = findViewById(R.id.temp_text);
    }

    public void setTemperature() {
        String s = ReceiveService.temp+"°C";
        tempText.setText(s);
    }

    public void checkInternetState() {
        ConnectivityManager mCM = (ConnectivityManager) this.getSystemService(Service.CONNECTIVITY_SERVICE);
        if (mCM != null) {
            NetworkInfo networkInfo = mCM.getActiveNetworkInfo();
            if ((networkInfo != null) && (networkInfo.getState() == NetworkInfo.State.CONNECTED)) {
                return;
            }
        }
        Toast.makeText(this, "인터넷이 연결되어 있지 않습니다.", Toast.LENGTH_LONG).show();
        finishAffinity();
    }
}
