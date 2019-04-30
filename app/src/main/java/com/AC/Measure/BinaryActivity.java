package com.AC.Measure;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import java.io.FileInputStream;
import java.io.FileNotFoundException;



public class BinaryActivity extends Activity {

    private ImageView iv;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_binary);
        init(); // 初始化
    }

    @Override
    protected void onPause() { // onPause() 為 TabActivity 所使用
        iv.setVisibility(View.INVISIBLE); // 隱藏 Binary 圖片
        super.onPause();
    }

    @Override
    protected void onResume() { // onResume() 為 TabActivity 所使用
        super.onResume();
        iv.setVisibility(View.VISIBLE); // 顯示 Binary 圖片
    }

    private void init() { // 初始化
        try {
            BitmapFactory.Options options = new BitmapFactory.Options(); // 防止OOM(Out of Memory 所設定的選項物件
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inSampleSize = 2;

            Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(ArMeasureActivity.Save_path + "/" + getIntent().getExtras().getString("binary")), null, options); // 讀取 binary 圖檔
            Matrix matrix = new Matrix(); // 圖片旋轉 90 度所設定
            matrix.setRotate(359.0f);

            iv = new ImageView(this); // 產生顯示圖片物件
            iv.setBitmap(Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false)); // 重新建立翻轉 90 度的圖片並設定到顯示圖片的物件上
            ((FrameLayout) findViewById(R.id.binary)).addView(iv); // 將顯示圖片物件設定到 Layout 上

            bitmap.recycle(); // 馬上回收圖片 防止 OOM
            System.gc(); // 提出垃圾回收建議 防止 OOM
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}