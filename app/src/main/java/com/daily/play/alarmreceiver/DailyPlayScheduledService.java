package com.daily.play.alarmreceiver;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.daily.play.R;
import com.daily.play.activities.MainActivity;
import com.daily.play.exceptions.NoSpaceException;
import com.daily.play.exceptions.NoWifiException;
import com.daily.play.managers.DailyPlayMusicManager;
import com.daily.play.utils.DailyPlaySharedPrefUtils;
import com.daily.play.utils.LogUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Jordan on 6/30/2014.
 */
public class DailyPlayScheduledService extends IntentService{
    public static final String DAILY_PLAY_SCHEDULED_SERVICE = "DailyPlayScheduledService";
    public static final int NOTIFICATION_ID = 1;

    public DailyPlayScheduledService() {
        super(DAILY_PLAY_SCHEDULED_SERVICE);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        DailyPlaySharedPrefUtils.init(getApplication());
        sendNotification("DailyPlay", "Starting to download your new DailyPlay list.");
        DailyPlayMusicManager dailyPlayMusicManager = DailyPlayMusicManager.getInstance();
        try {
            dailyPlayMusicManager.login();
            dailyPlayMusicManager.getDailyPlayMusic(getApplicationContext());
            sendNotification("DailyPlay list downloaded!", "Your songs were successfully downloaded.  Enjoy your new DailyPlay list!");
        } catch(NoWifiException e) {
            Log.e("DailyPlay", e.toString());
            e.printStackTrace();
            sendNotification("Something went wrong.", "Your device was not connected to Wifi. We were unable to download a new DailyPlay list.");
        } catch(NoSpaceException e) {
            Log.e("DailyPlay", e.toString());
            e.printStackTrace();
            sendNotification("Something went wrong.", "There was not enough space to download a new DailyPlay list.  Try freeing up some space and downloading again.");
        } catch (Exception e) {
            Log.e("DailyPlay", e.toString());
            e.printStackTrace();
            LogUtils.appendLog(e);
            sendNotification("Something went wrong.", "An error occurred while trying to download your DailyPlay list.");
        }

        Date date = new Date(System.currentTimeMillis());
        DateFormat format = new SimpleDateFormat("HH:mm:ss");
        Log.i("DailyPlay - downloaded@", format.format(date));
        DailyPlayAlarmReceiver.completeWakefulIntent(intent);
    }

    private void sendNotification(String title, String message) {
        if (!DailyPlaySharedPrefUtils.shouldShowNotifications()) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentText(message)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        Intent result = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(result);
        PendingIntent contentIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
