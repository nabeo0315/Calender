package com.example.nabeo.calender;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.example.nabeo.calender.MainActivity.context;
import static com.example.nabeo.calender.MySQLiteOpenHelper.DROP_BSSID_TABLE;
import static com.example.nabeo.calender.MySQLiteOpenHelper.DROP_ROOM_TABLE;
import static com.example.nabeo.calender.MySQLiteOpenHelper.DROP_WIFI_TABLE;

/**
 * Created by nabeo on 2017/05/23.
 */

public class ScanWifi extends AppCompatActivity {
    private static EditText place;
    private static TextView output;
    private static TextView predict_place;
    private AlertDialog.Builder alert, scan_alert;
    private static SQLiteDatabase db;
    public final static String ROOT_DIR = Environment.getExternalStorageDirectory().toString();
    private String SVM_DIR = ROOT_DIR +"/Calender";
    private String SVM_OUTPUT = SVM_DIR + "/output.txt";
    private String SVM_EXPERIMENT = SVM_DIR + "/predict_result.csv";
    private String SVM_PREDICT_DATA = SVM_DIR + "/predict_data.txt";
    private String SVM_PREDICT_SCALED = SVM_DIR + "/predict_scaled.txt";
    private String SVM_SCALE = SVM_DIR + "/scale.txt";
    private String SVM_MODEL = SVM_DIR + "/model2.model";
    private static Context context_scanwifi;
    private static String p;
    public static int count, counter;
    private String room;
    private RadioGroup radioGroup;
    private RadioButton radioButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_setting);
        //context_scanwifi = getApplicationContext();
        context_scanwifi = this;

        db = new MySQLiteOpenHelper(context_scanwifi).getWritableDatabase();

        place = (EditText)findViewById(R.id.editText);
        predict_place = (EditText)findViewById(R.id.predict_place);
        output = (TextView)findViewById(R.id.output);

        Button scanButton = (Button)findViewById(R.id.scan_button);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiScanData();
            }
        });

        Button dbButton = (Button)findViewById(R.id.show_db);
        dbButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent dbIntent = new Intent(context_scanwifi, show_db.class);
                startActivity(dbIntent);
            }
        });

        alert = new AlertDialog.Builder(this);
        alert.setTitle("データベース消去");
        alert.setMessage("よろしいですか？");
        alert.setPositiveButton("はい", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                db.execSQL(DROP_WIFI_TABLE);
                db.execSQL(DROP_ROOM_TABLE);
                db.execSQL(DROP_BSSID_TABLE);
                db.close();
            }
        });
        alert.setNegativeButton("いいえ", null);

        Button delete_button = (Button)findViewById(R.id.delete_button);
        delete_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alert.show();
            }
        });

        Button make_train_data = (Button)findViewById(R.id.make_train_data);
        make_train_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AsyncTaskTrain async_train = new AsyncTaskTrain(context_scanwifi);
                async_train.execute();
            }
        });

        Button make_predict_data = (Button)findViewById(R.id.predict_button);
        make_predict_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                RadioGroup radioGroup = (RadioGroup)findViewById(R.id.radioButtonGroup);
