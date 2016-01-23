package de.bigboot.qcircleview.config;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toolbar;

import de.bigboot.qcircleview.R;

public class LibrariesActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_libraries);
        setActionBar((Toolbar) findViewById(R.id.toolbar));
    }
}
