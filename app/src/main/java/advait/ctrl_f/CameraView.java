package advait.ctrl_f;

/**
 * Class used to display the camera preview. Holds the camera object (mCamera) and interacts with it.
 */

import android.content.Context;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback{
    private SurfaceHolder mHolder;
    private Camera mCamera;

    //Constructor
    public CameraView(Context context, Camera camera){
        super(context);

        //Set camera to the desired camera (passed in as a parameter)
        mCamera = camera;

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
    }

    //Sets camera to the correct display orientation depending on the rotation of the screen
    //Additionally, returns the current screen orientation (integer representing an angle)
    public static int setCameraDisplayOrientation(AppCompatActivity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        if (camera != null) {
            android.hardware.Camera.CameraInfo info =
                    new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(cameraId, info);
            int rotation = activity.getWindowManager().getDefaultDisplay()
                    .getRotation();
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0:
                    degrees = 0;
                    break;
                case Surface.ROTATION_90:
                    degrees = 90;
                    break;
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;
            }

            int result;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360;
                result = (360 - result) % 360;  // compensate the mirror
            } else {  // back-facing
                result = (info.orientation - degrees + 360) % 360;
            }
            Log.d("ori", Integer.toString(result));
            camera.setDisplayOrientation(result);
            return result;
        }
        return -1;
    }

    //Called when the SurfaceView is created
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try{
            mCamera.setPreviewDisplay(surfaceHolder);

//            preview.addView(mPreview);
            mCamera.startPreview();

            this.getHolder().addCallback(this);
        } catch (IOException e) {
            Log.d("ERROR", "camera error in surfaceCreated method" + e.getMessage());
        }
    }


    //Called when the SurfaceView is changed/modified
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if(mHolder.getSurface() == null)
            return;
        try{
            mCamera.stopPreview();
        } catch (Exception e){
        }

        try{
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d("ERROR", "Camera error on surfaceChanged " + e.getMessage());
        }
    }


    //Called when the SurfaceView is destroyed
    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mCamera.stopPreview();
        this.getHolder().removeCallback(this);
        mCamera.release();
//        mCamera.stopPreview();
//        mCamera.setPreviewCallback(null);
//
//        mCamera.release();
    }
}
