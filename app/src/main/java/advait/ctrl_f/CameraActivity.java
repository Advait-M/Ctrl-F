package advait.ctrl_f;

/**
 * @author Advait Maybhate
 * Activity used to initialize the application. Shows camera preview to the user.
 * When "Capture" button is pressed, captures the image and analyzes it (optical character recognition)
 * using the Tesseract engine. Then, asks the user for the desired word to search for (similar to Ctrl-F in computer documents).
 * Highlights all instances of the word in the image (any words that contain the desired text are also highlighted).
 */

import android.Manifest.permission;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

//Inherits from AppCompatActivity (default Android activity)
public class CameraActivity extends AppCompatActivity {
    // Stores the image captured
    Bitmap image;
    // Canvas to draw on (highlight words in image)
    Canvas mCanvas;
    // Tesseract API used for optical character recognition (OCR) - Maintained by Google
    private TessBaseAPI mTess;
    // File location where training data is stored
    String datapath = "";
    // Permissions id (integer)
    public static final int MY_PERMISSIONS_REQUEST_PERMISSIONS = 0;

    // ID for image or video files
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    // Stores camera object - used for taking pictures
    private Camera mCamera;
    // Stores user entered text (desired word)
    private String mText = "";
    // Stores a CameraView object to show camera preview
    private CameraView mPreview;
    // Stores File object (where the image captured is stored)
    private File mMediaFile;
    // Tag used for logging
    private static final String TAG = "CTRL-F_ADVAIT";

