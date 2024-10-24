package org.breezyweather.daily.adapter.holder;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.breezyweather.R;
import org.breezyweather.daily.adapter.DailyWeatherAdapter;

public class LineHolder extends DailyWeatherAdapter.ViewHolder {

    public LineHolder(ViewGroup parent) {
        super(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weather_daily_line, parent, false));
    }

    @Override
    public void onBindView(DailyWeatherAdapter.ViewModel model, int position) {
        // do nothing.
    }
}
