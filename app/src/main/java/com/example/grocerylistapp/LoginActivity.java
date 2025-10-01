package com.example.grocerylistapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log; // Make sure Log is imported
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.grocerylistapp.databinding.ActivityLoginBinding; // For View Binding
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser; // Import FirebaseUser

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity_DEBUG"; // Tag for logging

    private ActivityLoginBinding binding; // View Binding variable
    private FirebaseAuth auth;            // Firebase Auth variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate - START");

        // Inflate the layout using View Binding
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            // Initialize Firebase Auth
            // You can use FirebaseUtils.getAuth() if you prefer,
            // but direct getInstance() is also common.
            auth = FirebaseAuth.getInstance();
            Log.d(TAG, "FirebaseAuth.getInstance() SUCCEEDED");

            // Check if user is already logged in
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser != null) {
                Log.d(TAG, "User already logged in: " + currentUser.getUid() + ". Redirecting to MainActivity.");
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish(); // Finish LoginActivity so user can't navigate back to it
                return;   // Important: return to prevent rest of onCreate if redirecting
            } else {
                Log.d(TAG, "No user currently logged in. Proceeding with login/register UI.");
            }

        } catch (Exception e) {
            // Catch any unexpected errors during Firebase setup
            Log.e(TAG, "Error during FirebaseAuth setup or current user check", e);
            Toast.makeText(this, "Initialization error. Please try restarting the app.", Toast.LENGTH_LONG).show();
            // Depending on the severity, you might want to finish() the activity here
            // For now, we'll let it proceed to show the login form, but auth might be null.
        }


        // Set up Login Button click listener
        binding.btnLogin.setOnClickListener(v -> {
            Log.d(TAG, "Login button clicked.");
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                binding.etEmail.setError("Email is required.");
                Toast.makeText(LoginActivity.this, "Please enter your email.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Login attempt with empty email.");
                return;
            }

            if (TextUtils.isEmpty(password)) {
                binding.etPassword.setError("Password is required.");
                Toast.makeText(LoginActivity.this, "Please enter your password.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Login attempt with empty password.");
                return;
            }

            // Ensure auth object is not null before using it
            if (auth == null) {
                Toast.makeText(LoginActivity.this, "Authentication service not ready. Please restart.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "auth is null in btnLogin.setOnClickListener. Firebase might not have initialized correctly.");
                return;
            }

            Log.d(TAG, "Attempting signInWithEmailAndPassword for: " + email);
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        // Added 'this' for clarity, ensures listener is removed if activity is destroyed.
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Login SUCCESSFUL for: " + email);
                                Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish(); // Finish LoginActivity
                            } else {
                                Log.e(TAG, "Login FAILED for: " + email, task.getException());
                                // Provide a more user-friendly error or specific ones based on task.getException()
                                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                                Toast.makeText(LoginActivity.this, "Login failed: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });

        // Set up Register TextView click listener
        binding.tvRegister.setOnClickListener(v -> {
            Log.d(TAG, "Register TextView clicked. Starting RegisterActivity.");
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            // Typically, you don't finish LoginActivity when going to RegisterActivity,
            // so the user can come back if they decide not to register.
        });

        Log.d(TAG, "onCreate - END (UI and listeners set up)");
    }
}