package com.example.tutornearme;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.tutornearme.Model.TutorInfoModel;
import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SignInActivity extends AppCompatActivity {
    private final static int LOGIN_REQUEST_CODE = 1717;
    private List<AuthUI.IdpConfig> providers;         // List of providers such as google, twitter etc.
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;  // for authentication state

    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    FirebaseDatabase database;
    DatabaseReference tutorInfoRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_progress);
        // separation of method
        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // register listener
        firebaseAuth.addAuthStateListener(listener);

        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStop() {
        // unregister listener
        if(firebaseAuth != null && listener != null){
            firebaseAuth.removeAuthStateListener(listener);
        }
        super.onStop();
    }

    private void init(){
        ButterKnife.bind(this);

        database = FirebaseDatabase.getInstance();
        tutorInfoRef = database.getReference(CommonClass.TUTOR_INFO_REFERENCE);
        // create and assign the providers to be used to "providers"
        providers = Arrays.asList(
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        // initialize firebase auth
        firebaseAuth = FirebaseAuth.getInstance();
        listener = myFirebaseAuth -> {
            // get current user
            FirebaseUser user = myFirebaseAuth.getCurrentUser();
            // check if user exists else show log in layout
            if(user != null){
                checkUserFromFirebase();
            }else{
                showLoginLayout();
            }
        };
    }

    private void checkUserFromFirebase() {
        tutorInfoRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(snapshot.exists()){
                        //Toast.makeText(SignInActivity.this,"There is already an existing account with this user" ,Toast.LENGTH_SHORT).show();
                        TutorInfoModel tutorInfoModel = snapshot.getValue(TutorInfoModel.class);
                        goToTutorHomeActivity(tutorInfoModel);

                    }else{
                        showRegisterLayout();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(SignInActivity.this,"Error: " + error.getMessage(),Toast.LENGTH_SHORT).show();
                }
            });

    }

    private void goToTutorHomeActivity(TutorInfoModel tutorInfoModel) {
        CommonClass.currentUser = tutorInfoModel;
        startActivity(new Intent(SignInActivity.this, TutorHomeActivity.class));
        finish();

    }

    private void showRegisterLayout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,R.style.DialogTheme);
        View itemView = View.inflate(this, R.layout.layout_register, null);

        TextInputEditText edt_first_name = itemView.findViewById(R.id.edt_first_name);
        TextInputEditText edt_last_name = itemView.findViewById(R.id.edt_last_name);
        TextInputEditText edt_phone_number = itemView.findViewById(R.id.edt_phone_number);

        Button btn_continue = itemView.findViewById(R.id.btn_register);

        // set data
        if(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber() != null &&
                (!TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber()))) {

            edt_phone_number.setText(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());

        }

        // set View
        builder.setView(itemView);
        AlertDialog dialog = builder.create();
        dialog.show();

        btn_continue.setOnClickListener(view -> {
            if(TextUtils.isEmpty(edt_first_name.getText().toString().trim()))
            {
                Toast.makeText(SignInActivity.this,"Please fill in first name field",Toast.LENGTH_SHORT).show();

            }else if(TextUtils.isEmpty(edt_last_name.getText().toString().trim())){
                Toast.makeText(SignInActivity.this,"Please fill in last name field",Toast.LENGTH_SHORT).show();

            }else if(TextUtils.isEmpty(edt_phone_number.getText().toString().trim())){
                Toast.makeText(SignInActivity.this,"Please fill in phone number field",Toast.LENGTH_SHORT).show();

            }else{
                TutorInfoModel model = new TutorInfoModel();
                model.setFirstName(edt_first_name.getText().toString());
                model.setLastName(edt_last_name.getText().toString());
                model.setPhoneNumber(edt_phone_number.getText().toString());
                model.setRating(0.0);

                tutorInfoRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .setValue(model)
                        .addOnFailureListener(e -> {
                            Toast.makeText(SignInActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(SignInActivity.this, "Registered Successfully!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            goToTutorHomeActivity(model);
                        });
            }
        });
    }

    private void showLoginLayout() {
        // inflate the layout
        AuthMethodPickerLayout authMethodPickerLayout = new AuthMethodPickerLayout
                .Builder(R.layout.activity_sign_in)
                .setPhoneButtonId(R.id.btn_phone_sign_in)
                .setGoogleButtonId(R.id.btn_google_sign_in)
                .build();
        // start firbase intent UI
        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setIsSmartLockEnabled(false)
                .setTheme(R.style.LoginTheme)
                .setAvailableProviders(providers)
                .build(), LOGIN_REQUEST_CODE);
    }
    // when sign-in flow is complete result goes here
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == LOGIN_REQUEST_CODE){
            IdpResponse idpResponse = IdpResponse.fromResultIntent(data);
            if(resultCode == RESULT_OK){
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            }else{
                Toast.makeText(SignInActivity.this,"[ERROR]: " + idpResponse.getError().getMessage(),Toast.LENGTH_SHORT).show();
            }
        }
    }
}
