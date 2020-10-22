package snow.music.activity.navigation;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import snow.music.R;
import snow.music.databinding.ActivityNavigationBinding;
import snow.player.PlayerClient;
import snow.player.PlayerService;
import snow.player.lifecycle.PlayerViewModel;

public class NavigationActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityNavigationBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_navigation);

        ViewModelProvider viewModelProvider = new ViewModelProvider(this);
        PlayerViewModel playerViewModel = viewModelProvider.get(PlayerViewModel.class);

        initPlayerViewModel(playerViewModel);
        initDiskPanel(binding.rvDiskPanel, playerViewModel);

        binding.setPlayerViewModel(playerViewModel);
    }

    private void initDiskPanel(RecyclerView diskPanel, PlayerViewModel playerViewModel) {
        diskPanel.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        diskPanel.setAdapter(new DiskPanelAdapter(playerViewModel));

        PagerSnapHelper pagerSnapHelper = new PagerSnapHelper();
        pagerSnapHelper.attachToRecyclerView(diskPanel);
    }

    private void initPlayerViewModel(PlayerViewModel playerViewModel) {
        if (playerViewModel.isInitialized()) {
            return;
        }

        PlayerClient playerClient = PlayerClient.newInstance(this, PlayerService.class);
        playerViewModel.init(this, playerClient);
        playerClient.connect();

        playerViewModel.setAutoDisconnect(true);
    }
}