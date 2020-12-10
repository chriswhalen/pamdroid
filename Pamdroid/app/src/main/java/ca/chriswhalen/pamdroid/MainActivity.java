package ca.chriswhalen.pamdroid;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.InputType;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{
    private Boolean active;
    private Integer port;

    private Button setupButton;
    private FloatingActionButton actionButton;
    private TextView statusText;

    private Intent serverIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        statusText = findViewById(R.id.statusText);
        actionButton = findViewById(R.id.actionButton);
        setupButton = findViewById(R.id.portButton);

        serverIntent = new Intent(this, MainService.class);
        serverIntent.putExtra("port", port);

        if (savedInstanceState != null)
        {
            port = savedInstanceState.getInt("port");
            active = savedInstanceState.getBoolean("active");
            if (!active) startServer();
        }
        else
        {
            port = 19200;
            startServer();
        }

        actionButton.setOnClickListener(this);
        setupButton.setOnClickListener(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, PersistableBundle outPersistentState)
    {
        outState.putBoolean("active", active);
        outState.putInt("port", port);

        super.onSaveInstanceState(outState, outPersistentState);
    }

    @SuppressLint("DefaultLocale")
    private void startServer()
    {
        String ip;

        setupButton.setEnabled(false);

        statusText.setText(getString(R.string.text_starting));

        startService(serverIntent);

        actionButton.setImageDrawable(ContextCompat.getDrawable(this,
                R.drawable.ic_stop_white_24dp));

        active = true;

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        statusText.setText(String.format("%s:%d", ip, port));
    }

    private void stopServer()
    {
        setupButton.setEnabled(true);

        stopService(serverIntent);

        actionButton.setImageDrawable(ContextCompat.getDrawable(this,
                R.drawable.ic_play_arrow_white_24dp));

        active = false;

        statusText.setText(R.string.text_stopped);
    }

    private void setPort()
    {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.text_port));

        EditText portInput = new EditText(this);
        portInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        dialog.setView(portInput);
        dialog.setCancelable(true);

        dialog.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());

        dialog.setPositiveButton("Set", (dialogInterface, i) ->
        {
            port = Integer.parseInt(portInput.getText().toString());
            serverIntent.removeExtra("port");
            serverIntent.putExtra("port", port);
            stopServer();
            startServer();
        });

        dialog.show();
    }

    @Override
    public void onClick(@NonNull View view)
    {
        if (view.getId() == R.id.actionButton)
        {
            if (active) stopServer();
            else startServer();
        }
        else if (view.getId() == R.id.portButton)
        {
            setPort();
        }
    }
}

