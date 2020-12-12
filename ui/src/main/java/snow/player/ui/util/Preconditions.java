package snow.player.ui.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class Preconditions {
    private Preconditions() {
        throw new AssertionError();
    }

    @NonNull
    public static <T> T checkNotNull(@Nullable T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }
}
