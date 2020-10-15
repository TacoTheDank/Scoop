package tk.wasdennnoch.scoop.data.app;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

public class App {

    final Drawable icon;
    final String name;
    final String packageName;
    boolean selected;

    App(Drawable icon, String name, String packageName) {
        this.icon = icon;
        this.name = name;
        this.packageName = packageName;
    }

    @NonNull
    @Override
    public String toString() {
        return "App[" +
                "; packageName " + packageName +
                "; selected " + selected +
                "]";
    }
}
