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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StreamTokenizer;

/**
 * Created by nabeo on 2017/07/12.
 */
public class AsyncTaskPredict2 extends AsyncTask<Void, Integer, Integer> {
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
    private String SVM_DIR = MainActivity.ROOT_DIR +"/Calender";
    private String SVM_PREDICT_DATA = SVM_DIR + "/predict_data.txt";
    private String SVM_PREDICT_SCALED = SVM_DIR + "/predict_scaled.txt";
    private String SVM_SCALE = SVM_DIR + "/scale.txt";
    private String SVM_MODEL = SVM_DIR + "/model.model";
    private String SVM_OUTPUT = SVM_DIR + "/output.txt";

    AsyncTaskPredict2(Context context){
        this.context = context;
        this.hlpr = new MySQLiteOpenHelper(context);
        this.db = hlpr.getWritableDatabase();
        this.flag = false;
        this.finish = false;
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
        dialog.setTitle("scanning");
        dialog.setMessage("scanning data");
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
        dialog.setMax(3);
        dialog.setProgress(0);
        dialog.show();
    }

    @Override
    protected Integer doInBackground(Void... voids){
        count = 0;
        sb = new StringBuilder();
        manager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

        if(new File(SVM_DIR).exists()) new File(SVM_DIR).mkdir();
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //sb.append(count+1);
                for(ScanResult result: manager.getScanResults()) {
                    Cursor c = db.rawQuery("select id from bssid where mac = '" + result.BSSID + "'", null);
                    if(c.getCount() == 0) continue;
                    c.moveToNext();
                    db.execSQL("insert into predict(bssid_id, rssi, count) values (" + c.getInt(c.getColumnIndex("id")) + "," + result.level +"," + (count+1) +")");
                    //sb.append(" " + c.getInt(c.getColumnIndex("id")) + ":" + result.level);
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
                if (count < 3) {//繰り返しの回数を指定
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
        for(int i = 1; i < 4; i++) {
            Cursor c = db.rawQuery("select * from predict where count =" + i + " order by bssid_id asc", null);
            sb.append(0);
            while (c.moveToNext()) {
                sb.append(" " + c.getInt(c.getColumnIndex("bssid_id")) + ":" + c.getInt(c.getColumnIndex("rssi")));
            }
            sb.append("\n");
        }
        try{
            // FileOutputStream out = this.context.openFileOutput("train_data.csv", Context.MODE_PRIVATE);
            //PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
            //writer.append(sb.toString());
            //writer.close();
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
            jniSvmPredict(SVM_PREDICT_SCALED + " " + SVM_MODEL + " " + SVM_OUTPUT);
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
        setResult();
        dialog.dismiss();
        MainActivity.lock2 = false;
    }

    public  void setResult(){
        StringBuilder sb = new StringBuilder();


        try{
            File file = new File(SVM_OUTPUT);
            FileReader filereader = new FileReader(file);
            StreamTokenizer st = new StreamTokenizer(filereader);

            while(st.nextToken()!= StreamTokenizer.TT_EOF){
                Log.v("st", String.valueOf(st.nval));
                Cursor c = db.rawQuery("select name from room where id='" + st.nval + "'", null);
                c.moveToNext();
                sb.append(c.getString(c.getColumnIndex("name")) + "\n");
            }
        }catch(FileNotFoundException e){
            System.out.println(e);
        }catch(IOException e){
            System.out.println(e);
        }

        MainActivity.predictedRoom.setText(sb.toString());
    }

}