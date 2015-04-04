package com.example.clement.sumito;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

public class DrawView extends View {
    Paint paint = new Paint();
    Canvas canvas = new Canvas();
    Bitmap boule;
    float x = 10, y = 20;
    public DrawView(Context context) {
        super(context);
        boule = BitmapFactory.decodeResource(getResources(), R.drawable.boulenoire);
    }

    @Override
    public void onDraw(final Canvas canvas) {
        this.canvas = canvas;
        final int h = canvas.getHeight();
        final int w = canvas.getWidth();
        boule = Bitmap.createScaledBitmap(boule, h/20, h/20, true);
        canvas.drawBitmap(boule, x-h/40, y-h/40, null);
    }

    public void dessinerBoule(final Canvas canvas){
        canvas.drawBitmap(boule, x, y, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        x = event.getRawX();
        y = event.getRawY();
        this.invalidate();
        return true;
    }

}