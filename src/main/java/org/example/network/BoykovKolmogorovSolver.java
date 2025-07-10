package org.example.network;

import java.sql.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class BoykovKolmogorovSolver extends NetworkFlowSolverBase{

    Queue<Integer> treeS;
    Queue<Integer> treeT;

    Queue<Integer> activeS;
    Queue<Integer> activeT;

    Queue<Integer> orphans;


    // false for S tree, true for T tree
    private boolean[] nodeInTree;

    // path information for augmenting tree operations
    private Edge[] parent;


    public BoykovKolmogorovSolver(int n, int s, int t){
        super(n, s, t);
    }

    @Override
    public void solve(){

        nodeInTree = new boolean[n];

        parent = new Edge[n];

        treeS = new ArrayDeque<>();
        treeT = new ArrayDeque<>();

        activeS = new ArrayDeque<>();
        activeT = new ArrayDeque<>();


        // initiate
        visit(s);
        visit(t);
        treeS.offer(s);
        treeT.offer(t);
        activeS.offer(s);
        activeT.offer(t);



        while(!activeS.isEmpty()){
            int startS = activeS.poll();
            int startT = activeT.poll();

            int sLeafGrow = activeGrow(startS);
            int tleafGrow = activeGrow(startT);

            if(sLeafGrow > 0){
                augmentPath();
            }

            if(tleafGrow > 0){
                augmentPath();
            }
        }

    }


    /**
     * check adjacent neighbors for the node
     */
    public int activeGrow(int n){

        for(Edge edge : graph[n]){
            if(edge.capacity - edge.flow <= 0) continue;

            int neighbor = edge.to;

            if(!treeS.contains(neighbor) && !treeT.contains(neighbor)){
                // add to appropriate search tree and mark as active node
                if(!nodeInTree[n]) {
                    treeS.add(neighbor);
                    activeS.add(neighbor);
                } else {
                    treeT.add(neighbor);
                    activeT.add(neighbor);
                }
                nodeInTree[neighbor] = nodeInTree[n];

                parent[neighbor] = edge;
            }

            // tree collision
            if(nodeInTree[n] == !nodeInTree[neighbor]){
                return neighbor;
            }
        }

        return -1;
    }


    /**
     * augments path with previous history provided for all nodes
     */
    public void augmentPath(){
        long bottleneck = Long.MAX_VALUE;

        // find the bottleneck value by tracing the augmenting path back from t
        for(Edge edge = parent[t]; edge != null; edge = parent[edge.from]){
            bottleneck = Math.min(bottleneck, edge.capacity - edge.flow);
        }

        // augment the path and create orphans for the set O
        for(Edge edge = parent[t]; edge != null; edge = parent[edge.from]){
            edge.augment(bottleneck);

            // move node to orphans set if edge to node is saturated
            if(edge.capacity - edge.from <= 0){
                treeS.remove(edge.to);
                activeS.remove(edge.to);
                orphans.offer(edge.to);
            }
        }
    }



}
