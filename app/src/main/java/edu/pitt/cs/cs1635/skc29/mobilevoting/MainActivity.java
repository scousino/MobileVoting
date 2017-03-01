package edu.pitt.cs.cs1635.skc29.mobilevoting;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Handler;
import android.provider.Telephony;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {
    SmsManager defaultManager;
    BroadcastReceiver mySmsReceiver;
    private boolean adminLoggedIn = false;
    private final String ADMIN_PASS_KEY = "1862";
    private final int BEGIN_VOTING = 700;
    private final int END_VOTING = 710;
    private boolean startVoting = false;
    private ArrayList<Integer> demoCandidates;
    private String ADMIN_NUMBER;
    private SQLiteDatabase database;
    private SerialMessageExecutor voteExecutor;
    private VotingDatabase myDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Database setup
        myDatabase = new VotingDatabase(this,demoCandidates);

        //SMS Stuff
        defaultManager = android.telephony.SmsManager.getDefault();
        //Set up Executor for processing the messages
        voteExecutor = new SerialMessageExecutor(new Executor() {
            @Override
            public void execute(Runnable command) {
                new Thread(command).start();
            }
        });
        //Create a receiver to handle incoming SMS messages
        mySmsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle myBundle = intent.getExtras();
                SmsMessage[] messages = null;
                String format = "";
                String messageBody = "";
                String phoneNumber = "";
                if(myBundle != null) {
                    Object[] pdus = (Object[]) myBundle.get("pdus");
                    messages = new SmsMessage[pdus.length];

                    for(int i = 0; i < messages.length; i++) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            format = myBundle.getString("format");
                            messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                        } else {
                            messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        }
                        //TODO check if this should be messageBody +=
                        messageBody = messages[i].getMessageBody().toString();
                        phoneNumber = messages[i].getOriginatingAddress().toString();
                    }

                    //Launch uploader or send info in Intent, or spawn a background service

                    //Delete this after Milestone 2
                    if(!adminLoggedIn) {
                        //Validate message contents
                        if(messageBody.equalsIgnoreCase("Admin "+ADMIN_PASS_KEY)) {
                            adminLoggedIn = true;
                            ADMIN_NUMBER = phoneNumber;
                            sendResponseText("Admin login accepted.",phoneNumber);
                        }else {
                            sendResponseText("Invalid admin login. Please try again.",phoneNumber);
                        }
                    }else {
                        //Process Regular Message
                        if(validateMessage(messageBody)) {
                            int messageValue = Integer.parseInt(messageBody);
                            if(phoneNumber.equalsIgnoreCase(ADMIN_NUMBER)) {
                                switch (messageValue) {
                                    case BEGIN_VOTING:
                                        startVoting = true;
                                        //Create database? Or should I do it in onCreate.
                                        sendResponseText("Votes can now be processed",ADMIN_NUMBER);
                                        break;
                                    case END_VOTING:
                                        startVoting = false;
                                        sendResponseText("Votes can no longer be accepted.",
                                                phoneNumber);
                                        break;
                                    default:    //This is the case where the message is just a vote
                                        if(startVoting) {
                                            //Send vote to database to be verified and tallied
                                            //Functions in the runnable will verify vote validity
                                            // via phone number and candidate ID.
                                            //If valid, send vote to database. Otherwise respond
                                            //with appropriate message.
                                            voteExecutor.execute(new DatabaseWorkRunnable(phoneNumber,
                                                    messageValue,defaultManager,myDatabase));
                                        }
                                        break;
                                }
                            }
                        }else {
                            sendResponseText("Invalid vote. Text must contain only numbers.",
                                    phoneNumber);
                        }
                    }
                }
            }
            //End of onReceive Implementation
        };
        //End of BroadcastReceiver Implementation
        //Register the receiver for SMS
        registerReceiver(mySmsReceiver, new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));

    }

    //Sets up a list of dummy Candidate IDs for the database to initialize.
    private void demoMode() {
        demoCandidates = new ArrayList<>();
        demoCandidates.add(1);
        demoCandidates.add(2);
        demoCandidates.add(3);
    }

    private void sendResponseText(String message, String phoneNumber) {
        //TODO set phoneNumber variable
        defaultManager.sendTextMessage(phoneNumber, null, message, null, null);
    }

    private boolean validateMessage(String messageBody) {
        return android.text.TextUtils.isDigitsOnly(messageBody);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mySmsReceiver);
    }

}
