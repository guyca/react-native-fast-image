package com.dylanvann.fastimage;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.target.ViewTarget;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

class FastImageViewManager extends SimpleViewManager<ImageView> {
    private static final String TAG = "FastImageViewManager";
    private static final String REACT_CLASS = "FastImageView";

    private static final String REACT_ON_LOAD_EVENT = "onFastImageLoad";

    private static final String REACT_ON_ERROR_EVENT = "onFastImageError";

    private static Drawable TRANSPARENT_DRAWABLE = new ColorDrawable(Color.TRANSPARENT);

    private static Map<String, Priority> REACT_PRIORITY_MAP =
            new HashMap<String, Priority>() {{
                put("low", Priority.LOW);
                put("normal", Priority.NORMAL);
                put("high", Priority.HIGH);
            }};

    private static Map<String, ImageView.ScaleType> REACT_RESIZE_MODE_MAP =
            new HashMap<String, ImageView.ScaleType>() {{
                put("contain", ScaleType.FIT_CENTER);
                put("cover", ScaleType.CENTER_CROP);
                put("stretch", ScaleType.FIT_XY);
                put("center", ScaleType.CENTER);
            }};

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected ImageView createViewInstance(ThemedReactContext reactContext) {
        return new ImageView(reactContext);
    }

    private static class LoadListener implements RequestListener<Drawable> {
        @Override
        public boolean onLoadFailed(@android.support.annotation.Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            Log.e(TAG, "onLoadFailed: ");
            if (!(target instanceof ImageViewTarget)) {
                return false;
            }
            ImageView view = (ImageView) ((ViewTarget) target).getView();
            WritableMap event = new WritableNativeMap();
            ThemedReactContext context = (ThemedReactContext) view.getContext();
            RCTEventEmitter eventEmitter = context.getJSModule(RCTEventEmitter.class);
            int viewId = view.getId();
            eventEmitter.receiveEvent(viewId, REACT_ON_ERROR_EVENT, event);
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            Log.d(TAG, "onResourceReady: [" + dataSource + "]");
            if (!(target instanceof ImageViewTarget)) {
                return false;
            }
            ImageView view = (ImageView) ((ViewTarget) target).getView();
            WritableMap event = new WritableNativeMap();
            ThemedReactContext context = (ThemedReactContext) view.getContext();
            RCTEventEmitter eventEmitter = context.getJSModule(RCTEventEmitter.class);
            int viewId = view.getId();
            eventEmitter.receiveEvent(viewId, REACT_ON_LOAD_EVENT, event);
            return false;
        }
    }

    @ReactProp(name = "source")
    public void setSrc(final ImageView view, @Nullable ReadableMap source) {
        if (source == null) {
            cancelExistingRequests(view);
            view.setImageDrawable(null);
            return;
        }

        final String uriProp = source.getString("uri");
        final GlideUrl glideUrl = source.hasKey("headers") ? createUrlWithHeaders(source, uriProp) : new GlideUrl(uriProp);
        String priorityProp = source.hasKey("priority") ? source.getString("priority") : "normal";
        final Priority priority = REACT_PRIORITY_MAP.get(priorityProp);

        RequestOptions options = new RequestOptions()
                .priority(priority)
                .placeholder(TRANSPARENT_DRAWABLE)
                .diskCacheStrategy(DiskCacheStrategy.ALL);
        Log.i(TAG, "loading: " + uriProp + "\nwidth:" + view.getWidth() + " height: " + view.getHeight());
        Glide.with(view.getContext())
                .load(glideUrl)
                .apply(options)
                .listener(new LoadListener())
                .into(view);
    }

    private void cancelExistingRequests(@Nullable ImageView view) {
        if (view != null) {
            Log.e(TAG, "cancelExistingRequests");
            Glide.with(view.getContext()).clear(view);
        }
    }

    @NonNull
    private GlideUrl createUrlWithHeaders(ReadableMap source, String uriProp) {
        final ReadableMap headersMap = source.getMap("headers");
        ReadableMapKeySetIterator headersIterator = headersMap.keySetIterator();
        LazyHeaders.Builder headersBuilder = new LazyHeaders.Builder();
        while (headersIterator.hasNextKey()) {
            String key = headersIterator.nextKey();
            String value = headersMap.getString(key);
            headersBuilder.addHeader(key, value);
        }
        LazyHeaders headers = headersBuilder.build();
        return new GlideUrl(uriProp, headers);
    }

    @ReactProp(name = "resizeMode")
    public void setResizeMode(ImageView view, String resizeMode) {
        if (resizeMode == null) resizeMode = "contain";
        final ImageView.ScaleType scaleType = REACT_RESIZE_MODE_MAP.get(resizeMode);
        view.setScaleType(scaleType);
    }

    @Override
    public void onDropViewInstance(ImageView view) {
        cancelExistingRequests(view);
        super.onDropViewInstance(view);
    }

    @Override
    @Nullable
    public Map getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.of(
                REACT_ON_LOAD_EVENT,
                MapBuilder.of("registrationName", REACT_ON_LOAD_EVENT),
                REACT_ON_ERROR_EVENT,
                MapBuilder.of("registrationName", REACT_ON_ERROR_EVENT)
        );
    }
}
