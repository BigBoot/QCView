package de.bigboot.qcircleview.config;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.soundcloud.android.crop.Crop;
import com.viewpagerindicator.CirclePageIndicator;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.OptionsMenuItem;
import org.androidannotations.annotations.ViewById;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.bigboot.qcircleview.Preferences;
import de.bigboot.qcircleview.R;
import de.bigboot.qcircleview.RootTools;
import de.bigboot.qcircleview.SmartcoverService_;
import de.bigboot.qcircleview.updater.UpdateManager;

/**
 * Created by Marco Kirchner
 */
@EActivity(R.layout.activity_quickcirclemod_settings)
@OptionsMenu(R.menu.menu_quickcirclemod_settings)
public class QuickcirclemodSettings extends Activity {
    private static final int IMPORT_FILE_REQUEST_CODE = 42;

    @ViewById(R.id.viewpager)
    ViewPager viewPager;
    @ViewById(R.id.pageIndicator)
    CirclePageIndicator pagerIndicator;

    @ViewById(R.id.lblAuthorContent)
    TextView author;
    @ViewById(R.id.lblDescriptionContent)
    TextView description;
    @ViewById(R.id.lblTitle)
    TextView title;

    @ViewById(R.id.layout_root)
    View layoutRoot;
    @ViewById(R.id.lblEmpty)
    TextView empty;

    @ViewById(R.id.apply_theme)
    Button applyButton;
    @ViewById(R.id.customize_theme)
    Button customizeThemeButton;
    @ViewById(R.id.applied)
    TextView applied;

    @ViewById(R.id.toolbar)
    Toolbar toolbar;

    @OptionsMenuItem(R.id.action_delete)
    MenuItem actionDelete;

    private Clock currentClock = null;

    @Bean
    protected ClockAdapter adapter;
    protected Preferences prefs;

    @Override
    protected void onResume() {
        super.onResume();
        if(prefs.getBoolean(Preferences.BooleanSettings.AutoUpdate))
            new UpdateManager(this).checkForUpdate();
    }

    @AfterViews
    protected void init() {
        setActionBar(toolbar);
        prefs = new Preferences(this);

        if(prefs.getBoolean(Preferences.BooleanSettings.FirstStart)) {
            FirstStartActivity_.intent(this).start();
            this.finish();
            return;
        }

        viewPager.setAdapter(adapter);

        pagerIndicator.setViewPager(viewPager);
        pagerIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
            }

            @Override
            public void onPageSelected(int i) {
                loadClockInfo(i);
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
        loadClockInfo(0);
        SmartcoverService_.intent(this).start();
    }

    private void loadClockInfo (int i) {
        if (adapter.getCount() == 0||adapter.getCount() == 1) {
            empty.setVisibility(View.VISIBLE);
            layoutRoot.setVisibility(View.GONE);
            currentClock = null;
            setApplied(false);
        } else {
            if( i < 0 || i >= adapter.getCount())
                return;

            Clock c = adapter.getClock(i);
            title.setText(c.getTitle());
            author.setText(c.getAuthor());
            description.setText(c.getDescription());
            empty.setVisibility(View.GONE);
            layoutRoot.setVisibility(View.VISIBLE);
            currentClock = c;
            setApplied(c.equals(prefs.getActiveClock()));
        }
        if(currentClock instanceof Clock.StaticClock) {
            if(actionDelete != null) {
                actionDelete.setVisible(false);
            }
            if (currentClock != null && currentClock.getId().equals("de.bigboot.qcircleview.digital")) {
                customizeThemeButton.setVisibility(View.VISIBLE);
            }else if (currentClock != null && currentClock.getId().equals("de.bigboot.qcircleview.weather")) {
                customizeThemeButton.setVisibility(View.GONE);
            }
        } else {
            if(actionDelete != null) {
                actionDelete.setVisible(currentClock != null);
            }
            customizeThemeButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }



    @Click(R.id.customize_theme)
    protected void customize() {
        File file = new File(getFilesDir() + "/" + "digital_background");
        if (file.exists()) {
            file.delete();
            adapter.notifyDataSetChanged();
        }

        Crop.pickImage(this);
    }

    @Click(R.id.apply_theme)
    protected void apply() {
        if(currentClock == null) {
            prefs.setActiveClock(null);
        } else {
            prefs.setActiveClock(currentClock);
            setApplied(true);
        }
    }

    private void setApplied(boolean isApplied) {
        applyButton.setVisibility(isApplied? View.INVISIBLE: View.VISIBLE);
        applied.setVisibility(isApplied? View.VISIBLE: View.GONE);
    }

    @OptionsItem(R.id.action_delete)
    protected void delete() {
        if(currentClock != null) {
            String path = getFilesDir() + "/" + currentClock.getId() + "/";
            File file = new File(path);

            if (file.exists()) {
                String deleteCmd = "rm -r " + path;
                Runtime runtime = Runtime.getRuntime();
                try {
                    runtime.exec(deleteCmd);
                } catch (IOException e) { }
            }

            adapter.deleteClock(currentClock);
            loadClockInfo(adapter.getCount()-1);
        }
    }

    @OptionsItem(R.id.action_settings)
    protected void showSettings() {
        SettingsActivity_.intent(this).start();
    }

    @Click(R.id.add_clock)
    protected void importFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        startActivityForResult(intent, IMPORT_FILE_REQUEST_CODE);
    }

