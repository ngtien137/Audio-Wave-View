package com.lhd.demoaudiowaveview;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lhd.audiowave.AudioWaveView;

import static com.lhd.audiowave.AudioWaveView.ENABLE_LOG;

public class MainActivity extends AppCompatActivity implements AudioWaveView.IAudioListener {

    private String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private AudioWaveView audioWaveView;
    private LinearLayout llLoading;
    private TextView tvLoading;
    private TextView tvMode;
    private EditText edtMin;
    private EditText edtProgress;
    private EditText edtMax;
    private SimpleRxTask simpleRxTask;

    @Override
    public void onBackPressed() {
        if (simpleRxTask != null && simpleRxTask.isRunning()) {
            simpleRxTask.cancel();
        } else
            super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        audioWaveView = findViewById(R.id.audioView);
        llLoading = findViewById(R.id.llLoading);
        tvLoading = findViewById(R.id.tvLoading);
        tvMode = findViewById(R.id.tvMode);
        edtProgress = findViewById(R.id.edtProgress);
        edtMin = findViewById(R.id.edtMin);
        edtMax = findViewById(R.id.edtMax);
        audioWaveView.setAudioListener(this);
        if (!checkPermission()) {
            grantPermission();
        }
        audioWaveView.setInteractedListener(new AudioWaveView.IInteractedListener() {

            @Override
            public void onTouchDownAudioBar() {

            }

            @Override
            public void onTouchReleaseAudioBar() {

            }

            @Override
            public void onAudioBarScaling() {

            }

            @Override
            public void onRangerChanging(float minProgress, float maxProgress, AudioWaveView.AdjustMode adjustMode) {
                //eLog("RangeChanging: Range[", minProgress, ",", maxProgress, "], AdjustMode:", adjustMode);
            }

            @Override
            public void onStopFling(boolean isForcedStop) {
                eLog("Stop Fling - ByForce: ", isForcedStop);
            }

            @Override
            public void onStartFling() {
                eLog("OnStart Fling");
            }
        });
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
                final String path = getAudioPath(uri);
                simpleRxTask = new SimpleRxTask(new SimpleRxTask.Listener() {
                    @Override
                    public void onStart() {
                        llLoading.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onComplete() {
                        eLog("Loading complete");
                        llLoading.setVisibility(View.GONE);
                    }

                    @Override
                    public void onDoing() {
                        audioWaveView.setAudioPath(path);
                    }

                    @Override
                    public void onCancel() {
                        llLoading.setVisibility(View.GONE);
                    }
                });
                simpleRxTask.start();
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

    @SuppressLint("SetTextI18n")
    @Override
    public void onLoadingAudio(int progress, boolean prepareView) {
        (new Handler(getMainLooper())).post(() -> {
            tvLoading.setText("Loading " + progress + "%");
        });
    }

    @Override
    public void onLoadingAudioComplete() {
        
    }

    @Override
    public void onLoadingAudioError(Exception exceptionError) {

    }

    @SuppressLint("SetTextI18n")
    public void changeMode(View view) {
        if (audioWaveView.getModeEdit() == AudioWaveView.ModeEdit.NONE) {
            audioWaveView.setModeEdit(AudioWaveView.ModeEdit.CUT);
            audioWaveView.setLeftAnchorAlignImage(getAppDrawable(R.drawable.ic_anchor_left_cut), AudioWaveView.Align.RIGHT, AudioWaveView.Align.BOTTOM);
            audioWaveView.setRightAnchorAlignImage(getAppDrawable(R.drawable.ic_anchor_right_cut), AudioWaveView.Align.LEFT, AudioWaveView.Align.BOTTOM);
            audioWaveView.setTextValuePullTogether(true);
        } else if (audioWaveView.getModeEdit() == AudioWaveView.ModeEdit.CUT) {
            audioWaveView.setModeEdit(AudioWaveView.ModeEdit.TRIM);
            audioWaveView.setLeftAnchorAlignImage(getAppDrawable(R.drawable.ic_anchor_left_trim), AudioWaveView.Align.LEFT, AudioWaveView.Align.BOTTOM);
            audioWaveView.setRightAnchorAlignImage(getAppDrawable(R.drawable.ic_anchor_right_trim), AudioWaveView.Align.RIGHT, AudioWaveView.Align.BOTTOM);
            audioWaveView.setTextValuePullTogether(false);
        } else if (audioWaveView.getModeEdit() == AudioWaveView.ModeEdit.TRIM) {
            audioWaveView.setModeEdit(AudioWaveView.ModeEdit.NONE);
        }
        tvMode.setText("Mode: " + audioWaveView.getModeEdit());
    }

    public void applyRange(View view) {
        try {
            float minValue = Float.parseFloat(edtMin.getText().toString());
            float maxValue = Float.parseFloat(edtMax.getText().toString());
            audioWaveView.setRangeProgress(minValue, maxValue);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid value", Toast.LENGTH_SHORT).show();
        }
    }

    public void setProgress(View view) {
        try {
            float progress = Float.parseFloat(edtProgress.getText().toString());
            audioWaveView.setProgress(progress, true);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid value", Toast.LENGTH_SHORT).show();
        }
    }

    private Drawable getAppDrawable(@DrawableRes int id) {
        return ContextCompat.getDrawable(this, id);
    }


}