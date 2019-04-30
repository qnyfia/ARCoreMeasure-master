package com.AC.Measure;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

public class ImageActivity extends TabActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
//        TextView result = findViewById(R.id.result);
//        Intent intent = getIntent();
//        float distance_result = intent.getExtras().getFloat("Distance_result",0);
//        result.setText("Distance"+ distance_result);
        init();
    }

    @Override
    protected void onDestroy() { // 當顯示圖片的 Activity 結束被釋放時
        super.onDestroy();
        ImageView.reset(); // 重新設定 ImageView 有關靜態部份共用的變數
    }
    private void init() {
        Bundle bundle = getIntent().getExtras();
        TabHost tabhost = getTabHost();
        Intent intent = new Intent(this, CannyActivity.class);
        intent.putExtras(bundle);
        tabhost.addTab(tabhost.newTabSpec("坎尼邊緣偵測").setIndicator("坎尼邊緣偵測").setContent(intent)); // 設定第一個 Tab

        intent = new Intent(this, BinaryActivity.class);
        intent.putExtras(bundle);
        tabhost.addTab(tabhost.newTabSpec("二值化影像處理").setIndicator("二值化影像處理").setContent(intent)); // 設定第二個 Tab

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageView.clearPoint(); // 清除原有在 ImageView 上所選的點
                Display display = getWindowManager().getDefaultDisplay();
                // 取得螢幕高度
                int height = display.getHeight();
                Toast toast = Toast.makeText(ImageActivity.this, "量測點佈點已清除 !", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM, 0, height / 10);
                toast.show();
            }
        });
    }
}
