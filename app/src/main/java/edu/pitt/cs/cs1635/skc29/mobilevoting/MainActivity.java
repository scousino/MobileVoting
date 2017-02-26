package edu.pitt.cs.cs1635.skc29.mobilevoting;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.provider.Telephony;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

public class MainActivity extends AppCompatActivity {
    SmsManager defaultManager;
    BroadcastReceiver mySmsReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        defaultManager = android.telephony.SmsManager.getDefault();
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

                }
            }
        };

        //Register the receiver for SMS
        registerReceiver(mySmsReceiver, new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));
    }

    private void sendResponseText(String message) {
        //TODO set phoneNumber variable
        String phoneNumber = "";
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
