package com.app.mishtube;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.app.mishtube.fragments.LocalVideoFragment;
import com.app.mishtube.fragments.VideoHistoryFragment;
import com.app.mishtube.helper.FileUtils;
import com.app.mishtube.services.FloatingViewService;

public class MainActivity extends AppCompatActivity {

    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;

    private VideoView videoView;
    private ProgressBar progressBar;
    private ImageView imageView;
    private AppCompatButton openWidget;

    private Uri videoUri;
    private int position;
    private Handler mHandler;

    private static final String TAG_VIDEO_HISTORY = "VideoHistory";
    private static final String TAG_LOCAL_VIDEO = "LocalVideo";
    private static final String TAG_YOUTUBE = "YouTube";
    private String CURRENT_TAG = TAG_VIDEO_HISTORY;

    private final int PERMISSION_REQUEST_CODE = 1;

    private static final int REQUEST_CODE = 6384;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)){

            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);

        } else {
            initializeView();
        }
    }

    private void initializeView(){
        videoView = (VideoView)findViewById(R.id.myVideo);
        imageView = (ImageView) findViewById(R.id.videoImage);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        openWidget = (AppCompatButton) findViewById(R.id.openWidget);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                //handleReceivedText(intent);

            } else if (type.startsWith("video/")) {
                handleReceivedVideo(intent);
            }
        } else if (action.equals("ReturnedUri")) {
            handleVideoReturned(intent);
        }

        openWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (videoUri != null){
                    Intent intent = new Intent(MainActivity.this, FloatingViewService.class);
                    intent.setData(videoUri);
                    intent.putExtra("POSITION", videoView.getCurrentPosition());
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setAction("FLOAT_LOCAL");
                    startService(intent);
                    finish();
                } else {
                    Toast.makeText(MainActivity.this, "Kindly select video", Toast.LENGTH_LONG).show();
                }
            }
        });

        swapButton();
    }

    private void swapButton(){
        if (videoUri != null) {
            openWidget.setVisibility(View.VISIBLE);

        } else {
            openWidget.setVisibility(View.GONE);
        }
    }

    private void handleReceivedText(Intent intent) {
        String url = intent.getStringExtra(Intent.EXTRA_TEXT);
        Log.d("TAGGER", url);

        //loadActivity(url, 0);
    }

    private void handleReceivedVideo(Intent intent) {
        if (intent != null) {
            videoUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

            loadVideo(videoUri, 0);
        }
    }

    private void handleVideoReturned(Intent intent) {
        if (intent != null) {
            videoUri = intent.getData();
            position = intent.getIntExtra("POSITION", 0);

            loadVideo(videoUri, position);
        }
    }

    private void loadVideo(Uri videoUri, int position){

        videoView.setVideoURI(videoUri);
        videoView.setBackgroundColor(Color.BLACK);
        imageView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        videoView.seekTo(position);
        videoView.start();

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                progressBar.setVisibility(View.GONE);
                imageView.setVisibility(View.GONE);
                videoView.setBackgroundColor(Color.TRANSPARENT);
            }
        });

        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        swapButton();
    }

    private void loadActivity(String videoUri, int position){
        Intent intent = new Intent(MainActivity.this, YouTubeActivity.class);
        intent.putExtra("URI", videoUri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void loadFragment(final String title, final String videoUri, final int position){
        /*Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Fragment fragment = getFragment(title, videoUri, position);
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.setCustomAnimations(android.R.anim.fade_in,
                        android.R.anim.fade_out);
                fragmentTransaction.replace(R.id.main_content_area, fragment, CURRENT_TAG);
                fragmentTransaction.commitAllowingStateLoss();
            }
        };

        if (runnable != null) {
            mHandler.post(runnable);
        }*/
    }

    private Fragment getFragment(String title, String videoUri, int position) {
        Bundle bundle = new Bundle();

        switch (title) {
            case TAG_VIDEO_HISTORY:
                VideoHistoryFragment videoHistoryFragment = new VideoHistoryFragment();
                CURRENT_TAG = TAG_VIDEO_HISTORY;
                return videoHistoryFragment;
            case TAG_LOCAL_VIDEO:
                LocalVideoFragment localVideoFragment = new LocalVideoFragment();
                bundle.putString("URI", videoUri);
                bundle.putInt("POSITION", position);
                localVideoFragment.setArguments(bundle);
                CURRENT_TAG = TAG_LOCAL_VIDEO;
                return localVideoFragment;
            /*case TAG_YOUTUBE:
                YouTubeFragment youTubeFragment = new YouTubeFragment();
                bundle.putString("URI", videoUri);
                youTubeFragment.setArguments(bundle);
                CURRENT_TAG = TAG_YOUTUBE;
                return youTubeFragment;*/
            default:
                return new VideoHistoryFragment();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CODE_DRAW_OVER_OTHER_APP_PERMISSION:
                if (resultCode == RESULT_OK) {
                    initializeView();
                } else { //Permission is not available
                    Toast.makeText(this,
                            "Draw over other app permission not available. Closing the application",
                            Toast.LENGTH_SHORT).show();

                    finish();
                }
                break;
            case REQUEST_CODE:
                // If the file selection was successful
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        // Get the URI of the selected file
                        videoUri = data.getData();
                        try {
                            // Get the file path from the URI
                            //final String path = FileUtils.getPath(this, uri);
                            String mimeType = FileUtils.getMimeType(this, videoUri);

                            if (mimeType.contains("video/")){
                                loadVideo(videoUri, 0);

                            } else {
                                Toast.makeText(MainActivity.this,
                                        "Selected media not supported ", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Log.e("FileSelector", "File select error", e);
                        }
                    }
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_select:
                selectVideo();
                break;
        }
        return true;
    }

    private void showChooser() {
        // Use the GET_CONTENT intent from the utility class
        Intent target = FileUtils.createGetContentIntent();
        // Create the chooser Intent
        Intent intent = Intent.createChooser(
                target, getString(R.string.chooser_title));
        try {
            startActivityForResult(intent, REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            // The reason for the existence of aFileChooser
        }
    }

    private void selectVideo(){
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkPermission()) {
                showChooser();
                Log.e("permission", "Permission already granted.");

            } else {
                requestPermission();
            }
        }
    }

    private boolean checkPermission(){
        int result = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(MainActivity.this,
                            "Permission accepted", Toast.LENGTH_LONG).show();
                    showChooser();

                } else {
                    Toast.makeText(MainActivity.this,
                            "Permission denied", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
}
