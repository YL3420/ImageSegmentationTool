package org.example.network;

import java.sql.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public class BoykovKolmogorovSolver extends NetworkFlowSolverBase{


    Set<Integer> activeS;
    Set<Integer> activeT;

    Set<Integer> orphans;


    // -1 for S tree, 1 for T tree
    private int[] nodeInTree;

    // path information for augmenting tree operations
    private Edge[] parent;

    // helps track children
    private List<Integer> sTree;
    private List<Integer> tTree;


    public BoykovKolmogorovSolver(int n, int s, int t){
        super(n, s, t);
    }

    @Override
    public void solve(){

        nodeInTree = new int[n];
        orphans = new LinkedHashSet<>();

        parent = new Edge[n];

        activeS = new LinkedHashSet<>();
        activeT = new LinkedHashSet<>();

        sTree = new LinkedList<>();
        tTree = new LinkedList<>();

        // initiate
        visit(s);
        visit(t);
        nodeInTree[s] = -1;
        nodeInTree[t] = 1;
        activeS.add(s);
        activeT.add(t);
        sTree.add(s);
        tTree.add(t);

        long flow = Long.MAX_VALUE;

        while(flow != 0) {
            flow = 0;
            // grow tree S and find augmenting path
            while(!activeS.isEmpty()) {
                int curr = activeS.iterator().next();

                Edge collisionEdge = activeGrow(curr);
                if (collisionEdge != null) {
                    flow = augmentPath(collisionEdge);
                    System.out.println(flow);
                    maxFlow += flow;
                    break;
                } else {
                    activeS.remove(curr);
                }
            }

            // grow tree T and find augmenting path
            while (!activeT.isEmpty()){
                int curr = activeT.iterator().next();

                Edge collisionEdge = activeGrow(curr);
                if (collisionEdge != null) {
                    flow = augmentPath(collisionEdge);
                    maxFlow += flow;
                    break;
                } else {
                    activeT.remove(curr);
                }
            }

            adoptOrphans();
        }

    }


    /**
     * check adjacent neighbors for the node
     */
    public Edge activeGrow(int n){

        for(Edge edge : graph[n]){
            if(edge.remainingCapacity() <= 0) continue;

            int neighbor = edge.to;
            if(nodeInTree[n] != 0 && nodeInTree[neighbor] != 0 &&
                    nodeInTree[n] == -nodeInTree[neighbor]){

                // if neighbor is already part of the tree and it's in the opposite tree, report collision
                return edge;
            } else if (parent[neighbor] == null && nodeInTree[neighbor] == 0) {

                // if neighbor is not already in a tree, we incorporate it to the appropriate tree
                if(nodeInTree[n] < 0) {
                    activeS.add(neighbor);
                    sTree.add(neighbor);
                } else if (nodeInTree[n] > 0) {
                    activeT.add(neighbor);
                    tTree.add(neighbor);
                }
                nodeInTree[neighbor] = nodeInTree[n];
                parent[neighbor] = edge;
            }

            // otherwise, we ignore the neighbor
        }

        // return null if there's no collision. In that case, we remove the active node and set it as passive
        return null;
    }


    /**
     * augments path with previous history provided for all nodes
     */
    public long augmentPath(Edge collisionEdge){
        long bottleneck = Long.MAX_VALUE;

        // finding bottleneck backwards
        for(Edge edge = parent[collisionEdge.from]; edge != null; edge = parent[edge.from]){
            if(edge.remainingCapacity() <= 0) return 0;
            bottleneck = Math.min(bottleneck, edge.remainingCapacity());
        }

        for(Edge edge = parent[collisionEdge.to]; edge != null; edge = parent[edge.from]){
            if(edge.remainingCapacity() <= 0) return 0;
            bottleneck = Math.min(bottleneck, edge.remainingCapacity());
        }

        bottleneck = Math.min(bottleneck, collisionEdge.remainingCapacity());

        // augment collision edge
        collisionEdge.augment(bottleneck);

        // augment the path backward
        for(Edge edge = parent[collisionEdge.from]; edge != null; edge = parent[edge.from]){
            edge.augment(bottleneck);

            if(edge.remainingCapacity() <= 0 && parent[edge.to]== edge){
                orphans.add(edge.to);
                activeS.remove(edge.to);
                activeT.remove(edge.to);

                parent[edge.to] = null;
            }
        }

        // augment the path forward
        for(Edge edge = parent[collisionEdge.to]; edge != null; edge = parent[edge.from]){
            edge.augment(bottleneck);

            if(edge.remainingCapacity() <= 0 && parent[edge.to]== edge){
                orphans.add(edge.to);
                activeS.remove(edge.to);
                activeT.remove(edge.to);

                parent[edge.to] = null;
            }
        }

        return bottleneck;
    }


    /**
     * orphan nodes is a terminology used in the paper, it refers to nodes that are neither associated
     * with S or T tree after being disconnected by saturation of an edge, so it is not a free node either.
     *
     * This method processes the orphan nodes and place them back to either the S or T tree via other connections
     * or put them in the free nodes set
     */
    public void adoptOrphans(){
        while(!orphans.isEmpty()){
            int node = orphans.iterator().next();
            boolean adopted = false;

            // use residual edges to find parents
            for(Edge edge : graph[node]){
                int p = edge.to;

                int curr = p;
                while(parent[curr] != null){
                    curr = parent[curr].from;
                }
                if(curr != s) continue;


                if(nodeInTree[node] != 0 && nodeInTree[node] == nodeInTree[p]){
                    parent[node] = edge;
                    adopted = true;
                }
            }


            if(!adopted){
                parent[node] = null;
                nodeInTree[node] = 0;

                // orphan the children
                for(Edge edge : graph[node]){
                    int c = edge.to;

                    if(parent[c] != null && parent[c].from == node){
                        orphans.add(c);
                        activeS.remove(edge.to);
                        activeT.remove(edge.to);
                        parent[c] = null;
                    }
                }
            }

            orphans.remove(node);
        }
    }


    public static void main(String[] args) {
        testSmallFlowGraph();
    }

    // Testing graph from:
    // http://crypto.cs.mcgill.ca/~crepeau/COMP251/KeyNoteSlides/07demo-maxflowCS-C.pdf
    private static void testSmallFlowGraph() {
        int n = 6;
        int s = n - 1;
        int t = n - 2;

        BoykovKolmogorovSolver solver;
        solver = new BoykovKolmogorovSolver(n, s, t);

        // Source edges
        solver.addEdge(s, 0, 10);
        solver.addEdge(s, 1, 10);

        // Sink edges
        solver.addEdge(2, t, 10);
        solver.addEdge(3, t, 10);

        // Middle edges
        solver.addEdge(0, 1, 2);
        solver.addEdge(0, 2, 4);
        solver.addEdge(0, 3, 8);
        solver.addEdge(1, 3, 9);
        solver.addEdge(3, 2, 6);

        System.out.println(solver.getMaxFlow()); // 19
    }
}


