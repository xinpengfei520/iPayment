package com.xpf.pay.library.base;

/**
 * Created by x-sir on 2019/1/27 :)
 * Function:抽象支付接口
 */
public abstract class AbstractPayment {

    /**
     * 必须要实现的方法
     */
    public abstract void pay();

    /**
     * 可选择实现的方法
     */
    public void auth() {

    }
}
