package com.app.project_remedi;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions; // Import SetOptions

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddMedications extends AppCompatActivity {
    private EditText medNameInput, totalQtyInput, dosageQtyInput, timeInput;
    private Button addButton;
    private static final int PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medications);

        medNameInput = findViewById(R.id.medNameInput);
        totalQtyInput = findViewById(R.id.totalQtyInput);
        dosageQtyInput = findViewById(R.id.dosageQtyInput);
        timeInput = findViewById(R.id.timeInput);
        addButton = findViewById(R.id.button);

        timeInput.setFocusable(true);
        timeInput.setClickable(true);
        timeInput.setOnClickListener(v -> showTimePickerDialog());

        addButton.setOnClickListener(v -> checkPermissionsAndSave());
    }

    private void showTimePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
                    String amPm = hourOfDay < 12 ? "AM" : "PM";
                    int displayHour = hourOfDay % 12;
                    if (displayHour == 0) displayHour = 12;
                    String timeStr = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minuteOfHour, amPm);
                    timeInput.setText(timeStr);
                }, hour, minute, false); // Use 'false' for 12-hour clock
        timePickerDialog.show();
    }

    private void checkPermissionsAndSave() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                new AlertDialog.Builder(this)
                        .setTitle("Notification Permission")
                        .setMessage("ReMedi needs notification permission to show the medication alarm when it triggers.")
                        .setPositiveButton("Ask for Permission", (dialog, which) -> {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> checkExactAlarmPermissionAndSave())
                        .show();
            } else {
                checkExactAlarmPermissionAndSave();
            }
        } else {
            checkExactAlarmPermissionAndSave();
        }
    }
    private void checkExactAlarmPermissionAndSave() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                new AlertDialog.Builder(this)
                        .setTitle("Alarm Permission Required")
                        .setMessage("Please grant 'Alarms & reminders' permission for reliable medication reminders at the exact time.")
                        .setPositiveButton("Go to Settings", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            startActivity(intent);
                        })
                        .setNegativeButton("Skip", (dialog, which) -> saveMedication())
                        .show();
                return;
            }
        }
        saveMedication();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkExactAlarmPermissionAndSave();
            } else {
                Toast.makeText(this, "Notification permission denied. Alarms may not show.", Toast.LENGTH_LONG).show();
                checkExactAlarmPermissionAndSave();
            }
        }
    }

    private void saveMedication() {
        String medName = medNameInput.getText().toString().trim();
        String totalQtyStr = totalQtyInput.getText().toString().trim();
        String dosageQtyStr = dosageQtyInput.getText().toString().trim();
        String medTime = timeInput.getText().toString().trim();

        if (medName.isEmpty() || totalQtyStr.isEmpty() || dosageQtyStr.isEmpty() || medTime.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int notificationId = (medName + medTime).hashCode();

        Map<String, Object> med = new HashMap<>();
        med.put("name", medName);
        med.put("totalQty", totalQtyStr);
        med.put("dosageQty", dosageQtyStr);
        med.put("time", medTime);
        med.put("notificationId", notificationId);
        med.put("taken", false);
        setMedicationAlarm(medName, medTime,notificationId);
    }
    private void setMedicationAlarm(String medName, String medTime, int notificationId) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.setAction("com.example.remedi.MEDICATION_ALARM");
        intent.putExtra("MED_NAME", medName);
        intent.putExtra("MED_TIME", medTime);
        intent.putExtra("NOTIFICATION_ID", notificationId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Calendar calendar = Calendar.getInstance();
        try {
            String[] timeParts = medTime.split(" ");
            String time = timeParts[0];
            String ampm = timeParts[1];

            String[] hourMinute = time.split(":");
            int hour = Integer.parseInt(hourMinute[0]);
            int minute = Integer.parseInt(hourMinute[1]);

            if (ampm.equalsIgnoreCase("PM") && hour != 12) {
                hour += 12;
            } else if (ampm.equalsIgnoreCase("AM") && hour == 12) {
                hour = 0; // Midnight case
            }

            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

        } catch (Exception e) {
            Log.e("AddMedication", "Error parsing time: " + medTime, e);
            Toast.makeText(this, "Alarm not set due to time format error.", Toast.LENGTH_LONG).show();
            return;
        }

        if (calendar.before(Calendar.getInstance())) {
            Toast.makeText(this, "Alarm not set due to invalid time.", Toast.LENGTH_LONG).show();
        }

        long alarmTime = calendar.getTimeInMillis();

        if (alarmManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                }
                Log.d("AddMedication", "Alarm set for " + medName + " at " + calendar.getTime().toString());
            } catch (SecurityException e) {
                Log.e("AddMedication", "Failed to set alarm: Permission denied (SCHEDULE_EXACT_ALARM)", e);
                Toast.makeText(this, "Alarm failed: Exact alarm permission missing.", Toast.LENGTH_LONG).show();
            }
        }
    }
}