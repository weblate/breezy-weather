package org.breezyweather.common.ui.widgets.insets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

import org.breezyweather.common.basic.insets.FitBothSideBarHelper;
import org.breezyweather.common.basic.insets.FitBothSideBarView;
import org.breezyweather.R;

public class FitSystemBarNestedScrollView extends NestedScrollView
        implements FitBothSideBarView {

    private final FitBothSideBarHelper mHelper;

    public FitSystemBarNestedScrollView(@NonNull Context context) {
        this(context, null);
    }

    public FitSystemBarNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FitSystemBarNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.FitSystemBarNestedScrollView, defStyleAttr, 0);
        int fitSide = a.getInt(R.styleable.FitSystemBarNestedScrollView_sv_side, SIDE_TOP | SIDE_BOTTOM);
        a.recycle();

        mHelper = new FitBothSideBarHelper(this, fitSide);
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        return mHelper.onApplyWindowInsets(insets);
    }

    @Override
    public boolean fitSystemWindows(Rect insets) {
        return mHelper.fitSystemWindows(insets);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setPadding(0, mHelper.top(), 0, mHelper.bottom());
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void addFitSide(@FitSide int side) {
        mHelper.addFitSide(side);
    }

    @Override
    public void removeFitSide(@FitSide int side) {
        mHelper.removeFitSide(side);
    }

    @Override
    public void setFitSystemBarEnabled(boolean top, boolean bottom) {
        mHelper.setFitSystemBarEnabled(top, bottom);
    }

    @Override
    public int getTopWindowInset() {
        return mHelper.top();
    }

    @Override
    public int getBottomWindowInset() {
        return mHelper.bottom();
    }
}
