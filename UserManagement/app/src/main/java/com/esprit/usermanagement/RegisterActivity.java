package com.esprit.usermanagement;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword, etConfirmPassword, etAddress, etPhone;
    private Button btnRegister;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etAddress = findViewById(R.id.etAddress);
        etPhone = findViewById(R.id.etPhone);
        btnRegister = findViewById(R.id.btnRegister);

        db = AppDatabase.getInstance(this);

        btnRegister.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();
            String address = etAddress.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();

            if (validateInputs(username, email, password, confirmPassword, phone)) {
                User user = new User();
                user.setUsername(username);
                user.setEmail(email);
                user.setPassword(password);
                user.setAddress(address);
                user.setPhoneNumber(phone);
                user.setProfileImage(""); // Placeholder for profile image

                Executors.newSingleThreadExecutor().execute(() -> {
                    db.userDao().insertUser(user);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    });
                });
            }
        });

        TextView tvLogin = findViewById(R.id.tvLogin);
        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private boolean validateInputs(String username, String email, String password, String confirmPassword, String phone) {
        if (username.length() < 10) {
            etUsername.setError("Username must be at least 10 characters long");
            return false;
        }

        if (!isValidEmail(email)) {
            etEmail.setError("Enter a valid email address");
            return false;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password cannot be empty");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return false;
        }

        if (!isValidPhoneNumber(phone)) {
            etPhone.setError("Phone number must be exactly 8 digits");
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return Pattern.matches(emailPattern, email);
    }

    private boolean isValidPhoneNumber(String phone) {
        return phone.matches("\\d{8}");
    }
}
