package edu.pitt.cs.cs1635.skc29.mobilevoting;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.text.NumberFormat;
import java.util.StringTokenizer;

public class ScriptTester extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_tester);
        Button enterButton = (Button)findViewById(R.id.testButton);
        enterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText testCode = (EditText)findViewById(R.id.testCode);
                String code = testCode.getText().toString();
                runCodeSimulation(code);
            }
        });
    }

    private boolean runCodeSimulation(String code) //Function To Test Code
    {
        StringTokenizer stringTokenizer = new StringTokenizer(code, "\n", false);
        AddCandidates addCandidates = new AddCandidates(); //AddCandidates Class
        if(stringTokenizer.nextToken().equalsIgnoreCase("ADD")) //Add Candidates
        {
            String nextToken = stringTokenizer.nextToken();
            try { //Inserts all the candidates
                while (true) {
                    if (!addCandidates.checkCandidates(Integer.parseInt(nextToken))) {
                        addCandidates.candidates.add(Integer.parseInt(nextToken));
                    } else
                        return false;
                }
            }
            catch (NumberFormatException e) //Number is a Word
            {
                if(!nextToken.equalsIgnoreCase("BEGIN"))
                    return false;
            }

            if(addCandidates.candidates.isEmpty()) //Nothing In List, No Candidates To Vote For
                    return false;

            Button beginButton = (Button)findViewById(R.id.beginButton);
            beginButton.performClick(); //Click Begin Button

            MainActivity main = new MainActivity();
            String phoneNum, voteID;
            while(true)
            {
                phoneNum = stringTokenizer.nextToken();
                voteID = stringTokenizer.nextToken();

            }





                //Begin Voting
        }

        return false;
    }
}
