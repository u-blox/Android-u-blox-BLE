package com.ublox.BLE.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.ublox.BLE.R;
import com.ublox.BLE.mesh.C209Node;

import java.util.ArrayList;
import java.util.List;

/*
 * Note! This class is part of the experimental mesh features.
 * Stability is not guaranteed and user experience may be poor.
 */

public class MeshListFragment extends Fragment {
    private MeshListListener listener;
    private List<C209Node> nodes;
    private BaseAdapter adapter;

    public MeshListFragment() {
        nodes = new ArrayList<>();
        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return nodes.size();
            }

            @Override
            public Object getItem(int position) {
                return nodes.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = convertView == null
                    ? getActivity().getLayoutInflater().inflate(R.layout.list_item_mesh_node, null)
                    : convertView;
                TextView v = view.findViewById(R.id.nodeName);
                v.setText(nodes.get(position).getName());
                return view;
            }
        };
    }

    public static MeshListFragment newInstance() {
        MeshListFragment fragment = new MeshListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mesh_list, container, false);
        ListView v = view.findViewById(R.id.meshList);
        v.setOnItemClickListener((parent, listView, position, id) -> selectNode(position));
        v.setAdapter(adapter);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof MeshListListener) {
            listener = (MeshListListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public void setList(List<C209Node> nodes) {
        this.nodes.clear();
        this.nodes.addAll(nodes);
        adapter.notifyDataSetChanged();
    }

    private void selectNode(int position) {
        if (listener == null || position >= nodes.size()) return;
        listener.nodeSelected(nodes.get(position));
    }

    public interface MeshListListener {
        void nodeSelected(C209Node node);
    }

}
