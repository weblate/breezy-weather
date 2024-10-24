package org.breezyweather.db.generators;

import java.util.ArrayList;
import java.util.List;

import org.breezyweather.common.basic.models.options.provider.WeatherSource;
import org.breezyweather.common.basic.models.weather.Minutely;
import org.breezyweather.db.converters.WeatherSourceConverter;
import org.breezyweather.db.entities.MinutelyEntity;

public class MinutelyEntityGenerator {

    public static MinutelyEntity generate(String cityId, WeatherSource source, Minutely minutely) {
        MinutelyEntity entity = new MinutelyEntity();

        entity.cityId = cityId;
        entity.weatherSource = new WeatherSourceConverter().convertToDatabaseValue(source);
        
        entity.date = minutely.getDate();

        entity.weatherCode = minutely.getWeatherCode();
        entity.weatherText = minutely.getWeatherText();

        entity.minuteInterval = minutely.getMinuteInterval();
        entity.dbz = minutely.getDbz();
        entity.cloudCover = minutely.getCloudCover();

        return entity;
    }

    public static List<MinutelyEntity> generate(String cityId, WeatherSource source,
                                                List<Minutely> minutelyList) {
        List<MinutelyEntity> entityList = new ArrayList<>(minutelyList.size());
        for (Minutely minutely : minutelyList) {
            entityList.add(generate(cityId, source, minutely));
        }
        return entityList;
    }

    public static Minutely generate(MinutelyEntity entity) {
        return new Minutely(
                entity.date, entity.weatherText, entity.weatherCode,
                entity.minuteInterval, entity.dbz, entity.cloudCover
        );
    }

    public static List<Minutely> generate(List<MinutelyEntity> entityList) {
        List<Minutely> dailyList = new ArrayList<>(entityList.size());
        for (MinutelyEntity entity : entityList) {
            dailyList.add(generate(entity));
        }
        return dailyList;
    }
}
