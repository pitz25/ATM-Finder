package com.example.atm_finder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class EditProfile extends AppCompatActivity {

    private EditText editTextFullName, editTextPhone, editTextUsername, editTextBio;
    private Button buttonUpdateProfile, buttonLogout, buttonDeleteAccount;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        createNotificationChannel();
        requestNotificationPermission();

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(EditProfile.this, LoginActivity.class));
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());

        editTextFullName = findViewById(R.id.editTextFullName);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextBio = findViewById(R.id.editTextBio);
        buttonUpdateProfile = findViewById(R.id.buttonUpdateProfile);
        buttonLogout = findViewById(R.id.buttonLogout);
        buttonDeleteAccount = findViewById(R.id.buttonDeleteAccount);

        loadUserData();

        buttonUpdateProfile.setOnClickListener(v -> updateProfile());

        buttonLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(EditProfile.this, "Logged out", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(EditProfile.this, LoginActivity.class));
            finish();
        });

        buttonDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());
    }

    private void loadUserData() {
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    editTextFullName.setText(snapshot.child("fullName").getValue(String.class));
                    editTextPhone.setText(snapshot.child("phone").getValue(String.class));
                    editTextUsername.setText(snapshot.child("username").getValue(String.class));
                    editTextBio.setText(snapshot.child("bio").getValue(String.class));
                }
            } else {
                Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProfile() {
        String fullName = editTextFullName.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String username = editTextUsername.getText().toString().trim();
        String bio = editTextBio.getText().toString().trim();

        if (fullName.isEmpty() || phone.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        userRef.child("fullName").setValue(fullName);
        userRef.child("phone").setValue(phone);
        userRef.child("username").setValue(username);
        userRef.child("bio").setValue(bio);

        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

        showProfileUpdatedNotification();
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account?")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        String uid = currentUser.getUid();

        userRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                currentUser.delete().addOnCompleteListener(deleteTask -> {
                    if (deleteTask.isSuccessful()) {
                        Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(EditProfile.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to delete account", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Failed to remove user data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "profile_channel",
                    "Profile Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for profile updates");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showProfileUpdatedNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "profile_channel")
                .setSmallIcon(R.drawable.baseline_person_24)
                .setContentTitle("Profile Updated")
                .setContentText("Profile updated successfully")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 102);
            }
        }
    }
}
