package com.example.crypto_chat;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import cz.msebera.android.httpclient.Header;

import static com.example.crypto_chat.ServerIPConfig.SERVER_IP;

public class SignUpActivity extends AppCompatActivity {
    private Button sumbit_btn;
    private Button skip_btn;
    private EditText input_id;
    private EditText input_name;
    private EditText input_pwd;
    private String passwordHashInput;
    private SignUpActivity signUpFragment;
    SHA256Util sha256Util = new SHA256Util();
    private static AsyncHttpClient client = new AsyncHttpClient();

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_signup);
        signUpFragment= this;
        input_id = findViewById(R.id.id_input);
        input_name= findViewById(R.id.name_input);
        input_pwd = findViewById(R.id.password_input);
        skip_btn = findViewById(R.id.sign_up_skip_button);
        skip_btn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        sumbit_btn = findViewById(R.id.sign_up_button);
        sumbit_btn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptsignin();
            }
        });
    }

    private void attemptsignin() {
        // 에러를 리셋 (전에 에러가 나서 다시 실행할 경우 이미 에러가 set 되있음)
        input_id.setError(null);
        input_name.setError(null);
        input_pwd.setError(null);

        String studentId = input_id.getText().toString().trim();
        String password = input_pwd.getText().toString().trim();
        String name = input_name.getText().toString().trim();

        // 각각의 폼이 비어있을 경우 에러발생 및 알려줌
        if (TextUtils.isEmpty(name)) {
            input_name.setError(getString(R.string.error_field_required));
            input_name.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(studentId)) {
            input_id.setError(getString(R.string.error_field_required));
            input_id.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            input_pwd.setError(getString(R.string.error_field_required));
            input_pwd.requestFocus();
            return;
        }

//        studentId = input_id.getText().toString();
//        password = input_pwd.getText().toString();
//        name = input_name.getText().toString();
        // 패스워드를 서버에 보내 확인하기 전에 SHA-256으로 해시로 만듬
        try {
            passwordHashInput = sha256Util.getSHA256(password);
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestParams params = new RequestParams();
        params.put("studentId", studentId);
        params.put("password", passwordHashInput);
        params.put("name", name);

        client.get( SERVER_IP +"register/?studentId="+studentId+"&password="+passwordHashInput+"&name="+name, new AsyncHttpResponseHandler() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                int statusCodeReceived = statusCode;
                if(statusCodeReceived == 201){
                    Log.e("status","success");
                    finish();
                    return;
                }
                else{
                    Log.e("status","error");
                    return;
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e("status","error");
            }
        });
    }
}
