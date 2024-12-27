package com.example.unimate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Upload extends AppCompatActivity {

    private static final int PICK_CSV_FILE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_upload_page);

        // Initialize the Upload CSV button
        Button uploadCsvButton = findViewById(R.id.uploadCsvButton);

        // Set an onClick listener for the button
        uploadCsvButton.setOnClickListener(view -> {
            // Open file picker to choose a CSV file
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("text/csv");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                startActivityForResult(Intent.createChooser(intent, "Select CSV File"), PICK_CSV_FILE);
            } catch (Exception e) {
                Toast.makeText(this, "File picker not found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_CSV_FILE && resultCode == RESULT_OK && data != null) {
            // Get the URI of the selected file
            Uri csvFileUri = data.getData();

            if (csvFileUri != null) {
                try {
                    // Open the file and read its content
                    InputStream inputStream = getContentResolver().openInputStream(csvFileUri);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                    StringBuilder csvContent = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        csvContent.append(line).append("\n");
                    }

                    // Close the stream
                    inputStream.close();

                    // Log or display the content of the CSV file
                    Log.d("CSV_CONTENT", csvContent.toString());
                    Toast.makeText(this, "CSV file uploaded successfully", Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    Log.e("CSV_ERROR", "Error reading CSV file", e);
                    Toast.makeText(this, "Failed to read the file", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
