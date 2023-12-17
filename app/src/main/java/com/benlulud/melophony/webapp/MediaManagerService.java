package com.benlulud.melophony.webapp;

import android.app.Service;
import android.os.IBinder;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Binder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.app.NotificationManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.core.app.NotificationCompat;
import android.media.AudioManager;
import android.content.ComponentName;
import androidx.core.app.NotificationManagerCompat;

public class MediaManagerService extends Service {
    private static final String TAG = MediaManagerService.class.getSimpleName();

    private static final String ACTION_PREVIOUS_TRACK = "setPreviousTrack";
    private static final String ACTION_PLAY_PAUSE = "playPause";
    private static final String ACTION_NEXT_TRACK = "setNextTrack";

    private WebView webView;
    private MediaSessionCompat mediaSession;

    private final IBinder binder = new MediaManagerBinder();

    public class MediaManagerBinder extends Binder {
        MediaManagerService getService() {
            return MediaManagerService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");

        createNotificationChannel();
        addMediaReceivers();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {

    }

    public void setWebView(final WebView webView) {
        this.webView = webView;
    }

    private void addMediaReceivers() {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case ACTION_PREVIOUS_TRACK:
                        previousTrack();
                        break;
                    case ACTION_PLAY_PAUSE:
                        playPause();
                        break;
                    case ACTION_NEXT_TRACK:
                        nextTrack();
                        break;
                }
            }
        };
        final IntentFilter notificationActionsFilter = new IntentFilter();
        notificationActionsFilter.addAction(ACTION_PREVIOUS_TRACK);
        notificationActionsFilter.addAction(ACTION_PLAY_PAUSE);
        notificationActionsFilter.addAction(ACTION_NEXT_TRACK);
        registerReceiver(broadcastReceiver, notificationActionsFilter);

        BroadcastReceiver stopNoiseReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                    Log.i(TAG, "#### Noisy");
                    pause();
                }
            }
        };
        registerReceiver(stopNoiseReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }

    public void addNotification(final boolean isPlaying, final String title, final String artistName) {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        mediaSession = new MediaSessionCompat(this, TAG);

        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            public void onPause() {
                Log.i(TAG, "Remote pause command: ");
                pause();
            }

            public void onPlay() {
                Log.i(TAG, "Remote play command: ");
                play();
            }

            public void onSkipToPrevious() {
                Log.i(TAG, "Remote previous track command: ");
                previousTrack();
            }

            public void onSkipToNext() {
                Log.i(TAG, "Remote next track command: ");
                nextTrack();
            }
        });

        Bitmap image = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artistName)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 100)
            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, 0)
            .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, 10)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, image)
            .build());

        mediaSession.setPlaybackState(
            new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                .build()
        );

        mediaSession.setActive(true);

        final int playPauseLogo = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        final String playPauseAction = isPlaying ? "Pause" : "Play";

        final Notification notification = new NotificationCompat.Builder(this, TAG)
                .setSmallIcon(R.drawable.ic_music_note)
                .setLargeIcon(image)
                .setContentTitle(title)
                .setContentText(artistName)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .addAction(R.drawable.ic_skip_previous, "Previous", getIntent(ACTION_PREVIOUS_TRACK))
                .addAction(playPauseLogo, playPauseAction, getIntent(ACTION_PLAY_PAUSE))
                .addAction(R.drawable.ic_skip_next, "Next", getIntent(ACTION_NEXT_TRACK))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession.getSessionToken()))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        notificationManagerCompat.notify(1, notification);
        startForeground(1, notification);
    }

    private PendingIntent getIntent(final String action) {
        return PendingIntent.getBroadcast(this, 0, new Intent(action), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final CharSequence name = getString(R.string.channel_name);
            final String description = getString(R.string.channel_description);
            final int importance = NotificationManager.IMPORTANCE_DEFAULT;

            final NotificationChannel channel = new NotificationChannel(TAG, name, importance);
            channel.setDescription(description);

            final NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Javascript callbacks

    private void runInWebView(final String action) {
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl("javascript:Melophony.handleAndroidCommand('" + action + "')");
            }
        });
    }

    private void previousTrack() {
        runInWebView("PREVIOUS");
    }

    private void playPause() {
        runInWebView("PLAY_PAUSE");
    }

    private void play() {
        runInWebView("PLAY");
    }

    private void pause() {
        runInWebView("PAUSE");
    }

    private void nextTrack() {
        runInWebView("NEXT");
    }
}