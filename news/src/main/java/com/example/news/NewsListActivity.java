package com.example.news;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import io.github.iamyours.router.annotation.Route;

@Route(path = "/news/news_list")
public class NewsListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_main);
        TextView titleTv = findViewById(R.id.title1);
        if (getIntent() != null) {
            String title = getIntent().getStringExtra("title");
            titleTv.setText(title);
        }
        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("name", "张三");
                setResult(1000, intent);
                finish();
            }
        });
    }
}
