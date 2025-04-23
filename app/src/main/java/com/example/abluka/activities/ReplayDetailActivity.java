package com.example.abluka.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.abluka.R;
import com.example.abluka.models.BoardPosition;
import com.example.abluka.models.GameBoard;
import com.example.abluka.models.GameMove;
import com.example.abluka.views.GameBoardView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReplayDetailActivity extends AppCompatActivity {
    private GameBoard board;
    private List<Map<String, Object>> moves;
    private int currentMoveIndex = -1;
    private boolean isPlaying = false;
    private double playbackSpeed = 1.0;
    private Handler handler = new Handler();
    private Runnable playbackRunnable;

    private GameBoardView gameBoardView;
    private TextView moveCounterText;
    private TextView speedText;
    private Button resetButton;
    private Button prevButton;
    private Button playPauseButton;
    private Button nextButton;
    private Button speedButton;
    private View moveDescriptionLayout;
    private TextView moveDescriptionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_replay_detail);

        // Gradient arka plan ayarla
        ConstraintLayout mainLayout = findViewById(R.id.replay_detail_layout);
        mainLayout.setBackground(getResources().getDrawable(R.drawable.gradient_background));

        // View'ları başlat
        gameBoardView = findViewById(R.id.game_board_view);
        moveCounterText = findViewById(R.id.move_counter_text);
        speedText = findViewById(R.id.speed_text);
        resetButton = findViewById(R.id.reset_button);
        prevButton = findViewById(R.id.prev_button);
        playPauseButton = findViewById(R.id.play_pause_button);
        nextButton = findViewById(R.id.next_button);
        speedButton = findViewById(R.id.speed_button);
        moveDescriptionLayout = findViewById(R.id.move_description_layout);
        moveDescriptionText = findViewById(R.id.move_description_text);

        // Maç verilerini al
        int matchIndex = getIntent().getIntExtra("MATCH_INDEX", 0);
        loadMatchData(matchIndex);

        // Buton işlevleri
        resetButton.setOnClickListener(v -> resetReplay());
        prevButton.setOnClickListener(v -> playPreviousMove());
        playPauseButton.setOnClickListener(v -> togglePlayback());
        nextButton.setOnClickListener(v -> playNextMove());
        speedButton.setOnClickListener(v -> changePlaybackSpeed());

        // Oynatma runnable'ı
        playbackRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    playNextMove();
                    if (isPlaying) {
                        handler.postDelayed(this, (long) (1000 / playbackSpeed));
                    }
                }
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(playbackRunnable);
    }

    private void loadMatchData(int matchIndex) {
        SharedPreferences prefs = getSharedPreferences("AblukaPref", MODE_PRIVATE);
        String matchesJson = prefs.getString("matches", null);

        if (matchesJson != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> matches = gson.fromJson(matchesJson, type);

            if (matchIndex < matches.size()) {
                Map<String, Object> matchData = matches.get(matchIndex);

                // Tahtayı başlat
                String player1 = (String) matchData.get("player1");
                String player2 = (String) matchData.get("player2");
                board = new GameBoard(player1 != null ? player1 : "black", player2 != null ? player2 : "white");

                // Hamle geçmişini al
                List<Map<String, Object>> moveHistoryData = (List<Map<String, Object>>) matchData.get("moveHistory");
                moves = moveHistoryData != null ? moveHistoryData : new ArrayList<>();

                // Başlangıç durumuna sıfırla
                resetReplay();
            }
        }
    }

    private void resetReplay() {
        // Tahtayı başlangıç durumuna sıfırla
        board = new GameBoard(board.getPlayer1(), board.getPlayer2());
        currentMoveIndex = -1;
        isPlaying = false;
        handler.removeCallbacks(playbackRunnable);

        // UI'ı güncelle
        updateUI();
    }

    private void playNextMove() {
        if (currentMoveIndex < moves.size() - 1) {
            currentMoveIndex++;
            applyMove(currentMoveIndex);

            // UI'ı güncelle
            updateUI();

            if (isPlaying) {
                // Sonraki hamle için zamanlayıcı ayarla
                handler.postDelayed(playbackRunnable, (long) (1000 / playbackSpeed));
            }
        } else {
            // Hamlelerin sonuna gelindi
            isPlaying = false;
            updateUI();
        }
    }

    private void playPreviousMove() {
        if (currentMoveIndex >= 0) {
            currentMoveIndex--;

            // Tahtayı sıfırla ve tüm hamleleri mevcut indekse kadar uygula
            board = new GameBoard(board.getPlayer1(), board.getPlayer2());

            for (int i = 0; i <= currentMoveIndex; i++) {
                applyMove(i);
            }

            // UI'ı güncelle
            updateUI();
        }
    }

    private void applyMove(int moveIndex) {
        if (moveIndex < 0 || moveIndex >= moves.size()) return;

        Map<String, Object> move = moves.get(moveIndex);
        BoardPosition from = BoardPosition.fromJson((Map<String, Object>) move.get("from"));
        BoardPosition to = BoardPosition.fromJson((Map<String, Object>) move.get("to"));
        BoardPosition block = BoardPosition.fromJson((Map<String, Object>) move.get("block"));

        // Hangi oyuncunun hamle yaptığını belirle (sırayla)
        String player = moveIndex % 2 == 0 ? board.getPlayer1() : board.getPlayer2();

        // Taş hareketini uygula
        board.movePiece(player, from, to);

        // Engel yerleştirmeyi uygula
        if (block.getX() >= 0 && block.getY() >= 0) {
            board.placeBlock(block.getX(), block.getY());
        }
    }

    private void togglePlayback() {
        isPlaying = !isPlaying;

        if (isPlaying) {
            if (currentMoveIndex >= moves.size() - 1) {
                // Eğer sondaysak, başa dön
                resetReplay();
                playNextMove();
            } else {
                // Oynatmayı başlat
                handler.post(playbackRunnable);
            }
        } else {
            // Oynatmayı durdur
            handler.removeCallbacks(playbackRunnable);
        }

        // UI'ı güncelle
        updateUI();
    }

    private void changePlaybackSpeed() {
        // Hızları döngüsel olarak değiştir: 1x -> 2x -> 3x -> 1x
        if (playbackSpeed == 1.0) {
            playbackSpeed = 2.0;
        } else if (playbackSpeed == 2.0) {
            playbackSpeed = 3.0;
        } else {
            playbackSpeed = 1.0;
        }

        // UI'ı güncelle
        updateUI();
    }

    private void updateUI() {
        // Tahtayı güncelle
        gameBoardView.setBoard(board);

        // Hamle sayacını güncelle
        int currentMoveNumber = currentMoveIndex + 1;
        int totalMoves = moves.size();
        moveCounterText.setText("Hamle " + currentMoveNumber + " / " + totalMoves);

        // Hız metnini güncelle
        speedText.setText(playbackSpeed + "x");

        // Oynat/Duraklat butonunu güncelle: setIcon() yerine compound drawable kullanalım
        int iconRes = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        playPauseButton.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);

        // Butonları etkinleştir/devre dışı bırak
        prevButton.setEnabled(currentMoveIndex >= 0);
        nextButton.setEnabled(currentMoveIndex < moves.size() - 1);

        // Hamle açıklamasını güncelle
        if (currentMoveIndex >= 0 && currentMoveIndex < moves.size()) {
            moveDescriptionLayout.setVisibility(View.VISIBLE);

            Map<String, Object> move = moves.get(currentMoveIndex);
            String playerText = currentMoveIndex % 2 == 0 ? "Siyah Oyuncu" : "Beyaz Oyuncu";
            String moveDesc = buildMoveDescription(move);

            moveDescriptionText.setText(playerText + "\n" + moveDesc);
        } else {
            moveDescriptionLayout.setVisibility(View.GONE);
        }
    }

    private String buildMoveDescription(Map<String, Object> move) {
        BoardPosition from = BoardPosition.fromJson((Map<String, Object>) move.get("from"));
        BoardPosition to = BoardPosition.fromJson((Map<String, Object>) move.get("to"));
        BoardPosition block = BoardPosition.fromJson((Map<String, Object>) move.get("block"));

        return "Taş: (" + from.getX() + "," + from.getY() + ") → (" + to.getX() + "," + to.getY() + ") | Engel: (" + block.getX() + "," + block.getY() + ")";
    }
}