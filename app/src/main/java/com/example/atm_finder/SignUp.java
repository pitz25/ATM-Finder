package com.example.atm_finder;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUp extends AppCompatActivity {

    private EditText editTextFullName, editTextUsernameSignup, editTextEmailSignup,
            editTextPhone, editTextPasswordSignup, editTextConfirmPasswordSignup;
    private Button buttonSignup;
    private ProgressBar progressBarSignup;

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef, usernamesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Link UI elements
        editTextFullName = findViewById(R.id.editTextFullName);
        editTextUsernameSignup = findViewById(R.id.editTextUsernameSignup);
        editTextEmailSignup = findViewById(R.id.editTextEmailSignup);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextPasswordSignup = findViewById(R.id.editTextPasswordSignup);
        editTextConfirmPasswordSignup = findViewById(R.id.editTextConfirmPasswordSignup);
        buttonSignup = findViewById(R.id.buttonSignup);
        progressBarSignup = findViewById(R.id.progressBarSignup);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        usernamesRef = FirebaseDatabase.getInstance().getReference("Usernames");

        buttonSignup.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String fullName = editTextFullName.getText().toString().trim();
        String username = editTextUsernameSignup.getText().toString().trim();
        String email = editTextEmailSignup.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String password = editTextPasswordSignup.getText().toString().trim();
        String confirmPassword = editTextConfirmPasswordSignup.getText().toString().trim();

        // Input validation
        if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(username) ||
                TextUtils.isEmpty(email) || TextUtils.isEmpty(phone) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBarSignup.setVisibility(View.VISIBLE);

        // Create user in Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBarSignup.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Save user details under "Users"
                            User newUser = new User(fullName, username, email, phone);
                            usersRef.child(user.getUid()).setValue(newUser);

                            // Save username -> email mapping
                            usernamesRef.child(username).setValue(email);

                            Toast.makeText(SignUp.this, "Registration successful. Please log in.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignUp.this, LoginActivity.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(SignUp.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
