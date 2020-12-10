package ca.chriswhalen.pamdroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jesusm.kfingerprintmanager.KFingerprintManager;

import org.jetbrains.annotations.NotNull;

import de.adorsys.android.securestoragelibrary.SecurePreferences;

public class SetupActivity extends AppCompatActivity implements View.OnClickListener
{
    private Boolean registered;
    private String secret;

    private Button retryButton;
    private TextView statusText;

    private KFingerprintManager fingerprintManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup);

        statusText = (TextView) findViewById(R.id.setupStatus);
        retryButton = (Button) findViewById(R.id.setupRetryButton);

        retryButton.setOnClickListener(this);
        retryButton.setEnabled(false);

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
            attemptEncrypt();
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
            attemptEncrypt();

            unregisterReceiver(screenCheck);
            registered = false;
        }
    };

    private void attemptEncrypt()
    {
        secret = SecurePreferences.getStringValue(getString(R.string.key),"");

        fingerprintManager.encrypt(secret, new KFingerprintManager.EncryptionCallback()
        {
            @Override
            public void onEncryptionSuccess(@NotNull String message)
            {
                secret = null;
                SecurePreferences.removeValue(getString(R.string.key));
                SecurePreferences.setValue(getString(R.string.secret), message);
                returnToServer();
            }

            @Override
            public void onEncryptionFailed()
            {
                statusText.setText(R.string.error_crypto);
                returnToServer();
            }

            @Override
            public void onFingerprintNotRecognized()
            {
                statusText.setText(R.string.error_unrecognized);
                retryButton.setEnabled(true);
            }

            @Override
            public void onAuthenticationFailedWithHelp(@Nullable String help)
            {
                statusText.setText(help);
                retryButton.setEnabled(true);
            }

            @Override
            public void onFingerprintNotAvailable()
            {
                statusText.setText(R.string.error_unavailable);
                returnToServer();
            }

            @Override
            public void onCancelled()
            {
                statusText.setText(R.string.text_stopped);
                retryButton.setEnabled(true);
            }
        }, getSupportFragmentManager());
    }

    private void returnToServer()
    {
        synchronized (HTTPServer.syncToken) { HTTPServer.syncToken.notify(); }
        finish();
    }

    @Override
    public void onClick(View view)
    {
        if (view.getId() == R.id.setupRetryButton) attemptEncrypt();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if (registered) unregisterReceiver(screenCheck);

        returnToServer();
    }
}
