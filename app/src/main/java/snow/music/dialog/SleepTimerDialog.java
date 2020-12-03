package snow.music.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import snow.music.R;
import snow.music.service.AppPlayerService;
import snow.music.util.PlayerUtil;
import snow.player.lifecycle.PlayerViewModel;

public class SleepTimerDialog extends BottomDialog {
    private SleepTimerView mSleepTimerView;

    public static SleepTimerDialog newInstance() {
        return new SleepTimerDialog();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentActivity activity = Objects.requireNonNull(getActivity());

        ViewModelProvider provider = new ViewModelProvider(activity);
        PlayerViewModel playerViewModel = provider.get(PlayerViewModel.class);
        PlayerUtil.initPlayerViewModel(activity, playerViewModel, AppPlayerService.class);

        mSleepTimerView = getSleepTimerView(playerViewModel.getPlayerClient().isSleepTimerStarted());
        mSleepTimerView.init(this, playerViewModel);
    }

    private SleepTimerView getSleepTimerView(boolean started) {
        if (started) {
            return new RunningView();
        }

        return new SettingView();
    }

    @Override
    protected void onInitDialog(AppCompatDialog dialog) {
        mSleepTimerView.onCreateDialog(dialog);
    }

    @Override
    protected boolean keepOnRestarted() {
        return false;
    }

    private static abstract class SleepTimerView {
        private DialogFragment mFragment;
        private PlayerViewModel mPlayerViewModel;

        public void init(DialogFragment fragment, PlayerViewModel playerViewModel) {
            mFragment = fragment;
            mPlayerViewModel = playerViewModel;
        }

        public DialogFragment getFragment() {
            return mFragment;
        }

        public Context getContext() {
            return getFragment().getContext();
        }

        public void dismiss() {
            mFragment.dismiss();
        }

        public final PlayerViewModel getPlayerViewModel() {
            return mPlayerViewModel;
        }

        public abstract void onCreateDialog(AppCompatDialog dialog);
    }

    private static class SettingView extends SleepTimerView {
        private static final String KEY_CHECKED_ITEM_ID = "CHECKED_ITEM_ID";
        private static final String KEY_SEEK_BAR_PROGRESS = "SEEK_BAR_PROGRESS";
        private static final String PREFERENCE_NAME = "SleepTimerSetting";

        private SharedPreferences mSettingPreferences;

        private View item5Minutes;
        private View item10Minutes;
        private View item15Minutes;
        private View item30Minutes;
        private View item40Minutes;
        private View item60Minutes;
        private View itemCustomMinutes;

        private Button btnNegative;
        private Button btnPositive;

        private MinutesItemGroup mMinutesItemGroup;

        @Override
        public void onCreateDialog(AppCompatDialog dialog) {
            dialog.setContentView(R.layout.dialog_sleep_timer_setting);

            mSettingPreferences = getContext().getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);

            findViews(dialog);
            initViews();
            addClickListener();
        }

        private void findViews(AppCompatDialog dialog) {
            item5Minutes = dialog.findViewById(R.id.item5Minutes);
            item10Minutes = dialog.findViewById(R.id.item10Minutes);
            item15Minutes = dialog.findViewById(R.id.item15Minutes);
            item30Minutes = dialog.findViewById(R.id.item30Minutes);
            item40Minutes = dialog.findViewById(R.id.item40Minutes);
            item60Minutes = dialog.findViewById(R.id.item60Minutes);
            itemCustomMinutes = dialog.findViewById(R.id.itemCustomMinutes);

            btnNegative = dialog.findViewById(R.id.btnNegative);
            btnPositive = dialog.findViewById(R.id.btnPositive);
        }

