package com.esprit.usermanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PasswordResetActivity extends AppCompatActivity {
    private EditText etUsername;
    private Button btnSendCode;

    private Executor executor; // Executor for background tasks

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_reset);

        etUsername = findViewById(R.id.etUsername);
        btnSendCode = findViewById(R.id.btnSendCode);

        // Initialize the Executor
        executor = Executors.newSingleThreadExecutor(); // Single thread executor for background tasks

        btnSendCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();

                if (username.isEmpty()) {
                    Toast.makeText(PasswordResetActivity.this, "Please enter your username", Toast.LENGTH_SHORT).show();
                } else {
                    // Execute the database query in the background using Executor
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            // Check if the username exists in the Room DB
                            AppDatabase db = AppDatabase.getInstance(PasswordResetActivity.this);
                            User user = db.userDao().getUserByUsername(username);

                            if (user != null) {
                                // Generate a random reset code
                                String resetCode = generateResetCode();

                                // Send the reset code to the user's email
                                String subject = "Password Reset Code";
                                String body = "Your password reset code is: " + resetCode;

                                // Use MailSender to send the email
                                String email = user.getEmail(); // Get the email from the user object
                                MailSender mailSender = new MailSender("samerbelghith2017@gmail.com", "uuyu pfnb eoia kpjm"); // Use your SMTP email credentials

                                // Sending the email in a separate thread to avoid blocking the UI thread
                                try {
                                    mailSender.sendMail(subject, body, email);
                                    // Update the UI on the main thread
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(PasswordResetActivity.this, "Reset code sent to your email", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(PasswordResetActivity.this, CodeVerificationActivity.class);
                                            startActivity(intent);
                                        }
                                    });
                                } catch (Exception e) {
                                    // Handle the error and update the UI on the main thread
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(PasswordResetActivity.this, "Error sending email", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            } else {
                                // If the user is not found, show an error message on the main thread
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(PasswordResetActivity.this, "Username not found", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    // Generate a random 6-digit code
    private String generateResetCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // Generates a random 6-digit number
        SharedPreferences sharedPreferences = getSharedPreferences("ForgotPassword", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("reset_code", String.valueOf(code)); // Save the reset code with a key
        editor.putString("username",etUsername.getText().toString());
        editor.apply();
        return String.valueOf(code);
    }
}