    private Clock importZip(InputStream zip) throws ImportClockException {
        try {
            File outputDir = getCacheDir();
            File outputFile = File.createTempFile("zip", ".zip", outputDir);

            BufferedInputStream in = new BufferedInputStream(zip);
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile));

            byte buffer[] = new byte[1024];
            int r;
            while ((r = in.read(buffer, 0, buffer.length)) != -1) {
                out.write(buffer, 0, r);
            }
            out.close();
            in.close();

            Clock c = importZip(outputFile.getAbsolutePath());
            outputFile.delete();
            return c;
        } catch (IOException e) {
            throw new ImportClockException(ImportClockException.Error.READ_ERROR);
        }
    }

    private Clock importZip(String zip) throws ImportClockException {
        try {
            ZipFile zipFile = new ZipFile(zip);

            ZipEntry clockEntry = zipFile.getEntry("clock.xml");
            if(clockEntry == null)
                throw new ImportClockException(ImportClockException.Error.NO_CLOCK_XML);

            Clock clock = Clock.fromXML(zipFile.getInputStream(clockEntry));

            String path = getFilesDir() + "/" + clock.getId() + "/";
            File clockDir = new File(path);
            clockDir.mkdirs();
            setFilePermission(clockDir, "0755");

            File clockFile = new File(path + clockEntry.getName());
            writeFile(zipFile, clockEntry, clockFile);
            setFilePermission(clockFile, "0644");

            for(String filename : clock.getFiles()) {
                ZipEntry fileEntry = zipFile.getEntry(filename);
                if(fileEntry == null)
                    throw new ImportClockException(ImportClockException.Error.MISSING_FILE);

                File f = new File(path + filename);
                writeFile(zipFile, fileEntry, f);
                setFilePermission(f, "0644");
            }

            for(String filename : clock.getCopyOnlyFiles()) {
                ZipEntry fileEntry = zipFile.getEntry(filename);
                if(fileEntry == null)
                    throw new ImportClockException(ImportClockException.Error.MISSING_FILE);

                File f = new File(path + filename);
                writeFile(zipFile, fileEntry, f);
                setFilePermission(f, "0644");
            }

            ZipEntry previewEntry = zipFile.getEntry("preview.png");
            if(previewEntry != null) {
                File f = new File(path + previewEntry.getName());
                writeFile(zipFile, previewEntry, f);
                setFilePermission(f, "0644");
            }

            return clock;
        } catch (IOException e) {
            throw new ImportClockException(ImportClockException.Error.READ_ERROR);
        }
    }

    private void writeFile(ZipFile file, ZipEntry entry, File out) throws IOException {
        BufferedOutputStream fout = new BufferedOutputStream(new FileOutputStream(out));
        BufferedInputStream in = new BufferedInputStream(file.getInputStream(entry));

        int count;
        byte[] buffer = new byte[1024];
        while ((count = in.read(buffer)) != -1)
        {
            fout.write(buffer, 0, count);
        }

        fout.close();
        in.close();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case IMPORT_FILE_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    new AsyncTask<Uri, Void, Clock>() {
                        ProgressDialog ringProgressDialog;
                        ImportClockException exception = null;

                        @Override
                        protected void onPreExecute() {
                            ringProgressDialog = ProgressDialog.show(QuickcirclemodSettings.this, "Please wait ...", "Importing Zip", true);
                            ringProgressDialog.setCancelable(false);
                        }

                        @Override
                        protected void onPostExecute(Clock c) {
                            ringProgressDialog.dismiss();
                            if(exception != null) {
                                handleImportException(exception);
                                return;
                            }
                            addImportedClock(c);
                        }

                        @Override
                        protected Clock doInBackground(Uri... uris) {
                            try {
                                InputStream in = getContentResolver().openInputStream(uris[uris.length-1]);;
                                return importZip(in);
                            } catch (ImportClockException e) {
                                exception = e;
                            } catch (FileNotFoundException e) {
                                exception = new ImportClockException(ImportClockException.Error.READ_ERROR);
                            }
                            return null;
                        }
                    }.execute(data.getData());
                }
                break;

            case Crop.REQUEST_PICK:
                if ( resultCode == RESULT_OK) {
                    beginCrop(data.getData());
                }
                break;

            case Crop.REQUEST_CROP:
                handleCrop(resultCode, data);
                break;
        }
    }

    private void beginCrop(Uri source) {
        Uri outputUri = Uri.fromFile(new File(getCacheDir(), "cropped"));
        new Crop(source).output(outputUri).asSquare().withMaxSize(1046, 1046).start(this);
    }

    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            File file = new File(Crop.getOutput(result).getPath());
            file.renameTo(new File(getFilesDir() + "/digital_background"));
            adapter.notifyDataSetChanged();
        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addImportedClock(Clock c) {
        adapter.addClock(c);
        viewPager.setCurrentItem(adapter.getCount() - 1);
        loadClockInfo(adapter.getCount() - 1);
    }

    private void handleImportException(ImportClockException e) {

        int msg;
        switch (e.getError()) {
            case NO_CLOCK_XML:
                msg = R.string.err_no_xml;
                break;
            case INVALID_CLOCK_XML:
                msg = R.string.err_invalid_xml;
                break;
            case READ_ERROR:
                msg = R.string.err_read_error;
                break;
            case MISSING_FILE:
                msg = R.string.err_missing_file;
                break;

            default:
                msg = R.string.err_import;
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private boolean setFilePermission(File f, String permission) {
        return RootTools.chmod(f, permission, false);
    }

}
