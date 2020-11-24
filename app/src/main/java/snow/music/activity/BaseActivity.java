package snow.music.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import snow.player.PlayerClient;

public class BaseActivity extends AppCompatActivity {
    @Nullable
    private PlayerClient mPlayerClient;

    public void setPlayerClient(@Nullable PlayerClient playerClient) {
        mPlayerClient = playerClient;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mPlayerClient != null && !mPlayerClient.isConnected()) {
            mPlayerClient.connect();
        }
    }
}
