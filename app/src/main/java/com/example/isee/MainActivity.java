package com.example.isee;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final String FILENAME = "ObjectDetection.JPEG";
    private LinearLayout Layout_scrollView = null;
    public static Module mModule = null;
    public static Context context = null;
    public boolean firstActivation = true;
    private final String[] PERMISSIONS = {
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
            android.Manifest.permission.INTERNET,
    };


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(firstActivation) {
            initVariables();

            askPermissions();
            runMainActivityReceiver();
            startISEE();
            firstActivation = false;
        }
    }

    private void setLayout_scrollView() {
        Bundle extras = getIntent().getExtras();
        if (extras != null)
            setScrollViewText(extras.getString("mainActivityData"));

    }

    private void askPermissions(){
        if (!hasPermissions(this, PERMISSIONS)) {
            int PERMISSION_ALL = 1;
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initVariables(){
        //Context init
        this.context = this.getApplicationContext();
        //Speech recognizer init
        new SpeechRecognition();
        //layout init
        Layout_scrollView = findViewById(R.id.Layout_scrollView);
        //text to speech init
        TextToSpeechConvert.initTTS();
        //YOLOv5 init
        try {
            mModule = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "yolov5s.torchscript.ptl"));
            BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("classes.txt")));
            String line;
            List<String> classes = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                classes.add(line);
            }
            PrePostProcessor.mClasses = new String[classes.size()];
            classes.toArray(PrePostProcessor.mClasses);
        } catch (IOException e) {
            Log.e("Object Detection", "Error reading assets", e);
            finish();
        }
        //scroll view set text
        setLayout_scrollView();
        //
        setScrollViewText("ISEE : isee is on.");

    }



    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("MainActivity", "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("MainActivity", "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        this.context.deleteFile(FILENAME);
        super.onDestroy();
    }

    private void runToForegroundService(String command){
        Intent intent = new Intent(this ,ForegroundForSpeechService.class);
        intent.putExtra("command",command);
        startForegroundService(intent);
    }

    public void runMainActivityReceiver(){
        BroadcastReceiver ForegroundBR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                String app_log = arg1.getExtras().getString("main");
                switch(app_log){
                    case "stopIsee":
                        stopISEE();
                        finishAffinity();
                        System.exit(0);
                        break;
                    case "ISEE : starting obstacle detection":
                        final Intent intent = new Intent(MainActivity.this, ObjectDetectionForCamera.class);
                        intent.putExtra("mainActivityData",getTextFromLayOut(Layout_scrollView));
                        startActivity(intent);
                        break;
                    default:
                        setScrollViewText(app_log);
                        break;
                }
            }
        };
        registerReceiver(ForegroundBR, new IntentFilter("MainActivityReceiver"));
    }

    private void setScrollViewText(String text){
        TextView TV = new TextView(this);
        TV.setText(text);
        Layout_scrollView.addView(TV,0);
    }

    private void stopISEE(){
        runToForegroundService("OFF");
        sendToSpeechRecognition("OFF");
    }

    private void startISEE(){
        runToForegroundService("ON");
        sendToSpeechRecognition("ON");
    }


    private void sendToSpeechRecognition(String text){
        Intent intent = new Intent("SpeechRecognitionReceiver");
        intent.putExtra("SR", text);
        getApplicationContext().sendBroadcast(intent);
    }

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0)
            return file.getAbsolutePath();
        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1)
                    os.write(buffer, 0, read);
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    private String getTextFromLayOut(LinearLayout LL){
        String text = "";
        View view = null;
        for (int i = 0; i < LL.getChildCount(); i++){
            view = LL.getChildAt(i);
            if(view instanceof TextView)
                text += ((TextView)view).getText().toString() + "\n";
        }
        return text;
    }
}
