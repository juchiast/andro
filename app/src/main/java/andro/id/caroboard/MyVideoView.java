package andro.id.caroboard;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.TextView;
import android.widget.VideoView;

/**
 * Created by qcuong98 on 11/10/18.
 */
public class MyVideoView extends VideoView {
    int widthVideo;
    int heightVideo;

    public MyVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public MyVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    public MyVideoView(Context context) {
        super(context);
    }

    public void rotate(double radian) {
        int lenVideo = this.getDuration();
        int delta = (int)(radian / (2 * Math.PI)) * lenVideo;
        delta %= lenVideo;
        if (delta < 0)
            delta += lenVideo;

        this.seekTo((this.getCurrentPosition() + delta) % this.getDuration());
    }

    @Override
    public void setVideoURI(Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(this.getContext(), uri);
        widthVideo = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        heightVideo = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        super.setVideoURI(uri);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(widthVideo, widthMeasureSpec);
        int height = getDefaultSize(heightVideo, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }
}
