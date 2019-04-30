package com.AC.Measure;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.AC.Measure.db.ImageHelper;
import com.crashlytics.android.Crashlytics;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.java.helloar.CameraPermissionHelper;
import com.google.ar.core.examples.java.helloar.DisplayRotationHelper;
import com.google.ar.core.examples.java.helloar.rendering.BackgroundRenderer;
import com.google.ar.core.examples.java.helloar.rendering.ObjectRenderer;
import com.google.ar.core.examples.java.helloar.rendering.PlaneRenderer;
import com.google.ar.core.examples.java.helloar.rendering.PointCloudRenderer;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotTrackingException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.AC.Measure.renderer.RectanglePolygonRenderer;

import com.AC.Measure.model.Image;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;



import io.fabric.sdk.android.Fabric;

/**
 * Created by user on 2017/9/25.
 */

public class ArMeasureActivity extends AppCompatActivity {
    public static final String Save_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/ARMeasure";

    private static final String TAG = ArMeasureActivity.class.getSimpleName();
    private static final String ASSET_NAME_CUBE_OBJ = "cube.obj";
    private static final String ASSET_NAME_CUBE = "cube_green.png";
    private static final String ASSET_NAME_CUBE_SELECTED = "cube_cyan.png";
    private  boolean capturePicture = false;
    private ProgressDialog progress;
    private Bundle bundle = new Bundle();
    private float x1, y1,x2,y2;


    private static final int MAX_CUBE_COUNT = 16;

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView = null;

    private boolean installRequested;

    private Session session = null;
    private GestureDetector gestureDetector;
    private Snackbar messageSnackbar = null;
    private DisplayRotationHelper displayRotationHelper;

    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloud = new PointCloudRenderer();

    private final ObjectRenderer cube = new ObjectRenderer();
    private final ObjectRenderer cubeSelected = new ObjectRenderer();
    private RectanglePolygonRenderer rectRenderer = null;

    private final EdgeDetector edgeDetector = new EdgeDetector();

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] anchorMatrix = new float[MAX_CUBE_COUNT];
    private final ImageView[] ivCubeIconList = new ImageView[MAX_CUBE_COUNT];
    private final int[] cubeIconIdArray = {
            R.id.iv_cube1,
            R.id.iv_cube2,
            R.id.iv_cube3,
            R.id.iv_cube4,
            R.id.iv_cube5,
            R.id.iv_cube6,
            R.id.iv_cube7,
            R.id.iv_cube8,
            R.id.iv_cube9,
            R.id.iv_cube10,
            R.id.iv_cube11,
            R.id.iv_cube12,
            R.id.iv_cube13,
            R.id.iv_cube14,
            R.id.iv_cube15,
            R.id.iv_cube16
    };

    // Tap handling and UI.
    private ArrayBlockingQueue<MotionEvent> queuedSingleTaps = new ArrayBlockingQueue<>(MAX_CUBE_COUNT);
    private ArrayBlockingQueue<MotionEvent> queuedLongPress = new ArrayBlockingQueue<>(MAX_CUBE_COUNT);
    private final ArrayList<Anchor> anchors = new ArrayList<>();
    private ArrayList<Float> showingTapPointX = new ArrayList<>();
    private ArrayList<Float> showingTapPointY = new ArrayList<>();

    private ArrayBlockingQueue<Float> queuedScrollDx = new ArrayBlockingQueue<>(MAX_CUBE_COUNT);
    private ArrayBlockingQueue<Float> queuedScrollDy = new ArrayBlockingQueue<>(MAX_CUBE_COUNT);
    private Config config;

    private void log(String tag, String log){
        if(BuildConfig.DEBUG) {
            Log.d(tag, log);
        }
    }

    private void log(Exception e){
        try {
            Crashlytics.logException(e);
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }catch (Exception ex){
            if (BuildConfig.DEBUG) {
                ex.printStackTrace();
            }
        }
    }

    private void logStatus(String msg){
        try {
            Crashlytics.log(msg);
        }catch (Exception e){
            log(e);
        }
    }

    //    OverlayView overlayViewForTest;
    private TextView tv_result;
    private FloatingActionButton fab;

    private GLSurfaceRenderer glSerfaceRenderer = null;
    private GestureDetector.SimpleOnGestureListener gestureDetectorListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            // Queue tap if there is space. Tap is lost if queue is full.
            queuedSingleTaps.offer(e);
