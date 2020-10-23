package com.ublox.BLE;

import android.app.Activity;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.BoundedMatcher;

import com.ublox.BLE.bluetooth.BluetoothPeripheral;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.Map;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

public class EspressoExtensions {

    public static TypeSafeMatcher<BluetoothPeripheral> withDevice(final String address) {
        return new TypeSafeMatcher<BluetoothPeripheral>() {

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("With bluetooth device: %s", address));
            }

            @Override
            protected boolean matchesSafely(BluetoothPeripheral peripheral) {
                return address.equals(peripheral.identifier());
            }
        };
    }

    public static BoundedMatcher<Object, Map> withMapping(String key, String value) {
        return new BoundedMatcher<Object, Map>(Map.class) {

            @Override
            public boolean matchesSafely(Map map) {
                return map.containsKey(key) && map.get(key).toString().equals(value);
            }

            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText(String.format("With Map Entry: %s=%s", key, value));
            }
        };
    }

    public static ViewInteractionGroup onEveryView(ViewInteraction... interactions) {
        return new ViewInteractionGroup(interactions);
    }

    public static ViewInteraction onToast(Activity activity, String toast) {
        return onView(withText(toast))
            .inRoot(withDecorView(not(activity.getWindow().getDecorView())));
    }

    public static class ViewInteractionGroup {
        private ViewInteraction[] interactions;

        public ViewInteractionGroup (ViewInteraction... interactions) {
            this.interactions = interactions;
        }

        public void check(ViewAssertion assertion) {
            for (ViewInteraction interaction: interactions) {
                interaction.check(assertion);
            }
        }

    }
}
