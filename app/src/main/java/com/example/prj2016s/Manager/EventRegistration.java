package com.example.prj2016s.Manager;

import java.io.InputStream;

/**
 * Created by Kang on 2016-06-25.
 */
public class EventRegistration {
    private CallbackEvent callbackEvent;

    public EventRegistration(CallbackEvent event){
        callbackEvent = event;
    }

    public InputStream doWork(){
        return callbackEvent.callbackMethod();
    }
}