//            log(TAG, "onSingleTapUp, e=" + e.getRawX() + ", " + e.getRawY());
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            queuedLongPress.offer(e);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
//            log(TAG, "onScroll, dx=" + distanceX + " dy=" + distanceY);
            queuedScrollDx.offer(distanceX);
            queuedScrollDy.offer(distanceY);
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);
        if(OpenCVLoader.initDebug()) {
            Toast.makeText(getApplicationContext(),"OpenCV loaded Successful",Toast.LENGTH_SHORT).show();
        } else
            {
            Toast.makeText(getApplicationContext(),"Could not load openCV",Toast.LENGTH_SHORT).show();
        }


//        overlayViewForTest = (OverlayView)findViewById(R.id.overlay_for_test);
        tv_result = findViewById(R.id.tv_result);
        fab = findViewById(R.id.fab);



        for(int i=0; i<cubeIconIdArray.length; i++){
            ivCubeIconList[i] = findViewById(cubeIconIdArray[i]);
            ivCubeIconList[i].setTag(i);
            ivCubeIconList[i].setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    try {
                        int index = Integer.valueOf(view.getTag().toString());
                        logStatus("click index cube: " + index);
                        glSerfaceRenderer.setNowTouchingPointIndex(index);
                        glSerfaceRenderer.showMoreAction();
                    }catch (Exception e){
                        log(e);
                    }
                }
            });
        }

        fab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                logStatus("click fab");
                PopupWindow popUp = getPopupWindow();
