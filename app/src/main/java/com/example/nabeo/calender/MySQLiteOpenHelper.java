package com.example.nabeo.calender;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by nabeo on 2017/04/23.
 */

public class MySQLiteOpenHelper extends SQLiteOpenHelper{
    static String DBNAME = "wifi.db";
    static int DBVERSION = 1;
    final static String CREATE_WIFI_TABLE = "create table if not exists wifi(id integer primary key autoincrement, timestamp text, room_id integer, bssid_id integer, count integer, ssid text, rssi integer, state text)";
    final static String DROP_WIFI_TABLE = "drop table if exists wifi";
    final static String CREATE_ROOM_TABLE = "create table if not exists room(id integer primary key autoincrement, name text unique)";
    final static String DROP_ROOM_TABLE = "drop table if exists room";
    final static String CREATE_BSSID_TABLE = "create table if not exists bssid(id integer primary key autoincrement, mac text unique)";
    final static String DROP_BSSID_TABLE = "drop table if exists bssid";
    final static String CREATE_PREDICT_TABLE = "create table if not exists predict(bssid_id integer, rssi integer, count integer)";
    final static String DROP_PREDICT_TABLE = "drop table if exists predict";
    final static String CREATE_CLASS_TABLE = "create table if not exists class(class_id integer primary key autoincrement, class_name text, starttime text unique, endtime text unique, place text)";
    final static String DROP_CLASS_TABLE = "drop table if exists class";
    final static String CREATE_ATTENDANCE_TABLE = "create table if not exists attendance(id integer primary key autoincrement, class text, date text unique, room text)";
    final static String DROP_ATTENDANCE_TABLE = "drop table if exists attendance";

    public MySQLiteOpenHelper(Context context){
        super(context, DBNAME, null, DBVERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqliteDatabase){

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int a, int b){

    }
}
