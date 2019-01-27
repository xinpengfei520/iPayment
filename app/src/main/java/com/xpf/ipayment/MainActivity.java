package com.xpf.ipayment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.xpf.pay.library.api.PayApi;
import com.xpf.pay.library.pay.AliPay;
import com.xpf.pay.library.pay.AliPayV2;
import com.xpf.pay.library.pay.WeChatPay;

/**
 * 重要说明：
 * 本 Demo 只是为了方便直接向商户展示支付宝的整个支付流程，所以将加签过程直接放在客户端完成
 * （包括OrderInfoUtil2_0）。
 * 在真实 App 中，私钥（如 RSA_PRIVATE 等）数据严禁放在客户端，同时加签过程务必要放在服务端完成，
 * 否则可能造成商户私密数据泄露或被盗用，造成不必要的资金损失，面临各种安全风险。
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();
    }

    /**
     * 获取权限使用的 RequestCode
     */
    private static final int PERMISSIONS_REQUEST_CODE = 1002;

    /**
     * 检查支付宝 SDK 所需的权限，并在必要的时候动态获取。
     * 在 targetSDK = 23 以上，READ_PHONE_STATE 和 WRITE_EXTERNAL_STORAGE 权限需要应用在运行时获取。
     * 如果接入支付宝 SDK 的应用 targetSdk 在 23 以下，可以省略这个步骤。
     */
    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
        } else {
            showToast(getString(R.string.permission_already_granted));
        }
    }

    /**
     * 权限获取回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                // 用户取消了权限弹窗
                if (grantResults.length == 0) {
                    showToast(getString(R.string.permission_rejected));
                    return;
                }

                // 用户拒绝了某些权限
                for (int x : grantResults) {
                    if (x == PackageManager.PERMISSION_DENIED) {
                        showToast(getString(R.string.permission_rejected));
                        return;
                    }
                }

                // 所需的权限均正常获取
                showToast(getString(R.string.permission_granted));
            }
        }
    }

    public void payV2(View v) {
        //pay();
    }

    /**
     * 支付宝支付业务示例
     */
    public void pay(String rsaPrivate, String rsaPublic, String partner, String seller, String outTradeNo, String price, String orderSubject, String orderBody, String callbackUrl) {
        AliPay.OnAliPayListener onAliPayListener = new AliPay.OnAliPayListener() {
            @Override
            public void onPaySuccess(String resultInfo) {

            }

            @Override
            public void onPayFailed(String resultInfo) {

            }

            @Override
            public void onPayConfirming(String resultInfo) {

            }

            @Override
            public void onPayCheck(String status) {

            }
        };

        // 1.构建支付宝支付配置
        AliPay aliPay = new AliPay.Builder(this)
                .setRsaPrivate(rsaPrivate) //设置私钥 (商户私钥，pkcs8格式)
                .setRsaPublic(rsaPublic) // 设置公钥(// 支付宝公钥)
                .setPartner(partner) // 设置商户
                .setSeller(seller) // 设置商户收款账号
                .setOutTradeNo(outTradeNo) // 设置唯一订单号
                .setPrice(price) // 设置订单价格
                .setSubject(orderSubject) // 设置订单标题
                .setBody(orderBody) // 设置订单内容 订单详情
                .setCallbackUrl(callbackUrl) // 设置回调地址
                .setOnPaymentListener(onAliPayListener) // 设置支付的监听器
                .build();

        String sdkVersion = aliPay.getSdkVersion();
        showToast("sdkVersion:" + sdkVersion);

        // 2.发送支付宝支付请求
        PayApi.getInstance().toPay(aliPay);
    }

    /**
     * 支付宝支付业务示例
     */
    public void payV2(String partner, String seller, String outTradeNo, String price, String orderSubject, String orderBody, String callbackUrl) {
        // 1.创建支付宝支付订单的信息
        String rawAliOrderInfo = new AliPayV2.AliOrderInfo()
                .setPartner(partner) //商户PID || 签约合作者身份ID
                .setSeller(seller)  // 商户收款账号 || 签约卖家支付宝账号
                .setOutTradeNo(outTradeNo) //设置唯一订单号
                .setSubject(orderSubject) //设置订单标题
                .setBody(orderBody) //设置订单内容
                .setPrice(price) //设置订单价格
                .setCallbackUrl(callbackUrl) //设置回调链接
                .createOrderInfo(); //创建支付宝支付订单信息


        // 2.签名  支付宝支付订单的信息 ===>>>  商户私钥签名之后的订单信息
        // TODO 这里需要从服务器获取用商户私钥签名之后的订单信息
        String signAliOrderInfo = getSignAliOrderInfoFromServer(rawAliOrderInfo);

        AliPayV2 aliPayV2 = new AliPayV2.Builder(this)
                .setRawAliPayOrderInfo(rawAliOrderInfo)//支付宝支付订单信息
                .setSignedAliPayOrderInfo(signAliOrderInfo) //设置 商户私钥RSA加密后的支付宝支付订单信息
                .setOnAliPayListener(null)
                .build();

        // 3.发送支付宝支付请求

        PayApi.getInstance().toPay(aliPayV2);
    }

    private String getSignAliOrderInfoFromServer(String rawAliOrderInfo) {
        return null;
    }

    /**
     * 支付宝账户授权业务示例
     */
    public void authV2(View v) {

    }

    /**
     * 获取支付宝 SDK 版本号。
     */
    public void showSdkVersion(View v) {

    }

    /**
     * 将 H5 网页版支付转换成支付宝 App 支付的示例
     */
    public void h5Pay(View v) {
        Intent intent = new Intent(this, H5PayDemoActivity.class);
        Bundle extras = new Bundle();

        /*
         * URL 是要测试的网站，在 Demo App 中会使用 H5PayDemoActivity 内的 WebView 打开。
         *
         * 可以填写任一支持支付宝支付的网站（如淘宝或一号店），在网站中下订单并唤起支付宝；
         * 或者直接填写由支付宝文档提供的“网站 Demo”生成的订单地址
         * （如 https://mclient.alipay.com/h5Continue.htm?h5_route_token=303ff0894cd4dccf591b089761dexxxx）
         * 进行测试。
         *
         * H5PayDemoActivity 中的 MyWebViewClient.shouldOverrideUrlLoading() 实现了拦截 URL 唤起支付宝，
         * 可以参考它实现自定义的 URL 拦截逻辑。
         */
        String url = "https://m.taobao.com";
        extras.putString("url", url);
        intent.putExtras(extras);
        startActivity(intent);
    }

    private void wechatPay(String appid, String partnerid, String prepayid, String noncestr, String timestamp, String sign, String orderBody, String callbackUrl) {
        WeChatPay.OnWeChatPayListener onWeChatPayListener = new WeChatPay.OnWeChatPayListener() {
            @Override
            public void onPaySuccess(int errorCode) {

            }

            @Override
            public void onPayFailure(int errorCode) {

            }
        };

        // 1.创建微信支付请求
        WeChatPay weChatPay = new WeChatPay.Builder(this)
                .setAppId(appid) // 微信支付 AppID
                .setPartnerId(partnerid) // 微信支付商户号
                .setPrepayId(prepayid) // 预支付码
                //.setPackageValue(wechatPayReq.get)//"Sign=WXPay"
                .setNonceStr(noncestr)
                .setTimeStamp(timestamp) // 时间戳
                .setSign(sign) // 签名
                .setOnPaymentListener(onWeChatPayListener)
                .build();

        // 2.发送微信支付请求
        PayApi.getInstance().toPay(weChatPay);
    }

    private void showToast(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }
}
