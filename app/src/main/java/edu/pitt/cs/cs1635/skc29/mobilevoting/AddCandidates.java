package edu.pitt.cs.cs1635.skc29.mobilevoting;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

public class AddCandidates extends AppCompatActivity {

    ArrayList<Integer> candidates = new ArrayList<Integer>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_candidates);



        Button addButton, doneButton; //Buttons In View

        addButton = (Button)findViewById(R.id.addButton);
        doneButton = (Button)findViewById(R.id.doneButton);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int newID;
                TextView message = (TextView)findViewById(R.id.addScreen_message);
                EditText enterField = (EditText)findViewById(R.id.id_field);
                message.setText("");
                newID = Integer.parseInt(enterField.getText().toString()); //Get Value

                if(!checkCandidates(newID))
                {
                    candidates.add(newID); //Add New ID
                }

                else
                {
                    message.setText("ID Already Exists In Candidate List!");
                }

            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent returnIntent = new Intent(); //Intent That Will Be Returned
                returnIntent.putIntegerArrayListExtra("candidates", candidates);
                setResult(RESULT_OK, returnIntent);
                finish();
            }
        });
    }

    private boolean checkCandidates(int newID)
    {
        if(candidates.contains((Integer)newID))
        {
            return true;
        }

        return false;
    }
}
