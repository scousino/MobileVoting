package edu.pitt.cs.cs1635.skc29.mobilevoting;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;

public class AddCandidates extends AppCompatActivity {

    ArrayList<Integer> candidates = new ArrayList<Integer>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_candidates);

        Intent startingIntent = getIntent();


        Button addButton, doneButton;

        addButton = (Button)findViewById(R.id.addButton);
        doneButton = (Button)findViewById(R.id.doneButton);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int newID;
                newID = Integer.parseInt(((EditText)findViewById(R.id.id_field)).getText().toString());
                if(!checkCandidates(newID))
                {
                    candidates.add(newID); //Add New ID
                }
            }
        });

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
