package com.example.clement.sumito;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

import com.example.clement.sumito.R;

public class DrawView extends View {
    Paint paint = new Paint();
    Canvas canvas = new Canvas();
    Bitmap boules[], plateau;
    float x = 10, y = 20;
    int w, h, width, height;

    public DrawView(Context context) {
        super(context);
        boules = new Bitmap[] {BitmapFactory.decodeResource(getResources(), R.drawable.boulenoire), BitmapFactory.decodeResource(getResources(), R.drawable.boulenoire)};
        plateau = BitmapFactory.decodeResource(getResources(), R.drawable.cadre);
    }

    @Override
    public void onDraw(final Canvas canvas) {
        this.canvas = canvas;
        h = canvas.getHeight();
        w = canvas.getWidth();
        if (1073 * w < 1420 * h) {
            width = w;
            height = 1073 * w / 1420;
        } else {
            height = h;
            width = 1420 * h / 1073;
        }

        boules[0] = Bitmap.createScaledBitmap(boules[0], height / 10, height / 10, true);
        boules[1] = Bitmap.createScaledBitmap(boules[0], height / 10, height / 10, true);
        plateau = Bitmap.createScaledBitmap(plateau, width, height, true);
        canvas.drawBitmap(plateau, (w - width) / 2, (h - height) / 2, null);
        canvas.drawBitmap(boules[0], x - h / 40, y - h / 40, null);
        canvas.drawBitmap(boules[1], x + h / 40, y + h / 40, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        x = event.getRawX();
        y = event.getRawY();
        this.invalidate();
        return true;
    }

}