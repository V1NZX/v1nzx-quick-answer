package com.v1nzx.quickanswer;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int OVERLAY_REQ = 100;
    private EditText etLink;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        etLink = findViewById(R.id.etLink);
        Button btnPermission = findViewById(R.id.btnPermission);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);

        btnPermission.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(i, OVERLAY_REQ);
            } else {
                Toast.makeText(this, "Overlay OK", Toast.LENGTH_SHORT).show();
            }
        });

        btnStart.setOnClickListener(v -> {
            String link = etLink.getText().toString().trim();
            if (link.isEmpty() || !link.startsWith("http")) {
                Toast.makeText(this, "Isi link dulu!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Izinkan overlay dulu!", Toast.LENGTH_SHORT).show();
                return;
            }

            getSharedPreferences("v1nzx", MODE_PRIVATE).edit()
                    .putString("exam_url", link).apply();

            Intent svc = new Intent(this, FloatingService.class);
            svc.putExtra("url", link);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc);
            else startService(svc);

            Toast.makeText(this, "Floating aktif", Toast.LENGTH_SHORT).show();
            moveTaskToBack(true);
        });

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, FloatingService.class));
            Toast.makeText(this, "Stop", Toast.LENGTH_SHORT).show();
        });

        String saved = getSharedPreferences("v1nzx", MODE_PRIVATE)
                .getString("exam_url", "");
        if (!saved.isEmpty()) etLink.setText(saved);
    }
}
