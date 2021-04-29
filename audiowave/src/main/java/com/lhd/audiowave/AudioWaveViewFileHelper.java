package com.lhd.audiowave;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class AudioWaveViewFileHelper {

    public static String CACHE_FOLDER = "AudioWAVE_CACHE";

    public static File getFileFromUri(Context context, Uri uri, String extension) {
        try {
            String cacheFolder = context.getCacheDir() + "/" + CACHE_FOLDER + "/";
            File folderCache = new File(cacheFolder);
            if (!folderCache.exists()) {
                folderCache.mkdirs();
            }
            String outputPath = cacheFolder + "cache_" + System.currentTimeMillis() + "." + extension;
            InputStream initialStream = context.getContentResolver().openInputStream(uri);
            byte[] buffer = new byte[initialStream.available()];
            initialStream.read(buffer);

            File targetFile = new File(outputPath);
            OutputStream outStream = new FileOutputStream(targetFile);
            outStream.write(buffer);
            return targetFile;
        } catch (Exception e) {

        }
        return new File(uri.getPath());
    }

}
