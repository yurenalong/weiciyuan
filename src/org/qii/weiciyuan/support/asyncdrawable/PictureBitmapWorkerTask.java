package org.qii.weiciyuan.support.asyncdrawable;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.Display;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import org.qii.weiciyuan.R;
import org.qii.weiciyuan.support.file.FileLocationMethod;
import org.qii.weiciyuan.support.imagetool.ImageTool;
import org.qii.weiciyuan.support.lib.MyAsyncTask;
import org.qii.weiciyuan.support.utils.GlobalContext;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: Jiang Qi
 * Date: 12-8-3
 */
public class PictureBitmapWorkerTask extends MyAsyncTask<String, Void, Bitmap> {


    private LruCache<String, Bitmap> lruCache;
    private String data = "";
    private final List<WeakReference<ImageView>> viewList = new ArrayList<WeakReference<ImageView>>();

    private Map<String, PictureBitmapWorkerTask> taskMap;

    private Activity activity;

    private FileLocationMethod method;

    int reqWidth;
    int reqHeight;

    public String getUrl() {
        return data;
    }

    public PictureBitmapWorkerTask(Map<String, PictureBitmapWorkerTask> taskMap,
                                   ImageView view, String url, Activity activity, FileLocationMethod method) {

        this.lruCache = GlobalContext.getInstance().getAvatarCache();
        this.taskMap = taskMap;
        viewList.add(new WeakReference<ImageView>(view));
        this.data = url;
        this.activity = activity;
        this.method = method;

    }

    public void addView(ImageView view) {
        viewList.add(new WeakReference<ImageView>(view));
    }


    @Override
    protected Bitmap doInBackground(String... url) {

        if (!isCancelled()) {
            switch (method) {

                case picture_thumbnail:
                    return ImageTool.getThumbnailPictureWithRoundedCorner(data);

                case picture_bmiddle:
                    DisplayMetrics metrics = new DisplayMetrics();
                    Display display = activity.getWindowManager().getDefaultDisplay();
                    display.getMetrics(metrics);
                    float reSize = activity.getResources().getDisplayMetrics().density;
                    //because height is 80dp
                    int height = activity.getResources().getDimensionPixelSize(R.dimen.timeline_big_avatar_height);
                    //8 is  layout padding
                    int width = (int) (metrics.widthPixels - (8 + 8) * reSize);

                    return ImageTool.getMiddlePictureInTimeLine(data, width, height, null);

            }
        }
        return null;
    }

    @Override
    protected void onCancelled(Bitmap bitmap) {
        clean();
        super.onCancelled(bitmap);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {

        super.onPostExecute(bitmap);
        for (WeakReference<ImageView> view : viewList) {
            if (view != null && view.get() != null) {
                if (canDisplay(view)) {
                    if (bitmap != null) {
                        playImageViewAnimation(view, bitmap);
                        lruCache.put(data, bitmap);
                    } else {
                        view.get().setImageDrawable(new ColorDrawable(Color.TRANSPARENT));
                    }
                }
            }

        }

        clean();
    }

    private boolean canDisplay(WeakReference<ImageView> view) {
        if (view != null && view.get() != null) {
            ImageView imageView = view.get();
            PictureBitmapWorkerTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
            if (this == bitmapDownloaderTask) {
                return true;
            }
        }
        return false;
    }

    private static PictureBitmapWorkerTask getBitmapDownloaderTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof PictureBitmapDrawable) {
                PictureBitmapDrawable downloadedDrawable = (PictureBitmapDrawable) drawable;
                return downloadedDrawable.getBitmapDownloaderTask();
            }
        }
        return null;
    }

    private void playImageViewAnimation(final WeakReference<ImageView> view, final Bitmap bitmap) {
        final Animation anim_out = AnimationUtils.loadAnimation(activity, R.anim.timeline_pic_fade_out);
        final Animation anim_in = AnimationUtils.loadAnimation(activity, R.anim.timeline_pic_fade_in);

        anim_out.setAnimationListener(new Animation.AnimationListener() {
            //setTag at animation start time
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {

                anim_in.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }
                });

                if (canDisplay(view)) {
                    view.get().setImageBitmap(bitmap);
                    view.get().startAnimation(anim_in);
                }
            }
        });
        if (view.get().getAnimation() == null || view.get().getAnimation().hasEnded())
            view.get().startAnimation(anim_out);
    }

    private void clean() {
        if (taskMap != null && taskMap.get(data) != null) {
            taskMap.remove(data);
        }
        viewList.clear();
        activity = null;
        taskMap = null;
        lruCache = null;
    }
}
