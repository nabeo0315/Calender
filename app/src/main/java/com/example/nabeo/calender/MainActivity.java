package com.example.nabeo.calender;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {
    public static Context context;
    private static MySQLiteOpenHelper hlpr;
    private static SQLiteDatabase db;
    private TextView step_detector;
    public static TextView predictedRoom, today_event;
    private AsyncTaskPredict2 async;
    private AsyncTest at;
    private SensorManager sensorManager;
    private Sensor sensor;
    private double threshold = 0.5;
    private int walkingCounter = 0;
    private int stopCounter = 0;
    private boolean stopFlag = true;
    public static boolean lock = false, lock2 = false;
    public final static String ROOT_DIR = Environment.getExternalStorageDirectory().toString();
    private String SVM_DIR = ROOT_DIR +"/Calender";
    private String SVM_OUTPUT = SVM_DIR + "/output.txt";
    private String SVM_EXPERIMENT = SVM_DIR + "/predict_result.csv";
    private static DateFormat formatter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        hlpr = new MySQLiteOpenHelper(context);
        db = hlpr.getWritableDatabase();
        formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        db.execSQL(hlpr.CREATE_ATTENDANCE_TABLE);

        today_event = (TextView)findViewById(R.id.today_event);
        step_detector = (TextView)findViewById(R.id.step_detector);
        predictedRoom = (TextView)findViewById(R.id.predict_tv);
        //predictedRoom.addTextChangedListener(this);
        step_detector.setText("停止中");
        if(today_event == null) Log.v("today_event", "null");
        //textView.setText("hello");

        CalendarView calenderView = (CalendarView)findViewById(R.id.calendarView);
        calenderView.setOnDateChangeListener(new CalendarView.OnDateChangeListener(){
            @Override
            public void onSelectedDayChange(CalendarView calendarView, int year, int month, int dayOfMonth){
                Log.v("you" , year + "年" + (month + 1) + "月" + dayOfMonth + "日");
                Log.v("a", "workit");
                Intent intent = new Intent(MainActivity.this, DayDetail.class);
                intent.putExtra("year", year);
                intent.putExtra("month", month);
                intent.putExtra("dayOfMonth", dayOfMonth);
                startActivity(intent);
            }
        });

        Button button = (Button)findViewById(R.id.show_today_event);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setEvent();
                ContentValues cv = new ContentValues();
            }
        });

        Calendar scanTime = Calendar.getInstance();
        scanTime.set(Calendar.HOUR_OF_DAY, 15);
        scanTime.set(Calendar.MINUTE, 5);
        scanTime.set(Calendar.SECOND, 0);
        Calendar calendar_now = Calendar.getInstance();
        long scanTime_millis = scanTime.getTimeInMillis();
        //long calendar_now_millis = calendar_now.getTimeInMillis();
        //setAlarmManager(scanTime_millis);

        //scanTime_millisが過去であれば、明日に設定
        if(scanTime_millis >= (System.currentTimeMillis())){
            setAlarmManager(scanTime_millis);
        }else{
            scanTime.add(Calendar.DAY_OF_MONTH, 1);
            scanTime_millis = scanTime.getTimeInMillis();
            setAlarmManager(scanTime_millis);
        }

//        Calendar calendar = Calendar.getInstance(); // Calendar取得
//        calendar.setTimeInMillis(System.currentTimeMillis()); // 現在時刻を取得
//        calendar.add(Calendar.SECOND, 15); // 現時刻より15秒後を設定
//        setAlarmManager(calendar.getTimeInMillis());

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                //Log.v("sensor", "active");
                String str = "加速度センサー値:" + "\nX軸:" + event.values[0] + "\nY軸:" + event.values[1] + "\nZ軸:" + event.values[2];
                double xyz = Math.sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2]);
//                Log.d("a",str);
//                Log.d("xyz", String.valueOf(xyz));
                if (xyz > threshold) {
                    walkingCounter++;
                    if (walkingCounter >= 38) {
                        stopFlag = false;
                        stopCounter = 0;
                        step_detector.setText("移動しています");
                        lock = false;
                    }
                } else {
                    stopCounter++;
                    if (stopCounter >= 13) {
                        step_detector.setText("停止中");
                        //AsyncTask.Status status = async.getStatus();
                        if (!lock && !lock2) {
                            lock = true;
                            lock2 = true;
                            doAsync();
                        }
                        walkingCounter = 0;
                        stopFlag = true;
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        }, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            /// /Nexus5x 80ms
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.menu_wifiScan).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent wifi_setting_intent = new Intent(MainActivity.this, ScanWifi.class);
                startActivity(wifi_setting_intent);
                return true;
            }
        });

        menu.findItem(R.id.menu_class).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent class_setting_intent = new Intent(MainActivity.this, SetClass.class);
                startActivity(class_setting_intent);
                return true;
            }
        });
        return true;
    }


    //public static Context getContext(){

    //return context;
    //}

    public static void setEvent(){
        if(db == null) Log.v("db", "null");
        Cursor c = db.rawQuery("select class_name, starttime, place from class", null);
        StringBuilder sb = new StringBuilder();
        sb.append("本日の予定\n");
        while(c.moveToNext()) {
            sb.append("-----------------------------------\n" +
                        c.getString(0) + "\n" +
                        "場所:" + c.getString(2) + "\n" +
                        "開始時間:" + formatter.format(Long.parseLong(c.getString(1))) + "\n");
          }
         today_event.setText(sb.toString());
    }

    private void doAsync(){
        async = new AsyncTaskPredict2(MainActivity.this);
        Log.v("www", "startAsyncTask");
        //async.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void setScanTime(long startTime, long endtime, Context context, String room, String class_name){
        //long scanTime = when.getTimeInMillis();
        int _id = (int)startTime;//idを開始時間から作成
        long interval = (endtime - startTime) / (600 * 1000);
        Log.v("interval", Integer.toString((int)interval));

        Intent intent = new Intent(context, CheckAttendance.class);
        intent.putExtra("room",  room);
        //intent.putExtra("startTime", Long.toString(startTime));
        intent.putExtra("class_name", class_name);
        for(int i = 0; i < (int)interval; i++) {
            Log.v("set alarm:", Long.toString(startTime));
            Log.v("_id", Integer.toString(_id));
            intent.putExtra("_id", _id);
            intent.putExtra("startTime", Long.toString(startTime));
            PendingIntent pendingIntent = PendingIntent.getService(context, _id, intent, 0);//一度だけ実行
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, startTime, pendingIntent);
            startTime += 600*1000;
            _id = (int)startTime;
        }
    }

    public void setAlarmManager(long target_time){
        Intent i = new Intent(MainActivity.this, SetTodayEvents.class);
        PendingIntent sender = PendingIntent.getBroadcast(MainActivity.this, 1, i, 0);
        AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, target_time, AlarmManager.INTERVAL_DAY, sender);
        //am.cancel(sender);
        //不要なアラームを解除
//        Intent i2 = new Intent(MainActivity.this, SetTodayEvents.class);
//        PendingIntent sender2 = PendingIntent.getBroadcast(MainActivity.this, 1, i2, 0);
//        AlarmManager test = (AlarmManager)getSystemService(ALARM_SERVICE);
        //test.set(AlarmManager.RTC_WAKEUP, 0000000000000, sender2);
//        test.cancel(sender2);
    }
}