package com.lhd.demoaudiowaveview;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import com.lhd.audiowave.AudioWaveView;

import static com.lhd.audiowave.AudioWaveView.ENABLE_LOG;

public class MainActivity extends AppCompatActivity {

    private String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private AudioWaveView audioWaveView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        audioWaveView = findViewById(R.id.audioView);
        if (!checkPermission()) {
            grantPermission();
        }
    }

    private boolean checkPermission() {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    private void grantPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, 1);
        }
    }

    public void chooseAudio(View v) {
        if (checkPermission()) {
            Intent intent_upload = new Intent();
            intent_upload.setAction(Intent.ACTION_PICK);
            intent_upload.setData(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent_upload, 1);
        } else {
            grantPermission();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {

            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                eLog(uri);
                String path = getAudioPath(uri);
                audioWaveView.setAudioPath(path);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private String getAudioPath(Uri uri) {
        String[] data = {MediaStore.Audio.Media.DATA};
        CursorLoader loader = new CursorLoader(getApplicationContext(), uri, data, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    public void eLog(Object... message) {
        if (ENABLE_LOG) {
            StringBuilder mes = new StringBuilder();
            for (Object sMes : message
            ) {
                String m = "null";
                if (sMes != null)
                    m = sMes.toString();
                mes.append(m);
            }
            Log.e("AudioWaveViewLog", mes.toString());
        }
    }
}