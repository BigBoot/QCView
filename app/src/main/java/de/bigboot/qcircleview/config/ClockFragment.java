package de.bigboot.qcircleview.config;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.widget.ImageView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.ViewById;

import de.bigboot.qcircleview.R;

/**
 * Created by Marco Kirchner
 */
@EFragment(R.layout.fragment_clock)
public class ClockFragment extends Fragment {
   @ViewById(R.id.imageView)
   ImageView imageView;

    @FragmentArg
    Bitmap previewImage;

    @AfterViews
    protected void init () {
        imageView.setImageBitmap(previewImage);
    }
}
