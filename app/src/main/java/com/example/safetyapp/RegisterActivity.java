package com.example.safetyapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private EditText inputName, inputEmail, inputPassword;
    private Button btnRegister, btnGoLogin;
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("Users");

        inputName = findViewById(R.id.inputName);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoLogin = findViewById(R.id.btnGoLogin);

        btnRegister.setOnClickListener(v -> registerUser());
        btnGoLogin.setOnClickListener(v -> startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));
    }

    private void registerUser() {
        String name = inputName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { inputName.setError("Name is required"); return; }
        if (TextUtils.isEmpty(email)) { inputEmail.setError("Email is required"); return; }
        if (TextUtils.isEmpty(password)) { inputPassword.setError("Password is required"); return; }
        if (password.length() < 6) { inputPassword.setError("Password must be at least 6 characters"); return; }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            UserModel user = new UserModel(name, email);
                            dbRef.child(firebaseUser.getUid()).setValue(user)
                                    .addOnCompleteListener(dbTask -> {
                                        if (dbTask.isSuccessful()) {
                                            Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                            finish();
                                        } else {
                                            Toast.makeText(RegisterActivity.this, "Database error: " + dbTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(RegisterActivity.this, "This email is already registered. Please login.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
