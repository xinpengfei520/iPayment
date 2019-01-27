package com.xpf.pay.library.h5;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.xpf.pay.library.BuildConfig;

/**
 * Created by x-sir on 2019/1/27 :)
 * Function:
 */
public class PayWebView extends WebView {

    public PayWebView(Context context) {
        this(context, null);
    }

    public PayWebView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PayWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        WebSettings settings = getSettings();
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setJavaScriptEnabled(true);
        settings.setSavePassword(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setAllowFileAccess(false);

        // 启用 WebView 调试模式,注意，请勿在实际 App 中打开！
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }
}
