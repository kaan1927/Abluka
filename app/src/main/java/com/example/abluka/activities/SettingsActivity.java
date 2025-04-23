package com.example.abluka.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.abluka.R;
import com.example.abluka.services.VibrationService;

public class SettingsActivity extends AppCompatActivity {
    private String difficulty = "Medium";
    private boolean soundEnabled = true;
    private boolean vibrationEnabled = true;
    private boolean showHints = true;
    private VibrationService vibrationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Titreşim servisini başlat
        vibrationService = new VibrationService(getSystemService(VIBRATOR_SERVICE));

        // Gradient arka plan ayarla
        ConstraintLayout mainLayout = findViewById(R.id.settings_layout);
        mainLayout.setBackground(getResources().getDrawable(R.drawable.gradient_background));

        // Ayarları yükle
        loadSettings();

        // Zorluk seviyesi radio butonları
        RadioGroup difficultyGroup = findViewById(R.id.difficulty_group);
        difficultyGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioButton = findViewById(checkedId);
                difficulty = radioButton.getTag().toString();
            }
        });

        // Zorluk seviyesini ayarla
        if (difficulty.equals("Easy")) {
            ((RadioButton) findViewById(R.id.radio_easy)).setChecked(true);
        } else if (difficulty.equals("Hard")) {
            ((RadioButton) findViewById(R.id.radio_hard)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.radio_medium)).setChecked(true);
        }

        // Switch'leri ayarla
        Switch soundSwitch = findViewById(R.id.sound_switch);
        Switch vibrationSwitch = findViewById(R.id.vibration_switch);
        Switch hintsSwitch = findViewById(R.id.hints_switch);

        soundSwitch.setChecked(soundEnabled);
        vibrationSwitch.setChecked(vibrationEnabled);
        hintsSwitch.setChecked(showHints);

        soundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> soundEnabled = isChecked);

        vibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            vibrationEnabled = isChecked;
            // Titreşim açıldığında test titreşimi ver
            if (isChecked) {
                vibrationService.testVibration();
            }
        });

        hintsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> showHints = isChecked);

        // Titreşim test butonu
        Button vibrationTestButton = findViewById(R.id.vibration_test_button);
        vibrationTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, VibrationTestActivity.class);
                startActivity(intent);
            }
        });

        // Kaydet butonu
        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });

        // Sıfırla butonu
        Button resetButton = findViewById(R.id.reset_button);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResetConfirmationDialog();
            }
        });
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("AblukaPref", MODE_PRIVATE);
        difficulty = prefs.getString("difficulty", "Medium");
        soundEnabled = prefs.getBoolean("soundEnabled", true);
        vibrationEnabled = prefs.getBoolean("vibrationEnabled", true);
        showHints = prefs.getBoolean("showHints", true);
    }

    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences("AblukaPref", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("difficulty", difficulty);
        editor.putBoolean("soundEnabled", soundEnabled);
        editor.putBoolean("vibrationEnabled", vibrationEnabled);
        editor.putBoolean("showHints", showHints);
        editor.apply();

        // Titreşim ayarı kaydedildiğinde ve açıksa test titreşimi ver
        if (vibrationEnabled) {
            vibrationService.testVibration();
        }

        Toast.makeText(this, "Ayarlar kaydedildi!", Toast.LENGTH_SHORT).show();
    }

    private void showResetConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Ayarları Sıfırla")
                .setMessage("Tüm ayarlar varsayılan değerlere sıfırlanacak. Emin misiniz?")
                .setNegativeButton("İptal", null)
                .setPositiveButton("Sıfırla", (dialog, which) -> {
                    difficulty = "Medium";
                    soundEnabled = true;
                    vibrationEnabled = true;
                    showHints = true;

                    // UI'ı güncelle
                    ((RadioButton) findViewById(R.id.radio_medium)).setChecked(true);
                    ((Switch) findViewById(R.id.sound_switch)).setChecked(true);
                    ((Switch) findViewById(R.id.vibration_switch)).setChecked(true);
                    ((Switch) findViewById(R.id.hints_switch)).setChecked(true);

                    saveSettings();
                })
                .show();
    }
}