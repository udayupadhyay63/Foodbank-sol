package com.example.foodbank2021;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener{

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch active;
    private EditText user_email, user_password;

    // Show login page
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button signin = findViewById(R.id.signIn);
        TextView forgotPassword = findViewById(R.id.forgotpw);

        user_email=(EditText)findViewById(R.id.email);
        user_password=(EditText)findViewById(R.id.password);
        active = findViewById(R.id.active);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        // on click listener for buttons
        forgotPassword.setOnClickListener(this);
        signin.setOnClickListener(this);
    };

    // move to sign-up (Registration) or sign-in (Login) page
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.signIn:
                userLogin();
                break;
            case R.id.forgotpw:
                startActivity(new Intent(this, ForgotPassword.class));
                break;
        }
    };

    // a helper function for login info validity check
    private boolean validityCheck(String input_email, String input_password) {
        if (input_email.isEmpty()) {
            user_email.setError("Email is required.");
            user_email.requestFocus();
            return true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(input_email).matches()) {
            user_email.setError("Please enter valid email.");
            user_email.requestFocus();
            return true;
        }
        if (input_password.isEmpty()) {
            user_password.setError("Password is required.");
            user_password.requestFocus();
            return true;
        } else if (input_password.length() < 6) {
            user_password.setError("Minimum password length should be 6 characters.");
            user_password.requestFocus();
            return true;
        }
        return false;
    };

    // login activity (validity check, account match, google authentication)
    private void userLogin() {
        // casting global user data to string
        String email = user_email.getText().toString().trim();
        String password = user_password.getText().toString().trim();

        // if input data is invalid, do nothing
        if (validityCheck(email, password))
            return;

        // Firebase Authentication Sign-in Method (reference to "Users")
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {

            // a helper function to login as admin/user
            private void login(String uid) {
                DatabaseReference userRef = databaseReference.child(uid);
                userRef.addValueEventListener(new ValueEventListener() {    // attach a listener to get "as" value
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (active.isChecked()) {   // remember me switch ON ("as" will be saved)
                            if (Objects.equals(snapshot.child("as").getValue(String.class), "admin")) { // admin
                                preferences.setDataLogin(LoginActivity.this, true);
                                preferences.setDataAs(LoginActivity.this, "admin");
                                startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                            } else if (Objects.equals(snapshot.child("as").getValue(String.class), "user")) { // user
                                preferences.setDataLogin(LoginActivity.this, true);
                                preferences.setDataAs(LoginActivity.this, "user");
                                startActivity(new Intent(LoginActivity.this, UserActivity.class));
                            }
                        } else {    // remember me switch OFF ("as" history will be ignored)
                            if (Objects.equals(snapshot.child("as").getValue(String.class), "admin")) { // admin
                                preferences.setDataLogin(LoginActivity.this, false);
                                startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                            } else if (Objects.equals(snapshot.child("as").getValue(String.class), "user")) { // user
                                preferences.setDataLogin(LoginActivity.this, false);
                                startActivity(new Intent(LoginActivity.this, UserActivity.class));
                            }
                        }
                    };
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // do nothing
                    };
                });
            };

            // core authentication process using the previous helper function
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
                    assert user != null;
                    login(user.getUid());

                } else {    // unsuccessful task
                    Toast.makeText(LoginActivity.this,"Failed to login! Please check your credentials.",Toast.LENGTH_LONG).show();
                }
            };
        });
    };

    // choose either admin page or user page after successfully log into the app
    @Override
    protected void onStart() {
        super.onStart();
        if (preferences.getDataLogin(this)) {
            if (preferences.getDataAs(this).equals("admin")) {
                startActivity(new Intent(LoginActivity.this, AdminActivity.class));
                finish();
            } else if (preferences.getDataAs(this).equals("user")) {
                startActivity(new Intent(LoginActivity.this, UserActivity.class));
                finish();
            }
        }
    };
}
