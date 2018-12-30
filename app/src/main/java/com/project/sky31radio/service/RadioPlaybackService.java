package com.project.sky31radio.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.RemoteViews;

import com.project.sky31radio.App;
import com.project.sky31radio.IRadioService;
import com.project.sky31radio.R;
import com.project.sky31radio.model.Program;
import com.project.sky31radio.ui.HomeActivity;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;


/**
 * Created by linroid on 1/14/15.
 */
public class RadioPlaybackService extends Service implements AudioManager.OnAudioFocusChangeListener {
    private static final int NOTIFICATION_ID = 1;

    public static final String KEY_PROGRAM_LIST = "program_list";
    public static final String KEY_PROGRAM_POSITION = "program_position";
    public static final String KEY_PROGRAM = "program";
    public static final String KEY_IS_PLAYING = "is_playing";


    public static final String EXTRA_POSITION = "position";
    public static final String EXTRA_PERCENT = "percent";

    public static final String ACTION_PROGRAM_CHANGED = "com.linroid.radio.intent.action.PROGRAM_CHANGED";
    public static final String ACTION_PLAYING_STATUS_CHANGED = "com.linroid.radio.intent.action.PLAYING_STATUS_CHANGED";
    public static final String ACTION_PLAY = "com.linroid.radio.intent.action.PLAY";
    public static final String ACTION_PAUSE = "com.linroid.radio.intent.action.PAUSE";
    public static final String ACTION_NEXT = "com.linroid.radio.intent.action.NEXT";
    public static final String ACTION_PREVIOUS = "com.linroid.radio.intent.action.PREVIOUS";
    public static final String ACTION_SEEK_TO_POSITION = "com.linroid.radio.intent.action.SEEK_TO_POSITION";
    public static final String ACTION_SEEK_TO_PERCENT = "com.linroid.radio.intent.action.SEEK_TO_PERCENT";
    public static final String ACTION_STOP = "com.linroid.radio.intent.action.STOP";
    public static final String ACTION_SELECT_PROGRAM_LIST = "com.linroid.radio.intent.action.SELECT_PROGRAM_LIST";


    PlayerReceiver receiver;
    RadioPlayer player;
    NotificationManager notificationManager;

    WifiManager.WifiLock wifiLock;
    AudioManager audioManager;
    @Inject
    Picasso picasso;


    @Override
    public IBinder onBind(Intent intent) {
        Timber.i("onBind: Intent %s", intent.toString());
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        App app = (App) getApplication();
        app.inject(this);

        player = new RadioPlayer();
        receiver = new PlayerReceiver();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SELECT_PROGRAM_LIST);
        filter.addAction(ACTION_PLAY);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_STOP);
        filter.addAction(ACTION_PREVIOUS);
        filter.addAction(ACTION_SEEK_TO_POSITION);
        filter.addAction(ACTION_SEEK_TO_PERCENT);
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(receiver, filter);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "wifi_lock");
        wifiLock.acquire();
        int result = audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // could not get audio focus.
        }
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
    }
    @Override
    public boolean onUnbind(Intent intent) {
        Timber.w("onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent==null){
            Timber.e("onStartCommand: Null intent");
            return super.onStartCommand(intent, flags, startId);
        }
        Timber.i("onStartCommand, intent: %s", intent.toString());
        Bundle data = intent.getExtras();

        List<Program> programList = data.getParcelableArrayList(KEY_PROGRAM_LIST);
        int playingIndex = data.getInt(KEY_PROGRAM_POSITION);
        player.setProgramList(programList, playingIndex);
        player.loadProgram();
        return super.onStartCommand(intent, flags, startId);
    }
    private void sendProgramChangedBroadcast(Program program){
        buildNotification();
        Intent broadCastIntent = new Intent();
        broadCastIntent.setAction(ACTION_PROGRAM_CHANGED);
        broadCastIntent.putExtra(RadioPlaybackService.KEY_PROGRAM, program);
        sendBroadcast(broadCastIntent);
    }
    private void sendPlayingStatusChangedBroadcast(boolean isPlaying){
        if(isPlaying){
            final Intent audioEffectIntent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
            audioEffectIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.mediaPlayer.getAudioSessionId());
            audioEffectIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
            sendBroadcast(audioEffectIntent);
        }

        buildNotification();

        Intent statusChangedIntent = new Intent();
        statusChangedIntent.setAction(ACTION_PLAYING_STATUS_CHANGED);
        statusChangedIntent.putExtra(RadioPlaybackService.KEY_IS_PLAYING, isPlaying);
        sendBroadcast(statusChangedIntent);
    }
    @Override
    public void onDestroy() {
        Timber.d("onDestroy");
        stopForeground(true);
        notificationManager.cancel(NOTIFICATION_ID);
        player.destroy();
        player = null;
        unregisterReceiver(receiver);
        wifiLock.release();
    }

    private void buildNotification(){
//        Notification.MediaStyle style = new Notification.MediaStyle();
//        style.setShowActionsInCompactView(0, 1, 2);

        int playButtonIconResId;
        String playButtonAction;
        final boolean isPlaying = player.isPlaying();
        if(isPlaying){
            playButtonAction = ACTION_PAUSE;
            playButtonIconResId = R.drawable.ic_stat_action_pause;
        }else{
            playButtonAction = ACTION_PLAY;
            playButtonIconResId = R.drawable.ic_stat_action_play_arrow;
        }
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 1, intent, 0);
        Program playingProgram = player.getPlayingProgram();

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_playing)
                .setContentTitle(playingProgram.getTitle())
                .setContentText(playingProgram.getAuthor())
                .setContentIntent(contentIntent)
                .setShowWhen(true);
