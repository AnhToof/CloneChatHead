package com.flipkart.chatheads.ui.container;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import com.flipkart.chatheads.ui.ChatHead;
import com.flipkart.chatheads.ui.ChatHeadArrangement;
import com.flipkart.chatheads.ui.ChatHeadManager;
import com.flipkart.chatheads.ui.FrameChatHeadContainer;
import com.flipkart.chatheads.ui.HostFrameLayout;
import com.flipkart.chatheads.ui.MaximizedArrangement;
import com.flipkart.chatheads.ui.MinimizedArrangement;

import static android.content.Context.WINDOW_SERVICE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

/**
 * Created by kiran.kumar on 08/11/16.
 */

public class WindowManagerContainer extends FrameChatHeadContainer {
    /**
     * A transparent view of the size of chat head which capture motion events and delegates them to
     * the real view (frame layout)
     * This view is required since window managers will delegate the touch events to the window
     * beneath it only if they are outside the bounds.
     * {@link android.view.WindowManager.LayoutParams#FLAG_NOT_TOUCH_MODAL}
     */

    private Context mContext;
    private View motionCaptureView;

    private int cachedHeight;
    private int cachedWidth;
    private WindowManager windowManager;
    private ChatHeadArrangement currentArrangement;
    private boolean motionCaptureViewAdded;

