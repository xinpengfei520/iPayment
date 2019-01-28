package com.xpf.pay.library.pay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.alipay.sdk.app.AuthTask;
import com.alipay.sdk.app.PayTask;
import com.xpf.pay.library.base.AbstractPayment;
import com.xpf.pay.library.bean.PayResult;
import com.xpf.pay.library.utils.OrderInfoUtil2_0;
import com.xpf.pay.library.utils.SignUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Created by x-sir on 2019/1/26 :)
 * Function:支付宝支付
 */
public class AliPay extends AbstractPayment {

    /**
     * 用于支付宝支付业务的入参 app_id。
     */
    private static final String APPID = "";
    /**
     * 用于支付宝账户登录授权业务的入参 pid。
     */
    private static final String PID = "";
    /**
     * 用于支付宝账户登录授权业务的入参 target_id。
     */
    private static final String TARGET_ID = "";

    /**
     * pkcs8 格式的商户私钥。
     * 如下私钥，RSA2_PRIVATE 或者 RSA_PRIVATE 只需要填入一个，如果两个都设置了，本 Demo 将优先
     * 使用 RSA2_PRIVATE。RSA2_PRIVATE 可以保证商户交易在更加安全的环境下进行，建议商户使用
     * RSA2_PRIVATE。
     * 建议使用支付宝提供的公私钥生成工具生成和获取 RSA2_PRIVATE。
     * 工具地址：https://doc.open.alipay.com/docs/doc.htm?treeId=291&articleId=106097&docType=1
     */
    private static final String RSA2_PRIVATE = "";
    private static final String RSA_PRIVATE = "";

    /**
     * ali pay sdk flag
     */
    private static final int SDK_PAY_FLAG = 1;
    private static final int SDK_AUTH_FLAG = 2;

    private Activity mActivity;

    // 商户网站唯一订单号
    private String outTradeNo;
    // 商品名称
    private String subject;
    // 商品详情
    private String body;
    // 商品金额
    private String price;
    // 服务器异步通知页面路径
    private String callbackUrl;

    // 商户私钥，pkcs8格式
    private String rsaPrivate;
    // 支付宝公钥
    private String rsaPublic;
    // 商户PID
    // 签约合作者身份ID
    private String partner;
    // 商户收款账号
    // 签约卖家支付宝账号
    private String seller;
    private String appId;

    //支付宝支付监听
    private OnAliPayListener mOnAliPayListener;

    private Handler mHandler;

