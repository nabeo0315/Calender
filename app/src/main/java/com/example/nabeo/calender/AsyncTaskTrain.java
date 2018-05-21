package com.example.nabeo.calender;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by nabeo on 2017/04/26.
 */

public class AsyncTaskTrain extends AsyncTask<Void, Integer, Integer> {
    private Context context;
    private ProgressDialog progressDialog;
    private SQLiteDatabase db;
    private File file;

    private String SVM_DIR = ScanWifi.ROOT_DIR +"/Calender";
    private String SVM_TRAIN_DATA = SVM_DIR + "/train_data.txt";
    private String SVM_TRAIN_SCALED = SVM_DIR + "/train_scaled.txt";
    private String SVM_SCALE = SVM_DIR + "/scale.txt";
    private String SVM_MODEL = SVM_DIR + "/model2.model";

    AsyncTaskTrain(Context context){
        this.context = context;
    }

    static {
        System.loadLibrary("jnilibsvm");
    }

    public native void jniSvmTrain(String option);

    @Override
    protected void onPreExecute(){
        db = new MySQLiteOpenHelper(context).getWritableDatabase();
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("学習中");
        progressDialog.setMessage("学習中です");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }


    @Override
    protected Integer doInBackground(Void... voids){
        if(!new File(SVM_DIR).exists()) new File(SVM_DIR).mkdir();
        StringBuilder sb = new StringBuilder();
        Cursor c = db.rawQuery("select id from room", null);
        c.moveToNext();
        while(c.moveToNext()) {
            int room_id = c.getInt(c.getColumnIndex("id"));
            Cursor c2 = db.rawQuery("select distinct count from wifi where room_id = " + room_id, null);
                while (c2.moveToNext()) {
                int count = c2.getInt(c2.getColumnIndex("count"));
                int a;
                Cursor c3 = db.rawQuery("select * from wifi where count = " + count + " and room_id = " + room_id + " order by bssid_id asc", null);
                c3.moveToNext();
                Log.v("a", "a");
                sb.append(c3.getInt(c3.getColumnIndex("room_id")));
                do {
                    //電大のAP以外を学習データに含めない
                    if(!(c3.getString(c3.getColumnIndex("ssid")).startsWith("TDU_MRCL"))) continue;
                    sb.append(" " + c3.getInt(c3.getColumnIndex("bssid_id")) + ":" + c3.getInt(c3.getColumnIndex("rssi")));
                    Log.v("b", "b");
                } while (c3.moveToNext());
                c3.close();
                sb.append("\n");
            }
            c2.close();
        }
        c.close();
        try{
           // FileOutputStream out = this.context.openFileOutput("train_data.csv", Context.MODE_PRIVATE);
            //PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
            //writer.append(sb.toString());
            //writer.close();
            file = new File(SVM_TRAIN_DATA);
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
            jniSvmTrain("-t 0 -b 1 " + SVM_TRAIN_SCALED + " " + SVM_MODEL);
        }catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    private void Scale(){
        try {
            String[] args = {"-l", "-1", "-u", "0", "-s", SVM_SCALE, SVM_TRAIN_DATA};
            Scaler scaler = new Scaler();
            scaler.setOut_path(SVM_TRAIN_SCALED);
            scaler.run(args);

            //Scaller scaller = new Scaller().loadRange(new File(SVM_SCALE));
            //scaller.calcScaleFromFile(new File(SVM_BEFORE_SCALE), new File(SVM_TRAIN_DATA));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPostExecute(Integer i){
        progressDialog.dismiss();
    }
}

