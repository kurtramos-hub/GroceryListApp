package com.example.grocerylistapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log; // Make sure Log is imported
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener; // Ensure this is imported for detailed error
import com.google.android.gms.tasks.OnSuccessListener; // Ensure this is imported
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

// Removed OnCompleteListener and Task imports as we are using OnSuccessListener/OnFailureListener directly

public class AddEditActivity extends AppCompatActivity {
    private static final String TAG = "AddEditActivity";

    private EditText etItemName, etQuantity;
    private Button btnSave;

    private String receivedItemId; // This is the ID from MainActivity if editing
    private DatabaseReference userSpecificGroceriesRef; // Reference to /groceries/{uid}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        etItemName = findViewById(R.id.etItemName);
        etQuantity = findViewById(R.id.etQuantity);
        btnSave = findViewById(R.id.btnSave);

        // --- Get current authenticated user and set up database reference ---
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Error: You are not logged in.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "User is not authenticated. Cannot save or edit item.");
            finish(); // Close activity if no user
            return;
        }
        String currentUserId = currentUser.getUid();
        userSpecificGroceriesRef = FirebaseDatabase.getInstance()
                .getReference("groceries")
                .child(currentUserId);
        Log.d(TAG, "Database reference for user operations: " + userSpecificGroceriesRef.toString());


        // --- Check if this is an edit operation ---
        // MainActivity should pass these extras if it's an edit
        receivedItemId = getIntent().getStringExtra("ITEM_ID");
        if (receivedItemId != null) {
            setTitle("Edit Item");
            String name = getIntent().getStringExtra("ITEM_NAME");
            int qty = getIntent().getIntExtra("ITEM_QTY", 1); // Default to 1 if not found

            etItemName.setText(name);
            etQuantity.setText(String.valueOf(qty));
            Log.d(TAG, "Editing item. ID: " + receivedItemId + ", Name: " + name + ", Qty: " + qty);
        } else {
            setTitle("Add New Item");
            Log.d(TAG, "Adding new item.");
        }


        btnSave.setOnClickListener(v -> {
            String itemName = etItemName.getText().toString().trim();
            String quantityStr = etQuantity.getText().toString().trim();
            int quantity = 1; // Default quantity

            if (TextUtils.isEmpty(itemName)) {
                etItemName.setError("Item name cannot be empty.");
                Toast.makeText(this, "Please enter item name.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!TextUtils.isEmpty(quantityStr)) {
                try {
                    quantity = Integer.parseInt(quantityStr);
                    if (quantity <= 0) {
                        etQuantity.setError("Quantity must be positive.");
                        Toast.makeText(this, "Quantity must be a positive number.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    etQuantity.setError("Invalid quantity.");
                    Toast.makeText(this, "Invalid quantity format.", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Invalid quantity format: " + quantityStr, e);
                    return; // Stop processing if quantity is invalid
                }
            } else {
                // If quantity field is empty, you might want to default it or require it
                // For now, it defaults to 1 as initialized.
                // If quantity is required:
                // etQuantity.setError("Quantity cannot be empty.");
                // Toast.makeText(this, "Please enter quantity.", Toast.LENGTH_SHORT).show();
                // return;
            }


            if (userSpecificGroceriesRef == null) {
                Toast.makeText(this, "Database error. Please restart the app.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "userSpecificGroceriesRef is null. Cannot proceed with save.");
                return;
            }

            // --- Logic for saving or updating ---
            if (receivedItemId == null) {
                addNewItem(itemName, quantity);
            } else {
                updateExistingItem(receivedItemId, itemName, quantity);
            }
        });
    }

    private void addNewItem(String name, int quantity) {
        String newFirebaseKey = userSpecificGroceriesRef.push().getKey();

        if (newFirebaseKey == null) {
            Toast.makeText(this, "Could not create item entry. Please try again.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Firebase push().getKey() returned null for new item.");
            return;
        }

        // Assuming your GroceryItem constructor is: public GroceryItem(String id, String name, int quantity)
        GroceryItem item = new GroceryItem(newFirebaseKey, name, quantity);

        Log.d(TAG, "Attempting to save NEW item to path: " + userSpecificGroceriesRef.child(newFirebaseKey).toString() + " with data: " + item.toString());

        userSpecificGroceriesRef.child(newFirebaseKey).setValue(item)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddEditActivity.this, "Item added successfully!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Successfully added new item with key: " + newFirebaseKey);
                    finish(); // Go back to MainActivity
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddEditActivity.this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to save new item to " + userSpecificGroceriesRef.child(newFirebaseKey).toString(), e);
                });
    }

    private void updateExistingItem(String itemIdToUpdate, String name, int quantity) {
        // Assuming your GroceryItem constructor is: public GroceryItem(String id, String name, int quantity)
        GroceryItem item = new GroceryItem(itemIdToUpdate, name, quantity);
        // Note: When using setValue() with a POJO that contains an 'id' field,
        // Firebase will overwrite the entire node at itemIdToUpdate with this new object.
        // If the 'id' field in your 'item' object (itemIdToUpdate) matches the key of the node being updated,
        // it's effectively just updating the fields.

        Log.d(TAG, "Attempting to UPDATE item at path: " + userSpecificGroceriesRef.child(itemIdToUpdate).toString() + " with data: " + item.toString());

        userSpecificGroceriesRef.child(itemIdToUpdate).setValue(item)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddEditActivity.this, "Item updated successfully!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Successfully updated item with ID: " + itemIdToUpdate);
                    finish(); // Go back to MainActivity
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddEditActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to update item at " + userSpecificGroceriesRef.child(itemIdToUpdate).toString(), e);
                });
    }
}