package com.xpf.pay.library.h5;

import android.app.Activity;
import android.text.TextUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.alipay.sdk.app.H5PayCallback;
import com.alipay.sdk.app.PayTask;
import com.alipay.sdk.util.H5PayResultModel;

/**
 * Created by x-sir on 2019/1/27 :)
 * Function:
 */
public class PayWebViewClient extends WebViewClient {

    private Activity mActivity;

    public PayWebViewClient(Activity activity) {
        this.mActivity = activity;
    }

    @Override
    public boolean shouldOverrideUrlLoading(final WebView view, String url) {
        if (!(url.startsWith("http") || url.startsWith("https"))) {
            return true;
        }

        /**
         * 推荐采用的新的二合一接口(payInterceptorWithUrl),只需调用一次
         */
        final PayTask task = new PayTask(mActivity);
        boolean isIntercepted = task.payInterceptorWithUrl(url, true, new H5PayCallback() {
            @Override
            public void onPayResult(final H5PayResultModel result) {
                final String url = result.getReturnUrl();
                if (!TextUtils.isEmpty(url)) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            view.loadUrl(url);
                        }
                    });
                }
            }
        });

        /**
         * 判断是否成功拦截
         * 若成功拦截，则无需继续加载该URL；否则继续加载
         */
        if (!isIntercepted) {
            view.loadUrl(url);
        }
        return true;
    }
}
