package com.example.abluka.services;

import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class VibrationService {
    private final Vibrator vibrator;
    
    public VibrationService(Object vibratorService) {
        this.vibrator = (Vibrator) vibratorService;
    }
    
    public boolean isVibrationEnabled() {
        return vibrator != null && vibrator.hasVibrator();
    }
    
    public void testVibration() {
        if (isVibrationEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(100);
            }
        }
    }
    
    public void vibrateOnMove() {
        if (isVibrationEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }
    
    public void vibrateOnGameEnd() {
        if (isVibrationEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                long[] pattern = {0, 100, 100, 100, 100, 200};
                int[] amplitudes = {0, 255, 0, 255, 0, 255};
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1));
            } else {
                long[] pattern = {0, 100, 100, 100, 100, 200};
                vibrator.vibrate(pattern, -1);
            }
        }
    }
    
    public void cancelVibration() {
        if (vibrator != null) {
            vibrator.cancel();
        }
    }
}
