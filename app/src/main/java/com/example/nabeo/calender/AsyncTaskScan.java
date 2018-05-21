package com.example.nabeo.calender;

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
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.example.nabeo.calender.MySQLiteOpenHelper.CREATE_BSSID_TABLE;
import static com.example.nabeo.calender.MySQLiteOpenHelper.CREATE_ROOM_TABLE;
import static com.example.nabeo.calender.MySQLiteOpenHelper.CREATE_WIFI_TABLE;
import static com.example.nabeo.calender.MySQLiteOpenHelper.CREATE_BSSID_TABLE;
import static com.example.nabeo.calender.MySQLiteOpenHelper.CREATE_ROOM_TABLE;
import static com.example.nabeo.calender.MySQLiteOpenHelper.CREATE_WIFI_TABLE;

/**
 * Created by nabeo on 2017/04/23.
 */

public class AsyncTaskScan extends AsyncTask<Void, Integer, Integer> {
    private ProgressDialog dialog;
    private String room_name;
    private String room_state;
    private Context context;
    private SQLiteDatabase db;
    private MySQLiteOpenHelper hlper;
    private boolean finish;
    private boolean flag;
    private int count;
    private WifiManager manager;

    AsyncTaskScan(Context context, String room_name, String room_state){
        this.context = context;
        this.room_name = room_name;
        this.count = 0;
        this.hlper = new MySQLiteOpenHelper(context);
        this.db = hlper.getWritableDatabase();
        this.finish = false;
        this.flag = false;
        this.room_state = room_state;
//        if(state == "使用中"){
//            this.room_state = "nowUsing";
//        }else{
//            this.room_state = "notUsed";
//        }
    }

    @Override
    public void onPreExecute(){
        db.execSQL(CREATE_WIFI_TABLE);
        db.execSQL(CREATE_ROOM_TABLE);
        db.execSQL(CREATE_BSSID_TABLE);
        dialog = new ProgressDialog(ScanWifi.getContext());
        dialog.setTitle("scanning");
        dialog.setMessage("scanning data");
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
        dialog.setMax(10);
        dialog.setProgress(0);
        dialog.show();
    }

    @Override
    public Integer doInBackground(Void... voids){
        int add = 0;
        if(db.rawQuery("select * from room where name = \"" + room_name + "\"", new String[]{}).getCount() != 0){
            Cursor c = db.rawQuery("select max(count) as max from wifi where room_id = '" + getRoomId(room_name) + "'", null);
            c.moveToNext();
            add = c.getInt(c.getColumnIndex("max"));
            count = add;
            c.close();
        }
        manager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                for(ScanResult result: manager.getScanResults()) {
                    Date date = new Date();
                    SimpleDateFormat f = new SimpleDateFormat("kk':'mm':'ss");
                    String sql = "insert into wifi(timestamp, room_id, bssid_id, count, ssid, rssi, state) values ('"
                          + f.format(date) + "', " + getRoomId(room_name) + ", "  + getBssidId(result.BSSID) + "," + (count+1) +
                            ", '" + result.SSID + "', " + result.level + ", '" + room_state + "')";
                    db.execSQL(sql);
                    Log.v("state", "success");
                }
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
                if (count < 10+add) {//繰り返しの回数を指定
                    publishProgress(count-add);
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
        return 0;
    }

    @Override
    public void onProgressUpdate(Integer... integers){
        dialog.setProgress(integers[0]);
    }

    @Override
    public void onPostExecute(Integer i){
        dialog.dismiss();
    }

    private int getRoomId(String room){
        int room_id;
        Cursor c = db.rawQuery("select * from room where name = '" + room + "'", null);
        if(c.getCount() == 0){
            db.execSQL("insert into room(name) values('" + room + "')");
           // Log.v("getRoomID state", "arrive");
        }
        c.close();
        c = null;
        c = db.rawQuery("select * from room where name = '" + room + "'", null);
        c.moveToNext();
        room_id = c.getInt(c.getColumnIndex("id"));
        c.close();
        //Log.v("getRoomId state 2", "close");
        return room_id;
    }

    private int getBssidId(String bssid){
        int bssid_id;
        Cursor c = db.rawQuery("select * from bssid where mac = '" + bssid + "'", null);
        if(c.getCount() == 0){
            db.execSQL("insert into bssid(mac) values('" + bssid + "')");
        }
        c.close();
        c = null;
        c = db.rawQuery("select * from bssid where mac = '" + bssid + "'", null);
        c.moveToNext();
        bssid_id = c.getInt(c.getColumnIndex("id"));
        c.close();
        return bssid_id;
    }
}
