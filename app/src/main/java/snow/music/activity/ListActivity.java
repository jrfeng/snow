package snow.music.activity;

import android.os.Bundle;

import androidx.annotation.Nullable;

import snow.music.R;

/**
 * 那些用来展示列表的 Activity 可以继承该类，该类对过渡动画进行了自定义配置。
 */
public class ListActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_bottom_slide_in, R.anim.activity_no_transition);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_no_transition, R.anim.activity_fade_out);
    }
}
