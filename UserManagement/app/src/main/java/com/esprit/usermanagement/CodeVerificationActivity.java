package com.esprit.usermanagement;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.esprit.usermanagement.AppDatabase;
import com.esprit.usermanagement.R;
import com.esprit.usermanagement.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CodeVerificationActivity extends AppCompatActivity {
    private EditText etResetCode;
    private Button btnVerifyCode;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code_verification);

        etResetCode = findViewById(R.id.etResetCode);
        btnVerifyCode = findViewById(R.id.btnVerifyCode);

        executorService = Executors.newSingleThreadExecutor();  // Initialize ExecutorService

        btnVerifyCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredCode = etResetCode.getText().toString();
                SharedPreferences sharedPreferences = getSharedPreferences("ForgotPassword", MODE_PRIVATE);
                String savedCode = sharedPreferences.getString("reset_code", null);
                String username = sharedPreferences.getString("username", null);

                if (enteredCode.isEmpty()) {
                    Toast.makeText(CodeVerificationActivity.this, "Please enter the reset code", Toast.LENGTH_SHORT).show();
                } else if (savedCode != null && enteredCode.equals(savedCode)) {
                    // The code matches, now show the password
                    showPassword(username);
                } else {
                    // Code does not match
                    Toast.makeText(CodeVerificationActivity.this, "Invalid reset code", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Fetch the password and show it in a dialog, this will be run on a background thread
    private void showPassword(String username) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // Access the database in a background thread
                AppDatabase db = AppDatabase.getInstance(CodeVerificationActivity.this);
                User user = db.userDao().getUserByUsername(username);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (user != null) {
                            // Show the password in a pop-up dialog
                            AlertDialog.Builder builder = new AlertDialog.Builder(CodeVerificationActivity.this);
                            builder.setTitle("Your Password")
                                    .setMessage("Your password is: " + user.getPassword()) // Display the password here
                                    .setPositiveButton("OK", null)
                                    .show();
                        } else {
                            Toast.makeText(CodeVerificationActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
}
