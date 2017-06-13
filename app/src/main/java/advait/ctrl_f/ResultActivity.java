package advait.ctrl_f;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import static android.provider.AlarmClock.EXTRA_MESSAGE;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        Intent intent = getIntent();
        //Bitmap highlightedImage = (Bitmap) intent.getParcelableExtra("BitmapImage");
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        System.out.println("OLALALALLA");
        System.out.println(message);
        ImageView imageView = (ImageView) findViewById(R.id.image_view);
        Bitmap myBitmap = BitmapFactory.decodeFile(message);
        imageView.setImageBitmap(myBitmap);
    }
}
