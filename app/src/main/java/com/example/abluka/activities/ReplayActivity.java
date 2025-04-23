package com.example.abluka.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.abluka.R;
import com.example.abluka.adapters.ReplayAdapter;
import com.example.abluka.models.Match;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReplayActivity extends AppCompatActivity {
    private List<Map<String, Object>> matches = new ArrayList<>();
    private boolean isLoading = true;
    
    private RecyclerView replayRecyclerView;
    private View emptyStateView;
    private TextView blackWinsText;
    private TextView whiteWinsText;
    private TextView totalMatchesText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_replay);
        
        // Gradient arka plan ayarla
        ConstraintLayout mainLayout = findViewById(R.id.replay_layout);
        mainLayout.setBackground(getResources().getDrawable(R.drawable.gradient_background));
        
        // View'ları başlat
        replayRecyclerView = findViewById(R.id.replay_recycler_view);
        emptyStateView = findViewById(R.id.empty_state_view);
        blackWinsText = findViewById(R.id.black_wins_text);
        whiteWinsText = findViewById(R.id.white_wins_text);
        totalMatchesText = findViewById(R.id.total_matches_text);
        
        // RecyclerView ayarla
        replayRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Yenile butonu
        FloatingActionButton refreshButton = findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(v -> loadMatches());
        
        // Maçları yükle
        loadMatches();
    }
    
    private void loadMatches() {
        // Yükleme göstergesini göster
        isLoading = true;
        
        // SharedPreferences'dan verileri al
        SharedPreferences prefs = getSharedPreferences("AblukaPref", MODE_PRIVATE);
        String matchesJson = prefs.getString("matches", null);
        
        if (matchesJson != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
            matches = gson.fromJson(matchesJson, type);
        } else {
            matches = new ArrayList<>();
        }
        
        // UI'ı güncelle
        updateUI();
        
        // Yükleme göstergesini gizle
        isLoading = false;
    }
    
    private void updateUI() {
        if (matches.isEmpty()) {
            emptyStateView.setVisibility(View.VISIBLE);
            replayRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateView.setVisibility(View.GONE);
            replayRecyclerView.setVisibility(View.VISIBLE);
            
            // Adapter'ı ayarla
            ReplayAdapter adapter = new ReplayAdapter(this, matches);
            adapter.setOnItemClickListener(position -> {
                Intent intent = new Intent(ReplayActivity.this, ReplayDetailActivity.class);
                intent.putExtra("MATCH_INDEX", position);
                startActivity(intent);
            });
            replayRecyclerView.setAdapter(adapter);
        }
        
        // İstatistikleri güncelle
        totalMatchesText.setText(String.valueOf(matches.size()));
        blackWinsText.setText(String.valueOf(countWins("black")));
        whiteWinsText.setText(String.valueOf(countWins("white")));
    }
    
    private int countWins(String player) {
        int count = 0;
        for (Map<String, Object> match : matches) {
            if (player.equals(match.get("winner"))) {
                count++;
            }
        }
        return count;
    }
}