    @SuppressLint("HandlerLeak")
    public AliPay() {
        super();
        mHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case SDK_PAY_FLAG:
                        @SuppressWarnings("unchecked")
                        PayResult payResult = new PayResult((String) msg.obj);
                        /**
                         * 对于支付结果，请商户依赖服务端的异步通知结果。同步通知结果，仅作为支付结束的通知。
                         * 判断resultStatus 为9000则代表支付成功
                         * 该笔订单是否真实支付成功，需要依赖服务端的异步通知。
                         */

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
                                    mOnAliPayListener.onPayFailed(resultInfo);
                            }
                        }
                        break;
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
        // 创建订单信息
        String orderInfo = getOrderInfo(this.partner,
                this.seller, this.outTradeNo, this.subject, this.body,
                this.price, this.callbackUrl);
        // 对订单做RSA 签名
        String sign = sign(orderInfo);
        try {
            // 仅需对sign 做URL编码
            sign = URLEncoder.encode(sign, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 完整的符合支付宝参数规范的订单信息
        final String payInfo = orderInfo + "&sign=\"" + sign + "\"&"
                + getSignType();

        Runnable payRunnable = new Runnable() {

            @Override
            public void run() {
                // 构造PayTask 对象
                PayTask alipay = new PayTask(mActivity);
                // 调用支付接口，获取支付结果
                String result = alipay.pay(payInfo, true);

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
     * 支付宝账户授权业务示例
     */
    @Override
    public void auth() {
        if (TextUtils.isEmpty(PID) || TextUtils.isEmpty(APPID)
                || (TextUtils.isEmpty(RSA2_PRIVATE) && TextUtils.isEmpty(RSA_PRIVATE))
                || TextUtils.isEmpty(TARGET_ID)) {
            throw new IllegalArgumentException("错误: 需要配置 PayDemoActivity 中的 APPID 和 RSA_PRIVATE");
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
                String auth = authTask.auth(authInfo, true);

                Message msg = Message.obtain();
                msg.what = SDK_AUTH_FLAG;
                msg.obj = auth;
                mHandler.sendMessage(msg);
            }
        };

        // 必须异步调用
        Thread authThread = new Thread(authRunnable);
        authThread.start();
    }

    /**
     * 获取支付宝 SDK 版本号
     */
    public String getSdkVersion() {
        return (mActivity == null) ? "" : new PayTask(mActivity).getVersion();
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
     * @return getOrderInfo
     */
    private String getOrderInfo(String partner, String seller, String outTradeNo, String subject, String body, String price, String callbackUrl) {
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
     * sign the order info. 对订单信息进行签名
     *
     * @param content 待签名订单信息
     */
    private String sign(String content) {
        return SignUtils.sign(content, this.rsaPrivate, false);
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
         * 商户网站唯一订单号
         */
        private String outTradeNo;
        /**
         * 商品名称
         */
        private String subject;
        /**
         * 商品详情
         */
        private String body;
        /**
         * 商品金额
         */
        private String price;
        /**
         * 服务器异步通知页面路径
         */
        private String callbackUrl;
        /**
         * 商户私钥，pkcs8 格式
         */
        private String rsaPrivate;
        /**
         * 支付宝公钥
         */
        private String rsaPublic;
        /**
         * 商户PID 签约合作者身份ID
         */
        private String partner;
        /**
         * 商户收款账号，签约卖家支付宝账号
         */
        private String seller;
        /**
         * appId(支付宝开放平台分配)
         */
        private String appId;

        private OnAliPayListener onAliPayListener;

        public Builder(Activity activity) {
            this.activity = activity;
        }

        public Builder setAppId(String appId) {
            this.appId = appId;
            return this;
        }

        public Builder setRsaPublic(String rsaPublic) {
            this.rsaPublic = rsaPublic;
            return this;
        }

        public Builder setSeller(String seller) {
            this.seller = seller;
            return this;
        }

        public Builder setPartner(String partner) {
            this.partner = partner;
            return this;
        }

        public Builder setRsaPrivate(String rsaPrivate) {
            this.rsaPrivate = rsaPrivate;
            return this;
        }

        /**
         * 设置唯一订单号
         *
         * @param outTradeNo outTradeNo
         * @return setOutTradeNo
         */
        public Builder setOutTradeNo(String outTradeNo) {
            this.outTradeNo = outTradeNo;
            return this;
        }

        /**
         * 设置订单标题
         *
         * @param subject subject
         * @return this
         */
        public Builder setSubject(String subject) {
            this.subject = subject;
            return this;
        }

        /**
         * 设置订单内容
         *
         * @param body body
         * @return this
         */
        public Builder setBody(String body) {
            this.body = body;
            return this;
        }

        /**
         * 设置订单价格
         *
         * @param price price
         * @return this
         */
        public Builder setPrice(String price) {
            this.price = price;
            return this;
        }

        /**
         * 设置回调
         *
         * @param callbackUrl callbackUrl
         * @return this
         */
        public Builder setCallbackUrl(String callbackUrl) {
            this.callbackUrl = callbackUrl;
            return this;
        }

        public Builder setOnPaymentListener(OnAliPayListener onAliPayListener) {
            this.onAliPayListener = onAliPayListener;
            return this;
        }

        public AliPay build() {
            AliPay aliPayReq = new AliPay();
            aliPayReq.mActivity = this.activity;
            aliPayReq.outTradeNo = this.outTradeNo;
            aliPayReq.subject = this.subject;
            aliPayReq.body = this.body;
            aliPayReq.price = this.price;
            aliPayReq.callbackUrl = this.callbackUrl;
            aliPayReq.rsaPrivate = this.rsaPrivate;
            aliPayReq.rsaPublic = this.rsaPublic;
            aliPayReq.appId = this.appId;
            aliPayReq.seller = this.seller;
            aliPayReq.partner = this.partner;
            aliPayReq.mOnAliPayListener = this.onAliPayListener;

            return aliPayReq;
        }
    }

    /**
     * 支付宝支付监听
     */
    public interface OnAliPayListener {
        void onPaySuccess(String resultInfo);

        void onPayFailed(String resultInfo);

        void onPayConfirming(String resultInfo);

        void onPayCheck(String status);
    }
}
