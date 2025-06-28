package com.example.atm_finder;


import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MyProfile extends AppCompatActivity {

    private TextView textFullName, textPhone, textUsername, textBio;
    private Button buttonEditProfile, buttonRefreshProfile;

    private FirebaseUser currentUser;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        // Link XML views
        textFullName = findViewById(R.id.textFullName);
        textPhone = findViewById(R.id.textPhone);
        textUsername = findViewById(R.id.textUsername);
        textBio = findViewById(R.id.textBio);
        buttonEditProfile = findViewById(R.id.buttonEditProfile);
        buttonRefreshProfile = findViewById(R.id.buttonRefreshProfile); // 🔄 New

        // Firebase Auth
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Firebase Database Reference
        String uid = currentUser.getUid();
        userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        // Load info
        loadUserInfo();

        // Edit Profile button
        buttonEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MyProfile.this, EditProfile.class);
            startActivity(intent);
        });

        // Refresh Profile button
        buttonRefreshProfile.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
            loadUserInfo();
        });
    }

    // Load user info from database
    private void loadUserInfo() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String fullName = snapshot.child("fullName").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String username = snapshot.child("username").getValue(String.class);
                    String bio = snapshot.child("bio").getValue(String.class);

                    textFullName.setText(fullName != null ? fullName : "No name");
                    textPhone.setText(phone != null ? phone : "No phone");
                    textUsername.setText(username != null ? "@" + username : "@unknown");
                    textBio.setText(bio != null ? bio : "No bio set");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyProfile.this, "Failed to load user info", Toast.LENGTH_SHORT).show();
            }
        });
    }

}