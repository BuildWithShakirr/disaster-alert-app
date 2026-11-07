package com.example.disasteralertapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import android.util.Log;
import com.google.firebase.FirebaseApp;


public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin, btnRegister;
    FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> loginUser());
        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if(email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener(task -> {
            if(task.isSuccessful()) {

                Log.e("REGISTER_DEBUG", "SUCCESS");

                Toast.makeText(this,
                        "Registration Successful",
                        Toast.LENGTH_SHORT).show();

                startActivity(new Intent(LoginActivity.this,
                        MainActivity.class));
                finish();

            } else {

                String error = task.getException() != null
                        ? task.getException().getMessage()
                        : "Unknown Error";

                Log.e("REGISTER_DEBUG", error);

                Toast.makeText(this,
                        "Registration Failed: " + error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loginUser() {
        Log.e("FIREBASE_DEBUG", "LOGIN FUNCTION CALLED");
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if(email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        
                        Log.d("FIREBASE_PROJECT", FirebaseApp.getInstance().getOptions().getProjectId());
                        Log.d("FIREBASE_APP_ID", FirebaseApp.getInstance().getOptions().getApplicationId());
                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}