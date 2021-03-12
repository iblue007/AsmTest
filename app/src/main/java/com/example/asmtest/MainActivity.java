package com.example.asmtest;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import io.github.iamyours.router.ARouter;
import io.github.iamyours.router.Callback;
import io.github.iamyours.router.annotation.Route;

@Route(path = "/app/main")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final TextView tv1 = findViewById(R.id.tv1);
        final TextView tv2 = findViewById(R.id.tv2);
        final TextView tv3 = findViewById(R.id.tv3);
        tv1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ARouter.getInstance().build("/news/news_list")
                        .withString("title", "from MainActivity")
                        .navigation(MainActivity.this, 1, new Callback() {
                            @Override
                            public void onActivityResult(int requestCode, int resultCode, Intent data) {
                                Log.i("test", "resultCode:$resultCode,data:$data");
                                if (data != null) {
                                    String name = data.getStringExtra("name");
                                    if (!TextUtils.isEmpty(name)) {
                                        tv1.setText(name);
                                    } else {
                                        tv1.setText("没有获取到数据");
                                    }

                                }
                            }
                        });
            }
        });

        tv2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Log.e("======", "======点我一下");
            }
        });
        tv3.setOnClickListener(new View.OnClickListener() {
            @NoDoubleClick
            @Override
            public void onClick(View view) {
                Log.e("======", "======点我一下（白名单）");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}