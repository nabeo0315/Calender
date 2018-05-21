package com.example.nabeo.calender;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Created by nabeo on 2017/07/12.
 */

public class AsyncTest extends AsyncTask <Void, Void, Void>{

    @Override
    protected void onPreExecute() {
        Log.v("asyncTest", "onPreExecute");
    }

    @Override
    protected Void doInBackground(Void... params) {
        Log.v("asyncTest", "doInBackground");
        return null;
    }
}
