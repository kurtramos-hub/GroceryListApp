package com.example.grocerylistapp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseUtils {
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();
    private static final FirebaseDatabase db = FirebaseDatabase.getInstance();

    public static FirebaseAuth getAuth() { return auth; }

    public static DatabaseReference userRef() {
        if (auth.getCurrentUser() == null)
            throw new IllegalStateException("No user logged in");
        return db.getReference("users")
                .child(auth.getCurrentUser().getUid())
                .child("groceries");
    }
}