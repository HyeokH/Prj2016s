package com.example.prj2016s.etc;

/**
 * Created by 성혁화 on 2016-06-26.
 */
public class EventRegistration {
    private CallbackEvent callbackEvent;

    public EventRegistration(CallbackEvent event){
        callbackEvent = event;
    }

    public void doWork(){
        callbackEvent.callbackMethod();
    }
}
