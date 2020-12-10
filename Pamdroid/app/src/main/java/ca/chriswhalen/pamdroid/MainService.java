package ca.chriswhalen.pamdroid;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.text.format.Formatter;

import java.io.IOException;

public class MainService extends Service
{
    private Integer port;

    private HTTPServer httpServer;
    private NotificationChannel channel;

    public MainService() {}

    private void createNotificationChannel()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            channel = new NotificationChannel(getString(R.string.channel_id), getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_MIN);

            channel.setDescription(getString(R.string.channel_description));

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (intent.hasExtra("port"))
            port = intent.getIntExtra("port",19200);

        else port = 19200;

        try
        {
            httpServer = new HTTPServer(this, port);
            httpServer.start();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        createNotificationChannel();

        if (channel == null) startForeground(1, new Notification());
        else startForeground(1, getNotification(ip, port));

        IntentFilter wifiFilter = new IntentFilter();
        wifiFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(wifiChanged, wifiFilter);

        return Service.START_STICKY;
    }

    private Notification getNotification(String ip, int port)
    {
        PendingIntent intent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this,
                getString(R.string.channel_id));

        notification.setSmallIcon(R.drawable.ic_fingerprint_white_24dp);
        notification.setContentTitle(ip + ":" + port);
        notification.setOngoing(true);
        notification.setContentIntent(intent);
        notification.setPriority(Notification.PRIORITY_MIN);

        return notification.build();
    }

    private final BroadcastReceiver wifiChanged =
    new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            NetworkInfo nwInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

            if (nwInfo.isConnected())
            {
                WifiInfo wfInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);

                NotificationManager notificationManager = (NotificationManager) getSystemService(
                        Context.NOTIFICATION_SERVICE);

                notificationManager.notify(1, getNotification(Formatter.formatIpAddress(
                        wfInfo.getIpAddress()),port));
            }
        }
    };

    @Override
    public void onDestroy()
    {
        stopForeground(true);
        httpServer.stop();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not implemented");
    }
}
