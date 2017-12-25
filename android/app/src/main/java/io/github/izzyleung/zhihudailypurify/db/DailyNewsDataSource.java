package io.github.izzyleung.zhihudailypurify.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Optional;

import io.github.izzyleung.ZhihuDailyPurify;

public final class DailyNewsDataSource {
    private SQLiteDatabase database;
    private DBHelper dbHelper;
    private String[] allColumns = {
            DBHelper.COLUMN_ID,
            DBHelper.COLUMN_DATE,
            DBHelper.COLUMN_FEED
    };

    public DailyNewsDataSource(Context context) {
        dbHelper = new DBHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    private void insertFeed(String date, ZhihuDailyPurify.Feed feed) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_DATE, date);
        values.put(DBHelper.COLUMN_FEED, feed.toByteArray());

        database.insert(DBHelper.TABLE_NAME, null, values);
    }

    private void updateNewsList(String date, ZhihuDailyPurify.Feed feed) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_DATE, date);
        values.put(DBHelper.COLUMN_FEED, feed.toByteArray());
        database.update(DBHelper.TABLE_NAME, values, DBHelper.COLUMN_DATE + "=" + date, null);
    }

    public void insertOrUpdateFeed(String date, ZhihuDailyPurify.Feed feed) {
        if (feedForDate(date).isInitialized()) {
            updateNewsList(date, feed);
        } else {
            insertFeed(date, feed);
        }
    }

    public ZhihuDailyPurify.Feed feedForDate(String date) {
        Cursor cursor = database.query(DBHelper.TABLE_NAME,
                allColumns, DBHelper.COLUMN_DATE + " = " + date, null, null, null, null);

        cursor.moveToFirst();

        ZhihuDailyPurify.Feed result = cursorToFeed(cursor);

        cursor.close();

        return result;
    }

    private ZhihuDailyPurify.Feed cursorToFeed(Cursor cursor) {
        return Optional.ofNullable(cursor)
                .filter(c -> c.getCount() > 0)
                .map(c -> feedFromByteArray(c.getBlob(2)))
                .orElse(ZhihuDailyPurify.Feed.getDefaultInstance());
    }

    private ZhihuDailyPurify.Feed feedFromByteArray(byte[] bytes) {
        try {
            return ZhihuDailyPurify.Feed.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            return ZhihuDailyPurify.Feed.getDefaultInstance();
        }
    }
}
