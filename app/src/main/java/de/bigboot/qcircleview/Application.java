package de.bigboot.qcircleview;

import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeModule;
import com.joanzapata.iconify.fonts.WeathericonsModule;

public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Iconify.with(new FontAwesomeModule()).with(new WeathericonsModule());
    }
}
