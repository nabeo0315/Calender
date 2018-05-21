package com.example.nabeo.calender;

import android.app.Application;
import android.content.Context;

/**
 * Created by nabeo on 2017/06/13.
 */

public class MyContext extends Application {
    private static Context context;

    @Override
    public void onCreate(){
        super.onCreate();
        MyContext.context = getApplicationContext();
    }

    public static Context getAppContext(){
        return MyContext.context;
    }
}
