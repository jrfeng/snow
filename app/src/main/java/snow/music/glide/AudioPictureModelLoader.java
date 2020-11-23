package snow.music.glide;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.signature.ObjectKey;
import com.google.common.base.Preconditions;

import java.io.File;
import java.nio.ByteBuffer;

public class AudioPictureModelLoader implements ModelLoader<String, ByteBuffer> {
    private final Context mContext;

    public AudioPictureModelLoader(@NonNull Context context) {
        Preconditions.checkNotNull(context);
        mContext = context.getApplicationContext();
    }

    @Nullable
    @Override
    public LoadData<ByteBuffer> buildLoadData(@NonNull String model, int width, int height, @NonNull Options options) {
        ObjectKey key = new ObjectKey(model);
        DataFetcher<ByteBuffer> fetcher = new AudioPictureDataFeather(mContext, model);

        return new ModelLoader.LoadData<>(key, fetcher);
    }

    @Override
    public boolean handles(@NonNull String model) {
        // 是否是 content uri, file uri, 文件路径
        Uri uri = Uri.parse(model);
        File file = new File(model);

        String scheme = uri.getScheme();
        return "content".equalsIgnoreCase(scheme)
                || "file".equalsIgnoreCase(scheme)
                || file.isFile();
    }

    public static class AudioPictureDataFeather implements DataFetcher<ByteBuffer> {
        private final Context mContext;
        private final String mPath;
        private final MediaMetadataRetriever mMediaMetadataRetriever;

        public AudioPictureDataFeather(@NonNull Context context, @NonNull String path) {
            Preconditions.checkNotNull(context);
            Preconditions.checkNotNull(path);

            mContext = context.getApplicationContext();
            mPath = path;
            mMediaMetadataRetriever = new MediaMetadataRetriever();
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super ByteBuffer> callback) {
            try {
                mMediaMetadataRetriever.setDataSource(mContext, Uri.parse(mPath));
                byte[] data = mMediaMetadataRetriever.getEmbeddedPicture();
                if (data == null) {
                    callback.onLoadFailed(new Exception("not find embedded picture."));
                    return;
                }
                callback.onDataReady(ByteBuffer.wrap(data));
            } catch (Exception e) {
                callback.onLoadFailed(e);
            }

        }

        @Override
        public void cleanup() {
            mMediaMetadataRetriever.release();
        }

        @Override
        public void cancel() {
            // 忽略, 因为 MediaMetadataRetriever 获取音频文件内嵌图片的操作是不可取消的
        }

        @NonNull
        @Override
        public Class<ByteBuffer> getDataClass() {
            return ByteBuffer.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.LOCAL;
        }
    }

    public static class Factory implements ModelLoaderFactory<String, ByteBuffer> {
        private final Context mContext;

        public Factory(Context context) {
            mContext = context.getApplicationContext();
        }

        @NonNull
        @Override
        public ModelLoader<String, ByteBuffer> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new AudioPictureModelLoader(mContext);
        }

        @Override
        public void teardown() {
            // ignore
        }
    }
}