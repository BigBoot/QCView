package de.bigboot.qcircleview.cover;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Outline;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.ViewsById;

import java.util.List;

import de.bigboot.qcircleview.NotificationCache;
import de.bigboot.qcircleview.NotificationService;
import de.bigboot.qcircleview.R;

/**
 * Created by Marco Kirchner.
 */
@EFragment(R.layout.pager_item)
public class CoverNotificationFragment extends Fragment {
    public NotificationService.Notification getNotification() {
        return notification;
    }

    @FragmentArg
    protected NotificationService.Notification notification;

    @ViewById(R.id.root)
    protected View rootView;

    @ViewById(R.id.title)
    protected AlphaTextView titleView;

    @ViewById(R.id.text)
    protected AlphaTextView textView;

    @ViewById(R.id.scrollview)
    protected ScrollView textScrollView;

    @ViewById(R.id.icon)
    protected ImageView iconView;

    @ViewById(R.id.image)
    protected ImageView imageView;

    @ViewById(R.id.image_background)
    protected View imageBackgroundView;

    @ViewById(R.id.panel)
    protected RelativeLayout panelView;

    @ViewById(R.id.delete_notification)
    protected ImageView deleteNotificationView;

    @ViewsById({R.id.action1, R.id.action2, R.id.action3})
    protected List<Button> actionButtons;

    private Interpolator interpolator = new AccelerateInterpolator(1.5f);
    private Drawable imageDrawable;
    private boolean hasImage = false;

    @Override
    public void onResume() {
        super.onResume();
    }

    @AfterViews
    protected void init() {
        rootView.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        rootView.setClipToOutline(true);
        if(NotificationCache.INSTANCE.getNotificationImage(notification) != null) {
            Drawable image = new BitmapDrawable(getResources(),
                    NotificationCache.INSTANCE.getNotificationImage(notification));
            imageView.setImageDrawable(image);
            hasImage = true;
        }

        PackageManager pm = getActivity().getPackageManager();
        String pckg = notification.getPackageName();
        Drawable icon = null;
        try {
            icon = pm.getApplicationIcon(pckg);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        iconView.setImageDrawable(icon);
        iconView.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                switch (dragEvent.getAction()) {
                    case DragEvent.ACTION_DRAG_ENDED:
                        ObjectAnimator fadeOutAnimator = ObjectAnimator.ofFloat(deleteNotificationView, View.ALPHA, 1f, 0f);
                        fadeOutAnimator.setDuration(300); //ms
                        fadeOutAnimator.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                deleteNotificationView.setVisibility(View.GONE);
                            }
                        });
                        fadeOutAnimator.start();
                        break;
                    case DragEvent.ACTION_DROP:
                        if(view == deleteNotificationView) {
                            titleView.setText("Deleted");
                        }
                        break;
                    case DragEvent.ACTION_DRAG_ENTERED:

                        break;
                    case DragEvent.ACTION_DRAG_EXITED:

                        break;
                }
                return true;
            }
        });
        iconView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!notification.isClearable()) {
                            return false;
                        }
                        ClipData data = ClipData.newPlainText("", "");
                        View.DragShadowBuilder shadow = new View.DragShadowBuilder(view);
                        view.startDrag(data,shadow,null,0);

                        ObjectAnimator fadeInAnimator = ObjectAnimator.ofFloat(deleteNotificationView, View.ALPHA, 0f, 1f);
                        fadeInAnimator.setDuration(300); //ms
                        fadeInAnimator.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                deleteNotificationView.setVisibility(View.VISIBLE);
                            }
                        });
                        fadeInAnimator.start();
                        return true;
                }
                return false;
            }
        });
        deleteNotificationView.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                switch (dragEvent.getAction()) {
                    case DragEvent.ACTION_DRAG_ENDED:
                        break;
                    case DragEvent.ACTION_DROP:
                        if(view == deleteNotificationView) {
                            getActivity().sendBroadcast(new Intent(NotificationService.ACTION_COMMAND)
                            .putExtra(NotificationService.EXTRA_COMMAND, NotificationService.COMMAND_DELETE_NOTIFICATION)
                            .putExtra(NotificationService.EXTRA_NOTIFCATION, notification));
                        }
                        break;
                    case DragEvent.ACTION_DRAG_ENTERED:
                        break;
                    case DragEvent.ACTION_DRAG_EXITED:
                        break;
                }
                return true;
            }
        });

        titleView.setText(notification.getTitle());
        textView.setText(notification.getText());

        List<NotificationService.Action> actions = notification.getActions();
        if(actions.size() >= 1) {
            setButtonAction(0, actions.get(0));
        }
        if(actions.size() >= 2) {
            setButtonAction(1, actions.get(1));
        }
        if(actions.size() >= 3) {
            setButtonAction(2, actions.get(2));
        }
    }

    private void setButtonAction(int button, NotificationService.Action action) {
        actionButtons.get(button).setVisibility(View.VISIBLE);
        actionButtons.get(button).setText(action.getTitle());
    }

    private void setTopMargin(View view, int topMargin) {
        if(view == null)
            return;
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if( lp instanceof ViewGroup.MarginLayoutParams)
        {
            ((ViewGroup.MarginLayoutParams) lp).topMargin = topMargin;
        }
    }

    private void setBottom(View view, int bottomMargin) {
        if(view == null)
            return;
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if( lp instanceof ViewGroup.MarginLayoutParams)
        {
            ((ViewGroup.MarginLayoutParams) lp).bottomMargin = bottomMargin;
        }
    }

    private void setRightMargin(View view, int rightMargin) {
        if(view == null)
            return;
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if( lp instanceof ViewGroup.MarginLayoutParams)
        {
            ((ViewGroup.MarginLayoutParams) lp).rightMargin = rightMargin;
            view.setLayoutParams(lp);
        }
    }

    @Click({R.id.action1, R.id.action2, R.id.action3})
    protected void onAction(View sender) {
        NotificationService.Action action;
        switch (sender.getId()) {
            case R.id.action1:
                action = notification.getActions().get(0);
                break;
            case R.id.action2:
                action = notification.getActions().get(1);
                break;
            case R.id.action3:
                action = notification.getActions().get(2);
                break;
            default:
                return;
        }
        try {
            action.getIntent().send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }
}
