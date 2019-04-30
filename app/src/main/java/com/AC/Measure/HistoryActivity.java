package com.AC.Measure;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class HistoryActivity extends AppCompatActivity {
    private Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        initView();
    }
    private void initView() {
        new AlertDialog.Builder(HistoryActivity.this) // 建立互動訊息
                .setMessage("目前無歷史資料 !")
                .setCancelable(false)
                .setPositiveButton("返回", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //context = this;
                        startActivity(new Intent(context, ArMeasureActivity.class));
                    }
                }).show();
    }
}
