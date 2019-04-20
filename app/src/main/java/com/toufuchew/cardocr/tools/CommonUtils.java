package com.toufuchew.cardocr.tools;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class CommonUtils {

    public static final String TAG = "CardOCR";

    public static final String APP_PATH = Environment.getExternalStorageDirectory().toString() + "/CardOCR/";

    public static String objToString(Object o) {
        String toString = "";
        if (o instanceof int[])
            toString += Arrays.toString((int[]) o);
        else if (o instanceof double[])
            toString += Arrays.toString((double[]) o);
        else if (o instanceof byte[])
            toString += Arrays.toString((byte[]) o);
        else if (o instanceof String[]) {
            toString += "[";
            for (String s : (String[])o)
                toString += (", " + s);
            toString = toString.replaceFirst(", ", "") + "]";
        }
        else toString += o.toString();
        return toString;
    }

    public static void info(Object msg) {
        info(false, msg);
    }

    public static void info(boolean error, Object msg) {
        if (error) {
            Log.e(TAG, objToString(msg));
            return;
        }
        Log.i(TAG, objToString(msg));
    }

    public static void prepareDirectory(String path) {
        if (!analyze(path, path)) {
            info("prepareDirectory error: Creation of directory " + path + " failed, check does Android Manifest have permission to write to external storage.");
            return;
        }
        info("Created directory at " + path);
    }

    public static void copyAssets(InputStream inputStream, String outputPath) {
        try {
            OutputStream out = new FileOutputStream(outputPath);
            byte buff[] = new byte[1024];
            int len;
            while ((len = inputStream.read(buff)) != -1) {
                out.write(buff, 0, len);
            }
            out.flush();
            out.close();
            info("File has copied to " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void emptyDirectory(String path) {
        File root = new File(path);
        if (root.exists()) {
            if (root.isDirectory()) {
                File[] child = root.listFiles();
                for (File c : child) {
                    if (!c.getName().contains(".txt"))
                        c.delete();
                }
            } else {
                info("cleanDirectory error: " + path + " is not a directory path");
                return;
            }
            info("Folder " + path + " has been emptied");
        }
    }


    private static boolean analyze(String path, String origin) {
        File dir = new File(path);
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                boolean result = analyze(dir.getParent(), origin);
                if (origin.equals(path))
                    return false;
                return result;
            }
            return true;
        }
        return path.equals(origin);
    }

    public static File openFile(String relativePath) {
        return new File(APP_PATH + relativePath);
    }

}
