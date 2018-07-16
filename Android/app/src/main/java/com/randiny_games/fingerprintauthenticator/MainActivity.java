package com.randiny_games.fingerprintauthenticator;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private MyHTTPD server;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = (TextView)findViewById(R.id.statusText);
        statusText.setText(getString(R.string.starting));

        Intent intent = new Intent(this, server.class);
        startService(intent);



        statusText.setText(getString(R.string.started));



    }


}

