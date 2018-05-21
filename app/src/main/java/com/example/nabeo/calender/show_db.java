package com.example.nabeo.calender;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static com.example.nabeo.calender.MySQLiteOpenHelper.CREATE_BSSID_TABLE;
import static com.example.nabeo.calender.MySQLiteOpenHelper.CREATE_ROOM_TABLE;
import static com.example.nabeo.calender.MySQLiteOpenHelper.CREATE_WIFI_TABLE;

/**
 * Created by nabeo on 2017/04/25.
 */

public class show_db extends AppCompatActivity {
    private SQLiteDatabase db;
    private TextView display;
    private String SVM_DIR = ScanWifi.ROOT_DIR +"/Calender";
    private String BSSID_DB = SVM_DIR + "/bssid_db.txt";
    private String ROOM_DB = SVM_DIR + "/room_db.txt";
    private String WIFI_DB = SVM_DIR + "/wifi_db.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_db);

        db = new MySQLiteOpenHelper(this).getWritableDatabase();

         display = (TextView)findViewById(R.id.display);

        Button wifi_button = (Button)findViewById(R.id.show_wifi_button);
        wifi_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder sb = new StringBuilder();
                db.execSQL(CREATE_WIFI_TABLE);
                Cursor c = db.rawQuery("select * from wifi", null);
                if(c.getCount() != 0){
                    while (c.moveToNext()) {
                        sb.append(c.getInt(c.getColumnIndex("id")) + "," + c.getString(c.getColumnIndex("timestamp")) + ","
                                + c.getInt(c.getColumnIndex("room_id")) + "," + c.getInt(c.getColumnIndex("bssid_id")) + ","
                                + c.getInt(c.getColumnIndex("count")) + "," + c.getString(c.getColumnIndex("ssid")) + ","
                                + c.getInt(c.getColumnIndex("rssi")) + c.getString(c.getColumnIndex("state")) + "\n");
                    }
                    c.close();
                    display.setText(sb.toString());
                }else{
                    display.setText("");
                }
                try{

                    File file = new File(WIFI_DB);
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

            }
        });

        Button room_button = (Button)findViewById(R.id.room_button);
        room_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder sb = new StringBuilder();
                db.execSQL(CREATE_ROOM_TABLE);
//                db.delete("wifi", "room_id==32", null);
//                db.delete("room", "id==32", null);
                Cursor c = db.rawQuery("select * from room", null);
                if(c.getCount() != 0) {
                    while (c.moveToNext()) {
                        sb.append(c.getInt(0) + "," + c.getString(1) + "\n");
                    }
                    c.close();
                    display.setText(sb.toString());
                }else{
                    display.setText("");
                }
                try{

                    File file = new File(ROOM_DB);
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
            }
        });

        Button bssid_button = (Button)findViewById(R.id.bssid_button);
        bssid_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder sb = new StringBuilder();
                db.execSQL(CREATE_BSSID_TABLE);
                Cursor c = db.rawQuery("select * from bssid", null);
                if(c.getCount() != 0) {
                    while (c.moveToNext()) {
                        sb.append(c.getInt(0) + "," + c.getString(1) + "\n");
                        //sb.append(c.getString(0) + "\n");
                    }
                    c.close();
                    display.setText(sb.toString());
                }
                try{

                    File file = new File(BSSID_DB);
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
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.db_menu, menu);
        menu.findItem(R.id.db_menu).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(show_db.this, OperateDatabase.class);
                startActivity(intent);
                return true;
            }
        });
        return true;
    }
}
