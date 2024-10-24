package org.breezyweather.daily.adapter.model;

import org.breezyweather.daily.adapter.DailyWeatherAdapter;

public class LargeTitle implements DailyWeatherAdapter.ViewModel {

    private String title;

    public LargeTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public static boolean isCode(int code) {
        return code == 0;
    }

    @Override
    public int getCode() {
        return 0;
    }
}
