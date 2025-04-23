package com.example.abluka.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.abluka.R;
import com.example.abluka.adapters.RecentGamesAdapter;
import com.example.abluka.models.Match;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsActivity extends AppCompatActivity {
    private int totalGames = 0;
    private int player1Wins = 0;
    private int player2Wins = 0;
    private List<Match> matches = new ArrayList<>();
    private boolean isLoading = true;

    private ProgressBar loadingIndicator;
    private LinearLayout statsContent;
    private RecyclerView recentGamesRecyclerView;
    private RecentGamesAdapter recentGamesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        // Gradient arka plan ayarla
        ConstraintLayout mainLayout = findViewById(R.id.stats_layout);
        mainLayout.setBackground(getResources().getDrawable(R.drawable.gradient_background));

        // View'ları başlat
        loadingIndicator = findViewById(R.id.loading_indicator);
        statsContent = findViewById(R.id.stats_content);
        recentGamesRecyclerView = findViewById(R.id.recent_games_recycler_view);

        // RecyclerView ayarla
        recentGamesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentGamesAdapter = new RecentGamesAdapter(this, new ArrayList<>());
        recentGamesRecyclerView.setAdapter(recentGamesAdapter);

        // Yenile butonu
        FloatingActionButton refreshButton = findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(v -> loadStats());

        // İstatistikleri yükle
        loadStats();
    }

    private void loadStats() {
        // Yükleme göstergesini göster
        isLoading = true;
        loadingIndicator.setVisibility(View.VISIBLE);
        statsContent.setVisibility(View.GONE);

        // Asenkron olarak yükleme yapıyormuş gibi kısa bir gecikme ekle
        statsContent.postDelayed(() -> {
            // SharedPreferences'dan verileri al
            SharedPreferences prefs = getSharedPreferences("AblukaPref", MODE_PRIVATE);
            totalGames = prefs.getInt("totalGames", 0);
            player1Wins = prefs.getInt("player1Wins", 0);
            player2Wins = prefs.getInt("player2Wins", 0);

            // Maç geçmişini al
            String matchesJson = prefs.getString("matches", null);
            if (matchesJson != null) {
                Gson gson = new Gson();
                Type type = new TypeToken<List<Match>>(){}.getType();
                matches = gson.fromJson(matchesJson, type);
            } else {
                matches = new ArrayList<>();
            }

            // UI'ı güncelle
            updateUI();

            // Yükleme göstergesini gizle
            isLoading = false;
            loadingIndicator.setVisibility(View.GONE);
            statsContent.setVisibility(View.VISIBLE);
        }, 500); // 500ms gecikme
    }

    private void updateUI() {
        // Genel istatistikler
        TextView totalGamesText = findViewById(R.id.total_games_value);
        TextView avgMovesText = findViewById(R.id.avg_moves_value);
        TextView vsAIGamesText = findViewById(R.id.vs_ai_games_value);
        TextView twoPlayerGamesText = findViewById(R.id.two_player_games_value);

        totalGamesText.setText(String.valueOf(totalGames));

        // Ortalama hamle sayısını hesapla
        int totalMoves = 0;
        for (Match match : matches) {
            totalMoves += Integer.parseInt(match.getMoves());
        }
        double avgMoves = totalGames > 0 ? (double) totalMoves / totalGames : 0;
        avgMovesText.setText(String.format("%.1f", avgMoves));

        // Oyun modlarını say
        int vsAIGames = 0;
        int twoPlayerGames = 0;
        for (Match match : matches) {
            if ("vsAI".equals(match.getMode())) {
                vsAIGames++;
            } else {
                twoPlayerGames++;
            }
        }
        vsAIGamesText.setText(String.valueOf(vsAIGames));
        twoPlayerGamesText.setText(String.valueOf(twoPlayerGames));

        // Kazanma oranları
        double blackWinRate = totalGames > 0 ? (double) player1Wins / totalGames * 100 : 0;
        double whiteWinRate = totalGames > 0 ? (double) player2Wins / totalGames * 100 : 0;

        TextView blackWinsText = findViewById(R.id.black_wins_text);
        TextView whiteWinsText = findViewById(R.id.white_wins_text);
        TextView blackWinRateText = findViewById(R.id.black_win_rate_text);
        TextView whiteWinRateText = findViewById(R.id.white_win_rate_text);
        View blackProgressBar = findViewById(R.id.black_progress_bar);
        View whiteProgressBar = findViewById(R.id.white_progress_bar);

        blackWinsText.setText(player1Wins + " Zafer");
        whiteWinsText.setText(player2Wins + " Zafer");
        blackWinRateText.setText(String.format("%.1f%%", blackWinRate));
        whiteWinRateText.setText(String.format("%.1f%%", whiteWinRate));

        // Progress bar'ları güncelle
        LinearLayout.LayoutParams blackParams = (LinearLayout.LayoutParams) blackProgressBar.getLayoutParams();
        LinearLayout.LayoutParams whiteParams = (LinearLayout.LayoutParams) whiteProgressBar.getLayoutParams();

        int maxWidth = getResources().getDisplayMetrics().widthPixels - 112;
        blackParams.width = (int) (blackWinRate / 100 * maxWidth);
        whiteParams.width = (int) (whiteWinRate / 100 * maxWidth);

        blackProgressBar.setLayoutParams(blackParams);
        whiteProgressBar.setLayoutParams(whiteParams);

        // Son oyunları göster
        if (matches.isEmpty()) {
            findViewById(R.id.no_games_layout).setVisibility(View.VISIBLE);
            recentGamesRecyclerView.setVisibility(View.GONE);
        } else {
            findViewById(R.id.no_games_layout).setVisibility(View.GONE);
            recentGamesRecyclerView.setVisibility(View.VISIBLE);

            // Son 3 oyunu göster
            List<Match> recentMatches = new ArrayList<>();
            for (int i = 0; i < Math.min(3, matches.size()); i++) {
                recentMatches.add(matches.get(i));
            }

            // Adapterin beklediği veri tipi List<Map<String, Object>> ise, Match nesnelerini dönüştürelim:
            recentGamesAdapter.updateMatches(convertMatches(recentMatches));
        }

        // "Tümünü Gör" butonu
        findViewById(R.id.view_all_button).setOnClickListener(v -> {
            startActivity(new Intent(StatsActivity.this, ReplayActivity.class));
        });
    }

    // Match nesnesini Map'e dönüştüren basit yardımcı metot. (Gerekli alanları ekleyin)
    private List<Map<String, Object>> convertMatches(List<Match> matchList) {
        List<Map<String, Object>> converted = new ArrayList<>();
        for (Match m : matchList) {
            Map<String, Object> map = new HashMap<>();
            map.put("moves", m.getMoves());
            map.put("mode", m.getMode());
            // İhtiyacınıza göre diğer alanlar da eklenebilir.
            converted.add(map);
        }
        return converted;
    }
}