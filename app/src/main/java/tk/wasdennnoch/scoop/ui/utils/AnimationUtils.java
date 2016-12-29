package tk.wasdennnoch.scoop.ui.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.Interpolator;

public class AnimationUtils {

    private static final Interpolator FAST_OUT_SLOW_IN = new FastOutSlowInInterpolator();

    public static final int ANIM_DURATION_DEFAULT = 400;

    public static void slideToolbar(Toolbar t, boolean out, int duration) {
        slideToolbar(t, out, duration, false, null);
    }

    public static void slideToolbar(final Toolbar t, final boolean out, int duration, final boolean setVisibility, final Runnable endAction) {
        if (setVisibility && !out) t.setVisibility(View.VISIBLE);
        int actionBarSize = getActionBarSize(t.getContext());
        t.setAlpha(out ? 1 : 0);
        //noinspection ResourceType // yeah AS really
        t.setTranslationY(out ? 0 : -actionBarSize);
        ViewPropertyAnimator animator = t.animate()
                .alpha(out ? 0 : 1)
                .translationY(out ? -actionBarSize : 0)
                .setDuration(duration)
                .setInterpolator(FAST_OUT_SLOW_IN);
        addEndAction(animator, new Runnable() {
            @Override
            public void run() {
                if (setVisibility && out) t.setVisibility(View.GONE);
                if (endAction != null)
                    endAction.run();
            }
        });
        animator.start();
    }

    private static int getActionBarSize(Context context) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.actionBarSize, value, true);
        return TypedValue.complexToDimensionPixelSize(value.data, context.getResources().getDisplayMetrics());
    }

    private static void addEndAction(ViewPropertyAnimator anim, final Runnable action) {
        if (Build.VERSION.SDK_INT >= 16) {
            anim.withEndAction(action);
        } else {
            anim.setListener(action == null ? null : new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    action.run();
                }
            });
        }
    }

}
