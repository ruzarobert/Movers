package com.robertruzsa.movers.auth;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.parse.FunctionCallback;
import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.robertruzsa.movers.view.GetPhoneNumberActivity;
import com.robertruzsa.movers.view.SignUpActivity;
import com.robertruzsa.movers.view.VerificationActivity;
import com.santalu.maskedittext.MaskEditText;

import org.json.JSONObject;

import java.util.HashMap;

public class Authentication {

    Context context;

    static Authentication _instance;

    private Authentication(Context context) {
        this.context = context;
    }

    public static Authentication Get(Context context) {
        if (_instance == null)
            _instance = new Authentication(context);
        return _instance;
    }

    public void anonymousLogin() {
        if (ParseUser.getCurrentUser() == null) {
            ParseAnonymousUtils.logIn(new LogInCallback() {
                @Override
                public void done(ParseUser user, ParseException e) {
                    if (e == null)
                        Log.i("Info", "Anonymous login successful");
                    else
                        Log.i("Info", "Anonymous login failed");
                }
            });
        } else {
            if (ParseUser.getCurrentUser().get("userType") != null) {
                Log.i("Info", "Redirecting as " + ParseUser.getCurrentUser().get("userType"));
                redirectActivity();
            }
        }
    }

    public void redirectActivity() {
        if (ParseUser.getCurrentUser().get("userType").equals("client")) {
            Intent intent = new Intent(context, GetPhoneNumberActivity.class);
            context.startActivity(intent);
        } else {
            // Start MoverActivity
        }
    }

    public void requestPhoneVerification(final String phoneNumber, final ProgressDialog progressDialog) {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("phoneNumber", phoneNumber);
        ParseCloud.callFunctionInBackground("sendVerificationCode", params, new FunctionCallback<JSONObject>() {
            public void done(JSONObject response, ParseException e) {
                if (e == null) {
                    Log.d("Response", "No exceptions!");
                    Toast.makeText(context, "A verifikációs kód elküldésre került.", Toast.LENGTH_LONG).show();

                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        Intent intent = new Intent(context, VerificationActivity.class);
                        intent.putExtra("phoneNumber", phoneNumber);
                        context.startActivity(intent);
                    }

                } else {
                    Log.i("Response", "Exception: " + e);
                    Toast.makeText(context, "A verifikációs kód elküldése során hiba történt. Próbálkozzon újra.", Toast.LENGTH_LONG).show();
                    if (progressDialog != null)
                        progressDialog.dismiss();
                }
            }
        });
    }

    public void verifyEnteredCode(String code, String phoneNumber, final MaskEditText verificationCodeEditText) {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("phoneNumber", phoneNumber);
        params.put("phoneVerificationCode", code);
        ParseCloud.callFunctionInBackground("verifyPhoneNumber", params, new FunctionCallback<String>() {
            public void done(String response, ParseException e) {
                if (e == null) {
                    Log.d("Response", "no exceptions! " + response);
                    ParseUser.becomeInBackground(response, new LogInCallback() {
                        @Override
                        public void done(ParseUser parseUser, ParseException e) {
                            if (e == null) {
                                Log.d("Response", "no exceptions! ");
                                Toast.makeText(context, "Sikeres autentikcáió.", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(context, SignUpActivity.class);
                                context.startActivity(intent);
                            } else {
                                Log.d("Response", "Exception: " + e);
                                Toast.makeText(context, "Az autentikcáió során hiba történt. Próbálkozzon újra." + e, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                } else {
                    Log.d("Response", "Exception: " + response + e);
                    if (verificationCodeEditText != null)
                        verificationCodeEditText.setError("Érvénytelen kód.");
                    else
                        Toast.makeText(context, "Érvénytelen kód.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
