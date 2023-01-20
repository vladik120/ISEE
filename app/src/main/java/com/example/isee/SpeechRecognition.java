package com.example.isee;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

public class SpeechRecognition implements RecognitionListener {
    private SpeechRecognizer speech = null;
    private Intent recognizerIntent = null;
    private String LOG_TAG = "VoiceRecognitionActivity";

    public SpeechRecognition() {
        resetSpeechRecognizer();
        setRecogniserIntent();
        runSpeechRecognitionReceiver();
    }

    public void start(){
        if(TextToSpeechConvert.TTS != null)
            while(TextToSpeechConvert.TTS.isSpeaking()){sleepFor(50);}
        setRecogniserIntent();
        resetSpeechRecognizer();
        muteSystemAudio(true);
        speech.startListening(recognizerIntent);

    }

    public void stop(){
        speech.destroy();
        muteSystemAudio(false);
    }

    private void resetSpeechRecognizer() {
        if(speech != null)
            speech.destroy();
        speech = SpeechRecognizer.createSpeechRecognizer(MainActivity.context);
        if(SpeechRecognizer.isRecognitionAvailable(MainActivity.context))
            speech.setRecognitionListener(this);
        else
            ((Activity)MainActivity.context).finish();

    }

    private void setRecogniserIntent() {
        if (recognizerIntent == null){
            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en_US");
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        }
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.i(LOG_TAG, "onBeginningOfSpeech");

    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived: " + buffer);
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");

    }

    @Override
    public void onResults(Bundle results) {
        stop();

        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String text = "";
        for (String result : matches)
            text += "USER : " + result + "\n";

        CommandsAnalyzer.start(text);
        start();
    }

    @Override
    public void onError(int errorCode) {
        String errorMessage = getErrorText(errorCode);
        Log.i(LOG_TAG, "FAILED " + errorMessage);
        // rest voice recogniser
        resetSpeechRecognizer();
        speech.startListening(recognizerIntent);
    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
        //Log.i(LOG_TAG, "onEvent");
    }

    @Override
    public void onPartialResults(Bundle arg0) {
        //Log.i(LOG_TAG, "onPartialResults");
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
        //Log.i(LOG_TAG, "onReadyForSpeech");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        //Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
        Intent intent = new Intent("SpeechRecognition");
        intent.putExtra("speechToText", "onRmsChanged");
        intent.putExtra("speechToTextInteger", rmsdB);
        MainActivity.context.sendBroadcast(intent);
    }

    public String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }

    public void runSpeechRecognitionReceiver(){
        BroadcastReceiver ForegroundBR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                String app_log = arg1.getExtras().getString("SR");
                switch(app_log) {
                    case "ON":
                        start();
                        break;
                    case "OFF":
                        stop();
                        break;
                    case "KILL":
                        speech.stopListening();
                        speech.cancel();
                        speech.destroy();
                        break;
                    default:
                }

            }
        };
        MainActivity.context.registerReceiver(ForegroundBR, new IntentFilter("SpeechRecognitionReceiver"));
    }

    private void muteSystemAudio(boolean mute){
        AudioManager manager=(AudioManager)MainActivity.context.getSystemService(Context.AUDIO_SERVICE);
        manager.setStreamMute(AudioManager.STREAM_NOTIFICATION, mute);
        manager.setStreamMute(AudioManager.STREAM_SYSTEM, mute);
    }

    private static void sleepFor(long millisecond) {
        try{
            Thread.sleep(millisecond);
        }
        catch(Exception e){}
    }
}
