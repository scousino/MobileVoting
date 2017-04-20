package edu.pitt.cs.cs1635.skc29.mobilevoting;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Telephony;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    SmsManager defaultManager;
    BroadcastReceiver mySmsReceiver;
    private final String ADMIN_PASS_KEY = "1862";
    private boolean startVoting = false;
    private ArrayList<Integer> demoCandidates;
    private VotingDatabase myDatabase;
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
    private LinearLayout myResultLayout;
    private boolean receiverRegistered = false;
    private boolean executorAlive = true;
    private int numTestRun = 0;

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
                ArrayList<Integer> dbCandidates = myDatabase.getCandidatesCopy();
                Intent i = new Intent(MainActivity.this,AddCandidates.class);
                i.putIntegerArrayListExtra("dbCandidates",dbCandidates);
                startActivityForResult(i,ADD_CANDIDATE_REQUEST);
            }
        });


        //Debugging setup
        if(debugging) {
            startVoting = true;
        }


        //Must request permissions from user at runtime
        permissionRequest();

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
        receiverRegistered = true;
    }

    private void stopVoting() {
        //Stop allowing votes to be tallied and the correct response message sent
        startVoting = false;

        //Unregister SMS Receiver so we no longer get text messages
        if(receiverRegistered) {
            unregisterReceiver(mySmsReceiver);
            receiverRegistered = false;
        }

        //Display message to admin
        Toast.makeText(this,VOTE_END_MSG_ADMIN,Toast.LENGTH_SHORT).show();

        //Create and Launch ProgressDialog
        ProgressDialog progress;
        progress = new ProgressDialog(this);
        progress.setCancelable(false);
        progress.setMessage("Tallying the final results");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setProgress(0);
        progress.show();

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


        //Close progress dialog
        progress.dismiss();

        //Compile tally results
        compileResults();

        //Prompt for database destruction
        databaseDestructPrompt();

        //Display results
        myResultLayout.setVisibility(View.VISIBLE);

        //Disable/Enable appropriate buttons
        addCandButton.setEnabled(true);
        endButton.setEnabled(false);

        //Executor dead
        executorAlive = false;
    }

    private void databaseDestructPrompt() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int buttonClicked) {
                switch (buttonClicked){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        //Clear database
                        myDatabase.clearDatabase();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        //Do nothing
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Would you like to destroy the database and reset the list of candidates?");
        builder.setPositiveButton("Yes", dialogClickListener);
        builder.setNegativeButton("No", dialogClickListener).show();
    }

    private void compileResults() {
        //Init layout variable to add TextViews containing the final results
        myResultLayout = (LinearLayout) findViewById(R.id.myResultLayout);

        //Retrieve the final results
        ArrayList<VotingDatabase.Result> resList = myDatabase.getResults();

        for(VotingDatabase.Result x : resList) {
            //Create a TextView variable to store result info and store in myLayout
            TextView results = new TextView(myResultLayout.getContext());
            results.setText(x.toString());
            results.setTextSize(TypedValue.COMPLEX_UNIT_SP,20);
            myResultLayout.addView(results);
        }

    }

    private void beginVoting() {
        //Ensure at least one candidate ID has been added
        if(myDatabase.isCandidateEmpty()) {
            Toast.makeText(this,NO_CANDIDATES_MSG_ADMIN,Toast.LENGTH_SHORT).show();
        }else {
            //Prompt for admin passkey
            authenticateAdminAction();
            if(!executorAlive) {
                ex = Executors.newSingleThreadExecutor();
            }
            if(!receiverRegistered) {
                registerReceiver(mySmsReceiver, new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));
            }
        }

    }

    //Displays the dialog box which prompts for an administrator passkey
    private void authenticateAdminAction() {
        //Create the login dialog box
        final Dialog login = new Dialog(MainActivity.this);
        login.setContentView(R.layout.dialog_admin_passkey);
        login.setTitle("Administrator Authentication");
        login.setCancelable(false);

        //Initialize the variables for the GUI

        Button loginBtn = (Button) login.findViewById(R.id.loginButton);
        Button cancelBtn = (Button) login.findViewById(R.id.cancelButton);
        final EditText passkeyInput = (EditText) login.findViewById(R.id.passkeyInput);

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
                        //Disable/Enable appropriate buttons
                        addCandButton.setEnabled(false);    //Candidates cannot be added once voting has begun
                        beginButton.setEnabled(false);
                        endButton.setEnabled(true);
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
                if(!myDatabase.isCandidateEmpty()) {
                    beginButton.setEnabled(true);
                    endButton.setEnabled(false);
                }

            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mySmsReceiver);
        myDatabase.clearDatabase();
    }

    //Method for Testing Code
    private void testSimulator()
    {
        numTestRun++;

        //Init executor for testing
        ex = Executors.newSingleThreadExecutor();

        InputStream stream = getResources().openRawResource(R.raw.testfile);
        BufferedReader fileReader = new BufferedReader(new InputStreamReader(stream));
        try
        {
            if(!fileReader.readLine().equals("ADD"))
            {
                Toast.makeText(this, "FILE FORMAT ERROR - NO ADD COMMAND", Toast.LENGTH_LONG).show();
            }
            else
            {
                ArrayList<Integer> candidates = new ArrayList<Integer>();
                String nextID;
                nextID = fileReader.readLine();
                while(!nextID.equals("BEGIN")) //Adds Candidate IDs
                {
                    if(!candidates.contains((Integer.parseInt(nextID))))
                    {
                        candidates.add(Integer.parseInt(nextID));
                    }
                    else {
                        Toast.makeText(this,"ERROR - DUPLICATE CANDIDATE ID", Toast.LENGTH_LONG).show();
                        //Close input streams and readers
                        fileReader.close();
                        stream.close();
                        return;
                    }

                    nextID = fileReader.readLine();
                }

                myDatabase.setCandidates(candidates); //Add Candidates Officially

                ArrayList<Integer> votes = new ArrayList<Integer>();
                ArrayList<String> phoneNumbers = new ArrayList<String>();

                String nextVote, nextPhone;
                nextPhone = fileReader.readLine();
                nextVote = fileReader.readLine();

                while(!nextPhone.equals("END")) //Adds Candidate IDs
                {
                    if(!phoneNumbers.contains(nextPhone) && candidates.contains(Integer.parseInt(nextVote)))
                    {
                        phoneNumbers.add(nextPhone);
                        votes.add(Integer.parseInt(nextVote));
                    }
                    else
                    {
                        Toast.makeText(this,"VOTING ERROR", Toast.LENGTH_LONG).show();
                        //Close input streams and readers
                        fileReader.close();
                        stream.close();
                        return;
                    }

                    nextPhone = fileReader.readLine();
                    nextVote = fileReader.readLine();
                }

                for(int i = 0; i < phoneNumbers.size(); i++)
                {
                    ex.execute(new DatabaseWorkRunnable(phoneNumbers.get(i), votes.get(i),defaultManager,myDatabase,true));
                }

                stopVoting();
                Toast.makeText(this,"CODE TEST SUCCESSFUL", Toast.LENGTH_LONG).show();

                //Close input streams and readers
                fileReader.close();
                stream.close();


            }
        }

        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_test:
                testSimulator();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

}
