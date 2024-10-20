package org.breezyweather.daily.adapter.holder;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import org.breezyweather.common.basic.models.weather.AirQuality;
import org.breezyweather.common.ui.widgets.RoundProgress;
import org.breezyweather.R;
import org.breezyweather.daily.adapter.DailyWeatherAdapter;
import org.breezyweather.daily.adapter.model.DailyAirQuality;

public class AirQualityHolder extends DailyWeatherAdapter.ViewHolder {

    private final RoundProgress mProgress;
    private final TextView mContent;

    public AirQualityHolder(ViewGroup parent) {
        super(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weather_daily_air, parent, false));
        mProgress = itemView.findViewById(R.id.item_weather_daily_air_progress);
        mContent = itemView.findViewById(R.id.item_weather_daily_air_content);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindView(DailyWeatherAdapter.ViewModel model, int position) {
        AirQuality airQuality = ((DailyAirQuality) model).getAirQuality();

        int aqi = airQuality.getIndex(null);
        int color = airQuality.getColor(itemView.getContext(), null);

        mProgress.setMax(400);
        mProgress.setProgress(aqi);
        mProgress.setProgressColor(color);
        mProgress.setProgressBackgroundColor(
                ColorUtils.setAlphaComponent(color, (int) (255 * 0.1))
        );

        mContent.setText(aqi + " / " + airQuality.getName(itemView.getContext(), null));
    }
}