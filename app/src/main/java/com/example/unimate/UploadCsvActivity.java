package com.example.unimate;



import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class UploadCsvActivity extends AppCompatActivity {
    private static final int FILE_PICKER_CODE = 101;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_csv);

        db = FirebaseFirestore.getInstance();
        Button btnUpload = findViewById(R.id.btnUploadCsv);
        progressBar = findViewById(R.id.progressBar);

        btnUpload.setOnClickListener(v -> openFilePicker());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent, FILE_PICKER_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_PICKER_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                progressBar.setVisibility(View.VISIBLE);
                parseAndUploadCsv(uri);
            }
        }
    }

    private void parseAndUploadCsv(Uri uri) {
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                boolean isDataRow = false;
                Map<String, Map<String, Map<String, Map<String, Object>>>> batches = new HashMap<>();

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("SUNDAY,Batch")) {
                        isDataRow = true;
                        continue;
                    }
                    if (!isDataRow || line.trim().isEmpty() || line.contains("BUS TIME")) continue;

                    String[] cells = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    if (cells.length < 9) continue;

                    String batch = cells[1].trim();
                    String section = cells[2].trim();
                    if (batch.isEmpty() || section.isEmpty()) continue;

                    String[] timeSlots = {
                            "09:00-10:20AM", "10:20-11:40AM", "11:40-1:00PM",
                            "1:30-2:50PM", "2:50-4:10PM"
                    };

                    Map<String, Map<String, Object>> sectionData = new HashMap<>();

                    for (int i = 3; i <= 7; i++) { // Columns 3-7 (time slots)
                        String cell = cells[i].replace("\"", "").trim();
                        if (cell.isEmpty()) continue;

                        String[] parts = cell.split("\\s+");
                        if (parts.length < 3) continue;

                        Map<String, Object> classInfo = new HashMap<>();
                        classInfo.put("course", parts[0]);
                        classInfo.put("instructor", parts[1]);
                        classInfo.put("room", parts[2]);

                        sectionData.put(timeSlots[i-3], classInfo);
                    }

                    batches.computeIfAbsent(batch, k -> new HashMap<>())
                            .computeIfAbsent(section, k -> new HashMap<>())
                            .putAll(sectionData);
                }

                uploadToFirestore(batches);
                runOnUiThread(() -> progressBar.setVisibility(View.GONE));

            } catch (IOException e) {
                Log.e("CSV", "Error processing file", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void uploadToFirestore(Map<String, Map<String, Map<String, Map<String, Object>>>> data) {
        Map<String, Object> firestoreData = new HashMap<>();
        for (Map.Entry<String, Map<String, Map<String, Map<String, Object>>>> batchEntry : data.entrySet()) {
            firestoreData.put("batch_" + batchEntry.getKey(), batchEntry.getValue());
        }

        db.collection("schedules").document("sunday")
                .set(firestoreData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> runOnUiThread(() ->
                        Toast.makeText(UploadCsvActivity.this, "Schedule updated!", Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e -> runOnUiThread(() ->
                        Toast.makeText(UploadCsvActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()));
    }
}