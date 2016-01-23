package de.bigboot.qcircleview.cover;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.joanzapata.iconify.widget.IconTextView;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.Receiver;
import org.androidannotations.annotations.ViewById;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.bigboot.qcircleview.Preferences;
import de.bigboot.qcircleview.R;


@EFragment(R.layout.clock_weather)
public class CoverWeatherClockFragment extends Fragment{
    private static final String OPEN_WEATHER_MAP_NAME =
            "http://api.openweathermap.org/data/2.5/weather?q=%s&units=%s&lang=%s";
    private static final String OPEN_WEATHER_MAP_COORD =
            "http://api.openweathermap.org/data/2.5/weather?lon=%s&lat=%s&units=%s&lang=%s";

    private static WeatherData weatherCache = null;
    private static long lastUpdate = 0;

    @ViewById(R.id.weather_icon)
    protected IconTextView weatherIcon;
    @ViewById(R.id.city)
    protected TextView city;
    @ViewById(R.id.current_temperature)
    protected TextView temperature;
    @ViewById(R.id.date)
    protected TextView date;
    @ViewById(R.id.content)
    protected FrameLayout content;
    @ViewById(R.id.description)
    protected TextView description;
    @ViewById(R.id.refresh_icon)
    protected IconTextView refreshIcon;
    @ViewById(R.id.refreshing_icon)
    protected IconTextView refreshingIcon;
    @ViewById(R.id.locationIcon)
    protected IconTextView locationIcon;

    protected Preferences preferences;
    protected LocationProvider locationProvider;


    private enum RefreshState {
        Automatic,
        Manual,
        Refreshing
    }

    private void setRefreshState(RefreshState state) {
        switch (state) {
            case Automatic:
                refreshIcon.setVisibility(View.GONE);
                refreshingIcon.setVisibility(View.GONE);
                locationIcon.setVisibility(View.VISIBLE);
                break;
            case Manual:
                refreshIcon.setVisibility(View.VISIBLE);
                refreshingIcon.setVisibility(View.GONE);
                locationIcon.setVisibility(View.GONE);
                break;
            case Refreshing:
                refreshIcon.setVisibility(View.GONE);
                refreshingIcon.setVisibility(View.VISIBLE);
                locationIcon.setVisibility(View.GONE);
                break;
        }
    }

