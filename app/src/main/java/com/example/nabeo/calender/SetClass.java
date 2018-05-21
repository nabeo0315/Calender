package com.example.nabeo.calender;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;

import static com.example.nabeo.calender.MainActivity.context;

/**
 * Created by nabeo on 2017/05/31.
 */

public class SetClass extends AppCompatActivity {

    private final int MY_PERMISSIONS_REQUEST_READ_CALENDAR = 123;
    private Handler handler;
    private MySQLiteOpenHelper msql;
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.class_setting);
        this.msql = new MySQLiteOpenHelper(context);
        this.db = msql.getWritableDatabase();
        final TextView tv = (TextView)findViewById(R.id.show_event);

//        Button button = (Button) findViewById(R.id.get_event);
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//            }
//        });


        StringBuilder sb = new StringBuilder();
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        Cursor c = db.rawQuery("select class, date, room from attendance", null);
        while(c.moveToNext()){
            sb.append("講義:" + c.getString(0) + " 時間:" + formatter.format(Long.parseLong(c.getString(1))) + " 場所:" + c.getString(2));
            sb.append("\n");
        }
        final String text = sb.toString();

        handler = new Handler();
        final Thread thread = new Thread(){
            public void run(){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        tv.setText(text);
                    }
                });
            }
        };

        thread.start();

    }
}
