package com.ublox.BLE.fragments;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.ublox.BLE.R;
import com.ublox.BLE.utils.GattAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ServicesFragment extends Fragment {

    public interface IServiceFragmentInteraction {
        void onRead(BluetoothGattCharacteristic characteristic);
        void onWrite(BluetoothGattCharacteristic characteristic, byte[] value);
        void onNotify(BluetoothGattCharacteristic characteristic);
    }

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    public static ServicesFragment newInstance() {
        ServicesFragment fragment = new ServicesFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public ServicesFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_services, container, false);
    }


    ExpandableListView mGattServicesList;

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.bBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view.findViewById(R.id.llCharacteristic).setVisibility(View.GONE);
            }
        });


        mGattServicesList = (ExpandableListView) view.findViewById(R.id.gatt_services_list);
    }

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    List<BluetoothGattService> mServices = new ArrayList<>();

    public void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        mServices = gattServices;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();

            currentServiceData.put(
                    LIST_NAME, GattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();

                currentCharaData.put(
                        LIST_NAME, GattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                getActivity(),
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
    }

    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        getView().findViewById(R.id.llCharacteristic).setVisibility(View.VISIBLE);
                        getView().findViewById(R.id.llWrite).setVisibility(View.GONE);
                        String properties = "";



                        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            properties += " READ";
                            currentUuid = characteristic.getUuid().toString();
                            mInteractionListener.onRead(characteristic);
                        }

                        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            properties += " NOTIFY";
                            currentUuid = characteristic.getUuid().toString();
                            mInteractionListener.onNotify(characteristic);
                        }

                        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                            properties += " WRITE";

                            getView().findViewById(R.id.llWrite).setVisibility(View.VISIBLE);

                            getView().findViewById(R.id.bSend).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    EditText etSendString = (EditText) getView().findViewById(R.id.etSendString);
                                    EditText etSendHex = (EditText) getView().findViewById(R.id.etSendHex);
                                    EditText etSendInt = (EditText) getView().findViewById(R.id.etSendInt);
                                    String value = "";
                                    byte[] bytes = null;
                                    if (!etSendString.getText().toString().trim().isEmpty()) {
                                        value = etSendString.getText().toString();
                                        bytes = value.getBytes();
                                    } else if (!etSendHex.getText().toString().trim().isEmpty()) {
                                        value = etSendHex.getText().toString();
                                        bytes = hexStringToByteArray(value);
                                    } else if (!etSendInt.getText().toString().trim().isEmpty()) {
                                        value = etSendInt.getText().toString();
                                        try {
                                            int intVal = Integer.parseInt(value);
                                            bytes = new byte[]{(byte) intVal};
                                        } catch (Exception e) {}
                                    }
                                    if (bytes != null && bytes.length != 0) {
                                        try {
                                            mInteractionListener.onWrite(characteristic, bytes);
                                        } catch (Exception e) {}
                                    }
                                    etSendString.setText("");
                                    etSendHex.setText("");
                                    etSendInt.setText("");
                                }
                            });
                        }

                        TextView tvCharacteristicName = (TextView) getView().findViewById(R.id.tvCharacteristicName);
                        tvCharacteristicName.setText(GattAttributes.lookup(currentUuid, "Unknown characteristic"));

                        TextView tvValue = (TextView) getView().findViewById(R.id.tvValue);
                        tvValue.setText("");

                        TextView tvProperties = (TextView) getView().findViewById(R.id.tvProperties);
                        tvProperties.setText(properties.trim());
                        return true;
                    }
                    return false;
                }
            };

    public String currentUuid = "";

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public boolean hasGottenServices() {
        if (mServices.size() == 0) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mInteractionListener = (IServiceFragmentInteraction) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement IServiceFragmentInteraction");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mInteractionListener = null;
    }

    private IServiceFragmentInteraction mInteractionListener;
}
