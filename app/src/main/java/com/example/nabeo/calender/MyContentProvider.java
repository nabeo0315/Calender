package com.example.nabeo.calender;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * Created by nabeo on 2017/09/27.
 */

public class MyContentProvider extends ContentProvider {
    public static final String AUTHORITY = "com.example.nabeo.calendar.myprovider";
    public static final String PATH = "user";
    public static final String CONTENT_TYPE = "vnd.android.cursor.item/vnd.example.users";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.dir/vnd.example.users";
    public static final Uri CONTENT_URI = Uri.parse("content://" + MyContentProvider.AUTHORITY + "/");
    private static final int USERS = 1;
    private static final int USER_ID = 2;
    private static UriMatcher uriMatcher;
    private MySQLiteOpenHelper helper;
    static{
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, PATH, USERS);
        uriMatcher.addURI(AUTHORITY, PATH + "/#", USER_ID);
    }

    @Override
    public boolean onCreate() {
        helper = new MySQLiteOpenHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        switch (uriMatcher.match(uri)){
            case USERS:
            case USER_ID:
                queryBuilder.setTables("bssid");
                break;
            default:
                throw  new IllegalArgumentException("Unknown URI " + uri);
        }
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String insertTable;
        Uri contentUri;
        switch (uriMatcher.match(uri)){
            case USERS:
                insertTable = "bssid";
                contentUri = CONTENT_URI;
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = helper.getWritableDatabase();
        long rowID = db.insert(insertTable, null, values);
        if(rowID > 0){
            Uri returnUri = ContentUris.withAppendedId(contentUri, rowID);
            getContext().getContentResolver().notifyChange(returnUri, null);
            return returnUri;
        }else{
            throw new IllegalArgumentException("Failed to insert row into " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int count;
        String id = uri.getPathSegments().get(1);
        count = db.update("bssid", values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int count;
        switch (uriMatcher.match(uri)){
            case USERS:
            case USER_ID:
                count = db.delete("bssid", selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)){
            case USERS:
                return CONTENT_TYPE;
            case USER_ID:
                return CONTENT_ITEM_TYPE;
            default:
                throw  new IllegalArgumentException("Unknown URI " + uri);
        }
    }
}
