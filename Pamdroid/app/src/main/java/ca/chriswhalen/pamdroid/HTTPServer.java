package ca.chriswhalen.pamdroid;

import android.content.Context;
import android.content.Intent;

import org.jboss.aerogear.security.otp.Totp;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import de.adorsys.android.securestoragelibrary.SecurePreferences;
import fi.iki.elonen.NanoHTTPD;

public class HTTPServer extends NanoHTTPD
{
    public static final Object syncToken = new Object();
    private final int port;
    private final Context context;

    public HTTPServer(Context context, Integer port)
    {
        super(port);

        this.port = port;
        this.context = context;
    }

    public int getPort(){ return port; }

    @Override
    public Response serve(IHTTPSession session)
    {
        String uri = session.getUri();
        JSONObject msg = new JSONObject();

        // Return the identity of the server for verification purpose
        switch (uri)
        {
            case "/identity":
            {
                try
                {
                    msg.put("identity", "Pamdroid");
                    msg.put("version", 1);
                }
                catch (JSONException e) { e.printStackTrace(); }

                return newFixedLengthResponse(msg.toString());
            }

            // Return authentication token
            case "/token":
            {
                // Call auth activity
                Intent intent = new Intent(context, AuthActivity.class);
                context.startActivity(intent);

                // Pause until auth activity finished
                synchronized (syncToken)
                {
                    try { syncToken.wait(); }
                    catch (InterruptedException e) { e.printStackTrace(); }
                }

                // Get decrypted key
                String secret = SecurePreferences.getStringValue("key", "");

                // Check if there's problem
                if (secret != null && secret.equals(""))
                {
                    try { msg.put("token", "000000"); }
                    catch (JSONException e) { e.printStackTrace(); }
                }
                else
                {
                    // Create token
                    Totp theTotp = new Totp(secret);
                    String theToken;
                    theToken = theTotp.now();

                    // Setting up response message
                    try { msg.put("token", theToken); }
                    catch (JSONException e) { e.printStackTrace(); }

                    // Clear decrypted key
                    SecurePreferences.removeValue("key");
                }

                return newFixedLengthResponse(msg.toString());
            }

            // Store secret for setup
            case "/store":
            {
                // Get parameter
                String secret;

                try { secret = session.getParameters().get("secret").get(0); }
                catch (Exception e)
                {
                    e.printStackTrace();
                    secret = "";
                }

                // Check if secret received successfully
                if (!secret.equals(""))
                {
                    SecurePreferences.setValue("key", secret);

                    // Launching setup activity
                    Intent intent = new Intent(context, SetupActivity.class);
                    context.startActivity(intent);

                    // Wait for finish
                    synchronized (syncToken)
                    {
                        try { syncToken.wait(); }
                        catch (InterruptedException e) { e.printStackTrace(); }
                    }

                    if (SecurePreferences.getStringValue("decryptedKey", "").equals(""))
                    {
                        try { msg.put("status", "success"); }
                        catch (JSONException e) { e.printStackTrace(); }
                    }
                    else
                    {
                        try
                        {
                            msg.put("status", "error");
                            msg.put("error", "authFail");
                        }
                        catch (JSONException e) { e.printStackTrace(); }
                    }
                }
                else
                {
                    try
                    {
                        msg.put("status", "error");
                        msg.put("error", "secretFail");
                    }
                    catch (JSONException e) { e.printStackTrace(); }
                }

                return newFixedLengthResponse(msg.toString());
            }
        }

        return null;
    }
}
