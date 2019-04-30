package com.AC.Measure;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.AC.Measure.db.ImageHelper;
import com.AC.Measure.model.Image;
import com.AC.Measure.model.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageView extends SurfaceView implements SurfaceHolder.Callback {

    private final int SEARCH_RANGE = 20;
    private final int DRAG = 1;
    private final int ZOOM = 2;
    private final int NONE = 3;
    private final int FLAG_COUNT = 5;

    private static Point point[] = new Point[4];
    private static boolean holdScalePoint;
    private static float x;
    private static String message;

    private SurfaceHolder holder;
    private Bitmap drawBitmap;
    private int bitmapWidth, bitmapHeight;
    private Matrix matrix;
    private Matrix savedMatrix;
    private Matrix tmpMatrix;
    private Paint pointPaint;

    private float oldDistance, newDistance;

    private Point startPoint = new Point();
    private Point midPoint = new Point();
    private Point indexPoint = new Point();
    private Point currentPoint = new Point();
    private RectF src, dst;
    private int flag;
    private int action = NONE;
    private float scale;
    private long startTime;
    private Context context;

    public ImageView(Context context) {
        super(context);
        this.context = context;
        holder = getHolder();
        holder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) { // 當建立畫布完成後
        tmpMatrix = new Matrix();

        pointPaint = new Paint();
        pointPaint.setColor(Color.RED);
        pointPaint.setStyle(Paint.Style.STROKE);
        pointPaint.setStrokeWidth(5.0f);
        pointPaint.setAntiAlias(true);

        Point bitmapPoint = new Point();
        bitmapPoint.x = 0.0f;
        bitmapPoint.y = 0.0f;

        Canvas canvas = holder.lockCanvas();

        indexPoint.x = canvas.getWidth() / 2.0f;
        indexPoint.y = canvas.getHeight() / 2.0f;

        matrix.setTranslate(bitmapPoint.x, bitmapPoint.y);
        src = new RectF(0.0f, 0.0f, drawBitmap.getWidth(), drawBitmap.getHeight());
        dst = new RectF(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight());
        matrix.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);
        savedMatrix.set(matrix);

        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(drawBitmap, matrix, null);

        drawPoint(canvas);

        holder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { // 當畫布大小被改變後

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) { // 當繪布被釋放後
        point = toBitmapPoint(point);

        matrix.set(savedMatrix);
        matrix.setRectToRect(src, dst, Matrix.ScaleToFit.FILL);

        point = toDispalyPoint(point);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) { // 當繪布被觸碰時
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                startPoint.x = event.getX();
                startPoint.y = event.getY();
                startTime = System.currentTimeMillis();
                flag = FLAG_COUNT - 3;
                action = DRAG;
                break;
            }

            case MotionEvent.ACTION_UP: {
                if (System.currentTimeMillis() - startTime < 150L)
                    onClick(event);
                action = NONE;
                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                oldDistance = getDistance(event);
                if (oldDistance > 10f) {
                    action = ZOOM;
                    savedMatrix.set(matrix);
                    midPoint = pointMid(event);
                }
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                flag = 0;
                action = DRAG;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (action == DRAG)
                    onDrag(event);
                flag++;
                if (action == ZOOM)
                    onZoom(event);
            }

            default:
                onNone(event);
        }
        return true;
    }

    private void onDrag(MotionEvent event) { // 當為拖拉事件時
        matrix.set(savedMatrix);

        currentPoint.x = event.getX();
        currentPoint.y = event.getY();

        if (flag > FLAG_COUNT) {
            point = toBitmapPoint(point);

            matrix.postTranslate((float) (currentPoint.x - startPoint.x),
                    (float) (currentPoint.y - startPoint.y));
            indexPoint.x = indexPoint.x + currentPoint.x - startPoint.x;
            indexPoint.y = indexPoint.y + currentPoint.y - startPoint.y;

            point = toDispalyPoint(point);
        }

        Canvas canvas = holder.lockCanvas();

        canvas.drawColor(Color.BLACK);

        canvas.drawBitmap(drawBitmap, matrix, null);

        drawPoint(canvas);

        holder.unlockCanvasAndPost(canvas);

        startPoint.x = currentPoint.x;
        startPoint.y = currentPoint.y;
        savedMatrix.set(matrix);
    }

    private void onZoom(MotionEvent event) { // 當為放大事件時
        newDistance = getDistance(event);
        if (newDistance > 10.0f) {
            point = toBitmapPoint(point);

            matrix.set(savedMatrix);
            scale = (oldDistance + ((newDistance - oldDistance) / 2.0f)) / oldDistance;
            matrix.postScale(scale, scale, midPoint.x, midPoint.y);
            float distance = getDistance(midPoint, indexPoint);
            float sina = (indexPoint.y - midPoint.y) / distance;
            float cosa = (indexPoint.x - midPoint.x) / distance;
            distance *= scale;
            indexPoint.x = distance * cosa + midPoint.x;
            indexPoint.y = distance * sina + midPoint.y;
            oldDistance = newDistance;

            point = toDispalyPoint(point);

            Canvas canvas = holder.lockCanvas();

            canvas.drawColor(Color.BLACK);

            canvas.drawBitmap(drawBitmap, matrix, null);

            drawPoint(canvas);

            holder.unlockCanvasAndPost(canvas);

            savedMatrix.set(matrix);
        }
    }

    @SuppressLint("NewApi")
    private void onClick(MotionEvent event) { // 當為點擊事件時
        setClickPoint(event.getX(), event.getY());

        Canvas canvas = holder.lockCanvas();

        canvas.drawColor(Color.BLACK);

        canvas.drawBitmap(drawBitmap, matrix, null);

        drawPoint(canvas);

        holder.unlockCanvasAndPost(canvas);

//        if (!holdScalePoint && point[1] != null && x == 0) {
//            insertX();
//        }
//        else
        if (point[3] != null) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("是否計算距離？");
            builder.setPositiveButton("是", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    point = toBitmapPoint(point);
                    float distance1 = getDistance(point[0], point[1]),
                            distance2 = getDistance(point[2], point[3]);
                    point = toDispalyPoint(point);
                    float x = ((Activity)context).getIntent().getExtras().getFloat("Distance_result"), y = distance2 / distance1 * x;

                    String message = "Distance 1 : " + distance1 + " Pixel\n" +
                            "Distance 2 : " + distance2 + " Pixel\n\n" +
                            "Distance 1 : Distance 2 = X : Y\n" +
                            distance1 + " : " + " = " + x + " : " + y + "\n\n";

                    ImageHelper ih = new ImageHelper(context);

                    String first = ((Activity)context).getIntent().getExtras().getString("first");
                    Image image = ih.get(first)[0];
                    image.setResult(first);

                    String imageMessage = image.getMessage();
                    if (context instanceof CannyActivity) {
                        message = "Canny:\n" + message;

                        if (imageMessage.indexOf("Canny") > -1) {
                            if (imageMessage.indexOf("Binary") > -1)
                                imageMessage = message + imageMessage.substring(imageMessage.indexOf("Binary"), imageMessage.length());
                            else
                                imageMessage = message;
                        } else
                            imageMessage = message;
                    } else {
                        message = "Binary:\n" + message;

                        if (imageMessage.indexOf("Canny") > -1) {
                            if (imageMessage.indexOf("Binary") > -1)
                                imageMessage = imageMessage.substring(0, imageMessage.indexOf("Binary")) + message;
                            else
                                imageMessage = imageMessage + message;
                        } else
                            imageMessage = message;
                    }

                    image.setMessage(imageMessage);
                    ih.update(image);

                    ih.close();

                    Toast.makeText(context, "儲存完成", Toast.LENGTH_SHORT).show();

                    point[2] = null;
                    point[3] = null;

                    final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("量測結果");
                    builder.setMessage(message);
                    builder.setPositiveButton("確定", null);
                    builder.create().show();

                    dialog.dismiss();
                }

            });
            builder.setNegativeButton("否", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    point[2] = null;
                    point[3] = null;

                    dialog.dismiss();
                }

            });
            builder.create().show();
        }
    }

    private void onNone(MotionEvent event) { // 當為沒有任何事件時

    }

    @SuppressLint("NewApi")
    private void insertX() { // 輸入 x
        final AlertDialog.Builder builder = new AlertDialog.Builder(context); // 建立互動訊息
        builder.setTitle("請輸入比例X");
        builder.setCancelable(false);

        final EditText et = new EditText(context);

        builder.setView(et);

        builder.setPositiveButton("確定", null);
        builder.setNegativeButton("取消",  null);

        final AlertDialog ad = builder.create();
        ad.setCancelable(false);
        ad.setOnShowListener(new DialogInterface.OnShowListener(){

            @Override
            public void onShow(DialogInterface dialog) {
                // TODO Auto-generated method stub
                Button button = ad.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        // TODO Auto-generated method stub
                        try {
                            x = Float.parseFloat(et.getText().toString());
                            holdScalePoint = true;
                            ad.dismiss();
                        } catch (NumberFormatException e) {
                            // TODO Auto-generated method stub
                            e.printStackTrace();
                            Toast.makeText(context, "請輸入數字", Toast.LENGTH_SHORT).show();
                        }
                    }

                });

                button = ad.getButton(AlertDialog.BUTTON_NEGATIVE);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        // TODO Auto-generated method stub
                        point[0] = null;
                        point[1] = null;
                        ad.dismiss();
                    }

                });
            }

        });
        ad.show();
    }

    private void drawPoint(Canvas canvas) { // 畫出點擊所產生的點
        for (int i=0; i<point.length; i++)
            if (point[i] != null) {
                canvas.drawCircle(point[i].x, point[i].y, 20.0f, pointPaint);

                if ((i+1) % 2 == 0)
                    canvas.drawLine(point[i - 1].x, point[i - 1].y, point[i].x, point[i].y, pointPaint);
            }
    }

    private void setClickPoint(float x, float y) { // 設定觸碰點
        for (int i=0; i<point.length; i++)
            if (point[i] == null) {
                point[i] = getClickPoint(x, y);
                point[i].setIndex(i);

                if ((i + 1) % 2 == 0) {
                    point = toBitmapPoint(point);

                    Point newPoint[] = findEdges(point[i - 1], point[i], Color.BLACK);
                    if (newPoint != null) {
                        point[i - 1] = newPoint[0];
                        point[i - 1].setIndex(i - 1);

                        point[i] = newPoint[1];
                        point[i].setIndex(i);
                    }

                    point = toDispalyPoint(point);
                }

                break;
            }
    }

    private Point getClickPoint(float x, float y) { // 取得觸碰點
        float pts[] = {x, y};
        tmpMatrix.reset();
        matrix.invert(tmpMatrix);
        tmpMatrix.mapPoints(pts);
        Point point = new Point();
        point.x = pts[0];
        point.y = pts[1];
        point = searchNearbyBlackPoint(point);

        tmpMatrix.invert(tmpMatrix);
        pts = new float[]{point.x, point.y};
        tmpMatrix.mapPoints(pts);
        point = new Point();
        point.x = pts[0];
        point.y = pts[1];

        return point;
    }

    private Point[] toBitmapPoint(Point point[]) { // 將觸碰點位置轉換成影像的實際位置
        for (int i=0; i<point.length; i++)
            if (point[i] != null)
                point[i] = getBitmapPoint(point[i]);
            else
                break;

        return point;
    }

    private Point getBitmapPoint(Point point) { // 取得觸碰點在影像的實際位置
        float pts[] = {point.x, point.y};
        tmpMatrix.reset();
        matrix.invert(tmpMatrix);
        tmpMatrix.mapPoints(pts);
        point.x = pts[0];
        point.y = pts[1];
        return point;
    }

    private Point[] toDispalyPoint(Point point[]) {  // 將觸碰點位置轉換成螢幕的實際位置
        for (int i=0; i<point.length; i++)
            if (point[i] != null)
                point[i] = getDisplayPoint(point[i]);
            else
                break;

        return point;
    }

    private Point getDisplayPoint(Point point) { // 取得觸碰點在螢幕的實際位置
        float pts[] = {(int)(point.x + 0.5f), (int)(point.y + 0.5f)};
        matrix.mapPoints(pts);
        point.x = pts[0];
        point.y = pts[1];
        return point;
    }

    private Point searchNearbyBlackPoint(Point point) { // 搜尋最近的黑點
        HashMap<Point, Float> blackPointMap = new HashMap<Point, Float>();

        int x = (int) (point.x - SEARCH_RANGE),
                y = (int) (point.y + SEARCH_RANGE),
                width = getWidth(),
                height = getHeight();

        if (x < 0)
            x = 0;
        else if (x > width)
            x = width;

        if (y < 0)
            y = 0;
        else if (y > height)
            y = height;

        for (int i=y; i<y; i++)
            for (int j=x; j<x; j++)
                if (drawBitmap.getPixel(j, i) == Color.BLACK) {
                    Point blackPoint = new Point();
                    blackPoint.x = j;
                    blackPoint.y = i;
                    blackPointMap.put(blackPoint, getDistance(point, blackPoint));
                }

        if (blackPointMap.size() > 0) {
            List<Map.Entry<Point, Float>> list = new ArrayList<Map.Entry<Point, Float>>(blackPointMap.entrySet());
            if (list.size() > 1)
                Collections.sort(list, new Comparator<Map.Entry<Point, Float>>(){

                    public int compare(Map.Entry<Point, Float> entry1, Map.Entry<Point, Float> entry2){
                        if (entry1.getValue() > entry2.getValue())
                            return 1;
                        else if (entry1.getValue() == entry2.getValue())
                            return 0;
                        else
                            return -1;
                    }

                });
            point = list.get(0).getKey();
        }

        return point;
    }

    private Point[] findEdges(Point point1, Point point2, int findColor) { // 搜尋邊緣線上的黑點
        Point point[] = null;
        Point result1 = getFirstEdge(point1, point2, findColor);
        if (result1 != null) {
            Point result2 = getFirstEdge(point2, result1, findColor);
            if (result2 != null) {
                point = new Point[]{result1, result2};

                if ((int)result1.x == (int)result2.x && (int)result1.y == (int)result2.y)
                    result2.x++;
            }
        }

        return point;
    }

    private Point getFirstEdge(Point point1, Point point2, int findColor) {// 取得第一個邊緣線
        Point result = null;
        if (!(point1.x == point2.x && point1.y == point2.y)) {
            float deltaX = point2.x - point1.x;
            float deltaY = point2.y - point1.y;
            float pX = point1.x;
            float pY = point1.y;
            float inc = 0.0f;

            if (Math.abs(deltaX) > Math.abs(deltaY)) {
                while (drawBitmap.getPixel((int)(pX), (int)(pY)) != findColor && pX != point2.x) {
                    if (deltaX > 0.0f)
                        inc++;
                    else
                        inc--;

                    pX = point1.x + inc;
                    pY = point1.y + Math.round(inc / deltaX * deltaY);

                    if (pX < 0.0f || pX >= bitmapWidth ||
                            pY < 0.0f || pY >= bitmapHeight) {
                        pX = point2.x;
                        break;
                    }
                }

                if (pX == point2.x)
                    result = null;
                else {
                    result = new Point();
                    result.x = pX;
                    result.y = pY;
                }
            } else {
                while (drawBitmap.getPixel((int)(pX), (int)(pY)) != findColor && pY != point2.y) {
                    if (deltaY > 0.0f)
                        inc++;
                    else
                        inc--;

                    pY = point1.y + inc;
                    pX = point1.x + Math.round(inc / deltaY * deltaX);

                    if (pX < 0.0f || pX >= bitmapWidth ||
                            pY < 0.0f || pY >= bitmapHeight) {
                        pY = point2.y;
                        break;
                    }
                }
                if (pY == point2.y)
                    result = null;
                else {
                    result = new Point();
                    result.x = pX;
                    result.y = pY;
                }
            }
        }
        return result;
    }

    private float getDistance(MotionEvent event) { // 取得距離
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private float getDistance(Point p1, Point p2) { // 取得距離
        float x = (p1.x - p2.x) * (p1.x - p2.x);
        float y = (p1.y - p2.y) * (p1.y - p2.y);
        return (float) Math.sqrt(x + y);
    }

    private Point pointMid(MotionEvent event) { // 取得兩點中心
        Point pointMid = new Point();
        pointMid.x = (event.getX(0) + event.getX(1)) / 2.0f;
        pointMid.y = (event.getY(0) + event.getY(1)) / 2.0f;
        return pointMid;
    }

    public void setBitmap(Bitmap bitmap) { // 設定圖片
        drawBitmap = bitmap;
        bitmapWidth = bitmap.getWidth();
        bitmapHeight = bitmap.getHeight();
        matrix = new Matrix();
        matrix.setTranslate(0f, 0f);
        savedMatrix = new Matrix();
        savedMatrix.setTranslate(0f, 0f);
        holder = getHolder();
        holder.addCallback(this);
    }

    public static Point[] getPoint() { // 取得觸碰點
        return point;
    }

    public static void setPoint(Point points[]) { // 取定觸碰點
        if (points != null) {
            point = points;

            if (point[1] != null)
                holdScalePoint = true;
        }
    }

    public static void clearPoint() { // 清除觸碰點
        for (int i=0; i<point.length; i++)
            point[i] = null;
        holdScalePoint = false;
        x = 0.0f;
    }

    public static float getXScale() {
        return x;
    }

    public static void setXScale(float x) {
        ImageView.x = x;
    }

    public static String getMessage() {
        return message;
    }

    public static void reset() {
        for (int i=0; i<point.length; i++)
            point[i] = null;
        message = null;
        x = 0.0f;
        holdScalePoint = false;
    }

}