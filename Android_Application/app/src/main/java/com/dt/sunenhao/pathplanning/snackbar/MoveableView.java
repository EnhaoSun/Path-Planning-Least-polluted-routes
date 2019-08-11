package com.dt.sunenhao.pathplanning.snackbar;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

@CoordinatorLayout.DefaultBehavior(MoveUpwardBehavior.class)
public class MoveableView extends AppCompatImageView {
    public MoveableView(Context context) {
        super(context);
    }

    public MoveableView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MoveableView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