//                int id = radioGroup.getCheckedRadioButtonId();
//                RadioButton radioButton = (RadioButton)findViewById(id);
//                if(place.getText().toString().isEmpty()) return;
                AsyncTaskPredict task = new AsyncTaskPredict(context_scanwifi, output);
                p = predict_place.getText().toString();
                task.execute();
                //predictLocation();

            }
        });
    }

    private void predictLocation(){
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                for(ScanResult result: ((WifiManager)context.getSystemService(Context.WIFI_SERVICE)).getScanResults()) {
                    Cursor c = db.rawQuery("select id from bssid where mac = '" + result.BSSID + "'", null);
                    //電大のAP以外を推定データに含めない
                    if(!result.SSID.startsWith("TDU_MRCL")) continue;
                    if(c.getCount() == 0) continue;
                    c.moveToNext();
                    db.execSQL("insert into predict(bssid_id, rssi, count) values (" + c.getInt(c.getColumnIndex("id")) + "," + result.level +"," + (count+1) +")");
                    c.close();

                    Log.v("state", "success");
                }
                StringBuilder sb = new StringBuilder();

                for(int i = 1; i < 11; i++) {
                    Cursor c = db.rawQuery("select * from predict where count =" + i + " order by bssid_id asc", null);
                    sb.append(0);
                    while (c.moveToNext()) {
                        sb.append(" " + c.getInt(c.getColumnIndex("bssid_id")) + ":" + c.getInt(c.getColumnIndex("rssi")));
                    }
                    sb.append("\n");
                }

                try{
                    File file = new File(SVM_PREDICT_DATA);
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


                new Handler().post(new DisplayToast(ScanWifi.this, "scanCount：" + String.valueOf(count)));
                ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).startScan();
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).startScan();
    }

    private void wifiScanData(){
        room = place.getText().toString();
        scan_alert = new AlertDialog.Builder(this);
        scan_alert.setTitle("新しいロケーション");
        scan_alert.setMessage("新しいロケーションです。スキャンしますか？");
        scan_alert.setPositiveButton("はい", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AsyncTaskScan task = new AsyncTaskScan(context_scanwifi, room, radioButton.getText().toString());
                task.execute();
                place.setText("");
            }
        });
        scan_alert.setNegativeButton("いいえ", null);
        if(room.isEmpty()){
            return ;
        }else{
            radioGroup = (RadioGroup)findViewById(R.id.radioButtonGroup);
            int id = radioGroup.getCheckedRadioButtonId();
            radioButton = (RadioButton)findViewById(id);

            Cursor c = db.rawQuery("select * from room where name = '" + room + "'", null);
            Log.d("www", String.valueOf(c.getColumnCount()));
            Log.d("www", room);
            if(c.getCount() == 0){
                scan_alert.show();
                return;
            }else{
//                AsyncTaskScan task = new AsyncTaskScan(context_scanwifi, room, radioButton.getText().toString());
//                task.execute();
                //db.execSQL("delete from wifi where room_id = 11 and count > 40");

                counter = 0;
                if(db.rawQuery("select * from room where name = \"" + room + "\"", new String[]{}).getCount() != 0){
                    Cursor cursor = db.rawQuery("select max(count) as max from wifi where room_id = '" + getRoomId(room) + "'", null);
                    cursor.moveToNext();
                    count = cursor.getInt(cursor.getColumnIndex("max"));
                    cursor.close();
                }

                registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        counter++;
                        if(counter < 11) {
                            Log.v("broadcastReceiver", "receive");
                            count++;
                            scanTrainData(room, radioButton.getText().toString());
                        }else{
                            unregisterReceiver(this);
                        }
                        ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).startScan();
                    }
                }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).startScan();
                place.setText("");
            }

        }
    }

    public void setResult(){
        StringBuilder sb = new StringBuilder();
        int[] results;
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        Map.Entry<String, Integer> maxEntry = null;
        String room, line = null;

        try{
            File file = new File(SVM_OUTPUT);
            BufferedReader bf = new BufferedReader(new FileReader(file));
            bf.readLine();
            line = bf.readLine();
            while(line != null){
                String[] num = line.split(" ");
                Log.v("st", String.valueOf(num[0]));
                Cursor c = db.rawQuery("select name from room where id='" + num[0] + "'", null);
                c.moveToNext();
                room = c.getString(c.getColumnIndex("name"));
                //sb.append(c.getString(c.getColumnIndex("name")) + "\n");
                if(map.containsKey(room)){
                    map.put(room, map.get(room) + 1);
                }else{
                    map.put(room, 1);
                }
                line = bf.readLine();
            }

            for(Map.Entry<String, Integer> entry : map.entrySet()){
                if(maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0){
                    maxEntry = entry;
                }
            }

            sb.append(maxEntry.getKey());

        }catch(FileNotFoundException e){
            System.out.println(e);
        }catch(IOException e){
            System.out.println(e);
        }

        output.setText(sb.toString());

        //if(new File(SVM_EXPERIMENT).exists()) new File(SVM_EXPERIMENT).mkdir();
        StringBuilder sb2 = new StringBuilder();
        sb2.append(p);
        try{
            File file = new File(SVM_OUTPUT);
            BufferedReader br = new BufferedReader(new FileReader(file));
            try{
                while(true){
                    String line2 = br.readLine();
                    if(line2 == null){
                        break;
                    }
                    sb2.append("," + line);
                }
                sb2.append("\n");
            }finally {
                br.close();
            }
        }catch(FileNotFoundException e){
            System.out.println(e);
        }catch(IOException e){
            e.printStackTrace();
        }
        try{
            File file = new File(SVM_EXPERIMENT);
            FileWriter fw = new FileWriter(file, true);
            fw.write(sb2.toString());
            fw.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void scanTrainData(String room_name, String room_state){
        new Handler().post(new DisplayToast(this, "スキャン回数：" + String.valueOf(count)));
        for(ScanResult result: ((WifiManager)context.getSystemService(Context.WIFI_SERVICE)).getScanResults()) {
            String sql = "insert into wifi(timestamp, room_id, bssid_id, count, ssid, rssi, state) values ('"
                    + new SimpleDateFormat("kk':'mm':'ss").format(new Date()) + "', " + getRoomId(room_name) + ", "  + getBssidId(result.BSSID) + "," + count +
                    ", '" + result.SSID + "', " + result.level + ", '" + room_state + "')";
            db.execSQL(sql);
            Log.v("state", result.SSID);
        }
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

    public static Context getContext(){
        return context_scanwifi;
    }
}
