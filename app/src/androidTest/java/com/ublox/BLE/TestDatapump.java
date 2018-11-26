package com.ublox.BLE;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.ublox.BLE.activities.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.ublox.BLE.Wait.waitFor;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class TestDatapump {

    @Rule
    public ActivityTestRule<MainActivity> activityTestRule = new MainWithBluetoothTestRule();

    @Before
    public void setup() {
        waitFor(500);
        onView(withText("TEST")).perform(click());
    }

    @Test
    public void sendsOnly1ExtraCredit_whenReceivingBetween16And32Packets() {
        onView(withId(R.id.swCredits)).perform(click());

        MainActivity mainActivity = activityTestRule.getActivity();
        MockGatt gatt = (MockGatt) mainActivity.getLeService().getGatt();

        gatt.fifo.setValue(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19});
        for (int i = 0; i < 25; i++) {
            gatt.notifyCharacteristic(gatt.fifo);
        }
        waitFor(500);
        // TODO: Handle async testing better than this (glorified Thread.sleep)

        int creditsSent = gatt.getWritten().size();

        // Expect 2, 1 for initial SPS setup and 1 when 16 packets have been received.
        assertThat(creditsSent, equalTo(2));
    }
}
