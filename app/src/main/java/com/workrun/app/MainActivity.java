package com.workrun.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.net.URISyntaxException;
import java.util.ArrayList;

public final class MainActivity extends Activity {
    private static final String HOME = "https://workrun.girish-suchil01.chatgpt.site/";
    private static final String HOST = "workrun.girish-suchil01.chatgpt.site";
    private static final int GREEN = Color.rgb(6,62,48);
    private WebView main;
    private FrameLayout content;
    private LinearLayout error;
    private ProgressBar progress;
    private boolean mainFailed;
    private final ArrayList<WebView> popups = new ArrayList<WebView>();

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        progress = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);root.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));
        content=new FrameLayout(this);root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        main=makeWebView(false);content.addView(main,new FrameLayout.LayoutParams(-1,-1));
        error=makeError();content.addView(error,new FrameLayout.LayoutParams(-1,-1));error.setVisibility(View.GONE);
        setContentView(root);
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(
                    android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.ime());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
            root.requestApplyInsets();
        }
        CookieManager.getInstance().setAcceptCookie(true);
        // Always fetch the current published site on a fresh app launch.
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::navigateBack);
        }
        main.loadUrl(HOME);
    }
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}
    private LinearLayout makeError(){
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);panel.setPadding(dp(28),dp(28),dp(28),dp(28));panel.setBackgroundColor(Color.WHITE);
        TextView title=new TextView(this);title.setText("Unable to connect");title.setTextSize(24);title.setTextColor(GREEN);title.setGravity(Gravity.CENTER);panel.addView(title);
        TextView help=new TextView(this);help.setText("Check your internet connection and try again. WorkRun uses the live website.");help.setTextSize(16);help.setGravity(Gravity.CENTER);help.setPadding(0,dp(16),0,dp(24));panel.addView(help);
        Button retry=new Button(this);retry.setText("Try again");retry.setOnClickListener(v->{mainFailed=false;error.setVisibility(View.GONE);main.reload();});panel.addView(retry);
        Button browser=new Button(this);browser.setText("Open in browser");browser.setOnClickListener(v->openBrowser(HOME));panel.addView(browser);return panel;
    }
    private WebView makeWebView(final boolean popup){
        final WebView web=new WebView(this);
        WebSettings settings=web.getSettings();settings.setUserAgentString(settings.getUserAgentString()+" WorkRunAndroid/2.2");settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);settings.setMediaPlaybackRequiresUserGesture(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return route(request.getUrl(),request.isForMainFrame(),popup);}
            @Override public boolean shouldOverrideUrlLoading(WebView view,String url){return route(Uri.parse(url),true,popup);}
            @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap favicon){progress.setVisibility(View.VISIBLE);if(!popup){mainFailed=false;error.setVisibility(View.GONE);}}
            @Override public void onPageFinished(WebView view,String url){progress.setVisibility(View.GONE);CookieManager.getInstance().flush();if(!popup&&!mainFailed)error.setVisibility(View.GONE);}
            @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError failure){if(request.isForMainFrame()){if(popup)Toast.makeText(MainActivity.this,"Payment page could not load. Check your connection.",Toast.LENGTH_LONG).show();else showError();}}
            @Override public void onReceivedHttpError(WebView view,WebResourceRequest request,WebResourceResponse response){if(request.isForMainFrame()&&response.getStatusCode()>=500&&!popup)showError();}
            @Override public void onReceivedSslError(WebView view,SslErrorHandler handler,SslError failure){handler.cancel();if(!popup)showError();else Toast.makeText(MainActivity.this,"This page could not establish a secure connection.",Toast.LENGTH_LONG).show();}
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView view,int value){progress.setProgress(value);if(value==100)progress.setVisibility(View.GONE);}
            @Override public boolean onCreateWindow(WebView view,boolean dialog,boolean userGesture,Message result){
                final WebView child=makeWebView(true);popups.add(child);content.addView(child,new FrameLayout.LayoutParams(-1,-1));
                WebView.WebViewTransport transport=(WebView.WebViewTransport)result.obj;transport.setWebView(child);result.sendToTarget();return true;
            }
            @Override public void onCloseWindow(WebView window){closePopup(window);}
        });
        return web;
    }
    private boolean route(Uri uri,boolean top,boolean popup){
        String scheme=uri.getScheme();if(scheme==null)return true;
        if("https".equalsIgnoreCase(scheme)){
            String host=uri.getHost();boolean internal=HOST.equalsIgnoreCase(host)||(host!=null&&(host.equals("razorpay.com")||host.endsWith(".razorpay.com")));
            if(!top||popup||internal)return false;
            openBrowser(uri.toString());return true;
        }
        if("about".equals(scheme)&&"about:blank".equals(uri.toString()))return false;
        if("http".equalsIgnoreCase(scheme)){Toast.makeText(this,"Only secure HTTPS links are supported.",Toast.LENGTH_SHORT).show();return true;}
        if("intent".equals(scheme)){
            try{Intent target=Intent.parseUri(uri.toString(),Intent.URI_INTENT_SCHEME);
                String fallback=target.getStringExtra("browser_fallback_url");target.setComponent(null);target.setSelector(null);target.addCategory(Intent.CATEGORY_BROWSABLE);
                String targetScheme=target.getData()!=null?target.getData().getScheme():"";
                if(!allowedExternal(targetScheme))return true;
                target.removeExtra("browser_fallback_url");
                try{startActivity(target);}catch(ActivityNotFoundException e){if(fallback!=null&&"https".equals(Uri.parse(fallback).getScheme()))openBrowser(fallback);else Toast.makeText(this,"Install a compatible payment app or use another payment method.",Toast.LENGTH_LONG).show();}
            }catch(URISyntaxException e){Toast.makeText(this,"Unable to open this link.",Toast.LENGTH_SHORT).show();}
            return true;
        }
        if(allowedExternal(scheme)){try{Intent intent=new Intent(Intent.ACTION_VIEW,uri);intent.addCategory(Intent.CATEGORY_BROWSABLE);startActivity(intent);}catch(ActivityNotFoundException e){Toast.makeText(this,"No compatible app is installed.",Toast.LENGTH_LONG).show();}return true;}
        return true;
    }
    private boolean allowedExternal(String scheme){return "https".equals(scheme)||"upi".equals(scheme)||"tel".equals(scheme)||"mailto".equals(scheme)||"whatsapp".equals(scheme)||"phonepe".equals(scheme)||"paytmmp".equals(scheme)||"tez".equals(scheme)||"gpay".equals(scheme)||"bhim".equals(scheme)||"credpay".equals(scheme);}
    private void openBrowser(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(ActivityNotFoundException e){Toast.makeText(this,"Install a browser to open this link.",Toast.LENGTH_SHORT).show();}}
    private void showError(){mainFailed=true;progress.setVisibility(View.GONE);error.setVisibility(View.VISIBLE);}
    private void closePopup(WebView web){if(popups.remove(web)){content.removeView(web);web.stopLoading();web.destroy();}}
    private void closeAllPopups(){while(!popups.isEmpty())closePopup(popups.get(popups.size()-1));}
    // Older Android fallback; API 33+ uses OnBackInvokedDispatcher.
@android.annotation.SuppressLint("GestureBackNavigation")
@Override
public void onBackPressed() {
    navigateBack();
}
    private void navigateBack(){if(!popups.isEmpty()){WebView current=popups.get(popups.size()-1);if(current.canGoBack())current.goBack();else closePopup(current);}else if(main.canGoBack())main.goBack();else finish();}
    @Override public void onPause(){super.onPause();CookieManager.getInstance().flush();}
    @Override public void onDestroy(){closeAllPopups();if(main!=null){((ViewGroup)main.getParent()).removeView(main);main.destroy();}super.onDestroy();}
}
