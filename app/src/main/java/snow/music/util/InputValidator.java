package snow.music.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import snow.music.R;
import snow.music.dialog.InputDialog;
import snow.music.store.MusicStore;

public class InputValidator implements InputDialog.Validator {
    private Context mContext;
    private String mInvalidateHint;

    public InputValidator(Context context) {
        mContext = context;
    }

    @Override
    public boolean isValid(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            mInvalidateHint = mContext.getString(R.string.hint_please_input_music_list_title);
            return false;
        }

        if (MusicStore.getInstance().isNameExists(input)) {
            mInvalidateHint = mContext.getString(R.string.hint_music_list_name_exists);
            return false;
        }

        return true;
    }

    @NonNull
    @Override
    public String getInvalidateHint() {
        return mInvalidateHint;
    }
}
