package com.example.crypto_chat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import static com.example.crypto_chat.ServerIPConfig.SERVER_IP;
import cz.msebera.android.httpclient.Header;


import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import io.socket.client.Socket;
import io.socket.client.SocketIOException;
import io.socket.emitter.Emitter;

import static com.loopj.android.http.AsyncHttpClient.log;


/**
 * A chat fragment containing messages view and input form.
 */
public class MainFragment extends Fragment {

    private static final String TAG = "MainFragment";
    private static final int REQUEST_LOGIN = 0;
    private static final int REQUEST_KEY = 1;
    private ImageButton refresh_btn;
    private ImageButton decrypt_btn;
    private RecyclerView mMessagesView;
    private EditText mInputMessageView;
    private List<Message> mMessages = new ArrayList<Message>();
    private RecyclerView.Adapter mAdapter;
    private String mUsername;
    private String mEncryptKey;

    // SHA-256 해쉬 변수의 초기값을 지정해주지 않으면 LoginActivity에서 key 입력전에 메세지가 오면
    // AES 디코딩할때 key로 null값을 가져가서 AES256 디코드에 에러가 발생하여 앱이 크래시
    private String mEncryptSHA = "0000000000000000000000000000000000000000000000000000000000000000";
//    private Socket mSocket;
    private static AsyncHttpClient client = new AsyncHttpClient();

    SHA256Util sha256Util = new SHA256Util();

