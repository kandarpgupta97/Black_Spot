package com.example.kd.blackspot;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class Signin extends AppCompatActivity {

    EditText p_number;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        p_number= findViewById(R.id.phone_number);
        findViewById(R.id.done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String num = p_number.getText().toString().trim();
                if(num.isEmpty() || num.length()<10)
                {
                    p_number.setError("Enter a valid mobile number");
                    p_number.requestFocus();
                    return;
                }

                Intent intent = new Intent(Signin.this, OTP_Verification.class);
                intent.putExtra("number", num);
                startActivity(intent);
            }
        });
    }
}
