package com.module.dot.view;

import android.os.Bundle;
import android.view.KeyEvent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.module.dot.R;
import com.module.dot.view.adapters.MainPagerAdapter;
import com.module.dot.view.utils.ScannerGunHelper;

public class MainActivity extends AppCompatActivity {

    private ScanFragment scanFragment;
    private ScannerGunHelper scannerGunHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scannerGunHelper = new ScannerGunHelper();

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        MainPagerAdapter adapter = new MainPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "盘点" : "历史");
        }).attach();

        // Get reference to ScanFragment
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    scanFragment = (ScanFragment) getSupportFragmentManager()
                            .findFragmentByTag("f0");
                }
            }
        });
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Intercept scanner gun input
        String barcode = scannerGunHelper.processKeyEvent(event);
        if (barcode != null && scanFragment != null) {
            scanFragment.onBarcodeScanned(barcode);
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    public ScannerGunHelper getScannerGunHelper() {
        return scannerGunHelper;
    }
}
