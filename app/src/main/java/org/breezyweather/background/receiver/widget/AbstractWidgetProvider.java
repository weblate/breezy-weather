package org.breezyweather.background.receiver.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

import org.breezyweather.background.polling.PollingManager;

/**
 * Abstract widget provider.
 * */
public abstract class AbstractWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        PollingManager.resetAllBackgroundTask(context, true);
    }
}