//                .setContent(collapseViews);
////                .addAction(R.drawable.ic_stat_action_skip_previous, null, createAction(ACTION_PREVIOUS))
////                .addAction(playButtonIconResId, null, createAction(playButtonAction))
////                .addAction(R.drawable.ic_stat_action_skip_next, null, createAction(ACTION_NEXT));

        RemoteViews collapsedViews = new RemoteViews(getPackageName(), R.layout.collapsed_notification);
        collapsedViews.setTextViewText(R.id.program_title, playingProgram.getTitle());
        collapsedViews.setImageViewResource(R.id.btn_play_pause, playButtonIconResId);
        collapsedViews.setOnClickPendingIntent(R.id.btn_play_pause, createAction(playButtonAction));
        collapsedViews.setOnClickPendingIntent(R.id.btn_skip_next, createAction(ACTION_NEXT));

        builder.setContent(collapsedViews);
        final Notification notification = builder.build();
        notification.contentView = collapsedViews;

        if(Build.VERSION.SDK_INT >= 16){
            RemoteViews expandedViews = new RemoteViews(getPackageName(), R.layout.expanded_notification);
            expandedViews.setTextViewText(R.id.program_title, playingProgram.getTitle());
            expandedViews.setTextViewText(R.id.program_author, playingProgram.getAuthor());
            expandedViews.setOnClickPendingIntent(R.id.btn_skip_next, createAction(ACTION_NEXT));
            expandedViews.setOnClickPendingIntent(R.id.btn_skip_previous, createAction(ACTION_PREVIOUS));
            expandedViews.setOnClickPendingIntent(R.id.btn_play_pause, createAction(playButtonAction));
            expandedViews.setOnClickPendingIntent(R.id.stop, createAction(ACTION_STOP));
            expandedViews.setImageViewResource(R.id.btn_play_pause, playButtonIconResId);
            notification.bigContentView = expandedViews;
        }
            picasso.load(playingProgram.getThumbnail())
                    .into(new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
//                    builder.setLargeIcon(bitmap);
//                    Notification notification = builder.build();
                    notification.contentView.setImageViewBitmap(R.id.thumbnail, bitmap);
                    if(Build.VERSION.SDK_INT >= 16){
                        notification.bigContentView.setImageViewBitmap(R.id.thumbnail, bitmap);
                    }
                    if(isPlaying) {
                        notification.flags |= Notification.FLAG_NO_CLEAR;
                        notificationManager.notify(NOTIFICATION_ID, notification);

                        startForeground(NOTIFICATION_ID, notification);
                    }else {
                        stopForeground(false);
//                        builder.setAutoCancel(true);
                        notification.flags = 0;
                        notification.deleteIntent = createAction(ACTION_STOP);
                        notificationManager.notify(NOTIFICATION_ID, notification);
                    }
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }
            });


    }
    private PendingIntent createAction(String action){
        Intent intent = new Intent();
        intent.setAction(action);
        return PendingIntent.getBroadcast(this, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                Timber.i("AudioManager.AUDIOFOCUS_GAIN");
//                player.play();
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                Timber.i("AudioManager.AUDIOFOCUS_LOSS");
                if(player!=null){
                    player.stop();
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                Timber.i("AudioManager.AUDIOFOCUS_LOSS_TRANSIENT");
                if(player!=null){
                    player.pause();
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                Timber.i("AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                break;
        }
    }

    private class RadioPlayer implements MediaPlayer.OnBufferingUpdateListener,
            MediaPlayer.OnPreparedListener,
            MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener{
        private boolean isPlaying = false;
        private List<Program> programList = new ArrayList<>();
        private int playingIndex;
        MediaPlayer mediaPlayer;
        public RadioPlayer() {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setLooping(true);
            mediaPlayer.setOnBufferingUpdateListener(this);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setWakeMode(RadioPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);
        }

        public boolean isPlaying(){
            return mediaPlayer!=null && (isPlaying||mediaPlayer.isPlaying());
        }
        public void play() {
            Timber.d("player play");
            isPlaying = true;
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
            sendPlayingStatusChangedBroadcast(true);

        }
        public void pause() {
            isPlaying = false;
            if (mediaPlayer != null) {
                mediaPlayer.pause();
            }
            sendPlayingStatusChangedBroadcast(false);
            Timber.d("player paused");
        }

        public void stop() {
            Timber.d("stop");
            isPlaying = false;
            if (mediaPlayer != null) {
                mediaPlayer.reset();
                mediaPlayer.release();
            }
            mediaPlayer = null;
            stopSelf();
        }

        private void next() {
            if(playingIndex==programList.size()-1){
                return;
            }
            playingIndex = (playingIndex + 1) % programList.size();
            loadProgram();
        }

        private void previous() {
            playingIndex = (playingIndex - 1) % programList.size();
            playingIndex = playingIndex<=0 ? programList.size()-1 : playingIndex;
            loadProgram();
        }

        public void loadProgram() {
            isPlaying = true;
            Program program = programList.get(playingIndex);
            if(program.getAudio()==null){
                return;
            }
            String url = program.getAudio().getSrc();
            Timber.i("loadProgram: %s(%s)", program.getTitle(), url);
            mediaPlayer.reset();
            try {
                mediaPlayer.setDataSource(url);
                mediaPlayer.prepareAsync();
            } catch (IOException | IllegalStateException e) {
                Timber.e(e, "load program failed");
            }
            sendProgramChangedBroadcast(program);
        }
        public void setProgramList(List<Program> programList, int playingIndex) {
            this.programList = programList;
            this.playingIndex = playingIndex;
        }

        public int getDuration() {
            return mediaPlayer.getDuration();
        }

        public void seekToPercent(float percent) {
            int position = (int) (mediaPlayer.getDuration() * percent / 100);
            seekToPosition(position);
        }

        public void seekToPosition(int position) {
            mediaPlayer.seekTo(position);
            sendPlayingStatusChangedBroadcast(isPlaying());
        }

        public void destroy() {
            isPlaying = false;
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }



        public long getPosition() {
            return mediaPlayer.getCurrentPosition();
        }

        public Program getPlayingProgram() {
            return programList.get(playingIndex);
        }

        public int getSessionId() {
            return mediaPlayer.getAudioSessionId();
        }
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if(percent == 100){
                Timber.d("buffer complete:%d%%", percent);
            }
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            Timber.v("onPrepared");
            this.play();
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Timber.e("onError");
            return true;
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            Timber.i("onCompletion, %d/%d", mp.getCurrentPosition(), mp.getDuration());
            player.next();
        }

    }
    IBinder mBinder = new IRadioService.Stub() {
        @Override
        public void play() throws RemoteException {
            player.play();
        }
        @Override
        public void pause() throws RemoteException {
            player.pause();
        }

        @Override
        public void stop() throws RemoteException {
            player.stop();
        }

        @Override
        public void seekToPosition(int position) throws RemoteException {
            player.seekToPosition(position);
        }

        @Override
        public void seekToPercent(int percent) throws RemoteException {
            player.seekToPercent(percent);
        }

        @Override
        public void next() throws RemoteException {
            player.next();
        }

        @Override
        public void previous() throws RemoteException {
            player.previous();
        }

        @Override
        public long getDuration() throws RemoteException {
            return player.getDuration();
        }

        @Override
        public long getPosition() throws RemoteException {
            return player.getPosition();
        }

        @Override
        public int getPlayerSessionId() throws RemoteException {
            return player.getSessionId();
        }

        @Override
        public boolean isPlaying() throws RemoteException {
            return player!=null && player.isPlaying();
        }

        @Override
        public Program getPlayingProgram() throws RemoteException {
            return player.getPlayingProgram();
        }
    };
    private class PlayerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Timber.i("onReceive, Intent: %s, Extras:%s", intent.toString(), intent.getExtras()==null ? null : intent.getExtras().toString());
            switch (intent.getAction()) {
                case ACTION_PLAY:
                    player.play();
                    break;
                case ACTION_NEXT:
                    player.next();
                    break;
                case ACTION_PREVIOUS:
                    player.previous();
                    break;
                case ACTION_SEEK_TO_POSITION:
                    int position = intent.getIntExtra(EXTRA_POSITION, 0);
                    Timber.d("ACTION_SEEK_TO_POSITION, position:%d", position);
                    player.seekToPosition(position);
                    break;
                case ACTION_SEEK_TO_PERCENT:
                    int percent = intent.getIntExtra(EXTRA_PERCENT, 0);
                    player.seekToPosition(percent);
                    break;
                case ACTION_STOP:
                    player.stop();
                    break;
                case ACTION_PAUSE:
                    player.pause();
                    break;
                case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                    player.pause();
                    break;

            }
        }
    }

    PhoneStateListener mPhoneListener = new PhoneStateListener(){
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                case TelephonyManager.CALL_STATE_RINGING:
                    if (player.isPlaying()) {
                        player.pause();
                    }
                    break;
                default:
                    break;
            }
        }
    };

}