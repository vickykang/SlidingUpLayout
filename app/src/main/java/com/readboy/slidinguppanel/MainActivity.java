package com.readboy.slidinguppanel;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.readboy.slidinguppanel.views.SlidingUpLayout;


public class MainActivity extends Activity {

    private int[] resIds = {
            R.drawable.selector_btn_dragger,
            R.drawable.selector_btn_common,
            R.drawable.image
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button mButton = (Button) findViewById(R.id.btn_change_dragger);
        final SlidingUpLayout mSlidingLayout = (SlidingUpLayout) findViewById(R.id.sliding_layout);



        mButton.setOnClickListener(new View.OnClickListener() {
            int index = 0;

            @Override
            public void onClick(View v) {
                mSlidingLayout.setDraggerBackgroundResource(resIds[index]);
                if (++index >= resIds.length)
                    index = 0;
            }
        });
    }
}
