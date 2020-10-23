package com.ublox.BLE.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.ublox.BLE.R;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

/*
 * Note! This class is part of the experimental mesh features.
 * Stability is not guaranteed and user experience may be poor.
 */

public class MeshProxyConnectionFragment extends Fragment {
    private MeshProxyConnectionListener listener;

    public MeshProxyConnectionFragment() {

    }

    public static MeshProxyConnectionFragment newInstance() {
        MeshProxyConnectionFragment fragment = new MeshProxyConnectionFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mesh_proxy_connection, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button back = view.findViewById(R.id.buttonMeshBack);
        back.setOnClickListener(v -> {
            if (listener == null) return;
            listener.close();
        });

        Button connect = view.findViewById(R.id.buttonMeshReconnect);
        connect.setOnClickListener(v -> {
            if (listener == null) return;
            listener.connect();
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof MeshProxyConnectionListener) {
            listener = (MeshProxyConnectionListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public void setStatus(String status) {
        View v = getView();
        if (v == null) return;

        TextView text = v.findViewById(R.id.textViewProxyStatus);
        text.setText(status);
    }

    public void setVisible(boolean visible) {
        View v = getView();
        if (v == null) return;

        int visibility = visible ? VISIBLE : INVISIBLE;

        v.findViewById(R.id.textViewMeshDrop).setVisibility(visibility);
        v.findViewById(R.id.buttonMeshReconnect).setVisibility(visibility);
        v.findViewById(R.id.buttonMeshBack).setVisibility(visibility);
    }

    public interface MeshProxyConnectionListener {
        void connect();
        void close();
    }
}
