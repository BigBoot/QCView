package de.bigboot.qcircleview.cover;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;
import com.malinskiy.materialicons.widget.IconTextView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.Receiver;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.ViewsById;

import java.util.List;

import de.bigboot.qcircleview.R;

/**
 * Created by Marco Kirchner.
 */
@EFragment(R.layout.pager_music)
public class CoverMusicFragment extends Fragment {
    // Note that ACTION_VOLUME_CHANGED is a private broadcast so it may not work
    // However since it doesn't break anything and it's the ony way I found, I'll leave it here
    private static final String ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION";

    protected MediaController mediaController;

    @ViewById(R.id.root)
    protected ViewGroup root;

    @ViewById(R.id.artist)
    protected TextView artist;
    @ViewById(R.id.title)
    protected TextView title;

    @ViewById(R.id.toggle_play)
    protected IconTextView togglePlay;

    @ViewById(R.id.background)
    protected ImageView background;

    @ViewById(R.id.volume_control)
    protected View volumeControl;
    @ViewById(R.id.volume_bar)
    protected SeekBar volumeBar;

    @ViewsById({R.id.toggle_play, R.id.previous, R.id.next})
    protected List<IconTextView> controlButtons;
    @ViewsById({R.id.volume_bar, R.id.volume_increase, R.id.volume_decrease})
    protected List<View> volumeControls;

    private MediaCallback mediaCallback = new MediaCallback();
    private static final int[] BACKGROUND_COLORS = new int[] {
        0xC8F44336, 0xC8E91E63, 0xC89C27B0, 0xC8673AB7, 0xC83F51B5, 0xC82196F3, 0xC800BCD4,
        0xC8009688, 0xC84CAF50, 0xC88BC34A, 0xC8CDDC39, 0xC8FFEB3B, 0xC8FFC107, 0xC8FF9800,
        0xC8FF5722, 0xC8795548, 0xC89E9E9E, 0xC8607D8B};

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mediaController != null) {
            mediaController.unregisterCallback(mediaCallback);
        }
    }

    public void setMediaController(MediaController mediaController) {
        if(mediaController != null) {
            mediaController.unregisterCallback(mediaCallback);
        }
        this.mediaController = mediaController;
        init();
    }

    private int nameToColor(String name) {
        int i = 0;
        for(char c : name.toCharArray()) {
            i = (i + c) % BACKGROUND_COLORS.length;
        }
        return BACKGROUND_COLORS[i];
    }

    @AfterViews
    protected void init() {
        if(artist == null) {
            return;
        }
        updateAll();
        mediaController.registerCallback(mediaCallback);
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    mediaController.setVolumeTo(progress, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    protected void updateAll() {
        updatePlaybackState();
        updateMetadata();
        updatePlaybackInfo();
    }

    @Receiver(actions = ACTION_VOLUME_CHANGED)
    protected void updatePlaybackInfo() {
        MediaController.PlaybackInfo playbackInfo = mediaController.getPlaybackInfo();
        if (playbackInfo != null) {
            volumeControl.setVisibility(View.VISIBLE);
            int maxVolume = playbackInfo.getMaxVolume();
            int curVolume = playbackInfo.getCurrentVolume();
            volumeBar.setMax(maxVolume);
            volumeBar.setProgress(curVolume);
        } else {
            volumeControl.setVisibility(View.INVISIBLE);
        }
    }

    protected void updatePlaybackState() {
        PlaybackState playbackState = mediaController.getPlaybackState();
        if(playbackState != null && playbackState.getState() == PlaybackState.STATE_PLAYING) {
            togglePlay.setText("{md-pause}");
        } else {
            togglePlay.setText("{md-play-arrow}");
        }
    }

    protected void updateMetadata() {
        MediaMetadata mediaMetadata = mediaController.getMetadata();
        if(mediaMetadata != null) {
            artist.setText(mediaMetadata.getText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST));
            title.setText(mediaMetadata.getText(MediaMetadata.METADATA_KEY_TITLE));
            Bitmap b =  mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            if (b != null) {
                root.setBackground(new BitmapDrawable(b));
            }
            Drawable d = new IconDrawable(getActivity(), FontAwesomeIcons.fa_music)
            .color(Color.argb(200, 50, 50, 50));
            background.setImageDrawable(d);
            background.setBackgroundColor(nameToColor(mediaController.getPackageName()));
        }
    }

    @Click(R.id.next)
    protected void onNext() {
        mediaController.getTransportControls().skipToNext();
    }

    @Click(R.id.previous)
    protected void onPrevious() {
        mediaController.getTransportControls().skipToPrevious();

    }

    @Click(R.id.toggle_play)
    protected void onTogglePlay() {
        if(mediaController.getPlaybackState().getState() == PlaybackState.STATE_PLAYING) {
            mediaController.getTransportControls().pause();
        } else {
            mediaController.getTransportControls().play();
        }
    }

    @Click(R.id.volume_control)
    protected void onVolumeControl() {
        if (volumeBar.getVisibility() == View.INVISIBLE) {
            updatePlaybackInfo();
            for (View view : volumeControls)
                view.setVisibility(View.VISIBLE);
            for (IconTextView button : controlButtons)
                button.setVisibility(View.INVISIBLE);
        } else {
            for (View view : volumeControls)
                view.setVisibility(View.INVISIBLE);
            for (IconTextView button : controlButtons)
                button.setVisibility(View.VISIBLE);
        }
    }

    @Click(R.id.volume_decrease)
    protected void onVolumeDecrease() {
        mediaController.adjustVolume(AudioManager.ADJUST_LOWER, 0);
    }

    @Click(R.id.volume_increase)
    protected void onVolumeIncrease() {
        mediaController.adjustVolume(AudioManager.ADJUST_RAISE, 0);
    }

    private class MediaCallback extends MediaController.Callback {

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            updatePlaybackState();
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            updateMetadata();
        }

        @Override
        public void onAudioInfoChanged(MediaController.PlaybackInfo info) {
            updatePlaybackInfo();
        }
    }
}
