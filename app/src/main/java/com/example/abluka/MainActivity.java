package com.example.abluka;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.abluka.activities.HomeActivity;
import com.example.abluka.services.VibrationService;

public class MainActivity extends AppCompatActivity {
    private VibrationService vibrationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Ekran yönlendirmesini ayarla (Portrait mode)
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // Titreşim servisini başlat
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrationService = new VibrationService(vibrator);
            
            // Cihazın titreşim özelliğini kontrol et
            boolean hasVibrator = vibrator.hasVibrator();
            boolean hasAmplitudeControl = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                hasAmplitudeControl = vibrator.hasAmplitudeControl();
            }

            System.out.println("Cihaz titreşim destekliyor mu: " + hasVibrator);
            System.out.println("Cihaz titreşim şiddeti kontrolü destekliyor mu: " + hasAmplitudeControl);
            
            // Basit bir titreşim testi
            if (hasVibrator) {
                vibrationService.testVibration();
                System.out.println("Titreşim servisi başarıyla başlatıldı");
            }
        } catch (Exception e) {
            System.out.println("Titreşim servisi başlatma hatası: " + e.getMessage());
        }
        
        // Ana ekrana yönlendir
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}
