package com.example.abluka.models;

import java.util.HashMap;
import java.util.Map;

public class GameMove {
    private final BoardPosition from;
    private final BoardPosition to;
    private final BoardPosition block;
    
    public GameMove(BoardPosition from, BoardPosition to, BoardPosition block) {
        this.from = from;
        this.to = to;
        this.block = block;
    }
    
    public BoardPosition getFrom() {
        return from;
    }
    
    public BoardPosition getTo() {
        return to;
    }
    
    public BoardPosition getBlock() {
        return block;
    }
    
    // GameMove'u JSON'a dönüştür
    public Map<String, Object> toJson() {
        Map<String, Object> json = new HashMap<>();
        json.put("from", from.toJson());
        json.put("to", to.toJson());
        json.put("block", block.toJson());
        return json;
    }
    
    // JSON'dan GameMove oluştur
    public static GameMove fromJson(Map<String, Object> json) {
        BoardPosition from = BoardPosition.fromJson((Map<String, Object>) json.get("from"));
        BoardPosition to = BoardPosition.fromJson((Map<String, Object>) json.get("to"));
        BoardPosition block = BoardPosition.fromJson((Map<String, Object>) json.get("block"));
        return new GameMove(from, to, block);
    }
}
