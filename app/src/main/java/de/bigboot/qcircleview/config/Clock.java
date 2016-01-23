package de.bigboot.qcircleview.config;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.bigboot.qcircleview.cover.CoverAnalogClockFragment_;
import de.bigboot.qcircleview.cover.CoverDigitalClockFragment_;
import de.bigboot.qcircleview.cover.CoverWeatherClockFragment_;

/**
* Created by Marco Kirchner
*/
public class Clock implements Serializable {
    public enum Device {
        G2,
        G3
    }

    private String title = null;
    private String id = null;
    private String author = null;
    private String description = null;
    private Device device = Device.G3;
    private List<String> files = new ArrayList<String>();
    private List<String> copyOnlyFiles = new ArrayList<String>();
    private int activate = -1;
    private HashMap<String, HashMap<String, String>> options = new HashMap<String, HashMap<String, String>>();

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public Device getDevice() {
        return device;
    }

    public List<String> getFiles() {
        return files;
    }

    public List<String> getCopyOnlyFiles() {
        return copyOnlyFiles;
    }

    public int getActivate() {
        return activate;
    }

    public HashMap<String, HashMap<String, String>> getOptions() {
        return options;
    }

    public static Clock fromXML (String xml) throws IOException {
        return fromXML(new StringReader(xml));
    }

    public static Clock fromXML (InputStream in) throws IOException {
        return fromXML(new InputStreamReader(in));
    }

