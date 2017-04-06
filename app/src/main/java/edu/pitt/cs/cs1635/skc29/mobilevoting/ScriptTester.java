package edu.pitt.cs.cs1635.skc29.mobilevoting;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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

    private void runCodeSimulation(String code) //Function To Test Code
    {

    }
}
