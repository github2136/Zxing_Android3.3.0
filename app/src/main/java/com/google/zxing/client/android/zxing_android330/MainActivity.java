package com.google.zxing.client.android.zxing_android330;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.zxing.client.android.ZxingActivity;

public class MainActivity extends AppCompatActivity {
    TextView tv1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btn1 = (Button) findViewById(R.id.btn_1);
        tv1 = (TextView) findViewById(R.id.tv_1);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ZxingActivity.class);
                intent.putExtra(ZxingActivity.ARG_SCAN_WIDTH_DP, 200);
                intent.putExtra(ZxingActivity.ARG_SCAN_HEIGHT_DP, 200);
                intent.putExtra(ZxingActivity.ARG_SCAN_PIC, true);
//                intent.putExtra(ZxingActivity.ARG_SCAN_HEIGHT_SCALE, 0d);
                intent.putExtra(ZxingActivity.ARG_SCAN_COLOR, ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null));
                startActivityForResult(intent, 1);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            tv1.setText(data.getStringExtra(ZxingActivity.ARG_RESULT));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
