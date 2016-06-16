package com.example.prj2016s.uncompilable.RtspPlayer;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.MediaController;

//import com.namh.mmsplayer.R;


/**
 * Created by namh on 2015-01-08.
 */
public class RtspPlayerService extends Service implements MediaController.MediaPlayerControl {

    private static final String TAG = "RtspPlayerService";
    private RtspPlayer mmsPlayer;

    // mCurrentState is a VideoView object's current state.
    // mTargetState is the state that a method caller intends to reach.
    // For instance, regardless the VideoView object's current state,
    // calling pause() intends to bring the object to a target state
    // of State.PAUSED.
    private int mCurrentState = RtspPlayer.State.IDLE;
    private int mTargetState = RtspPlayer.State.IDLE;


    private int mSeekWhenPrepared;  // recording the seek position while preparing

    private int mCurrentBufferPercentage;
    private boolean mCanPause;
    private boolean mCanSeekBack;
    private boolean mCanSeekForward;

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();


    //////////////////////////////
    ////    Notification
    //////////////////////////////
    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    ///private int NOTIFICATION_ID = R.string.mp3player_service_started;
    ///private boolean mIsNotificationShowing = false;
    ///private Notification mNotification;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public RtspPlayerService getService() {
            return RtspPlayerService.this;
        }
    }


    public String testFunc() {

        return "Test";
    }


    private boolean isInPlaybackState() {
        return (mmsPlayer != null &&
                mCurrentState != RtspPlayer.State.ERROR &&
                mCurrentState != RtspPlayer.State.IDLE &&
                mCurrentState != RtspPlayer.State.PREPARING);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() : initMediaPlayer()");
        initMediaPlayer();
    }

    private void initMediaPlayer() {
        try {
            mmsPlayer = new RtspPlayer(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    // ... react appropriately ...
                    // The MediaPlayer has moved to the Error state, must be reset!

                    // @see http://developer.android.com/reference/android/media/MediaPlayer.OnErrorListener.html
                    Log.e(TAG, "media player error");
                    if (mmsPlayer != null)
                        mmsPlayer.reset();
                    Log.e(TAG, "what:" + what + "; extra:" + extra);
                    return false;
                }
            }, new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mCurrentState = RtspPlayer.State.PREPARED;

                }
            }, new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {

                }
            });
        } catch (Exception e) {
            // @see http://developer.android.com/guide/topics/media/mediaplayer.html#mediaplayer
            Log.d(TAG, "initMediaPlayer() : " + String.valueOf(e));
            e.printStackTrace();
            if (mmsPlayer != null)
                mmsPlayer.release();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mmsPlayer != null) {
            mmsPlayer.release();
            mmsPlayer = null;
            stopForeground(true);
        }

    }


    ////////////////////////////////////////////////////////////
    ////
    ////    implements MediaController.MediaPlayerControl
    ////
    ////////////////////////////////////////////////////////////


    @Override
    public void start() {
        Log.e(TAG, "start() begin");
        if (isInPlaybackState()) {

            Log.e(TAG, "start() : isInPlaybackState");
            mmsPlayer.start();
            mCurrentState = RtspPlayer.State.PLAYING;

            /*if (!mIsNotificationShowing) {
                Notification noti = buildMp3PlayerNotification();
                startForeground(NOTIFICATION_ID, noti);
                mIsNotificationShowing = true;
            }*/

            // TODO : use later if needed
            //updateProgressBar();
            //mInteractives.setLocalPlayBackState(getPlayBackState());

        }
        mTargetState = RtspPlayer.State.PLAYING;
    }

    @Override
    public void pause() {
        if (isInPlaybackState()) {
            if (mmsPlayer.isPlaying()) {
                mmsPlayer.pause();
                mCurrentState = RtspPlayer.State.PAUSED;

                // TODO : use later if needed
                //stopUpdateProgressBar();
                //mInteractives.setLocalPlayBackState(getPlayBackState());
            }
        }
        mTargetState = RtspPlayer.State.PAUSED;

    }

    @Override
    public int getDuration() {
        if (isInPlaybackState()) {
            return mmsPlayer.getDuration();
        }
        return -1;
    }

    @Override
    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return mmsPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public void seekTo(int msec) {
        if (isInPlaybackState()) {
            mmsPlayer.seekTo(msec);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = msec;
        }

    }

    @Override
    public boolean isPlaying() {
        return isInPlaybackState() && mmsPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        if (mmsPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;    //mCanPause
    }

    @Override
    public boolean canSeekBackward() {
        return true;    //mCanSeekBack
    }

    @Override
    public boolean canSeekForward() {
        return true;    //mCanSeekForward
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    /**
     * Show a notification while this service is running.
     */
    /*
    private Notification buildMp3PlayerNotification() {

        return getNotification();
    }*/

    /**
     *
    private Notification getNotification() {
        if (mNotification != null)
            return mNotification;


//        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle()
//                .addLine(
//                        "M.Twain (Google+) Haiku is more than a cert...")
//                .addLine("M.Twain Reminder")
//                .addLine("M.Twain Lunch?")
//                .addLine("M.Twain Revised Specs")
//                .addLine("M.Twain ")
//                .addLine(
//                        "Google Play Celebrate 25 billion apps with Goo..")
//                .addLine(
//                        "Stack Exchange StackOverflow weekly Newsl...")
//                .setBigContentTitle("6 new message")
//                .setSummaryText("mtwain@android.com");

        mNotification = (new RtspPlayerNotification(this)).getNotification();

        return mNotification;
    }
    */

}
