package org.breezyweather.background.interfaces;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.service.quicksettings.Tile;

import androidx.annotation.RequiresApi;

import org.breezyweather.common.basic.models.Location;
import org.breezyweather.common.utils.helpers.IntentHelper;
import org.breezyweather.db.repositories.LocationEntityRepository;
import org.breezyweather.db.repositories.WeatherEntityRepository;
import org.breezyweather.theme.resource.ResourceHelper;
import org.breezyweather.theme.resource.ResourcesProviderFactory;
import org.breezyweather.settings.SettingsManager;

/**
 * Tile service.
 * TODO: Memory leak
 **/
@RequiresApi(api = Build.VERSION_CODES.N)
public class TileService extends android.service.quicksettings.TileService {

    @Override
    public void onTileAdded() {
        refreshTile(this, getQsTile());
    }

    @Override
    public void onTileRemoved() {
        // do nothing.
    }

    @Override
    public void onStartListening () {
        refreshTile(this, getQsTile());
    }

    @Override
    public void onStopListening () {
        refreshTile(this, getQsTile());
    }

    @SuppressLint("WrongConstant")
    @Override
    public void onClick () {
        try {
            Object statusBarManager = getSystemService("statusbar");
            if (statusBarManager != null) {
                statusBarManager
                        .getClass()
                        .getMethod("collapsePanels")
                        .invoke(statusBarManager);
            }
        } catch (Exception ignored) {

        }
        IntentHelper.startMainActivity(this);
    }

    private static void refreshTile(Context context, Tile tile) {
        if (tile == null) {
            return;
        }
        Location location = LocationEntityRepository.INSTANCE.readLocationList(context).get(0);
        location = Location.copy(location, WeatherEntityRepository.INSTANCE.readWeather(location));
        if (location.getWeather() != null && location.getWeather().getCurrent() != null) {
            if (location.getWeather().getCurrent().getWeatherCode() != null) {
                tile.setIcon(
                        ResourceHelper.getMinimalIcon(
                                ResourcesProviderFactory.getNewInstance(),
                                location.getWeather().getCurrent().getWeatherCode(),
                                location.isDaylight()
                        )
                );
            }
            if (location.getWeather().getCurrent().getTemperature() != null) {
                tile.setLabel(
                        location.getWeather().getCurrent().getTemperature().getTemperature(
                                context,
                                SettingsManager.getInstance(context).getTemperatureUnit())
                );
            }
            tile.setState(Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }
}