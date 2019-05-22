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
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.ublox.BLE.EspressoExtensions.onToast;
import static com.ublox.BLE.Wait.waitFor;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
public class TestChatFragment {

    @Rule
    public ActivityTestRule<MainActivity> act = new MainWithBluetoothTestRule();

    @Before
    public void setup() {
        waitFor(500);
        onView(withText("CHAT")).perform(click());
    }

    @Test
    public void sendMessage_connected() {
        typeMessage("Hello World!");
        onView(allOf(withId(R.id.tvMessage), withText("Hello World!"))).check(matches(isDisplayed()));
    }

    @Test
    public void sendMessage_notConnected() {
        onView(withId(R.id.menu_disconnect)).perform(click());
        typeMessage("Hello World!");
        onToast(act.getActivity(), "You need to be connected to a device in order to do this");
    }

    @Test
    public void sendMessage_withCr() {
        onView(withId(R.id.swSendWithCR)).perform(click());
        typeMessage("Hello World!");
        onView(allOf(withId(R.id.tvMessage), withText("Hello World!\r"))).check(matches(isDisplayed()));
    }

    private static void typeMessage(String message) {
        onView(withId(R.id.etMessage)).perform(typeText(message), closeSoftKeyboard());
        onView(allOf(withId(R.id.bSend), hasSibling(withId(R.id.etMessage)))).perform(click());
    }
}
