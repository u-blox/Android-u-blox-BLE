package com.ublox.BLE.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ublox.BLE.R;

import java.util.ArrayList;


public class ChatAdapter extends BaseAdapter {

    public static class ChatMessage {
        public String message;
        public String date;
        public boolean isMe;

        public ChatMessage(String message, String date, boolean isMe) {
            this.message = message;
            this.date = date;
            this.isMe = isMe;
        }
    }

    private ArrayList<ChatMessage> messages = new ArrayList<>();
    private Context mContext;


    public ChatAdapter(Context context) {
        this.mContext = context;
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ChatMessage message = messages.get(position);

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view;

        if (message.isMe) {
            // Get left chat view
            view = inflater.inflate(R.layout.chat_me, parent, false);
        } else {
            // Get right chat view
            view = inflater.inflate(R.layout.chat_ble, parent, false);
        }

        TextView tvMessage = (TextView) view.findViewById(R.id.tvMessage);
        TextView tvDateTime = (TextView) view.findViewById(R.id.tvDateTime);

        tvMessage.setText(message.message);
        tvDateTime.setText(message.date);

        return view;
    }

}