    private static Clock fromXML (Reader reader) throws IOException {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(reader);
            int eventType = xpp.getEventType();
            Clock clock = null;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if(eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().equalsIgnoreCase("clock"))
                        clock = new Clock();
                    else if (xpp.getName().equalsIgnoreCase("STATIC_CLOCK")) {
                        xpp.next();
                        if(!xpp.getName().equalsIgnoreCase("id"))
                            return null;
                        String id = xpp.nextText();
                        for(StaticClock sclock : STATIC_CLOCKS) {
                            if (sclock.getId().equals(id)) {
                                return sclock;
                            }
                        }
                        return null;
                    } else if(clock != null)
                        if(xpp.getName().equalsIgnoreCase("title"))
                            clock.title = xpp.nextText();
                        else if(xpp.getName().equalsIgnoreCase("id"))
                            clock.id = xpp.nextText();
                        else if(xpp.getName().equalsIgnoreCase("author"))
                            clock.author = xpp.nextText();
                        else if(xpp.getName().equalsIgnoreCase("description"))
                            clock.description = xpp.nextText();
                        else if(xpp.getName().equalsIgnoreCase("file")) {
                            if("true".equalsIgnoreCase(xpp.getAttributeValue(null, "copyOnly")))
                                clock.copyOnlyFiles.add(xpp.nextText());
                            else
                                clock.files.add(xpp.nextText());
                        } else if(xpp.getName().equalsIgnoreCase("activate"))
                            clock.activate = Integer.parseInt(xpp.nextText());
                        else if(xpp.getName().equalsIgnoreCase("device"))
                            clock.device = xpp.nextText().equalsIgnoreCase("G2") ? Device.G2 : Device.G3;
                        else if(xpp.getName().startsWith("options-clock"))
                            clock.options.put(xpp.getName().substring(8), parseOptions(xpp));
                        else if(xpp.getName().equalsIgnoreCase("date-display"))
                            clock.options.put("date-display", parseOptions(xpp));

                }
                eventType = xpp.next();
            }
            return clock;
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        throw new IOException("Invalid clock.xml");
    }

    private static HashMap<String, String> parseOptions(XmlPullParser xpp) throws IOException {
        HashMap<String, String> options = new HashMap<String, String>();

        int eventType = 0;
        try {
            eventType = xpp.next();
            while (eventType != XmlPullParser.END_TAG) {
                if(eventType == XmlPullParser.START_TAG) {
                    String option = xpp.getName();
                    String value = xpp.nextText();
                    options.put(option, value);

                    xpp.next();
                }
                eventType = xpp.next();
            }
            return options;
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        throw new IOException("Invalid clock.xml");
    }

    public String toXML () {
        StringBuilder sb = new StringBuilder();
        sb.append(
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
        "<clock>\n" +
        "\t<title>" + title + "</title>\n" +
        "\t<id>" + id + "</id>\n" +
        "\t<author>" + author + "</author>\n" +
        "\t<description>" + description + "</description>\n" +
        "\t<activate>" + activate + "</activate>\n" +
        "\t<device>" + device.name() + "</device>\n" +
        "\t<replaces>\n");
        for (String file : files)
            sb.append("\t\t<file>" + file + "</file>\n");

        for (Map.Entry<String, HashMap<String, String>> entry : options.entrySet()) {
            String key;
            if (entry.getKey().equalsIgnoreCase("date-display"))
                key = entry.getKey();
            else
                key = "options-" + entry.getKey();

            sb.append("\t<" + key + ">\n");
            for(Map.Entry<String, String> option : entry.getValue().entrySet()) {
                sb.append("\t\t<"+option.getKey()+">" + option.getValue() + "</"+option.getKey()+">\n");
            }
            sb.append("\t</" + key + ">\n");
        }
        sb.append(
        "\t</replaces>\n" +
        "</clock>");

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Clock clock = (Clock) o;

        if (!id.equals(clock.id)) return false;

        return true;
    }

    public static Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    protected static Drawable loadDrawable(Context context, String path) {
        return loadDrawable(context, path, false);
    }

    protected static Drawable loadDrawable(Context context, String path, boolean round) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDensity = dm.densityDpi;
        options.inScreenDensity = dm.densityDpi;
        options.inTargetDensity = dm.densityDpi;

        Bitmap bmp = BitmapFactory.decodeFile(path, options);
        if(round) {
            bmp = getCroppedBitmap(bmp);
        }
        return new BitmapDrawable(context.getResources(), bmp);
    }

    public Drawable getHourDrawable(Context context) {
        return getDrawable(context, "hour");
    }

    public Drawable getMinuteDrawable(Context context) {
        return getDrawable(context, "minute");
    }

    public Drawable getSecondDrawable(Context context) {
        return getDrawable(context, "second");
    }

    public Drawable getBackgroundDrawable(Context context) {
        return getDrawable(context, "bg", true);
    }

    public Drawable getDayBackgroundDrawable(Context context) {
        return getDrawable(context, "day_bg");
    }

    public Drawable getDrawable(Context context, String name) {
        return getDrawable(context, name, false);
    }

    public Drawable getDrawable(Context context, String name, boolean round) {
        String file = findFile(name + ".png");
        if(file != null) {
            return loadDrawable(context, context.getFilesDir() + "/"
                    + getId() + "/" + file, round);
        }
        return null;
    }

    public File getFile(Context context, String name) {
        String file = findFile(name);
        if(file != null) {
            return new File(context.getFilesDir() + "/"
                    + getId() + "/" + file);
        }
        return null;
    }

    private String findFile(String name) {
        if(files.contains("b2_quickcircle_analog_style01_" + name)) {
            return "b2_quickcircle_analog_style01_" + name;
        } else if(files.contains("b2_quickcircle_analog_style02_" + name)) {
            return "b2_quickcircle_analog_style02_" + name;
        } else if(files.contains("b2_quickcircle_analog_style03_" + name)) {
            return "b2_quickcircle_analog_style03_" + name;
        } else if(files.contains(name)) {
            return name;
        }
        return null;
    }

    public Fragment getFragment() {
        return CoverAnalogClockFragment_.builder().clock(this).build();
    }

    @Override
    public int hashCode() {
        return getId().hashCode() + 17;
    }

    public abstract static class StaticClock extends Clock {

        @Override
        public Device getDevice() {
            return Device.G3;
        }

        @Override
        public List<String> getFiles() {
            return new ArrayList<>(0);
        }

        @Override
        public List<String> getCopyOnlyFiles() {
            return new ArrayList<>(0);
        }

        @Override
        public Drawable getHourDrawable(Context context) {
            return null;
        }

        @Override
        public Drawable getMinuteDrawable(Context context) {
            return null;
        }

        @Override
        public Drawable getSecondDrawable(Context context) {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            return o == this;
        }

        @Override
        public HashMap<String, HashMap<String, String>> getOptions() {
            return new HashMap<>(0);
        }

        @Override
        public String toXML() {
            return "<?xml version=\"1.0\" encoding=\"utf-8\"?><STATIC_CLOCK><id>" + getId() + "</id></STATIC_CLOCK>";
        }
    }

    public static final StaticClock DIGITAL_CLOCK = new StaticClock() {
        @Override
        public String getTitle() {
            return "Digital Clock";
        }

        @Override
        public String getId() {
            return "de.bigboot.qcircleview.digital";
        }

        @Override
        public String getAuthor() {
            return "Chabino.G";
        }

        @Override
        public String getDescription() {
            return "A digital clock with customizable Background";
        }

        @Override
        public Drawable getBackgroundDrawable(Context context) {
            File f = new File(context.getFilesDir() + "/" + "/digital_background");
            if (!f.exists())
                return null;
            return loadDrawable(context, f.getAbsolutePath(), true);
        }

        @Override
        public Fragment getFragment() {
            return CoverDigitalClockFragment_.builder().build();
        }

        @Override
        public int getActivate() {
            return -1;
        }
    };
    public static final StaticClock WEATHER_CLOCK = new StaticClock() {
        @Override
        public String getTitle() {
            return "Weather Clock";
        }

        @Override
        public String getId() {
            return "de.bigboot.qcircleview.weather";
        }

        @Override
        public String getAuthor() {
            return "Chabino.G";
        }

        @Override
        public String getDescription() {
            return "A digital clock with automatic Background according to the weather report";
        }

        @Override
        public Drawable getBackgroundDrawable(Context context) {

            return null;
        }

        @Override
        public int getActivate() {
            return -1;
        }
        @Override
        public Fragment getFragment() {
            return CoverWeatherClockFragment_.builder().build();
        }
    };

    public static final StaticClock[] STATIC_CLOCKS = new StaticClock[] {
            DIGITAL_CLOCK,
            WEATHER_CLOCK
    };
}
