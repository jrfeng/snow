package snow.music.fragment.ringtone;

import androidx.lifecycle.ViewModel;

import snow.music.store.Music;

public class RingtoneViewModel extends ViewModel {
    private Music mRingtoneMusic;

    public Music getRingtoneMusic() {
        return mRingtoneMusic;
    }

    public void setRingtoneMusic(Music ringtoneMusic) {
        mRingtoneMusic = ringtoneMusic;
    }
}
