package com.ublox.BLE;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.support.test.espresso.PerformException;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;

import com.ublox.BLE.activities.DevicesActivity;
import com.ublox.BLE.bluetooth.BluetoothCentral;
import com.ublox.BLE.bluetooth.BluetoothPeripheral;
import com.ublox.BLE.utils.GattAttributes;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.UUID;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.ublox.BLE.EspressoExtensions.withDevice;
import static com.ublox.BLE.Wait.waitFor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class TestDevicesActivity {
    BluetoothCentral mockScanner;

    @Rule
    public ActivityTestRule<DevicesActivity> act = new ActivityTestRule<>(DevicesActivity.class);

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(Manifest.permission.ACCESS_COARSE_LOCATION);

    @Before
    public void setup() {
        mockScanner = mock(BluetoothCentral.class);

        try {
            replaceScannerViaReflection();
        } catch (Exception e) {
            throw new RuntimeException("Could not mock scanner");
        }
    }

    @Test
    public void openAboutPage() {
        onView(withId(R.id.menu_about)).perform(click());
        onView(withId(R.id.bVisit)).check(matches(isDisplayed()));
    }

    @Test
    public void startCallsScan() {
        onView(withId(R.id.menu_scan)).perform(click());
        verify(mockScanner, atLeastOnce()).scan(any());
    }

    @Test
    public void whenScanStartedDisplayStop() {
        when(mockScanner.getState()).thenReturn(BluetoothCentral.State.SCANNING);
        act.getActivity().centralChangedState(mockScanner);

        onView(withId(R.id.menu_stop)).check(matches(isDisplayed()));
    }

    @Test
    public void stopCallsStopOnScan() {
        when(mockScanner.getState()).thenReturn(BluetoothCentral.State.SCANNING);
        act.getActivity().centralChangedState(mockScanner);
        onView(withId(R.id.menu_stop)).perform(click());
        verify(mockScanner, atLeastOnce()).stop();
    }

    @Test
    public void whenStoppedDisplayScanAgain() {
        when(mockScanner.getState()).thenReturn(
            BluetoothCentral.State.SCANNING,
            BluetoothCentral.State.ON
        );
        act.getActivity().centralChangedState(mockScanner);
        //ToDo: timing-dependant test, figure out a better way.
        waitFor(1000);
        act.getActivity().centralChangedState(mockScanner);

        onView(withId(R.id.menu_scan)).check(matches(isDisplayed()));
    }

    @Test
    public void displayScannedPeripherals() {
        listMockedPeripherals();

        onData(withDevice("00:00:00:00:00:04")).check(matches(isDisplayed()));
        onData(withDevice("00:00:00:00:00:03")).check(matches(isDisplayed()));
        onData(withDevice("00:00:00:00:00:02")).check(matches(isDisplayed()));
        onData(withDevice("00:00:00:00:00:01")).check(matches(isDisplayed()));
    }

    @Test
    public void filterWithSps() {
        listMockedPeripherals();
        onView(withId(R.id.checkBoxSps)).perform(click());

        onData(withDevice("00:00:00:00:00:02")).check(matches(isDisplayed()));
        onData(withDevice("00:00:00:00:00:01")).check(matches(isDisplayed()));
    }

    @Test
    public void filterDevicesAreSavedAnyway() {
        onView(withId(R.id.checkBoxSps)).perform(click());
        listMockedPeripherals();
        onView(withId(R.id.checkBoxSps)).perform(click());

        onData(withDevice("00:00:00:00:00:04")).check(matches(isDisplayed()));
        onData(withDevice("00:00:00:00:00:03")).check(matches(isDisplayed()));
        onData(withDevice("00:00:00:00:00:02")).check(matches(isDisplayed()));
        onData(withDevice("00:00:00:00:00:01")).check(matches(isDisplayed()));
    }

    @Test
    public void filterOnName() {
        listMockedPeripherals();

        onView(withId(R.id.filterText)).perform(typeText("u-blox"), closeSoftKeyboard());


        onData(withDevice("00:00:00:00:00:03")).check(matches(isDisplayed()));
        onData(withDevice("00:00:00:00:00:01")).check(matches(isDisplayed()));
    }

    @Test
    public void filterNameAlsoWorksOnAddress() {
        listMockedPeripherals();

        onView(withId(R.id.filterText)).perform(typeText(":02"), closeSoftKeyboard());

        onData(withDevice("00:00:00:00:00:02")).check(matches(isDisplayed()));
    }

    @Test
    public void filterCombineNameAndSps() {
        listMockedPeripherals();

        onView(withId(R.id.checkBoxSps)).perform(click());
        onView(withId(R.id.filterText)).perform(typeText("u-blox"), closeSoftKeyboard());

        onData(withDevice("00:00:00:00:00:01")).check(matches(isDisplayed()));
    }


    @Test()
    public void connect() {
        listMockedPeripherals();
        try {
            onData(withDevice("00:00:00:00:00:01")).perform(click());

            throw new RuntimeException("Expected ClassCastException not thrown");
        } catch (PerformException e) {
            //ToDo: for the time being connecting will cast, since our mock won't cast we'll get an exception
            //in reality we'd like to verify that an intent was sent
            if (!(e.getCause() instanceof ClassCastException)) throw new RuntimeException("Expected ClassCastException not thrown");
        }

        verify(mockScanner, atLeastOnce()).stop();
    }

    private void replaceScannerViaReflection() throws Exception {
        DevicesActivity actActivity = act.getActivity();

        Class<DevicesActivity> devicesActivityClass = DevicesActivity.class;

        Field scanner = devicesActivityClass.getDeclaredField("scanner");

        scanner.setAccessible(true);
        scanner.set(actActivity, mockScanner);
        scanner.setAccessible(false);
    }

    private void listMockedPeripherals() {
        when(mockScanner.getState()).thenReturn(BluetoothCentral.State.SCANNING);
        onView(withId(R.id.menu_scan)).perform(click());

        BluetoothPeripheral namedWithSps = mockPeripheral("00:00:00:00:00:01", "u-blox Device", true);
        BluetoothPeripheral unnamedWithSps = mockPeripheral("00:00:00:00:00:02", null, true);
        BluetoothPeripheral namedWithoutSps = mockPeripheral("00:00:00:00:00:03", "u-blox Device", false);
        BluetoothPeripheral unnamedWithoutSps = mockPeripheral("00:00:00:00:00:04", null, false);

        DevicesActivity activity = act.getActivity();
        activity.centralFoundPeripheral(mockScanner, namedWithSps);
        activity.centralFoundPeripheral(mockScanner, unnamedWithSps);
        activity.centralFoundPeripheral(mockScanner, namedWithoutSps);
        activity.centralFoundPeripheral(mockScanner, unnamedWithoutSps);
    }

    private BluetoothPeripheral mockPeripheral(String id, String name, boolean sps) {
        BluetoothPeripheral peripheral = mock(BluetoothPeripheral.class);

        when(peripheral.bondState()).thenReturn(BluetoothDevice.BOND_NONE);
        when(peripheral.rssi()).thenReturn(-50);
        when(peripheral.identifier()).thenReturn(id);
        when(peripheral.name()).thenReturn(name);
        when(peripheral.advertisedService(UUID.fromString(GattAttributes.UUID_SERVICE_SERIAL_PORT))).thenReturn(sps);

        return peripheral;
    }
}
