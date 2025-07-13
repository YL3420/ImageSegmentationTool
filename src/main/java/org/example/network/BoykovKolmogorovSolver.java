package org.example.network;

import java.sql.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public class BoykovKolmogorovSolver extends NetworkFlowSolverBase{


    Set<Integer> activeSet;
    Set<Integer> orphans;


    // -1 for S tree, 1 for T tree
    private int[] nodeInTree;

    // path information for augmenting tree operations
    private Edge[] parent;

    public BoykovKolmogorovSolver(int n, int s, int t){
        super(n, s, t);
    }

    @Override
    public void solve(){

        nodeInTree = new int[n];
        orphans = new LinkedHashSet<>();

        parent = new Edge[n];

        activeSet = new LinkedHashSet<>();

        // initiate
        visit(s);
        visit(t);
        nodeInTree[s] = -1;
        nodeInTree[t] = 1;

        activeSet.add(s);
        activeSet.add(t);

        minCut[s] = true;


        long flow = Long.MAX_VALUE;

        while(true) {

            Edge collisionEdge = activeGrow();
            if(collisionEdge != null){
                flow = augmentPath(collisionEdge);
            } else {
                break;
            }

            if(flow == Long.MAX_VALUE) break;
            maxFlow += flow;

            adoptOrphans();
        }

    }


    /**
     *  for each active node in the active set, expand and incorporate new members to the active set and the
     *  tree. if all neighbors are discovered, active node becomes passive
     *
     *  if collision with another tree is detected, return the collision edge
     *  if all members of active sets are exhausted, return null, no collision found and therefore,
     *  no augmenting path found
     */
    public Edge activeGrow(){

        while(!activeSet.isEmpty()){
            int active = activeSet.iterator().next();

            for(Edge edge : graph[active]){
                if(nodeInTree[active] < 0 && edge.remainingCapacity() <= 0) continue;
                else if(nodeInTree[active] > 0 && edge.residual.remainingCapacity() <= 0) continue;

                int potentialChild = edge.to;

                if(nodeInTree[potentialChild] == 0) {
                    nodeInTree[potentialChild] = nodeInTree[active];
                    parent[potentialChild] = edge;
                    activeSet.add(potentialChild);
                    // incorporate into cut
                    if(nodeInTree[active] == -1) minCut[potentialChild] = true;
                }else if(nodeInTree[potentialChild] != 0 && nodeInTree[potentialChild] != nodeInTree[active]){
                    return edge;
                }
            }
            activeSet.remove(active);
        }

        return null;
    }

    /**
     *  augment the path by the bottleneck value
     *
     *  orphan nodes that are on the receiving end of a saturated edge as a result of the augmentation
     */
    public long augmentPath(Edge collisionEdge){

        // check tree and handle directionality
        if(nodeInTree[collisionEdge.from] > 0){
            collisionEdge = collisionEdge.residual;
        }

        // finding the bottleneck value
        long bottleneck = Long.MAX_VALUE;
        for(Edge edge = parent[collisionEdge.from]; edge != null; edge = parent[edge.from]){
            bottleneck = Math.min(bottleneck, edge.remainingCapacity());
        }
        for(Edge edge = parent[collisionEdge.to]; edge != null; edge = parent[edge.from]){
            bottleneck = Math.min(bottleneck, edge.residual.remainingCapacity());
        }
        bottleneck = Math.min(bottleneck, collisionEdge.remainingCapacity());

        if(bottleneck == Long.MAX_VALUE) return 0;


        // augmenting the path with the bottleneck value
        for(Edge edge = parent[collisionEdge.from]; edge != null; edge = parent[edge.from]){
            edge.augment(bottleneck);

            // orphan node
            int orphan = edge.to;
            if(edge.remainingCapacity() <= 0){
                parent[orphan] = null;
                orphans.add(orphan);
            }
        }
        for(Edge edge = parent[collisionEdge.to]; edge != null; edge = parent[edge.from]){
            edge.residual.augment(bottleneck);

            //orphan node
            int orphan = edge.to;
            if(edge.residual.remainingCapacity() <= 0){
                parent[orphan] = null;
                orphans.add(orphan);
            }
        }
        collisionEdge.augment(bottleneck);

        return bottleneck;
    }


    /**
     * attempt to adopt an orphaned node to the same tree as it was in
     *
     * if not, mark all of its neighbors that connect to the orphan with a non-saturated node as active
     * and mark the node's first layer of children as orphan and process in the next iterations
     */
    public void adoptOrphans(){
        while(!orphans.isEmpty()){
            int orphan = orphans.iterator().next();
            orphans.remove(orphan);

            boolean adopted = false;

            // trying to find valid parent
            for(Edge edge : graph[orphan]){

                int potentialParent = edge.to;

                // stay consistent with parent edge handling
                edge = edge.residual;

                // check if parent is valid, step 1
                if(nodeInTree[potentialParent] != nodeInTree[orphan]) continue;
                if(nodeInTree[potentialParent] < 0 && edge.remainingCapacity() <= 0) continue;
                else if (nodeInTree[potentialParent] > 0 && edge.residual.remainingCapacity() <= 0) continue;

                // check if parent is valid if it's actually rooted at the desired terminal
                if(checkSrc(potentialParent, (nodeInTree[potentialParent] == 1) ? t : s)){
                    parent[orphan] = edge;
                    adopted = true;
                    break;
                }

            }

            if(adopted) continue;
            //  ----- if no valid parent found ------->

            // scan neighbors in the same tree
            for(Edge edge : graph[orphan]){
                int neighbor = edge.to;

                if(nodeInTree[neighbor] < 0)
                    edge = edge.residual;

                if(nodeInTree[neighbor] != 0 && nodeInTree[neighbor] == nodeInTree[orphan]){
                    if(edge.remainingCapacity() > 0){
                        activeSet.add(neighbor);
                    }

                    if(parent[neighbor] != null && parent[neighbor].from == orphan){
                        orphans.add(neighbor);
                        parent[neighbor] = null;
                    }
                }
            }

            // modify minCut
            if(nodeInTree[orphan] == -1) minCut[orphan] = false;
            nodeInTree[orphan] = 0;
            activeSet.remove(orphan);
        }
    }

    /**
     * check if a given node is rooted at the given terminal where terminal = -1, 1
     */
    private boolean checkSrc(int node, int terminal){
        int curr = node;
        while(parent[curr] != null){
//            if(curr == terminal) return true;
            curr = parent[curr].from;
        }

        return curr == terminal;
    }


    /**
     * run the single test case
     */
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


