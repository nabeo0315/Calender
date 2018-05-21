package com.example.nabeo.calender;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static java.util.Calendar.getInstance;

/**
 * Created by nabeo on 2017/05/21.
 */

public class DayDetail extends AppCompatActivity {
    private int year;
    private int month;
    private int dayOfMonth;
    private int MY_PERMISSIONS_REQUEST_READ_CALENDAR = 123;
    private SimpleDateFormat format;
    private Calendar calendar;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.day_detail);

        format = new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN);
        Intent intent = getIntent();
        year = intent.getIntExtra("year", 0);
        month = intent.getIntExtra("month", 0);
        dayOfMonth = intent.getIntExtra("dayOfMonth", 0);

        calendar = Calendar.getInstance();
        calendar.set(year, month, dayOfMonth, 0, 0, 0);

        textView = (TextView)findViewById(R.id.show_date);

        Button event_button = (Button)findViewById(R.id.event_button);
//        event_button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                AsyncTaskGetEvent async = new AsyncTaskGetEvent(DayDetail.this, calendar, textView);
//                async.execute();
//            }
//        });
        AsyncTaskGetEvent async = new AsyncTaskGetEvent(DayDetail.this, calendar, textView);
        async.execute();
    }

    private class AsyncTaskGetEvent extends AsyncTask<Void, Integer, Integer> {
        private ProgressDialog progressDialog;
        private Context context;
        private ContentResolver cr;
        private TextView textView;
        private StringBuilder sb = new StringBuilder();
        private Calendar calendar;

        AsyncTaskGetEvent(Context context, Calendar calendar, TextView textView){
            this.textView = (TextView)findViewById(R.id.show_event);
            this.context = context;
            this.calendar = calendar;
            this.textView = textView;
        }

        @Override
        protected void onPreExecute(){
            progressDialog = new ProgressDialog(context);
            progressDialog.setTitle("取得中");
            progressDialog.setMessage("取得中です");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Integer doInBackground(Void... voids){


            String[] projection = {
                    CalendarContract.Events._ID,
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.EVENT_LOCATION,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.DTEND,
                    CalendarContract.Events.RDATE,
                    CalendarContract.Events.RRULE
            };

            String[] INSTANCE_PROJECTION = new String[] {
                    CalendarContract.Instances.EVENT_ID,      // 0
                    CalendarContract.Instances.BEGIN,// 1
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.EVENT_LOCATION
            };

            String selection = "(" +
                    "(" + CalendarContract.Events.ACCOUNT_NAME + " = ?) AND" +
                    "(" + CalendarContract.Events.ACCOUNT_TYPE + " = ?)" +
                    ")";
            String[] selectionArgs = new String[]{"y.0407love.ms.vega@gmail.com", "com.google"};
            cr = getContentResolver();
            if(cr != null) Log.i("cr", "notnull");
            Uri uri = Uri.parse("content://com.android.calendar/events");

            //選択された日時をエポックミリ秒に変換
            long startMillis = calendar.getTimeInMillis();
            long endMillis = calendar.getTimeInMillis() + Long.parseLong("86399000");
            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, startMillis);
            ContentUris.appendId(builder, endMillis);

            Log.v("startMills", Long.toString(startMillis));
            Log.v("endMills", Long.toString(endMillis));

            if (ContextCompat.checkSelfPermission(DayDetail.this,
                    Manifest.permission.READ_CALENDAR)
                    != PackageManager.PERMISSION_GRANTED) {


                if (ActivityCompat.shouldShowRequestPermissionRationale(DayDetail.this,
                        Manifest.permission.READ_CALENDAR)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(DayDetail.this,
                            new String[]{Manifest.permission.READ_CALENDAR},
                            MY_PERMISSIONS_REQUEST_READ_CALENDAR);

                    // MY_PERMISSIONS_REQUEST_READ_CALENDAR is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }else{
                //Cursor cursor = cr.query(uri, projection, selection, selectionArgs, null);
                //Cursor cursor = cr.query(uri, projection, null, null, null);
                Cursor cur =  cr.query(builder.build(),
                        INSTANCE_PROJECTION,
                        null,
                        null,
                        null);

                while (cur.moveToNext()) {
                    String title = null;
                    long eventID = 0;
                    long beginVal = 0;
                    long endVal = 0;
                    String place = null;

                    // Get the field values
                    eventID = cur.getLong(0);
                    beginVal = cur.getLong(1);
                    endVal = cur.getLong(2);
                    title = cur.getString(3);
                    place = cur.getString(4);

                    // Do something with the values.
                    Log.v("", "Event:  " + title);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(beginVal);
                    DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                    Log.i("", "start: " + formatter.format(beginVal));
                    calendar =  Calendar.getInstance();
                    calendar.setTimeInMillis(endVal);
                    Log.i("", "end: " + formatter.format(endVal));
                    Log.i("", "startTime:" + startMillis);

                    sb.append(eventID + ":" + title + "\n" +
                                "場所:" + place + "\n" +
                                formatter.format(beginVal) + "-" + formatter.format(endVal) + "\n" +
                                "-----------------------------------------------------\n");
                }

//                for(boolean hasNext = cursor.moveToFirst(); hasNext; hasNext = cursor.moveToNext()) {
//                    long eventID = cursor.getLong(0);
//                    String title = cursor.getString(1);
//                    String location = cursor.getString(2);
//                    long startSec = cursor.getLong(3);
//                    long endSec = cursor.getLong(4);
//                    String rdate = cursor.getString(5);
//                    String rrule = cursor.getString(6);
//
//
//                    Date eventDate = new Date(startSec);
//
//                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN);
//                    SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.JAPAN);
//                    String event = format.format(eventDate);
//                    String selected = format.format(calendar.getTime());
//                    Log.v("calendardate", selected);
//                    Log.v("eventdate", event);
//                    //Log.v("rdate", rdate);
//                    //Log.v("rrule", rrule);
//                    if(event.equals(selected)) {
//                        Log.v("watanabe", eventID + ":" + title);
//                        Log.v("a", format.format(eventDate) + " - " + format.format(endSec));
//                        Log.v("a", "-----------------------------------");
//                        sb.append(eventID + ":" + title + "\n" +
//                                "場所:" + location + rrule + "\n" +
//                                format2.format(eventDate) + " - " + format2.format(endSec) + rdate + "\n" +
//                                "-----------------------------------\n");
//                    }
//                }

                //cursor.close();

            }

            return 0;
        }

        @Override
        public void onPostExecute(Integer i){
            progressDialog.dismiss();
            textView.setText(sb.toString());
        }

    }
}
