package com.app.project_remedi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Login extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText logEmail,logPass;
    private Button logButton;
    private TextView signRedirect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        auth = FirebaseAuth.getInstance();
        logEmail = findViewById(R.id.logEmail);
        logPass = findViewById(R.id.logPass);
        logButton = findViewById(R.id.logButton);
        signRedirect = findViewById(R.id.signupRedirect);
        logButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = logEmail.getText().toString().trim();
                String pass = logPass.getText().toString().trim();
                if (email.isEmpty()) {
                    logEmail.setError("Please enter email");
                }
                if (pass.isEmpty()) {
                    logPass.setError("Please enter password");
                }
                else {
                    auth.signInWithEmailAndPassword(email, pass)
                            .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                                @Override
                                public void onSuccess(AuthResult authResult) {
                                    Toast.makeText(Login.this,"Login successful",Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(Login.this, HomePage.class));
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    String error = ((FirebaseAuthException) e).getErrorCode();
                                    switch (error) {
                                        case "wrong-password":
                                            Toast.makeText(Login.this,"Incorrect password",Toast.LENGTH_SHORT).show();
                                            break;
                                        case "user-not-found":
                                            Toast.makeText(Login.this,"User not found. Please sign up",Toast.LENGTH_SHORT).show();
                                            break;
                                        case "invalid-email":
                                            Toast.makeText(Login.this,"Please enter a valid email",Toast.LENGTH_SHORT).show();
                                            break;
                                        default:
                                            Toast.makeText(Login.this,"Login failed: "+error,Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });
        signRedirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Login.this, Signup.class));
            }
        });
    }
}