package snow.music.databinding;

import android.widget.ImageView;

import androidx.databinding.BindingAdapter;

public final class AppBinderAdapter {
    @BindingAdapter("srcCompat")
    public static void setSrcCompat(ImageView imageView, int resId) {
        imageView.setImageResource(resId);
    }
}