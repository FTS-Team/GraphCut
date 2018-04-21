package com.sky.graphcut;

import java.util.ArrayList;

public class GCGraph {
    //结点类
    private class Vtx {
        public Vtx next;        //用于构建先进先出队列
        public int parent;      //父节点发出的弧
        public int first;       //从该结点发出的第一条弧
        public int ts;          //time-stamp时间戳
        public int dist;        //distance,到树根的距离
        public float weight;   //t-value, 即到终端结点的权值
        public int is_sink;     //只能是0或1，0表示前景点(源节点)，1表示背景点(汇节点)
    }
    //弧类
    private class Edge {
        public int dst;         //弧指向的顶点
        public int next;        //同一个原点的下一条弧
        public float weight;   //弧的权值
    }

    private ArrayList<Vtx>  vtcs;   //存储所有的结点
    private ArrayList<Edge> edges;  //存储所有的弧
    private float flow;            //图的流量

    //构造函数
    public GCGraph() {
        flow = 0;
    }
    public GCGraph(int vtxCount, int edgeCount) {
        create(vtxCount, edgeCount);
    }
    public void create(int vtxCount, int edgeCount) {
        vtcs = new ArrayList<Vtx>(vtxCount);
        edges = new ArrayList<Edge>(edgeCount+2);
        flow = 0;
    }

    //添加空结点,返回结点索引号
    public int addVtx(){
        Vtx v = new Vtx();
        vtcs.add(v);
        return vtcs.size()-1;
    }

    //添加结点i和结点j之间的弧 n-link (普通结点之间的弧)
    public void addEdges(int i, int j, float w, float revw) {

        //check i and j
        if(i < 0 || i >= vtcs.size() || j < 0 || j >= vtcs.size() || i == j) {
            return;
        }
        //check weight and reWeight
        if(w < 0 || revw < 0) {
            return;
        }
        if( edges.size() == 0 ){
            edges.add(new Edge());
            edges.add(new Edge());
        }

        Edge fromI = new Edge();
        Edge toI = new Edge();
        //插入正向弧
        fromI.dst = j;
        fromI.weight = w;
        fromI.next = vtcs.get(i).first;
        vtcs.get(i).first = edges.size();
        edges.add(fromI);
        //插入反向弧
        toI.dst = i;
        toI.weight = revw;
        toI.next = vtcs.get(j).first;
        vtcs.get(j).first = edges.size();
        edges.add(toI);
    }

    //为结点i的添加一条弧 t-link (到终端结点的弧)
    public void addTermWeights(int i, float sourceW, float sinkW) {
        //check i
        if(i < 0 || i >= vtcs.size()) {
            return;
        }

        //如果该结点既连接source点，也连接sink点，则计算flow和该点weight
        float dw = vtcs.get(i).weight;
        if(dw > 0){
            sourceW += dw;
        }
        else{
            sinkW -= dw;
        }
        flow += (sourceW < sinkW)? sourceW : sinkW;
        vtcs.get(i).weight = sourceW - sinkW;
    }

