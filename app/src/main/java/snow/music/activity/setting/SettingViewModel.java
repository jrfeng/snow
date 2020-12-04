package snow.music.activity.setting;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.base.Preconditions;

import android.app.Application;
import snow.music.util.NightModeUtil;
import snow.player.lifecycle.PlayerViewModel;

public class SettingViewModel extends AndroidViewModel {
    private MutableLiveData<NightModeUtil.Mode> mNightMode;
    private MutableLiveData<Boolean> mPlayWithOtherApp;

    private PlayerViewModel mPlayerViewModel;
    private boolean mInitialized;

    public SettingViewModel(Application application) {
        super(application);

        mNightMode = new MutableLiveData<>(NightModeUtil.getNightMode(application));
        mPlayWithOtherApp = new MutableLiveData<>(false);
    }

    public void init(@NonNull PlayerViewModel playerViewModel) {
        Preconditions.checkNotNull(playerViewModel);

        if (mInitialized) {
            return;
        }

        mInitialized = true;
        mPlayerViewModel = playerViewModel;
        mPlayWithOtherApp.setValue(mPlayerViewModel.getPlayerClient().isIgnoreAudioFocus());
    }

    @NonNull
    public LiveData<NightModeUtil.Mode> getNightMode() {
        return mNightMode;
    }

    @NonNull
    public LiveData<Boolean> getPlayWithOtherApp() {
        return mPlayWithOtherApp;
    }

    public void setNightMode(@NonNull NightModeUtil.Mode mode) {
        Preconditions.checkNotNull(mode);

        if (mode == mNightMode.getValue()) {
            return;
        }

        mNightMode.setValue(mode);
        NightModeUtil.setDefaultNightMode(getApplication(), mode);
    }

    public void setPlayWithOtherApp(boolean playWithOtherApp) {
        Boolean value = mPlayWithOtherApp.getValue();
        assert value != null;

        if (value == playWithOtherApp) {
            return;
        }

        mPlayWithOtherApp.setValue(playWithOtherApp);
        mPlayerViewModel.getPlayerClient().setIgnoreAudioFocus(playWithOtherApp);
    }
}
