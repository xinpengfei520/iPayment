package com.xpf.pay.library.pay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.alipay.sdk.app.AuthTask;
import com.alipay.sdk.app.PayTask;
import com.xpf.pay.library.base.AbstractPayment;
import com.xpf.pay.library.bean.PayResult;
import com.xpf.pay.library.utils.OrderInfoUtil2_0;

import java.util.Map;

/**
 * Created by x-sir on 2019/1/26 :)
 * Function:支付宝支付 V2
 */
public class AliPayV2 extends AbstractPayment {

    /**
     * 用于支付宝支付业务的入参 app_id。
     */
    public static final String APPID = "";
    /**
     * 用于支付宝账户登录授权业务的入参 pid。
     */
    public static final String PID = "";
    /**
     * 用于支付宝账户登录授权业务的入参 target_id。
     */
    public static final String TARGET_ID = "";

    /**
     * pkcs8 格式的商户私钥。
     * 如下私钥，RSA2_PRIVATE 或者 RSA_PRIVATE 只需要填入一个，如果两个都设置了，本 Demo 将优先
     * 使用 RSA2_PRIVATE。RSA2_PRIVATE 可以保证商户交易在更加安全的环境下进行，建议商户使用
     * RSA2_PRIVATE。
     * 建议使用支付宝提供的公私钥生成工具生成和获取 RSA2_PRIVATE。
     * 工具地址：https://doc.open.alipay.com/docs/doc.htm?treeId=291&articleId=106097&docType=1
     */
    public static final String RSA2_PRIVATE = "";
    public static final String RSA_PRIVATE = "";

    /**
     * ali pay sdk flag
     */
    private static final int SDK_PAY_FLAG = 1;
    private static final int SDK_AUTH_FLAG = 2;

    private Activity mActivity;

    // 未签名的订单信息
    private String rawAliPayOrderInfo;
    // 服务器签名成功的订单信息
    private String signedAliPayOrderInfo;

    // 支付宝支付监听
    private OnAliPayListener mOnAliPayListener;

    private Handler mHandler;

