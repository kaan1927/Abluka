package com.example.abluka.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.abluka.R;

import java.util.List;
import java.util.Map;

public class RecentGamesAdapter extends RecyclerView.Adapter<RecentGamesAdapter.ViewHolder> {
    private final Context context;
    private List<Map<String, Object>> matches;
    private OnItemClickListener listener;
    
    public interface OnItemClickListener {
        void onItemClick(int position);
    }
    
    public RecentGamesAdapter(Context context, List<Map<String, Object>> matches) {
        this.context = context;
        this.matches = matches;
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    
    public void updateMatches(List<Map<String, Object>> matches) {
        this.matches = matches;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recent_game, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> match = matches.get(position);
        
        String matchId = (String) match.get("matchId");
        String mode = (String) match.get("mode");
        String moves = (String) match.get("moves");
        String winner = (String) match.get("winner");
        
        String winnerText = "black".equals(winner) ? "Siyah" : "Beyaz";
        String modeText = "vsAI".equals(mode) ? "Yapay Zekaya Karşı" : "İki Oyunculu";
        
        holder.matchIdText.setText(matchId != null ? matchId : "Bilinmeyen Maç");
        holder.modeText.setText(modeText);
        holder.movesText.setText(moves + " Hamle");
        holder.winnerText.setText(winnerText);
        
        // Kazanan rengini ayarla
        if ("black".equals(winner)) {
            holder.winnerText.setBackgroundResource(R.drawable.black_winner_background);
        } else {
            holder.winnerText.setBackgroundResource(R.drawable.white_winner_background);
        }
        
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onItemClick(holder.getAdapterPosition());
                }
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return matches.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView matchIdText;
        TextView modeText;
        TextView movesText;
        TextView winnerText;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            matchIdText = itemView.findViewById(R.id.match_id_text);
            modeText = itemView.findViewById(R.id.mode_text);
            movesText = itemView.findViewById(R.id.moves_text);
            winnerText = itemView.findViewById(R.id.winner_text);
        }
    }
}
