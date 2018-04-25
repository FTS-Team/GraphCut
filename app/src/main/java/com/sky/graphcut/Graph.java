package com.sky.graphcut;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import java.util.Random;

import java.util.ArrayList;
import java.util.List;

public class Graph {

    //灰度均值
//    private static boolean[] histogram_object = new boolean[256];
//    private static boolean[] histogram_background = new boolean[256];
      private static short[] histogram_object = new short[257];//最后一位表示总数
      private static short[] histogram_background = new short[257];

    //图像处理参数
    private static final int DETA_B = 5;//相邻能量值高斯参数
    private static final float WEIGHT_P = 0.0f;//P概率所占权重
    private static final  float K = 9;
    private static final  float MinCap = 0.01f;

    //背景or对象
    public static final float OBJECT = 0;
    public static final float BACKGROUND = 1;
    public static final float OTHER = 2;

    //周围8个点
    public static int []offsetX = {-1, 0, 1,1,1,0,-1,-1};
    public static int []offsetY = {-1,-1,-1,0,1,1, 1, 0};


    //根据位图构建图
    public static void buildGraph(Bitmap img_chosen,float [][] graph){

        //对象与背景各自均值设置
        setHistogram(img_chosen,graph);

        //Log.d("gray", "object: " + aver_object+" ; background: " + aver_background );
        //为graph赋值-完成图的构建
        int width = img_chosen.getWidth();
        int height = img_chosen.getHeight();
        for(int i = 0; i < width; i++){
            for(int j = 0; j < height ; j++){

                int index = j * width + i;//graph中的索引
                int Ip = getGray(img_chosen,i,j);//当前点灰度值

                //周围8个点代价
                for(int k = 0; k < 8; k++){

                    //对应的行列
                    int nextX = i + offsetX[k];
                    int nextY = j + offsetY[k];

                    if(nextX >= 0 && nextX < width && nextY >=0 && nextY < height){
                        int Iq = getGray(img_chosen,nextX,nextY);
                        graph[index][k] = getBEnergy(Ip,Iq);
                    }
                    else {//边缘点
                        graph[index][k] = 0;
                    }

                }

                //从S点（Object)到当前点的代价
                graph[index][8] = getREnergy_S(img_chosen,graph,i,j);
                //当前点到T点(Background)的代价
                graph[index][9] = getREnergy_T(img_chosen,graph,i,j);

            }
        }

    }

    private static int getGray(Bitmap bitmap,int x,int y){

        int R = Color.red(bitmap.getPixel(x,y));
        int G = Color.green(bitmap.getPixel(x,y));
        int B = Color.blue(bitmap.getPixel(x,y));
        return (R*38 + G*75 + B*15) >> 7;

    }


    private static float getBEnergy(int Ip,int Iq){

        float res = (float)Math.exp((-(Ip-Iq)*(Ip-Iq)/(2*DETA_B*DETA_B)));

        if(res < MinCap) {
            res = 0;
        }

        return res;

    }

    private static float getREnergy_S(Bitmap img_chosen,float[][] graph, int x,int y){
        int width = img_chosen.getWidth();
        int index = y*width + x;
        float type = graph[index][10];
        if(type == OBJECT){
           return K;
        }
        else if(type == BACKGROUND){
            return 0;
        }
        else {

            int Ip = getGray(img_chosen,x,y);
            if(histogram_object[Ip] > 0){
                return K * WEIGHT_P;
            }
            else {
                return 0;
            }

//            if(histogram_background[Ip] == 0){
//                return WEIGHT_P*K;
//            }
//            else {
//                float Energy = (float)-Math.log((histogram_background[Ip]/histogram_background[256]));
//                if(Energy > K){
//                    Energy = K;
//                }
//                if(Energy < MinCap) {
//                    Energy = 0;
//                }
//                return Energy*WEIGHT_P;
//            }
        }
    }
    private static float getREnergy_T(Bitmap img_chosen,float[][] graph,int x,int y){

        int width = img_chosen.getWidth();
        int index = y*width + x;
        float type = graph[index][10];
        if(type == OBJECT){
            return 0;
        }
        else if(type == BACKGROUND){
            return K;
        }
        else {
            int Ip = getGray(img_chosen,x,y);

            if(histogram_background[Ip] > 0){
                return K*WEIGHT_P;
            }
            else {
                return 0;
            }

//            if(histogram_object[Ip] == 0){
//                return WEIGHT_P*K;
//            }
//            else {
//                float Energy = (float)-Math.log((histogram_object[Ip]/histogram_object[256]));
//                if(Energy > K){
//                    Energy = K;
//                }
//                if(Energy < MinCap) {
//                    Energy = 0;
//                }
//                return WEIGHT_P*Energy;
//            }
        }

    }

