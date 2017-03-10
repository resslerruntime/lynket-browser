package arun.com.chromer.activities.browsing.incognito;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import arun.com.chromer.R;
import arun.com.chromer.activities.CustomTabActivity;
import arun.com.chromer.activities.OpenIntentWithActivity;
import arun.com.chromer.activities.settings.Preferences;
import arun.com.chromer.customtabs.callbacks.ClipboardService;
import arun.com.chromer.customtabs.callbacks.FavShareBroadcastReceiver;
import arun.com.chromer.customtabs.callbacks.SecondaryBrowserReceiver;
import arun.com.chromer.data.website.WebsiteRepository;
import arun.com.chromer.data.website.model.WebSite;
import arun.com.chromer.util.Utils;
import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK;
import static android.graphics.Color.WHITE;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static arun.com.chromer.activities.settings.Preferences.PREFERRED_ACTION_BROWSER;
import static arun.com.chromer.activities.settings.Preferences.PREFERRED_ACTION_FAV_SHARE;
import static arun.com.chromer.activities.settings.Preferences.PREFERRED_ACTION_GEN_SHARE;
import static arun.com.chromer.shared.Constants.ACTION_MINIMIZE;
import static arun.com.chromer.shared.Constants.EXTRA_KEY_ORIGINAL_URL;
import static arun.com.chromer.shared.Constants.EXTRA_KEY_WEBSITE;

