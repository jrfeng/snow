package snow.music.activity.setting;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import java.util.Objects;

import snow.music.R;
import snow.music.dialog.MessageDialog;
import snow.music.service.AppPlayerService;
import snow.music.util.CheckGroup;
import snow.music.util.NightModeUtil;
import snow.music.util.PlayerUtil;
import snow.player.lifecycle.PlayerViewModel;

public class SettingActivity extends AppCompatActivity {
    private static final int DARK_MODE_ID_FOLLOW_SYSTEM = 1;
    private static final int DARK_MODE_ID_OFF = 2;
    private static final int DARK_MODE_ID_ON = 3;

    private SettingViewModel mSettingViewModel;

    private View itemFollowSystem;
    private View itemDarkModeOff;
    private View itemDarkModeOn;

    private View itemPlayWithOtherApp;
    private SwitchCompat swPlayWithOtherApp;

    private CheckGroup mCheckGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        initViewModel();
        findViews();
        initViews();
        addClickListener();
    }

    private void initViewModel() {
        ViewModelProvider provider = new ViewModelProvider(this);

        PlayerViewModel playerViewModel = provider.get(PlayerViewModel.class);
        PlayerUtil.initPlayerViewModel(this, playerViewModel, AppPlayerService.class);

        mSettingViewModel = provider.get(SettingViewModel.class);
        mSettingViewModel.init(playerViewModel);
    }

    private void findViews() {
        itemFollowSystem = findViewById(R.id.itemFollowSystem);
        itemDarkModeOff = findViewById(R.id.itemDarkModeOff);
        itemDarkModeOn = findViewById(R.id.itemDarkModeOn);

        itemPlayWithOtherApp = findViewById(R.id.itemPlayWithOtherApp);
        swPlayWithOtherApp = findViewById(R.id.swPlayWithOtherApp);
    }

    private void initViews() {
        mCheckGroup = new CheckGroup();

        DarkModeItem followSystem = new DarkModeItem(DARK_MODE_ID_FOLLOW_SYSTEM, itemFollowSystem);
        DarkModeItem darkModeOff = new DarkModeItem(DARK_MODE_ID_OFF, itemDarkModeOff);
        DarkModeItem darkModeOn = new DarkModeItem(DARK_MODE_ID_ON, itemDarkModeOn);

        mCheckGroup.addItem(followSystem);
        mCheckGroup.addItem(darkModeOff);
        mCheckGroup.addItem(darkModeOn);

        mSettingViewModel.getNightMode()
                .observe(this, mode -> {
                    switch (mode) {
                        case NIGHT_FOLLOW_SYSTEM:
                            mCheckGroup.setChecked(DARK_MODE_ID_FOLLOW_SYSTEM);
                            break;
                        case NIGHT_NO:
                            mCheckGroup.setChecked(DARK_MODE_ID_OFF);
                            break;
                        case NIGHT_YES:
                            mCheckGroup.setChecked(DARK_MODE_ID_ON);
                            break;
                    }
                });

        Boolean value = mSettingViewModel.getPlayWithOtherApp().getValue();
        swPlayWithOtherApp.setChecked(Objects.requireNonNull(value));
    }

    private void addClickListener() {
        itemFollowSystem.setOnClickListener(v -> mCheckGroup.setChecked(DARK_MODE_ID_FOLLOW_SYSTEM));
        itemDarkModeOff.setOnClickListener(v -> mCheckGroup.setChecked(DARK_MODE_ID_OFF));
        itemDarkModeOn.setOnClickListener(v -> mCheckGroup.setChecked(DARK_MODE_ID_ON));

        mCheckGroup.setOnCheckedItemChangeListener(checkedItemId -> {
            switch (checkedItemId) {
                case DARK_MODE_ID_FOLLOW_SYSTEM:
                    mSettingViewModel.setNightMode(NightModeUtil.Mode.NIGHT_FOLLOW_SYSTEM);
                    break;
                case DARK_MODE_ID_OFF:
                    mSettingViewModel.setNightMode(NightModeUtil.Mode.NIGHT_NO);
                    break;
                case DARK_MODE_ID_ON:
                    mSettingViewModel.setNightMode(NightModeUtil.Mode.NIGHT_YES);
                    break;
                default:
                    break;
            }
        });

        itemPlayWithOtherApp.setOnClickListener(v -> swPlayWithOtherApp.toggle());
        swPlayWithOtherApp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showPlayWithOtherAppTipsDialog();
                return;
            }

            mSettingViewModel.setPlayWithOtherApp(false);
        });
    }

    private void showPlayWithOtherAppTipsDialog() {
        MessageDialog dialog = new MessageDialog.Builder(this)
                .setMessage(R.string.description_play_with_other_app)
                .setNegativeButtonClickListener((dialog1, which) -> swPlayWithOtherApp.setChecked(false))
                .setPositiveButtonClickListener((dialog1, which) -> mSettingViewModel.setPlayWithOtherApp(true))
                .build();

        dialog.setCancelable(false);

        dialog.show(getSupportFragmentManager(), "PlayWithOtherAppTips");
    }

    public void finishSelf(View view) {
        finish();
    }

    private static class DarkModeItem extends CheckGroup.CheckItem {
        private ImageView ivChecked;

        public DarkModeItem(int id, View itemView) {
            super(id);
            ivChecked = itemView.findViewById(R.id.ivChecked);
        }

        @Override
        public void onChecked() {
            ivChecked.setVisibility(View.VISIBLE);
        }

        @Override
        public void onUnchecked() {
            ivChecked.setVisibility(View.GONE);
        }
    }
}