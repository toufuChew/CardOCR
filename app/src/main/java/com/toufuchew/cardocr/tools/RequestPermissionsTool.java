package com.toufuchew.cardocr.tools;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;

public interface RequestPermissionsTool {
    final int MULTIPLE_PERMISSIONS = 0x10;

    /**
     * request manifests permissions
     * @param context
     * @param permissions
     * @return true if all permissions have been granted, otherwise return false
     */
    boolean requestPermissions(AppCompatActivity context, String[] permissions);

    boolean isPermissionsGranted(Context context, String... permissions);

    void onPermissionDenied(AppCompatActivity context);
}
