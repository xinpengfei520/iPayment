package com.xpf.ipayment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.xpf.pay.library.h5.PayWebView;
import com.xpf.pay.library.h5.PayWebViewClient;

public class H5PayDemoActivity extends Activity {

    private PayWebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = null;
        try {
            extras = getIntent().getExtras();
        } catch (Exception e) {
            finish();
            return;
        }
        if (extras == null) {
            finish();
            return;
        }
        String url = null;
        try {
            url = extras.getString("url");
        } catch (Exception e) {
            finish();
            return;
        }
        if (TextUtils.isEmpty(url)) {
            // 测试H5支付，必须设置要打开的url网站
            new AlertDialog.Builder(H5PayDemoActivity.this).setTitle(R.string.error)
                    .setMessage(R.string.error_missing_h5_pay_url)
                    .setPositiveButton(R.string.confirm, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            finish();
                        }
                    }).show();

        }
        super.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout layout = new LinearLayout(getApplicationContext());
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        layout.setOrientation(LinearLayout.VERTICAL);
        setContentView(layout, params);

        mWebView = new PayWebView(getApplicationContext());
        params.weight = 1;
        mWebView.setVisibility(View.VISIBLE);
        layout.addView(mWebView, params);

        mWebView.setVerticalScrollbarOverlay(true);
        mWebView.setWebViewClient(new PayWebViewClient(this));
        mWebView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            finish();
        }
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebView != null) {
            mWebView.removeAllViews();
            try {
                mWebView.destroy();
            } catch (Throwable t) {
                t.printStackTrace();
            }
            mWebView = null;
        }
    }
}
