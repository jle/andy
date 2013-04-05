package com.vandalsoftware.android.spdyexample;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.vandalsoftware.android.spdy.SpdyClient;
import com.vandalsoftware.android.spdy.SpdyGet;
import com.vandalsoftware.android.spdy.SpdyHttpRequest;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class SpdyActivity extends Activity {
    private static final String TAG = "spdy";
    private LogView mLogView;
    private Socket mSock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mLogView = (LogView) findViewById(R.id.logview);
        Button startBtn = (Button) findViewById(R.id.start);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ConnectTask().execute();
            }
        });
        Button stopBtn = (Button) findViewById(R.id.stop);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                log("Closing.");
                Socket s = mSock;
                if (s != null) {
                    try {
                        s.close();
                        Log.d(TAG, "socket closed.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                log("Finished.");
            }
        });
        log("Ready.");
    }

    private void log(String message) {
        Log.d(TAG, message);
        mLogView.log(message);
    }

    class ConnectTask extends AsyncTask<Void, Void, Socket> {
        @Override
        protected void onPostExecute(Socket s) {
            mSock = s;
        }

        @Override
        protected Socket doInBackground(Void... voids) {
            Socket s = null;
            try {
                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(null, null, null);
                SpdyClient client = new SpdyClient(ctx.getSocketFactory());
                SpdyHttpRequest httpMessage = new SpdyGet("https://api.twitter.com/1.1/statuses/home_timeline.json");
                client.execute(httpMessage);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (false && s != null) {
                    try {
                        s.close();
                        Log.d(TAG, "socket closed.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return s;
        }

    }
}
