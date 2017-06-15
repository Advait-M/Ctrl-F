package advait.ctrl_f;

/**
 * @author Advait Maybhate
 * Activity used to show image with highlighted words after Tesseract analysis is done.
 * Called by CameraActivity. Allows user to restart the application if desired.
 */

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

//Inherits from AppCompatActivity (default Android activity)
public class ResultActivity extends AppCompatActivity {

    // Called when activity is started - initializes activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use XML to initialize graphical user interface (GUI)
        setContentView(R.layout.activity_result);

        // Get the intent that started this activity
        Intent intent = getIntent();
        // Get image (with highlighted words) path from intent
        String imagePath = intent.getStringExtra(EXTRA_MESSAGE);
        // Find ImageView XML element
        ImageView imageView = (ImageView) findViewById(R.id.image_view);
        // Create bitmap from image path given by intent
        Bitmap myBitmap = BitmapFactory.decodeFile(imagePath);
        // Show image in ImageView
        imageView.setImageBitmap(myBitmap);

        // Restart button allows user to restart the application
        final Button restartButton = (Button) findViewById(R.id.button_restart);
        restartButton.setOnClickListener(
                new View.OnClickListener() {
                    // Whenever restart button is clicked
                    @Override
                    public void onClick(View v) {
                        // Start the CameraActivity using an intent
                        Intent intent = new Intent(ResultActivity.this, CameraActivity.class);
                        startActivity(intent);

                    }
                }
        );
    }
}
// End of ResultActivity class