package org.breezyweather.common.ui.decorations;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Px;
import androidx.recyclerview.widget.RecyclerView;

import org.breezyweather.R;

public class GridMarginsDecoration extends RecyclerView.ItemDecoration {

    private @Px final float mMarginsVertical;
    private @Px final float mMarginsHorizontal;

    public GridMarginsDecoration(Context context, RecyclerView recyclerView) {
        this(context.getResources().getDimensionPixelSize(R.dimen.little_margin), recyclerView);
    }

    public GridMarginsDecoration(@Px float margins, RecyclerView recyclerView) {
        this(margins, margins, recyclerView);
    }

    public GridMarginsDecoration(@Px float marginsVertical, @Px float marginsHorizontal, RecyclerView recyclerView) {
        mMarginsVertical = marginsVertical;
        mMarginsHorizontal = marginsHorizontal;
        recyclerView.setClipToPadding(false);
        recyclerView.setPadding(
                (int) marginsHorizontal / 2,
                (int) marginsVertical / 2,
                (int) marginsHorizontal / 2,
                (int) marginsVertical / 2
        );
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.set(
                (int) (mMarginsHorizontal / 2),
                (int) (mMarginsVertical / 2),
                (int) (mMarginsHorizontal / 2),
                (int) (mMarginsVertical / 2)
        );
    }
}