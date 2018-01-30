package com.app.mishtube.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.app.mishtube.MainActivity;
import com.app.mishtube.R;

/**
 * Created by Mishael on 10/4/2017.
 */

public class FloatingViewService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private VideoView videoView;
    private ProgressBar progressBar;
    private ImageView imageView;
    private Uri videoUrl;
    private int position;

    ScaleGestureDetector scaleGestureDetector;
    private float scale = 1f;

    public FloatingViewService(){

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(floatingView, params);

        final View collapsedView = floatingView.findViewById(R.id.collapse_view);

        final View expandedView = floatingView.findViewById(R.id.expanded_container);

        //Set the close button
        ImageView closeButtonCollapsed = (ImageView) floatingView.findViewById(R.id.close_btn);
        closeButtonCollapsed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //close the service and remove the from from the window
                position = videoView.getCurrentPosition();
                stopSelf();
            }
        });

        //Set the close button
        ImageView closeButton = (ImageView) floatingView.findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                collapsedView.setVisibility(View.VISIBLE);
                expandedView.setVisibility(View.GONE);
                videoView.setVisibility(View.GONE);
            }
        });

        ImageView openButton = (ImageView) floatingView.findViewById(R.id.open_button);
        openButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Open the application  click.
                returnHome(videoUrl);

                //close the service and remove view from the view hierarchy
                stopSelf();
            }
        });

        //Drag and move floating view using user's touch action.
        floatingView.findViewById(R.id.root_container).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //scaleGestureDetector.onTouchEvent(event);

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:

                        //remember the initial position.
                        initialX = params.x;
                        initialY = params.y;

                        //get the touch location
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        int Xdiff = (int) (event.getRawX() - initialTouchX);
                        int Ydiff = (int) (event.getRawY() - initialTouchY);


                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                        //So that is click event.
                        if (Xdiff < 10 && Ydiff < 10) {
                            if (isViewCollapsed()) {
                                //When user clicks on the image view of the collapsed layout,
                                //visibility of the collapsed layout will be changed to "View.GONE"
                                //and expanded view will become visible.
                                collapsedView.setVisibility(View.GONE);
                                expandedView.setVisibility(View.VISIBLE);
                                videoView.setVisibility(View.VISIBLE);
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        //Calculate the X and Y coordinates of the view.
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);

                        //Update the layout with new X & Y coordinate
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }

                return false;
            }
        });

        videoView = (VideoView) floatingView.findViewById(R.id.videoWidget);
        progressBar = (ProgressBar) floatingView.findViewById(R.id.progress_bar);
        imageView = (ImageView) floatingView.findViewById(R.id.collapsed_iv);

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());


        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isViewCollapsed()) {
                    //When user clicks on the image view of the collapsed layout,
                    //visibility of the collapsed layout will be changed to "View.GONE"
                    //and expanded view will become visible.
                    playVideo(videoUrl, position);
                    collapsedView.setVisibility(View.GONE);
                    expandedView.setVisibility(View.VISIBLE);
                    videoView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        videoUrl = intent.getData();
        String action = intent.getAction();

        if (action.equals("FLOAT_LOCAL")) {
            position = intent.getIntExtra("POSITION", 0);

            playVideo(videoUrl, position);

        } else if (action.equals("FLOAT_YOUTUBE")){

        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void playVideo(final Uri videoUrl, int position){

        videoView.setVideoURI(videoUrl);
        videoView.seekTo(position);
        videoView.setVisibility(View.VISIBLE);
        videoView.setBackgroundColor(Color.BLACK);
        progressBar.setVisibility(View.VISIBLE);
        videoView.start();

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                videoView.setBackgroundColor(Color.TRANSPARENT);
                progressBar.setVisibility(View.GONE);
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopSelf();
                returnHome(videoUrl);
            }
        });
    }

    private void returnHome(Uri videoUrl)
    {
        Intent intent = new Intent(FloatingViewService.this, MainActivity.class);
        intent.setData(videoUrl);
        intent.setAction("ReturnedUri");
        intent.putExtra("POSITION", videoView.getCurrentPosition());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private boolean isViewCollapsed() {
        return floatingView == null || floatingView.findViewById(R.id.collapse_view).getVisibility() == View.VISIBLE;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) windowManager.removeView(floatingView);
    }

    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        float onScaleBegin = 0;
        float onScaleEnd = 0;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scale *= detector.getScaleFactor();

            floatingView.setScaleX(scale);
            floatingView.setScaleY(scale);

            videoView.setScaleX(scale);
            videoView.setScaleY(scale);

            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            onScaleBegin = scale;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            onScaleEnd = scale;
        }
    }
}