        private void initViews() {
            Set<MinutesItem> minutesItemSet = new HashSet<>();

            minutesItemSet.add(new FixedMinutesItem(1, item5Minutes, 5));
            minutesItemSet.add(new FixedMinutesItem(2, item10Minutes, 10));
            minutesItemSet.add(new FixedMinutesItem(3, item15Minutes, 15));
            minutesItemSet.add(new FixedMinutesItem(4, item30Minutes, 30));
            minutesItemSet.add(new FixedMinutesItem(5, item40Minutes, 40));
            minutesItemSet.add(new FixedMinutesItem(6, item60Minutes, 60));
            minutesItemSet.add(new CustomMinutesItem(7, itemCustomMinutes, mSettingPreferences));

            mMinutesItemGroup = new MinutesItemGroup(minutesItemSet);
            mMinutesItemGroup.setChecked(getCheckItemId());

            mMinutesItemGroup.setOnCheckedItemChangeListener(checkedItemId ->
                    mSettingPreferences.edit()
                            .putInt(KEY_CHECKED_ITEM_ID, checkedItemId)
                            .apply()
            );
        }

        private void addClickListener() {
            btnNegative.setOnClickListener(v -> dismiss());
            btnPositive.setOnClickListener(v -> {
                getPlayerViewModel().startSleepTimer(mMinutesItemGroup.getMinutes() * 60_000);
                Toast.makeText(getContext(), R.string.toast_start_sleep_timer, Toast.LENGTH_SHORT).show();
                dismiss();
            });
        }

        private int getCheckItemId() {
            return mSettingPreferences.getInt(KEY_CHECKED_ITEM_ID, 1);
        }

        private static class MinutesItemGroup {
            private Set<MinutesItem> mMinutesItems;
            private int mCheckedItemId;

            @Nullable
            private OnCheckedItemChangeListener mOnCheckedItemChangeListener;

            MinutesItemGroup(Set<MinutesItem> minutesItemSet) {
                mMinutesItems = new HashSet<>(minutesItemSet);

                for (MinutesItem item : mMinutesItems) {
                    item.setGroup(this);
                }
            }

            public void setChecked(int itemId) {
                mCheckedItemId = itemId;
                notifyCheckedItemChanged(mCheckedItemId);
                for (MinutesItem item : mMinutesItems) {
                    if (item.getItemId() == itemId) {
                        item.onChecked();
                    } else {
                        item.onUnchecked();
                    }
                }
            }

            public int getMinutes() {
                for (MinutesItem item : mMinutesItems) {
                    if (item.getItemId() == mCheckedItemId) {
                        return item.getMinutes();
                    }
                }

                return 1;
            }

            public void setOnCheckedItemChangeListener(@Nullable OnCheckedItemChangeListener onCheckedItemChangeListener) {
                mOnCheckedItemChangeListener = onCheckedItemChangeListener;
            }

            private void notifyCheckedItemChanged(int checkedItemId) {
                if (mOnCheckedItemChangeListener != null) {
                    mOnCheckedItemChangeListener.onCheckedItemChanged(checkedItemId);
                }
            }

            public interface OnCheckedItemChangeListener {
                void onCheckedItemChanged(int checkedItemId);
            }
        }

        private static abstract class MinutesItem {
            @Nullable
            private MinutesItemGroup mGroup;

            private int mItemId;
            private int mCheckedTextColor;
            private int mUncheckedTextColor;

            MinutesItem(int itemId, View itemView) {
                mItemId = itemId;

                Resources res = itemView.getResources();
                mCheckedTextColor = res.getColor(R.color.colorText);
                mUncheckedTextColor = res.getColor(R.color.colorTextDisabled);
            }

            public int getItemId() {
                return mItemId;
            }

            public int getCheckedTextColor() {
                return mCheckedTextColor;
            }

            public int getUncheckedTextColor() {
                return mUncheckedTextColor;
            }

            public void setGroup(@Nullable MinutesItemGroup group) {
                mGroup = group;
            }

            public void requestChecked() {
                if (mGroup == null) {
                    return;
                }

                mGroup.setChecked(mItemId);
            }

            public abstract int getMinutes();

            public abstract void onChecked();

            public abstract void onUnchecked();
        }

        private static class FixedMinutesItem extends MinutesItem {
            private int mMinutes;
            private TextView tvMinutes;
            private ImageView ivChecked;

