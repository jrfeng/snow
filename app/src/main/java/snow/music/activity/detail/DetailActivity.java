package snow.music.activity.detail;

import android.os.Bundle;

import androidx.annotation.Nullable;

import snow.music.R;
import snow.music.activity.BaseActivity;

/**
 * 对 Activity 的过渡动画进行了自定义。
 */
public class DetailActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_no_transition);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_no_transition, R.anim.activity_fade_out);
    }
}
