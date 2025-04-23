package com.example.abluka.activities;

import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.abluka.R;
import com.example.abluka.services.VibrationService;

public class VibrationTestActivity extends AppCompatActivity {
    private VibrationService vibrationService;
    private String testResult = "Henüz test edilmedi";
    private boolean vibrationEnabled = true;
    private boolean hasVibrator = false;
    private boolean hasAmplitudeControl = false;
    
    private TextView statusText;
    private TextView resultText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vibration_test);
        
        // Gradient arka plan ayarla
        ConstraintLayout mainLayout = findViewById(R.id.vibration_test_layout);
        mainLayout.setBackground(getResources().getDrawable(R.drawable.gradient_background));
        
        // View'ları başlat
        statusText = findViewById(R.id.status_text);
        resultText = findViewById(R.id.result_text);
        
        // Titreşim servisini başlat
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrationService = new VibrationService(vibrator);
        
        // Ayarları yükle
        loadSettings();
        
        // Titreşim yeteneklerini kontrol et
        checkVibrationCapabilities();
        
        // Test butonları
        Button simpleVibrationButton = findViewById(R.id.simple_vibration_button);
        Button patternVibrationButton = findViewById(R.id.pattern_vibration_button);
        Button amplitudeVibrationButton = findViewById(R.id.amplitude_vibration_button);
        Button gameMoveVibrationButton = findViewById(R.id.game_move_vibration_button);
        Button gameEndVibrationButton = findViewById(R.id.game_end_vibration_button);
        Button cancelVibrationButton = findViewById(R.id.cancel_vibration_button);
        
        simpleVibrationButton.setOnClickListener(v -> testSimpleVibration());
        patternVibrationButton.setOnClickListener(v -> testPatternVibration());
        amplitudeVibrationButton.setOnClickListener(v -> testAmplitudeVibration());
        gameMoveVibrationButton.setOnClickListener(v -> testGameMoveVibration());
        gameEndVibrationButton.setOnClickListener(v -> testGameEndVibration());
        cancelVibrationButton.setOnClickListener(v -> cancelVibration());
        
        // Amplitude butonunu devre dışı bırak eğer desteklenmiyorsa
        amplitudeVibrationButton.setEnabled(hasAmplitudeControl);
    }
    
    private void loadSettings() {
        vibrationEnabled = vibrationService.isVibrationEnabled();
        updateStatusText();
    }
    
    private void checkVibrationCapabilities() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            hasVibrator = vibrator.hasVibrator();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                hasAmplitudeControl = vibrator.hasAmplitudeControl();
            }
            updateStatusText();
        } catch (Exception e) {
            System.out.println("Titreşim yetenekleri kontrolü hatası: " + e.getMessage());
        }
    }
    
    private void updateStatusText() {
        String status = "Titreşim Ayarı: " + (vibrationEnabled ? "Açık" : "Kapalı") + "\n" +
                "Titreşim Desteği: " + (hasVibrator ? "Var" : "Yok") + "\n" +
                "Şiddet Kontrolü: " + (hasAmplitudeControl ? "Destekleniyor" : "Desteklenmiyor");
        
        statusText.setText(status);
        resultText.setText("Son Test Sonucu: " + testResult);
    }
    
    private void testSimpleVibration() {
        testResult = "Basit titreşim test ediliyor...";
        updateStatusText();
        
        try {
            vibrationService.testVibration();
            testResult = "Basit titreşim başarılı!";
        } catch (Exception e) {
            testResult = "Basit titreşim hatası: " + e.getMessage();
        }
        
        updateStatusText();
    }
    
    private void testPatternVibration() {
        testResult = "Desen titreşimi test ediliyor...";
        updateStatusText();
        
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            long[] pattern = {0, 100, 100, 200, 100, 300, 100};
            vibrator.vibrate(pattern, -1);
            testResult = "Desen titreşimi başarılı!";
        } catch (Exception e) {
            testResult = "Desen titreşimi hatası: " + e.getMessage();
        }
        
        updateStatusText();
    }
    
    private void testAmplitudeVibration() {
        if (!hasAmplitudeControl) {
            testResult = "Bu cihaz titreşim şiddeti kontrolünü desteklemiyor";
            updateStatusText();
            return;
        }
        
        testResult = "Şiddet titreşimi test ediliyor...";
        updateStatusText();
        
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                int[] amplitudes = {0, 50, 0, 100, 0, 255};
                long[] timings = {0, 500, 100, 500, 100, 500};
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(timings, amplitudes, -1));
            } else {
                long[] pattern = {0, 500, 100, 500, 100, 500};
                vibrator.vibrate(pattern, -1);
            }
            testResult = "Şiddet titreşimi başarılı!";
        } catch (Exception e) {
            testResult = "Şiddet titreşimi hatası: " + e.getMessage();
        }
        
        updateStatusText();
    }
    
    private void testGameMoveVibration() {
        testResult = "Taş hareketi titreşimi test ediliyor...";
        updateStatusText();
        
        try {
            vibrationService.vibrateOnMove();
            testResult = "Taş hareketi titreşimi başarılı!";
        } catch (Exception e) {
            testResult = "Taş hareketi titreşimi hatası: " + e.getMessage();
        }
        
        updateStatusText();
    }
    
    private void testGameEndVibration() {
        testResult = "Oyun sonu titreşimi test ediliyor...";
        updateStatusText();
        
        try {
            vibrationService.vibrateOnGameEnd();
            testResult = "Oyun sonu titreşimi başarılı!";
        } catch (Exception e) {
            testResult = "Oyun sonu titreşimi hatası: " + e.getMessage();
        }
        
        updateStatusText();
    }
    
    private void cancelVibration() {
        try {
            vibrationService.cancelVibration();
            testResult = "Titreşim iptal edildi";
        } catch (Exception e) {
            testResult = "Titreşim iptal hatası: " + e.getMessage();
        }
        
        updateStatusText();
    }
}
