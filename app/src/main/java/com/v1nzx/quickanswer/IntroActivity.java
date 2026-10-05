package com.v1nzx.quickanswer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class IntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        SharedPreferences prefs = getSharedPreferences("v1nzx_intro", MODE_PRIVATE);
        if (prefs.getBoolean("agreed", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_intro);

        Button btnAgree = findViewById(R.id.btnAgree);
        CheckBox cbAgree = findViewById(R.id.cbAgree);

        btnAgree.setOnClickListener(v -> {
            if (cbAgree.isChecked()) {
                prefs.edit().putBoolean("agreed", true).apply();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Centang dulu untuk setuju!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