    //最大流最小割算法
    public float maxFlow() {
        //本函数中仅有的可能出现的负值,下面如果存在判别某值是否小于0，
        //意味着判断当前结点是否为终端结点，或者孤立点
        final int TERMINAL = -1, ORPHAN = -2;

        int curr_ts = 0;

        //先进先出队列
        Vtx stub = new Vtx(), nilNode = stub, first = nilNode, last = nilNode;
        stub.next = nilNode;

        //孤点集合
        ArrayList<Vtx> orphans = new ArrayList<Vtx>();

        // 遍历所有的结点，初始化先进先出队列即 active结点队列
        for(int i = 0; i < vtcs.size(); i++) {
            Vtx v = vtcs.get(i);
            v.ts = 0;
            if(v.weight != 0) {
                last = last.next = v;
                v.dist = 1;
                v.parent = TERMINAL;
                if(v.weight < 0) {
                    v.is_sink = 1;
                }
                else {
                    v.is_sink = 0;
                }
            }
            else{
                v.parent = 0;
            }
        }
        first = first.next;
        last.next = nilNode;
        nilNode.next = null;

        /*
        for(int i = 0; i < vtcs.size(); i++)
        {
            Vtx temp = vtcs.get(i);
            System.out.println(temp.parent + "  "+temp.weight + "  "+temp.first
                    + "  "+temp.is_sink + "  "+temp.ts + "  "+temp.dist  );
        }
        System.out.println();
        for(int i = 0; i < edges.size(); i++){
            Edge temp = edges.get(i);
            System.out.println(temp.dst + "  "+temp.next + "  "+temp.weight);
        }*/

        while(true)
        {
            Vtx current, neighbor;          //current表示当前元素，neighbor为其相邻元素
            int e0 = -1, ei = 0, ej = 0;
            float minWeight, weight;
            int current_isSink;

            //1. growth: S 和 T 树的生长，找到一条s->t的路径
            while(first != nilNode)
            {
                current = first;
                if( current.parent != 0){     // current非孤点
                    //System.out.println("growth: "+current.parent);

                    current_isSink = current.is_sink;
                    //广度优先搜索，以此搜索当前结点所有相邻结点
                    for(ei = current.first; ei != 0; ei = edges.get(ei).next){
                        //System.out.println("deepSearch: "+ei + "  "+(ei^current_isSink) + "  " + edges.get(ei^current_isSink).weight);

                        //每对结点都拥有两个反向的边，ei^vt表明检测的边是与v结点同向的
                        if(edges.get(ei^current_isSink).weight == 0){
                            continue;
                        }
                        //取出邻接点neighbor
                        neighbor = vtcs.get(edges.get(ei).dst);
                        //System.out.println(neighbor.first);
                        //如果neighbor为孤点，current接受neighbor为其子节点
                        if(neighbor.parent == 0){
                            neighbor.is_sink = current_isSink;
                            neighbor.parent = ei^1;
                            neighbor.ts = current.ts;
                            neighbor.dist = current.dist + 1;
                            //neighbor不在队列中，入队，插入位置为队尾
                            if(neighbor.next == null){
                                neighbor.next = nilNode;
                                last = last.next = neighbor;
                            }
                            continue;
                        }
                        //neighbor和current的isSink不同，则找到一条路径
                        if(neighbor.is_sink != current_isSink)
                        {
                            e0 = ei ^ current_isSink;
                            break;
                        }
                        //如果neighbor已经存在父节点，
                        //但是如果neighbor的路径长度大于current+1，且current的时间戳较早，
                        //说明current走弯路了，修改current的路径，使其成为current的子结点
                        if(neighbor.dist > (current.dist+1) && neighbor.ts <= current.ts){
                            neighbor.parent = ei ^ 1;
                            neighbor.ts = current.ts;
                            neighbor.dist = current.dist+1;
                        }
                    }
                    if(e0 > 0){
                        break;
                    }
                }
                //出队列
                first = first.next;
                current.next = null;
            }
            if(e0 <= 0)
            {
                break;
            }

            //2.augmentation: 流量统计与树的拆分

            //2.1查找路径中的最小权值:
            //遍历整条路径分两个方向进行，从当前结点开始，向前回溯s树，向后回溯t树
            minWeight = edges.get(e0).weight;
            if(minWeight <= 0){
                System.exit(0);
            }
            // 2次遍历， k=1: 回溯s树， k=0: 回溯t树
            for (int k = 1; k >= 0; k--) {
                for( current = vtcs.get(edges.get(e0^k).dst); ;current = vtcs.get(edges.get(ei).dst)){
                    if((ei = current.parent) < 0){
                        break;
                    }
                    weight = edges.get(ei^k).weight;
                    minWeight = Math.min(minWeight,weight);
                    if(minWeight <= 0){
                        System.exit(0);
                    }
                }
                weight = Math.abs(current.weight);
                minWeight = Math.min(minWeight,weight);
                if(minWeight <= 0){
                    System.exit(0);
                }
            }

            //2.2修改当前路径中的所有的weight权值
            edges.get(e0).weight -= minWeight;
            edges.get(e0^1).weight += minWeight;
            flow += minWeight;
            // 2次遍历， k=1: 回溯s树， k=0: 回溯t树
            for (int k = 1; k >= 0; k--) {
                for( current = vtcs.get(edges.get(e0^k).dst); ;current = vtcs.get(edges.get(ei).dst)){
                    if((ei = current.parent) < 0){
                        break;
                    }
                    edges.get(ei^(k^1)).weight += minWeight;
                    if((edges.get(ei^k).weight -= minWeight) == 0){
                        orphans.add(current);
                        current.parent = ORPHAN;
                    }
                }
                current.weight = current.weight + minWeight*(1-k*2);
                if(current.weight == 0){
                    orphans.add(current);
                    current.parent = ORPHAN;
                }
            }

            //3.adoption: 树的重构 寻找新的父节点，恢复搜索
            curr_ts++;
            while (!orphans.isEmpty()){
                //删除孤点栈顶元素并取出
                Vtx current2 = orphans.get(orphans.size()-1);
                orphans.remove(orphans.size()-1);

                int d, minDist = Integer.MAX_VALUE;
                e0 = 0;
                current_isSink = current2.is_sink;

                //遍历当前结点的相邻点，ei为当前弧的编号
                for(ei = current2.first; ei != 0; ei = edges.get(ei).next){
                    if(edges.get(ei^(current_isSink^1)).weight == 0){
                        continue;
                    }
                    neighbor = vtcs.get(edges.get(ei).dst);
                    if(neighbor.is_sink != current_isSink || neighbor.parent == 0){
                        continue;
                    }

                    // compute the distance to the tree root
                    for(d=0; ; ){
                        if(neighbor.ts == curr_ts){
                            d += neighbor.dist;
                            break;
                        }
                        ej = neighbor.parent;
                        d++;
                        if(ej < 0){
                            if(ej == ORPHAN){
                                d = Integer.MAX_VALUE - 1;
                            }
                            else{
                                neighbor.ts = curr_ts;
                                neighbor.dist = 1;
                            }
                            break;
                        }
                        neighbor = vtcs.get(edges.get(ej).dst);
                    }
                    // update the distance
                    if(++d < Integer.MAX_VALUE){
                        if(d < minDist){
                            minDist = d;
                            e0 = ei;
                        }
                        for(neighbor = vtcs.get(edges.get(ei).dst); neighbor.ts != curr_ts;
                            neighbor = vtcs.get(edges.get(neighbor.parent).dst)){
                            neighbor.ts = curr_ts;
                            neighbor.dist = --d;
                        }
                    }
                }

                if((current2.parent = e0) > 0){
                    current2.ts = curr_ts;
                    current2.dist = minDist;
                    continue;
                }
                /* no parent is found */
                current2.ts = 0;
                for(ei = current2.first; ei != 0; ei = edges.get(ei).next){
                    neighbor = vtcs.get(edges.get(ei).dst);
                    ej = neighbor.parent;
                    if(neighbor.is_sink != current_isSink || ej == 0){
                        continue;
                    }
                    if(edges.get(ei^(current_isSink^1)).weight != 0 && neighbor.next != null){
                        neighbor.next = nilNode;
                        last = last.next = neighbor;
                    }
                    if(ej > 0 && vtcs.get(edges.get(ej).dst) == current2){
                        orphans.add(neighbor);
                        neighbor.parent = ORPHAN;
                    }
                }
            }
        }
        return flow;
    }

    //判断结点i是否为前景点
    public boolean inSourceSegment( int i ) {
        if( i < 0 || i >= vtcs.size())
        {
            return false;
        }
        return vtcs.get(i).is_sink == 0;
    }

}
