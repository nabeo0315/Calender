package com.example.nabeo.calender;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Queue;

import static com.example.nabeo.calender.MySQLiteOpenHelper.CREATE_CLASS_TABLE;
import static com.example.nabeo.calender.MySQLiteOpenHelper.DROP_CLASS_TABLE;

/**
 * Created by nabeo on 2017/06/13.
 */

public class SetTodayEvents extends BroadcastReceiver {
    //public static Queue<String> queue;

    @Override
    public void onReceive(Context context,Intent intent){
        Calendar calendar = Calendar.getInstance();
        GetTodayEvents getTodayEvents = new GetTodayEvents(context, calendar);
        //queue = new ArrayDeque<String>();
        Log.v("SetTodayEvent", "start");
        getTodayEvents.execute();
    }

    public class GetTodayEvents extends AsyncTask<Void, Integer, Integer> {
        private ProgressDialog progressDialog;
        private Context context;
        private ContentResolver cr;
        private TextView textView;
        private StringBuilder sb;
        private Calendar calendar;
        private int MY_PERMISSIONS_REQUEST_READ_CALENDAR = 123;
        private int today_year;
        private int today_month;
        private int today_date;
        private MySQLiteOpenHelper msql;
        private SQLiteDatabase db;

        GetTodayEvents(Context context, Calendar calendar){
            this.context = context;
            this.calendar = calendar;
            this.sb = new StringBuilder();
            this.today_year = calendar.get(Calendar.YEAR);
            this.today_month = calendar.get(Calendar.MONTH);
            this.today_date = calendar.get(Calendar.DATE);
            this.msql = new MySQLiteOpenHelper(context);
            this.db = msql.getWritableDatabase();
        }

        @Override
        protected void onPreExecute(){
//        progressDialog = new ProgressDialog(context);
//        progressDialog.setTitle("取得中");
//        progressDialog.setMessage("取得中です");
//        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//        progressDialog.setCancelable(false);
//        progressDialog.show();
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
            if((cr = context.getContentResolver()) == null) Log.v("cr" , "null");
            Uri uri = Uri.parse("content://com.android.calendar/events");

            Calendar calendar = Calendar.getInstance();
            calendar.set(today_year, today_month, today_date, 0, 0, 0);

            Log.v("today", today_year + " " + today_month + " " + today_date);
            long startMillis = calendar.getTimeInMillis();
            long endMillis = calendar.getTimeInMillis() + Long.parseLong("86399000");
            Log.v("mills", Long.toString(startMillis));
            Log.v("mills", Long.toString(endMillis));

            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, startMillis);
            ContentUris.appendId(builder, endMillis);

            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_CALENDAR)
                    != PackageManager.PERMISSION_GRANTED) {


//                if (ActivityCompat.shouldShowRequestPermissionRationale(context,
//                        Manifest.permission.READ_CALENDAR)) {
//                    // Show an expanation to the user *asynchronously* -- don't block
//                    // this thread waiting for the user's response! After the user
//                    // sees the explanation, try again to request the permission.
//
//                } else {
//
//                    // No explanation needed, we can request the permission.
//
//                    ActivityCompat.requestPermissions(context,
//                            new String[]{Manifest.permission.READ_CALENDAR},
//                            MY_PERMISSIONS_REQUEST_READ_CALENDAR);
//
//                    // MY_PERMISSIONS_REQUEST_READ_CALENDAR is an
//                    // app-defined int constant. The callback method gets the
//                    // result of the request.
//                }
            }else{
                //Cursor cursor = cr.query(uri, projection, selection, selectionArgs, null);
                //Cursor cursor = cr.query(uri, projection, null, null, null);
                Cursor cur =  cr.query(builder.build(),
                        INSTANCE_PROJECTION,
                        null,
                        null,
                        null);

                db.execSQL(DROP_CLASS_TABLE);
                db.execSQL(CREATE_CLASS_TABLE);

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

                    if(title.matches(".*" + "(講義)" + ".*")) {
                        String sql = "insert into class(class_name, starttime, endtime, place) " +
                                "values('" + title + "', '" + Long.toString(beginVal) + "', '" + endVal + "', '" + place + "')";
                        db.execSQL(sql);

                        // Do something with the values.
                        Log.v("", "Event:  " + title);
                        Calendar calendar_begin = Calendar.getInstance();
                        calendar_begin.setTimeInMillis(beginVal);
                        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                        Log.i("", "start: " + formatter.format(beginVal));
                        Calendar calendar_end = Calendar.getInstance();
                        calendar_end.setTimeInMillis(endVal);
                        Log.i("", "end: " + formatter.format(endVal));
                        Log.i("", "startTime:" + startMillis);

                        sb.append(eventID + ":" + title + "\n" +
                                "場所:" + place + "\n" +
                                formatter.format(beginVal) + "-" + formatter.format(endVal) + "\n" +
                                "-----------------------------------------------------\n");
                    }
                }
            }

            return 0;
        }

        @Override
        public void onPostExecute(Integer i){
            Log.i("asynctask state", "complete");
            Toast.makeText(context, "completed", Toast.LENGTH_SHORT).show();
            setClassTime();
        }

        public void setClassTime(){
            Cursor c = db.rawQuery("select class_name, starttime, endtime, place from class", null);
            while(c.moveToNext()){
                if(System.currentTimeMillis() > Long.parseLong(c.getString(1))) continue;
                MainActivity.setScanTime(Long.parseLong(c.getString(1)), Long.parseLong(c.getString(2)), context, c.getString(3), c.getString(0));
            }
        }
    }
}
