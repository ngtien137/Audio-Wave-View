package com.lhd.audiowave;

import android.os.Build;

import androidx.annotation.RequiresApi;

public class AudioWaveViewException extends Exception {
    public AudioWaveViewException() {
    }

    public AudioWaveViewException(String message) {
        super(message);
    }

    public AudioWaveViewException(String message, Throwable cause) {
        super(message, cause);
    }

    public AudioWaveViewException(Throwable cause) {
        super(cause);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public AudioWaveViewException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
