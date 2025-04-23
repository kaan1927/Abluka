package com.example.abluka.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameBoard {
    private final String player1;
    private final String player2;
    private final Map<String, List<BoardPosition>> pieces;
    private final boolean[][] blocks;
    private final Map<String, Integer> blocksLeft;
    
    public GameBoard(String player1, String player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.pieces = new HashMap<>();
        this.blocks = new boolean[7][7];
        this.blocksLeft = new HashMap<>();
        
        // Oyuncuların başlangıç pozisyonlarını ayarla
        List<BoardPosition> player1Pieces = new ArrayList<>();
        player1Pieces.add(new BoardPosition(3, 6)); // Siyah oyuncu sol ortada başlar
        pieces.put(player1, player1Pieces);
        
        List<BoardPosition> player2Pieces = new ArrayList<>();
        player2Pieces.add(new BoardPosition(6, 3)); // Beyaz oyuncu sağ ortada başlar
        pieces.put(player2, player2Pieces);
        
        // Engelleri temizle
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                blocks[i][j] = false;
            }
        }
    }
    
    // Derin kopya oluşturmak için
    public GameBoard clone() {
        GameBoard clone = new GameBoard(player1, player2);
        
        // Taşları kopyala
        clone.pieces.clear();
        for (Map.Entry<String, List<BoardPosition>> entry : pieces.entrySet()) {
            List<BoardPosition> piecesCopy = new ArrayList<>();
            for (BoardPosition pos : entry.getValue()) {
                piecesCopy.add(new BoardPosition(pos.getX(), pos.getY()));
            }
            clone.pieces.put(entry.getKey(), piecesCopy);
        }
        
        // Engelleri kopyala
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                clone.blocks[i][j] = blocks[i][j];
            }
        }
        
        // Kalan engelleri kopyala
        clone.blocksLeft.putAll(blocksLeft);
        
        return clone;
    }
    
    // Bir hücrenin boş olup olmadığını kontrol et
    public boolean isEmpty(int x, int y) {
        // Sınırlar dışında mı?
        if (x < 0 || x >= 7 || y < 0 || y >= 7) {
            return false;
        }
        
        // Engel var mı?
        if (blocks[x][y]) {
            return false;
        }
        
        // Taş var mı?
        for (List<BoardPosition> playerPieces : pieces.values()) {
            for (BoardPosition pos : playerPieces) {
                if (pos.getX() == x && pos.getY() == y) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    // Bir taşı hareket ettir
    public void movePiece(String player, BoardPosition from, BoardPosition to) {
        List<BoardPosition> playerPieces = pieces.get(player);
        if (playerPieces != null) {
            for (int i = 0; i < playerPieces.size(); i++) {
                BoardPosition pos = playerPieces.get(i);
                if (pos.getX() == from.getX() && pos.getY() == from.getY()) {
                    playerPieces.set(i, to);
                    break;
                }
            }
        }
    }
    
    // Engel yerleştir
    public void placeBlock(int x, int y) {
        if (x >= 0 && x < 7 && y >= 0 && y < 7) {
            blocks[x][y] = true;
        }
    }
    
    // Bir taşın yapabileceği hamleleri bul
    public List<BoardPosition> getAvailableMoves(BoardPosition pos) {
        List<BoardPosition> moves = new ArrayList<>();
        
        // 8 yöne hareket kontrolü (yatay, dikey, çapraz)
        int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};
        
        for (int i = 0; i < 8; i++) {
            int newX = pos.getX() + dx[i];
            int newY = pos.getY() + dy[i];
            
            if (isEmpty(newX, newY)) {
                moves.add(new BoardPosition(newX, newY));
            }
        }
        
        return moves;
    }
    
    // Getter metodları
    public String getPlayer1() {
        return player1;
    }
    
    public String getPlayer2() {
        return player2;
    }
    
    public Map<String, List<BoardPosition>> getPieces() {
        return pieces;
    }
    
    public boolean[][] getBlocks() {
        return blocks;
    }
    
    public Map<String, Integer> getBlocksLeft() {
        return blocksLeft;
    }
}
