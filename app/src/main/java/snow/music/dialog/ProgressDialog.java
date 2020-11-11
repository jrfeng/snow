package snow.music.dialog;

import android.content.Context;
import android.os.Build;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDialog;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import snow.music.R;

public class ProgressDialog extends BottomDialog {
    private String mTitle;
    private int mMax = 100;
    private int mProgress;
    private int mSecondaryProgress;

    @Nullable
    private ProgressBar mProgressBar;
    @Nullable
    private TextView tvProgress;

    private Disposable mDelayDismissDisposable;

    @Override
    protected void onInitDialog(AppCompatDialog dialog) {
        dialog.setContentView(R.layout.dialog_progress);

        mProgressBar = dialog.findViewById(R.id.progressBar);
        tvProgress = dialog.findViewById(R.id.tvProgress);

        TextView tvDialogTitle = dialog.findViewById(R.id.tvDialogTitle);
        Objects.requireNonNull(tvDialogTitle).setText(mTitle);

        updateProgress();
        dialog.setCanceledOnTouchOutside(false);
    }

    @Override
    protected boolean keepOnRestarted() {
        return false;
    }

    public void setMax(int max) {
        mMax = Math.max(1, max);
        updateProgress();
    }

    public void setProgress(int progress) {
        mProgress = Math.max(0, progress);
        updateProgress();
    }

    public void setSecondaryProgress(int secondaryProgress) {
        mSecondaryProgress = Math.max(0, secondaryProgress);
        updateProgress();
    }

    @Override
    public void dismiss() {
        if (mDelayDismissDisposable != null && !mDelayDismissDisposable.isDisposed()) {
            mDelayDismissDisposable.dispose();
        }
        super.dismiss();
    }

    /**
     * 延迟 500 毫秒后执行 dismiss() 操作。
     */
    public void delayDismiss() {
        mDelayDismissDisposable = Observable.timer(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> dismiss());
    }

    private void updateProgress() {
        if (mProgressBar != null) {
            mProgressBar.setMax(mMax);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mProgressBar.setProgress(mProgress, true);
            } else {
                mProgressBar.setProgress(mProgress);
            }
            mProgressBar.setSecondaryProgress(mSecondaryProgress);
        }

        if (tvProgress != null) {
            tvProgress.setText(getTextPercent());
        }
    }

    private String getTextPercent() {
        int percent = (int) (((mProgress * 1.0) / mMax) * 100);
        return percent + "%";
    }

    public static class Builder {
        private Context mContext;
        private String mTitle;
        private int mMax;
        private int mProgress;
        private int mSecondaryProgress;

        public Builder(@NonNull Context context) {
            Preconditions.checkNotNull(context);
            mContext = context;
            mTitle = context.getString(R.string.title_progress);
            mProgress = 0;
            mMax = 100;
        }

        public Builder setTitle(@StringRes int resId) {
            return setTitle(mContext.getString(resId));
        }

        public Builder setTitle(@NonNull String title) {
            Preconditions.checkNotNull(title);
            mTitle = title;
            return this;
        }

        public Builder setMax(int max) {
            mMax = Math.max(1, max);
            return this;
        }

        public Builder setProgress(int progress) {
            mProgress = Math.max(0, progress);
            return this;
        }

        public Builder setSecondaryProgress(int secondaryProgress) {
            mSecondaryProgress = Math.max(0, secondaryProgress);
            return this;
        }

        public ProgressDialog build() {
            ProgressDialog dialog = new ProgressDialog();
            dialog.mTitle = mTitle;
            dialog.mMax = mMax;
            dialog.mProgress = mProgress;
            dialog.mSecondaryProgress = mSecondaryProgress;
            return dialog;
        }
    }
}