    @SuppressLint("HandlerLeak")
    public AliPayV2() {
        super();

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case SDK_PAY_FLAG: {
                        @SuppressWarnings("unchecked")
                        PayResult payResult = new PayResult((Map<String, String>) msg.obj);

                        // 支付宝返回此次支付结果及加签，建议对支付宝签名信息拿签约时支付宝提供的公钥做验签
                        String resultInfo = payResult.getResult();

                        String resultStatus = payResult.getResultStatus();

                        // 判断resultStatus 为“9000”则代表支付成功，具体状态码代表含义可参考接口文档
                        if (TextUtils.equals(resultStatus, "9000")) {
                            Toast.makeText(mActivity, "支付成功", Toast.LENGTH_SHORT).show();
                            if (mOnAliPayListener != null)
                                mOnAliPayListener.onPaySuccess(resultInfo);
                        } else {
                            // 判断resultStatus 为非“9000”则代表可能支付失败
                            // “8000”代表支付结果因为支付渠道原因或者系统原因还在等待支付结果确认，最终交易是否成功以服务端异步通知为准（小概率状态）
                            if (TextUtils.equals(resultStatus, "8000")) {
                                Toast.makeText(mActivity, "支付结果确认中", Toast.LENGTH_SHORT).show();
                                if (mOnAliPayListener != null)
                                    mOnAliPayListener.onPayConfirming(resultInfo);

                            } else {
                                // 其他值就可以判断为支付失败，包括用户主动取消支付，或者系统返回的错误
                                Toast.makeText(mActivity, "支付失败", Toast.LENGTH_SHORT).show();
                                if (mOnAliPayListener != null)
                                    mOnAliPayListener.onPayFailure(resultInfo);
                            }
                        }
                        break;
                    }
                    case SDK_AUTH_FLAG: {
                        Toast.makeText(mActivity, "检查结果为：" + msg.obj, Toast.LENGTH_SHORT).show();
                        if (mOnAliPayListener != null)
                            mOnAliPayListener.onPayCheck(msg.obj.toString());
                        break;
                    }
                    default:
                        break;
                }
            }
        };
    }


    /**
     * 发送支付宝支付请求
     */
    @Override
    public void pay() {
        if (TextUtils.isEmpty(APPID) || (TextUtils.isEmpty(RSA2_PRIVATE) && TextUtils.isEmpty(RSA_PRIVATE))) {
            showAlert(mActivity, "错误: 需要配置 PayDemoActivity 中的 APPID 和 RSA_PRIVATE");
            return;
        }

        /*
         * 这里只是为了方便直接向商户展示支付宝的整个支付流程；所以Demo中加签过程直接放在客户端完成；
         * 真实App里，privateKey等数据严禁放在客户端，加签过程务必要放在服务端完成；
         * 防止商户私密数据泄露，造成不必要的资金损失，及面临各种安全风险；
         *
         * orderInfo 的获取必须来自服务端；
         */
        boolean rsa2 = (RSA2_PRIVATE.length() > 0);
        Map<String, String> params = OrderInfoUtil2_0.buildOrderParamMap(APPID, rsa2);
        String orderParam = OrderInfoUtil2_0.buildOrderParam(params);

        String privateKey = rsa2 ? RSA2_PRIVATE : RSA_PRIVATE;
        String sign = OrderInfoUtil2_0.getSign(params, privateKey, rsa2);
        final String orderInfo = orderParam + "&" + sign;

        final Runnable payRunnable = new Runnable() {

            @Override
            public void run() {
                PayTask alipay = new PayTask(mActivity);
                Map<String, String> result = alipay.payV2(orderInfo, true);
                Log.i("msp", result.toString());

                Message msg = new Message();
                msg.what = SDK_PAY_FLAG;
                msg.obj = result;
                mHandler.sendMessage(msg);
            }
        };

        // 必须异步调用
        Thread payThread = new Thread(payRunnable);
        payThread.start();
    }

    /**
     * 查询终端设备是否存在支付宝认证账户
     */
    @Override
    public void auth() {
        if (TextUtils.isEmpty(PID) || TextUtils.isEmpty(APPID)
                || (TextUtils.isEmpty(RSA2_PRIVATE) && TextUtils.isEmpty(RSA_PRIVATE))
                || TextUtils.isEmpty(TARGET_ID)) {
            showAlert(mActivity, "错误: 需要配置 PayDemoActivity 中的 APPID 和 RSA_PRIVATE");
            return;
        }

        /*
         * 这里只是为了方便直接向商户展示支付宝的整个支付流程；所以Demo中加签过程直接放在客户端完成；
         * 真实App里，privateKey等数据严禁放在客户端，加签过程务必要放在服务端完成；
         * 防止商户私密数据泄露，造成不必要的资金损失，及面临各种安全风险；
         *
         * authInfo 的获取必须来自服务端；
         */
        boolean rsa2 = (RSA2_PRIVATE.length() > 0);
        Map<String, String> authInfoMap = OrderInfoUtil2_0.buildAuthInfoMap(PID, APPID, TARGET_ID, rsa2);
        String info = OrderInfoUtil2_0.buildOrderParam(authInfoMap);

        String privateKey = rsa2 ? RSA2_PRIVATE : RSA_PRIVATE;
        String sign = OrderInfoUtil2_0.getSign(authInfoMap, privateKey, rsa2);
        final String authInfo = info + "&" + sign;
        Runnable authRunnable = new Runnable() {

            @Override
            public void run() {
                // 构造AuthTask 对象
                AuthTask authTask = new AuthTask(mActivity);
                // 调用授权接口，获取授权结果
                Map<String, String> result = authTask.authV2(authInfo, true);

                Message msg = new Message();
                msg.what = SDK_AUTH_FLAG;
                msg.obj = result;
                mHandler.sendMessage(msg);
            }
        };

        // 必须异步调用
        Thread authThread = new Thread(authRunnable);
        authThread.start();
    }


    /**
     * 创建订单信息
     *
     * @param partner     签约合作者身份ID
     * @param seller      签约卖家支付宝账号
     * @param outTradeNo  商户网站唯一订单号
     * @param subject     商品名称
     * @param body        商品详情
     * @param price       商品金额
     * @param callbackUrl 服务器异步通知页面路径
     * @return
     */
    private static String getOrderInfo(String partner, String seller, String outTradeNo, String subject, String body, String price, String callbackUrl) {
        // 签约合作者身份ID
        String orderInfo = "partner=" + "\"" + partner + "\"";
        // 签约卖家支付宝账号
        orderInfo += "&seller_id=" + "\"" + seller + "\"";
        // 商户网站唯一订单号
        orderInfo += "&out_trade_no=" + "\"" + outTradeNo + "\"";
        // 商品名称
        orderInfo += "&subject=" + "\"" + subject + "\"";
        // 商品详情
        orderInfo += "&body=" + "\"" + body + "\"";
        // 商品金额
        orderInfo += "&total_fee=" + "\"" + price + "\"";
        // 服务器异步通知页面路径
//		orderInfo += "&notify_url=" + "\"" + "http://notify.msp.hk/notify.htm"
//				+ "\"";
        orderInfo += "&notify_url=" + "\"" + callbackUrl
                + "\"";
        // 服务接口名称， 固定值
        orderInfo += "&service=\"mobile.securitypay.pay\"";
        // 支付类型， 固定值
        orderInfo += "&payment_type=\"1\"";
        // 参数编码， 固定值
        orderInfo += "&_input_charset=\"utf-8\"";
        // 设置未付款交易的超时时间
        // 默认30分钟，一旦超时，该笔交易就会自动被关闭。
        // 取值范围：1m～15d。
        // m-分钟，h-小时，d-天，1c-当天（无论交易何时创建，都在0点关闭）。
        // 该参数数值不接受小数点，如1.5h，可转换为90m。
        orderInfo += "&it_b_pay=\"30m\"";
        // extern_token为经过快登授权获取到的alipay_open_id,带上此参数用户将使用授权的账户进行支付
        // orderInfo += "&extern_token=" + "\"" + extern_token + "\"";
        // 支付宝处理完请求后，当前页面跳转到商户指定页面的路径，可空
        orderInfo += "&return_url=\"m.alipay.com\"";
        // 调用银行卡支付，需配置此参数，参与签名， 固定值 （需要签约《无线银行卡快捷支付》才能使用）
        // orderInfo += "&paymethod=\"expressGateway\"";

        return orderInfo;
    }

    /**
     * get the sign type we use. 获取签名方式
     */
    public String getSignType() {
        return "sign_type=\"RSA\"";
    }


    public static class Builder {
        /**
         * 上下文
         */
        private Activity activity;
        /**
         * 未签名的订单信息
         */
        private String rawAliPayOrderInfo;
        /**
         * 服务器签名成功的订单信息
         */
        private String signedAliPayOrderInfo;

        private OnAliPayListener onAliPayListener;

        public Builder(Activity activity) {
            this.activity = activity;
        }

        /**
         * 设置未签名的订单信息
         *
         * @param rawAliPayOrderInfo
         * @return
         */
        public Builder setRawAliPayOrderInfo(String rawAliPayOrderInfo) {
            this.rawAliPayOrderInfo = rawAliPayOrderInfo;
            return this;
        }

        /**
         * 设置服务器签名成功的订单信息
         *
         * @param signedAliPayOrderInfo
         * @return
         */
        public Builder setSignedAliPayOrderInfo(String signedAliPayOrderInfo) {
            this.signedAliPayOrderInfo = signedAliPayOrderInfo;
            return this;
        }

        public Builder setOnAliPayListener(OnAliPayListener onAliPayListener) {
            this.onAliPayListener = onAliPayListener;
            return this;
        }

        public AliPayV2 build() {
            AliPayV2 aliPayReq = new AliPayV2();
            aliPayReq.mActivity = this.activity;
            aliPayReq.rawAliPayOrderInfo = this.rawAliPayOrderInfo;
            aliPayReq.signedAliPayOrderInfo = this.signedAliPayOrderInfo;
            aliPayReq.mOnAliPayListener = this.onAliPayListener;

            return aliPayReq;
        }
    }

    /**
     * 支付宝支付订单信息的信息类
     * 官方demo是暴露了商户私钥，pkcs8格式的，这是极其不安全的。官方也建议私钥签名订单这一块放到服务器去处理。
     * 所以为了避免商户私钥暴露在客户端，订单的加密过程放到服务器去处理
     */
    public static class AliOrderInfo {
        private String partner;
        private String seller;
        private String outTradeNo;
        private String subject;
        private String body;
        private String price;
        private String callbackUrl;

        /**
         * 设置商户
         *
         * @param partner
         * @return
         */
        public AliOrderInfo setPartner(String partner) {
            this.partner = partner;
            return this;
        }

        /**
         * 设置商户账号
         *
         * @param seller
         * @return
         */
        public AliOrderInfo setSeller(String seller) {
            this.seller = seller;
            return this;
        }

        /**
         * 设置唯一订单号
         *
         * @param outTradeNo
         * @return
         */
        public AliOrderInfo setOutTradeNo(String outTradeNo) {
            this.outTradeNo = outTradeNo;
            return this;
        }

        /**
         * 设置订单标题
         *
         * @param subject
         * @return
         */
        public AliOrderInfo setSubject(String subject) {
            this.subject = subject;
            return this;
        }

        /**
         * 设置订单详情
         *
         * @param body
         * @return
         */
        public AliOrderInfo setBody(String body) {
            this.body = body;
            return this;
        }

        /**
         * 设置价格
         *
         * @param price
         * @return
         */
        public AliOrderInfo setPrice(String price) {
            this.price = price;
            return this;
        }

        /**
         * 设置请求回调
         *
         * @param callbackUrl
         * @return
         */
        public AliOrderInfo setCallbackUrl(String callbackUrl) {
            this.callbackUrl = callbackUrl;
            return this;
        }

        /**
         * 创建订单详情
         *
         * @return
         */
        public String createOrderInfo() {
            return getOrderInfo(partner, seller, outTradeNo, subject, body, price, callbackUrl);
        }
    }

    private static void showAlert(Context ctx, String info) {
        showAlert(ctx, info, null);
    }

    private static void showAlert(Context ctx, String info, DialogInterface.OnDismissListener onDismiss) {
        new AlertDialog.Builder(ctx)
                .setMessage(info)
                .setPositiveButton("确定", null)
                .setOnDismissListener(onDismiss)
                .show();
    }

    /**
     * 支付宝支付监听
     */
    public interface OnAliPayListener {
        void onPaySuccess(String resultInfo);

        void onPayFailure(String resultInfo);

        void onPayConfirming(String resultInfo);

        void onPayCheck(String status);
    }
}
