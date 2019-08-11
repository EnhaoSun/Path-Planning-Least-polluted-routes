package com.dt.sunenhao.pathplanning.snackbar;


import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

@CoordinatorLayout.DefaultBehavior(MoveUpwardBehavior.class)
public class MoveableImageButton extends AppCompatImageButton {
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