    private static final int OVERLAY_TYPE;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            HostFrameLayout frameLayout = getFrameLayout();
            if (frameLayout != null) {
                frameLayout.minimize();
            }
        }
    };

    private IntentFilter mIntentFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

    static {
        if (Build.VERSION.SDK_INT >= 26) {
            OVERLAY_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            OVERLAY_TYPE = WindowManager.LayoutParams.TYPE_PRIORITY_PHONE;
        }
    }

    public WindowManagerContainer(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void onInitialized(ChatHeadManager manager) {
        super.onInitialized(manager);
        motionCaptureView = new MotionCaptureView(getContext());

        MotionCapturingTouchListener listener = new MotionCapturingTouchListener();
        motionCaptureView.setOnTouchListener(listener);
        registerReceiver(getContext());
    }

    public void registerReceiver(Context context) {
        context.registerReceiver(mBroadcastReceiver, mIntentFilter);
    }

    public void unregisterReceiver(Context context) {
        context.unregisterReceiver(mBroadcastReceiver);
    }

    public WindowManager getWindowManager() {
        if (windowManager == null) {
            windowManager = (WindowManager) getContext().getSystemService(WINDOW_SERVICE);
        }
        return windowManager;
    }

    protected void setContainerHeight(View container, int height) {
        if (motionCaptureView.getWindowToken() != null) {
            WindowManager.LayoutParams layoutParams = getOrCreateLayoutParamsForContainer(container);
            layoutParams.height = height;
            getWindowManager().updateViewLayout(container, layoutParams);
        }
    }

    protected void setContainerWidth(View container, int width) {
        if (motionCaptureView.getWindowToken() != null) {
            WindowManager.LayoutParams layoutParams = getOrCreateLayoutParamsForContainer(container);
            layoutParams.width = width;
            getWindowManager().updateViewLayout(container, layoutParams);
        }
    }

    protected WindowManager.LayoutParams getOrCreateLayoutParamsForContainer(View container) {
        WindowManager.LayoutParams layoutParams =
                (WindowManager.LayoutParams) container.getLayoutParams();
        if (layoutParams == null) {
            layoutParams = createContainerLayoutParams(false);
            container.setLayoutParams(layoutParams);
        }
        return layoutParams;
    }

    protected void setContainerX(View container, int xPosition) {
        if (motionCaptureView.getWindowToken() != null) {
            WindowManager.LayoutParams layoutParams = getOrCreateLayoutParamsForContainer(container);
            layoutParams.x = xPosition;
            getWindowManager().updateViewLayout(container, layoutParams);
        }
    }

    protected int getContainerX(View container) {
        WindowManager.LayoutParams layoutParams = getOrCreateLayoutParamsForContainer(container);
        return layoutParams.x;
    }

    protected void setContainerY(View container, int yPosition) {
        if (motionCaptureView.getWindowToken() != null) {
            WindowManager.LayoutParams layoutParams = getOrCreateLayoutParamsForContainer(container);
            layoutParams.y = yPosition;
            getWindowManager().updateViewLayout(container, layoutParams);
        }
    }

    protected int getContainerY(View container) {
        WindowManager.LayoutParams layoutParams = getOrCreateLayoutParamsForContainer(container);
        return layoutParams.y;
    }

    protected WindowManager.LayoutParams createContainerLayoutParams(boolean focusable) {
        int focusableFlag;
        if (focusable) {
            focusableFlag = FLAG_NOT_TOUCH_MODAL;
        } else {
            focusableFlag = FLAG_NOT_TOUCHABLE | FLAG_NOT_FOCUSABLE;
        }
        WindowManager.LayoutParams layoutParams =
                new WindowManager.LayoutParams(MATCH_PARENT, MATCH_PARENT, OVERLAY_TYPE,
                        focusableFlag, PixelFormat.TRANSLUCENT);
        layoutParams.x = 0;
        layoutParams.y = 0;
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        return layoutParams;
    }

    @Override
    public void addContainer(View container, boolean focusable) {
        WindowManager.LayoutParams containerLayoutParams = createContainerLayoutParams(focusable);
        addContainer(container, containerLayoutParams);
    }

    public void addContainer(View container, WindowManager.LayoutParams containerLayoutParams) {
        container.setLayoutParams(containerLayoutParams);
        getWindowManager().addView(container, containerLayoutParams);
    }

    @Override
    public void setViewX(View view, int xPosition) {
        super.setViewX(view, xPosition);
        if (view instanceof ChatHead) {
            boolean hero = ((ChatHead) view).isHero();
            if (hero && currentArrangement instanceof MinimizedArrangement) {
                setContainerX(motionCaptureView, xPosition);
                setContainerWidth(motionCaptureView, view.getMeasuredWidth());
            }
        }
    }

    @Override
    public void setViewY(View view, int yPosition) {
        super.setViewY(view, yPosition);
        if (view instanceof ChatHead && currentArrangement instanceof MinimizedArrangement) {
            boolean hero = ((ChatHead) view).isHero();
            if (hero) {
                setContainerY(motionCaptureView, yPosition);
                setContainerHeight(motionCaptureView, view.getMeasuredHeight());
            }
        }
    }

    @Override
    public void onArrangementChanged(ChatHeadArrangement oldArrangement,
            ChatHeadArrangement newArrangement) {
        currentArrangement = newArrangement;
        if (oldArrangement instanceof MinimizedArrangement
                && newArrangement instanceof MaximizedArrangement) {
            // about to be maximized
            WindowManager.LayoutParams layoutParams =
                    getOrCreateLayoutParamsForContainer(motionCaptureView);
            layoutParams.flags |= FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCHABLE;
            if (motionCaptureView != null && motionCaptureView.getWindowToken() != null) {
                windowManager.updateViewLayout(motionCaptureView, layoutParams);
            }

            layoutParams = getOrCreateLayoutParamsForContainer(getFrameLayout());
            layoutParams.flags &= ~FLAG_NOT_FOCUSABLE; //add focusability
            layoutParams.flags &= ~FLAG_NOT_TOUCHABLE; //add focusability
            layoutParams.flags |= FLAG_NOT_TOUCH_MODAL;

            windowManager.updateViewLayout(getFrameLayout(), layoutParams);

            setContainerX(motionCaptureView, 0);
            setContainerY(motionCaptureView, 0);
            setContainerWidth(motionCaptureView, getFrameLayout().getMeasuredWidth());
            setContainerHeight(motionCaptureView, getFrameLayout().getMeasuredHeight());
        } else {
            // about to be minimized
            WindowManager.LayoutParams layoutParams =
                    getOrCreateLayoutParamsForContainer(motionCaptureView);
            layoutParams.flags |= FLAG_NOT_FOCUSABLE; //remove focusability
            layoutParams.flags &= ~FLAG_NOT_TOUCHABLE; //add touch
            layoutParams.flags |= FLAG_NOT_TOUCH_MODAL; //add touch
            if (motionCaptureView != null && motionCaptureView.getWindowToken() != null) {
                windowManager.updateViewLayout(motionCaptureView, layoutParams);
            }

            layoutParams = getOrCreateLayoutParamsForContainer(getFrameLayout());
            layoutParams.flags |= FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCHABLE;
            windowManager.updateViewLayout(getFrameLayout(), layoutParams);
        }
    }

    @Override
    public void addView(View view, ViewGroup.LayoutParams layoutParams) {
        super.addView(view, layoutParams);
        if (!motionCaptureViewAdded && getManager().getChatHeads().size() > 0) {
            addContainer(motionCaptureView, true);
            WindowManager.LayoutParams motionCaptureParams =
                    getOrCreateLayoutParamsForContainer(motionCaptureView);
            motionCaptureParams.width = 0;
            motionCaptureParams.height = 0;
            windowManager.updateViewLayout(motionCaptureView, motionCaptureParams);
            motionCaptureViewAdded = true;
        }
    }

    @Override
    public void removeView(View view) {
        super.removeView(view);
        if (getManager().getChatHeads().size() == 0) {
            windowManager.removeViewImmediate(motionCaptureView);
            motionCaptureViewAdded = false;
        }
    }

    private void removeContainer(View motionCaptureView) {
        windowManager.removeView(motionCaptureView);
    }

    public void destroy() {
        if (motionCaptureView != null && motionCaptureView.getWindowToken() != null) {
            windowManager.removeViewImmediate(motionCaptureView);
        }
        windowManager.removeViewImmediate(getFrameLayout());
        mContext.unregisterReceiver(mBroadcastReceiver);
    }

    protected class MotionCapturingTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            event.offsetLocation(getContainerX(v), getContainerY(v));
            HostFrameLayout frameLayout = getFrameLayout();
            if (frameLayout != null) {
                return frameLayout.dispatchTouchEvent(event);
            } else {
                return false;
            }
        }
    }

    private class MotionCaptureView extends View {
        public MotionCaptureView(Context context) {
            super(context);
        }
    }
}
