package com.example.abluka.activities;

import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;  // Değişiklik: ImageView yerine LinearLayout olarak import edebilir veya gereksizse kaldırabilirsiniz.
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.abluka.R;
import com.example.abluka.utils.GameMode;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Ana layout için gradient arka plan ayarla
        ConstraintLayout mainLayout = findViewById(R.id.main_layout);
        mainLayout.setBackground(getResources().getDrawable(R.drawable.gradient_background));

        // Logo ve başlık ayarları
        setupGameLogo();

        // Ana menü butonları
        Button vsAiButton = findViewById(R.id.vs_ai_button);
        Button twoPlayerButton = findViewById(R.id.two_player_button);

        vsAiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, GameActivity.class);
                intent.putExtra("GAME_MODE", GameMode.VS_AI.name());
                startActivity(intent);
            }
        });

        twoPlayerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, GameActivity.class);
                intent.putExtra("GAME_MODE", GameMode.TWO_PLAYER.name());
                startActivity(intent);
            }
        });

        // İkincil menü butonları
        findViewById(R.id.settings_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
            }
        });

        findViewById(R.id.stats_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, StatsActivity.class));
            }
        });

        findViewById(R.id.replay_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, ReplayActivity.class));
            }
        });

        // Versiyon bilgisi
        TextView versionText = findViewById(R.id.version_text);
        versionText.setText("v1.0.0");
    }

    private void setupGameLogo() {
        // Eğer layout dosyanızda "logo_container" LinearLayout olarak tanımlandıysa, tipini da ona göre ayarlayın.
        LinearLayout logoContainer = findViewById(R.id.logo_container);
        // Eğer logoContainer üzerinde özel bir işlem yapılmayacaksa, bu satırı silebilirsiniz.

        TextView gameTitle = findViewById(R.id.game_title);

        // Başlık için gradient efekti
        Shader shader = new LinearGradient(
                0, 0, 0, gameTitle.getTextSize(),
                getResources().getColor(R.color.gold_start),
                getResources().getColor(R.color.gold_end),
                Shader.TileMode.CLAMP);
        gameTitle.getPaint().setShader(shader);
    }
}