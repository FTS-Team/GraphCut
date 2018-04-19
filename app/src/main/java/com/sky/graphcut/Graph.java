package com.sky.graphcut;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

public class Graph {

    //灰度均值
    private static int aver_object;
    private static int aver_background;

    //图像处理参数
    private static final int DETA_B = 5;//相邻能量值高斯参数
    private static final int DETA_P = 3;//概率分布的高斯参数
    private static final double WEIGHT_P = 0.2;//P能量所占权重

    //背景or对象
    public static final double OBJECT = 0.0;
    public static final double BACKGROUND = 1.0;
    public static final double OTHER = 2.0;

    //周围8个点
    public static int []offsetX = {-1, 0, 1,1,1,0,-1,-1};
    public static int []offsetY = {-1,-1,-1,0,1,1, 1, 0};


    //根据位图构建图
    public static void buildGraph(Bitmap img_chosen,double [][] graph){

        //对象与背景各自均值设置
        setAverGray(img_chosen,graph);

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
                    else {
                        graph[index][k] = -1.0;
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


    private static double getBEnergy(int Ip,int Iq){

        return Math.exp((-(Ip-Iq)*(Ip-Iq)/(2*DETA_B*DETA_B)));

    }

    private static double getREnergy_S(Bitmap img_chosen,double[][] graph, int x,int y){

        int width = img_chosen.getWidth();
        int index = y*width + x;
        double type = graph[index][10];
        if(type == OBJECT){

            double maxBEnergy = 0;
            for(int i = 0; i < 8; i++){
                if(graph[index][i] > maxBEnergy){
                    maxBEnergy = graph[index][i];
                }
            }
            return 1 + maxBEnergy;
        }
        else if(type == BACKGROUND){
            return 0.0;
        }
        else {
            //当前点灰度值
            int Ip = getGray(img_chosen,x,y);
            //当前点属于背景点的概率
            double pr;
            if(aver_background == -1.0){
                pr = 0.5;
            }
            else {
                pr = Math.exp(-(Ip-aver_background)*(Ip-aver_background)/(2*DETA_P*DETA_P));
            }
            return WEIGHT_P*-Math.log(pr);
        }

    }
    private static double getREnergy_T(Bitmap img_chosen,double[][] graph,int x,int y){

        int width = img_chosen.getWidth();
        int index = y*width + x;
        double type = graph[index][10];
        if(type == OBJECT){
            return 0.0;
        }
        else if(type == BACKGROUND){
            double maxBEnergy = 0;
            for(int i = 0; i < 8; i++){
                if(graph[index][i] > maxBEnergy){
                    maxBEnergy = graph[index][i];
                }
            }
            return 1 + maxBEnergy;
        }
        else {
            //当前点灰度值
            int Ip = getGray(img_chosen,x,y);
            //当前点属于物体的概率
            double pr;
            if(aver_object == -1.0){
                pr = 0.5;
            }
            else {
                pr = Math.exp(-(Ip-aver_object)*(Ip-aver_object)/(2*DETA_P*DETA_P));
            }

            return WEIGHT_P*(-Math.log(pr));
        }

    }

    //根据当点的分类获取各类别灰度均值
    private static void setAverGray(Bitmap img_chosen,double[][] graph){

        int sum_object = 0;
        int sum_background = 0;
        int count_object = 0;
        int count_background = 0;

        int width = img_chosen.getWidth();
        int height = img_chosen.getHeight();

        for(int i = 0; i < width; i++){
            for(int j = 0; j < height ; j++){

                //像素点的索引
                int index = j*width + i;

                if(graph[index][10] == OBJECT){

                    sum_object += getGray(img_chosen,i,j);
                    count_object ++;

                }
                else if(graph[index][10] == BACKGROUND){

                    sum_background += getGray(img_chosen,i,j);
                    count_background ++;

                }

            }
        }

        if(count_object == 0){
            aver_object = -1;
        }
        else {
            aver_object = sum_object / count_object;
        }

        if(count_background == 0){
            aver_background = -1;
        }
        else {
            aver_background = sum_background / count_background;
        }

    }

}
