package edu.pitt.cs.cs1635.skc29.mobilevoting;

import android.database.sqlite.SQLiteDatabase;
import android.telephony.SmsManager;

import java.lang.Runnable;

/**
 * Created by Spencer Cousino on 2/28/2017.
 */

class DatabaseWorkRunnable implements Runnable {
    private String pNumber;
    private int candidate;
    SmsManager textMessageManager;
    private VotingDatabase database;

    //Include db as third parameter in constructor
    DatabaseWorkRunnable(String number, int id, SmsManager txtManager, VotingDatabase db) {
        pNumber = number;
        candidate = id;
        textMessageManager = txtManager;
        database = db;
    }

    @Override
    public void run() {
        //Check if voter has already voted


        //Check if valid candidate ID


        //If here, then insert into db.
    }

}
