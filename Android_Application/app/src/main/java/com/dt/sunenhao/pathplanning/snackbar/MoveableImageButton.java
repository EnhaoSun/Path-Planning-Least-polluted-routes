package com.dt.sunenhao.pathplanning.snackbar;


import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;

@CoordinatorLayout.DefaultBehavior(MoveUpwardBehavior.class)
public class MoveableImageButton extends android.support.v7.widget.AppCompatImageButton  {
    public MoveableImageButton(Context context) {
        super(context);
    }

    public MoveableImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MoveableImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}

