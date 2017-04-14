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
    private boolean testFlag;

    //Include db as third parameter in constructor
    DatabaseWorkRunnable(String number, int id, SmsManager txtManager, VotingDatabase db, boolean test) {
        pNumber = number;
        candidate = id;
        textMessageManager = txtManager;
        database = db;
        testFlag = test;
    }

    DatabaseWorkRunnable(String number, int id, SmsManager txtManager, VotingDatabase db) {
        this(number,id,txtManager,db,false);
    }

    @Override
    public void run() {
        //Check if voter has already voted
        if(database.checkVoter(pNumber)) {
            //Send invalid voter response message
            if(!testFlag) {
                textMessageManager.sendTextMessage(pNumber,null,INVALID_VOTER,null,null);
            }
        }else {
            //Check if candidate exists
            if(!database.checkCandidate(candidate)) {
                //Send invalid candidate response message
                if(!testFlag) {
                    textMessageManager.sendTextMessage(pNumber,null,INVALID_CANDIDATE,null,null);
                }
            }else {
                //Passed both voter and candidate checks. Insert vote into database.
                boolean result = database.addVote(pNumber,candidate);
                //Send acknowledgment response
                if(!testFlag) {
                    textMessageManager.sendTextMessage(pNumber,null,VALID_VOTE,null,null);
                }
            }
        }

    }

}
