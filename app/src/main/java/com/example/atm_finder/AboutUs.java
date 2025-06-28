package com.example.atm_finder;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutUs extends AppCompatActivity {

    private TextView dropdownToggle, developerList;
    private boolean isListVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        dropdownToggle = findViewById(R.id.dropdownToggle);
        developerList = findViewById(R.id.developerList);

        dropdownToggle.setOnClickListener(view -> {
            isListVisible = !isListVisible;
            developerList.setVisibility(isListVisible ? View.VISIBLE : View.GONE);
            dropdownToggle.setText(isListVisible ? "🔼 Hide Developers" : "🔽 Show Developers");
        });
    }
}