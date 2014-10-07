package it.moondroid.chatbot;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import it.moondroid.chatbot.alice.Alice;

/**
 * Created by Marco on 06/10/2014.
 */
public class BrainService extends Service {

    public static final String COMMAND_QUESTION = "it.moondroid.chatbot.BrainService.COMMAND_QUESTION";
    public static final String COMMAND_ACTION = "it.moondroid.chatbot.BrainService.COMMAND_ACTION";
    public static final int ACTION_START = 1;
    public static final int ACTION_STOP = -1;
    private static final int NOTIFICATION_ID = 1337;
    private static boolean isBrainLoaded = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("BrainService","onCreate()");
        //TODO
        //(new LoadBrainThread()).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent==null){
            return Service.START_STICKY;
        }

        String question = intent.getStringExtra(COMMAND_QUESTION);
        if(question!=null){
            Log.d("BrainService","onStartCommand() question:"+question);
            String answer = "";
            if (isBrainLoaded) {
                answer = Alice.getInstance().processInput(question);
            } else {
                answer = "My brain has not been loaded yet.";
            }

            Intent localIntent =
                    new Intent(Constants.BROADCAST_ACTION_BRAIN_ANSWER)
                            // Puts the answer into the Intent
                            .putExtra(Constants.EXTRA_BRAIN_ANSWER, answer);
            // Broadcasts the Intent to receivers in this app.
            LocalBroadcastManager.getInstance(BrainService.this).sendBroadcast(localIntent);
        }

        int action = intent.getIntExtra(COMMAND_ACTION, 0);
        Log.d("BrainService","onStartCommand() action: "+action);
        if(action==ACTION_STOP){
            stopSelf();
        }
        if(action==ACTION_START){
            (new LoadBrainThread()).start();
        }

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("BrainService", "onDestroy()");
        isBrainLoaded = false;
        stopForeground(true);
    }

    private final class LoadBrainThread extends Thread {
        @Override
        public void run() {

            Alice.setup(BrainService.this);

            Intent localIntent =
                    new Intent(Constants.BROADCAST_ACTION_BRAIN_LOADING)
                            // Puts the status into the Intent
                            .putExtra(Constants.EXTENDED_BRAIN_STATUS, Constants.STATUS_BRAIN_LOADED);
            // Broadcasts the Intent to receivers in this app.
            LocalBroadcastManager.getInstance(BrainService.this).sendBroadcast(localIntent);

            isBrainLoaded = true;

            //Show notification
            Intent openIntent = new Intent(BrainService.this, MainActivity.class);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent openPIntent = PendingIntent.getActivity(BrainService.this, 0, openIntent, 0);

            Intent stopIntent = new Intent(BrainService.this, BrainService.class);
            stopIntent.putExtra(COMMAND_ACTION, ACTION_STOP);
            PendingIntent stopPIntent = PendingIntent.getService(BrainService.this, 0, stopIntent, 0);

            Intent restartIntent = new Intent(BrainService.this, BrainService.class);
            restartIntent.putExtra(COMMAND_ACTION, ACTION_START);
            PendingIntent restartPIntent = PendingIntent.getService(BrainService.this, 0, restartIntent, 0);

            Notification note =
                    new NotificationCompat.Builder(BrainService.this)
                            .setSmallIcon(R.drawable.ic_stat_notify_chat)
                            .setTicker("Brain Loaded")
                            .setContentTitle("Alice Brain")
                            .setContentText("You can talk to me")
                            .setAutoCancel(false)
                            .setContentIntent(openPIntent)
                            .setWhen(System.currentTimeMillis())
                            .addAction(R.drawable.ic_stat_reload, "Restart", restartPIntent)
                            .addAction(R.drawable.ic_stat_stop, "Stop", stopPIntent)
                            .build();

            startForeground(NOTIFICATION_ID, note);
        }
    }
}