//                popUp.showAsDropDown(v, 0, 0); // show popup like dropdown list
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    float screenWidth = getResources().getDisplayMetrics().widthPixels;
                    float screenHeight = getResources().getDisplayMetrics().heightPixels;
                    popUp.showAtLocation(v, Gravity.NO_GRAVITY, (int)screenWidth/2, (int)screenHeight/2);
                } else {
                    popUp.showAsDropDown(v);
                }
            }
        });
        fab.hide();


        displayRotationHelper = new DisplayRotationHelper(/*context=*/ this);

        if(CameraPermissionHelper.hasCameraPermission(this)){
            setupRenderer();
        }

        installRequested = false;
    }

    public void onSavePicture(View view) {
        new AlertDialog.Builder(ArMeasureActivity.this) // 建立互動訊息
                .setMessage("是否進行裂縫量測？")
                .setCancelable(true)
                .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // the image from the onDrawFrame() method.
                        // This is required for OpenGL so we are on the rendering thread.
                        progress = new ProgressDialog(ArMeasureActivity.this);
                        progress.setMessage("影像處理中．．．");
                        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        progress.setCanceledOnTouchOutside(false);
                        progress.show();
                        capturePicture = true;
                    }
                })
        .setNegativeButton("取消", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }

        })
        .show();
    }

    private void setupRenderer(){
        if(surfaceView != null){
            return;
        }
        surfaceView = findViewById(R.id.surfaceview);

        // Set up tap listener.
        gestureDetector = new GestureDetector(this, gestureDetectorListener);
        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                x1 = event.getX();
                y1 = event.getY();
                Intent intent = new Intent(ArMeasureActivity.this,ImageView.class);
                bundle.putFloat("positionX",x1);
                bundle.putFloat("positionY",y1);
                intent.putExtras(bundle);
                return gestureDetector.onTouchEvent(event);
            }
        });
        glSerfaceRenderer = new GLSurfaceRenderer(this);
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(glSerfaceRenderer);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        logStatus("onResume()");
        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                session = new Session(/* context= */ this);
                config = new Config(session);
            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (Exception e) {
                message = "This device does not support AR";
                exception = e;
            }

            if (message != null) {
                showSnackbarMessage(message, true);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }

            // Create default config and check if supported.

            if (!session.isSupported(config)) {
                showSnackbarMessage("This device does not support AR", true);
            }
            config.setFocusMode(Config.FocusMode.AUTO);
            session.configure(config);

            setupRenderer();
        }

        showLoadingMessage();
        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }
        surfaceView.onResume();
        displayRotationHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        logStatus("onPause()");
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            logStatus("onRequestPermissionsResult()");
            Toast.makeText(this, R.string.need_permission, Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        logStatus("onWindowFocusChanged()");
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void showLoadingMessage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageSnackbar = Snackbar.make(
                        ArMeasureActivity.this.findViewById(android.R.id.content),
                        "Searching for surfaces...", Snackbar.LENGTH_INDEFINITE);
                messageSnackbar.getView().setBackgroundColor(0xbf323232);
                messageSnackbar.show();
            }
        });
    }

    private void hideLoadingMessage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(messageSnackbar != null) {
                    messageSnackbar.dismiss();
                }
                messageSnackbar = null;
            }
        });
    }

    private void showSnackbarMessage(String message, boolean finishOnDismiss) {
        messageSnackbar =
                Snackbar.make(
                        ArMeasureActivity.this.findViewById(android.R.id.content),
                        message,
                        Snackbar.LENGTH_INDEFINITE);
        messageSnackbar.getView().setBackgroundColor(0xbf323232);
        if (finishOnDismiss) {
            messageSnackbar.setAction(
                    "Dismiss",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            messageSnackbar.dismiss();
                        }
                    });
            messageSnackbar.addCallback(
                    new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        @Override
                        public void onDismissed(Snackbar transientBottomBar, int event) {
                            super.onDismissed(transientBottomBar, event);
                            finish();
                        }
                    });
        }
        messageSnackbar.show();
    }

    private void toast(int stringResId){
        Toast.makeText(this, stringResId, Toast.LENGTH_SHORT).show();
    }
    private boolean isVerticalMode = false;
    private PopupWindow popupWindow;
    private PopupWindow getPopupWindow() {

        // initialize a pop up window type
        popupWindow = new PopupWindow(this);

        ArrayList<String> sortList = new ArrayList<>();
        sortList.add(getString(R.string.action_1));
        sortList.add(getString(R.string.action_2));
        sortList.add(getString(R.string.action_3));
        sortList.add(getString(R.string.action_4));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
                sortList);
        // the drop down list is a list view
        ListView listViewSort = new ListView(this);
        // set our adapter and pass our pop up window contents
        listViewSort.setAdapter(adapter);
        listViewSort.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 3:// move vertical axis
                        toast(R.string.action_4_toast);
                        break;
                    case 0:// delete
                        toast(R.string.action_1_toast);
                        break;
                    case 1:// set as first
                        toast(R.string.action_2_toast);
                        break;
                    case 2:// move horizontal axis
                    default:
                        toast(R.string.action_3_toast);
                        break;
                }
                return true;
            }
        });
        // set on item selected
        listViewSort.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 3:// move vertical axis
                        isVerticalMode = true;
                        popupWindow.dismiss();
                        break;
                    case 0:// delete
                        glSerfaceRenderer.deleteNowSelection();
                        popupWindow.dismiss();
                        fab.hide();
                        break;
                    case 1:// set as first
                        glSerfaceRenderer.setNowSelectionAsFirst();
                        popupWindow.dismiss();
                        fab.hide();
                        break;
                    case 2:// move horizontal axis
                    default:
                        isVerticalMode = false;
                        popupWindow.dismiss();
                        break;
                }

            }
        });
        // some other visual settings for popup window
        popupWindow.setFocusable(true);
        popupWindow.setWidth((int)(getResources().getDisplayMetrics().widthPixels * 0.4f));
        // popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.white));
        popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        // set the listview as popup content
        popupWindow.setContentView(listViewSort);
        return popupWindow;
    }

    private class GLSurfaceRenderer implements GLSurfaceView.Renderer{
        private static final String TAG = "GLSurfaceRenderer";
        private Context context;
        private final int DEFAULT_VALUE = -1;
        private int nowTouchingPointIndex = DEFAULT_VALUE;
        private int viewWidth = 0;
        private int viewHeight = 0;

        // according to cube.obj, cube diameter = 0.02f
        private final float cubeHitAreaRadius = 0.08f;
        private final float[] centerVertexOfCube = {0f, 0f, 0f, 1};
        private final float[] vertexResult = new float[4];

        private float[] tempTranslation = new float[3];
        private float[] tempRotation = new float[4];
        private float[] projmtx = new float[16];
        private float[] viewmtx = new float[16];

        public GLSurfaceRenderer(Context context){
            this.context = context;
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            logStatus("onSurfaceCreated()");
            GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(context);
            if (session != null) {
                session.setCameraTextureName(backgroundRenderer.getTextureId());
            }

            // Prepare the other rendering objects.
            try {
                rectRenderer = new RectanglePolygonRenderer();
                cube.createOnGlThread(context, ASSET_NAME_CUBE_OBJ, ASSET_NAME_CUBE);
                cube.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
                cubeSelected.createOnGlThread(context, ASSET_NAME_CUBE_OBJ, ASSET_NAME_CUBE_SELECTED);
                cubeSelected.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
            } catch (IOException e) {
                log(TAG, "Failed to read obj file");
            }
            try {
                planeRenderer.createOnGlThread(context, "trigrid.png");
            } catch (IOException e) {
                log(TAG, "Failed to read plane texture");
            }
            pointCloud.createOnGlThread(context);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            if(width <= 0 || height <= 0){
                logStatus("onSurfaceChanged(), <= 0");
                return;
            }
            logStatus("onSurfaceChanged()");

            displayRotationHelper.onSurfaceChanged(width, height);
            GLES20.glViewport(0, 0, width, height);
            viewWidth = width;
            viewHeight = height;
            setNowTouchingPointIndex(DEFAULT_VALUE);
        }

        public void deleteNowSelection(){
            logStatus("deleteNowSelection()");
            int index = nowTouchingPointIndex;
            if (index > -1){
                if(index < anchors.size()) {
                    anchors.remove(index).detach();
                }
                if(index < showingTapPointX.size()) {
                    showingTapPointX.remove(index);
                }
                if(index < showingTapPointY.size()) {
                    showingTapPointY.remove(index);
                }
            }
            setNowTouchingPointIndex(DEFAULT_VALUE);
        }

        public void setNowSelectionAsFirst(){
            logStatus("setNowSelectionAsFirst()");
            int index = nowTouchingPointIndex;
            if (index > -1 && index < anchors.size()) {
                if(index < anchors.size()){
                    for(int i=0; i<index; i++){
                        anchors.add(anchors.remove(0));
                    }
                }
                if(index < showingTapPointX.size()){
                    for(int i=0; i<index; i++){
                        showingTapPointX.add(showingTapPointX.remove(0));
                    }
                }
                if(index < showingTapPointY.size()){
                    for(int i=0; i<index; i++){
                        showingTapPointY.add(showingTapPointY.remove(0));
                    }
                }
            }
            setNowTouchingPointIndex(DEFAULT_VALUE);
        }

        public int getNowTouchingPointIndex(){
            return nowTouchingPointIndex;
        }

        public void setNowTouchingPointIndex(int index){
            nowTouchingPointIndex = index;
            showCubeStatus();
        }

        @Override
        public void onDrawFrame(GL10 gl) {
//            log(TAG, "onDrawFrame(), mTouches.size=" + mTouches.size());
            // Clear screen to notify driver it should not load any pixels from previous frame.
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            if(viewWidth == 0 || viewWidth == 0){
                return;
            }
            if (session == null) {
                return;
            }
            // Notify ARCore session that the view size changed so that the perspective matrix and
            // the video background can be properly adjusted.
            displayRotationHelper.updateSessionIfNeeded(session);

            try {
                session.setCameraTextureName(backgroundRenderer.getTextureId());

                // Obtain the current frame from ARSession. When the configuration is set to
                // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
                // camera framerate.
                Frame frame = session.update();
                Camera camera = frame.getCamera();
                // Draw background.
                backgroundRenderer.draw(frame);
                if (capturePicture) {
                    capturePicture = false;
                    try {
                        SavePicture();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // If not tracking, don't draw 3d objects.
                if (camera.getTrackingState() == TrackingState.PAUSED) {
                    return;
                }

                // Get projection matrix.
                camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

                // Get camera matrix and draw.
                camera.getViewMatrix(viewmtx, 0);

                // Compute lighting from average intensity of the image.
                final float lightIntensity = frame.getLightEstimate().getPixelIntensity();

                // Visualize tracked points.
                PointCloud pointCloud = frame.acquirePointCloud();
                ArMeasureActivity.this.pointCloud.update(pointCloud);
                ArMeasureActivity.this.pointCloud.draw(viewmtx, projmtx);

                // Application is responsible for releasing the point cloud resources after
                // using it.
                pointCloud.release();

                // Check if we detected at least one plane. If so, hide the loading message.
                if (messageSnackbar != null) {
                    for (Plane plane : session.getAllTrackables(Plane.class)) {
                        if (plane.getType() == com.google.ar.core.Plane.Type.HORIZONTAL_UPWARD_FACING &&
                                plane.getTrackingState() == TrackingState.TRACKING) {
                            hideLoadingMessage();
                            break;
                        }
                    }
                }

                // Visualize planes.
                planeRenderer.drawPlanes(
                        session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);

                // draw cube & line from last frame
                if(anchors.size() < 1){
                    // no point
                    showResult("");
                }else{
                    // draw selected cube
                    if(nowTouchingPointIndex != DEFAULT_VALUE) {
                        drawObj(getPose(anchors.get(nowTouchingPointIndex)), cubeSelected, viewmtx, projmtx, lightIntensity);
                        checkIfHit(cubeSelected, nowTouchingPointIndex);
                    }
                    StringBuilder sb = new StringBuilder();
                    float total = 0;
                    Pose point1;
                    // draw first cube
                    Pose point0 = getPose(anchors.get(0));
                    drawObj(point0, cube, viewmtx, projmtx, lightIntensity);
                    checkIfHit(cube, 0);
                    float point[] = point0.getXAxis();
                    // draw the rest cube
                    for(int i = 1; i < anchors.size(); i++){
                        point1 = getPose(anchors.get(i));
                        log("onDrawFrame()", "before drawObj()");
                        drawObj(point1, cube, viewmtx, projmtx, lightIntensity);
                        checkIfHit(cube, i);
                        log("onDrawFrame()", "before drawLine()");
                        drawLine(point0, point1, viewmtx, projmtx);

                        float distanceCm = ((int)(getDistance(point0, point1) * 1000))/10.0f;
                        total += distanceCm;
                        sb.append(" + ").append(distanceCm);

                        point0 = point1;
                    }

                    // show result
                    String result = (((int)(total * 10f))/10f) + "cm";
                    //Bundle Distance_result = new Bundle();
                    Intent intent = new Intent(ArMeasureActivity.this,ImageView.class);
                    bundle.putFloat("Distance_result",total);
                    intent.putExtras(bundle);
                    showResult(result);
                }

                // check if there is any touch event
                MotionEvent tap = queuedSingleTaps.poll();
                if(tap != null && camera.getTrackingState() == TrackingState.TRACKING){
                    for (HitResult hit : frame.hitTest(tap)) {
                        // Check if any plane was hit, and if it was hit inside the plane polygon.j
                        Trackable trackable = hit.getTrackable();
                        // Creates an anchor if a plane or an oriented point was hit.
                        if (trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                                || (trackable instanceof Point
                                    && ((Point) trackable).getOrientationMode()
                                        == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)) {
                            // Cap the number of objects created. This avoids overloading both the
                            // rendering system and ARCore.
                            if (anchors.size() >= 2) {
                                anchors.get(0).detach();
                                anchors.remove(0);

                                showingTapPointX.remove(0);
                                showingTapPointY.remove(0);
                            }

                            // Adding an Anchor tells ARCore that it should track this position in
                            // space. This anchor will be used in PlaneAttachment to place the 3d model
                            // in the correct position relative both to the world and to the plane.
                            anchors.add(hit.createAnchor());

                            showingTapPointX.add(tap.getX());
                            showingTapPointY.add(tap.getY());
                            nowTouchingPointIndex = anchors.size() - 1;

                            showMoreAction();
                            showCubeStatus();
                            break;
                        }
                    }
                }else{
                    handleMoveEvent(nowTouchingPointIndex);
                }
            } catch (Throwable t) {
                // Avoid crashing the application due to unhandled exceptions.
                Log.e(TAG, "Exception on the OpenGL thread", t);
            }
        }

        /**
         * Call from the GLThread to save a picture of the current frame.
         */
        public void SavePicture() throws IOException  {
            int pixelData[] = new int[viewWidth * viewHeight];
            // Read the pixels from the current GL frame.
            IntBuffer buf = IntBuffer.wrap(pixelData);
            buf.position(0);
            GLES20.glReadPixels(0,0,viewWidth,viewHeight, GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,buf);

            // Create a file in the Pictures/HelloAR album.
            //Bundle bundle = new Bundle();
            String formattime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ").format(new Date());
            ImageHelper ih = new ImageHelper(context);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true;
            //Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, options);


            Image image = new Image();
            image.setName(formattime + "-" + "1-first.png");
            bundle.putString("first", image.getName());
            final File out = new File(Save_path);

            // Make sure the directory exists
            if(!out.getParentFile().exists()) {
                    out.getParentFile().mkdirs();
                }

                //edgeDetector.detect(viewWidth,viewHeight,viewWidth,128,);

                // Convert the pixel data from RGBA to what Android wants, ARGB.
                int bitmapData[] = new int[pixelData.length];
            for(
                int i = 0; i<viewHeight;i++)
                {
                    for (int j = 0; j < viewWidth; j++) {
                        int p = pixelData[i * viewWidth + j];
                        int b = (p & 0x00ff0000) >> 16;
                        int r = (p & 0x000000ff) << 16;
                        int ga = p & 0xff00ff00;
                        bitmapData[(viewHeight - i - 1) * viewWidth + j] = ga | r | b;
                    }
                }



            // Create a bitmap.
            Bitmap bmp = Bitmap.createBitmap(bitmapData,
                    viewWidth, viewHeight, Bitmap.Config.ARGB_8888);


            // Write it to disk.
            FileOutputStream fos = new FileOutputStream(out.getPath() + "/" + image.getName());
            bmp.compress(Bitmap.CompressFormat.PNG,100,fos);
            //edgeDetector.detect(bmp).compress(Bitmap.CompressFormat.PNG,100,fos);
            fos.flush();
            fos.close();
            ih.set(image);

            image = new Image();
            image.setName(formattime + "-" + "2-binary.png");
            bundle.putString("binary", image.getName());
            fos = new FileOutputStream(out.getPath() + "/" + image.getName());
            Binary(bmp).compress(Bitmap.CompressFormat.PNG,100,fos);
            fos.flush();
            fos.close();
            ih.set(image);

            image = new Image();
            image.setName(formattime + "-" + "3-canny.png");
            bundle.putString("canny", image.getName());
            fos = new FileOutputStream(out.getPath() + "/" + image.getName());
            Canny(bmp).compress(Bitmap.CompressFormat.PNG,100,fos);
            fos.flush();
            fos.close();
            ih.set(image);

            //startActivity(new Intent(context, ImageActivity.class));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //showSnackbarMessage("Wrote " + out.getName(), false);
                    Display display = getWindowManager().getDefaultDisplay();// 取得螢幕高度
                    int height = display.getHeight();
                    Toast toast = Toast.makeText(context, "照片保存成功", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.BOTTOM, 0, height / 10);
                    toast.show();
                }
            });
            Intent intent = new Intent(context, ImageActivity.class);
            intent.putExtras(bundle);
            ((Activity)context).startActivityForResult(intent, 0);
            progress.dismiss();
        }

       public Bitmap Binary(Bitmap bitmap) { // 二值化方法
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            Bitmap binarymap = null;
            binarymap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    int col = binarymap.getPixel(i, j);
                    int alpha = col & 0xFF000000;
                    int red = (col & 0x00FF0000) >> 16;
                    int green = (col & 0x0000FF00) >> 8;
                    int blue = (col & 0x000000FF);
                    int gray = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
                    if (gray <= 95)
                        gray = 0;
                    else
                        gray = 255;
                    int newColor = alpha | (gray << 16) | (gray << 8) | gray;
                    binarymap.setPixel(i, j, newColor);
                }
            }
            return binarymap;
        }
        public Bitmap Canny(Bitmap bitmap) { // Canny 方法
            Mat srcBitmapMat = new Mat();
            Utils.bitmapToMat(bitmap, srcBitmapMat); // 將 Bitmap 轉換成 OpenCV 可處理的 Mat 物件
            Mat bitmapMat = new Mat();
            Imgproc.cvtColor(srcBitmapMat, bitmapMat, Imgproc.COLOR_BGR2GRAY, 1); // RGB轉換成灰階形式
            Imgproc.blur(bitmapMat, bitmapMat, new Size(3.0, 3.0)); // 模糊化
            Imgproc.Canny(bitmapMat, bitmapMat, 100.0, 100.0, 3, true); // Canny
//    	perspectiveCorrection(bitmapMat, srcBitmapMat); // 矯正矩形
            Imgproc.cvtColor(bitmapMat, bitmapMat, Imgproc.COLOR_GRAY2BGRA, 4); // 灰階轉換成RGB形式
            Utils.matToBitmap(bitmapMat, bitmap); // 將 Mat 轉換成 Android 可處理的 Bitmap 物件
            int width = bitmap.getWidth(), height = bitmap.getHeight();
//            for (int i=0; i<height; i++) // 黑白反轉迴圈
//                for (int j=0; j<width; j++)
//                    if (bitmap.getPixel(j, i) == Color.WHITE)
//                        bitmap.setPixel(j, i, Color.BLACK);
//                    else
//                        bitmap.setPixel(j, i, Color.WHITE);
            return bitmap;
        }

        private void handleMoveEvent(int nowSelectedIndex){
            try {
                if (showingTapPointX.size() < 1 || queuedScrollDx.size() < 2) {
                    // no action, don't move
                    return;
                }
                if (nowTouchingPointIndex == DEFAULT_VALUE) {
                    // no selected cube, don't move
                    return;
                }
                if (nowSelectedIndex >= showingTapPointX.size()) {
                    // wrong index, don't move.
                    return;
                }
                float scrollDx = 0;
                float scrollDy = 0;
                int scrollQueueSize = queuedScrollDx.size();
                for (int i = 0; i < scrollQueueSize; i++) {
                    scrollDx += queuedScrollDx.poll();
                    scrollDy += queuedScrollDy.poll();
                }

                if (isVerticalMode) {
                    Anchor anchor = anchors.remove(nowSelectedIndex);
                    anchor.detach();
                    setPoseDataToTempArray(getPose(anchor));
//                        log(TAG, "point[" + nowSelectedIndex + "] move vertical "+ (scrollDy / viewHeight) + ", tY=" + tempTranslation[1]
//                                + ", new tY=" + (tempTranslation[1] += (scrollDy / viewHeight)));
                    tempTranslation[1] += (scrollDy / viewHeight);
                    anchors.add(nowSelectedIndex,
                            session.createAnchor(new Pose(tempTranslation, tempRotation)));
                } else {
                    float toX = showingTapPointX.get(nowSelectedIndex) - scrollDx;
                    showingTapPointX.remove(nowSelectedIndex);
                    showingTapPointX.add(nowSelectedIndex, toX);

                    float toY = showingTapPointY.get(nowSelectedIndex) - scrollDy;
                    showingTapPointY.remove(nowSelectedIndex);
                    showingTapPointY.add(nowSelectedIndex, toY);

                    if (anchors.size() > nowSelectedIndex) {
                        Anchor anchor = anchors.remove(nowSelectedIndex);
                        anchor.detach();
                        // remove duplicated anchor
                        setPoseDataToTempArray(getPose(anchor));
                        tempTranslation[0] -= (scrollDx / viewWidth);
                        tempTranslation[2] -= (scrollDy / viewHeight);
                        anchors.add(nowSelectedIndex,
                                session.createAnchor(new Pose(tempTranslation, tempRotation)));
                    }
                }
            } catch (NotTrackingException e) {
                e.printStackTrace();
            }
        }

        private final float[] mPoseTranslation = new float[3];
        private final float[] mPoseRotation = new float[4];
        private Pose getPose(Anchor anchor){
            Pose pose = anchor.getPose();
            pose.getTranslation(mPoseTranslation, 0);
            pose.getRotationQuaternion(mPoseRotation, 0);
            return new Pose(mPoseTranslation, mPoseRotation);
        }

        private void setPoseDataToTempArray(Pose pose){
            pose.getTranslation(tempTranslation, 0);
            pose.getRotationQuaternion(tempRotation, 0);
        }

        private void drawLine(Pose pose0, Pose pose1, float[] viewmtx, float[] projmtx){
            float lineWidth = 0.005f;
            float lineWidthH = lineWidth / viewHeight * viewWidth;
            rectRenderer.setVerts(
                    pose0.tx() - lineWidth, pose0.ty() + lineWidthH, pose0.tz() - lineWidth,
                    pose0.tx() + lineWidth, pose0.ty() + lineWidthH, pose0.tz() + lineWidth,
                    pose1.tx() + lineWidth, pose1.ty() + lineWidthH, pose1.tz() + lineWidth,
                    pose1.tx() - lineWidth, pose1.ty() + lineWidthH, pose1.tz() - lineWidth
                    ,
                    pose0.tx() - lineWidth, pose0.ty() - lineWidthH, pose0.tz() - lineWidth,
                    pose0.tx() + lineWidth, pose0.ty() - lineWidthH, pose0.tz() + lineWidth,
                    pose1.tx() + lineWidth, pose1.ty() - lineWidthH, pose1.tz() + lineWidth,
                    pose1.tx() - lineWidth, pose1.ty() - lineWidthH, pose1.tz() - lineWidth
            );

            rectRenderer.draw(viewmtx, projmtx);
        }

        private void drawObj(Pose pose, ObjectRenderer renderer, float[] cameraView, float[] cameraPerspective, float lightIntensity){
            pose.toMatrix(anchorMatrix, 0);
            renderer.updateModelMatrix(anchorMatrix, 1);
            renderer.draw(cameraView, cameraPerspective, lightIntensity);
        }

        private void checkIfHit(ObjectRenderer renderer, int cubeIndex){
            if(isMVPMatrixHitMotionEvent(renderer.getModelViewProjectionMatrix(), queuedLongPress.peek())){
                // long press hit a cube, show context menu for the cube
                nowTouchingPointIndex = cubeIndex;
                queuedLongPress.poll();
                showMoreAction();
                showCubeStatus();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fab.performClick();
                    }
                });
            }else if(isMVPMatrixHitMotionEvent(renderer.getModelViewProjectionMatrix(), queuedSingleTaps.peek())){
                nowTouchingPointIndex = cubeIndex;
                queuedSingleTaps.poll();
                showMoreAction();
                showCubeStatus();
            }
        }

        private boolean isMVPMatrixHitMotionEvent(float[] ModelViewProjectionMatrix, MotionEvent event){
            if(event == null){
                return false;
            }
            Matrix.multiplyMV(vertexResult, 0, ModelViewProjectionMatrix, 0, centerVertexOfCube, 0);
            /**
             * vertexResult = [x, y, z, w]
             *
             * coordinates in View
             * ┌─────────────────────────────────────────┐╮
             * │[0, 0]                     [viewWidth, 0]│
             * │       [viewWidth/2, viewHeight/2]       │view height
             * │[0, viewHeight]   [viewWidth, viewHeight]│
             * └─────────────────────────────────────────┘╯
             * ╰                view width               ╯
             *
             * coordinates in GLSurfaceView frame
             * ┌─────────────────────────────────────────┐╮
             * │[-1.0,  1.0]                  [1.0,  1.0]│
             * │                 [0, 0]                  │view height
             * │[-1.0, -1.0]                  [1.0, -1.0]│
             * └─────────────────────────────────────────┘╯
             * ╰                view width               ╯
             */
            // circle hit test
            float radius = (viewWidth / 2) * (cubeHitAreaRadius/vertexResult[3]);
            float dx = event.getX() - (viewWidth / 2) * (1 + vertexResult[0]/vertexResult[3]);
            float dy = event.getY() - (viewHeight / 2) * (1 - vertexResult[1]/vertexResult[3]);
            double distance = Math.sqrt(dx * dx + dy * dy);
//            // for debug
//            overlayViewForTest.setPoint("cubeCenter", screenX, screenY);
//            overlayViewForTest.postInvalidate();
            return distance < radius;
        }

        private double getDistance(Pose pose0, Pose pose1){
            float dx = pose0.tx() - pose1.tx();
            float dy = pose0.ty() - pose1.ty();
            float dz = pose0.tz() - pose1.tz();
            return Math.sqrt(dx * dx + dz * dz + dy * dy);
        }

        private void showResult(final String result){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv_result.setText(result);
                }
            });
        }

        private void showMoreAction(){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fab.show();
                }
            });
        }

        private void hideMoreAction(){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fab.hide();
                }
            });
        }

        private void showCubeStatus(){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int nowSelectIndex = glSerfaceRenderer.getNowTouchingPointIndex();
                    for(int i = 0; i<ivCubeIconList.length && i< anchors.size(); i++){
                        ivCubeIconList[i].setEnabled(true);
                        ivCubeIconList[i].setActivated(i == nowSelectIndex);
                    }
                    for(int i = anchors.size(); i<ivCubeIconList.length; i++){
                        ivCubeIconList[i].setEnabled(false);
                    }
                }
            });
        }

    }
}
