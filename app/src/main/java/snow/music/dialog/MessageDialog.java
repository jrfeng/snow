package snow.music.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.google.common.base.Preconditions;

import java.util.Objects;

import snow.music.R;
import snow.music.util.DialogUtil;

public class MessageDialog extends AppCompatDialogFragment {
    private String mTitle;
    private String mMessage;
    private String mPositiveButtonText;
    private String mNegativeButtonText;
    private int mPositiveTextColor;
    private int mNegativeTextColor;
    private DialogInterface.OnClickListener mPositiveButtonClickListener;
    private DialogInterface.OnClickListener mNegativeButtonClickListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AppCompatDialog dialog = new AppCompatDialog(getContext(), getTheme());

        DialogUtil.setWith(dialog, WindowManager.LayoutParams.MATCH_PARENT);
        DialogUtil.setGravity(dialog, Gravity.BOTTOM);
        DialogUtil.setBackgroundDrawableResource(dialog, R.drawable.bg_playlist);
        DialogUtil.setAnimations(dialog, R.style.PlaylistTransition);
        dialog.setCanceledOnTouchOutside(true);

        dialog.setContentView(R.layout.dialog_message);

        TextView tvDialogTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvMessage);
        Button btnPositive = dialog.findViewById(R.id.btnPositive);
        Button btnNegative = dialog.findViewById(R.id.btnNegative);

        Objects.requireNonNull(tvDialogTitle);
        Objects.requireNonNull(tvMessage);
        Objects.requireNonNull(btnPositive);
        Objects.requireNonNull(btnNegative);

        tvDialogTitle.setText(mTitle);
        tvMessage.setText(mMessage);
        btnNegative.setText(mNegativeButtonText);
        btnPositive.setText(mPositiveButtonText);
        btnNegative.setTextColor(mNegativeTextColor);
        btnPositive.setTextColor(mPositiveTextColor);

        btnNegative.setOnClickListener(v -> {
            if (mNegativeButtonClickListener != null) {
                mNegativeButtonClickListener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
            }
            dismiss();
        });

        btnPositive.setOnClickListener(v -> {
            if (mPositiveButtonClickListener != null) {
                mPositiveButtonClickListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
            }
            dismiss();
        });

        return dialog;
    }

    public static class Builder {
        private Context mContext;
        private String mTitle;
        private String mMessage;
        private String mPositiveButtonText;
        private String mNegativeButtonText;
        private int mPositiveTextColor;
        private int mNegativeTextColor;
        private DialogInterface.OnClickListener mPositiveButtonClickListener;
        private DialogInterface.OnClickListener mNegativeButtonClickListener;

        public Builder(@NonNull Context context) {
            Preconditions.checkNotNull(context);
            mContext = context;

            mTitle = context.getString(R.string.message_dialog_default_title);
            mMessage = "";
            mPositiveButtonText = context.getString(R.string.message_dialog_positive_text);
            mNegativeButtonText = context.getString(R.string.message_dialog_negative_text);
            mPositiveTextColor = context.getResources().getColor(R.color.colorPrimary);
            mNegativeTextColor = context.getResources().getColor(R.color.colorText);
        }

        public Builder setTitle(@NonNull String title) {
            Preconditions.checkNotNull(title);
            mTitle = title;
            return this;
        }

        public Builder setTitle(@StringRes int titleId) {
            mTitle = mContext.getString(titleId);
            return this;
        }

        public Builder setMessage(@NonNull String message) {
            Preconditions.checkNotNull(message);
            mMessage = message;
            return this;
        }

        public Builder setMessage(@StringRes int messageId) {
            mMessage = mContext.getString(messageId);
            return this;
        }

        public Builder setPositiveButton(@NonNull String text, @Nullable DialogInterface.OnClickListener listener) {
            mPositiveButtonText = text;
            mPositiveButtonClickListener = listener;
            return this;
        }

        public Builder setPositiveButton(@StringRes int textId, @Nullable DialogInterface.OnClickListener listener) {
            mPositiveButtonText = mContext.getString(textId);
            mPositiveButtonClickListener = listener;
            return this;
        }

        public Builder setNegativeButton(@NonNull String text, @Nullable DialogInterface.OnClickListener listener) {
            mNegativeButtonText = text;
            mNegativeButtonClickListener = listener;
            return this;
        }

        public Builder setNegativeButton(@StringRes int textId, @Nullable DialogInterface.OnClickListener listener) {
            mNegativeButtonText = mContext.getString(textId);
            mNegativeButtonClickListener = listener;
            return this;
        }

        public Builder setPositiveTextColor(int color) {
            mPositiveTextColor = color;
            return this;
        }

        public Builder setNegativeTextColor(int color) {
            mNegativeTextColor = color;
            return this;
        }

        public MessageDialog build() {
            MessageDialog dialog = new MessageDialog();

            dialog.mTitle = mTitle;
            dialog.mMessage = mMessage;
            dialog.mPositiveButtonText = mPositiveButtonText;
            dialog.mNegativeButtonText = mNegativeButtonText;
            dialog.mPositiveTextColor = mPositiveTextColor;
            dialog.mNegativeTextColor = mNegativeTextColor;
            dialog.mPositiveButtonClickListener = mPositiveButtonClickListener;
            dialog.mNegativeButtonClickListener = mNegativeButtonClickListener;

            return dialog;
        }
    }
}
