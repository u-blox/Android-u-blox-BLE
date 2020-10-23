package com.ublox.BLE.fragments;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import com.ublox.BLE.R;
import com.ublox.BLE.activities.MainActivity;
import com.ublox.BLE.adapters.ChatAdapter;
import com.ublox.BLE.utils.PacketChunker;

import java.util.Date;


public class ChatFragment extends Fragment {

    private IChatInteractionListener mListener;

    public static ChatFragment newInstance() {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    private ChatAdapter chatAdapter;
    private ListView lvChat;
    private BluetoothGattCharacteristic characteristic;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        chatAdapter = new ChatAdapter(getActivity());

        lvChat = view.findViewById(R.id.lvChat);
        lvChat.setAdapter(chatAdapter);

        Button bSend = view.findViewById(R.id.bSend);
        final EditText etMessage = view.findViewById(R.id.etMessage);
        Switch swSendCR = view.findViewById(R.id.swSendWithCR);

        bSend.setOnClickListener(v -> {
            if (characteristic != null && ((MainActivity)getActivity()).isConnected()) {
                String msg = etMessage.getText().toString();
                if (swSendCR.isChecked()) {
                    msg = msg + "\r";
                }
                ChatAdapter.ChatMessage message = new ChatAdapter.ChatMessage(msg, new Date().toString(), true);
                chatAdapter.addMessage(message);
                for(byte[] chunk: PacketChunker.chunkify(message.message.getBytes(), 20)) {
                    mListener.onSendMessage(characteristic, chunk);
                }
            } else {
                Toast.makeText(getActivity(), "You need to be connected to a device in order to do this", Toast.LENGTH_LONG).show();
            }
            etMessage.setText("");
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (IChatInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MeshListInterface");
        }
    }

    public void addMessage(byte[] data) {
        ChatAdapter.ChatMessage message = new ChatAdapter.ChatMessage(new String(data), new Date().toString(), false);
        chatAdapter.addMessage(message);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void setCharacteristicFifo(BluetoothGattCharacteristic characteristic) {
        try {
            if (this.characteristic == null) {
                this.characteristic = characteristic;
                chatAdapter = new ChatAdapter(getActivity());
                lvChat.setAdapter(chatAdapter);
                mListener.onNotify(characteristic, true);
            } else if (!this.characteristic.equals(characteristic)) {
                this.characteristic = characteristic;
                chatAdapter = new ChatAdapter(getActivity());
                lvChat.setAdapter(chatAdapter);
                mListener.onNotify(characteristic, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface IChatInteractionListener {
        void onSendMessage(BluetoothGattCharacteristic characteristic, byte[] message);
        void onNotify(BluetoothGattCharacteristic characteristic, boolean enabled);
    }

}
