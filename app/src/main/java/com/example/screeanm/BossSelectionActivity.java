package com.example.screeanm;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class BossSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boss_selection);

        Button btnShhhark = findViewById(R.id.btnShhhark);
        btnShhhark.setOnClickListener(v -> {
            Intent intent = new Intent(BossSelectionActivity.this, BattleActivity.class);
            intent.putExtra("BOSS_NAME", "Shhhark");
            startActivity(intent);
        });
    }
}