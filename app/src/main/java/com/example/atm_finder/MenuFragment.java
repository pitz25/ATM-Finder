package com.example.atm_finder;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class MenuFragment extends Fragment {

    private TextView textWelcome, optionMyProfile, optionAboutUs, optionFaq, optionLogout;
    private FirebaseUser currentUser;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    public MenuFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_menu, container, false);

        // Firebase auth and current user
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Firebase DB reference
        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
        }

        // UI Bindings
        textWelcome = root.findViewById(R.id.textWelcome);
        optionMyProfile = root.findViewById(R.id.optionMyProfile);
        optionAboutUs = root.findViewById(R.id.optionAboutUs);
        optionFaq = root.findViewById(R.id.optionFaq);
        optionLogout = root.findViewById(R.id.optionLogout);

        // Show username from Firebase
        if (currentUser != null) {
            userRef.child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String username = snapshot.getValue(String.class);
                        textWelcome.setText("Welcome, " + username + "✨");
                    } else {
                        textWelcome.setText("Welcome, user");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    textWelcome.setText("Welcome, user");
                }
            });
        } else {
            textWelcome.setText("Welcome, user");
        }

        // My Profile
        optionMyProfile.setOnClickListener(v -> startActivity(new Intent(getActivity(), MyProfile.class)));

        // About Us
        optionAboutUs.setOnClickListener(v -> startActivity(new Intent(getActivity(), AboutUs.class)));

        // FAQ
        optionFaq.setOnClickListener(v -> startActivity(new Intent(getActivity(), Faq.class)));

        // Logout
        optionLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(getActivity(), "Logged out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        return root;
    }
}
