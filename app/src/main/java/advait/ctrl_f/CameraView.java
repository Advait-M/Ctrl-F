package advait.ctrl_f;

/**
 * @author Advait Maybhate
 * Class used to display the camera preview. Takes in the camera object (mCamera) and interacts with it.
 * Extends SurfaceView.
 */

import android.content.Context;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

// Inherits from SurfaceView and implements the SurfaceHolder Callback
public class CameraView extends SurfaceView implements SurfaceHolder.Callback{
    private SurfaceHolder mHolder;
    private Camera mCamera;

    // Constructor initializes mHolder and mCamera
    public CameraView(Context context, Camera camera){
        super(context);

        // Set camera to the desired camera (passed in as a parameter)
        mCamera = camera;

        // Get holder and add callback to the holder
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
    }

    // Sets camera to the correct display orientation depending on the rotation of the screen
    // Additionally, returns the current screen orientation (integer representing an angle)
    public static int setCameraDisplayOrientation(AppCompatActivity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        if (camera != null) {
            android.hardware.Camera.CameraInfo info =
                    new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(cameraId, info);
            // Get current rotation of the device
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            // Integer representation of orientation (degrees)
            // 0 degrees is landscape orientation
            int degrees = 0;
            // Set degrees to the desired value depending on orientation
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

            // Stores current orientation in degrees
            int result;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360;
                result = (360 - result) % 360;  // Compensate the mirror
            }
            else { // Back-facing camera
                result = (info.orientation - degrees + 360) % 360;
            }
            // Log orientation value
            Log.d("Orientation:", Integer.toString(result));
            // Set display orientation to the value calculated
            camera.setDisplayOrientation(result);
            // Return current orientation value (integer in degrees)
            return result;
        }
        // -1 return value represents error (camera is null)
        return -1;
    }

    // Called when the SurfaceView is created
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try{
            // Starts camera preview
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
            this.getHolder().addCallback(this);
        } catch (IOException e) {
            Log.d("ERROR", "camera error in surfaceCreated method" + e.getMessage());
        }
    }

    // Called when the SurfaceView is changed/modified
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if(mHolder.getSurface() == null)
            return;
        // Try to stop the preview
        try{
            mCamera.stopPreview();
        } catch (Exception e){
        }

        // Restart preview with the correct holder
        try{
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d("ERROR", "Camera error on surfaceChanged " + e.getMessage());
        }
    }

    // Called when the SurfaceView is destroyed - releases camera
    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        // Stop preview and release camera so other applications can use it
        mCamera.stopPreview();
        this.getHolder().removeCallback(this);
        mCamera.release();
    }
}
// End of CameraView class