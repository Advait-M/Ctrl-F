package advait.ctrl_f;

import android.Manifest;
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
import android.hardware.Camera.Size;
import android.net.Uri;
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
import android.widget.ImageView;
import android.widget.TextView;

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


public class CameraActivity extends AppCompatActivity {
    //Stores the image captured
    Bitmap image;
    Canvas mCanvas;
    //Tesseract API used for optical character recognition (OCR) - Maintained by Google
    private TessBaseAPI mTess;
    String datapath = "";
    public static final int MY_PERMISSIONS_REQUEST_PERMISSIONS = 0;

    //ID for image or video files
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    //Stores camera object
    private Camera mCamera;
    private String mText = "";
    //Stores a CameraView object to show camera preview
    private CameraView mPreview;
    //Stores File object (where the image captured is stored)
    private File mMediaFile;
    private static final String TAG = "CTRL-F_ADVAIT";

    //Called when the picture is taken. Rotates picture to the correct orientation.
    private PictureCallback mPicture = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            //Decode byte array into a Bitmap format
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            //Rotate according to current orientation
            bmp = rotate(bmp, CameraView.setCameraDisplayOrientation(CameraActivity.this, Camera.CameraInfo.CAMERA_FACING_BACK, mCamera));
            saveImage(bmp, true);
//            ByteArrayOutputStream stream = new ByteArrayOutputStream();
//            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
//            //Convert back into byte array
//            data = stream.toByteArray();
//
//            //If picture file is null then log an error and end the function
//            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
//            if (pictureFile == null){
//                Log.d(TAG, "Error creating media file, check storage permissions: ");
//                return;
//            }
//
//            try {
//                //Write to file using a FileOutputStream
//                FileOutputStream fos = new FileOutputStream(pictureFile);
//                fos.write(data);
//                fos.close();
//                System.out.println("reached bfr analyze");
//                analyzeImage();
//            } catch (FileNotFoundException e) {
//                Log.d(TAG, "File not found: " + e.getMessage());
//            } catch (IOException e) {
//                Log.d(TAG, "Error accessing file: " + e.getMessage());
//            }
        }
    };

    public String saveImage(Bitmap bmp, boolean processImage){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        //Convert back into byte array
        byte[] data = stream.toByteArray();

        //If picture file is null then log an error and end the function
        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        if (pictureFile == null){
            Log.d(TAG, "Error creating media file, check storage permissions: ");
            return null;
        }

        try {
            //Write to file using a FileOutputStream
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
            System.out.println("reached bfr analyze");
            if (processImage){
                analyzeImage();
                processImage();
            }
            return pictureFile.getCanonicalPath();
            //analyzeImage();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
        return null;
    }
    //Called on the creation of the application - initializes the Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (ContextCompat.checkSelfPermission(this, permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{permission.CAMERA, permission.READ_EXTERNAL_STORAGE, permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_PERMISSIONS);
        }
        else{
            main();
        }
    }
    //Analyze image (grabs image from external storage of the phone)
    public void analyzeImage(){
        String language = "eng";
        datapath = getFilesDir() + "/tesseract/";
        mTess = new TessBaseAPI();

        checkFile(new File(datapath + "tessdata/"));

        mTess.init(datapath, language);
        try {
            image = BitmapFactory.decodeFile(mMediaFile.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Call processImage() to do optical character recognition (OCR) using Tesseract

    }
    public void main(){
        try {
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        } catch (Exception e) {
            Log.d("ERROR", "Failed to get Camera: " + e.getMessage());
            e.printStackTrace();
        }

        mPreview = new CameraView(this, mCamera);
        CameraView.setCameraDisplayOrientation(this, Camera.CameraInfo.CAMERA_FACING_BACK, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        final Button captureButton = (Button) findViewById(R.id.button_capture);
        Camera.Parameters param;
        param = mCamera.getParameters();

        Camera.Size bestSize;
        List<Camera.Size> sizeList = mCamera.getParameters().getSupportedPreviewSizes();
        bestSize = sizeList.get(0);
        for (int i = 1; i < sizeList.size(); i++) {
            if ((sizeList.get(i).width * sizeList.get(i).height) > (bestSize.width * bestSize.height)) {
                bestSize = sizeList.get(i);
            }
        }
        param.setPictureSize(bestSize.width, bestSize.height);
        param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCamera.setParameters(param);

        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        mCamera.takePicture(null, null, mPicture);
                        captureButton.setText("Picture Taken");

                    }
                }
        );

        //init image
        //image = BitmapFactory.decodeResource(getResources(), R.drawable.test_image);

    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    main();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void highlightDesiredWord(String word){
        Bitmap mutableBitmap = image.copy(Bitmap.Config.ARGB_8888, true);
        mCanvas = new Canvas(mutableBitmap);
        Paint colour = new Paint();
        colour.setARGB(150, 255, 255, 0);
        ResultIterator iterator = mTess.getResultIterator();
        int count = 0;
        String lastUTF8Text;
        float lastConfidence;
        while (iterator.next(PageIteratorLevel.RIL_WORD)) {
            lastUTF8Text = iterator.getUTF8Text(PageIteratorLevel.RIL_WORD);
            lastConfidence = iterator.confidence(PageIteratorLevel.RIL_WORD);
            if (lastUTF8Text.toLowerCase().contains(word)) {

                mCanvas.drawRect(iterator.getBoundingRect(PageIteratorLevel.RIL_WORD), colour);
            }
            System.out.println(lastUTF8Text);
            System.out.println(lastConfidence);
            count++;

        }
        //Bitmap too big to pass through intent
        System.out.println("eh");
        String filePath = saveImage(mutableBitmap, false);
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(EXTRA_MESSAGE, filePath);
        startActivity(intent);
        finish();
//        ImageView img = new ImageView(this);
//        img.setImageBitmap(mutableBitmap);
//        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
//        preview.addView(img);
    }
    //Performs optical character recognition (OCR) on the image using Tesseract and updates screen accordingly
    public void processImage(){
        String OCRresult = null;
        mTess.setImage(image);
        OCRresult = mTess.getUTF8Text();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ctrl-F");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mText = input.getText().toString();
                highlightDesiredWord(mText);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
//        System.out.println(mTess.getWords().getBoxRects().get(0).toString());
//        //Bitmap bmp = BitmapFactory.decodeFile("/sdcard/Stored_Images/" + editTextStr + ".jpg");
//        Bitmap mutableBitmap = image.copy(Bitmap.Config.ARGB_8888, true);
//        mCanvas = new Canvas(mutableBitmap);
//        Paint colour = new Paint();
//        colour.setARGB(150, 255, 255, 0);
//
//        String[] splitResults = OCRresult.split(" ");
//        System.out.println("wordss");
//        System.out.println(mTess.getBoxText(0));
//        System.out.println("words2");
//
//        System.out.println("lengths");
//        String[] split2 = mTess.getBoxText(0).split(" ");
//        System.out.println(split2.length);
//        System.out.println(splitResults.length);
//        System.out.println(mTess.getWords().getBoxRects().size());
//        ResultIterator iterator = mTess.getResultIterator();
//        int count = 0;
//        String lastUTF8Text;
//        float lastConfidence;
//        while (iterator.next(PageIteratorLevel.RIL_WORD)) {
//            lastUTF8Text = iterator.getUTF8Text(PageIteratorLevel.RIL_WORD);
//            lastConfidence = iterator.confidence(PageIteratorLevel.RIL_WORD);
//            if (lastUTF8Text.toLowerCase().contains("name")) {
//
//                mCanvas.drawRect(iterator.getBoundingRect(PageIteratorLevel.RIL_WORD), colour);
//            }
//            System.out.println(lastUTF8Text);
//            System.out.println(lastConfidence);
//            count++;
//        }
////        for (int i = 0; i < mTess.getWords().getBoxRects().size(); i++) {
////            if (splitResults[i].equals("represents")) {
////                mCanvas.drawRect(mTess.getWords().getBoxRects().get(i), colour);
////            }
////        }
//        mCanvas.drawRect(mTess.getWords().getBoxRects().get(5), colour);
//        System.out.println("5th");
//        System.out.println(splitResults[5]);

//        System.out.println(OCRresult);
//        //Show recognized text to the user
//        TextView OCRTextView = (TextView) findViewById(R.id.OCRTextView);
        //OCRTextView.setText(OCRresult);
    }
    //Check if training data files exist already
    private void checkFile(File dir) {
        if (!dir.exists()&& dir.mkdirs()){
            //Copy files if they do not exist
            copyFiles();
        }
        if(dir.exists()) {
            String datafilepath = datapath+ "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);

            if (!datafile.exists()) {
                //Copy files if the data file does not exist
                copyFiles();
            }
        }
    }

    //Copies training data files into the desired directory on the phone
    private void copyFiles() {
        try {
            //Get file from assets folder in the application
            String filepath = datapath + "/tessdata/eng.traineddata";
            AssetManager assetManager = getAssets();

            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }


            outstream.flush();
            outstream.close();
            instream.close();

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

    //Rotate a Bitmap by the desired number of degrees (using a matrix)
    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.postRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    private File getOutputMediaFile(int type){
        // Check if SD card is mounted
        // using Environment.getExternalStorageState() before proceeding

        // Easy to share between applications
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Ctrl-F");

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("Ctrl-F", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        if (type == MEDIA_TYPE_IMAGE){
            mMediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mMediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mMediaFile;
    }

    //If the configuration (orientation) changes then modify the application state accordingly
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        //Update the activity orientation
        super.onConfigurationChanged(newConfig);
        //Update the camera preview orientation
        CameraView.setCameraDisplayOrientation(this, Camera.CameraInfo.CAMERA_FACING_BACK, mCamera);
    }

}