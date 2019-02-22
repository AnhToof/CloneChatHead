package com.flipkart.springyheads.demo;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.flipkart.chatheads.ui.ChatHead;
import com.flipkart.chatheads.ui.ChatHeadArrangement;
import com.flipkart.chatheads.ui.ChatHeadListener;
import com.flipkart.chatheads.ui.ChatHeadViewAdapter;
import com.flipkart.chatheads.ui.MaximizedArrangement;
import com.flipkart.chatheads.ui.MinimizedArrangement;
import com.flipkart.chatheads.ui.container.DefaultChatHeadManager;
import com.flipkart.chatheads.ui.container.WindowManagerContainer;
import com.flipkart.circularImageView.CircularDrawable;
import com.flipkart.circularImageView.TextDrawer;
import com.flipkart.circularImageView.notification.CircularNotificationDrawer;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ChatHeadService extends Service {

    // Binder given to clients
    private static final String TAG = ChatHeadService.class.getSimpleName();

    private final IBinder mBinder = new LocalBinder();
    private DefaultChatHeadManager<String> chatHeadManager;
    private int chatHeadIdentifier = 0;
    private WindowManagerContainer windowManagerContainer;
    private Map<String, View> viewCache = new HashMap<>();
    private boolean isChatHeadOpen = false;
    private boolean isKeyboardOpen = false;
    private View mView;

    public static Intent showFloating(Context context) {
        return new Intent(context, ChatHeadService.class);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        moveToForeground();
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManagerContainer = new WindowManagerContainer(this) {
            @Override
            protected void onBackPressed() {
                super.onBackPressed();
                if (isKeyboardOpen) {
                    if (mView != null) {
                        InputMethodManager imm =
                                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(mView.getWindowToken(), 0);
                        isKeyboardOpen = false;
                    }
                }
                if (isChatHeadOpen && !isKeyboardOpen) {
                    minimize();
                    isChatHeadOpen = false;
                }
            }
        };

        chatHeadManager = new DefaultChatHeadManager<String>(this, windowManagerContainer);
        chatHeadManager.setConfig(new CustomChatHeadConfig(getApplicationContext(), 0, 300));
        chatHeadManager.setViewAdapter(new ChatHeadViewAdapter<String>() {

            @Override
            public View attachView(String key, final ChatHead chatHead, ViewGroup parent) {
                View cachedView = viewCache.get(key);
                if (cachedView == null) {
                    LayoutInflater inflater =
                            (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                    final View view = inflater.inflate(R.layout.fragment_test, parent, false);
                    mView = view;
                    LinearLayout linearLayout = view.findViewById(R.id.linearLayout);
                    final EditText editText = view.findViewById(R.id.identifier);
                    cachedView = view;
                    view.getViewTreeObserver()
                            .addOnGlobalLayoutListener(
                                    new ViewTreeObserver.OnGlobalLayoutListener() {
                                        @Override
                                        public void onGlobalLayout() {
                                            if (chatHeadManager.getActiveArrangement() instanceof MinimizedArrangement) {
                                                isChatHeadOpen = false;
                                                isKeyboardOpen = false;
                                            } else {
                                                isChatHeadOpen = true;
                                            }
                                            if (isChatHeadOpen) {
                                                if (editText.isFocused()) {
                                                    int heightDiff = view.getRootView().getHeight()
                                                            - view.getHeight();
                                                    if (heightDiff > 100) {
                                                        isKeyboardOpen = true;
                                                    }
                                                }
                                            }
                                        }
                                    });
                    viewCache.put(key, view);
                }
                parent.addView(cachedView);
                return cachedView;
            }

            @Override
            public void detachView(String key, ChatHead<? extends Serializable> chatHead,
                    ViewGroup parent) {
                View cachedView = viewCache.get(key);
                if (cachedView != null) {
                    parent.removeView(cachedView);
                }
            }

            @Override
            public void removeView(String key, ChatHead<? extends Serializable> chatHead,
                    ViewGroup parent) {
                View cachedView = viewCache.get(key);
                if (cachedView != null) {
                    viewCache.remove(key);
                    parent.removeView(cachedView);
                }
            }

            @Override
            public Drawable getChatHeadDrawable(String key) {
                return ChatHeadService.this.getChatHeadDrawable(key);
            }
        });

        chatHeadManager.setListener(new ChatHeadListener() {
            @Override
            public void onChatHeadAdded(Object key) {
            }

            @Override
            public void onChatHeadRemoved(Object key, boolean userTriggered) {
                if (chatHeadManager.getChatHeads().isEmpty()) {
                    stopForeground(true);
                    stopSelf();
                    stopService(showFloating(getApplicationContext()));
                }
            }

            @Override
            public void onChatHeadArrangementChanged(ChatHeadArrangement oldArrangement,
                    ChatHeadArrangement newArrangement) {

            }

            @Override
            public void onChatHeadAnimateEnd(ChatHead chatHead) {

            }

            @Override
            public void onChatHeadAnimateStart(ChatHead chatHead) {

            }
        });

        addChatHead();
        chatHeadManager.setArrangement(MinimizedArrangement.class, null);
        moveToForeground();
    }

    private Drawable getChatHeadDrawable(String key) {
        Random rnd = new Random();
        int randomColor = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        CircularDrawable circularDrawable = new CircularDrawable();
        circularDrawable.setBitmapOrTextOrIcon(
                new TextDrawer().setText("C" + key).setBackgroundColor(randomColor));
        int badgeCount = 1;
        circularDrawable.setNotificationDrawer(
                new CircularNotificationDrawer().setNotificationText(String.valueOf(badgeCount))
                        .setNotificationAngle(135)
                        .setNotificationColor(Color.WHITE, Color.RED));
        circularDrawable.setBorder(Color.WHITE, 3);
        return circularDrawable;
    }

    private void moveToForeground() {
        Notification notification =
                new NotificationCompat.Builder(this).setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Springy heads")
                        .setContentText("Click to configure.")
                        .setContentIntent(PendingIntent.getActivity(this, 0,
                                new Intent(this, FloatingActivity.class), 0))
                        .build();

        startForeground(1, notification);
    }

    public void addChatHead() {
        chatHeadIdentifier++;
        chatHeadManager.addChatHead(String.valueOf(1), false, true);
        chatHeadManager.bringToFront(chatHeadManager.findChatHeadByKey(String.valueOf(1)));
    }

    public void removeChatHead() {
        removeAllChatHeads();
        stopSelf();
        stopForeground(true);
    }

    public void removeAllChatHeads() {
        chatHeadIdentifier = 0;
        chatHeadManager.removeAllChatHeads(true);
    }

    public void toggleArrangement() {
        if (chatHeadManager.getActiveArrangement() instanceof MinimizedArrangement) {
            chatHeadManager.setArrangement(MaximizedArrangement.class, null);
        } else {
            chatHeadManager.setArrangement(MinimizedArrangement.class, null);
        }
    }

    public void updateBadgeCount() {
        chatHeadManager.reloadDrawable(String.valueOf(1));
    }

    @Override
    public void onDestroy() {
        windowManagerContainer.destroy();
        chatHeadManager = null;
        super.onDestroy();
    }

    public void minimize() {
        chatHeadManager.setArrangement(MinimizedArrangement.class, null);
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        ChatHeadService getService() {
            // Return this instance of LocalService so clients can call public methods
            return ChatHeadService.this;
        }
    }
}