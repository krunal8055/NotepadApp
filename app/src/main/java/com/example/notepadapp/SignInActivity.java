package com.example.notepadapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.util.TimeUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class SignInActivity extends AppCompatActivity {
    LinearLayout phoneNoLayout,otpLayout;
    Button sendBtn, verifyBtn;
    EditText phoneNoEditText, otpEditText;
    TextView resendText,otpSendTxt;


    private PhoneAuthProvider.ForceResendingToken forceResendingToken; //IF CODE SEND FAILED, WILL USE TO RESEND CODE AGAIN
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String mVerification;
    private static final String TAG = "MAIN_TAG";
    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        //ALL INSTANCE
        phoneNoLayout = findViewById(R.id.phoneLL);   // Linear Layout
        otpLayout = findViewById(R.id.otpLL);    // Linear Layout
        sendBtn = findViewById(R.id.getOtp_btn);   // Send Btn
        verifyBtn = findViewById(R.id.login_btn);  // Verify and Login btn
        phoneNoEditText = findViewById(R.id.phone_no_login);  // field to enter phone no for verification
        otpEditText = findViewById(R.id.otp_login); // field to enter otp for verification
        resendText = findViewById(R.id.resend_otp_btn); //clickable textview to resend otp
        otpSendTxt = findViewById(R.id.otp_sent_text); // text that will confirm with user that phone no is correct or not

        //VISIBILITY FOR LINEAR LAYOUT
        phoneNoLayout.setVisibility(View.VISIBLE);
        otpLayout.setVisibility(View.GONE);

        //FIREBASE
        firebaseAuth = FirebaseAuth.getInstance();

        //PROGRESS DIALOG
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait....");
        progressDialog.setCanceledOnTouchOutside(false);

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                /** THIS IS CALLBACK WILL BE INVOKED IN TWO SITUATIONS :
                 1] INSTANT VERIFICATION.
                 -IN SOME CASES THE PHONE NO CAN BE INSTANTLY VERIFIED WITHOUT NEEDING TO SEND OR ENTER VERIFICATION CODE
                 2] AUTO-RETRIEVAL
                 -ON SOME DEVICES GOOGLE PLAY SERVICES CAN AUTOMATICALLY DETECT THE INCOMING VERIFICATION SMS AND PERFORM VERIFICATION WITHOUT USER ACTION.**/
                signInWithPhoneAuthCredentials(phoneAuthCredential);
            }
            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                /** THIS CALLBACK IS INVOKED IN AN INVALID REQUEST FOR VERIFICATION IS MADE. FOR INSTANCE IF THE PHONE NO FORMAT IS NOT VALID.  **/
                progressDialog.dismiss();
                Toast.makeText(SignInActivity.this,""+e.getMessage(),Toast.LENGTH_LONG).show();

            }

            @Override
            public void onCodeSent(@NonNull String verificationID, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                super.onCodeSent(verificationID, forceResendingToken);
                /** THE SMS VERIFICATION CODE HAS BEEN SENT TO THE PROVIDED NUMBER, WE NOW NEED TO ASK THE USER TO ENTER THE CODE
                 AND THEN CONSTRUCT A CREDENTIAL BY COMBINING THE CODE WITH A VERIFICATION ID. **/
                Log.e(TAG,"ON CODE SENT: "+verificationID);
                mVerification = verificationID;
                forceResendingToken = token;
                progressDialog.dismiss();

                phoneNoLayout.setVisibility(View.GONE);
                otpLayout.setVisibility(View.VISIBLE);
                Toast.makeText(SignInActivity.this,"Verification Code Sent....",Toast.LENGTH_LONG).show();

                otpSendTxt.setText("Please Enter Verification code that you receive on you phone number "+phoneNoEditText.getText().toString().trim());
            }
        };
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String phone_no = phoneNoEditText.getText().toString().trim();
                if(TextUtils.isEmpty(phone_no)){
                    Toast.makeText(SignInActivity.this,"Please Enter Phone no.",Toast.LENGTH_LONG).show();
                }else{
                    startPhoneNoVerification(phone_no);
                }
            }
        });

        resendText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String phone_no = phoneNoEditText.getText().toString().trim();
                if(TextUtils.isEmpty(phone_no)){
                    Toast.makeText(SignInActivity.this,"Please Enter Phone no.",Toast.LENGTH_LONG).show();
                }else{
                    resendVerificationCode(phone_no, forceResendingToken);
                }
            }
        });

        verifyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String otp = otpEditText.getText().toString().trim();
                if(TextUtils.isEmpty(otp)){
                    Toast.makeText(SignInActivity.this,"Please Enter Verification Code.",Toast.LENGTH_LONG).show();
                }else{
                    verifyCode(mVerification,otp);
                }
            }
        });
    }

    private void startPhoneNoVerification(String phone_no) {
        progressDialog.setTitle("Verifying Phone no....");
        progressDialog.show();

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phone_no)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void resendVerificationCode(String phone_no, PhoneAuthProvider.ForceResendingToken token) {
        progressDialog.setTitle("Resending the Verification code....");
        progressDialog.show();

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phone_no)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .setForceResendingToken(token)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyCode(String mVerification, String otp) {
        progressDialog.setTitle("Please wait Verifying Code....");
        progressDialog.show();

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerification,otp);
        signInWithPhoneAuthCredentials(credential);
    }

    private void signInWithPhoneAuthCredentials(PhoneAuthCredential credential) {
        progressDialog.setTitle("Logging in....");

        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        progressDialog.dismiss();
                        Toast.makeText(SignInActivity.this,"Login Success!",Toast.LENGTH_LONG).show();

                        Intent i = new Intent(SignInActivity.this,MainActivity.class);
                        startActivity(i);
                        finish();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(SignInActivity.this,""+e.getMessage(),Toast.LENGTH_LONG).show();
                    }
                });
    }

}