package com.example.grocerylistapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AddEditActivity extends AppCompatActivity {
    private static final String TAG = "AddEditActivity";

    private EditText etItemName, etQuantity;
    private Button btnSave;
    private Button btnBack; // NEW: Declare the back button

    private String receivedItemId;
    private DatabaseReference userSpecificGroceriesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        // --- NEW: Add this block to enable the ActionBar back arrow ---
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        // ----------------------------------------------------

        etItemName = findViewById(R.id.etItemName);
        etQuantity = findViewById(R.id.etQuantity);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack); // NEW: Initialize the back button

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Error: You are not logged in.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "User is not authenticated. Cannot save or edit item.");
            finish();
            return;
        }
        String currentUserId = currentUser.getUid();
        userSpecificGroceriesRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentUserId);
        Log.d(TAG, "Database reference for user operations: " + userSpecificGroceriesRef.toString());

        receivedItemId = getIntent().getStringExtra("ITEM_ID");
        if (receivedItemId != null) {
            setTitle("Edit Item");
            String itemName = getIntent().getStringExtra("ITEM_NAME");
            int qty = getIntent().getIntExtra("ITEM_QTY", 1);

            etItemName.setText(itemName);
            etQuantity.setText(String.valueOf(qty));
            Log.d(TAG, "Editing item. ID: " + receivedItemId + ", Name: " + itemName + ", Qty: " + qty);
        } else {
            setTitle("Add New Item");
            Log.d(TAG, "Adding new item.");
        }

        btnSave.setOnClickListener(v -> {
            String itemName = etItemName.getText().toString().trim();
            String quantityStr = etQuantity.getText().toString().trim();
            int quantity;

            if (TextUtils.isEmpty(itemName)) {
                etItemName.setError("Item name cannot be empty.");
                Toast.makeText(this, "Please enter item name.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                quantity = TextUtils.isEmpty(quantityStr) ? 1 : Integer.parseInt(quantityStr);
                if (quantity <= 0) {
                    etQuantity.setError("Quantity must be positive.");
                    Toast.makeText(this, "Quantity must be a positive number.", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                etQuantity.setError("Invalid quantity.");
                Toast.makeText(this, "Invalid quantity format.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Invalid quantity format: " + quantityStr, e);
                return;
            }

            if (userSpecificGroceriesRef == null) {
                Toast.makeText(this, "Database error. Please restart the app.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "userSpecificGroceriesRef is null. Cannot proceed with save.");
                return;
            }

            if (receivedItemId == null) {
                addNewItem(itemName, quantity);
            } else {
                updateExistingItem(receivedItemId, itemName, quantity);
            }
        });

        // NEW: Add the click listener for the explicit "Back" button
        btnBack.setOnClickListener(v -> {
            // Simply finish the current activity to go back
            finish();
        });
    }

    // --- NEW: Add this method to handle clicks on the ActionBar ---
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // The 'android.R.id.home' is the ID of the back arrow
        if (item.getItemId() == android.R.id.home) {
            finish(); // Closes the current activity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- NEW: Override finish() to apply the return animation consistently ---
    @Override
    public void finish() {
        super.finish();
        // This ensures the animation runs whether the user clicks our "Back" button,
        // the ActionBar arrow, or the physical device back button.
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.fade_in, R.anim.fade_out);
    }

    private void addNewItem(String itemName, int quantity) {
        String newFirebaseKey = userSpecificGroceriesRef.push().getKey();
        if (newFirebaseKey == null) {
            Toast.makeText(this, "Could not create item entry.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Firebase push().getKey() returned null.");
            return;
        }

        GroceryItem item = new GroceryItem(newFirebaseKey, itemName, quantity);
        userSpecificGroceriesRef.child(newFirebaseKey).setValue(item)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddEditActivity.this, "Item added successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to MainActivity after success
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddEditActivity.this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to save new item", e);
                });
    }

    private void updateExistingItem(String itemIdToUpdate, String itemName, int quantity) {
        GroceryItem item = new GroceryItem(itemIdToUpdate, itemName, quantity);
        userSpecificGroceriesRef.child(itemIdToUpdate).setValue(item)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddEditActivity.this, "Item updated successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to MainActivity after success
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddEditActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to update item", e);
                });
    }
}