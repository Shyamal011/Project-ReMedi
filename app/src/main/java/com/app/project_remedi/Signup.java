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
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Signup extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText signName,signEmail,signPass;
    private Button signButton;
    private TextView logRedirect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        auth = FirebaseAuth.getInstance();
        db=FirebaseFirestore.getInstance();
        signName=findViewById(R.id.signName);
        signEmail = findViewById(R.id.signEmail);
        signPass = findViewById(R.id.signPass);
        signButton = findViewById(R.id.signButton);
        logRedirect = findViewById(R.id.loginRedirect);
        signButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = signName.getText().toString().trim();
                String email = signEmail.getText().toString().trim();
                String pass = signPass.getText().toString().trim();
                if(email.isEmpty()) {
                    signEmail.setError("Please enter email");
                }
                if(pass.isEmpty()) {
                    signPass.setError("Please enter password");
                }
                else {
                    auth.createUserWithEmailAndPassword(email, pass)
                            .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                                @Override
                                public void onSuccess(AuthResult authResult) {
                                    String uid = auth.getCurrentUser().getUid();
                                    Map<String,Object> userData = new HashMap<>();
                                    userData.put("Name",name);
                                    userData.put("GuardianID",null);
                                    db.collection("Users").document(uid).set(userData);
                                    Toast.makeText(Signup.this, "Sign up successful", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(Signup.this, HomePage.class));
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    String error = ((FirebaseAuthException) e).getErrorCode();
                                    switch(error) {
                                        case "email-already-in-use":
                                            Toast.makeText(Signup.this, "Email already in use. Please login", Toast.LENGTH_SHORT).show();
                                            break;
                                        case "invalid-email":
                                            Toast.makeText(Signup.this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
                                            break;
                                        case "weak-password":
                                            Toast.makeText(Signup.this, "Password must be atleast 6 characters", Toast.LENGTH_SHORT).show();
                                            break;
                                        default:
                                            Toast.makeText(Signup.this, "Sign up failed: "+error, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });
        logRedirect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Signup.this,Login.class));
            }
        });
    }
}