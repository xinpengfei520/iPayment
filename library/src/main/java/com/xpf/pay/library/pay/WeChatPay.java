package com.xpf.pay.library.pay;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.xpf.pay.library.base.AbstractPayment;

/**
 * Created by x-sir on 2019/1/26 :)
 * Function:微信支付
 */
public class WeChatPay extends AbstractPayment {

    private static final String TAG = "WeChatPay";

    private Activity mActivity;

    /**
     * 微信支付 AppID
     */
    private String appId;
    /**
     * 微信支付商户号
     */
    private String partnerId;
    /**
     * 预支付码（重要）
     */
    private String prepayId;
    /**
     * "Sign=WXPay"
     */
    private String packageValue;
    private String nonceStr;
    /**
     * 时间戳
     */
    private String timeStamp;
    /**
     * 签名
     */
    private String sign;
    /**
     * 微信支付核心api
     */
    private IWXAPI mWXApi;
    /**
     * 微信支付监听
     */
    private OnWeChatPayListener mOnWeChatPayListener;

    private IWXAPIEventHandler iwxapiEventHandler = new IWXAPIEventHandler() {
        @Override
        public void onReq(BaseReq baseReq) {
            Toast.makeText(WeChatPay.this.mActivity, "onReq===>>>get baseReq.getType : " + baseReq.getType(), Toast.LENGTH_LONG).show();
            Log.d(TAG, "onReq===>>>get baseReq.getType : " + baseReq.getType());
        }

        @Override
        public void onResp(BaseResp baseResp) {
            Toast.makeText(WeChatPay.this.mActivity, "onResp===>>>get resp.getType : " + baseResp.getType(), Toast.LENGTH_LONG).show();

            /**
             *  0	成功	展示成功页面
             * -1	错误	可能的原因：签名错误、未注册APPID、项目设置APPID不正确、注册的APPID与设置的不匹配、其他异常等。
             * -2	用户取消	无需处理。发生场景：用户不支付了，点击取消，返回APP。
             */

            if (baseResp.getType() == ConstantsAPI.COMMAND_PAY_BY_WX) {
                Log.d(TAG, "onPayFinish,errCode=" + baseResp.errCode);
                if (mOnWeChatPayListener != null) {
                    if (baseResp.errCode == BaseResp.ErrCode.ERR_OK) { //        0 成功	展示成功页面
                        mOnWeChatPayListener.onPaySuccess(baseResp.errCode);
                    } else {//  -1	错误       -2	用户取消
                        mOnWeChatPayListener.onPayFailure(baseResp.errCode);
                    }
                }
            }
        }
    };

    public WeChatPay() {

    }

    /**
     * 发送微信支付请求
     */
    @Override
    public void pay() {
        mWXApi = WXAPIFactory.createWXAPI(mActivity, null);
        mWXApi.handleIntent(mActivity.getIntent(), iwxapiEventHandler);

        mWXApi.registerApp(this.appId);

        PayReq request = new PayReq();

        request.appId = this.appId;
        request.partnerId = this.partnerId;
        request.prepayId = this.prepayId;
        request.packageValue = this.packageValue != null ? this.packageValue : "Sign=WXPay";
        request.nonceStr = this.nonceStr;
        request.timeStamp = this.timeStamp;
        request.sign = this.sign;

        mWXApi.sendReq(request);
    }

    public static class Builder {
        /**
         * 上下文
         */
        private Activity activity;
        /**
         * 微信支付 AppID
         */
        private String appId;
        /**
         * 微信支付商户号
         */
        private String partnerId;
        /**
         * 预支付码（重要）
         */
        private String prepayId;
        /**
         * "Sign=WXPay"
         */
        private String packageValue = "Sign=WXPay";
        /**
         * nonceStr
         */
        private String nonceStr;
        /**
         * 时间戳
         */
        private String timeStamp;
        /**
         * 签名
         */
        private String sign;
        /**
         * 支付结果的监听器
         */
        private OnWeChatPayListener onWeChatPayListener;

        public Builder(Activity activity) {
            this.activity = activity;
        }

        /**
         * 设置微信支付AppID
         */
        public Builder setAppId(String appId) {
            this.appId = appId;
            return this;
        }

        /**
         * 微信支付商户号
         */
        public Builder setPartnerId(String partnerId) {
            this.partnerId = partnerId;
            return this;
        }

        /**
         * 设置预支付码（重要）
         */
        public Builder setPrepayId(String prepayId) {
            this.prepayId = prepayId;
            return this;
        }

        /**
         * 设置 packageValue
         */
        public Builder setPackageValue(String packageValue) {
            this.packageValue = packageValue;
            return this;
        }

        /**
         * 设置 nonceStr
         */
        public Builder setNonceStr(String nonceStr) {
            this.nonceStr = nonceStr;
            return this;
        }

        /**
         * 设置时间戳
         */
        public Builder setTimeStamp(String timeStamp) {
            this.timeStamp = timeStamp;
            return this;
        }

        /**
         * 设置签名
         */
        public Builder setSign(String sign) {
            this.sign = sign;
            return this;
        }

        /**
         * 设置支付的监听器
         */
        public Builder setOnPaymentListener(OnWeChatPayListener onWeChatPayListener) {
            this.onWeChatPayListener = onWeChatPayListener;
            return this;
        }

        public WeChatPay build() {
            WeChatPay weChatPay = new WeChatPay();
            weChatPay.mActivity = this.activity;
            // 微信支付AppID
            weChatPay.appId = this.appId;
            // 微信支付商户号
            weChatPay.partnerId = this.partnerId;
            // 预支付码（重要）
            weChatPay.prepayId = this.prepayId;
            // "Sign=WXPay"
            weChatPay.packageValue = this.packageValue;
            weChatPay.nonceStr = this.nonceStr;
            // 时间戳
            weChatPay.timeStamp = this.timeStamp;
            // 签名
            weChatPay.sign = this.sign;
            weChatPay.mOnWeChatPayListener = this.onWeChatPayListener;

            return weChatPay;
        }
    }

    /**
     * 微信支付监听器
     */
    public interface OnWeChatPayListener {
        void onPaySuccess(int errorCode);

        void onPayFailure(int errorCode);
    }
}
