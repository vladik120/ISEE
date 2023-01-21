package com.example.isee;

import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.util.Locale;


public class CommandsAnalyzer {
    private static boolean heyISEE = false;
    private static Long start;
    private static boolean identifyObstacles = false;

    public static void start(String text){
        if(identifyObstacles){
            ObjectDetectionForCamera.surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }

        if (text.contains("hey I see")){

            heyISEE = true;
            start = System.currentTimeMillis();
            Log.i("CommandsAnalyzer","start : got command isee");
            toMainAndSpeak("how may i help you?");
            return;
        }

        if (heyISEE){
            heyISEE = false;
            if ((System.currentTimeMillis() - start)/1000 <= 10)
                Commands(text);
            else {
                sendToMainActivity("ISEE : command over 10 seconds.");
                if(identifyObstacles){
                    ObjectDetectionForCamera.surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                }
            }
        }


    }

    private static void Commands(String text){
        if(!identifyObstacles){
            if(text.contains("start obstacles detection") || text.contains("start obstacle detection")) {
                toMainAndSpeak("starting obstacle detection");
                identifyObstacles = true;
                return;
            }
            if(text.contains("object detection")) {
                toMainAndSpeak("starting Object detection");
                ObjectDetection.startObjectDetectionCommand(R.integer.ObjectDetection,null);
                return;
            }
            if(text.contains("find")) {
                String find = setTextForCommand(text);
                toMainAndSpeak("looking for "+find);
                ObjectDetection.startObjectDetectionCommand(R.integer.Find,find);
                return;
            }
            if(text.contains("count")) {
                String count = setTextForCommand(text);
                toMainAndSpeak("counting "+count);
                ObjectDetection.startObjectDetectionCommand(R.integer.Count,count);
            }
            else {
                toMainAndSpeak("this command does not exist");
            }
        }else {
            if(text.contains("stop obstacles detection") || text.contains("stop obstacle detection") || text.contains("stop")) {
                toObjectDetectionForCamera("stopping obstacle detection");
                identifyObstacles = false;
            }else {
                TextToSpeechConvert.speak("obstacles detection is on");
                if(identifyObstacles){
                    ObjectDetectionForCamera.surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                }
            }
        }

        if(text.contains("stop I see") || text.contains("stop IC") || text.contains("quit") || text.contains("exit")) {
            toMainAndSpeak("stopping app");
            sleepFor(1000);
            sendToMainActivity("stopIsee");
            return;
        }
    }

    private static void sendToMainActivity(String text){
        Intent intent = new Intent("MainActivityReceiver");
        intent.putExtra("main", text);
        MainActivity.context.sendBroadcast(intent);
    }

    private static String setTextForCommand(String text){
        String command = text.replace("count","")
                .replace("find","")
                .replace(" ","")
                .replace("USER:","")
                .replace("\n","")
                .toLowerCase(Locale.ROOT);
        return command;
    }

    private static void toMainAndSpeak(String text){

        TextToSpeechConvert.speak(text);
        sendToMainActivity("ISEE : "+text);
    }

    private static void toObjectDetectionForCamera(String text){
        TextToSpeechConvert.speak(text);
        Intent intent = new Intent("DetectionForCameraReceiver");
        intent.putExtra("main", text);
        ObjectDetectionForCamera.context.sendBroadcast(intent);
    }

    private static void sleepFor(long millisecond) {
        try{
            Thread.sleep(millisecond);
        }
        catch(Exception e){}
    }


}
