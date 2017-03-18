package edu.pitt.cs.cs1635.skc29.mobilevoting;

import android.Manifest;
import android.app.Dialog;
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
import android.widget.EditText;
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
    private boolean startVoting = false;
    private ArrayList<Integer> demoCandidates;
    private String ADMIN_NUMBER;
    private VotingDatabase myDatabase;
    private TextView resultDisplay;
    private boolean debugging = false;
    private ExecutorService ex;
    private String VOTE_OVER_MSG_USER = "Sorry, votes are currently not being accepted.";
    private String VOTE_START_MSG_ADMIN = "Votes can now be processed";
    private String VOTE_END_MSG_ADMIN = "Votes can no longer be processed";
    private final String NO_CANDIDATES_MSG_ADMIN = "Voting cannot begin until you enter at least one" +
            " candidate ID";
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
                startActivityForResult(new Intent(MainActivity.this,AddCandidates.class),ADD_CANDIDATE_REQUEST);
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

                    //Process Message
                    if(validateMessage(messageBody)) {
                        int messageValue = Integer.parseInt(messageBody);
                        //Number is a voter
                        if(startVoting) {
                            //Send vote to database to be verified and tallied
                            //Functions in the runnable will verify vote validity
                            // via phone number and candidate ID.
                            //If valid, send vote to database. Otherwise respond
                            //with appropriate message.
                            ex.execute(new DatabaseWorkRunnable(phoneNumber,
                                    messageValue,defaultManager,myDatabase));
                        }else {
                            sendResponseText(VOTE_OVER_MSG_USER,phoneNumber);
                        }
                    }else {
                        sendResponseText("Invalid vote. Text must contain only numbers.",
                                phoneNumber);
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
        Toast.makeText(this,VOTE_END_MSG_ADMIN,Toast.LENGTH_SHORT).show();

        //TODO Launch ProgressDialog



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

        //TODO adapt for GUI interface
        //Send tally results to administrator
        sendResponseText(sb.toString(),ADMIN_NUMBER);

        //TODO prompt for database destruction
    }

    private void beginVoting() {
        //Ensure at least one candidate ID has been added
        if(myDatabase.isCandidateEmpty()) {
            Toast.makeText(this,NO_CANDIDATES_MSG_ADMIN,Toast.LENGTH_SHORT).show();
        }else {
            //Prompt for admin passkey
            authenticateAdminAction();
        }

    }

    //Displays the dialog box which prompts for an administrator passkey
    private void authenticateAdminAction() {
        //Create the login dialog box
        final Dialog login = new Dialog(this);
        login.setContentView(R.layout.dialog_admin_passkey);
        login.setTitle("Administrator Authentication");

        //Initialize the variables for the GUI
        Button loginBtn = (Button) findViewById(R.id.loginButton);
        Button cancelBtn = (Button) findViewById(R.id.cancelButton);
        final EditText passkeyInput = (EditText) findViewById(R.id.passkeyInput);

        //Set onClickListeneres for both buttons
        //loginBtn
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredKey = passkeyInput.getText().toString().trim();
                //Validate the string actually contains input
                if(enteredKey.length() > 0) {
                    if(ADMIN_PASS_KEY.equalsIgnoreCase(enteredKey)) {
                        //Allow votes to be processed
                        startVoting = true;
                        //Show feedback to user
                        Toast.makeText(MainActivity.this,VOTE_START_MSG_ADMIN,Toast.LENGTH_SHORT).show();
                        login.dismiss();
                    }else {
                        //Show feedback to user
                        Toast.makeText(MainActivity.this,"Invalid passkey",Toast.LENGTH_SHORT).show();
                        login.dismiss();
                    }
                }
            }
        });

        //cancelBtn
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Show feedback to user
                Toast.makeText(MainActivity.this,"Login cancelled",Toast.LENGTH_SHORT).show();
                login.dismiss();
            }
        });

        login.show();
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
