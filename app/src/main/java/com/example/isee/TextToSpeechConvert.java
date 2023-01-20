package com.example.isee;

import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

public class TextToSpeechConvert {
    public static TextToSpeech TTS = null;

    public static void speak(String text){
        sendToSpeechRecognition("OFF");
        if (TTS == null){
            initTTS();

        }
        TTS.speak(text, TextToSpeech.QUEUE_FLUSH,null ,TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED);
    }

    public static void initTTS() {
        if(TTS == null){
            TTS = new TextToSpeech(MainActivity.context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if(status == TextToSpeech.SUCCESS) {
                        TTS.setLanguage(Locale.US);
                        TTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                            @Override
                            public void onStart(String utteranceId) {
                                Log.i("TextToSpeech","On Start");
                            }

                            @Override
                            public void onDone(String utteranceId) {
                                Log.i("TextToSpeech","On Done");
                                sendToSpeechRecognition("ON");
                            }

                            @Override
                            public void onError(String utteranceId) {
                                Log.i("TextToSpeech","On Error");
                            }
                        });
                    }
                }
            });
        }
    }

    private static void sleepFor(long millisecond) {
        try{
            Thread.sleep(millisecond);
        }
        catch(Exception e){}
    }

    private static void sendToSpeechRecognition(String text){
        Intent intent = new Intent("SpeechRecognitionReceiver");
        intent.putExtra("SR", text);
        MainActivity.context.sendBroadcast(intent);
    }
}
