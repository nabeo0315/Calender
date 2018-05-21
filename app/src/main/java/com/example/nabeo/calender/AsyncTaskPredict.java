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
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by nabeo on 2017/05/02.
 */

public class AsyncTaskPredict extends AsyncTask <Void, Integer, Integer>{
    private ProgressDialog dialog;
    private Context context;
    private boolean flag;
    private boolean finish;
    private int count;
    private File file;
    private ScanWifi scanWifi;
    private WifiManager manager;
    private StringBuilder sb;
    private MySQLiteOpenHelper hlpr;
    private SQLiteDatabase db;
    private TextView output;
    private String room_name;
    private String room_state;
    private String SVM_DIR = ScanWifi.ROOT_DIR +"/Calender";
    private String SVM_PREDICT_DATA = SVM_DIR + "/predict_data.txt";
    private String SVM_PREDICT_SCALED = SVM_DIR + "/predict_scaled.txt";
    private String SVM_SCALE = SVM_DIR + "/scale.txt";
    private String SVM_MODEL = SVM_DIR + "/model2.model";
    private String SVM_OUTPUT = SVM_DIR + "/output.txt";

    AsyncTaskPredict(Context context, TextView output){
        this.context = context;
        this.hlpr = new MySQLiteOpenHelper(context);
        this.db = hlpr.getWritableDatabase();
        this.flag = false;
        this.finish = false;
        this.output = output;
        this.scanWifi = new ScanWifi();
        this.room_name = room_name;
        this.room_state = room_state;
    }

    static {
        System.loadLibrary("jnilibsvm");
    }

    public native void jniSvmPredict(String option);

    @Override
    public void onPreExecute(){
        db.execSQL(hlpr.DROP_PREDICT_TABLE);
        db.execSQL(hlpr.CREATE_PREDICT_TABLE);
        dialog = new ProgressDialog(context);
        dialog.setTitle("推定中");
        dialog.setMessage("推定中です");
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
        dialog.setMax(10);
        dialog.setProgress(0);
        dialog.show();
    }

    @Override
    protected Integer doInBackground(Void... voids){
        count = 0;
        sb = new StringBuilder();
        manager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
//        boolean room_exit = false;
//        int add = 0;
//        if(db.rawQuery("select * from room where name = \"" + room_name + "\"", new String[]{}).getCount() != 0){
//            room_exit = true;
//        }
//        if(room_exit){
//            Cursor c = db.rawQuery("select max(count) as max from wifi where room_id = '" + getRoomId(room_name) + "'", null);
//            c.moveToNext();
//            add = c.getInt(c.getColumnIndex("max"));
//            count = add;
//            c.close();
//        }
        if(new File(SVM_DIR).exists()) new File(SVM_DIR).mkdir();
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                    //sb.append(count+1);
                for(ScanResult result: manager.getScanResults()) {
                    Cursor c = db.rawQuery("select id from bssid where mac = '" + result.BSSID + "'", null);
                    //電大のAP以外を推定データに含めない
                    if(!result.SSID.startsWith("TDU_MRCL")) continue;
                    if(c.getCount() == 0) continue;
                    c.moveToNext();
                    db.execSQL("insert into predict(bssid_id, rssi, count) values (" + c.getInt(c.getColumnIndex("id")) + "," + result.level +"," + (count+1) +")");
                    c.close();

                    Log.v("state", "success");
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
                if (count < 10) {//繰り返しの回数を指定
                    publishProgress(count);
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
        for(int i = 1; i < 11; i++) {
            Cursor c = db.rawQuery("select * from predict where count =" + i + " order by bssid_id asc", null);
            sb.append(0);
            while (c.moveToNext()) {
                sb.append(" " + c.getInt(c.getColumnIndex("bssid_id")) + ":" + c.getInt(c.getColumnIndex("rssi")));
            }
            sb.append("\n");
        }
        try{
            file = new File(SVM_PREDICT_DATA);
            if(file.exists()){
                file.delete();
            }
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(sb.toString());
            bw.flush();
            bw.close();
        }catch (IOException e){
            e.printStackTrace();
        }

        Scale();

        try{
            jniSvmPredict("-b 1 " + SVM_PREDICT_SCALED + " " + SVM_MODEL + " " + SVM_OUTPUT);
        }catch (Exception e){
            e.printStackTrace();
        }

        return 0;
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

    @Override
    public void onProgressUpdate(Integer... integers){
        dialog.setProgress(integers[0]);
    }

    @Override
    public void onPostExecute(Integer i){
        scanWifi.setResult();
        dialog.dismiss();
    }

    public void countElements(){

    }

}
