package com.example.specializedu;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.graphics.Typeface;


public class OutputActivity extends AppCompatActivity {

    private TextView outputTextView;
    private File outputFile;
    private static final String TAG = "OutputActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_output);

        outputTextView = findViewById(R.id.outputTextView);
        Button shareButton = findViewById(R.id.shareButton);

        // Set Monospace font for the TextView
        outputTextView.setTypeface(Typeface.MONOSPACE);

        // Set padding for better column alignment (adjust as needed)
        outputTextView.setPadding(16, 16, 16, 16);

        // Request permissions if necessary
        requestPermissions();

        // Log the directory where files are stored
        File filesDir = getFilesDir();
        Log.d(TAG, "Files Directory: " + filesDir.getAbsolutePath());

        // Retrieve the output data from the Intent
        Intent intent = getIntent();
        if (intent != null) {
            String outputData = intent.getStringExtra("output_data");
            if (outputData != null) {
                outputTextView.setText(outputData);

                // Initialize the output file
                outputFile = new File(getFilesDir(), "output.csv");

                // Write outputData to the file
                try (FileWriter writer = new FileWriter(outputFile)) {
                    writer.write(outputData);
                    Log.d(TAG, "Output file written to: " + outputFile.getAbsolutePath());
                } catch (IOException e) {
                    Log.e(TAG, "Error writing output file", e);
                }
            } else {
                outputTextView.setText("No output data available.");
            }
        }

        // Set the OnClickListener for the share button
        shareButton.setOnClickListener(v -> shareOutputCsv());
    }

    private void requestPermissions() {
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    private void shareOutputCsv() {
        if (outputFile == null || !outputFile.exists()) {
            Log.w(TAG, "Output file not found for sharing");
            Toast.makeText(this, "Output file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri contentUri = FileProvider.getUriForFile(this, "com.example.specializedu.fileprovider", outputFile);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share CSV file"));
        Log.d(TAG, "Share intent launched for file: " + outputFile.getAbsolutePath());
    }
}
