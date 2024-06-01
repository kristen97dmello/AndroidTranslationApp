package ds.edu.translateapplication;
/*
Name: Kristen Dmello
AndrewID: kdmello
 */
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class MainActivity extends AppCompatActivity {

    // UI components
    private Spinner sourceLanguage;
    private Spinner targetLanguage;
    private Button translateButton;
    private TextView translatedTextView;

    // Handler to post tasks to the main thread
    private Handler mainHandler;


    // ExecutorService for background tasks
    private ImageView googleTranslateImage;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the XML view for this activity
        setContentView(R.layout.activity_main);
        mainHandler = new Handler(Looper.getMainLooper());

        // Initialize UI components
        sourceLanguage = findViewById(R.id.sourceLanguage);
        targetLanguage = findViewById(R.id.targetLanguage);
        translateButton = findViewById(R.id.translateButton);
        translatedTextView = findViewById(R.id.translatedText);
        googleTranslateImage = findViewById(R.id.googleTranslateImage);

        // Load an image asynchronously
        loadImage("https://developers.google.com/static/ml-kit/images/on_device_translate2x.png");

        // Set up language options for spinners
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,R.array.language_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceLanguage.setAdapter(adapter);
        targetLanguage.setAdapter(adapter);

    // Set up the spinners
        translateButton.setOnClickListener(v -> {
        String text = ((EditText)findViewById(R.id.textToTranslate)).getText().toString();
        String source = sourceLanguage.getSelectedItem().toString();
        String target = targetLanguage.getSelectedItem().toString();
        translateText(text, source, target);
    });
}

// Code to load an image from a url, in case the png file is unavailable
    private void loadImage(final String imageUrl) {
        executorService.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(imageUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    final Bitmap bmp = BitmapFactory.decodeStream(connection.getInputStream());
                    mainHandler.post(() -> googleTranslateImage.setImageBitmap(bmp));
                } else {
                    Log.e("Image Load Error", "HTTP error code: " + responseCode);
                }
            } catch (Exception e) {
                Log.e("Image Load Error", "Exception while loading image", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    /**
     * Translates text from a source language to a target language.
     * @param text The text to translate.
     * @param sourceLang The source language.
     * @param targetLang The target language.
     */
    private void translateText(final String text, final String sourceLang, final String targetLang) {
        executorService.execute(() -> {
            // Network call and text translation logic here
            try {
                String encodedText = URLEncoder.encode(text, "UTF-8");
                URL url = new URL("https://expert-potato-wrr6rv5rvwq429vp6-8080.app.github.dev/translate?text=" + encodedText + "&source=" + sourceLang + "&target=" + targetLang);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    JSONObject jsonObject = new JSONObject(stringBuilder.toString());
                    String translatedText = jsonObject.getJSONObject("data").getJSONArray("translations").getJSONObject(0).getString("translatedText");
                    // Now update UI on the main thread
                    runOnUiThread(() -> {
                        // Assuming 'translatedTextView' is a TextView in your Activity
                        translatedTextView.setText(translatedText);
                    });
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        translatedTextView.setText(R.string.translation_error);
                    }
                });
            }
        });
    }

    // Shutdown your executorService when it's no longer needed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

}
