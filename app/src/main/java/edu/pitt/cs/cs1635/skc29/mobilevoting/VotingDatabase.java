package edu.pitt.cs.cs1635.skc29.mobilevoting;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by Spencer Cousino on 2/28/2017.
 */

public class VotingDatabase {
    private DatabaseHandler dbHandler;
    private static SQLiteDatabase database;
    private static final String DB_NAME = "MobilePhone.db"; //Name of the Database
    public static final String TABLE_NAME = "VoterDatabase"; //Name of Our Phone Number Database
    public static final String COL_NUMBER = "Phone"; //Name of Column For Phone Numbers
    public static final String COL_CANDIDATE = "Candidate"; //Name of Column For Candidates
    private final ArrayList<Integer> candidates;

    public VotingDatabase(Context context, ArrayList<Integer> ids) {
        dbHandler = DatabaseHandler.getInstance(context);
        database = dbHandler.getWritableDatabase();
        candidates = ids;
    }

    public boolean checkVoter(String phoneNumber){ //Checks to see if voter has voted before
        String[] cols = new String[]{COL_NUMBER};
        String[] whereArgs = new String[]{phoneNumber};
        Cursor cursor = database.query(true, TABLE_NAME, cols, COL_NUMBER+"=?", whereArgs, null, null, null, null);
        if(cursor.moveToFirst()) //Exists, Return True
        {
            return true;
        }

        return false; //Doesn't Exist, Return False
    }

    //If the candidate exists, the method returns true.
    public boolean checkCandidate(int candidate) //Checks to see if candidate is valid
    {
        return candidates.contains(candidate);
    }

    public boolean addVote(String phoneNumber, int candidateID) {
        if(!checkVoter(phoneNumber)) //Voter Hasn't Voted Yet
        {
            ContentValues cv = new ContentValues();
            cv.put(COL_NUMBER, phoneNumber);
            cv.put(COL_CANDIDATE, candidateID);
            if(database.insert(TABLE_NAME, null, cv) != -1)
            {
                return true;
            }
        }

        return false;
    }

    public ArrayList<Result> getResults() {
        ArrayList<Result> result = new ArrayList<Result>();
        Cursor cursor = database.rawQuery("SELECT " + COL_CANDIDATE + ", COUNT(" + COL_CANDIDATE + ") AS VOTES FROM "
                + TABLE_NAME + " GROUP BY COL_CANDIDATE ORDER BY VOTES DESC", null);
        boolean moreRows = cursor.moveToFirst();

        while(moreRows)
        {
            result.add(new Result(cursor.getInt(0), cursor.getInt(1)));
            moreRows = cursor.moveToNext();
        }

        return result;
    }

    public void clearDatabase() {
        dbHandler.onUpgrade(database,1,2);
    }

    private class Result {
        int candidate;
        int votes;

        public Result(int c, int v)
        {
            c = candidate;
            v = votes;
        }

        public String toString()
        {
            return "Candidate: " + candidate + " --- Votes: " + votes;
        }
    }
}
