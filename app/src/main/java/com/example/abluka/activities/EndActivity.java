package com.example.abluka.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.abluka.R;
import com.example.abluka.utils.GameMode;

public class EndActivity extends AppCompatActivity {
    private String winner;
    private GameMode gameMode;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end);
        
        // Gradient arka plan ayarla
        ConstraintLayout mainLayout = findViewById(R.id.end_layout);
        mainLayout.setBackground(getResources().getDrawable(R.drawable.gradient_background));
        
        // Intent'ten verileri al
        winner = getIntent().getStringExtra("WINNER");
        String gameModeStr = getIntent().getStringExtra("GAME_MODE");
        gameMode = GameMode.valueOf(gameModeStr != null ? gameModeStr : GameMode.VS_AI.name());
        
        // Kazanan metni
        TextView winnerText = findViewById(R.id.winner_text);
        winnerText.setText("🎉 Tebrikler! " + getPieceColor(winner) + " kazandı! 🎉");
        
        // Ana menü butonu
        Button homeButton = findViewById(R.id.home_button);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EndActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
        
        // Tekrar oyna butonu
        Button replayButton = findViewById(R.id.replay_button);
        replayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EndActivity.this, GameActivity.class);
                intent.putExtra("GAME_MODE", gameMode.name());
                startActivity(intent);
                finish();
            }
        });
    }
    
    private String getPieceColor(String color) {
        return color.equals("black") ? "Siyah" : "Beyaz";
    }
}