    public MainFragment() {
        super();
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mAdapter = new MessageAdapter(context, mMessages);
        if (context instanceof Activity){ }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getchatlist();
        startSignIn();
        Intent intent = new Intent(this.getContext(),SignUpActivity.class);
        startActivity(intent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMessagesView = (RecyclerView) view.findViewById(R.id.messages);
        mMessagesView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mMessagesView.setAdapter(mAdapter);

        refresh_btn = (ImageButton) view.findViewById(R.id.refresh_button);
        refresh_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearchatlist();
                decrypt_btn.setEnabled(true);
            }
        });
        decrypt_btn = (ImageButton) view.findViewById(R.id.decrypt_button);
        decrypt_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decrypt_message();
                decrypt_btn.setEnabled(false);
            }

        });
        mInputMessageView = (EditText) view.findViewById(R.id.message_input);
        mInputMessageView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int id, KeyEvent event) {
                if (id == R.integer.send || id == EditorInfo.IME_NULL) {
                    attemptSend();
                    decrypt_btn.setEnabled(false);
                    return true;
                }
                return false;
            }
        });
        mInputMessageView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (null == mUsername) return;
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ImageButton sendButton = (ImageButton) view.findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSend();
                decrypt_btn.setEnabled(false);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        Log.e("data:",""+data);
        // LoginActivity 에서 돌아온 경우 requestCode == REQUEST_LOGIN
        // ChangeKeyActivity 에서 돌아온 경우 requestCode == REQUEST_KEY
        if (requestCode == REQUEST_LOGIN) {
            if (Activity.RESULT_OK != resultCode) {
                getActivity().finish();
                return;
            }

            mUsername = data.getStringExtra("username");
            // 입력한 암호키로 AES-256에 필요한 해시를 만듭니다
            mEncryptKey = data.getStringExtra("encrypt_key");
            try {
                mEncryptSHA = sha256Util.getSHA256(mEncryptKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
            int numUsers = 1;

            Log.e("test", "Key SHA256 Hash:" + mEncryptSHA);

            addLog(getResources().getString(R.string.message_welcome));
        } else if (requestCode == REQUEST_KEY){
            if (Activity.RESULT_OK != resultCode) {
                getActivity().finish();
                return;
            }

            mEncryptKey = data.getStringExtra("encrypt_key");
            try {
                mEncryptSHA = sha256Util.getSHA256(mEncryptKey);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.e("test", "Changed SHA256 Hash:" + mEncryptSHA);

            addLog(getResources().getString(R.string.message_welcome));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // 메뉴띄우기
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_leave) {
            leave();
            return true;
        }
        if (id == R.id.action_changeKey) {
            changeKey();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void clearchatlist(){
        int len = mMessages.size();
        mMessages.clear();
        mAdapter.notifyItemRangeRemoved(0,len);
        getchatlist();
    }
    private void decrypt_message() {
        for (int i = 0; i < mMessages.size(); i++) {
            String str = mMessages.get(i).getMessage();
            Log.e("test","origin str"+str);
            mMessages.get(i).setMessage(aes256MessageDecode(str));
            mAdapter.notifyItemChanged(i);
        }
    }
    private void getchatlist(){
        client.get( SERVER_IP +"getMessages/", new AsyncHttpResponseHandler() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                int statusCodeReceived = statusCode;
                if(statusCodeReceived == 201){
                    Log.e("status","success");
                    String nameResponse = new String(responseBody);
                    try {
                        JSONArray jarray = new JSONArray(nameResponse);
                        for(int i=0;i< jarray.length();i++){
                            JSONObject jObject = jarray.getJSONObject(i);
                            addMessage(jObject.getString("Message_from"),jObject.getString("Message"));
                        }
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                    return;
                }
                else{
                    Log.e("status","error");
                    return;
                }
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) { }
        });
    }
    private void addLog(String message) {
        mMessages.add(new Message.Builder(Message.TYPE_LOG)
                .message(message).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }
    private void addMessage(String username, String message) {
        mMessages.add(new Message.Builder(Message.TYPE_MESSAGE)
                .username(username).message(message).build());
        mAdapter.notifyItemInserted(mMessages.size() - 1);
        scrollToBottom();
    }

    private void attemptSend() {
        Log.e("mUsername",""+mUsername);
        if (mUsername==null) return;
        String originalMessage = mInputMessageView.getText().toString().trim();
        Log.e("test", "original message is =" + originalMessage); // log original message
        if (TextUtils.isEmpty(originalMessage)) {
            mInputMessageView.requestFocus();
            return;
        }
        // 보냈으니 에딧텍스트 비워주자
        mInputMessageView.setText("");

        // AES256 암호화
        String message = aes256MessageEncode(originalMessage);
        Log.e("test", "Encoded message is =" + message);
        Log.e("test","Decoded message is="+ message+ originalMessage+aes256MessageDecode(message));
        // 암호화 되기 전의 메세지를 내 화면에 뿌려주자
        addMessage(mUsername, originalMessage);

        client.get( SERVER_IP +"insertMessage/?message=" + message + "&messagefrom=" + mUsername, new AsyncHttpResponseHandler() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                int statusCodeReceived = statusCode;
                if(statusCodeReceived == 201){
                    Log.e("status","success");
                    return;
                }
                else{
                    Log.e("status","error");
                    return;
                }
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) { }
        });
    }


    // 메세지 AES-256 암호화
    private String aes256MessageEncode (String str) {
        String aes256Encode = null;
        try {
            // 만든 SHA-256 해시로 초기화
            AES256Util aes256Util = new AES256Util(mEncryptSHA);
            try {
                // 스트링 암호화
                aes256Encode = aes256Util.aesEncode(str);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return aes256Encode;
    }

    //AES-256 복호화
    private String aes256MessageDecode (String str) {
        String aes256Decode = null;
        try {
            // 만든 SHA-256 해시로 초기화
            AES256Util aes256Util = new AES256Util(mEncryptSHA);
            try {
                // 스트링 복호화
                aes256Decode = aes256Util.aesDecode(str);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return aes256Decode;
    }
    private void startSignIn() {
        mUsername = null;
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivityForResult(intent, REQUEST_LOGIN);
    }

    private void changeKey() {
        mEncryptKey = null;
        // SHA-256 해시값을 null로 만들어버리면 바꾸고 있는동안 메세지 오면 앱이 크래시... 나버린다... 주륵...
        mEncryptSHA = "0000000000000000000000000000000000000000000000000000000000000000";
        Intent intent = new Intent(getActivity(), ChangeKeyActivity.class);
        startActivityForResult(intent, REQUEST_KEY);
    }

    private void leave() {
        mUsername = null;
        mEncryptKey = null;
        // 얘도... 마찬가지...
        mEncryptSHA = "0000000000000000000000000000000000000000000000000000000000000000";
//        mSocket.disconnect();
//        mSocket.connect();
        startSignIn();
    }

    private void scrollToBottom() {
        mMessagesView.scrollToPosition(mAdapter.getItemCount() - 1);
    }
}