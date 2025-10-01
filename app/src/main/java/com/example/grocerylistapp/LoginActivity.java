package com.example.grocerylistapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
// NEW: Import View and MotionEvent for animations
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.grocerylistapp.databinding.ActivityLoginBinding;
// NEW: Import specific Firebase exceptions for better error handling
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
// Other imports
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity_DEBUG";

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate - START");

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();

        // Check if user is already logged in (your code is good here)
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already logged in: " + currentUser.getUid() + ". Redirecting to MainActivity.");
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        // --- NEW: Add Button and Page Animations ---
        final Animation buttonPressAnim = AnimationUtils.loadAnimation(this, R.anim.button_press);
        final Animation buttonReleaseAnim = AnimationUtils.loadAnimation(this, R.anim.button_release);

        // --- FIX 2: Use OnTouchListener for Login Button to handle animations ---
        binding.btnLogin.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.startAnimation(buttonPressAnim);
                    break;
                case MotionEvent.ACTION_UP:
                    view.startAnimation(buttonReleaseAnim);
                    // Perform the login logic on finger lift
                    loginUser();
                    break;
            }
            return true; // We handled the touch event
        });


        // --- Listener for Register TextView ---
        binding.tvRegister.setOnClickListener(v -> {
            Log.d(TAG, "Register TextView clicked. Starting RegisterActivity.");
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            // Apply a fade transition when going to Register page
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, R.anim.fade_out);
        });

        Log.d(TAG, "onCreate - END (UI and listeners set up)");
    }

    private void loginUser() {
        Log.d(TAG, "loginUser() method called.");
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email is required.");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password is required.");
            return;
        }

        Log.d(TAG, "Attempting signInWithEmailAndPassword for: " + email);
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // --- Login Successful ---
                        Log.d(TAG, "Login SUCCESSFUL for: " + email);
                        Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        // Apply fade transition on successful login
                        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, R.anim.fade_out);
                        finish();
                    } else {
                        // --- Login Failed: Check the exception ---
                        Log.w(TAG, "Login FAILED for: " + email, task.getException());
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthInvalidUserException e) {
                            // This error means the email address does not exist
                            Toast.makeText(LoginActivity.this, "No account found with this email.", Toast.LENGTH_LONG).show();
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            // **FIX 1: THIS IS THE FIX YOU ASKED FOR**
                            // This error means the password is wrong
                            Toast.makeText(LoginActivity.this, "Incorrect password. Please try again.", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            // Handle any other exceptions
                            Toast.makeText(LoginActivity.this, "Login failed. Please try again.", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Login failed with unknown exception: " + e.getMessage());
                        }
                    }
                });
    }
}