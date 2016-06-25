package com.example.prj2016s.Manager;

/**
 * Created by Kang on 2016-06-25.
 */
public class EventRegistration {
    private CallbackEvent callbackEvent;

    public EventRegistration(CallbackEvent event){
        callbackEvent = event;
    }

    public String doWork(){
        return callbackEvent.callbackMethod();
    }
}