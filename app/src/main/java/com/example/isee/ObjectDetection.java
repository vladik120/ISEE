package com.example.isee;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;


public class ObjectDetection extends PrePostProcessor{
    private static Bitmap mBitmap;
    private static float mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY;
    private static ImageView mImageView;
    private static String FILENAME = "ObjectDetection.JPEG";
    private static float mImageViewWidth;
    private static float mImageViewHeight;
    private static Module mModule = null;

    public static void startObjectDetectionCommand(int commandToPref,String object){
        initObjectDetection();
        TakePicture.start(commandToPref,object);
    }

    private static void initObjectDetection() {
        mImageView = new ImageView(MainActivity.context);
        mImageViewWidth = 3100;
        mImageViewHeight = 4000;
    }

    public static void command(int commandToPreform, String objectToCount_Find) {
        ArrayList<Result> results = classObjectDetection();
        switch (commandToPreform){
            case R.integer.ObjectDetection:
                detectTheObjects(results);
                break;
            case R.integer.Find:
                FindObjects(results,objectToCount_Find);
                break;
            case R.integer.Count:
                CountObjects(results,objectToCount_Find);
                break;
            default:
                break;
        }
    }


    private static ArrayList<Result> classObjectDetection(){
        InputStream imageFileOS = getPictureInputStream();
        mBitmap = BitmapFactory.decodeStream((InputStream)imageFileOS);

        try{imageFileOS.close();}
        catch(Exception e){e.printStackTrace();}
        MainActivity.context.deleteFile(FILENAME);
        if (mBitmap == null) {
            sendToMainActivity("classObjectDetection : mBitmap == null");
            return null;
        }
        try {
            if (mModule == null) {
                mModule = LiteModuleLoader.load(MainActivity.assetFilePath(MainActivity.context, "yolov5s.torchscript.ptl"));
            }
        } catch (IOException e) {
            Log.e("Object Detection camera", "Error reading assets", e);
            return null;
        }

        mImgScaleX = (float)mBitmap.getWidth() / PrePostProcessor.mInputWidth;
        mImgScaleY = (float)mBitmap.getHeight() / PrePostProcessor.mInputHeight;
        Log.i("classObjectDetection","mImgScaleX - " + mImgScaleX);
        Log.i("classObjectDetection","mImgScaleY - " + mImgScaleY);
        Log.i("classObjectDetection","mImageViewWidth - " + mImageView.getWidth());
        Log.i("classObjectDetection","mImageViewHeight - " + mImageView.getHeight());
        mIvScaleX = (mBitmap.getWidth() > mBitmap.getHeight() ? (float)mImageViewWidth / mBitmap.getWidth() : (float)mImageViewHeight / mBitmap.getHeight());
        mIvScaleY  = (mBitmap.getHeight() > mBitmap.getWidth() ? (float)mImageViewHeight / mBitmap.getHeight() : (float)mImageViewWidth / mBitmap.getWidth());
        Log.i("classObjectDetection","mIvScaleX - " + mIvScaleX);
        Log.i("classObjectDetection","mIvScaleY - " + mIvScaleY);
        mStartX = (mImageViewWidth - mIvScaleX * mBitmap.getWidth())/2;
        mStartY = (mImageViewHeight -  mIvScaleY * mBitmap.getHeight())/2;

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(mBitmap, PrePostProcessor.mInputWidth, PrePostProcessor.mInputHeight, true);
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB);
        IValue[] outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple();
        final Tensor outputTensor = outputTuple[0].toTensor();
        final float[] outputs = outputTensor.getDataAsFloatArray();
        return PrePostProcessor.outputsToNMSPredictions(outputs, mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY);
    }



    private static InputStream getPictureInputStream() {
        try {
            InputStream imageFileOS = MainActivity.context.openFileInput(FILENAME);
            return imageFileOS;
        }
        catch (Exception e){e.printStackTrace();}
        return null;
    }

    private static void sendToMainActivity(String text){
        Intent intent = new Intent("MainActivityReceiver");
        intent.putExtra("main", text);
        MainActivity.context.sendBroadcast(intent);
    }

    private static String provideObjects(ArrayList<Result> results){
        String objects = "\n";
        if(results == null){
            sendToMainActivity("startObjectDetection : results == null");
            if(MainActivity.context.deleteFile(FILENAME))
                sendToMainActivity("startObjectDetection : start - successfully deleted" + FILENAME);
            return null;
        }
        for (Result object : results)
            if (!objects.contains(PrePostProcessor.mClasses[object.classIndex]))
                objects += PrePostProcessor.mClasses[object.classIndex] + " \n";
        return objects;
    }

    private static void detectTheObjects(ArrayList<Result> results) {
        String objects = provideObjects(results);
        if(objects == null) {
            sendToMainActivity("detectTheObjects : objects == null");
            TextToSpeechConvert.speak("there was a problem the function provide objects");
            return;
        }

        if (objects.equals(""))
            objects = "couldn't detect objects around you.";
        else
            objects = "the objects around you are : " + objects;
        sendToMainActivity("ISEE : " + objects);
        TextToSpeechConvert.speak(objects);
    }

    private static void CountObjects(ArrayList<Result> results, String objectToCount_Find) {
        if(objectToCount_Find == null){
            TextToSpeechConvert.speak("can't count the object, please try again");
            return;
        }
        //
        String objects = "\n";
        if(results == null){
            sendToMainActivity("startObjectDetection : results == null");
            if(MainActivity.context.deleteFile(FILENAME))
                sendToMainActivity("startObjectDetection : start - successfully deleted" + FILENAME);

        }
        for (Result object : results)
                objects += PrePostProcessor.mClasses[object.classIndex] + " \n";
        //
        sendToMainActivity("CountObjects :" + objects);
        if(objects.contains(objectToCount_Find))
            objects = "The object " + objectToCount_Find + " appears " + countOccurrences(objects,objectToCount_Find) + " time.";
        else
            objects = "The object " + objectToCount_Find + " not found.";


        TextToSpeechConvert.speak(objects);
    }

    private static void FindObjects(ArrayList<Result> results, String objectToCount_Find) {
        if(objectToCount_Find == null){
            TextToSpeechConvert.speak("can't find the object,please try again");
            return;
        }
        String objects = provideObjects(results);
        sendToMainActivity("FindObjects :" + objects);
        if(objects.contains(objectToCount_Find))
            objects = "The object " + objectToCount_Find + " is in front of you.";
        else
            objects = "The object " + objectToCount_Find + " not found.";
        TextToSpeechConvert.speak(objects);
    }

    private static int countOccurrences(String str, String word) {
        String a[] = str.split("\n");
        int count = 0;
        Log.i("countOccurrences",a.toString());
        for (int i = 0; i < a.length; i++) {
            Log.i("countOccurrences","index " + i + " word : " + a[i]);
            if (a[i].contains(word))
                count++;
        }
        return count;
    }
}
