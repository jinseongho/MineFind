package me.ingeni.minefind;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

/**
 * Created by jinseongho on 2016. 5. 11..
 */
public class MineGridView extends GridView {

    public MineGridView(Context context) {
        super(context);
    }

    public MineGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MineGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(MEASURED_SIZE_MASK, MeasureSpec.AT_MOST));
        getLayoutParams().height = getMeasuredHeight();
    }
}
