package com.example.abluka.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.abluka.models.BoardPosition;
import com.example.abluka.models.GameBoard;

import java.util.List;

public class GameBoardView extends View {
    private GameBoard board;
    private Paint gridPaint;
    private Paint blackPiecePaint;
    private Paint whitePiecePaint;
    private Paint blockPaint;
    private Paint highlightPaint;
    private float cellSize;
    private OnCellTapListener listener;
    
    public interface OnCellTapListener {
        void onCellTap(int x, int y);
    }
    
    public GameBoardView(Context context) {
        super(context);
        init();
    }
    
    public GameBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public GameBoardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        gridPaint = new Paint();
        gridPaint.setColor(Color.BLACK);
        gridPaint.setStrokeWidth(2);
        gridPaint.setStyle(Paint.Style.STROKE);
        
        blackPiecePaint = new Paint();
        blackPiecePaint.setColor(Color.BLACK);
        blackPiecePaint.setStyle(Paint.Style.FILL);
        blackPiecePaint.setAntiAlias(true);
        
        whitePiecePaint = new Paint();
        whitePiecePaint.setColor(Color.WHITE);
        whitePiecePaint.setStyle(Paint.Style.FILL);
        whitePiecePaint.setAntiAlias(true);
        whitePiecePaint.setStrokeWidth(2);
        whitePiecePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        whitePiecePaint.setStrokeJoin(Paint.Join.ROUND);
        
        blockPaint = new Paint();
        blockPaint.setColor(Color.parseColor("#B22222")); // Kırmızı
        blockPaint.setStyle(Paint.Style.FILL);
        
        highlightPaint = new Paint();
        highlightPaint.setColor(Color.parseColor("#80FFC107")); // Sarı (yarı saydam)
        highlightPaint.setStyle(Paint.Style.FILL);
    }
    
    public void setBoard(GameBoard board) {
        this.board = board;
        invalidate(); // Yeniden çiz
    }
    
    public void setOnCellTapListener(OnCellTapListener listener) {
        this.listener = listener;
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int size = Math.min(width, height);
        
        cellSize = size / 7f;
        
        setMeasuredDimension(size, size);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (board == null) return;
        
        // Izgara çiz
        for (int i = 0; i <= 7; i++) {
            // Yatay çizgiler
            canvas.drawLine(0, i * cellSize, 7 * cellSize, i * cellSize, gridPaint);
            // Dikey çizgiler
            canvas.drawLine(i * cellSize, 0, i * cellSize, 7 * cellSize, gridPaint);
        }
        
        // Engelleri çiz
        boolean[][] blocks = board.getBlocks();
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                if (blocks[i][j]) {
                    float left = i * cellSize + cellSize * 0.1f;
                    float top = j * cellSize + cellSize * 0.1f;
                    float right = (i + 1) * cellSize - cellSize * 0.1f;
                    float bottom = (j + 1) * cellSize - cellSize * 0.1f;
                    
                    RectF rect = new RectF(left, top, right, bottom);
                    canvas.drawRect(rect, blockPaint);
                }
            }
        }
        
        // Taşları çiz
        for (String player : board.getPieces().keySet()) {
            List<BoardPosition> pieces = board.getPieces().get(player);
            Paint paint = player.equals(board.getPlayer1()) ? blackPiecePaint : whitePiecePaint;
            
            for (BoardPosition pos : pieces) {
                float cx = (pos.getX() + 0.5f) * cellSize;
                float cy = (pos.getY() + 0.5f) * cellSize;
                float radius = cellSize * 0.4f;
                
                canvas.drawCircle(cx, cy, radius, paint);
                
                // Beyaz taş için siyah kenar çiz
                if (player.equals(board.getPlayer2())) {
                    Paint strokePaint = new Paint();
                    strokePaint.setColor(Color.BLACK);
                    strokePaint.setStyle(Paint.Style.STROKE);
                    strokePaint.setStrokeWidth(2);
                    canvas.drawCircle(cx, cy, radius, strokePaint);
                }
            }
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && listener != null) {
            float x = event.getX();
            float y = event.getY();
            
            int boardX = (int) (x / cellSize);
            int boardY = (int) (y / cellSize);
            
            if (boardX >= 0 && boardX < 7 && boardY >= 0 && boardY < 7) {
                listener.onCellTap(boardX, boardY);
                return true;
            }
        }
        
        return super.onTouchEvent(event);
    }
}
