package snow.music.util;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

/**
 * 列表项拖拽回调。
 */
public class ItemDragCallback extends ItemTouchHelper.Callback {
    private boolean mDragging = false;
    private int mFromPosition = -1;

    private final OnDragCallback mCallback;

    public ItemDragCallback(@NonNull OnDragCallback callback) {
        Objects.requireNonNull(callback);
        mCallback = callback;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        if (!mDragging) {
            mDragging = true;
            mFromPosition = viewHolder.getBindingAdapterPosition();
        }

        mCallback.onDragging(viewHolder.getBindingAdapterPosition(), target.getBindingAdapterPosition());

        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // ignore
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        if (mFromPosition == -1) {
            return;
        }

        int fromPosition = mFromPosition;
        int toPosition = viewHolder.getBindingAdapterPosition();

        clearDraggingState();
        if (toPosition == fromPosition) {
            return;
        }

        mCallback.onDragComplete(fromPosition, toPosition);
    }

    private void clearDraggingState() {
        mDragging = false;
        mFromPosition = -1;
    }

    public interface OnDragCallback {
        void onDragging(int from, int target);

        void onDragComplete(int fromPosition, int toPosition);
    }
}
