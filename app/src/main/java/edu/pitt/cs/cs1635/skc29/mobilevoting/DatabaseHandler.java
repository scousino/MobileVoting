package edu.pitt.cs.cs1635.skc29.mobilevoting;

/**
 * Created by Jonathan on 2/27/2017.
 */

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.content.Context;
import android.content.ContentValues;

public class DatabaseHandler extend SQLiteOpenHelper
{
    private static final int DB_VERSION = 1; //Version of the DB
    private static final String DB_NAME = "MobilePhone.db"; //Name of the Database
    public static final String TABLE_NUMBERS = "PhoneNumbers"; //Name of Our Phone Number Database
    public static final String TABLE_VOTES = "Tally"; //Name of Our Tally Database
    public static final String COL_NUMBER = "Phone"; //Name of Column For Phone Numbers
    public static final String COL_CANDIDATE = "Candidate"; //Name of Column For Candidates
    public static final String COL_TALLY = "NumberVotes"; //Name of Column For Tally

    public DatabaseHandler(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) //Constructor
    {
        super(context, DB_NAME, factory, DB_VERSION);
    }

    public void onCreate(SQLiteDatabase db)
    {
        String query = "CREATE TABLE " + TABLE_NUMBERS + "(" +
                COL_NUMBER + " TEXT PRIMARY KEY " +
                ");";
        db.execSQL(query); //Execute Query

        String query = "CREATE TABLE " + TABLE_VOTES + "(" +
                COL_CANDIDATE + " INTEGER PRIMARY KEY " +
                COL_TALLY + " INTEGER " +
                ");";
        db.execSQL(query); //Execute Query
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NUMBERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VOTES);
        onCreate(db);
    }

    public void displayResults()
    {
        Cursor cursor = db.rawQuery("SELECT * FROM TABLE_VOTES ORDER BY COL_TALLY DESC"); //Get Results
        cursor.moveToFirst();
        int position = 1;
        do{
            System.out.println("Rank " + position + ":  Candidate #" + cursor.getString("COL_CANDIDATE") + " - Votes: " + cursor.getInt("COL_TALLY"));
            position++;
        }while(cursor.moveToNext());

    }

    public boolean updateDatabases(String phoneNumber, int candidateID)
    {
        Cursor cursor = db.rawQuery("SELECT COL_NUMBER FROM TABLE_NUMBERS WHERE COL_NUMBER = " + phoneNumber, null); //Query to See If Number Exists

        if(cursor.moveToFirst()) //Already Exists - Can't Update DB
        {
            return false;
        }

        else //Number Not In, Add To Database
        {
            if(db.insert(TABLE_NUMBERS, null, phoneNumber) == -1) //Insert Into DB
            {
                return false;
            }

            else //Insert Successful, Update Tally
            {
                cursor = db.rawQuery("SELECT * FROM TABLE_VOTES WHERE COL_CANDIDATE = " + candidateID, null);
                if(!cursor.moveToFirst()) //Not in the table yet, add
                {
                    db.insert(TABLE_VOTES, null, candidateID, 0); //Add This Row To Table
                }

                cursor = db.rawQuery("SELECT COL_TALLY FROM TABLE_VOTES WHERE COL_TALLY = " + candidateID, null);
                int currTally = (cursor.moveToFirst()).getInt("COL_TALLY"); //Get Current Tally
                currTally++; //Increment Current Tally

                ContentValues cv = new ContentValues();
                cv.put("COL_TALLY", currTally);
                if(db.update(TABLE_VOTES, cv, "COL_CANDIDATE = " + candidateID, null) == 0)
                    return false;

            }
        }

        return true;
    }

}