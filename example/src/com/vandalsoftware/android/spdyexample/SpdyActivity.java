package com.vandalsoftware.android.spdyexample;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class SpdyActivity extends Activity {
    private static final String TAG = "spdy";
    private LogView mLogView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mLogView = (LogView) findViewById(R.id.logview);
        Button startBtn = (Button) findViewById(R.id.start);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SpdyTask().execute();
            }
        });
        log("Ready.");
    }

    private void log(String message) {
        Log.d(TAG, message);
        mLogView.log(message);
    }

    class SpdyTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            log("Starting...");
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            log("Finished.");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            return null;
        }
    }
}
