package com.example.imageanalysis;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    // Demo credentials
    private static final String DEMO_USERNAME = "Test@example.com";
    private static final String DEMO_PASSWORD = "test123";

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // Set up login button click listener
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });
    }

    private void handleLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Check if fields are empty
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate credentials
        if (username.equals(DEMO_USERNAME) && password.equals(DEMO_PASSWORD)) {
            Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
            // Navigate to HomeActivity
            android.content.Intent intent = new android.content.Intent(MainActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, R.string.login_error, Toast.LENGTH_SHORT).show();
        }
    }
}