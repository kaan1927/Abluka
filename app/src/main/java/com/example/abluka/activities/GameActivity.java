package com.example.abluka.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.abluka.R;
import com.example.abluka.ai.AblukaAI;
import com.example.abluka.ai.HumanMoveAdvisor;
import com.example.abluka.models.BoardPosition;
import com.example.abluka.models.GameBoard;
import com.example.abluka.models.GameMove;
import com.example.abluka.services.VibrationService;
import com.example.abluka.utils.GameMode;
import com.example.abluka.views.GameBoardView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

public class GameActivity extends AppCompatActivity implements GameBoardView.OnCellTapListener {
    private GameBoard board;
    private String currentPlayer;
    private int phase = 1; // 1 = Taş Hareketi, 2 = Blok Koyma
    private List<GameState> history = new ArrayList<>();
    private List<GameMove> moveHistory = new ArrayList<>();
    private String humanMoveAdvice = "";
    private int aiDepth = 4; // Zorluk: Easy=1, Medium=2, Hard=4
    private boolean aiThinking = false;
    private boolean calculatingHumanMove = false;
    private boolean showHints = true;
    private boolean vibrationEnabled = true;
    private VibrationService vibrationService;
    private GameMode gameMode;

    private GameBoardView gameBoardView;
    private TextView turnIndicatorText;
    private TextView moveAdviceText;
    private View moveAdviceLayout;
    private Button undoButton;
    private Button resetButton;

    private Timer calculationTimeoutTimer;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Gradient arka plan ayarla
        ConstraintLayout mainLayout = findViewById(R.id.game_layout);
        mainLayout.setBackground(getResources().getDrawable(R.drawable.gradient_background));

        // View'ları başlat
        gameBoardView = findViewById(R.id.game_board_view);
        turnIndicatorText = findViewById(R.id.turn_indicator_text);
        moveAdviceLayout = findViewById(R.id.move_advice_layout);
        moveAdviceText = findViewById(R.id.move_advice_text);
        undoButton = findViewById(R.id.undo_button);
        resetButton = findViewById(R.id.reset_button);

        // Titreşim servisini başlat
        vibrationService = new VibrationService(getSystemService(VIBRATOR_SERVICE));

        // Oyun modunu al
        String gameModeStr = getIntent().getStringExtra("GAME_MODE");
        gameMode = GameMode.valueOf(gameModeStr != null ? gameModeStr : GameMode.VS_AI.name());

        // Ayarları asenkron olarak yükle ve ardından oyunu başlat
        loadSettingsAsync().thenRun(this::initializeGame);

        // Buton işlevleri
        undoButton.setOnClickListener(v -> undoMove());
        resetButton.setOnClickListener(v -> resetGame());

