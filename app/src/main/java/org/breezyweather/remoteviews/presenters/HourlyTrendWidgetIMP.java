package org.breezyweather.remoteviews.presenters;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import org.breezyweather.BreezyWeather;
import org.breezyweather.common.basic.models.Location;
import org.breezyweather.common.basic.models.options.unit.TemperatureUnit;
import org.breezyweather.common.basic.models.weather.Hourly;
import org.breezyweather.common.basic.models.weather.Weather;
import org.breezyweather.theme.ThemeManager;
import org.breezyweather.theme.resource.ResourceHelper;
import org.breezyweather.theme.resource.ResourcesProviderFactory;
import org.breezyweather.theme.resource.providers.ResourceProvider;
import org.breezyweather.theme.weatherView.WeatherViewController;
import org.breezyweather.R;
import org.breezyweather.background.receiver.widget.WidgetTrendHourlyProvider;
import org.breezyweather.common.utils.DisplayUtils;
import org.breezyweather.common.utils.helpers.AsyncHelper;
import org.breezyweather.remoteviews.trend.TrendLinearLayout;
import org.breezyweather.remoteviews.trend.WidgetItemView;
import org.breezyweather.settings.SettingsManager;

public class HourlyTrendWidgetIMP extends AbstractRemoteViewsPresenter {

