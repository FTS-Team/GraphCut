package com.sky.graphcut;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    //request code
    public static final int REQUEST_ALBUM = 1;

    //view and button
    private RelativeLayout view_choose_photo;
    private RelativeLayout view_chosen_photo;
    private Button btn_rechoose_photo;
    private Button btn_confirm_photo;
    private ImageView img_chosen_photo;

    //chosen photo
    Bitmap img_chosen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //find layout and button and imgView
        view_choose_photo = (RelativeLayout) findViewById(R.id.view_choose_photo);
        view_chosen_photo = (RelativeLayout) findViewById(R.id.view_chosen_photo);

        btn_rechoose_photo = (Button) findViewById(R.id.btn_rechoose_photo);
        btn_confirm_photo = (Button) findViewById(R.id.btn_confirm_photo);

        img_chosen_photo = (ImageView) findViewById(R.id.img_chosen);

        //set onClickListener
        view_choose_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(MainActivity.this, "选择图片", Toast.LENGTH_SHORT).show();
                pickImageFromAlbum();
            }
        });
        btn_rechoose_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(MainActivity.this, "重新选择图片", Toast.LENGTH_SHORT).show();
                pickImageFromAlbum();
            }
        });
        btn_confirm_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "确认图片", Toast.LENGTH_SHORT).show();
            }
        });

    }

    //switch Visibility between relativeLayout: view_choose_photo and view_chosen_photo
    private void switchViews() {
        view_choose_photo.setVisibility(View.INVISIBLE);
        view_chosen_photo.setVisibility(View.VISIBLE);
    }

    //onClickListener of choose photo
    public void pickImageFromAlbum() {
        Intent albumIntent = new Intent(Intent.ACTION_PICK);
        albumIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(albumIntent, REQUEST_ALBUM);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (RESULT_OK != resultCode) {
            Toast.makeText(MainActivity.this, "点击取消从相册选择", Toast.LENGTH_SHORT).show();
            return;
        }

        switchViews();

        switch (requestCode) {
            case REQUEST_ALBUM:
                try {
                    Uri imageUri = data.getData();
                    Log.e("TAG", imageUri.toString());
                    img_chosen_photo.setImageURI(imageUri);
                    img_chosen = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

        }
    }
}

