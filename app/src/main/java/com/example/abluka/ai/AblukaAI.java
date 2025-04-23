package com.example.abluka.ai;

import com.example.abluka.models.BoardPosition;
import com.example.abluka.models.GameBoard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ArrayList;

public class AblukaAI {
    
    public static Map<String, Object> computeBestMove(GameBoard board, String aiPlayer, String humanPlayer, int depth) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // AI'ın mevcut pozisyonu
            BoardPosition currentPos = board.getPieces().get(aiPlayer).get(0);
            
            // Mevcut pozisyondan yapılabilecek hamleler
            List<BoardPosition> availableMoves = board.getAvailableMoves(currentPos);
            
            if (availableMoves.isEmpty()) {
                throw new Exception("Yapılabilecek hamle yok");
            }
            
            BoardPosition bestMove = null;
            BoardPosition bestBlock = null;
            int bestScore = Integer.MIN_VALUE;
            
            // Her hamle için en iyi skoru hesapla
            for (BoardPosition move : availableMoves) {
                // Tahtanın geçici bir kopyasını oluştur
                GameBoard tempBoard = board.clone();
                
                // Hamleyi uygula
                tempBoard.movePiece(aiPlayer, currentPos, move);
                
                // Boş hücreleri bul (engel için)
                for (int x = 0; x < 7; x++) {
                    for (int y = 0; y < 7; y++) {
                        if (tempBoard.isEmpty(x, y)) {
                            // Engeli yerleştir
                            GameBoard blockBoard = tempBoard.clone();
                            blockBoard.placeBlock(x, y);
                            
                            // Skoru hesapla
                            int score = minimax(blockBoard, depth - 1, false, Integer.MIN_VALUE, Integer.MAX_VALUE, aiPlayer, humanPlayer);
                            
                            // Daha iyi bir skor bulunduysa güncelle
                            if (score > bestScore) {
                                bestScore = score;
                                bestMove = move;
                                bestBlock = new BoardPosition(x, y);
                            }
                        }
                    }
                }
            }
            
            // Eğer hala en iyi hamle bulunamadıysa (çok nadir bir durum)
            if (bestMove == null) {
                // Rastgele bir hamle seç
                Random random = new Random();
                bestMove = availableMoves.get(random.nextInt(availableMoves.size()));
                
                // Rastgele bir boş hücre bul
                List<BoardPosition> emptyPositions = new ArrayList<>();
                for (int x = 0; x < 7; x++) {
                    for (int y = 0; y < 7; y++) {
                        if (board.isEmpty(x, y) && (x != bestMove.getX() || y != bestMove.getY())) {
                            emptyPositions.add(new BoardPosition(x, y));
                        }
                    }
                }
                
                if (!emptyPositions.isEmpty()) {
                    bestBlock = emptyPositions.get(random.nextInt(emptyPositions.size()));
                } else {
                    // Eğer boş hücre yoksa (çok nadir)
                    bestBlock = new BoardPosition(0, 0);
                }
            }
            
            // Sonucu döndür
            result.put("bestMove", bestMove.toJson());
            result.put("bestBlock", bestBlock.toJson());
            result.put("score", bestScore);
            
        } catch (Exception e) {
            System.out.println("AI hesaplama hatası: " + e.getMessage());
            result.clear();
        }
        
        return result;
    }
    
    private static int minimax(GameBoard board, int depth, boolean isMaximizing, int alpha, int beta, String aiPlayer, String humanPlayer) {
        // Oyun sonu veya maksimum derinlik kontrolü
        if (depth == 0 || isGameOver(board, isMaximizing ? humanPlayer : aiPlayer)) {
            return evaluateBoard(board, aiPlayer, humanPlayer);
        }
        
        if (isMaximizing) {
            // AI'ın turu
            int maxScore = Integer.MIN_VALUE;
            BoardPosition currentPos = board.getPieces().get(aiPlayer).get(0);
            List<BoardPosition> availableMoves = board.getAvailableMoves(currentPos);
            
            for (BoardPosition move : availableMoves) {
                GameBoard tempBoard = board.clone();
                tempBoard.movePiece(aiPlayer, currentPos, move);
                
                for (int x = 0; x < 7; x++) {
                    for (int y = 0; y < 7; y++) {
                        if (tempBoard.isEmpty(x, y)) {
                            GameBoard blockBoard = tempBoard.clone();
                            blockBoard.placeBlock(x, y);
                            
                            int score = minimax(blockBoard, depth - 1, false, alpha, beta, aiPlayer, humanPlayer);
                            maxScore = Math.max(maxScore, score);
                            alpha = Math.max(alpha, score);
                            
                            if (beta <= alpha) {
                                break; // Beta kesme
                            }
                        }
                    }
                    
                    if (beta <= alpha) {
                        break; // Beta kesme
                    }
                }
            }
            
            return maxScore;
        } else {
            // İnsan oyuncunun turu
            int minScore = Integer.MAX_VALUE;
            BoardPosition currentPos = board.getPieces().get(humanPlayer).get(0);
            List<BoardPosition> availableMoves = board.getAvailableMoves(currentPos);
            
            for (BoardPosition move : availableMoves) {
                GameBoard tempBoard = board.clone();
                tempBoard.movePiece(humanPlayer, currentPos, move);
                
                for (int x = 0; x < 7; x++) {
                    for (int y = 0; y < 7; y++) {
                        if (tempBoard.isEmpty(x, y)) {
                            GameBoard blockBoard = tempBoard.clone();
                            blockBoard.placeBlock(x, y);
                            
                            int score = minimax(blockBoard, depth - 1, true, alpha, beta, aiPlayer, humanPlayer);
                            minScore = Math.min(minScore, score);
                            beta = Math.min(beta, score);
                            
                            if (beta <= alpha) {
                                break; // Alpha kesme
                            }
                        }
                    }
                    
                    if (beta <= alpha) {
                        break; // Alpha kesme
                    }
                }
            }
            
            return minScore;
        }
    }
    
    private static boolean isGameOver(GameBoard board, String currentPlayer) {
        BoardPosition currentPos = board.getPieces().get(currentPlayer).get(0);
        return board.getAvailableMoves(currentPos).isEmpty();
    }
    
    private static int evaluateBoard(GameBoard board, String aiPlayer, String humanPlayer) {
        BoardPosition aiPos = board.getPieces().get(aiPlayer).get(0);
        BoardPosition humanPos = board.getPieces().get(humanPlayer).get(0);
        
        // AI'ın yapabileceği hamle sayısı
        int aiMoves = board.getAvailableMoves(aiPos).size();
        
        // İnsan oyuncunun yapabileceği hamle sayısı
        int humanMoves = board.getAvailableMoves(humanPos).size();
        
        // Eğer AI hareket edemiyorsa, çok kötü bir durum
        if (aiMoves == 0) {
            return -10000;
        }
        
        // Eğer insan oyuncu hareket edemiyorsa, çok iyi bir durum
        if (humanMoves == 0) {
            return 10000;
        }
        
        // Merkeze olan uzaklık (merkez daha değerli)
        int aiDistanceToCenter = Math.abs(aiPos.getX() - 3) + Math.abs(aiPos.getY() - 3);
        int humanDistanceToCenter = Math.abs(humanPos.getX() - 3) + Math.abs(humanPos.getY() - 3);
        
        // Skoru hesapla
        return (aiMoves * 100) - (humanMoves * 100) + (humanDistanceToCenter * 10) - (aiDistanceToCenter * 10);
    }
}
