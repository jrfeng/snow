package snow.music.databinding;

import android.widget.ImageView;

import androidx.databinding.BindingAdapter;
import androidx.databinding.BindingMethod;
import androidx.databinding.BindingMethods;

@BindingMethods({
        @BindingMethod(
                type = ImageView.class,
                attribute = "android:src",
                method = "setImageResource"
        )
})
public final class AppBinderAdapter {
    @BindingAdapter("srcCompat")
    public static void setSrcCompat(ImageView imageView, int resId) {
        imageView.setImageResource(resId);
    }
}