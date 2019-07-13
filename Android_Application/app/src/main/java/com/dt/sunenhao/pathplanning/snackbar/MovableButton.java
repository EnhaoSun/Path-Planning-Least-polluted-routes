package com.dt.sunenhao.pathplanning.snackbar;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;

@CoordinatorLayout.DefaultBehavior(MoveUpwardBehavior.class)
public class MovableButton extends android.support.v7.widget.AppCompatButton {

    public MovableButton(Context context) {
        super(context);
    }

    public MovableButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MovableButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
