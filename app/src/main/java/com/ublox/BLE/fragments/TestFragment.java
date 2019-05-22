package com.ublox.BLE.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothGatt;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ublox.BLE.R;
import com.ublox.BLE.activities.MainActivity;
import com.ublox.BLE.interfaces.ITestInteractionListener;
import com.ublox.BLE.utils.PhyMode;

import java.util.Locale;


public class TestFragment extends Fragment {

    private Switch swCredits;
    private Button bMtu;
    private Button bConnPrio;
    private Switch swTxTest;
    private EditText etPacketSize;
    private CheckBox cbBitError;
    private Button bRxReset;
    private RadioGroup rgMode;
    private TextView tvRxCounter;
    private TextView tvRxAvg;
    private TextView tvMtuSize;
    private TextView tvTxCounter;
    @RequiresApi(26)
    private LinearLayout llPhyMode;
    @RequiresApi(26)
    private RadioGroup rgPhyMode;


    private ITestInteractionListener mInteractionListener;
    private TextView tvTxAvg;

    public static TestFragment newInstance() {
        TestFragment fragment = new TestFragment();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_test, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swCredits = (Switch) view.findViewById(R.id.swCredits);
        swCredits.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!swTxTest.isChecked()) {
                    if (((MainActivity) getActivity()).isConnected()) {
                        if (isChecked) {
                            //Disconnect SPS...
                            mInteractionListener.onSwitchCredits(true);
                        } else {
                            //Disconnect SPS (with credits)...
                            mInteractionListener.onSwitchCredits(false);
                        }
                    } else {
                        swCredits.setChecked(false);
                    }
                } else {
                    swCredits.setChecked(!isChecked);
                }
            }
        });

        bMtu = (Button) view.findViewById(R.id.bMtu);
        bMtu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText etMtuSize = (EditText) view.findViewById(R.id.etMtuSize);
                try {
                    int mtuSize = Integer.parseInt(etMtuSize.getText().toString());
                    etMtuSize.setText("");
                    mInteractionListener.onMtuSizeChanged(mtuSize);
                } catch (NumberFormatException e) {
                    Toast.makeText(getActivity(), "You must enter a number", Toast.LENGTH_LONG).show();
                }
            }
        });

        Spinner spConnPrio = (Spinner) view.findViewById(R.id.spConnPrio);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(view.getContext(),R.array.conn_prio, android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spConnPrio.setAdapter(adapter);

        bConnPrio = (Button) view.findViewById(R.id.bConnPrio);
        bConnPrio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Spinner spConnPrio = (Spinner) view.findViewById(R.id.spConnPrio);
                    String selectedItem = spConnPrio.getSelectedItem().toString();
                    switch (selectedItem) {
                        case "BALANCED":
                            mInteractionListener.updateConnectionPrio(BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
                            break;
                        case "HIGH":
                            mInteractionListener.updateConnectionPrio(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                            break;
                        case "LOW POWER":
                            mInteractionListener.updateConnectionPrio(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER);
                            break;
                    }
                }
            }
        });

        swTxTest = (Switch) view.findViewById(R.id.swTxTest);
        swTxTest.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mInteractionListener.onModeSet(isInContinuousMode());
                if (isChecked) {
                    tvTxCounter.setText((String.format(Locale.US, "%.0f", 0.0) + " B"));
                    mInteractionListener.onSwitchTest(true);
                } else {
                    mInteractionListener.onSwitchTest(false);
                }
            }
        });

        etPacketSize = (EditText) view.findViewById(R.id.etPacketSize);
        etPacketSize.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    mInteractionListener.onPacketSizeChanged(Integer.parseInt(s.toString()));
                    if (!swTxTest.isClickable()) {
                        swTxTest.setClickable(true);
                    }
                } catch (NumberFormatException exception) {
                    if (swTxTest.isClickable()) {
                        swTxTest.setClickable(false);
                    }
                    Toast.makeText(getActivity(), "The packet length number too big or not valid!",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        cbBitError = (CheckBox) view.findViewById(R.id.cbBitError);
        cbBitError.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mInteractionListener.onBitErrorChanged(isChecked);
            }
        });

        bRxReset = (Button) view.findViewById(R.id.bRxReset);
        bRxReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInteractionListener.onReset();
                tvRxCounter = (TextView) view.findViewById(R.id.tvRxCounter);
                tvRxCounter.setText("0 B");
                tvRxAvg.setText((String.format(Locale.US, "%.2f", 0.0) + " kbps"));
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            llPhyMode = (LinearLayout) view.findViewById(R.id.llPhyMode);
            rgPhyMode = (RadioGroup) view.findViewById(R.id.rgPhyMode);
            rgPhyMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @RequiresApi(26)
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    switch (checkedId) {
                        case R.id.rb1Mpbs:
                            mInteractionListener.onPhyModeChange(PhyMode.PHY_1M);
                            return;
                        case R.id.rb2Mpbs:
                            mInteractionListener.onPhyModeChange(PhyMode.PHY_2M);
                            return;
                        default:
                            mInteractionListener.onPhyModeChange(PhyMode.PHY_UNDEFINED);
                            return;
                    }
                }
            });
        }

        rgMode = (RadioGroup) view.findViewById(R.id.rgMode);
        tvTxCounter = (TextView) view.findViewById(R.id.tvTxCounter);
        tvTxAvg = (TextView) view.findViewById(R.id.tvTxAvg);
        tvRxAvg = (TextView) view.findViewById(R.id.tvRxAvg);
        tvMtuSize = (TextView) view.findViewById(R.id.tvMtuSize);
        tvRxCounter = (TextView) view.findViewById(R.id.tvRxCounter);

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mInteractionListener = (ITestInteractionListener) activity;
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

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (getView() != null && !isVisibleToUser) {
            //Stop an ongoing TX test
            setTxTestToOff(getView());
            //Disconnect SPS
            setSwCreditsToOff(getView());
        }
    }

    public boolean isInContinuousMode() {
        return rgMode.getCheckedRadioButtonId() == R.id.continuous;
    }


    public void updateTxCounter(String value, String average) {
        tvTxCounter.setText(value);
        tvTxAvg.setText(average);
    }

    public void updateRxCounter(String rxValue, String average) {
        tvRxCounter.setText(rxValue);
        tvRxAvg.setText(average);
    }

    public void setTxTestToOff(View v) {
        Switch swTxTest = (Switch) v.findViewById(R.id.swTxTest);
        swTxTest.setChecked(false);
    }

    public void setSwCreditsToOff(View v) {
        Switch swCredits = (Switch) v.findViewById(R.id.swCredits);
        swCredits.setChecked(false);
    }

    public void updateMTUSize(int mtu) {
        tvMtuSize.setText(String.format(Locale.US, "%d", mtu));
        int packetSize = mtu - 3;
        etPacketSize.setText(String.format(Locale.US, "%d", packetSize));
    }

    @RequiresApi(26)
    public void setIsLE2MPhySupported(PhyMode txPhyMode) {
        if (txPhyMode == PhyMode.PHY_2M) {
            llPhyMode.setVisibility(View.VISIBLE);
        } else {
            llPhyMode.setVisibility(View.GONE);
        }
    }
}