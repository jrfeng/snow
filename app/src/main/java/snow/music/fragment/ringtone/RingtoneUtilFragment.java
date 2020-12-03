package snow.music.fragment.ringtone;

import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.base.Preconditions;

import snow.music.R;
import snow.music.dialog.MessageDialog;
import snow.music.store.Music;

public class RingtoneUtilFragment extends Fragment {
    private static final int REQUEST_CODE_WRITE_SETTINGS = 1;

    private Context mContext;
    private RingtoneViewModel mRingtoneViewModel;

    @Nullable
    private Music mMusic;

    public static void setAsRingtone(@NonNull FragmentManager fm, @NonNull Music music) {
        Preconditions.checkNotNull(fm);
        Preconditions.checkNotNull(music);

        RingtoneUtilFragment fragment = new RingtoneUtilFragment();

        fragment.mMusic = music;

        fm.beginTransaction()
                .add(fragment, "setAsRingtone")
                .commit();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;

        ViewModelProvider provider = new ViewModelProvider(this);
        mRingtoneViewModel = provider.get(RingtoneViewModel.class);

        if (mMusic != null) {
            mRingtoneViewModel.setRingtoneMusic(mMusic);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAsRingtone(mRingtoneViewModel.getRingtoneMusic());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_CODE_WRITE_SETTINGS) {
            return;
        }

        Music ringtoneMusic = mRingtoneViewModel.getRingtoneMusic();
        mRingtoneViewModel.setRingtoneMusic(null);

        if (ringtoneMusic == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        if (checkSetRingtonePermission()) {
            setAsRingtone(ringtoneMusic);
        } else {
            Toast.makeText(mContext, R.string.toast_request_permission_failed, Toast.LENGTH_SHORT).show();
            removeSelf();
        }
    }

    private void setAsRingtone(@NonNull Music music) {
        Preconditions.checkNotNull(music);

        if (checkSetRingtonePermission()) {
            showSetAsRingtoneDialog(music);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mRingtoneViewModel.setRingtoneMusic(music);
            requestSetRingtonePermission();
        }
    }

    private boolean checkSetRingtonePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.System.canWrite(mContext);
        }
        return true;
    }

    private void showSetAsRingtoneDialog(Music music) {
        MessageDialog dialog = new MessageDialog.Builder(mContext)
                .setTitle(music.getTitle())
                .setMessage(R.string.message_set_as_ringtone)
                .setNegativeButtonClickListener((dialog1, which) -> removeSelf())
                .setPositiveButtonClickListener(((dialog1, which) -> {
                    RingtoneManager.setActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE, Uri.parse(music.getUri()));
                    Toast.makeText(mContext, R.string.toast_set_successfully, Toast.LENGTH_SHORT).show();
                    removeSelf();
                }))
                .build();

        dialog.show(getParentFragmentManager(), "setAsRingtoneDialog");
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private void requestSetRingtonePermission() {
        MessageDialog messageDialog = new MessageDialog.Builder(mContext)
                .setMessage(R.string.message_need_write_settings_permission)
                .setNegativeButtonClickListener((dialog1, which) -> removeSelf())
                .setPositiveButton(R.string.positive_text_request, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + mContext.getApplicationContext().getPackageName()));
                    startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS);
                })
                .build();

        messageDialog.show(getParentFragmentManager(), "requestSetRingtonePermission");
    }

    private void removeSelf() {
        getParentFragmentManager().beginTransaction()
                .remove(this)
                .commit();
    }
}
