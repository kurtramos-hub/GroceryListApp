package com.example.grocerylistapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
// Import View and MotionEvent for animations
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.grocerylistapp.databinding.ActivityRegisterBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseUtils.getAuth();

        // Load button animations
        final Animation buttonPressAnim = AnimationUtils.loadAnimation(this, R.anim.button_press);
        final Animation buttonReleaseAnim = AnimationUtils.loadAnimation(this, R.anim.button_release);

        // Use OnTouchListener for register button animation
        binding.btnRegister.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.startAnimation(buttonPressAnim);
                    break;
                case MotionEvent.ACTION_UP:
                    view.startAnimation(buttonReleaseAnim);
                    registerUser(); // Call the register logic
                    break;
            }
            return true;
        });

        // --- NEW: Add OnClickListener for the Login TextView ---
        binding.tvLogin.setOnClickListener(v -> {
            // Finish the current activity to go back to LoginActivity
            finish();
        });
    }

    // NEW: Extracted registration logic into its own method
    private void registerUser() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            binding.etName.setError("Name is required.");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email is required.");
            return;
        }

        if (password.length() < 6) {
            binding.etPassword.setError("Password must be at least 6 characters.");
            return;
        }

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Set the user's display name
                if (auth.getCurrentUser() != null) {
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();
                    auth.getCurrentUser().updateProfile(profileUpdates);
                }
                Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(intent);
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, R.anim.fade_out);
                finish();
            } else {
                // FIX: Add specific error handling for registration
                try {
                    throw task.getException();
                } catch (FirebaseAuthUserCollisionException e) {
                    Toast.makeText(RegisterActivity.this, "This email is already registered.", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(RegisterActivity.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // NEW: Override finish() to apply the return animation
    @Override
    public void finish() {
        super.finish();
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.fade_in, R.anim.fade_out);
    }
}