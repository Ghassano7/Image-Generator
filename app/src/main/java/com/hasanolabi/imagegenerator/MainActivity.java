package com.hasanolabi.imagegenerator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 1;
    Context context = this;
    EditText inputText;
    MaterialButton generateBtn;
    MaterialButton downloadBtn;
    ProgressBar progressBar;
    ImageView imageView;
     String mImageUrl;

    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputText = findViewById(R.id.input_text);
        generateBtn = findViewById(R.id.generate_btn);
        downloadBtn = findViewById(R.id.download_btn);
        progressBar = findViewById(R.id.progress_bar);
        imageView = findViewById(R.id.image_view);
        if(mImageUrl == null){
            downloadBtn.setVisibility(View.INVISIBLE);
        }
        generateBtn.setOnClickListener((v)->{
            String text = inputText.getText().toString().trim();
            if(text.isEmpty()){
                inputText.setError("Text can't be empty");
                return;
            }
            callAPI(text);
        });
        downloadBtn.setOnClickListener((v)->{
            if(mImageUrl!= null){
                imageDownload(mImageUrl);
            }
        });

    }

    void callAPI(String text){
        //API CALL
        setInProgress(true);
        JSONObject jsonBody = new JSONObject();
        try{
            jsonBody.put("prompt",text);
            jsonBody.put("size","256x256");
        }catch (Exception e){
            e.printStackTrace();
        }
        RequestBody requestBody = RequestBody.create(jsonBody.toString(),JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/images/generations")
                .header("Authorization","Bearer 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")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(()-> {
                    e.printStackTrace();
                        Toast.makeText(getApplicationContext(),"Failed to generate image"+e,Toast.LENGTH_LONG).show();
                    setInProgress(false);

                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)  {

                try{
                    ResponseBody body = response.body();
                    if(body != null){
                        String bodyString = body.string();
                        Log.i("com.hasanolabi.imagegenerator",bodyString);
                        JSONObject jsonObject = new JSONObject(bodyString);
                        boolean isError = jsonObject.has("error");
                        if(isError){
                            Toast.makeText(getApplicationContext(),"Failed to generate image"+bodyString,Toast.LENGTH_LONG).show();
                            setInProgress(false);
                            return;
                        }
                        String imageUrl = jsonObject.getJSONArray("data").getJSONObject(0).getString("url");
                        loadImage(imageUrl);
                        mImageUrl = imageUrl;
                        setInProgress(false);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

    }
    //save image
    public void imageDownload( String url){
        Picasso.get()
                .load(url)
                .into(getTarget(url));
    }
    private Target getTarget(final String url){
        return new Target(){

            @Override
            public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        final ContentValues values = new ContentValues();
                        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "Image");
                        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM);

                        final ContentResolver resolver = context.getContentResolver();
                        Uri uri = null;

                        try {
                            final Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                            uri = resolver.insert(contentUri, values);

                            if (uri == null)
                                throw new IOException("Failed to create new MediaStore record.");

                            try (final OutputStream stream = resolver.openOutputStream(uri)) {
                                if (stream == null)
                                    throw new IOException("Failed to open output stream.");

                                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream))
                                    throw new IOException("Failed to save bitmap.");
                            }

//                            return uri;
                        }
                        catch (IOException e) {

                            if (uri != null) {
                                // Don't leave an orphan entry in the MediaStore
                                resolver.delete(uri, null, null);
                            }

//                            throw e;
                        }

//
//
//                        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/" + url);
//                        try {
//                            file.createNewFile();
//                            FileOutputStream ostream = new FileOutputStream(file);
//                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, ostream);
//                            ostream.flush();
//                            ostream.close();
//                        } catch (IOException e) {
//                            Log.e("IOException", e.getLocalizedMessage());
//                        }
                    }
                }).start();

            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {

            }


            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };
    }
    void setInProgress(boolean inProgress){
        runOnUiThread(()->{
            if(inProgress){
                progressBar.setVisibility(View.VISIBLE);
                generateBtn.setVisibility(View.GONE);
            }else{
                progressBar.setVisibility(View.GONE);
                generateBtn.setVisibility(View.VISIBLE);
            }
        });

    }

    void loadImage(String url){
        //load image

        runOnUiThread(()-> {
            Picasso.get().load(url).into(imageView);
            downloadBtn.setVisibility(View.VISIBLE);
        });

    }
}


















