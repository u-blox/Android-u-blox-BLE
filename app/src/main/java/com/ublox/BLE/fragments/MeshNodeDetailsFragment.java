package com.ublox.BLE.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.ublox.BLE.R;
import com.ublox.BLE.mesh.C209Node;
import com.ublox.BLE.mesh.C209SensorData;
import com.ublox.BLE.view.SimpleGraph;

import java.util.ArrayList;

/*
 * Note! This class is part of the experimental mesh features.
 * Stability is not guaranteed and user experience may be poor.
 */

public class MeshNodeDetailsFragment extends Fragment {
    private C209Node node;

    public MeshNodeDetailsFragment() {}

    public static MeshNodeDetailsFragment newInstance() {
        MeshNodeDetailsFragment fragment = new MeshNodeDetailsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mesh_node_details, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EditText name = view.findViewById(R.id.nodeName);
        name.setText(node.getName());
        update();
    }

    public void setNode(C209Node node) {
        this.node = node;
    }

    public void update() {
        if (node == null) return;

        long timeNow = System.currentTimeMillis();

        ArrayList<Float> timeTemp = new ArrayList<>();
        ArrayList<Float> temps = new ArrayList<>();
        for (C209SensorData data: node.getTemperature()) {
            temps.add((float) data.getValue());
            timeTemp.add((float) ((data.getTime() - timeNow)/1000));
        }
        SimpleGraph graph = getView().findViewById(R.id.simpleGraph);
        graph.setValues(timeTemp, temps);

        ArrayList<Float> timeLux = new ArrayList<>();
        ArrayList<Float> lux = new ArrayList<>();
        for (C209SensorData data: node.getIlluminance()) {
            lux.add((float) data.getValue());
            timeLux.add((float) ((data.getTime() - timeNow)/1000));
        }
        SimpleGraph graph2 = getView().findViewById(R.id.simpleGraph2);
        graph2.setValues(timeLux, lux);
    }
}
