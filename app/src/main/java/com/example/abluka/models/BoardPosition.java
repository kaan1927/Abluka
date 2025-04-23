package com.example.abluka.models;

import java.util.HashMap;
import java.util.Map;

public class BoardPosition {
    private final int x;
    private final int y;
    
    public BoardPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    // JSON'dan BoardPosition oluştur
    public static BoardPosition fromJson(Map<String, Object> json) {
        int x = ((Number) json.get("x")).intValue();
        int y = ((Number) json.get("y")).intValue();
        return new BoardPosition(x, y);
    }
    
    // BoardPosition'ı JSON'a dönüştür
    public Map<String, Object> toJson() {
        Map<String, Object> json = new HashMap<>();
        json.put("x", x);
        json.put("y", y);
        return json;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        BoardPosition that = (BoardPosition) obj;
        return x == that.x && y == that.y;
    }
    
    @Override
    public int hashCode() {
        return 31 * x + y;
    }
    
    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
