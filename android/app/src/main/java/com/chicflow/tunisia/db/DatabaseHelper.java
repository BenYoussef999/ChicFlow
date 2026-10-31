package com.chicflow.tunisia.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * The phone's SQLite database ("chicflow.db").
 * Tables are created with "IF NOT EXISTS", so onCreate and onUpgrade can share the same code:
 * to add a table in a later version, add it to createTables() and raise DB_VERSION.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "chicflow.db";
    private static final int DB_VERSION = 1;

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        createTables(db);       // only creates what is missing, existing data is kept
    }

    private void createTables(SQLiteDatabase db) {
        db.execSQL(ItemSql.CREATE_TABLE);
        db.execSQL(ItemSql.CREATE_INDEX_UUID);
        db.execSQL(ItemSql.CREATE_INDEX_REMOTE);
    }
}