public class WebViewActivity extends AppCompatActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.web_view)
    WebView webView;
    private String baseUrl = "";
    private BroadcastReceiver minimizeReceiver;
    private final CompositeSubscription subscriptions = new CompositeSubscription();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseUrl = getIntent().getDataString();

        setContentView(R.layout.activity_web_view);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        final WebSite webSite = getIntent().getParcelableExtra(EXTRA_KEY_WEBSITE);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(xyz.klinker.android.article.R.drawable.article_ic_close);
            getSupportActionBar().setTitle(webSite != null ? webSite.safeLabel() : baseUrl);
        }


        if (Preferences.get(this).aggressiveLoading()) {
            delayedGoToBack();
        }
        registerMinimizeReceiver();
        beginExtraction(webSite);

      /*  webView.setWebViewClient(new WebViewClient() {

        });
        final WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webView.loadUrl(getIntent().getDataString());*/
    }

    private void beginExtraction(@Nullable WebSite webSite) {
        if (webSite != null && webSite.title != null && webSite.faviconUrl != null) {
            Timber.d("Website info exists, setting description");
            applyDescriptionFromWebsite(webSite);
        } else {
            Timber.d("No info found, beginning parsing");
            final Subscription s = WebsiteRepository.getInstance(this)
                    .getWebsite(baseUrl)
                    .doOnNext(this::applyDescriptionFromWebsite)
                    .doOnError(Timber::e)
                    .subscribe();
            subscriptions.add(s);
        }
    }

    private void delayedGoToBack() {
        new Handler().postDelayed(() -> moveTaskToBack(true), 650);
    }

    @TargetApi(LOLLIPOP)
    private void applyDescriptionFromWebsite(@Nullable final WebSite webSite) {
        if (webSite != null) {
            final String title = webSite.safeLabel();
            final String faviconUrl = webSite.faviconUrl;
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
            }
            if (Utils.isLollipopAbove()) {
                setTaskDescription(new ActivityManager.TaskDescription(title, null, webSite.themeColor()));
                Glide.with(this)
                        .load(faviconUrl)
                        .asBitmap()
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(Bitmap icon, GlideAnimation<? super Bitmap> glideAnimation) {
                                setTaskDescription(new ActivityManager.TaskDescription(title, icon, webSite.themeColor()));
                            }
                        });
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.article_view_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem actionButton = menu.findItem(R.id.menu_action_button);
        switch (Preferences.get(this).preferredAction()) {
            case PREFERRED_ACTION_BROWSER:
                final String browser = Preferences.get(this).secondaryBrowserPackage();
                if (Utils.isPackageInstalled(this, browser)) {
                    actionButton.setTitle(R.string.choose_secondary_browser);
                    final ComponentName componentName = ComponentName.unflattenFromString(Preferences.get(this).secondaryBrowserComponent());
                    try {
                        actionButton.setIcon(getPackageManager().getActivityIcon(componentName));
                    } catch (PackageManager.NameNotFoundException e) {
                        actionButton.setVisible(false);
                    }
                } else {
                    actionButton.setVisible(false);
                }
                break;
            case PREFERRED_ACTION_FAV_SHARE:
                final String favShare = Preferences.get(this).favSharePackage();
                if (Utils.isPackageInstalled(this, favShare)) {
                    actionButton.setTitle(R.string.fav_share_app);
                    final ComponentName componentName = ComponentName.unflattenFromString(Preferences.get(this).favShareComponent());
                    try {
                        actionButton.setIcon(getPackageManager().getActivityIcon(componentName));
                    } catch (PackageManager.NameNotFoundException e) {
                        actionButton.setVisible(false);
                    }
                } else {
                    actionButton.setVisible(false);
                }
                break;
            case PREFERRED_ACTION_GEN_SHARE:
                actionButton.setIcon(new IconicsDrawable(this)
                        .icon(CommunityMaterial.Icon.cmd_share_variant)
                        .color(WHITE)
                        .sizeDp(24));
                actionButton.setTitle(R.string.share);
                break;
        }

        final MenuItem fullPage = menu.findItem(R.id.menu_open_full_page);
        fullPage.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_button:
                switch (Preferences.get(this).preferredAction()) {
                    case PREFERRED_ACTION_BROWSER:
                        sendBroadcast(new Intent(this, SecondaryBrowserReceiver.class)
                                .setData(Uri.parse(baseUrl)));
                        break;
                    case PREFERRED_ACTION_FAV_SHARE:
                        sendBroadcast(new Intent(this, FavShareBroadcastReceiver.class)
                                .setData(Uri.parse(baseUrl)));
                        break;
                    case PREFERRED_ACTION_GEN_SHARE:
                        shareUrl();
                        break;
                }
                break;
            case R.id.menu_copy_link:
                startService(new Intent(this, ClipboardService.class)
                        .setData(Uri.parse(baseUrl)));
                break;
            case R.id.menu_open_with:
                startActivity(new Intent(this, OpenIntentWithActivity.class)
                        .setData(Uri.parse(baseUrl)));
                break;
            case R.id.menu_share:
                shareUrl();
                break;
            case R.id.menu_open_full_page:
                final Intent customTabActivity = new Intent(this, CustomTabActivity.class);
                customTabActivity.setData(Uri.parse(baseUrl));
                if (Preferences.get(this).mergeTabs()) {
                    customTabActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                    customTabActivity.addFlags(FLAG_ACTIVITY_MULTIPLE_TASK);
                }
                startActivity(customTabActivity);
                finish();
                break;
            case R.id.menu_more:
                Toast.makeText(this, "Todo", Toast.LENGTH_SHORT).show();
                break;
        }
        return true;
    }

    private void shareUrl() {
        Utils.shareText(this, baseUrl);
    }

    private void registerMinimizeReceiver() {
        minimizeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equalsIgnoreCase(ACTION_MINIMIZE) && intent.hasExtra(EXTRA_KEY_ORIGINAL_URL)) {
                    final String url = intent.getStringExtra(EXTRA_KEY_ORIGINAL_URL);
                    if (baseUrl.equalsIgnoreCase(url)) {
                        try {
                            Timber.d("Minimized %s", url);
                            moveTaskToBack(true);
                        } catch (Exception e) {
                            Timber.e(e);
                        }
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(minimizeReceiver, new IntentFilter(ACTION_MINIMIZE));
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(minimizeReceiver);
        subscriptions.clear();
        super.onDestroy();
    }
}