    public static void updateWidgetView(Context context, Location location) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            innerUpdateWidget(context, location);
            return;
        }

        AsyncHelper.runOnIO(() -> innerUpdateWidget(context, location));
    }

    @WorkerThread
    private static void innerUpdateWidget(Context context, Location location) {
        WidgetConfig config = getWidgetConfig(
                context,
                context.getString(R.string.sp_widget_hourly_trend_setting)
        );
        if (config.cardStyle.equals("none")) {
            config.cardStyle = "light";
        }

        AppWidgetManager.getInstance(context).updateAppWidget(
                new ComponentName(context, WidgetTrendHourlyProvider.class),
                getRemoteViews(
                        context, location,
                        DisplayUtils.getTabletListAdaptiveWidth(
                                context,
                                context.getResources().getDisplayMetrics().widthPixels
                        ),
                        config.cardStyle, config.cardAlpha
                )
        );
    }

    @WorkerThread @Nullable
    @SuppressLint({"InflateParams", "WrongThread"})
    private static View getDrawableView(Context context, Location location, boolean lightTheme) {
        Weather weather = location.getWeather();
        if (weather == null) {
            return null;
        }

        ResourceProvider provider = ResourcesProviderFactory.getNewInstance();

        int itemCount = Math.min(5, weather.getHourlyForecast().size());
        Float[] temperatures;
        Integer highestTemperature = null;
        Integer lowestTemperature = null;

        boolean minimalIcon = SettingsManager.getInstance(context).isWidgetMinimalIconEnabled();
        TemperatureUnit temperatureUnit = SettingsManager.getInstance(context).getTemperatureUnit();

        temperatures = new Float[Math.max(0, itemCount * 2 - 1)];
        for (int i = 0; i < temperatures.length; i += 2) {
            temperatures[i] = weather.getHourlyForecast().get(i / 2).getTemperature() != null &&
                    weather.getHourlyForecast().get(i / 2).getTemperature().getTemperature() != null ?
                    Float.valueOf(weather.getHourlyForecast().get(i / 2).getTemperature().getTemperature())
            : null;
        }
        for (int i = 1; i < temperatures.length; i += 2) {
            if (temperatures[i - 1] != null && temperatures[i + 1] != null) {
                temperatures[i] = (temperatures[i - 1] + temperatures[i + 1]) * 0.5F;
            } else {
                temperatures[i] = null;
            }
        }

        if (weather.getYesterday() != null) {
            if (weather.getYesterday().getDaytimeTemperature() != null) {
                highestTemperature = weather.getYesterday().getDaytimeTemperature();
            }
            if (weather.getYesterday().getNighttimeTemperature() != null) {
                lowestTemperature = weather.getYesterday().getNighttimeTemperature();
            }
        }
        for (int i = 0; i < itemCount; i++) {
            if (weather.getHourlyForecast().get(i).getTemperature() != null && weather.getHourlyForecast().get(i).getTemperature().getTemperature() != null) {
                if (highestTemperature == null || weather.getHourlyForecast().get(i).getTemperature().getTemperature() > highestTemperature) {
                    highestTemperature = weather.getHourlyForecast().get(i).getTemperature().getTemperature();
                }
                if (lowestTemperature == null || weather.getHourlyForecast().get(i).getTemperature().getTemperature() < lowestTemperature) {
                    lowestTemperature = weather.getHourlyForecast().get(i).getTemperature().getTemperature();
                }
            }
        }

        View drawableView = LayoutInflater.from(context)
                .inflate(R.layout.widget_trend_hourly, null, false);
        if (weather.getYesterday() != null) {
            TrendLinearLayout trendParent = drawableView.findViewById(R.id.widget_trend_hourly);
            trendParent.setData(
                    new Integer[] {
                            weather.getYesterday().getDaytimeTemperature(),
                            weather.getYesterday().getNighttimeTemperature()
                    },
                    highestTemperature,
                    lowestTemperature,
                    temperatureUnit,
                    false
            );
            trendParent.setColor(lightTheme);
        }
        WidgetItemView[] items;
        if (itemCount == 5) {
            items = new WidgetItemView[] {
                    drawableView.findViewById(R.id.widget_trend_hourly_item_1),
                    drawableView.findViewById(R.id.widget_trend_hourly_item_2),
                    drawableView.findViewById(R.id.widget_trend_hourly_item_3),
                    drawableView.findViewById(R.id.widget_trend_hourly_item_4),
                    drawableView.findViewById(R.id.widget_trend_hourly_item_5)
            };
        } else if (itemCount == 4) {
            items = new WidgetItemView[] {
                    drawableView.findViewById(R.id.widget_trend_hourly_item_1),
                    drawableView.findViewById(R.id.widget_trend_hourly_item_2),
                    drawableView.findViewById(R.id.widget_trend_hourly_item_3),
                    drawableView.findViewById(R.id.widget_trend_hourly_item_4)
            };
        } else if (itemCount == 3) {
            items = new WidgetItemView[] {
                    drawableView.findViewById(R.id.widget_trend_hourly_item_1),
                    drawableView.findViewById(R.id.widget_trend_hourly_item_2),
                    drawableView.findViewById(R.id.widget_trend_hourly_item_3)
            };
        } else if (itemCount == 2) {
            items = new WidgetItemView[] {
                    drawableView.findViewById(R.id.widget_trend_hourly_item_1),
                    drawableView.findViewById(R.id.widget_trend_hourly_item_2)
            };
        } else if (itemCount == 1) {
            items = new WidgetItemView[] {
                    drawableView.findViewById(R.id.widget_trend_hourly_item_1)
            };
        } else {
            items = new WidgetItemView[] {};
        }
        int[] colors = ThemeManager.getInstance(context).getWeatherThemeDelegate().getThemeColors(
                context, WeatherViewController.getWeatherKind(weather), location.isDaylight()
        );
        for (int i = 0; i < items.length; i++) {
            Hourly hourly = weather.getHourlyForecast().get(i);

            items[i].setTitleText(hourly.getHour(context, location.getTimeZone()));
            items[i].setSubtitleText(null);

            if (hourly.getWeatherCode() != null) {
                items[i].setTopIconDrawable(
                        ResourceHelper.getWidgetNotificationIcon(
                                provider, hourly.getWeatherCode(), hourly.isDaylight(), minimalIcon, lightTheme
                        )
                );
            }

            if (hourly.getTemperature() != null) {
                items[i].getTrendItemView().setData(
                        buildTemperatureArrayForItem(temperatures, i),
                        null,
                        hourly.getTemperature().getShortTemperature(context, temperatureUnit),
                        null,
                        highestTemperature != null ? Float.valueOf(highestTemperature) : null,
                        lowestTemperature != null ? Float.valueOf(lowestTemperature) : null,
                        null, null, null, null
                );
            }
            items[i].getTrendItemView().setLineColors(
                    colors[1], colors[2],
                    lightTheme
                            ? ColorUtils.setAlphaComponent(Color.BLACK, (int) (255 * 0.05))
                            : ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.1))
            );
            items[i].getTrendItemView().setShadowColors(colors[1], colors[2], lightTheme);
            items[i].getTrendItemView().setTextColors(
                    lightTheme
                            ? ContextCompat.getColor(context, R.color.colorTextDark)
                            : ContextCompat.getColor(context, R.color.colorTextLight),
                    lightTheme
                            ? ContextCompat.getColor(context, R.color.colorTextDark2nd)
                            : ContextCompat.getColor(context, R.color.colorTextLight2nd),
                    lightTheme
                            ? ContextCompat.getColor(context, R.color.colorTextGrey2nd)
                            : ContextCompat.getColor(context, R.color.colorTextGrey)
            );
            items[i].getTrendItemView().setHistogramAlpha(lightTheme ? 0.2f : 0.5f);

            items[i].setBottomIconDrawable(null);

            items[i].setColor(lightTheme);
        }

        return drawableView;
    }

    @SuppressLint("WrongThread")
    @WorkerThread
    private static RemoteViews getRemoteViews(Context context, @Nullable View drawableView,
                                              Location location, int width,
                                              int cardAlpha, String cardStyle) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_remote);
        if (drawableView == null) {
            return views;
        }

        WidgetItemView[] items = new WidgetItemView[] {
                drawableView.findViewById(R.id.widget_trend_hourly_item_1),
                drawableView.findViewById(R.id.widget_trend_hourly_item_2),
                drawableView.findViewById(R.id.widget_trend_hourly_item_3),
                drawableView.findViewById(R.id.widget_trend_hourly_item_4),
                drawableView.findViewById(R.id.widget_trend_hourly_item_5),
        };
        for (WidgetItemView i : items) {
            i.setSize(width / 5f);
        }
        drawableView.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        drawableView.layout(
                0,
                0,
                drawableView.getMeasuredWidth(),
                drawableView.getMeasuredHeight()
        );

        Bitmap cache = Bitmap.createBitmap(
                drawableView.getMeasuredWidth(),
                drawableView.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(cache);
        drawableView.draw(canvas);

        views.setImageViewBitmap(R.id.widget_remote_drawable, cache);
        views.setViewVisibility(R.id.widget_remote_progress, View.GONE);

        WidgetColor.ColorType colorType;
        switch (cardStyle) {
            case "light":
                colorType = WidgetColor.ColorType.LIGHT;
                break;

            case "dark":
                colorType = WidgetColor.ColorType.DARK;
                break;

            default:
                colorType = WidgetColor.ColorType.AUTO;
                break;
        }
        views.setImageViewResource(
                R.id.widget_remote_card,
                getCardBackgroundId(colorType)
        );
        views.setInt(
                R.id.widget_remote_card,
                "setImageAlpha",
                (int) (cardAlpha / 100.0 * 255)
        );

        setOnClickPendingIntent(context, views, location);

        return views;
    }

    @WorkerThread
    public static RemoteViews getRemoteViews(Context context, Location location,
                                             int width, String cardStyle, int cardAlpha) {
        boolean lightTheme;
        switch (cardStyle) {
            case "light":
                lightTheme = true;
                break;

            case "dark":
                lightTheme = false;
                break;

            default:
                lightTheme = location.isDaylight();
                break;
        }
        return getRemoteViews(
                context,
                getDrawableView(context, location, lightTheme),
                location,
                width,
                cardAlpha,
                cardStyle
        );
    }

    public static boolean isEnable(Context context) {
        int[] widgetIds = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, WidgetTrendHourlyProvider.class));
        return widgetIds != null && widgetIds.length > 0;
    }

    private static Float[] buildTemperatureArrayForItem(Float[] temps, int index) {
        Float[] a = new Float[3];
        a[1] = temps[2 * index];
        if (2 * index - 1 < 0) {
            a[0] = null;
        } else {
            a[0] = temps[2 * index - 1];
        }
        if (2 * index + 1 >= temps.length) {
            a[2] = null;
        } else {
            a[2] = temps[2 * index + 1];
        }
        return a;
    }

    private static void setOnClickPendingIntent(Context context, RemoteViews views,
                                                Location location) {
        views.setOnClickPendingIntent(
                R.id.widget_remote_drawable,
                getWeatherPendingIntent(
                        context,
                        location,
                        BreezyWeather.WIDGET_TREND_HOURLY_PENDING_INTENT_CODE_WEATHER
                )
        );
    }
}
