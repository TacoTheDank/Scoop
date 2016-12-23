package tk.wasdennnoch.scoop.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;

import tk.wasdennnoch.scoop.R;

@SuppressWarnings("unused")
public class SwitchPreferenceCompat extends CheckBoxPreference {

    public SwitchPreferenceCompat(Context context) {
        super(context);
        init();
    }

    public SwitchPreferenceCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SwitchPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SwitchPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setWidgetLayoutResource(R.layout.preference_switch_layout);
    }
}