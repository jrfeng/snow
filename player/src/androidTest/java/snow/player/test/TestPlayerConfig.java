package snow.player.test;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import snow.player.Player;
import snow.player.PlayerConfig;

public class TestPlayerConfig extends PlayerConfig {
    private Player.SoundQuality mSoundQuality;
    private Bundle mAudioEffectConfig;
    private boolean mAudioEffectEnabled;
    private boolean mOnlyWifiNetwork;
    private boolean mIgnoreLossAudioFocus;

    public TestPlayerConfig(@NonNull Context context, @NonNull String id) {
        super(context, id);
    }

    @NonNull
    @Override
    public Player.SoundQuality getSoundQuality() {
        return mSoundQuality;
    }

    @Override
    public void setSoundQuality(@NonNull Player.SoundQuality soundQuality) {
        mSoundQuality = soundQuality;
    }

    @NonNull
    @Override
    public Bundle getAudioEffectConfig() {
        return mAudioEffectConfig;
    }

    @Override
    public void setAudioEffectConfig(@NonNull Bundle audioEffectConfig) {
        mAudioEffectConfig = audioEffectConfig;
    }

    @Override
    public boolean isAudioEffectEnabled() {
        return mAudioEffectEnabled;
    }

    @Override
    public void setAudioEffectEnabled(boolean audioEffectEnabled) {
        mAudioEffectEnabled = audioEffectEnabled;
    }

    @Override
    public boolean isOnlyWifiNetwork() {
        return mOnlyWifiNetwork;
    }

    @Override
    public void setOnlyWifiNetwork(boolean onlyWifiNetwork) {
        mOnlyWifiNetwork = onlyWifiNetwork;
    }

    @Override
    public boolean isIgnoreLossAudioFocus() {
        return mIgnoreLossAudioFocus;
    }

    @Override
    public void setIgnoreLossAudioFocus(boolean ignoreLossAudioFocus) {
        mIgnoreLossAudioFocus = ignoreLossAudioFocus;
    }
}
