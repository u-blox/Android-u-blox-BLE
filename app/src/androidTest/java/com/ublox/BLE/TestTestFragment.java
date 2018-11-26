package com.ublox.BLE;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.ublox.BLE.activities.MainActivity;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isNotChecked;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.ublox.BLE.ConstantsForTests.MODE_PACKET;
import static com.ublox.BLE.ConstantsForTests.MTU_MAX;
import static com.ublox.BLE.ConstantsForTests.MTU_MIN;
import static com.ublox.BLE.ConstantsForTests.MTU_TOO_LARGE;
import static com.ublox.BLE.ConstantsForTests.MTU_TOO_SMALL;
import static com.ublox.BLE.ConstantsForTests.RX_CLEAR;
import static com.ublox.BLE.ConstantsForTests.TAB_TEST;
import static com.ublox.BLE.ConstantsForTests.TOAST_MTU_EMPTY;
import static com.ublox.BLE.ConstantsForTests.TOAST_PACKET_EMPTY;
import static com.ublox.BLE.ConstantsForTests.TWENTY_BYTES;
import static com.ublox.BLE.ConstantsForTests.ZERO_BYTES;
import static com.ublox.BLE.EspressoExtensions.onToast;
import static com.ublox.BLE.Wait.waitFor;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
public class TestTestFragment {
    DeviceConnectionIdler connected;

    @Rule
    public ActivityTestRule<MainActivity> act = new MainWithBluetoothTestRule();

    @Before
    public void setup() {
        waitFor(500);
        onView(withText(TAB_TEST)).perform(click());
    }

    @Test
    public void requestMTU_empty() {
        onView(withId(R.id.bMtu)).perform(click(), click());
        onToast(act.getActivity(), TOAST_MTU_EMPTY).check(matches(isDisplayed()));
    }

    @Test
    public void requestMTU_tooSmall() {
        enterMTU(MTU_TOO_SMALL);
        onView(withId(R.id.tvMtuSize)).check(matches(not(withText(MTU_TOO_SMALL))));
    }

    @Test
    public void requestMTU_min() {
        enterMTU(MTU_MIN);
        onView(withId(R.id.tvMtuSize)).check(matches(withText(MTU_MIN)));
    }

    @Test
    public void requestMTU_max() {
        enterMTU(MTU_MAX);
        onView(withId(R.id.tvMtuSize)).check(matches(withText(MTU_MAX)));
    }

    @Test
    public void requestMTU_tooLarge() {
        enterMTU(MTU_TOO_LARGE);
        onView(withId(R.id.tvMtuSize)).check(matches(withText(MTU_MAX)));
    }

    @Test
    public void toggleCredits() {
        onView(withId(R.id.swCredits)).perform(click());
        onView(withId(R.id.swCredits)).check(matches(isChecked()));
    }

    @Ignore // TODO: Enable when we can figure out why clicking txTest doesn't return.
    @Test
    public void toggleCredits_ongoing() {
        onView(withId(R.id.swTxTest)).perform(click());
        onView(withId(R.id.swCredits)).perform(click());
        onView(withId(R.id.swCredits)).check(matches(isNotChecked()));
    }

    @Test
    public void sendOnePacket() {
        onView(withText(MODE_PACKET)).perform(click());
        onView(withId(R.id.swTxTest)).perform(click());
        onView(withId(R.id.tvTxCounter)).check(matches(withText(TWENTY_BYTES)));
    }

    @Ignore // TODO: Enable when we can figure out why clicking txTest doesn't return.
    @Test
    public void sendContinuous() {
        onView(withId(R.id.continuous)).perform(click());
        onView(withId(R.id.swTxTest)).perform(click());
        onView(withId(R.id.tvTxCounter)).check(matches(not(withText(ZERO_BYTES))));
    }

    @Test
    public void clearRx() {
        onView(withId(R.id.bRxReset)).perform(click());
        onView(withId(R.id.tvRxAvg)).check(matches(withText(RX_CLEAR)));
    }

    @Test
    public void setPacketEmpty() {
        onView(withId(R.id.etPacketSize)).perform(click(), clearText(), closeSoftKeyboard());
        onToast(act.getActivity(), TOAST_PACKET_EMPTY).check(matches(isDisplayed()));
    }

    @Test
    public void toggleCredits_disconnectedNoChange() {
        onView(withId(R.id.menu_disconnect)).perform(click());
        onView(withId(R.id.swCredits)).perform(click());
        onView(withId(R.id.swCredits)).check(matches(isNotChecked()));
    }

    @Test
    public void requestMTU_disconnectedNoChange() {
        onView(withId(R.id.menu_disconnect)).perform(click());
        enterMTU(MTU_MAX);
        onView(withId(R.id.tvMtuSize)).check(matches(not(withText(MTU_MAX))));
    }

    @Test
    public void startTxTest_disconnectedNoChange() {
        onView(withId(R.id.menu_disconnect)).perform(click());
        onView(withId(R.id.swTxTest)).perform(click());
        onView(withId(R.id.tvTxCounter)).check(matches(withText(ZERO_BYTES)));
    }

    private void enterMTU(String mtu) {
        onView(withId(R.id.etMtuSize)).perform(click(), typeText(mtu), closeSoftKeyboard());
        onView(withId(R.id.bMtu)).perform(click());
    }

}