    @AfterViews
    protected void init() {
        String timeStamp = SimpleDateFormat.getDateInstance().format(new Date());
        date.setText(timeStamp);
        setRefreshState(preferences.getWeatherRefreshInterval() == -1 ? RefreshState.Manual : RefreshState.Automatic);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = new Preferences(getActivity());
        locationProvider = new LocationProvider(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (weatherCache == null || preferences.weatherCheckForceRefresh()) {
            updateWeatherData();
        } else if (preferences.getWeatherRefreshInterval() >= 0 &&
                preferences.getWeatherRefreshInterval() + lastUpdate >= System.currentTimeMillis()) {
                updateWeatherData();
        } else {
            renderWeather(weatherCache);
        }
    }

    @Click(R.id.refresh_icon)
    protected void onRefreshClicked() {
        updateWeatherData();
    }

    protected void updateWeatherData() {
        setRefreshState(RefreshState.Refreshing);
        if (preferences.getBoolean(Preferences.BooleanSettings.WeatherUsingGPS)) {
            locationProvider.getCurrentLocation(new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null)
                        loadWeatherData(getActivity(), location.getLongitude(), location.getLatitude());
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });
        } else {
            loadWeatherData(getActivity(), preferences.getWeatherLocation());
        }
    }

    public void renderWeather(WeatherData weatherData) {
        if (System.currentTimeMillis() - lastUpdate <= 1000) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setRefreshState(preferences.getWeatherRefreshInterval() == -1 ? RefreshState.Manual : RefreshState.Automatic);
                }
            }, 1000l + System.currentTimeMillis() - lastUpdate);
        } else {
            setRefreshState(preferences.getWeatherRefreshInterval() == -1 ? RefreshState.Manual : RefreshState.Automatic);
        }

        if(!weatherData.isValid()) {
            return;
        }

        weatherCache = weatherData;
        try {
            setWeatherIcon(weatherData);

            temperature.setText(Math.round(weatherData.getTemperature()) + "°" + preferences.getWeatherUnit());
            description.setText(getActivity().getResources().getText(weatherData.getWeatherCondition().description));
            city.setText(weatherData.getCityName() + ", " + weatherData.getCountryCode());

        } catch (Exception e) {
            Log.e("SimpleWeather", "One or more fields not found in the JSON data");
        }
    }

    public void loadWeatherData(Context context, String city) {
        if (context == null)
            return;

        lastUpdate = System.currentTimeMillis();

        String url = String.format(OPEN_WEATHER_MAP_NAME,
                city,
                "F".equals(preferences.getWeatherUnit()) ? "imperial" : "metric",
                Locale.getDefault().getLanguage());
        Ion.with(context)
                .load(url)
                .addHeader("x-api-key", context.getString(R.string.open_weather_maps_app_id))
                .asJsonObject()
                .withResponse()
                .setCallback(new FutureCallback<Response<JsonObject>>() {
                    @Override
                    public void onCompleted(Exception e, Response<JsonObject> result) {
                        if (e == null) {
                            WeatherData weatherData = WeatherData.fromJson(result.getResult());
                            renderWeather(weatherData);
                        }
                    }
                });
    }

    public void loadWeatherData(Context context, double lon, double lat) {
        if (context == null)
            return;

        lastUpdate = System.currentTimeMillis();

        String url = String.format(OPEN_WEATHER_MAP_COORD,
                lon, lat,
                "F".equals(preferences.getWeatherUnit()) ? "imperial" : "metric",
                Locale.getDefault().getLanguage());
        Ion.with(context)
                .load(url)
                .addHeader("x-api-key", context.getString(R.string.open_weather_maps_app_id))
                .asJsonObject()
                .withResponse()
                .setCallback(new FutureCallback<Response<JsonObject>>() {
                    @Override
                    public void onCompleted(Exception e, Response<JsonObject> result) {
                        if (e == null) {
                            WeatherData weatherData = WeatherData.fromJson(result.getResult());
                            renderWeather(weatherData);
                        }
                    }
                });
    }

    private static class WeatherData {
        private long cityId = -1;
        private String cityName  = "";
        private String countryCode = "";
        private double lon = 0f, lat = 0f;
        private double temperature = 0f, temp_min = 0f, temp_max = 0f;
        private int pressure = 0, humidity = 0;
        private long sunrise = 0l, sunset = 0l;
        private WeatherCondition weatherCondition = WeatherCondition.CLEAR_SKY;

        private WeatherData() {

        }

        public long getCityId() {
            return cityId;
        }

        public String getCityName() {
            return cityName;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public double getLon() {
            return lon;
        }

        public double getLat() {
            return lat;
        }

        public double getTemperature() {
            return temperature;
        }

        public double getPressure() {
            return pressure;
        }

        public double getHumidity() {
            return humidity;
        }

        public double getTemp_min() {
            return temp_min;
        }

        public double getTemp_max() {
            return temp_max;
        }

        public long getSunrise() {
            return sunrise;
        }

        public long getSunset() {
            return sunset;
        }

        public boolean isValid() {
            return cityId >= 0;
        }

        public WeatherCondition getWeatherCondition() {
            return weatherCondition;
        }

        public static WeatherData fromJson(JsonObject json) {
            WeatherData data = new WeatherData();

            try {
                data.cityName = json.get("name").getAsString().toUpperCase(Locale.getDefault());

                JsonObject sys = json.getAsJsonObject("sys");
                data.countryCode = sys.get("country").getAsString();
                data.sunrise = sys.get("sunrise").getAsLong();
                data.sunset = sys.get("sunset").getAsLong();

                JsonObject weather = json.getAsJsonArray("weather").get(0).getAsJsonObject();
                data.weatherCondition = WeatherCondition.byId(weather.get("id").getAsInt());

                JsonObject coord = json.getAsJsonObject("coord");
                data.lon = coord.get("lon").getAsDouble();
                data.lat = coord.get("lat").getAsDouble();

                JsonObject main = json.getAsJsonObject("main");
                data.temperature = main.get("temp").getAsDouble();
                data.pressure = main.get("pressure").getAsInt();
                data.humidity = main.get("humidity").getAsInt();
                data.temp_min = main.get("temp_min").getAsDouble();
                data.temp_max = main.get("temp_max").getAsDouble();

                data.cityId = sys.get("id").getAsLong();
            } catch (Exception ex) {
                Log.d("WeatherClock", "Invalid response from weather provider", ex);
            }
            return data;
        }
    }

    public enum WeatherCondition {
        THUNDERSTORM_WITH_LIGHT_RAIN(200, R.string.thunderstorm_with_light_rain, "wi-day-thunderstorm", "wi-night-alt-thunderstorm"),
        THUNDERSTORM_WITH_RAIN(201, R.string.thunderstorm_with_rain, "wi-day-thunderstorm", "wi-night-alt-thunderstorm"),
        THUNDERSTORM_WITH_HEAVY_RAIN(202, R.string.thunderstorm_with_heavy_rain, "wi-day-thunderstorm", "wi-night-alt-thunderstorm"),
        LIGHT_THUNDERSTORM(210, R.string.light_thunderstorm, "wi-day-lightning", "wi-night-lightning"),
        THUNDERSTORM(211, R.string.thunderstorm, "wi-day-lightning", "wi-night-lightning"),
        HEAVY_THUNDERSTORM(212, R.string.heavy_thunderstorm, "wi-day-lightning", "wi-night-lightning"),
        RAGGED_THUNDERSTORM(221, R.string.ragged_thunderstorm, "wi-day-lightning", "wi-night-lightning"),
        THUNDERSTORM_WITH_LIGHT_DRIZZLE(230, R.string.thunderstorm_with_light_drizzle, "wi-day-storm-showers", "wi-night-alt-storm-showers"),
        THUNDERSTORM_WITH_DRIZZLE(231, R.string.thunderstorm_with_drizzle, "wi-day-storm-showers", "wi-night-alt-storm-showers"),
        THUNDERSTORM_WITH_HEAVY_DRIZZLE(232, R.string.thunderstorm_with_heavy_drizzle, "wi-day-storm-showers", "wi-night-alt-storm-showers"),
        LIGHT_INTENSITY_DRIZZLE(300, R.string.light_intensity_drizzle, "wi-day-showers", "wi-night-alt-showers"),
        DRIZZLE(301, R.string.drizzle, "wi-day-showers", "wi-night-alt-showers"),
        HEAVY_INTENSITY_DRIZZLE(302, R.string.heavy_intensity_drizzle, "wi-day-showers", "wi-night-alt-showers"),
        LIGHT_INTENSITY_DRIZZLE_RAIN(310, R.string.light_intensity_drizzle_rain, "wi-day-rain-mix", "wi-night-alt-rain-mix"),
        DRIZZLE_RAIN(311, R.string.drizzle_rain, "wi-day-rain-mix", "wi-night-alt-rain-mix"),
        HEAVY_INTENSITY_DRIZZLE_RAIN(312, R.string.heavy_intensity_drizzle_rain, "wi-day-rain-mix", "wi-night-alt-rain-mix"),
        SHOWER_RAIN_AND_DRIZZLE(313, R.string.shower_rain_and_drizzle, "wi-day-rain-mix", "wi-night-alt-rain-mix"),
        HEAVY_SHOWER_RAIN_AND_DRIZZLE(314, R.string.heavy_shower_rain_and_drizzle, "wi-day-rain-mix", "wi-night-alt-rain-mix"),
        SHOWER_DRIZZLE(321, R.string.shower_drizzle, "wi-day-rain-mix", "wi-night-alt-rain-mix"),
        LIGHT_RAIN(500, R.string.light_rain, "wi-day-rain", "wi-night-alt-rain"),
        MODERATE_RAIN(501, R.string.moderate_rain, "wi-day-rain", "wi-night-alt-rain"),
        HEAVY_INTENSITY_RAIN(502, R.string.heavy_intensity_rain, "wi-day-rain", "wi-night-alt-rain"),
        VERY_HEAVY_RAIN(503, R.string.very_heavy_rain, "wi-day-rain", "wi-night-alt-rain"),
        EXTREME_RAIN(504, R.string.extreme_rain, "wi-day-rain", "wi-night-alt-rain"),
        FREEZING_RAIN(511, R.string.freezing_rain, "wi-day-sleet", "wi-night-alt-rain"),
        LIGHT_INTENSITY_SHOWER_RAIN(520, R.string.light_intensity_shower_rain, "wi-day-showers", "wi-night-alt-showers"),
        SHOWER_RAIN(521, R.string.shower_rain, "wi-day-showers", "wi-night-alt-showers"),
        HEAVY_INTENSITY_SHOWER_RAIN(522, R.string.heavy_intensity_shower_rain, "wi-day-showers", "wi-night-alt-showers"),
        RAGGED_SHOWER_RAIN(531, R.string.ragged_shower_rain, "wi-day-showers", "wi-night-alt-showers"),
        LIGHT_SNOW(600, R.string.light_snow, "wi-day-snow", "wi-night-alt-snow"),
        SNOW(601, R.string.snow, "wi-day-snow", "wi-night-alt-snow"),
        HEAVY_SNOW(602, R.string.heavy_snow, "wi-day-snow", "wi-night-alt-snow"),
        SLEET(611, R.string.sleet, "wi-day-sleet", "wi-night-alt-sleet"),
        SHOWER_SLEET(612, R.string.shower_sleet, "wi-day-sleet", "wi-night-alt-sleet"),
        LIGHT_RAIN_AND_SNOW(615, R.string.light_rain_and_snow, "wi-day-sleet", "wi-night-alt-sleet"),
        RAIN_AND_SNOW(616, R.string.rain_and_snow, "wi-day-sleet", "wi-night-alt-sleet"),
        LIGHT_SHOWER_SNOW(620, R.string.light_shower_snow, "wi-day-snow", "wi-night-alt-snow"),
        SHOWER_SNOW(621, R.string.shower_snow, "wi-day-snow", "wi-night-alt-snow"),
        HEAVY_SHOWER_SNOW(622, R.string.heavy_shower_snow, "wi-day-snow", "wi-night-alt-snow"),
        MIST(701, R.string.mist, "wi-dust", "wi-dust"),
        SMOKE(711, R.string.smoke, "wi-smoke", "wi-smoke"),
        HAZE(721, R.string.haze, "wi-day-haze", "wi-night-fog"),
        SAND_DUST_WHIRLS(731, R.string.sand_dust_whirls, "wi-sandstorm", "wi-sandstorm"),
        FOG(741, R.string.fog, "wi-day-fog", "wi-night-fog"),
        SAND(751, R.string.sand, "wi-sandstorm", "wi-sandstorm"),
        DUST(761, R.string.dust, "wi-sandstorm", "wi-sandstorm"),
        VOLCANIC_ASH(762, R.string.volcanic_ash, "wi-volcano", "wi-volcano"),
        SQUALLS(771, R.string.squalls, "wi-day-cloudy-gusts", "wi-night-alt-cloudy-gusts"),
        TORNADO(781, R.string.tornado, "wi-tornado", "wi-tornado"),
        CLEAR_SKY(800, R.string.clear_sky, "wi-day-sunny", "wi-night-clear"),
        FEW_CLOUDS(801, R.string.few_clouds, "wi-day-cloudy", "wi-night-alt-cloudy"),
        SCATTERED_CLOUDS(802, R.string.scattered_clouds, "wi-day-cloudy", "wi-night-alt-cloudy"),
        BROKEN_CLOUDS(803, R.string.broken_clouds, "wi-day-cloudy", "wi-night-alt-cloudy"),
        OVERCAST_CLOUDS(804, R.string.overcast_clouds, "wi-day-cloudy", "wi-night-alt-cloudy"),
        EXTREME_TORNADO(900, R.string.extreme_tornado, "wi-tornado", "wi-tornado"),
        EXTREME_TROPICAL_STORM(901, R.string.extreme_tropical_storm, "wi-day-rain-wind", "wi-night-alt-rain-wind"),
        EXTREME_HURRICANE(902, R.string.extreme_hurricane, "wi-hurricane", "wi-hurricane"),
        EXTREME_COLD(903, R.string.extreme_cold, "wi-snowflake-cold", "wi-snowflake-cold"),
        EXTREME_HOT(904, R.string.extreme_hot, "wi-fire", "wi-fire"),
        EXTREME_WINDY(905, R.string.extreme_windy, "wi-strong-wind", "wi-strong-wind"),
        EXTREME_HAIL(906, R.string.extreme_hail, "wi-hail", "wi-hail"),
        ADDITIONAL_CALM(951, R.string.additional_calm, "wi-wind-beaufort-0", "wi-wind-beaufort-0"),
        ADDITIONAL_LIGHT_BREEZE(952, R.string.additional_light_breeze, "wi-wind-beaufort-2", "wi-wind-beaufort-2"),
        ADDITIONAL_GENTLE_BREEZE(953, R.string.additional_gentle_breeze, "wi-wind-beaufort-3", "wi-wind-beaufort-3"),
        ADDITIONAL_MODERATE_BREEZE(954, R.string.additional_moderate_breeze, "wi-wind-beaufort-4", "wi-wind-beaufort-4"),
        ADDITIONAL_FRESH_BREEZE(955, R.string.additional_fresh_breeze, "wi-wind-beaufort-5", "wi-wind-beaufort-5"),
        ADDITIONAL_STRONG_BREEZE(956, R.string.additional_strong_breeze, "wi-wind-beaufort-6", "wi-wind-beaufort-6"),
        ADDITIONAL_HIGH_WIND_NEAR_GALE(957, R.string.additional_high_wind_near_gale, "wi-wind-beaufort-7", "wi-wind-beaufort-7"),
        ADDITIONAL_GALE(958, R.string.additional_gale, "wi-wind-beaufort-8", "wi-wind-beaufort-8"),
        ADDITIONAL_SEVERE_GALE(959, R.string.additional_severe_gale, "wi-wind-beaufort-9", "wi-wind-beaufort-9"),
        ADDITIONAL_STORM(960, R.string.additional_storm, "wi-wind-beaufort-10", "wi-wind-beaufort-10"),
        ADDITIONAL_VIOLENT_STORM(961, R.string.additional_violent_storm, "wi-wind-beaufort-11", "wi-wind-beaufort-11"),
        ADDITIONAL_HURRICANE(962, R.string.additional_hurricane, "wi-wind-beaufort-12", "wi-wind-beaufort-12");

        private static final Map<Integer, WeatherCondition> typesById = new HashMap<>();

        static {
            for (WeatherCondition type : WeatherCondition.values()) {
                typesById.put(type.id, type);
            }
        }

        private final int id;
        private final int description;
        private final String iconDay;
        private final String iconNight;

        WeatherCondition(int id, int description, String iconDay, String iconNight) {
            this.id = id;
            this.description = description;
            this.iconDay = iconDay.replace("-", "_");
            this.iconNight = iconNight.replace("-", "_");
        }

        public static WeatherCondition byId(int id) {
            return typesById.get(id);
        }
    }

    public void setWeatherIcon(WeatherData weather) {

        boolean daytime = false;
        long currentTime = new Date().getTime();
        if (currentTime >= weather.sunrise*1000 && currentTime <= weather.sunset*1000)
            daytime = true;

        if (daytime)
            weatherIcon.setText("{" + weather.weatherCondition.iconDay + "}");
        else
            weatherIcon.setText("{" + weather.weatherCondition.iconNight + "}");


        content.setBackground(getActivity()
                .getResources()
                .getDrawable(getBackground(weather
                        .getWeatherCondition().id, daytime), null));
    }

    private int getBackground(int weatherId, boolean daytime) {
        switch (weatherId) {
            case 801: case 802: case 803:  case 804:
                return daytime ? R.drawable.weather_cloudy_day : R.drawable.weather_cloudy_night;

            case 300: case 301: case 302: case 310: case 311: case 312:
            case 313: case 314: case 321: case 500: case 501: case 502:
            case 503: case 504: case 520: case 521: case 522: case 531:
                return daytime ? R.drawable.weather_rainy_day : R.drawable.weather_rainy_day;

            case 200: case 201: case 202: case 210: case 211: case 212:
            case 221: case 230: case 231: case 232:
                return daytime ? R.drawable.weather_thunder_day : R.drawable.weather_thunder_night;

            case 511: case 600: case 601: case 602: case 611: case 612:
            case 615: case 616: case 620: case 621: case 622:
                return daytime ? R.drawable.weather_snowy_day : R.drawable.weather_snowy_night;

            case 701: case 711: case 721: case 731: case 741: case 751:
            case 761: case 762: case 771: case 781:
                return daytime ? R.drawable.weather_foggy_day : R.drawable.weather_foggy_night;

            case 800:
            default:
                return daytime ? R.drawable.weather_clear_day : R.drawable.weather_clear_night;
        }
    }

    @Receiver(actions = Intent.ACTION_DATE_CHANGED, registerAt = Receiver.RegisterAt.OnAttachOnDetach)
    public void onDateChanged() {
        if (date != null) {
            String timeStamp = SimpleDateFormat.getDateInstance().format(new Date());
            date.setText(timeStamp);
        }
    }





}
