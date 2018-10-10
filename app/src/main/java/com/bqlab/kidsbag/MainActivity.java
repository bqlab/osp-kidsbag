package com.bqlab.kidsbag;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, Runnable {

    private static final String TAG = MainActivity.class.getSimpleName();

    boolean isConnected = false;

    Integer temp = 0;
    Boolean buzz = false;
    Double lat = (double) 0, lng = (double) 0;

    Button mainCommand;
    TextView mainTemperature;
    GoogleMap googleMap;

    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference = firebaseDatabase.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setMembers();
        startService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isConnected = false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(0, 0)));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(10));
        googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(0, 0))
                .title("현위치"));
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
                        synchronization();
                        setMapMarker(lat, lng);
                        setTemperature(temp);
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void synchronization() {
        buzz = ReceiveService.buzz;
        temp = ReceiveService.temp;
        lat = ReceiveService.lat;
        lng = ReceiveService.lng;
    }

    public void setMembers() {
        isConnected = true;
        new Thread(MainActivity.this).start();
        mainCommand = findViewById(R.id.main_command);
        mainCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText e = new EditText(MainActivity.this);
                new AlertDialog.Builder(MainActivity.this)
                        .setView(e)
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (e.getText().toString()) {
                                    case "ini":
                                        databaseReference.child("buzz").setValue(false);
                                        databaseReference.child("temp").setValue(0);
                                        databaseReference.child("lat").setValue(0);
                                        databaseReference.child("lng").setValue(0);
                                        break;
                                    case "cel-def":
                                        databaseReference.child("temp").setValue(28);
                                        break;
                                    case "cel-oh":
                                        databaseReference.child("temp").setValue(40);
                                        break;
                                    case "bz":
                                        databaseReference.child("buzz").setValue(true);
                                        break;
                                    case "map-se":
                                        databaseReference.child("lat").setValue(37.5);
                                        databaseReference.child("lng").setValue(126.9);
                                        break;
                                    case "map-ny":
                                        databaseReference.child("lat").setValue(40.7);
                                        databaseReference.child("lng").setValue(-74.2);
                                        break;
                                }
                            }
                        })
                        .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });
        mainTemperature = findViewById(R.id.main_temperature);

        FragmentManager fragmentManager = getFragmentManager();
        MapFragment mapFragment = (MapFragment) fragmentManager.findFragmentById(R.id.main_map);
        mapFragment.getMapAsync(this);
    }

    public void setMapMarker(double lat, double lng) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lng)));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(10));
        googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lng))
                .title("현위치"));
    }

    public void setTemperature(int temp) {
        String s = getResources().getString(R.string.main_temperature) + temp + (getResources().getString(R.string.main_temperature_cel));
        mainTemperature.setText(s);
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

    public void startService() {
        Intent i = new Intent(this, ReceiveService.class);
        i.putExtra("content", "디바이스와 실시간으로 데이터를 동기화하고 있습니다.");
        startService(i);
    }

    public void stopService() {
        stopService(new Intent(this, ReceiveService.class));
    }
}
