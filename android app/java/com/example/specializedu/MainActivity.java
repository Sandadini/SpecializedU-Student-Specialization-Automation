package com.example.specializedu;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PICK_HONORS_FILE = 1;
    private static final int PICK_QUOTA_FILE = 2;

    private List<Student> students = new ArrayList<>();
    private List<Subject> subjects = new ArrayList<>();
    private TextView statusTextView;
    private TextView resultTextView;
    private Button selectHonorsButton, selectQuotaButton, outputButton;
    private Uri honorsFileUri, quotaFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.statusTextView);
//        resultTextView = findViewById(R.id.resultTextView);
        selectHonorsButton = findViewById(R.id.selectHonorsButton);
        selectQuotaButton = findViewById(R.id.selectQuotaButton);
        outputButton = findViewById(R.id.outputButton);

        selectHonorsButton.setOnClickListener(v -> openFilePicker(PICK_HONORS_FILE));
        selectQuotaButton.setOnClickListener(v -> openFilePicker(PICK_QUOTA_FILE));
        outputButton.setOnClickListener(v -> {
            if (honorsFileUri != null && quotaFileUri != null) {
                processData();
            } else {
                String errorMessage = "Please select both files first. ";
                errorMessage += (honorsFileUri == null) ? "Honors file is missing. " : "";
                errorMessage += (quotaFileUri == null) ? "Quota file is missing." : "";
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                statusTextView.setText(errorMessage);
            }
        });
    }

    private void openFilePicker(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri selectedFileUri = data.getData();
            if (requestCode == PICK_HONORS_FILE) {
                honorsFileUri = selectedFileUri;
                selectHonorsButton.setText("Honors List Selected");
                statusTextView.setText("Honors file selected");
                Toast.makeText(this, "Honors file selected", Toast.LENGTH_SHORT).show(); // Add this line
            } else if (requestCode == PICK_QUOTA_FILE) {
                quotaFileUri = selectedFileUri;
                selectQuotaButton.setText("Quota List Selected");
                statusTextView.setText("Quota file selected");
                Toast.makeText(this, "Quota file selected", Toast.LENGTH_SHORT).show(); // Add this line
            }
        } else {
            String errorMessage = "File selection cancelled or failed";
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            statusTextView.setText(errorMessage);
        }
    }


    private void processData() {
        new Thread(() -> {
            try {
                if (honorsFileUri == null || quotaFileUri == null) {
                    throw new IOException("One or both file URIs are null");
                }
                loadData(honorsFileUri, quotaFileUri);
                selectStudents();

                // Generate output string
                StringBuilder output = new StringBuilder();
                output.append("Student No:,Index No:,Special,GPA\n");
                for (Student student : students) {
                    if (!student.preferences.isEmpty() && student.preferences.get(0) != null) {
                        output.append(String.format("%s,%s,%s,%.2f\n",
                                student.studentNumber,
                                student.indexNumber,
                                student.preferences.get(0),
                                student.gpas.get(0)));
                    }
                }

                final String outputString = output.toString();

                runOnUiThread(() -> {
                    // Start OutputActivity and pass the output data
                    Intent intent = new Intent(MainActivity.this, OutputActivity.class);
                    intent.putExtra("output_data", outputString);
                    startActivity(intent);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error processing data", e);
                runOnUiThread(() -> {
                    String errorMessage = "Error: " + e.getMessage();
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    statusTextView.setText("Error occurred during processing: " + errorMessage);
                });
            }
        }).start();
    }

    private void loadData(Uri honorsFileUri, Uri quotaFileUri) throws IOException {
        students.clear();
        subjects.clear();

        // Load honors list
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(honorsFileUri)))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 2) {
                    Log.w(TAG, "Skipping invalid line in honors file: " + line);
                    continue;
                }
                Student student = new Student(parts[0], parts[1]);
                for (int i = 2; i < parts.length; i += 2) {
                    if (i + 1 < parts.length && parts[i] != null && !parts[i].isEmpty()) {
                        try {
                            student.preferences.add(parts[i]);
                            student.gpas.add(Double.parseDouble(parts[i + 1]));
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Invalid GPA for student " + student.studentNumber + ": " + parts[i + 1]);
                        }
                    }
                }
                if (!student.preferences.isEmpty()) {
                    students.add(student);
                } else {
                    Log.w(TAG, "Skipping student with no preferences: " + student.studentNumber);
                }
            }
        }

        Log.d(TAG, "Loaded " + students.size() + " students");

        // Sort students by p1 and then by v1 (descending)
        Collections.sort(students);

        // Load quota
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(quotaFileUri)))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2 && parts[0] != null && !parts[0].isEmpty()) {
                    try {
                        subjects.add(new Subject(parts[0], Integer.parseInt(parts[1])));
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Invalid quota for subject " + parts[0] + ": " + parts[1]);
                    }
                } else {
                    Log.w(TAG, "Skipping invalid line in quota file: " + line);
                }
            }
        }

        Log.d(TAG, "Loaded " + subjects.size() + " subjects");
    }

    private void selectStudents() {
        for (int iteration = 0; iteration < 20; iteration++) {
            for (Subject subject : subjects) {
                List<Student> eligibleStudents = new ArrayList<>();
                for (Student student : students) {
                    if (!student.preferences.isEmpty() && student.preferences.get(0) != null && student.preferences.get(0).equals(subject.name)) {
                        eligibleStudents.add(student);
                    }
                }

                int quotaExtension = 0;
                if (eligibleStudents.size() > subject.quota && !eligibleStudents.isEmpty()) {
                    double lastGpa = eligibleStudents.get(Math.min(subject.quota - 1, eligibleStudents.size() - 1)).gpas.get(0);
                    while (subject.quota + quotaExtension < eligibleStudents.size() &&
                            eligibleStudents.get(subject.quota + quotaExtension).gpas.get(0) == lastGpa) {
                        quotaExtension++;
                    }
                }

                int totalSelected = Math.min(subject.quota + quotaExtension, eligibleStudents.size());

                for (int i = totalSelected; i < eligibleStudents.size(); i++) {
                    Student student = eligibleStudents.get(i);
                    if (!student.preferences.isEmpty()) {
                        student.preferences.remove(0);
                        student.gpas.remove(0);
                    }
                    if (!student.preferences.isEmpty()) {
                        student.preferences.add(null);
                        student.gpas.add(null);
                    }
                }
            }

            // Remove students with no more preferences
            students.removeIf(student -> student.preferences.isEmpty());

            // Re-sort students after each iteration
            Collections.sort(students);
        }

        Log.d(TAG, "Selection process completed. Remaining students: " + students.size());
    }

    private static class Student implements Comparable<Student> {
        String studentNumber;
        String indexNumber;
        List<String> preferences;
        List<Double> gpas;

        public Student(String studentNumber, String indexNumber) {
            this.studentNumber = studentNumber;
            this.indexNumber = indexNumber;
            this.preferences = new ArrayList<>();
            this.gpas = new ArrayList<>();
        }

        @Override
        public int compareTo(Student other) {
            // Compare by first preference
            int p1Comparison = comparePreferences(this.preferences, other.preferences, 0);
            if (p1Comparison != 0) {
                return p1Comparison;
            }

            // If first preferences are the same, compare by first GPA (descending order)
            int v1Comparison = compareGPAs(this.gpas, other.gpas, 0);
            if (v1Comparison != 0) {
                return v1Comparison;
            }

            // If still equal, compare by student number
            return this.studentNumber.compareTo(other.studentNumber);
        }

        private static int comparePreferences(List<String> prefs1, List<String> prefs2, int index) {
            if (prefs1.size() <= index && prefs2.size() <= index) {
                return 0;
            }
            if (prefs1.size() <= index) {
                return -1;
            }
            if (prefs2.size() <= index) {
                return 1;
            }
            String p1 = prefs1.get(index);
            String p2 = prefs2.get(index);
            if (p1 == null && p2 == null) {
                return 0;
            }
            if (p1 == null) {
                return -1;
            }
            if (p2 == null) {
                return 1;
            }
            return p1.compareTo(p2);
        }

        private static int compareGPAs(List<Double> gpas1, List<Double> gpas2, int index) {
            if (gpas1.size() <= index && gpas2.size() <= index) {
                return 0;
            }
            if (gpas1.size() <= index) {
                return 1;
            }
            if (gpas2.size() <= index) {
                return -1;
            }
            Double v1 = gpas1.get(index);
            Double v2 = gpas2.get(index);
            if (v1 == null && v2 == null) {
                return 0;
            }
            if (v1 == null) {
                return 1;
            }
            if (v2 == null) {
                return -1;
            }
            return Double.compare(v2, v1); // Note: Descending order
        }
    }

    private static class Subject {
        String name;
        int quota;

        public Subject(String name, int quota) {
            this.name = name;
            this.quota = quota;
        }
    }
}