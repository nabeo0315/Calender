package com.example.nabeo.calender;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by nabeo on 2017/06/16.
 */

public class OperateDatabase extends AppCompatActivity {
    private SQLiteDatabase db;
    private MySQLiteOpenHelper hlpr;
    private Context context;
    private TextView db_tv;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.operate_db);
        context = this;
        db_tv = (TextView)findViewById(R.id.db_tv);

        Button exe_button = (Button)findViewById(R.id.execute_query);
        exe_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hlpr = new MySQLiteOpenHelper(context);
                db = hlpr.getWritableDatabase();
                EditText editText = (EditText)findViewById(R.id.input_query);
                String room = editText.getText().toString();
                StringBuilder sb = new StringBuilder();
                int id = -1;

                Cursor c = db.rawQuery("select id from room where name='" + room + "'", null);
                c.moveToNext();
                id = c.getInt(c.getColumnIndex("id"));

                Cursor c2 = db.rawQuery("select * from wifi where room_id=" + id, null);
                while(c2.moveToNext()){
                    sb.append(c2.getInt(0) + ", " + c2.getString(1) + ", " + c2.getInt(2) + ", " + c2.getInt(3) + ", " + c2.getString(4) + ", " + c2.getString(5) + ", " + c2.getString(6) + ", " + c2.getString(7) + "\n");
                }
                db_tv.setText(sb.toString());

                c.close();
                c2.close();
                db.close();
            }
        });

    }
}
