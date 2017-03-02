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
    private static ArrayList<Integer> candidates;
    private static ArrayList<Integer> noVotes;

    public VotingDatabase(Context context, ArrayList<Integer> ids) {
        dbHandler = DatabaseHandler.getInstance(context);
        database = dbHandler.getWritableDatabase();
        candidates = ids;
        noVotes = new ArrayList<>(ids);
    }

    //Returns true on success, and false if candidate already exists.
    public boolean addCandidate(int candidate) {
        if(candidates.contains(candidate)) {
            return false;
        }
        candidates.add(candidate);
        noVotes.add(candidate);
        return true;
    }

    public boolean checkVoter(String phoneNumber){ //Checks to see if voter has voted before
        String[] cols = new String[]{COL_NUMBER};
        String[] whereArgs = new String[]{phoneNumber};
        Cursor cursor = database.query(true, TABLE_NAME, cols, COL_NUMBER+"=?", whereArgs, null, null, null, null);
        if(cursor.moveToFirst()) //Exists, Return True
        {
            cursor.close();
            return true;
        }
        cursor.close();
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
            database.execSQL("INSERT INTO "+TABLE_NAME+"("+COL_CANDIDATE+","+COL_NUMBER+") VALUES("
                        +candidateID+","+phoneNumber+")");
            noVotes.remove(noVotes.indexOf(candidateID));

        }

        return false;
    }
    public void sqlTest() {
        Cursor c = database.rawQuery("SELECT * FROM VoterDatabase",null);
        boolean moreRows = c.moveToFirst();
        int x = c.getInt(c.getColumnIndex("Candidate"));
        x = 5 +5;
        c.close();
    }

    public ArrayList<Result> getResults() {
        ArrayList<Result> result = new ArrayList<Result>();
        String countFunction = "COUNT("+COL_CANDIDATE+")";
        String[] resultColumns = {COL_CANDIDATE,countFunction};
        Cursor c = database.query(TABLE_NAME,resultColumns,null,null,COL_CANDIDATE,null,countFunction+"DESC");
        boolean moreRows = c.moveToFirst();

        while(moreRows) {
            int ID = c.getInt(c.getColumnIndex(COL_CANDIDATE));
            //We minus 1 from the votes to account for the initial rows in the table.
            //These initial rows allow us to display candidates who received no votes easily.
            int votes = c.getInt(c.getColumnIndex(countFunction));
            result.add(new Result(ID,votes));
            moreRows = c.moveToNext();
        }
        c.close();

        //Adds candidates that received no votes to the result
        for(int x : noVotes) {
            result.add(new Result(x,0));
        }
        return result;
    }

    public void clearDatabase() {
        dbHandler.onUpgrade(database,1,2);
    }

    protected class Result {
        int candidate;
        int votes;

        public Result(int c, int v)
        {
            candidate = c;
            votes = v;
        }

        public String toString()
        {
            return "Candidate: " + candidate + " --- Votes: " + votes;
        }
    }
}
