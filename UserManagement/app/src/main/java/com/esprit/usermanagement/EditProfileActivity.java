package com.esprit.usermanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.bumptech.glide.Glide;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class EditProfileActivity extends AppCompatActivity {

    private EditText editName, editEmail, editAddress, editPhone;
    private ImageView profileImage;
    private Uri imageUri;
    private SharedPreferences sharedPreferences;
    private StorageReference storageReference;
    private Executor executor;
    AppDatabase db = AppDatabase.getInstance(this);  // Get the Room database instance
    UserDao userDao = db.userDao();  // Get the UserDao instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);
        executor = Executors.newSingleThreadExecutor();
        // Initialize views
        editName = findViewById(R.id.edit_name);
        editEmail = findViewById(R.id.edit_email);
        editAddress = findViewById(R.id.edit_address);
        editPhone = findViewById(R.id.edit_phone);
        profileImage = findViewById(R.id.profile_image);
        Button btnUploadImage = findViewById(R.id.btn_upload_image);
        Button btnSave = findViewById(R.id.btn_save);

        // Firebase Storage Reference
        storageReference = FirebaseStorage.getInstance().getReference("profile_images");

        // Load user data from SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        loadUserData();

        // Upload Image Button
        btnUploadImage.setOnClickListener(view -> selectImage());

        // Save Button
        btnSave.setOnClickListener(view -> saveUserProfile());
    }

    private void loadUserData() {
        String name = sharedPreferences.getString("username", "");
        String email = sharedPreferences.getString("email", "");
        String address = sharedPreferences.getString("address", "");
        String phone = sharedPreferences.getString("phone", "");
        String profileImageUrl = sharedPreferences.getString("profile_image", "");

        editName.setText(name);
        editEmail.setText(email);
        editAddress.setText(address);
        editPhone.setText(phone);

        if (!profileImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileImageUrl)
                    .into(profileImage);
        }
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            profileImage.setImageURI(imageUri);
        }
    }

    private void saveUserProfile() {
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String address = editAddress.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || address.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            uploadImageToFirebase(name, email, address, phone, imageUri);
        } else {
            // Save to SharedPreferences
            saveToSharedPreferences(name, email, address, phone, null);

            // Save to Room Database
            updateUserInDatabase(name, email, address, phone, null);
        }
    }

    private void uploadImageToFirebase(String name, String email, String address, String phone, Uri imageUri) {
        // Authenticate with Firebase using email and password
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        firebaseAuth.signInWithEmailAndPassword("admin@gmail.com", "123456789")
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Authentication successful, proceed to upload image
                        StorageReference fileRef = FirebaseStorage.getInstance().getReference()
                                .child(System.currentTimeMillis() + ".jpg");

                        fileRef.putFile(imageUri)
                                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                                        .addOnSuccessListener(uri -> {
                                            String imageUrl = uri.toString();

                                            // Save to SharedPreferences
                                            saveToSharedPreferences(name, email, address, phone, imageUrl);

                                            // Save to Room Database
                                            updateUserInDatabase(name, email, address, phone, imageUrl);
                                        }))
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // Authentication failed
                        Toast.makeText(this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveToSharedPreferences(String name, String email, String address, String phone, @Nullable String imageUrl) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", name);
        editor.putString("email", email);
        editor.putString("address", address);
        editor.putString("phone", phone);

        if (imageUrl != null) {
            editor.putString("profile_image", imageUrl);
        }

        editor.apply();
    }

    private void updateUserInDatabase(String name, String email, String address, String phone, @Nullable String imageUrl) {
        if (executor != null) {
            executor.execute(() -> {
                // Retrieve the username from SharedPreferences (assuming it's stored there)
                String username = sharedPreferences.getString("username", "");

                // Find the user by username in the database
                User user = userDao.getUserByUsername(username);

                if (user != null) {
                    // Update the user fields
                    user.setUsername(name);
                    user.setEmail(email);
                    user.setAddress(address);
                    user.setPhoneNumber(phone);
                    if (imageUrl != null) {
                        user.setProfileImage(imageUrl);
                    }

                    // Update the user in Room database
                    userDao.updateUser(user);
                } else {
                    // If the user doesn't exist, handle this case
                    runOnUiThread(() -> Toast.makeText(this, "User not found in database", Toast.LENGTH_SHORT).show());
                }
            });
        } else {
            Log.e("EditProfileActivity", "Executor is not initialized!");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }
    }
}
