package com.toufuchew.cardocr.tools;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RequestPermissionsAssistant implements RequestPermissionsTool {

    @Override
    public boolean requestPermissions(AppCompatActivity context, String[] permissions) {
        boolean hasGranted = true;

        Map<Integer, String> requestCodeToPermission = new HashMap<>();
        for (int i = 0; i < permissions.length; i++)
            requestCodeToPermission.put(i, permissions[i]);

        Set<Map.Entry<Integer, String>> perms = requestCodeToPermission.entrySet();
        for (Map.Entry<Integer, String> permission : perms) {
            if (!isPermissionsGranted(context, permission.getValue())) {
                hasGranted = false;
                if (ActivityCompat.shouldShowRequestPermissionRationale(context, permission.getValue())) {
                    ConfirmationDialog.newInstance(permission.getKey(), permission.getValue())
                            .show(context.getSupportFragmentManager(), ConfirmationDialog.CONFIRMATION);
                } else {
                    ActivityCompat.requestPermissions(context, permissions, MULTIPLE_PERMISSIONS);
                    break;
                }
            }
        }
        return hasGranted;
    }

    @Override
    public boolean isPermissionsGranted(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    @Override
    public void onPermissionDenied(AppCompatActivity context) {

    }

    public static final class ConfirmationDialog extends DialogFragment {

        private static final String ARG_PERMISSION = "permission";
        private static final String ARG_REQUEST_CODE = "request_code";

        public static final String CONFIRMATION = "Confirm";

        public static ConfirmationDialog newInstance(int permissionKey, String permission) {
            ConfirmationDialog dialog = new ConfirmationDialog();
            Bundle bundle = new Bundle();
            bundle.putInt(ARG_REQUEST_CODE, permissionKey);
            bundle.putString(ARG_PERMISSION, permission);
            dialog.setArguments(bundle);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage("Permissions need to allow")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(getActivity(),
                                    new String[]{getArguments().getString(ARG_PERMISSION)}, getArguments().getInt(ARG_REQUEST_CODE));
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Toast.makeText(getActivity(), "Application Not Available", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .create();
        }

    }
}
