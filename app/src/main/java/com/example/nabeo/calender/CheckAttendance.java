package com.example.nabeo.calender;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by nabeo on 2017/08/16.
 */

public class CheckAttendance extends IntentService {
    private ProgressDialog dialog;
    private Context context;
    private boolean flag;
    private boolean finish;
    private int count;
    private WifiManager manager;
    private StringBuilder sb;
    private MySQLiteOpenHelper hlpr;
    private SQLiteDatabase db;
    private File file;
    private DisplayToast displayToast;
    private Handler handler;
    private String room, class_name, date;
    private int _id;
    private String SVM_DIR = MainActivity.ROOT_DIR + "/Calender";
    private String SVM_PREDICT_DATA = SVM_DIR + "/predict_data.txt";
    private String SVM_PREDICT_SCALED = SVM_DIR + "/predict_scaled.txt";
    private String SVM_SCALE = SVM_DIR + "/scale.txt";
    private String SVM_MODEL = SVM_DIR + "/model.model";
    private String SVM_OUTPUT = SVM_DIR + "/output.txt";

    public CheckAttendance(){
        super("CheckAttendance");
        this.context = MainActivity.context;
        this.hlpr = new MySQLiteOpenHelper(context);
        this.db = hlpr.getWritableDatabase();
        this.flag = false;
        this.finish = false;
        this.handler = new Handler();
//        db.execSQL(hlpr.DROP_ATTENDANCE_TABLE);
        db.execSQL(hlpr.CREATE_ATTENDANCE_TABLE);
        System.loadLibrary("jnilibsvm");
    }

    public native void jniSvmPredict(String option);

    @Override
    public void onHandleIntent(Intent intent) {
        AsyncTaskPredict2 async = new AsyncTaskPredict2(context);
        Log.v("CheckAttendance", "check start");
//        async.execute();

        count = 0;
        sb = new StringBuilder();
        manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        room = (String)intent.getExtras().get("room");
        class_name = (String)intent.getExtras().get("class_name");
        date = (String)intent.getExtras().get("startTime");
        _id = (int)intent.getExtras().get("_id");
        Log.v("extras", room);
        Log.v("extras", class_name);
        Log.v("extras", date);
        Log.v("extras", Integer.toString(_id));
//        Cursor cursor = db.rawQuery("select class_name, starttime from class where place ='" + room + "'", null);
//        cursor.moveToNext();


        db.execSQL(hlpr.DROP_PREDICT_TABLE);
        db.execSQL(hlpr.CREATE_PREDICT_TABLE);

        if (new File(SVM_DIR).exists()) new File(SVM_DIR).mkdir();
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //sb.append(count+1);
                for (ScanResult result : manager.getScanResults()) {
                    Cursor c = db.rawQuery("select id from bssid where mac = '" + result.BSSID + "'", null);
                    if (c.getCount() == 0) continue;
                    c.moveToNext();
                    db.execSQL("insert into predict(bssid_id, rssi, count) values (" + c.getInt(c.getColumnIndex("id")) + "," + result.level + "," + (count + 1) + ")");
                    //sb.append(" " + c.getInt(c.getColumnIndex("id")) + ":" + result.level);
                    //Log.v("data", c.getInt(c.getColumnIndex("id")) + "," + result.level + "," + (count + 1) );
                    c.close();

                    Log.v("state_checkAttendance", "success");
                }
                //sb.append("\n");
                flag = true;
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        context.registerReceiver(broadcastReceiver, intentFilter);
        manager.startScan();
        while (!finish) {
            if (flag) {
                count++;
                if (count < 3) {//繰り返しの回数を指定
                    //publishProgress(count);
                    flag = false;
                    manager.startScan();
                } else {
                    finish = true;
                }
            }
            try {
                Thread.sleep(500);//繰り返しの多用を防止
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        context.unregisterReceiver(broadcastReceiver);

        //make predict data
        for (int i = 1; i < 4; i++) {
            Cursor c = db.rawQuery("select * from predict where count =" + i + " order by bssid_id asc", null);
            sb.append(0);
            while (c.moveToNext()) {
                sb.append(" " + c.getInt(c.getColumnIndex("bssid_id")) + ":" + c.getInt(c.getColumnIndex("rssi")));
            }
            sb.append("\n");
        }
        try {
            // FileOutputStream out = this.context.openFileOutput("train_data.csv", Context.MODE_PRIVATE);
            //PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
            //writer.append(sb.toString());
            //writer.close();
            file = new File(SVM_PREDICT_DATA);
            if (file.exists()) {
                file.delete();
            }
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(sb.toString());
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Scale();

        try {
            jniSvmPredict(SVM_PREDICT_SCALED + " " + SVM_MODEL + " " + SVM_OUTPUT);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setResult();
        //dialog.dismiss();
        MainActivity.lock2 = false;
    }

    private void Scale() {
        try {
            String[] args = {"-l", "-1", "-u", "0", "-r", SVM_SCALE, SVM_PREDICT_DATA};
            Scaler scaler = new Scaler();
            scaler.setOut_path(SVM_PREDICT_SCALED);
            scaler.run(args);

            //Scaller scaller = new Scaller().loadRange(new File(SVM_SCALE));
            //scaller.calcScaleFromFile(new File(SVM_PREDICT_BEFORE_SCALE), new File(SVM_PREDICT_DATA));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setResult(){
        StringBuilder sb = new StringBuilder();
        int[] results;
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        Map.Entry<String, Integer> maxEntry = null;
        String room_name;

        try{
            File file = new File(SVM_OUTPUT);
            FileReader filereader = new FileReader(file);
            StreamTokenizer st = new StreamTokenizer(filereader);

            while(st.nextToken()!= StreamTokenizer.TT_EOF){
                Log.v("st", String.valueOf(st.nval));
                Cursor c = db.rawQuery("select name from room where id='" + st.nval + "'", null);
                c.moveToNext();
                room_name = c.getString(c.getColumnIndex("name"));
                //sb.append(c.getString(c.getColumnIndex("name")) + "\n");
                if(map.containsKey(room_name)){
                    map.put(room_name, map.get(room_name) + 1);
                }else{
                    map.put(room_name, 1);
                }
            }

            for(Map.Entry<String, Integer> entry : map.entrySet()){
                if(maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0){
                    maxEntry = entry;
                }
            }

            sb.append(maxEntry.getKey());
            displayToast = new DisplayToast(context, sb.toString());
            handler.post(displayToast);
            Log.v("stringBuilder", sb.toString());
            Log.v("room", room);
            class_name = class_name.substring(4);
            if(sb.toString().equals(room)){
                db.execSQL("insert into attendance(class, date, room) values('" + class_name + "', '" + date + "', '" + room + "')");
                Log.v("result", "attending now");
            }

        }catch(FileNotFoundException e){
            System.out.println(e);
        }catch(IOException e){
            System.out.println(e);
        }

        cancelAlarm();


    }

    public void cancelAlarm(){
        Intent intent = new Intent(context, CheckAttendance.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, _id, intent, 0);//一度だけ実行
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }
}