        //设置直方图
        private static void setHistogram(Bitmap img_chosen,float[][] graph){


        //初始化
        for(int i = 0; i < 257; i++){
            histogram_object[i] = 0;
            histogram_background[i] = 0;
        }

        int width = img_chosen.getWidth();
        int height = img_chosen.getHeight();

        for(int i = 0; i < width; i++){
            for(int j = 0; j < height ; j++){
                //像素点的索引
                int index = j*width + i;
                int Ip = getGray(img_chosen,i,j);
                if(graph[index][10] == OBJECT){
                    histogram_object[Ip]++;
                    histogram_object[256]++;
                }
                else if(graph[index][10] == BACKGROUND){
                    histogram_background[Ip]++;
                    histogram_background[256]++;
                }

            }
        }


    }

    //maxFlow/minCut
    public static void minCut(Bitmap img_chosen,float [][] graph){


        int width = img_chosen.getWidth();
        int height = img_chosen.getHeight();
        int size = width * height;
        boolean []addPath = new boolean[width*height];//是否已遍历
        float []cap = new float[1];//当前路径最小容量

        //最大流
        for(int i = 0; i < size; ){
            //Log.d("tag", "maxFlow: " + "i :"+ i);
            for(int j = 0; j < size;j++){
                addPath[j] = false;
            }
            if(graph[i][8] > 0){
                cap[0] = graph[i][8];
                addPath[i] = true;
                if(findCut(i,cap,graph,addPath,width)){
                    Log.d("succeed", "Succeed: " + "i :"+ i + " ; cap" +cap[0]);
                    //初始化
                    graph[i][8] -= cap[0];
                    //i = 0;
                }
                else{
                    i++;
                }
            }
            else {
                i++;
            }
        }

        //初始化
        for(int i = 0; i < size; i++){
            addPath[i] = false;
        }
        // 分类
        for(int i = 0; i < size; i++){
            if(graph[i][8] > 0){
                classify(i,graph,addPath,width);
            }
        }

    }

    static private boolean findCut(int cur,float []cap,float[][] graph,boolean[] addPath,int width){

       //Log.d("tag", "findCut: " + "i :"+ cur + " ; cap : " + cap[0]);
        //到达背景点T
        if(graph[cur][9] > 0){
            cap[0] = Math.min(cap[0],graph[cur][9]);
            graph[cur][9] -= cap[0];
            return true;
        }

        //当前点的行列
        int x = cur % width;
        int y = cur / width;

        //Log.d("CapTemp", "CapTemp: " + "cur :"+ cur + " ; cap" + cap[0]);
        float capTemp = cap[0];

        //调整方向
//        lastDirection = (lastDirection + 8 - 2);
//        int []Direction = new int[8];
//        for(int i = 0; i < 8; i++){
//            Direction[i] = (lastDirection + i) % 8;
//        }

        //遍历周围8个点
        for(int i = 0; i < 8; i++){


            if(graph[cur][i] >= MinCap){

                cap[0] = Math.min(capTemp,graph[cur][i]);
                //下个点
                int nextX = x + offsetX[i];
                int nextY = y + offsetY[i];
                int nextIndex = nextY * width + nextX;

                //标记当前已选
                addPath[cur] = true;
                if(!addPath[nextIndex] ){//还没遍历
                    if(findCut(nextIndex,cap,graph,addPath,width)){
                        graph[cur][i] -= cap[0];//减去当前流量
                        if(graph[cur][i] < MinCap){
                            graph[cur][i] = 0;
                        }
                        graph[nextIndex][(i+4)%8] += cap[0];//加上反向流量
                        //graph[cur][10] = OBJECT;
                        return true;
                    }
                }
            }
        }

        cap[0] = capTemp;

        return false;
    }


    static private void classify(int cur,float[][] graph,boolean[] addPath,int width){

        graph[cur][10] = OBJECT;
        //当前点的行列
        int x = cur % width;
        int y = cur / width;

        //遍历周围8个点
        for(int i = 0; i < 8; i++){
            if(graph[cur][i] > 0){//证明不超过边界

                //下个点
                int nextX = x + offsetX[i];
                int nextY = y + offsetY[i];
                int nextIndex = nextY * width + nextX;

                addPath[cur] = true;
                if( !addPath[nextIndex] && graph[nextIndex][10] != BACKGROUND ){//还没遍历
                    classify(nextIndex,graph,addPath,width);
                }
            }
        }

    }
}
