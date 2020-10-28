package snow.music.dialog.playlist;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import snow.music.R;
import snow.music.util.DialogUtil;
import snow.player.PlayerClient;
import snow.player.PlayerService;
import snow.player.lifecycle.PlayerViewModel;

public class PlaylistDialog extends AppCompatDialogFragment {
    private Context mContext;
    private PlayerViewModel mPlayerViewModel;

    public static PlaylistDialog newInstance() {
        return new PlaylistDialog();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;

        ViewModelProvider provider = new ViewModelProvider(this);
        mPlayerViewModel = provider.get(PlayerViewModel.class);

        initPlayerViewModel();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AppCompatDialog dialog = new AppCompatDialog(getContext(), getTheme());

        DialogUtil.setWith(dialog, WindowManager.LayoutParams.MATCH_PARENT);
        DialogUtil.setGravity(dialog, Gravity.BOTTOM);
        DialogUtil.setBackgroundDrawableResource(dialog, R.drawable.bg_playlist);
        DialogUtil.setAnimations(dialog, R.style.PlaylistTransition);

        dialog.setContentView(R.layout.dialog_playlist);
        RecyclerView recyclerView = dialog.findViewById(R.id.rvPlaylist);
        if (recyclerView == null) {
            throw new IllegalStateException("RecyclerView is null.");
        }
        dialog.setCanceledOnTouchOutside(true);

        initPlaylist(recyclerView);

        return dialog;
    }

    private void initPlayerViewModel() {
        if (mPlayerViewModel.isInitialized()) {
            return;
        }

        PlayerClient playerClient = PlayerClient.newInstance(mContext, PlayerService.class);
        playerClient.setAutoConnect(true);
        playerClient.connect();

        mPlayerViewModel.init(mContext, playerClient);
        mPlayerViewModel.setAutoDisconnect(true);
    }

    private void initPlaylist(RecyclerView rvPlaylist) {
        rvPlaylist.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        rvPlaylist.setAdapter(new PlaylistAdapter(this, mPlayerViewModel));
    }
}
