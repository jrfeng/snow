package snow.music.util;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

import io.reactivex.Single;

/**
 * 获取本地音乐文件的内嵌图片。
 */
public final class EmbeddedPictureUtil {
    private EmbeddedPictureUtil() {
        throw new AssertionError();
    }

    public static Single<byte[]> getEmbeddedPicture(@NonNull Context context, @NonNull String uri) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(uri);

        return Single.create(emitter -> {
            if (uri.isEmpty()) {
                emitter.onSuccess(new byte[0]);
                return;
            }

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            try {
                retriever.setDataSource(context, Uri.parse(uri));
                byte[] picture = retriever.getEmbeddedPicture();
                retriever.release();

                if (emitter.isDisposed()) {
                    return;
                }

                if (picture == null) {
                    emitter.onSuccess(new byte[0]);
                } else {
                    emitter.onSuccess(picture);
                }

            } catch (Exception e) {
                emitter.onSuccess(new byte[0]);
            } finally {
                retriever.release();
            }
        });
    }
}
