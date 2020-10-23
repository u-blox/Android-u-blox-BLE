package com.ublox.BLE.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.ublox.BLE.R;

import static no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils.bytesToHex;
import static no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils.toByteArray;

/*
 * Note! This class is part of the experimental mesh features.
 * Stability is not guaranteed and user experience may be poor.
 */

public class KeyEntryFragment extends DialogFragment {
    private byte[] defaultNetKey = new byte[16];
    private byte[] defaultAppKey = new byte[16];
    private KeyEntryCallback callback;
    private String accept;

    public void setDefaultKeys(byte[] netKey, byte[] appKey) {
        if (netKey.length == 16) defaultNetKey = netKey;
        if (appKey.length == 16) defaultAppKey = appKey;
    }

    public void setAcceptKeys(String text, KeyEntryCallback callback) {
        accept = text;
        this.callback = callback;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_mesh_key, null);
        dialog.setView(view);
        dialog.setTitle(R.string.keyDialogText);
        set(fromBytes(defaultNetKey), R.id.editTextNetKey, view);
        set(fromBytes(defaultAppKey), R.id.editTextAppKey, view);
        dialog.setPositiveButton(accept, (dialogInterface, id) ->{
            if (callback == null) return;
            byte[] netKey = toBytes(textOf(R.id.editTextNetKey, view));
            byte[] appKey = toBytes(textOf(R.id.editTextAppKey, view));
            if (netKey.length != 16 || appKey.length != 16) return;
            callback.onKeys(netKey, appKey);
        });
        dialog.setNegativeButton("Cancel", (dialogInterface, id) ->{});

        return dialog.create();
    }

    private String textOf(int id, View v) {
        return ((EditText) v.findViewById(id)).getText().toString();
    }

    private void set(String text, int id, View v) {
        ((EditText) v.findViewById(id)).setText(text);
    }

    private String fromBytes(byte[] bytes) {
        return bytesToHex(bytes, false);
    }

    private byte[] toBytes(String hex) {
        return toByteArray(hex);
    }

    public interface KeyEntryCallback {
        void onKeys(byte[] netKey, byte[] appKey);
    }
}