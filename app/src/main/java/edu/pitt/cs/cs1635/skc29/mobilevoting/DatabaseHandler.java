package edu.pitt.cs.cs1635.skc29.mobilevoting;

/**
 * Created by Jonathan on 2/27/2017.
 */

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;

    public class DatabaseHandler extends SQLiteOpenHelper
    {
        private static DatabaseHandler sInstance;

        private static final int DB_VERSION = 1; //Version of the DB
        private static final String DB_NAME = "MobilePhone.db"; //Name of the Database
        public static final String TABLE_NAME = "VoterDatabase"; //Name of Our Phone Number Database
        public static final String COL_NUMBER = "Phone"; //Name of Column For Phone Numbers
        public static final String COL_CANDIDATE = "Candidate"; //Name of Column For Candidates


        public static synchronized DatabaseHandler getInstance(Context context) {

            // Use the application context, which will ensure that you
            // don't accidentally leak an Activity's context.
            if (sInstance == null) {
                sInstance = new DatabaseHandler(context.getApplicationContext());
            }
            return sInstance;
        }

        private DatabaseHandler(Context context) //Constructor
        {
            super(context, DB_NAME, null, DB_VERSION);
        }

        public void onCreate(SQLiteDatabase db)
        {
            String query = "CREATE TABLE " + TABLE_NAME + "(" +
                    COL_NUMBER + " TEXT PRIMARY KEY " +
                    COL_CANDIDATE + " INTEGER " +
                    ");";
            db.execSQL(query); //Execute Query
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }