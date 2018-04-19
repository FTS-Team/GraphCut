package com.sky.graphcut;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Canvas;
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



import android.graphics.Matrix;
import android.view.MotionEvent;
import android.view.View.OnTouchListener;




public class MainActivity extends AppCompatActivity {

    public enum DrawType{
        OBJECT,//0
        BACKGROUND,//1
        Other//2
    }

    //request code
    public static final int REQUEST_ALBUM = 1;

    //view and button
    private RelativeLayout view_choose_photo;
    private RelativeLayout view_chosen_photo;
    private Button btn_rechoose_photo;
    private Button btn_confirm_photo;
    private ImageView img_chosen_photo;

    //object or background
    private Button btn_object;
    private Button btn_background;
    private DrawType drawType;
    int radius ;

    //chosen photo
    Bitmap img_chosen;
    Bitmap img_draw;
    //Graph
    double [][]graph;

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

        btn_object = (Button) findViewById(R.id.btn_object);
        btn_background = (Button) findViewById(R.id.btn_background);

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

                //构建图
                Graph.buildGraph(img_chosen,graph);

                //MaxFlow
//                img_chosen_photo.setImageBitmap(img_chosen);
//                int width = img_chosen.getWidth();
//                int height = img_chosen.getHeight();
//                for(int i = 0; i < width*height; i++){
//                    String msg = " ";
//                    for(int j = 0; j < 11; j++){
//
//                        msg += " "+graph[i][j];
//                    }
//                    Log.d("tag", "confirm: " + msg);
//                }

            }
        });


        //Initialize attribute
        drawType = DrawType.OBJECT;
        btn_object.setBackgroundColor(getResources().getColor(R.color.colorAccent));

        btn_object.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                drawType = DrawType.OBJECT;
                btn_object.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                btn_background.setBackgroundColor(getResources().getColor(R.color.colorGray));
            }
        });

        btn_background.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                drawType = DrawType.BACKGROUND;
                btn_background.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                btn_object.setBackgroundColor(getResources().getColor(R.color.colorGray));
            }
        });

        //drawing the picture
        img_chosen_photo.setOnTouchListener(imgSourceOnTouchListener);

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

                    //让位图可修改
                    int width = img_chosen.getWidth();
                    int height = img_chosen.getHeight();
                    Bitmap.Config config = img_chosen.getConfig();
                    img_draw = Bitmap.createBitmap(width,height,config);

                    //创建画笔对象
                    Paint paint = new Paint();
                    //创建画板对象，把白纸铺在画板上
                    Canvas canvas = new Canvas(img_draw);
                    //开始作画，把原图的内容绘制在白纸上
                    canvas.drawBitmap(img_chosen, new Matrix(), paint);

                    //设置位图
                    img_chosen_photo.setImageBitmap(img_draw);

                    //图数据分配初始化
                    graph = new double[width*height][11];
                    for(int i = 0; i < width*height; i++){
                        graph[i][10] = Graph.OTHER;
                    }
                    //初始化线条半径
                    radius = Math.min(width,height)/40;

                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

        }
    }


    OnTouchListener imgSourceOnTouchListener = new OnTouchListener()
    {

        @Override
        public boolean onTouch(View view, MotionEvent event)
        {
            return processDrawImage(view,event);
        }
    };

    private boolean processDrawImage(View view, MotionEvent event){
        //获取X,Y坐标
        float eventX = event.getX();
        float eventY = event.getY();
        float[] eventXY = new float[] {eventX, eventY};

        //将屏幕坐标转换到图片坐标
        Matrix invertMatrix = new Matrix();
        ((ImageView)view).getImageMatrix().invert(invertMatrix);

        invertMatrix.mapPoints(eventXY);
        int x = (int)eventXY[0];
        int y = (int)eventXY[1];


        int width = img_draw.getWidth();
        int height = img_draw.getHeight();

        if(x < 0 || y < 0 || x > width - 1 || y > height - 1){
            return true;
        }

        for(int i = x - radius;i < x + radius ; i++){
            for(int j = y - radius; j < y + radius ; j++){
                if(i >= 0 && j >= 0 && i < width  && j < height
                        && Math.sqrt((i-x)*(i-x)+(j-y)*(j-y)) < radius){

                    //像素点的索引
                    int index = j*width + i;

                    if(drawType == DrawType.OBJECT){
                        img_draw.setPixel(i,j,getResources().getColor(R.color.colorAccent));
                        //分类为对象
                        graph[index][10] = Graph.OBJECT;

                    }
                    else {
                        img_draw.setPixel(i,j,getResources().getColor(R.color.colorPrimary));
                        //分类为背景
                        graph[index][10] = Graph.BACKGROUND;

                    }
                    img_chosen_photo.setImageBitmap(img_draw);
                }

            }
        }
        return true;
    }

}

