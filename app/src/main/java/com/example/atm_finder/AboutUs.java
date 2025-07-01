package com.example.atm_finder;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutUs extends AppCompatActivity {

    private TextView dropdownToggle, developerList, githubLink;
    private boolean isListVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        dropdownToggle = findViewById(R.id.dropdownToggle);
        developerList = findViewById(R.id.developerList);
        githubLink = findViewById(R.id.githubLink);

        dropdownToggle.setOnClickListener(view -> {
            isListVisible = !isListVisible;
            developerList.setVisibility(isListVisible ? View.VISIBLE : View.GONE);
            dropdownToggle.setText(isListVisible ? "🔼 Hide Developers" : "🔽 Show Developers");
        });

        // GitHub link click listener
        githubLink.setOnClickListener(view -> {
            String url = "https://github.com/pitz25/ATM-Finder";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });
    }
}
