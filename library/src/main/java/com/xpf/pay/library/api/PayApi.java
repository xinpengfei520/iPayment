package com.xpf.pay.library.api;

import com.xpf.pay.library.base.AbstractPayment;

/**
 * Created by x-sir on 2019/1/26 :)
 * Function:Common payment singleton class.
 */
public class PayApi {

    private PayApi() {
    }

    public static PayApi getInstance() {
        return PaymentHolder.instance;
    }

    private static class PaymentHolder {
        private static PayApi instance = new PayApi();
    }

    public void toPay(AbstractPayment abstractPayment) {
        abstractPayment.pay();
    }

    public void toAuth(AbstractPayment abstractPayment) {
        abstractPayment.auth();
    }
}