            FixedMinutesItem(int id, View itemView, int minutes) {
                super(id, itemView);

                mMinutes = Math.max(1, minutes);
                tvMinutes = itemView.findViewById(R.id.tvMinutes);
                ivChecked = itemView.findViewById(R.id.ivChecked);

                itemView.setOnClickListener(v -> requestChecked());
            }

            @Override
            public int getMinutes() {
                return mMinutes;
            }

            @Override
            public void onChecked() {
                tvMinutes.setTextColor(getCheckedTextColor());
                ivChecked.setVisibility(View.VISIBLE);
            }

            @Override
            public void onUnchecked() {
                tvMinutes.setTextColor(getUncheckedTextColor());
                ivChecked.setVisibility(View.GONE);
            }
        }

        private static class CustomMinutesItem extends MinutesItem {
            private TextView tvCustomMinutes;
            private TextView tvMaxMinutes;
            private SeekBar sbCustomMinutes;
            private ImageView ivChecked;

            CustomMinutesItem(int id, View itemView, SharedPreferences preferences) {
                super(id, itemView);

                tvCustomMinutes = itemView.findViewById(R.id.tvCustomMinutes);
                tvMaxMinutes = itemView.findViewById(R.id.tvMaxMinutes);
                sbCustomMinutes = itemView.findViewById(R.id.sbCustomMinutes);
                ivChecked = itemView.findViewById(R.id.ivChecked);

                sbCustomMinutes.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        tvCustomMinutes.setText(String.valueOf(getMinutes()));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        requestChecked();
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        saveSeekBarProgress(preferences, seekBar.getProgress());
                    }
                });

                tvCustomMinutes.setOnClickListener(v -> requestChecked());
                tvMaxMinutes.setOnClickListener(v -> requestChecked());

                int progress = getSeekBarProgress(preferences);
                sbCustomMinutes.setProgress(progress);
                tvCustomMinutes.setText(String.valueOf(getMinutes()));
            }

            private int getSeekBarProgress(SharedPreferences preferences) {
                return preferences.getInt(KEY_SEEK_BAR_PROGRESS, 0);
            }

            private void saveSeekBarProgress(SharedPreferences preferences, int progress) {
                preferences.edit()
                        .putInt(KEY_SEEK_BAR_PROGRESS, progress)
                        .apply();
            }

            @Override
            public int getMinutes() {
                return sbCustomMinutes.getProgress() + 1;
            }

            @Override
            public void onChecked() {
                tvCustomMinutes.setTextColor(getCheckedTextColor());
                tvMaxMinutes.setTextColor(getCheckedTextColor());
                ivChecked.setVisibility(View.VISIBLE);
            }

            @Override
            public void onUnchecked() {
                tvCustomMinutes.setTextColor(getUncheckedTextColor());
                tvMaxMinutes.setTextColor(getUncheckedTextColor());
                ivChecked.setVisibility(View.GONE);
            }
        }
    }

    private static class RunningView extends SleepTimerView {
        private TextView tvTextTimer;
        private Button btnCancelTimer;

        @Override
        public void onCreateDialog(AppCompatDialog dialog) {
            dialog.setContentView(R.layout.dialog_sleep_timer_running);

            findViews(dialog);
            initViews();
            addClickListener();
        }

        private void findViews(AppCompatDialog dialog) {
            tvTextTimer = dialog.findViewById(R.id.tvTextTimer);
            btnCancelTimer = dialog.findViewById(R.id.btnCancelTimer);
        }

        private void initViews() {
            getPlayerViewModel().getTextSleepTimerProgress()
                    .observe(getFragment(), progress -> tvTextTimer.setText(progress));

            getPlayerViewModel().getSleepTimerStarted()
                    .observe(getFragment(), started -> {
                        if (!started) {
                            dismiss();
                        }
                    });
        }

        private void addClickListener() {
            btnCancelTimer.setOnClickListener(v -> {
                getPlayerViewModel().cancelSleepTimer();
                dismiss();
                Toast.makeText(getContext(), R.string.toast_cancel_sleep_timer, Toast.LENGTH_SHORT).show();
            });
        }
    }
}
