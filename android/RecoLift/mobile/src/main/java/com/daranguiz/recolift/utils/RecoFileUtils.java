package com.daranguiz.recolift.utils;

import android.os.Environment;

import java.io.File;

public class RecoFileUtils {
    public RecoFileUtils() {
    }

    /* Get handle to file in public pictures directory */
    public File getFileInDcimStorage(String filename) {
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), filename);
        return file;
    }
}
