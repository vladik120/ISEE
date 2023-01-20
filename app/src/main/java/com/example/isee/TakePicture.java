package com.example.isee;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

public class TakePicture {
    private static Camera.PictureCallback photoCallback;
    private static Camera camera;
    private static String FILENAME = "ObjectDetection.JPEG";
    private static SurfaceTexture surfaceTexture;
    public static int commandToPreform = -1;
    public static String objectToCount_Find = null;

    public static void start(int commandToPref, String object) {
        initCamera();
        commandToPreform = commandToPref;
        objectToCount_Find = object;
        try {
            camera.setPreviewTexture(surfaceTexture);
            camera.startPreview();
            camera.takePicture(null, null, photoCallback);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void initCamera(){
        //initiate camera
        surfaceTexture = new SurfaceTexture(10);
        camera = Camera.open(findFrontFacingCamera());
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPictureFormat(PixelFormat.JPEG);
        camera.setParameters(parameters);
        camera.enableShutterSound(false);
        //what to do with picture
        photoCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                Log.e("Callback TAG", "Here in jpeg Callback");
                try {
                    Bitmap storedBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, null);

                    Matrix mat = new Matrix();
                    mat.postRotate(90);  // angle is the desired angle you wish to rotate
                    storedBitmap = Bitmap.createBitmap(storedBitmap, 0, 0, storedBitmap.getWidth(), storedBitmap.getHeight(), mat, true);

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    storedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    storedBitmap.recycle();

                    OutputStream imageFileOS = MainActivity.context.openFileOutput(FILENAME,MainActivity.context.MODE_APPEND);
                    imageFileOS.write(byteArray);
                    imageFileOS.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ObjectDetection.command(commandToPreform,objectToCount_Find);
            }
        };
    }

    private static int findFrontFacingCamera() {
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
                return i;

        }
        return 0;
    }

    private static void sendToMainActivity(String text){
        Intent intent = new Intent("MainActivityReceiver");
        intent.putExtra("main", text);
        MainActivity.context.sendBroadcast(intent);
    }

}
