package ca.chriswhalen.pamdroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jesusm.kfingerprintmanager.KFingerprintManager;

import org.jetbrains.annotations.NotNull;

import de.adorsys.android.securestoragelibrary.SecurePreferences;

public class AuthActivity extends AppCompatActivity implements View.OnClickListener
{
    private Boolean registered;

    private Button authButton;
    private TextView statusText;

    private KFingerprintManager fingerprintManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.auth);
        authButton = (Button) findViewById(R.id.authButton);
        statusText = (TextView) findViewById(R.id.authDesc);

        authButton.setOnClickListener(this);
        authButton.setEnabled(false);

        fingerprintManager = new KFingerprintManager(this, getString(R.string.app_name));

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = pm.isInteractive();

        registered = false;

        if (isScreenOn)
        {
            attemptDecrypt();
        }
        else
        {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            registerReceiver(screenCheck, filter);
            registered = true;
        }
    }

    private final BroadcastReceiver screenCheck = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            attemptDecrypt();

            unregisterReceiver(screenCheck);
            registered = false;
        }
    };

    private void attemptDecrypt()
    {
        String secret = SecurePreferences.getStringValue(getString(R.string.secret),"");

        SecurePreferences.removeValue(getString(R.string.key));

        if (secret.equals("")) failAuth();

        fingerprintManager.decrypt(secret, new KFingerprintManager.DecryptionCallback()
        {
            @Override
            public void onDecryptionSuccess(@NotNull String message)
            {
                SecurePreferences.setValue(getString(R.string.key), message);
                returnToServer();
            }

            @Override
            public void onDecryptionFailed()
            {
                statusText.setText(R.string.error_crypto);
                failAuth();
            }

            @Override
            public void onFingerprintNotRecognized()
            {
                statusText.setText(R.string.error_unrecognized);
                authButton.setEnabled(true);
            }

            @Override
            public void onAuthenticationFailedWithHelp(@Nullable String help)
            {
                statusText.setText(help);
                authButton.setEnabled(true);
            }

            @Override
            public void onFingerprintNotAvailable()
            {
                statusText.setText(R.string.error_unavailable);
                failAuth();
            }

            @Override
            public void onCancelled()
            {
                statusText.setText(R.string.text_stopped);
                authButton.setEnabled(true);
            }
        }, getSupportFragmentManager());
    }

    @Override
    public void onClick(View view)
    {
        if (view.getId() == R.id.authButton) attemptDecrypt();
    }

    private void returnToServer()
    {
        synchronized (HTTPServer.syncToken) { HTTPServer.syncToken.notify(); }
        finish();
    }

    private void failAuth()
    {
        Toast.makeText(this, R.string.error_crypto, Toast.LENGTH_LONG).show();
        returnToServer();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if (registered)
        {
            unregisterReceiver(screenCheck);
            registered = false;
        }
    }
}