        // GameBoardView'a listener ekle
        gameBoardView.setOnCellTapListener(this);
    }

    private CompletableFuture<Void> loadSettingsAsync() {
        return CompletableFuture.runAsync(() -> {
            SharedPreferences prefs = getSharedPreferences("AblukaPref", MODE_PRIVATE);
            String difficulty = prefs.getString("difficulty", "Medium");
            showHints = prefs.getBoolean("showHints", true);
            vibrationEnabled = prefs.getBoolean("vibrationEnabled", true);

            // Zorluk ayarına göre AI derinliği belirleniyor
            if (difficulty.equals("Easy")) {
                aiDepth = 1;
            } else if (difficulty.equals("Hard")) {
                aiDepth = 4;
            } else { // Medium
                aiDepth = 2;
            }

            System.out.println("Zorluk seviyesi: " + difficulty + ", AI derinliği: " + aiDepth);
        });
    }

    private void initializeGame() {
        board = new GameBoard("black", "white");
        currentPlayer = board.getPlayer1();
        phase = 1;

        // UI'ı güncelle
        updateUI();

        // Oyun başladığında insan oyuncuya ilk hamle önerisi sun (eğer ipuçları açıksa)
        if (gameMode == GameMode.VS_AI && showHints) {
            calculateBestMoveForHuman();
        }
    }

    private void updateUI() {
        // Tahtayı güncelle
        gameBoardView.setBoard(board);

        // Sıra göstergesini güncelle
        String playerText = currentPlayer.equals(board.getPlayer1()) ? "⚫ Siyah" : "⚪ Beyaz";
        String phaseText = phase == 1 ? "🚀 Taş Hareketi" : "🧱 Blok Koyma";

        // AI düşünüyorsa özel bir gösterge göster
        if (gameMode == GameMode.VS_AI && currentPlayer.equals(board.getPlayer2()) && aiThinking) {
            turnIndicatorText.setText("🤖 AI düşünüyor...");
            turnIndicatorText.setBackgroundResource(R.drawable.blue_gradient_background);
        } else {
            turnIndicatorText.setText("Sıra: " + playerText + " - " + phaseText);
            turnIndicatorText.setBackgroundResource(R.drawable.red_gradient_background);
        }

        // Hamle önerisi panelini güncelle
        if (showHints && gameMode == GameMode.VS_AI) {
            moveAdviceLayout.setVisibility(View.VISIBLE);

            if (currentPlayer.equals(board.getPlayer1())) {
                moveAdviceLayout.setVisibility(View.VISIBLE);

                if (calculatingHumanMove) {
                    moveAdviceText.setText("En iyi hamle hesaplanıyor...");
                } else {
                    moveAdviceText.setText(humanMoveAdvice.isEmpty() ?
                            "Hamle önerisi bekleniyor..." : humanMoveAdvice);
                }
            } else {
                moveAdviceLayout.setVisibility(View.GONE);
            }
        } else {
            moveAdviceLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCellTap(int x, int y) {
        // AI turu sırasında dokunuşları engelle
        if (gameMode == GameMode.VS_AI && currentPlayer.equals(board.getPlayer2())) return;

        // Oyun bitmiş mi kontrol et
        if (isGameOver()) return;

        if (phase == 1) {
            // Taş hareketi fazı
            BoardPosition currentPos = board.getPieces().get(currentPlayer).get(0);

            if (board.isEmpty(x, y)) {
                int dx = Math.abs(x - currentPos.getX());
                int dy = Math.abs(y - currentPos.getY());

                // Geçerli hamle kontrolü: Yatay, dikey veya çapraz olarak bir birim hareket
                if (dx <= 1 && dy <= 1 && !(dx == 0 && dy == 0)) {
                    history.add(createGameState());
                    board.movePiece(currentPlayer, currentPos, new BoardPosition(x, y));
                    moveHistory.add(new GameMove(currentPos, new BoardPosition(x, y), new BoardPosition(-1, -1)));
                    phase = 2; // Engel yerleştirme fazına geç

                    // Taş hareketi için titreşim
                    vibrateMove();

                    // UI'ı güncelle
                    updateUI();
                }
            }
        } else if (phase == 2) {
            // Engel yerleştirme fazı
            if (board.isEmpty(x, y)) {
                history.add(createGameState());
                board.placeBlock(x, y);

                // Engel sayısını güncelle (eğer sınırlı engel varsa)
                Map<String, Integer> blocksLeft = board.getBlocksLeft();
                if (blocksLeft.containsKey(currentPlayer) && blocksLeft.get(currentPlayer) > 0) {
                    blocksLeft.put(currentPlayer, blocksLeft.get(currentPlayer) - 1);
                }

                // Hareket geçmişini güncelle
                GameMove last = moveHistory.get(moveHistory.size() - 1);
                moveHistory.set(moveHistory.size() - 1,
                        new GameMove(last.getFrom(), last.getTo(), new BoardPosition(x, y)));

                phase = 1; // Taş hareketi fazına geri dön
                switchTurn(); // Sırayı diğer oyuncuya geçir

                // UI'ı güncelle
                updateUI();

                // Oyun sonu kontrolü
                if (isGameOver()) {
                    String winner = determineWinner();
                    updateStats(winner);
                    storeMatchHistory();

                    // 2 saniye sonra EndActivity'ye geç
                    new Handler().postDelayed(() -> {
                        Intent intent = new Intent(GameActivity.this, EndActivity.class);
                        intent.putExtra("WINNER", winner);
                        intent.putExtra("GAME_MODE", gameMode.name());
                        startActivity(intent);
                        finish();
                    }, 2000);
                }
            }
        }
    }

    private GameState createGameState() {
        return new GameState(board.clone(), currentPlayer, phase);
    }

    private boolean isGameOver() {
        if (phase != 1) return false; // Sadece taş hareketi fazında kontrol et

        // Mevcut oyuncunun hareket edebileceği yer yoksa oyun biter
        BoardPosition currentPos = board.getPieces().get(currentPlayer).get(0);
        boolean gameOver = board.getAvailableMoves(currentPos).isEmpty();

        // Oyun bittiyse titreşim ver
        if (gameOver) {
            vibrateGameEnd();
        }

        return gameOver;
    }

    private String determineWinner() {
        // Mevcut oyuncu hareket edemiyorsa, rakip kazanır
        if (board.getAvailableMoves(board.getPieces().get(currentPlayer).get(0)).isEmpty()) {
            return currentPlayer.equals(board.getPlayer1()) ? board.getPlayer2() : board.getPlayer1();
        }

        // Eğer player1 hareket edemiyorsa, player2 kazanır
        if (board.getAvailableMoves(board.getPieces().get(board.getPlayer1()).get(0)).isEmpty()) {
            return board.getPlayer2();
        }

        // Eğer player2 hareket edemiyorsa, player1 kazanır
        if (board.getAvailableMoves(board.getPieces().get(board.getPlayer2()).get(0)).isEmpty()) {
            return board.getPlayer1();
        }

        // Varsayılan olarak son hamleyi yapan oyuncu kazanır
        return currentPlayer.equals(board.getPlayer1()) ? board.getPlayer2() : board.getPlayer1();
    }

    private void switchTurn() {
        currentPlayer = currentPlayer.equals(board.getPlayer1()) ? board.getPlayer2() : board.getPlayer1();

        // AI'ın sırası geldiyse, AI turunu başlat
        if (gameMode == GameMode.VS_AI && currentPlayer.equals(board.getPlayer2())) {
            new Handler().postDelayed(this::aiTurn, 600);
        }
        // İnsan oyuncunun sırası geldiyse ve AI modundaysak ve ipuçları açıksa, hamle önerisi hesapla
        else if (gameMode == GameMode.VS_AI && currentPlayer.equals(board.getPlayer1()) && showHints) {
            new Handler().postDelayed(this::calculateBestMoveForHuman, 600);
        }
    }

    private void aiTurn() {
        // Oyun bitmiş mi kontrol et
        if (isGameOver()) {
            String winner = determineWinner();
            updateStats(winner);
            storeMatchHistory();

            // 2 saniye sonra EndActivity'ye geç
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(GameActivity.this, EndActivity.class);
                intent.putExtra("WINNER", winner);
                intent.putExtra("GAME_MODE", gameMode.name());
                startActivity(intent);
                finish();
            }, 2000);
            return;
        }

        // AI düşünmeye başladığını göster
        aiThinking = true;
        updateUI();

        // Zaman aşımı için timer ayarla - 8 saniye
        if (calculationTimeoutTimer != null) {
            calculationTimeoutTimer.cancel();
        }
        calculationTimeoutTimer = new Timer();
        calculationTimeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (aiThinking) {
                    mainHandler.post(() -> makeSimpleAIMove());
                }
            }
        }, 8000);

        // AI hesaplamasını arka planda yap
        new Thread(() -> {
            try {
                // Hard modda ise, düşünme animasyonunun görünmesi için kısa bir gecikme ekle
                if (aiDepth >= 4) {
                    Thread.sleep(500);
                }

                // AI hesaplamasını yap
                System.out.println("AI derinliği: " + aiDepth);
                Map<String, Object> action = AblukaAI.computeBestMove(
                        board,
                        board.getPlayer2(),
                        board.getPlayer1(),
                        aiDepth > 3 ? 3 : aiDepth // Derinliği sınırla
                );

                // Timer'ı iptal et
                calculationTimeoutTimer.cancel();

                // AI'ın geçerli bir hamle döndürdüğünden emin ol
                if (action.isEmpty() || action.get("bestMove") == null || action.get("bestBlock") == null) {
                    throw new Exception("AI geçerli bir hamle bulamadı");
                }

                // JSON'dan BoardPosition nesnelerini oluştur
                BoardPosition bestMove = BoardPosition.fromJson((Map<String, Object>) action.get("bestMove"));
                BoardPosition bestBlock = BoardPosition.fromJson((Map<String, Object>) action.get("bestBlock"));

                BoardPosition from = board.getPieces().get(board.getPlayer2()).get(0);
                GameMove move = new GameMove(from, bestMove, bestBlock);

                // UI thread'inde hamleyi uygula
                mainHandler.post(() -> {
                    aiThinking = false; // AI düşünmeyi bitirdi
                    history.add(createGameState());

                    // Taş hareketini uygula
                    board.movePiece(board.getPlayer2(), move.getFrom(), move.getTo());

                    // Taş hareketi için titreşim
                    vibrateMove();

                    // Engel yerleştirmeyi uygula
                    board.placeBlock(move.getBlock().getX(), move.getBlock().getY());

                    // Engel sayısını güncelle (eğer sınırlı engel varsa)
                    Map<String, Integer> blocksLeft = board.getBlocksLeft();
                    if (blocksLeft.containsKey(board.getPlayer2()) && blocksLeft.get(board.getPlayer2()) > 0) {
                        blocksLeft.put(board.getPlayer2(), blocksLeft.get(board.getPlayer2()) - 1);
                    }

                    // Hareket geçmişini güncelle
                    moveHistory.add(move);

                    // Sırayı insan oyuncuya geçir
                    currentPlayer = board.getPlayer1();

                    // UI'ı güncelle
                    updateUI();

                    // Oyun sonu kontrolü
                    if (isGameOver()) {
                        String winner = determineWinner();
                        updateStats(winner);
                        storeMatchHistory();

                        // 2 saniye sonra EndActivity'ye geç
                        new Handler().postDelayed(() -> {
                            Intent intent = new Intent(GameActivity.this, EndActivity.class);
                            intent.putExtra("WINNER", winner);
                            intent.putExtra("GAME_MODE", gameMode.name());
                            startActivity(intent);
                            finish();
                        }, 2000);
                    } else {
                        // İnsan oyuncunun sırası geldiyse ve ipuçları açıksa, hamle önerisi hesapla
                        if (showHints) {
                            calculateBestMoveForHuman();
                        }
                    }
                });
            } catch (Exception e) {
                System.out.println("AI hatası: " + e.getMessage());

                // Timer'ı iptal et
                calculationTimeoutTimer.cancel();

                // UI thread'inde basit bir hamle yap
                mainHandler.post(() -> makeSimpleAIMove());
            }
        }).start();
    }

    private void makeSimpleAIMove() {
        try {
            BoardPosition currentPos = board.getPieces().get(board.getPlayer2()).get(0);
            List<BoardPosition> availableMoves = board.getAvailableMoves(currentPos);

            if (!availableMoves.isEmpty()) {
                // Mevcut konuma en yakın hamleyi seç
                BoardPosition bestMove = availableMoves.get(0);
                int minDistance = 999;

                for (BoardPosition move : availableMoves) {
                    // Merkeze olan uzaklığı hesapla (3,3 merkez kabul edilirse)
                    int distance = Math.abs(move.getX() - 3) + Math.abs(move.getY() - 3);
                    if (distance < minDistance) {
                        minDistance = distance;
                        bestMove = move;
                    }
                }

                // Tahtanın geçici bir kopyasını oluştur
                GameBoard tempBoard = board.clone();

                // Geçici tahtada hamleyi uygula
                tempBoard.movePiece(board.getPlayer2(), currentPos, bestMove);

                // Boş bir hücre bul (engel için)
                BoardPosition bestBlock = new BoardPosition(0, 0);

                // İnsan oyuncunun pozisyonu
                BoardPosition humanPos = board.getPieces().get(board.getPlayer1()).get(0);

                // İnsan oyuncuya yakın bir engel pozisyonu bul
                int bestBlockDistance = 999;
                for (int x = 0; x < 7; x++) {
                    for (int y = 0; y < 7; y++) {
                        if (tempBoard.isEmpty(x, y)) {
                            int distance = Math.abs(x - humanPos.getX()) + Math.abs(y - humanPos.getY());
                            if (distance < bestBlockDistance && distance > 0) {
                                bestBlockDistance = distance;
                                bestBlock = new BoardPosition(x, y);
                            }
                        }
                    }
                }

                aiThinking = false; // AI düşünmeyi bitirdi
                history.add(createGameState());

                // Taş hareketini uygula
                board.movePiece(board.getPlayer2(), currentPos, bestMove);

                // Taş hareketi için titreşim
                vibrateMove();

                // Engel yerleştirmeyi uygula
                board.placeBlock(bestBlock.getX(), bestBlock.getY());

                // Engel sayısını güncelle (eğer sınırlı engel varsa)
                Map<String, Integer> blocksLeft = board.getBlocksLeft();
                if (blocksLeft.containsKey(board.getPlayer2()) && blocksLeft.get(board.getPlayer2()) > 0) {
                    blocksLeft.put(board.getPlayer2(), blocksLeft.get(board.getPlayer2()) - 1);
                }

                // Hareket geçmişini güncelle
                moveHistory.add(new GameMove(currentPos, bestMove, bestBlock));

                // Sırayı insan oyuncuya geçir
                currentPlayer = board.getPlayer1();

                // UI'ı güncelle
                updateUI();

                // Oyun sonu kontrolü
                if (isGameOver()) {
                    String winner = determineWinner();
                    updateStats(winner);
                    storeMatchHistory();

                    // 2 saniye sonra EndActivity'ye geç
                    new Handler().postDelayed(() -> {
                        Intent intent = new Intent(GameActivity.this, EndActivity.class);
                        intent.putExtra("WINNER", winner);
                        intent.putExtra("GAME_MODE", gameMode.name());
                        startActivity(intent);
                        finish();
                    }, 2000);
                } else {
                    // İnsan oyuncunun sırası geldiyse ve ipuçları açıksa, hamle önerisi hesapla
                    if (showHints) {
                        calculateBestMoveForHuman();
                    }
                }
            } else {
                // Eğer geçerli hamle yoksa, AI kaybetti
                aiThinking = false;
                updateUI();

                String winner = board.getPlayer1(); // İnsan oyuncu kazandı
                updateStats(winner);
                storeMatchHistory();

                // 2 saniye sonra EndActivity'ye geç
                new Handler().postDelayed(() -> {
                    Intent intent = new Intent(GameActivity.this, EndActivity.class);
                    intent.putExtra("WINNER", winner);
                    intent.putExtra("GAME_MODE", gameMode.name());
                    startActivity(intent);
                    finish();
                }, 2000);
            }
        } catch (Exception e) {
            System.out.println("Basit AI hamle hatası: " + e.getMessage());

            // AI düşünmeyi bitirdi (hata ile)
            aiThinking = false;
            updateUI();

            // AI hata verirse, insan oyuncuyu kazanan olarak belirle
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(GameActivity.this, EndActivity.class);
                intent.putExtra("WINNER", board.getPlayer1());
                intent.putExtra("GAME_MODE", gameMode.name());
                startActivity(intent);
                finish();
            }, 2000);
        }
    }

    private void calculateBestMoveForHuman() {
        // İpuçları kapalıysa veya AI modu değilse hesaplama yapma
        if (!showHints || !currentPlayer.equals(board.getPlayer1()) || gameMode != GameMode.VS_AI) return;

        calculatingHumanMove = true;
        humanMoveAdvice = "🔍 En iyi hamle hesaplanıyor...";
        updateUI();

        // Zaman aşımı için timer ayarla - 8 saniye
        if (calculationTimeoutTimer != null) {
            calculationTimeoutTimer.cancel();
        }
        calculationTimeoutTimer = new Timer();
        calculationTimeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (calculatingHumanMove) {
                    mainHandler.post(() -> {
                        calculatingHumanMove = false;
                        humanMoveAdvice = "⚠️ Hesaplama zaman aşımına uğradı. Basit bir öneri sunuluyor...";
                        updateUI();

                        // Basit bir öneri sun
                        provideFallbackSuggestion();
                    });
                }
            }
        }, 8000);

        // Hamle önerisini arka planda hesapla
        new Thread(() -> {
            try {
                // İnsan oyuncu için en iyi hamleyi hesapla
                Map<String, Object> result = HumanMoveAdvisor.getBestMoveForHuman(
                        board,
                        board.getPlayer1(), // İnsan oyuncu
                        board.getPlayer2(), // AI
                        aiDepth > 3 ? 3 : aiDepth // Derinliği sınırla
                );

                // Timer'ı iptal et
                calculationTimeoutTimer.cancel();

                if ((Boolean) result.get("success")) {
                    // JSON'dan BoardPosition nesnelerini oluştur
                    BoardPosition bestMove = BoardPosition.fromJson((Map<String, Object>) result.get("bestMove"));
                    BoardPosition bestBlock = BoardPosition.fromJson((Map<String, Object>) result.get("bestBlock"));

                    // Değerlendirme puanını daha anlaşılır bir şekilde göster
                    Object moveAnalysis = result.get("bestMoveAnalysis");
                    String evaluationText;

                    if (moveAnalysis instanceof Number) {
                        // Sayısal değeri kalite seviyesine dönüştür
                        double score = ((Number) moveAnalysis).doubleValue();
                        if (score >= 10000) {
                            evaluationText = "Kazandıran hamle! 🏆";
                        } else if (score > 1000) {
                            evaluationText = "Çok güçlü hamle! 💪";
                        } else if (score > 500) {
                            evaluationText = "Güçlü hamle 💯";
                        } else if (score > 300) {
                            evaluationText = "İyi hamle 👍";
                        } else if (score > 100) {
                            evaluationText = "Makul hamle ✓";
                        } else {
                            evaluationText = "Kabul edilebilir hamle";
                        }
                    } else {
                        evaluationText = "Önerilen hamle";
                    }

                    final String finalEvaluationText = evaluationText;

                    mainHandler.post(() -> {
                        calculatingHumanMove = false;
                        humanMoveAdvice =
                                "💡 Öneri:\n"
                                        + "🎯 Taşınızı (" + bestMove.getX() + "," + bestMove.getY() + ") konumuna hareket ettirin\n"
                                        + "🧱 Engeli (" + bestBlock.getX() + "," + bestBlock.getY() + ") konumuna yerleştirin\n"
                                        + "💯 Değerlendirme: " + finalEvaluationText;
                        updateUI();
                    });
                } else {
                    mainHandler.post(() -> {
                        calculatingHumanMove = false;
                        humanMoveAdvice = "❓ Öneri hesaplanamadı";
                        updateUI();

                        // Basit bir öneri sun
                        provideFallbackSuggestion();
                    });
                }
            } catch (Exception e) {
                System.out.println("Hamle önerisi hatası: " + e.getMessage());

                // Timer'ı iptal et
                calculationTimeoutTimer.cancel();

                mainHandler.post(() -> {
                    calculatingHumanMove = false;
                    humanMoveAdvice = "❓ Öneri hesaplanamadı";
                    updateUI();

                    // Basit bir öneri sun
                    provideFallbackSuggestion();
                });
            }
        }).start();
    }

    private void provideFallbackSuggestion() {
        try {
            BoardPosition currentPos = board.getPieces().get(board.getPlayer1()).get(0);
            List<BoardPosition> availableMoves = board.getAvailableMoves(currentPos);

            if (!availableMoves.isEmpty()) {
                // Mevcut konuma en yakın hamleyi seç
                BoardPosition bestMove = availableMoves.get(0);
                int minDistance = 999;

                for (BoardPosition move : availableMoves) {
                    // Merkeze olan uzaklığı hesapla (3,3 merkez kabul edilirse)
                    int distance = Math.abs(move.getX() - 3) + Math.abs(move.getY() - 3);
                    if (distance < minDistance) {
                        minDistance = distance;
                        bestMove = move;
                    }
                }

                // Boş bir hücre bul (engel için)
                BoardPosition bestBlock = new BoardPosition(0, 0);
                GameBoard tempBoard = board.clone();
                tempBoard.movePiece(board.getPlayer1(), currentPos, bestMove);

                // Rakibin pozisyonu
                BoardPosition aiPos = board.getPieces().get(board.getPlayer2()).get(0);

                // Rakibe yakın bir engel pozisyonu bul
                int bestBlockDistance = 999;
                for (int x = 0; x < 7; x++) {
                    for (int y = 0; y < 7; y++) {
                        if (tempBoard.isEmpty(x, y)) {
                            int distance = Math.abs(x - aiPos.getX()) + Math.abs(y - aiPos.getY());
                            if (distance < bestBlockDistance && distance > 0) {
                                bestBlockDistance = distance;
                                bestBlock = new BoardPosition(x, y);
                            }
                        }
                    }
                }

                humanMoveAdvice =
                        "💡 Basit Öneri:\n"
                                + "🎯 Taşınızı (" + bestMove.getX() + "," + bestMove.getY() + ") konumuna hareket ettirin\n"
                                + "🧱 Engeli (" + bestBlock.getX() + "," + bestBlock.getY() + ") konumuna yerleştirin\n"
                                + "💯 Değerlendirme: Makul hamle ✓";
                updateUI();
            }
        } catch (Exception e) {
            System.out.println("Basit öneri hatası: " + e.getMessage());
        }
    }

    private void vibrateMove() {
        if (vibrationEnabled) {
            vibrationService.vibrateOnMove();
        }
    }

    private void vibrateGameEnd() {
        if (vibrationEnabled) {
            vibrationService.vibrateOnGameEnd();
        }
    }

    private void undoMove() {
        if (!history.isEmpty()) {
            GameState prev = history.remove(history.size() - 1);
            board = prev.getBoard();
            currentPlayer = prev.getCurrentPlayer();
            phase = prev.getPhase();

            // Eğer hareket geçmişi varsa ve geri alınan hamle son hamle ise, onu da kaldır
            if (!moveHistory.isEmpty() && phase == 1) {
                moveHistory.remove(moveHistory.size() - 1);
            }

            // UI'ı güncelle
            updateUI();

            // İnsan oyuncunun sırası ise ve AI modundaysak ve ipuçları açıksa, hamle önerisi hesapla
            if (gameMode == GameMode.VS_AI && currentPlayer.equals(board.getPlayer1()) && showHints) {
                new Handler().postDelayed(this::calculateBestMoveForHuman, 600);
            }
        }
    }

    private void resetGame() {
        initializeGame();
        history.clear();
        moveHistory.clear();
        humanMoveAdvice = "";
        aiThinking = false;
        calculatingHumanMove = false;
        updateUI();
    }

    private void storeMatchHistory() {
        SharedPreferences prefs = getSharedPreferences("AblukaPref", MODE_PRIVATE);
        List<Map<String, Object>> matchesList = new ArrayList<>();

        String jsonStr = prefs.getString("matches", null);
        if (jsonStr != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
            matchesList = gson.fromJson(jsonStr, type);
        }

        Map<String, Object> matchData = new HashMap<>();
        matchData.put("matchId", "Match " + (matchesList.size() + 1));
        matchData.put("mode", gameMode == GameMode.VS_AI ? "vsAI" : "twoPlayer");
        matchData.put("moves", String.valueOf(moveHistory.size()));
        matchData.put("winner", determineWinner());

        List<Map<String, Object>> moveHistoryJson = new ArrayList<>();
        for (GameMove move : moveHistory) {
            moveHistoryJson.add(move.toJson());
        }
        matchData.put("moveHistory", moveHistoryJson);
        matchData.put("player1", board.getPlayer1());
        matchData.put("player2", board.getPlayer2());

        matchesList.add(0, matchData); // En başa ekle (en yeni)

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("matches", new Gson().toJson(matchesList));
        editor.apply();
    }

    private void updateStats(String winner) {
        SharedPreferences prefs = getSharedPreferences("AblukaPref", MODE_PRIVATE);
        int total = prefs.getInt("totalGames", 0);
        int p1 = prefs.getInt("player1Wins", 0);
        int p2 = prefs.getInt("player2Wins", 0);

        total++;
        if (winner.equals(board.getPlayer1())) {
            p1++;
        } else {
            p2++;
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("totalGames", total);
        editor.putInt("player1Wins", p1);
        editor.putInt("player2Wins", p2);
        editor.apply();
    }

    // GameState sınıfı - oyun durumunu saklamak için
    private static class GameState {
        private final GameBoard board;
        private final String currentPlayer;
        private final int phase;

        public GameState(GameBoard board, String currentPlayer, int phase) {
            this.board = board;
            this.currentPlayer = currentPlayer;
            this.phase = phase;
        }

        public GameBoard getBoard() {
            return board;
        }

        public String getCurrentPlayer() {
            return currentPlayer;
        }

        public int getPhase() {
            return phase;
        }
    }
}