package snow.music.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDialog;

import com.google.common.base.Preconditions;

import java.util.Objects;

import snow.music.R;

public class MessageDialog extends BottomDialog {
    private String mTitle;
    private String mMessage;
    private String mPositiveButtonText;
    private String mNegativeButtonText;
    private int mPositiveTextColor;
    private int mNegativeTextColor;
    private DialogInterface.OnClickListener mPositiveButtonClickListener;
    private DialogInterface.OnClickListener mNegativeButtonClickListener;

    @Override
    protected void onInitDialog(AppCompatDialog dialog) {
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

        public Builder setPositiveButtonClickListener(@Nullable DialogInterface.OnClickListener listener) {
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

        public Builder setNegativeButtonClickListener(@Nullable DialogInterface.OnClickListener listener) {
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
