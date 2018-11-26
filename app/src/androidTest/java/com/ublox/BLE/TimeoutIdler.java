package com.ublox.BLE;

import android.os.Handler;
import android.support.test.espresso.IdlingResource;


public class TimeoutIdler implements IdlingResource {
    public static final long POLLING_DELAY = 500;

    private ResourceCallback callback;
    private long start, timeout;

    protected TimeoutIdler(long timeout) {
        this.timeout = timeout;
        start = System.currentTimeMillis();
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean isIdleNow() {
        if (ready() || System.currentTimeMillis() - start > timeout) {
            if (callback != null) {
                callback.onTransitionToIdle(); //todo should preferably be called outside of here
            }
            return true;
        } else {
            //queue another call in 500 ms...instead of espressos default 5000.
            new Handler().postDelayed(()->isIdleNow(), POLLING_DELAY);
            return false;
        }
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        this.callback = callback;
    }

    protected boolean ready() {
        return false;
    }
}