    // Called when the picture is taken. Rotates picture to the correct orientation.
    private PictureCallback mPicture = new PictureCallback() {
        // data represents the image taken as a byte array
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            // Decode byte array into a Bitmap format
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            // Rotate according to current orientation
            bmp = rotate(bmp, CameraView.setCameraDisplayOrientation(CameraActivity.this, Camera.CameraInfo.CAMERA_FACING_BACK, mCamera));
            // Save image to desired directory
            saveImage(bmp, true);
        }
    };

    // Saves image to app directory in Pictures on the Android device
    public String saveImage(Bitmap bmp, boolean processImage) {
        // Convert bitmap to PNG format
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);

        // Convert back into byte array
        byte[] data = stream.toByteArray();

        // If picture file is null then log an error and end the function
        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions: ");
            return null;
        }

        try {
            // Write to file using a FileOutputStream
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
            // Boolean used to avoid infinite recursive loop since processImage() calls saveImage()
            // Runs setUpTess() and processImage() when the picture is first taken (via callback)
            if (processImage) {
                setUpTess();
                processImage();
            }
            // Returns path of where image was saved
            return pictureFile.getCanonicalPath();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
        return null;
    }

    // Called on the creation of the application - initializes the Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set up the activity
        super.onCreate(savedInstanceState);
        // Use XML to show the content to the user
        setContentView(R.layout.activity_camera);
        // No loading symbol in the initial screen
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        // Check to see if all required permissions are given
        if (ContextCompat.checkSelfPermission(this, permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Ask for permissions if they are not already given to the application
            ActivityCompat.requestPermissions(this,
                    new String[]{permission.CAMERA, permission.READ_EXTERNAL_STORAGE, permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_PERMISSIONS);
        }
        // If application already has required permissions, start the main() method to initialize the camera preview
        else{
            main();
        }
    }

    // Sets up training data (ensures it is loaded onto the phone)
    public void setUpTess() {
        // Language for optical character recognition (OCR) is English
        String language = "eng";
        datapath = getFilesDir() + "/tesseract/";
        // Tesseract API object stored in member variable
        mTess = new TessBaseAPI();

        // Ensure training data is loaded onto the phone
        checkFile(new File(datapath + "tessdata/"));

        mTess.init(datapath, language);
        try {
            // Stores image as a bitmap in a member variable
            image = BitmapFactory.decodeFile(mMediaFile.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Automatically called when the application is closed (still present in recent apps)
    @Override
    public void onPause() {
        super.onPause();
        // Release camera properly to ensure other applications can use it
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mPreview.getHolder().removeCallback(mPreview);
            mCamera.release();
            mCamera = null;
        }
    }

    // Called when the application is re-opened (was still in recent apps)
    @Override
    public void onResume() {
        super.onResume();
        setUpCamera();
    }

    // Set up camera object with desired parameters and orientation
    public void setUpCamera() {
        // Open camera and assign the object to member variable
        try {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        } catch (Exception e) {
            Log.d("ERROR", "Failed to get Camera: " + e.getMessage());
            e.printStackTrace();
        }

        Camera.Parameters param;
        // Get camera's current parameters
        param = mCamera.getParameters();

        // Set camera resolution to the best possible resolution
        Camera.Size bestSize;
        List<Camera.Size> sizeList = mCamera.getParameters().getSupportedPreviewSizes();
        bestSize = sizeList.get(0);
        // Searches through sizes list to find the highest resolution
        for (int i = 1; i < sizeList.size(); i++) {
            if ((sizeList.get(i).width * sizeList.get(i).height) > (bestSize.width * bestSize.height)) {
                bestSize = sizeList.get(i);
            }
        }
        param.setPictureSize(bestSize.width, bestSize.height);

        // Auto-focus mode to focus camera whenever image stabilizes
        param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        // Assign desired parameters to the camera object
        mCamera.setParameters(param);

        // Find XML object used to show camera preview
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        // Start camera preview
        mPreview = new CameraView(this, mCamera);
        // Ensure camera orientation is correct
        CameraView.setCameraDisplayOrientation(this, Camera.CameraInfo.CAMERA_FACING_BACK, mCamera);
        // Remove all previous content
        preview.removeAllViews();
        // Add camera preview to the graphical user interface (GUI)
        preview.addView(mPreview);
    }

    // Main method that is called once permissions are obtained in onCreate()
    // Sets up camera preview and button used for capturing images
    public void main() {
        setUpCamera();
        // Find button object from XML layouts
        final Button captureButton = (Button) findViewById(R.id.button_capture);

        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    // Called when button is clicked (onClickListener)
                    @Override
                    public void onClick(View v) {
                        // Show loading symbol (shown until Tesseract finishes analyzing the image for text)
                        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
                        // Capture an image (uses callback to call onPictureTaken method)
                        mCamera.takePicture(null, null, mPicture);
                        // Update button to show that image has been taken (updates button text)
                        captureButton.setText("Picture Taken");
                    }
                }
        );
    }

    // Automatically called when user responds to permissions request
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_PERMISSIONS: {
                //  If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // If permissions are given then proceed onto main() (starts camera preview and app functionality)
                    main();
                }
                else {
                    // Otherwise, permission is denied. Cannot run the application.
                    // Informs user that the application cannot run
                    Toast.makeText(this, "Cannot run application without permissions requested.", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    // Highlights desired word in image
    public void highlightDesiredWord(String word) {
        // Create a mutable (editable) bitmap from the given image
        Bitmap mutableBitmap = image.copy(Bitmap.Config.ARGB_8888, true);
        // Create canvas to draw on
        mCanvas = new Canvas(mutableBitmap);

        // Create a transparent yellow colour (used for highlighting)
        Paint colour = new Paint();
        // ARGB - Alpha Red Green Blue (alpha represents opacity/transparency)
        colour.setARGB(150, 255, 255, 0);

        // Create ResultIterator to go through the results obtained by Tesseract
        ResultIterator iterator = mTess.getResultIterator();
        // Keeps track of how many words contain the desired text
        int count = 0;
        // Last word found
        String lastUTF8Text;
        // Iterates through all words found
        while (iterator.next(PageIteratorLevel.RIL_WORD)) {
            // Get current word
            lastUTF8Text = iterator.getUTF8Text(PageIteratorLevel.RIL_WORD);
            // Check if word contains the desired text (which is given by user)
            // toLowerCase() ensures that capitalization does not matter
            if (lastUTF8Text.toLowerCase().contains(word)) {
                // If the word contains the desired text, then highlight the word's bounding box
                // Uses a transparent yellow colour
                mCanvas.drawRect(iterator.getBoundingRect(PageIteratorLevel.RIL_WORD), colour);
                // Increment count of words with desired text
                count++;
            }
        }
        // If no words contain desired text, inform the user with a toast notification
        if (count == 0) {
            Toast.makeText(this, "Word not found!", Toast.LENGTH_LONG).show();
        }
        // Bitmap is too big to pass through intent
        // Save bitmap to phone's storage
        String filePath = saveImage(mutableBitmap, false);
        // Create intent to switch to ResultActivity
        Intent intent = new Intent(this, ResultActivity.class);
        // Pass String containing location of the image file to ResultActivity
        intent.putExtra(EXTRA_MESSAGE, filePath);
        // Switch to ResultActivity
        startActivity(intent);
    }

    // Performs optical character recognition (OCR) on the image using Tesseract and updates screen accordingly
    public void processImage() {
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        mTess.setImage(image);
        // Calls recognize() in backend when loading the text
        String OCRresult = mTess.getUTF8Text();

        // Display dialog to user to get desired search term (word to be highlighted)s
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Enter search term:");

        // Set up the input field for dialog
        final EditText input = new EditText(this);
        // Specify the type of input expected
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        dialogBuilder.setView(input);

        // Set up the buttons
        dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            // If user presses "OK" then get the text from the user and highlight desired text
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mText = input.getText().toString(); // Assign text input to mText
                highlightDesiredWord(mText); // Highlight desired word
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            // If user presses "Cancel" then close the dialog
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel(); // Closes dialogs
            }
        });

        // Show dialog to user
        dialogBuilder.show();
    }

    // Check if training data files exist already
    private void checkFile(File dir) {
        if (!dir.exists()&& dir.mkdirs()) {
            // Copy files if they do not exist
            copyFiles();
        }
        if(dir.exists()) {
            String datafilepath = datapath+ "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);

            if (!datafile.exists()) {
                // Copy files if the data file does not exist
                copyFiles();
            }
        }
    }

    // Copies training data files into the desired directory on the phone
    private void copyFiles() {
        try {
            // Get file from assets folder in the application
            String filepath = datapath + "/tessdata/eng.traineddata";
            AssetManager assetManager = getAssets();

            // Used to transfer training data to the phone
            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            // Write the training data into the desired directory on the phone
            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();

            // Check that the file exists
            File file = new File(filepath);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Rotate a Bitmap by the desired number of degrees (using a matrix)
    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        // Rotate matrix by desired amount of degrees
        Matrix mtx = new Matrix();
        mtx.postRotate(degree);

        // Create new bitmap using rotate matrix
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    // Get File object to store image in (based on current date and time)
    private File getOutputMediaFile(int type) {
        // Directory to store images - easy to share between applications
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Ctrl-F");

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                // Log error if creating the directory failed
                Log.d("Ctrl-F", "failed to create directory");
                return null;
            }
        }

        //  Create a media file name using the current date and time (unique name)
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        // Check if media file is an image
        if (type == MEDIA_TYPE_IMAGE) {
            // Create new File object using desired path
            mMediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        }
        else{
            return null;
        }
        // Return File object of where the image will be stored
        return mMediaFile;
    }

    // If the configuration (orientation) changes then modify the application state accordingly
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // Update the activity orientation
        super.onConfigurationChanged(newConfig);
        // Update the camera preview orientation to ensure the orientation is appropriate
        // Method located in CameraView class
        CameraView.setCameraDisplayOrientation(this, Camera.CameraInfo.CAMERA_FACING_BACK, mCamera);
    }
}
// End of CameraActivity class