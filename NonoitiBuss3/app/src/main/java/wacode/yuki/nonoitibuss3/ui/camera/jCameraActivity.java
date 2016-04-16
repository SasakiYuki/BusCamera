package wacode.yuki.nonoitibuss3.ui.camera;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;


import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import wacode.yuki.nonoitibuss2.Utils.BackgroundThreadHelper;
import wacode.yuki.nonoitibuss2.View.Camera.CameraListener;
import wacode.yuki.nonoitibuss2.View.Main.FitTextureView;
import wacode.yuki.nonoitibuss3.R;
import wacode.yuki.nonoitibuss3.ui.camera.utils.TimeSwitcher;
import wacode.yuki.nonoitibuss3.utils.RealmUtils;


public class jCameraActivity extends FragmentActivity implements CameraListener {

    private static final String TAG = "Camera2App";
    public static final String REALM_STRING="ImageData.realm";

    private int REQUEST_CODE_CAMERA_PERMISSION = 0x01;

    private Size mPreviewSize;
    private FitTextureView mTextureView;

    private ImageReader mImageReader;
    private BackgroundThreadHelper mThread;
    private BaseCamera mCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mTextureView = (FitTextureView) findViewById(R.id.texture);
        mThread = new BackgroundThreadHelper();

        mCamera = new BaseCamera();
        mCamera.setInterface(this);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture();
//                cameraHandler(); // cameraHandlerを有効化することでバスの止まる時間に合わせて撮影をするHandlerが動く
            }
        });
    }

    private int getCurrentTime(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        return (hour*100+minute);
    }

    private void cameraHandler(){
        Handler handler = new Handler();
        handler.postDelayed(takeHandler(),2000);
    }

    private Runnable takeHandler(){
        return new Runnable() {
            @Override
            public void run() {
                if(TimeSwitcher.INSTANCE.checkBusStop(getCurrentTime())){
                    mCamera.takePicture();
                    cameraHandler();
                }else {
                    cameraHandler();
                    Log.d("sippai","sippai");
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        mThread.start();

        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        mThread.stop();
        super.onPause();
        finish();
    }

    private String setUpCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());

                setUpPreview(map.getOutputSizes(SurfaceTexture.class),
                        width, height, largest);
                configurePreviewTransform(width, height);

                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(
                        new ImageReader.OnImageAvailableListener() {

                            @Override
                            public void onImageAvailable(ImageReader reader) {
                                try {

                                    final Image image = reader.acquireLatestImage();
                                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                                    byte[] bytes = new byte[buffer.remaining()];
                                    buffer.get(bytes);
                                    image.close();
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 30, baos);
                                    baos.flush();
                                    byte[] bytes1 = baos.toByteArray();
                                    baos.close();
                                    String strBase64 = Base64.encodeToString(bytes1, Base64.NO_WRAP);
                                    RealmUtils.INSTANCE.put(jCameraActivity.this, REALM_STRING, strBase64);
                                }catch (Exception e){

                                }
                            }

                        }, mThread.getHandler());

                return cameraId;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            Log.e(TAG, "Camera Error:not support Camera2API");
        }

        return null;
    }

    private void openCamera(int width, int height) {
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }

        String cameraId = setUpCameraOutputs(width, height);
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCamera.isLocked()) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(cameraId, mCamera.getStateCallback(), mThread.getHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    private void closeCamera() {
        mCamera.close();
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    //Texture Listener
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configurePreviewTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };

    private void setUpPreview(Size[] choices, int width, int height, Size aspectRatio) {
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        if (bigEnough.size() > 0) {
            mPreviewSize = Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            mPreviewSize = choices[0];
        }

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mTextureView.setAspectRatio(
                    mPreviewSize.getWidth(), mPreviewSize.getHeight());
        } else {
            mTextureView.setAspectRatio(
                    mPreviewSize.getHeight(), mPreviewSize.getWidth());
        }
    }

    private void configurePreviewTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    // パーミッションの処理シーケンスはまだおかしい
    // Parmission handling for Android 6.0
    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            // 権限チェックした結果、持っていない場合はダイアログを出す
            new AlertDialog.Builder(this)
                    .setMessage("Request Permission")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CODE_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                    .create();
            return;
        }

        // 権限を取得する
        requestPermissions(new String[]{Manifest.permission.CAMERA},
                REQUEST_CODE_CAMERA_PERMISSION);
        return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                new AlertDialog.Builder(this)
                        .setMessage("Need Camera Permission")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        })
                        .create();
            }
            return;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public SurfaceTexture getSurfaceTextureFromTextureView() {
        return mTextureView.getSurfaceTexture();
    }

    @Override
    public Size getPreviewSize() {
        return mPreviewSize;
    }

    @Override
    public Handler getBackgroundHandler() {
        return mThread.getHandler();
    }

    @Override
    public Surface getImageRenderSurface() {
        return mImageReader.getSurface();
    }

    @Override
    public int getRotation() {
        return getWindowManager().getDefaultDisplay().getRotation();
    }


    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

}

