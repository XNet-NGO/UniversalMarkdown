package com.fluid.afm.func;

public interface Callback<T> {

    /**
     * Success
     *
     * @param t result
     */
    void onSuccess(T t);

    /**
     * Failure
     */
    void onFail();
}
