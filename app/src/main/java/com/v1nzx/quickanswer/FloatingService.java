package com.v1nzx.quickanswer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FloatingService extends Service {

    private WindowManager wm;
    private View btnView, menuView;
    private WindowManager.LayoutParams btnParams, menuParams;
    private boolean menuOpen = false;
    private WebView hiddenWebView;
    private String examUrl = "";
    private TextView tvStatus, tvQuestion, tvAnswer;

    @Override
    public void onCreate() {
        super.onCreate();

        String CH = "v1nzx_exam";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CH, "Exam Search",
                    NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                    .createNotificationChannel(c);
        }
        Notification n = new Notification.Builder(this, CH)
                .setContentTitle("V1NZX Quick Answer")
                .setContentText("Assistant aktif")
                .setSmallIcon(android.R.drawable.ic_menu_search)
                .build();
        startForeground(1, n);

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        hiddenWebView = new WebView(this);
        hiddenWebView.getSettings().setJavaScriptEnabled(true);
        hiddenWebView.setWebViewClient(new WebViewClient());

        setupFloatingBtn();
        setupMenu();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("url")) {
            examUrl = intent.getStringExtra("url");
        }
        return START_STICKY;
    }

    private void setupFloatingBtn() {
        btnView = LayoutInflater.from(this).inflate(R.layout.floating_button, null);
        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        btnParams = new WindowManager.LayoutParams(
                150, 150, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        btnParams.gravity = Gravity.TOP | Gravity.START;
        btnParams.x = 50;
        btnParams.y = 300;

        ImageView iv = btnView.findViewById(R.id.ivButton);
        iv.setOnTouchListener(new View.OnTouchListener() {
            int initX, initY;
            float initTX, initTY;
            boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initX = btnParams.x; initY = btnParams.y;
                        initTX = e.getRawX(); initTY = e.getRawY();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int)(e.getRawX() - initTX);
                        int dy = (int)(e.getRawY() - initTY);
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) moved = true;
                        btnParams.x = initX + dx;
                        btnParams.y = initY + dy;
                        wm.updateViewLayout(btnView, btnParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) toggleMenu();
                        return true;
                }
                return false;
            }
        });

        wm.addView(btnView, btnParams);
    }

    private void setupMenu() {
        menuView = LayoutInflater.from(this).inflate(R.layout.floating_menu, null);
        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        menuParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        menuParams.gravity = Gravity.TOP;
        menuParams.y = 200;

        tvStatus = menuView.findViewById(R.id.tvStatus);
        tvQuestion = menuView.findViewById(R.id.tvQuestion);
        tvAnswer = menuView.findViewById(R.id.tvAnswer);

        menuView.findViewById(R.id.btnScan).setOnClickListener(v -> scanExamPage());
        menuView.findViewById(R.id.btnGoogle).setOnClickListener(v -> {
            String q = tvQuestion.getText().toString();
            if (q.isEmpty() || q.startsWith("(")) {
                tvStatus.setText("Scan dulu!");
                return;
            }
            openBrowser("https://www.google.com/search?q=" + Uri.encode(q));
        });
        menuView.findViewById(R.id.btnClose).setOnClickListener(v -> toggleMenu());
        menuView.findViewById(R.id.btnAuto).setOnClickListener(v -> autoSearch());
    }

    private void toggleMenu() {
        if (menuOpen) {
            try { wm.removeView(menuView); } catch (Exception e) {}
            menuOpen = false;
        } else {
            try { wm.addView(menuView, menuParams); } catch (Exception e) {}
            menuOpen = true;
        }
    }

    private void scanExamPage() {
        if (examUrl.isEmpty()) {
            tvStatus.setText("Link belum di-set");
            return;
        }
        tvStatus.setText("Scanning...");

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request req = new Request.Builder().url(examUrl)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) Chrome/120 Mobile")
                        .build();
                Response res = client.newCall(req).execute();
                String html = res.body().string();

                runOnUiThread(() -> {
                    String question = QuestionExtractor.extractFirst(html);
                    if (question.isEmpty()) {
                        tvStatus.setText("Ga nemu pertanyaan");
                        tvQuestion.setText("(kosong)");
                    } else {
                        tvQuestion.setText(question);
                        tvStatus.setText("Ketemu");
                        autoSearch();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> tvStatus.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    private void autoSearch() {
        String q = tvQuestion.getText().toString();
        if (q.isEmpty() || q.startsWith("(")) {
            tvStatus.setText("Scan dulu!");
            return;
        }
        tvAnswer.setText("Searching...");
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request req = new Request.Builder()
                        .url("https://www.google.com/search?q=" + Uri.encode(q))
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) Chrome/120 Mobile")
                        .build();
                Response res = client.newCall(req).execute();
                String html = res.body().string();
                String answer = QuestionExtractor.parseGoogleAnswer(html);

                runOnUiThread(() -> tvAnswer.setText(answer));
            } catch (Exception e) {
                runOnUiThread(() -> tvAnswer.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    private void openBrowser(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (btnView != null) try { wm.removeView(btnView); } catch (Exception e) {}
        if (menuView != null) try { wm.removeView(menuView); } catch (Exception e) {}
        if (hiddenWebView != null) hiddenWebView.destroy();
    }

    @Nullable @Override public IBinder onBind(Intent i) { return null; }
          }
