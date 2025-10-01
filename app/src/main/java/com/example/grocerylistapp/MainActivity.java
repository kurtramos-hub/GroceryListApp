package com.example.grocerylistapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
// Import View and MotionEvent for the OnTouchListener
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView tvWelcome;
    private Button btnLogout, btnAdd;
    private RecyclerView rvGroceries;

    private GroceryAdapter adapter;
    private DatabaseReference userGroceriesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvWelcome = findViewById(R.id.tvWelcome);
        btnLogout = findViewById(R.id.btnLogout);
        btnAdd = findViewById(R.id.btnAdd);
        rvGroceries = findViewById(R.id.rvGroceries);

        // Load BOTH button animations for press and release
        final Animation buttonPressAnim = AnimationUtils.loadAnimation(this, R.anim.button_press);
        final Animation buttonReleaseAnim = AnimationUtils.loadAnimation(this, R.anim.button_release);

        FirebaseUser currentUser = FirebaseUtils.getAuth().getCurrentUser();

        if (currentUser == null) {
            Log.d(TAG, "No current user, redirecting to LoginActivity.");
            startActivity(new Intent(this, LoginActivity.class));
            // Apply page transition on entry
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, R.anim.fade_out);
            finish();
            return;
        }

        String name = currentUser.getDisplayName();
        if (name == null || name.trim().isEmpty()) {
            name = currentUser.getEmail();
        }
        tvWelcome.setText("Welcome " + (name != null ? name : "User"));

        // Adapter setup
        adapter = new GroceryAdapter(new ArrayList<>(), new GroceryAdapter.OnItemActionListener() {
            @Override
            public void onEdit(GroceryItem item) {
                if (item == null || item.getId() == null) {
                    Toast.makeText(MainActivity.this, "Cannot edit: Item data is missing.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
                intent.putExtra("ITEM_ID", item.getId());
                intent.putExtra("ITEM_NAME", item.getItem());
                intent.putExtra("ITEM_QTY", item.getQuantity());
                startActivity(intent);
                // Apply page transition when editing an item
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, R.anim.fade_out);
            }

            @Override
            public void onDelete(GroceryItem item) {
                if (item == null || item.getId() == null) {
                    Toast.makeText(MainActivity.this, "Cannot delete: Item data is missing.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (userGroceriesRef == null) {
                    Toast.makeText(MainActivity.this, "Database error. Please try again.", Toast.LENGTH_SHORT).show();
                    return;
                }
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete Item")
                        .setMessage("Are you sure you want to delete '" + item.getItem() + "'?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            userGroceriesRef.child(item.getId()).removeValue()
                                    .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "'" + item.getItem() + "' deleted.", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        rvGroceries.setLayoutManager(new LinearLayoutManager(this));
        rvGroceries.setAdapter(adapter);

        String userId = currentUser.getUid();
        userGroceriesRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        loadGroceries();

        // Create a reusable OnTouchListener for animations and actions
        View.OnTouchListener buttonTouchListener = (view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Action: Finger presses down
                    view.startAnimation(buttonPressAnim);
                    break;
                case MotionEvent.ACTION_UP:
                    // Action: Finger lifts up
                    view.startAnimation(buttonReleaseAnim);
                    // Perform the actual click action on finger lift
                    if (view.getId() == R.id.btnAdd) {
                        Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
                        startActivity(intent);
                        // Apply page transition
                        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, R.anim.fade_out);
                    } else if (view.getId() == R.id.btnLogout) {
                        FirebaseUtils.getAuth().signOut();
                        Log.d(TAG, "User signed out.");
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                        // Apply page transition
                        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, R.anim.fade_out);
                        finish();
                    }
                    break;
            }
            // Return true to indicate we handled the touch event
            return true;
        };

        // Apply the new listener to the buttons, replacing the old OnClickListeners
        btnAdd.setOnTouchListener(buttonTouchListener);
        btnLogout.setOnTouchListener(buttonTouchListener);
    }

    private void loadGroceries() {
        if (userGroceriesRef == null) {
            Log.e(TAG, "userGroceriesRef is null in loadGroceries.");
            return;
        }

        userGroceriesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<GroceryItem> tempItems = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        try {
                            GroceryItem item = childSnapshot.getValue(GroceryItem.class);
                            if (item != null) {
                                item.setId(childSnapshot.getKey());
                                tempItems.add(item);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing item: " + childSnapshot.getKey(), e);
                        }
                    }
                } else {
                    Log.d(TAG, "No groceries found for user.");
                }
                adapter.setItems(tempItems);
                Log.d(TAG, "Grocery list updated. Item count: " + tempItems.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load groceries: ", error.toException());
                Toast.makeText(MainActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
            }
        });
    }
}