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
    private String INVALID_VOTER = "Sorry, you have already voted today! You can only vote once.";
    private String INVALID_CANDIDATE = "You have entered an ID that does not exist. Please try again";
    private String VALID_VOTE = "Your vote has been accepted!";

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
        if(!database.checkVoter(pNumber)) {
            //Send invalid voter response message
            textMessageManager.sendTextMessage(pNumber,null,INVALID_VOTER,null,null);
        }else {
            //Check if candidate exists
            if(!database.checkCandidate(candidate)) {
                //Send invalid candidate response message
                textMessageManager.sendTextMessage(pNumber,null,INVALID_CANDIDATE,null,null);
            }else {
                //Passed both voter and candidate checks. Insert vote into database.
                database.addVote(pNumber,candidate);
                //Send acknowledgment response
                textMessageManager.sendTextMessage(pNumber,null,VALID_VOTE,null,null);
            }
        }

    }

}
