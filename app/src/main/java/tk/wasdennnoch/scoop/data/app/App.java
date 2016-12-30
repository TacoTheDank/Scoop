package tk.wasdennnoch.scoop.data.app;

import android.graphics.drawable.Drawable;

public class App {

    App(Drawable icon, String name, String packageName) {
        this.icon = icon;
        this.name = name;
        this.packageName = packageName;
    }

    Drawable icon;
    String name;
    String packageName;
    boolean selected;

    @Override
    public String toString() {
        return "App[" +
                "; packageName " + packageName +
                "; selected " + selected +
                "]";
    }
}
