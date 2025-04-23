package com.example.abluka.models;

import java.util.List;
import java.util.Map;

public class Match {
    private String matchId;
    private String mode;
    private String moves;
    private String winner;
    private List<Map<String, Object>> moveHistory;
    private String player1;
    private String player2;
    
    public Match(String matchId, String mode, String moves, String winner, 
                 List<Map<String, Object>> moveHistory, String player1, String player2) {
        this.matchId = matchId;
        this.mode = mode;
        this.moves = moves;
        this.winner = winner;
        this.moveHistory = moveHistory;
        this.player1 = player1;
        this.player2 = player2;
    }
    
    // Getter ve Setter metodları
    public String getMatchId() {
        return matchId;
    }
    
    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }
    
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
    
    public String getMoves() {
        return moves;
    }
    
    public void setMoves(String moves) {
        this.moves = moves;
    }
    
    public String getWinner() {
        return winner;
    }
    
    public void setWinner(String winner) {
        this.winner = winner;
    }
    
    public List<Map<String, Object>> getMoveHistory() {
        return moveHistory;
    }
    
    public void setMoveHistory(List<Map<String, Object>> moveHistory) {
        this.moveHistory = moveHistory;
    }
    
    public String getPlayer1() {
        return player1;
    }
    
    public void setPlayer1(String player1) {
        this.player1 = player1;
    }
    
    public String getPlayer2() {
        return player2;
    }
    
    public void setPlayer2(String player2) {
        this.player2 = player2;
    }
}
