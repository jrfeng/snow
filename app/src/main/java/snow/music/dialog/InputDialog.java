package snow.music.dialog;

import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDialog;

import com.google.common.base.Preconditions;

import snow.music.R;

public class InputDialog extends BottomDialog {
    private String mTitle;
    private String mHint;
    private Validator mValidator;
    private OnInputConfirmListener mInputConfirmListener;

    @Override
    protected void onInitDialog(AppCompatDialog dialog) {
        dialog.setContentView(R.layout.dialog_input);

        TextView tvDialogTitle = dialog.findViewById(R.id.tvDialogTitle);
        EditText etInput = dialog.findViewById(R.id.etInput);
        Button btnNegative = dialog.findViewById(R.id.btnNegative);
        Button btnPositive = dialog.findViewById(R.id.btnPositive);

        assert tvDialogTitle != null;
        assert etInput != null;
        assert btnNegative != null;
        assert btnPositive != null;

        tvDialogTitle.setText(mTitle);
        etInput.setHint(mHint);
        etInput.requestFocus();

        btnNegative.setOnClickListener(view -> dismiss());

        btnPositive.setOnClickListener(view -> {
            String input = etInput.getText().toString();
            if (mValidator.isValid(input)) {
                dismiss();
                mInputConfirmListener.onInputConfirmed(input);
                return;
            }

            Toast.makeText(getContext(), mValidator.getInvalidateHint(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected boolean keepOnRestarted() {
        return false;
    }

    public static class Builder {
        private Context mContext;

        private String mTitle;
        private String mHint;
        private Validator mValidator;
        private OnInputConfirmListener mInputConfirmListener;

        public Builder(Context context) {
            mContext = context;
            mTitle = context.getString(R.string.input_default_title);
            mHint = context.getString(R.string.input_default_hint);
        }

        public Builder setTitle(@StringRes int resId) {
            mTitle = mContext.getString(resId);
            return this;
        }

        public Builder setTitle(@NonNull String title) {
            Preconditions.checkNotNull(title);

            mTitle = title;
            return this;
        }

        public Builder setHint(@StringRes int resId) {
            mHint = mContext.getString(resId);
            return this;
        }

        public Builder setHint(@NonNull String hint) {
            Preconditions.checkNotNull(hint);

            mHint = hint;
            return this;
        }

        /**
         * 设置 “确认” 按钮点击监听器。
         *
         * @param validator 输入内容检查器，检测输入内容是否合法，不能为 null
         * @param listener  该监听器会在 “确认” 按钮被点击，且输入内容合法时被调用，不能为 null
         */
        public Builder setOnInputConfirmListener(@NonNull Validator validator, @NonNull OnInputConfirmListener listener) {
            Preconditions.checkNotNull(validator);
            Preconditions.checkNotNull(listener);

            mValidator = validator;
            mInputConfirmListener = listener;
            return this;
        }

        public InputDialog build() {
            InputDialog dialog = new InputDialog();

            dialog.mTitle = mTitle;
            dialog.mHint = mHint;
            dialog.mValidator = mValidator;
            dialog.mInputConfirmListener = mInputConfirmListener;

            return dialog;
        }
    }

    /**
     * 输入内容检查器。
     */
    public interface Validator {
        /**
         * 检查输入内容是否合法。
         *
         * @param input 输入内容，可能为 null
         * @return 如果输入内容合法，则返回 true，否则返回 false
         */
        boolean isValid(@Nullable String input);

        /**
         * 当输入内容不合法时会调用该方法来获取一个提示信息。
         *
         * @return 输入内容不合法时的提示信息
         */
        @NonNull
        String getInvalidateHint();
    }

    /**
     * 当 “确认” 按钮被点击，且输入内容通过 {@link Validator} 检查器的检查时会回调该接口
     */
    public interface OnInputConfirmListener {

        /**
         * 该方法会在 “确认” 按钮被点击，且通过 {@link Validator} 校验器的检查时调用。
         *
         * @param input 输入内容，可能为 null
         */
        void onInputConfirmed(@Nullable String input);
    }
}
