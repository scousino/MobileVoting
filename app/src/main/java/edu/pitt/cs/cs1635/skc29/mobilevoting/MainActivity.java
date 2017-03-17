package edu.pitt.cs.cs1635.skc29.mobilevoting;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Handler;
import android.provider.Telephony;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    SmsManager defaultManager;
    BroadcastReceiver mySmsReceiver;
    private boolean adminLoggedIn = false;
    private final String ADMIN_PASS_KEY = "1862";
    private final int BEGIN_VOTING = 700;
    private final int END_VOTING = 710;
    private final int ADD_ID = 500;
    private boolean startVoting = false;
    private ArrayList<Integer> demoCandidates;
    private String ADMIN_NUMBER;
    private VotingDatabase myDatabase;
    private TextView resultDisplay;
    private boolean debugging = false;
    private ExecutorService ex;
    private String VOTE_OVER_MSG = "Sorry, votes are currently not being accepted.";
    private static boolean waitingForID = false;
    private Button beginButton;
    private Button endButton;
    private Button addCandButton;
    private final int ADD_CANDIDATE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Map buttons to variables
        beginButton = (Button) findViewById(R.id.beginButton);
        endButton = (Button) findViewById(R.id.endButton);
        addCandButton = (Button) findViewById(R.id.addCandButton);

        //Set listeners for buttons
        beginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                beginVoting();
            }
        });

        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopVoting();
            }
        });

        addCandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(),ADD_CANDIDATE_REQUEST);
            }
        });


        //Debugging setup
        if(debugging) {
            startVoting = true;
            adminLoggedIn = true;
            ADMIN_NUMBER = "7246107369";
        }


        //Must request permissions from user at runtime
        permissionRequest();
        resultDisplay = (TextView) findViewById(R.id.displayResults);

        //Database setup
        myDatabase = new VotingDatabase(this);
        //TODO Fix after Milestone2
        myDatabase.clearDatabase();

        //SMS Stuff
        defaultManager = android.telephony.SmsManager.getDefault();

        //Set up Executor for processing the messages
        ex = Executors.newSingleThreadExecutor();

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

                        messageBody += messages[i].getMessageBody().toString();
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
                                if(waitingForID) {
                                    boolean success = myDatabase.addCandidate(messageValue);
                                    if(success) {
                                        sendResponseText("Candidate has been added",ADMIN_NUMBER);
                                        waitingForID = false;
                                    }else {
                                        sendResponseText("Candidate already exists",ADMIN_NUMBER);
                                    }
                                }else {
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
                                            stopVoting();
                                            break;
                                        case ADD_ID:
                                            waitingForID = true;
                                            sendResponseText("Please send the candidate ID you wish" +
                                                    "to add.",ADMIN_NUMBER);
                                            break;
                                        default:    //This is the case where the message is just a vote
                                            if(startVoting) {
                                                //Send vote to database to be verified and tallied
                                                //Functions in the runnable will verify vote validity
                                                // via phone number and candidate ID.
                                                //If valid, send vote to database. Otherwise respond
                                                //with appropriate message.
                                                ex.execute(new DatabaseWorkRunnable(phoneNumber,
                                                        messageValue,defaultManager,myDatabase));
                                            }else {
                                                sendResponseText(VOTE_OVER_MSG,phoneNumber);
                                            }
                                            break;
                                    }
                                }
                            }
                            //Number is a voter
                            else {
                                if(startVoting) {
                                    //Send vote to database to be verified and tallied
                                    //Functions in the runnable will verify vote validity
                                    // via phone number and candidate ID.
                                    //If valid, send vote to database. Otherwise respond
                                    //with appropriate message.
                                    ex.execute(new DatabaseWorkRunnable(phoneNumber,
                                            messageValue,defaultManager,myDatabase));
                                }else {
                                    sendResponseText(VOTE_OVER_MSG,phoneNumber);
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

    private void stopVoting() {
        //Stop allowing votes to be tallied and the correct response message sent
        startVoting = false;

        //Unregister SMS Receiver so we no longer get text messages
        unregisterReceiver(mySmsReceiver);

        //Display message to admin
        Toast.makeText(this,"Votes can longer be accepted",Toast.LENGTH_SHORT).show();

        //Launch ProgressDialog



        //Stop new tasks from being executed
        ex.shutdown();
        try {
            //Give all the tasks 30 seconds to complete
            if(!ex.awaitTermination(30, TimeUnit.SECONDS)) {
                //Cancel any still running tasks
                ex.shutdownNow();
            }
        }catch(InterruptedException e) {
            //If main thread was interrupted, re-cancel tasks
            ex.shutdownNow();
            //Preserve interrupt status
            Thread.currentThread().interrupt();
        }

        //Get tally results
        ArrayList<VotingDatabase.Result> results = myDatabase.getResults();
        StringBuilder sb = new StringBuilder();
        for(VotingDatabase.Result r : results) {
            sb.append(r.toString());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                sb.append(System.lineSeparator());
            }else {
                sb.append(System.getProperty("line.seperator"));
            }
        }

        //Send tally results to administrator
        sendResponseText(sb.toString(),ADMIN_NUMBER);
    }

    private void beginVoting() {
        startVoting = true;

        //Display message to admin
        Toast.makeText(this,"Votes can now be processed",Toast.LENGTH_SHORT).show();
    }

    private void permissionRequest() {
        int SMS_PERMISSIONS = 1;
        String[] PERMISSIONS = {Manifest.permission.READ_SMS,Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.SEND_SMS};
        if(!hasPermissions(PERMISSIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(PERMISSIONS,SMS_PERMISSIONS);
            }
        }

    }

    private boolean hasPermissions(String[] permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for(String permission : permissions) {
                if(ContextCompat.checkSelfPermission(this,permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == ADD_CANDIDATE_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                //Set the list of candidates received from the activity
                myDatabase.setCandidates(data.getIntegerArrayListExtra("candidates"));
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mySmsReceiver);
        myDatabase.clearDatabase();
    }

}
