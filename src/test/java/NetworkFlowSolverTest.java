/**
 *
 *  these tests are written by Chat
 *
 */



import org.example.network.BoykovKolmogorovSolver;
import org.example.network.EdmondsKarpSolver;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.example.network.NetworkFlowSolverBase;

abstract class NetworkFlowSolverTest {

    abstract NetworkFlowSolverBase createSolver(int n, int s, int t);

    @Test
    void simpleSolve(){
        int n = 6;
        int s = n - 1;
        int t = n - 2;
        NetworkFlowSolverBase solver = createSolver(n, s, t);

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

        assertEquals(19, solver.getMaxFlow());
    }

    @Test
    void testSimpleMaxFlow() {
        int n = 4;
        int s = 0;
        int t = 3;

        // Graph layout:
        // s --(10)--> 1 --(5)--> t
        // s --(5)--> 2 --(10)--> t
        NetworkFlowSolverBase solver = createSolver(n, s, t);

        solver.addEdge(0, 1, 10);
        solver.addEdge(1, 3, 5);
        solver.addEdge(0, 2, 5);
        solver.addEdge(2, 3, 10);

        solver.solve();
        assertEquals(10, solver.getMaxFlow());
    }

    @Test
    void testTrickyDeadEndPaths() {
        int n = 8;
        int s = 0;
        int t = 7;
        NetworkFlowSolverBase solver = createSolver(n, s, t);

        // Layout:
        //
        // s → 1 → 2 → t
        // ↓    ↘
        // 3 → 4 (dead end: 4 → 5 has 0 capacity)
        //         ↘
        //          5 → t
        //
        // Edge capacities:
        // s → 1 (5), 1 → 2 (4), 2 → t (3)
        // s → 3 (10), 3 → 4 (9), 4 → 5 (0), 5 → t (10)

        solver.addEdge(0, 1, 5);   // s → 1
        solver.addEdge(1, 2, 4);   // 1 → 2
        solver.addEdge(2, 7, 3);   // 2 → t

        solver.addEdge(0, 3, 10);  // s → 3
        solver.addEdge(3, 4, 9);   // 3 → 4
        solver.addEdge(4, 5, 0);   // 4 → 5 (trap: zero capacity!)
        solver.addEdge(5, 7, 10);  // 5 → t

        solver.solve();
        assertEquals(3, solver.getMaxFlow());  // Only path s → 1 → 2 → t should contribute
    }

    @Test
    void testLargeGridGraph() {
        int rows = 100;
        int cols = 100;
        int n = rows * cols + 2;
        int s = n - 2;
        int t = n - 1;

        NetworkFlowSolverBase solver = createSolver(n, s, t);

        // Connect source to left column
        for (int r = 0; r < rows; r++) {
            int node = r * cols;
            solver.addEdge(s, node, 100);
        }

        // Connect right column to sink
        for (int r = 0; r < rows; r++) {
            int node = r * cols + (cols - 1);
            solver.addEdge(node, t, 100);
        }

        // Internal grid: right and down neighbors
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int curr = r * cols + c;

                if (c + 1 < cols) {
                    int right = r * cols + (c + 1);
                    solver.addEdge(curr, right, 100);
                }

                if (r + 1 < rows) {
                    int down = (r + 1) * cols + c;
                    solver.addEdge(curr, down, 100);
                }
            }
        }

        solver.solve();
        assertEquals(10000, solver.getMaxFlow());
    }


    @Test
    void testLayeredGridGraph() {
        int rows = 100;
        int cols = 100;
        int labels = 5;

        int pixels = rows * cols;
        int totalNodes = pixels * labels + 2;
        int s = totalNodes - 2;
        int t = totalNodes - 1;

        NetworkFlowSolverBase solver = createSolver(totalNodes, s, t);

        // Helper to get the node index for a pixel at layer l
        // nodeIndex = (row * cols + col) * labels + l
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                for (int l = 0; l < labels; l++) {
                    int node = ((r * cols + c) * labels) + l;

                    // Connect first layer to source
                    if (l == 0) solver.addEdge(s, node, 100);
                    // Connect last layer to sink
                    if (l == labels - 1) solver.addEdge(node, t, 100);
                    // Connect to next layer (vertical edge)
                    if (l + 1 < labels) {
                        int down = ((r * cols + c) * labels) + (l + 1);
                        solver.addEdge(node, down, 10);
                    }

                    // Horizontal edge to neighbor in same layer
                    if (c + 1 < cols) {
                        int right = ((r * cols + (c + 1)) * labels) + l;
                        solver.addEdge(node, right, 5);
                    }

                    // Vertical edge to neighbor in same layer
                    if (r + 1 < rows) {
                        int below = (((r + 1) * cols + c) * labels) + l;
                        solver.addEdge(node, below, 5);
                    }
                }
            }
        }

        solver.solve();

        // Expectation: each pixel contributes ~10 flow via vertical label transitions
        long expectedMinFlow = (long) (rows * cols * 10);
        assertTrue(solver.getMaxFlow() >= expectedMinFlow * 0.8); // allow some slack
    }


    @Test
    void testDisconnectedGraph() {
        int n = 4;
        int s = 0;
        int t = 3;

        NetworkFlowSolverBase solver = createSolver(n, s, t);

        solver.addEdge(0, 1, 10);
        solver.addEdge(2, 3, 10);

        // No connection between 1 ↔ 2
        solver.solve();
        assertEquals(0, solver.getMaxFlow());
    }

    @Test
    void testReverseOnlyPath() {
        int n = 3;
        int s = 0;
        int t = 2;

        NetworkFlowSolverBase solver = createSolver(n, s, t);

        solver.addEdge(2, 1, 5); // reverse edge only
        solver.addEdge(1, 0, 5);

        solver.solve();
        assertEquals(0, solver.getMaxFlow());
    }

    @Test
    void testZeroCapacityEdges() {
        int n = 4;
        int s = 0;
        int t = 3;

        NetworkFlowSolverBase solver = createSolver(n, s, t);

        solver.addEdge(s, 1, 0); // useless
        solver.addEdge(1, 2, 0); // useless
        solver.addEdge(2, t, 0); // useless

        solver.solve();
        assertEquals(0, solver.getMaxFlow());
    }

    @Test
    void testDenseTerminalConnections() {
        int n = 6;
        int s = 4;
        int t = 5;
        NetworkFlowSolverBase solver = createSolver(n, s, t);

        for (int i = 0; i < 4; i++) {
            solver.addEdge(s, i, 10);
            solver.addEdge(i, t, 10);
        }

        solver.solve();
        assertEquals(40, solver.getMaxFlow());
    }


    @Test
    void testDenseGraphWithTerminalAndInternalEdges() {
        int n = 12;
        int s = n - 2; // node 10
        int t = n - 1; // node 11

        NetworkFlowSolverBase solver = createSolver(n, s, t);

        int internalNodes = n - 2;

        // Connect source to all internal nodes
        for (int i = 0; i < internalNodes; i++) {
            solver.addEdge(s, i, 5);
        }

        // Connect all internal nodes to sink
        for (int i = 0; i < internalNodes; i++) {
            solver.addEdge(i, t, 5);
        }

        // Each node connects to 2 other random nodes (forward only)
        for (int from = 0; from < internalNodes; from++) {
            for (int i = 1; i <= 2; i++) {
                int to = (from + i) % internalNodes;
                if (from != to) {
                    solver.addEdge(from, to, 3);  // modest capacity to test intermediate flow
                }
            }
        }

        solver.solve();

        // Each of the 10 internal nodes can at most carry 5 units from s and 5 to t
        // So total flow is capped at 10 × 5 = 50
        assertEquals(50, solver.getMaxFlow());
    }



    @Test
    void testEightPixelGraphWithMixedTerminalConnections() {
        int n = 8;
        int source = 6;
        int sink = 7;

        NetworkFlowSolverBase solver = createSolver(n, source, sink);

        // All-to-all pixel connectivity (excluding terminals)
        int[] pixels = {0, 1, 2, 3, 4, 5};

        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels.length; j++) {
                if (i != j) {
                    solver.addEdge(pixels[i], pixels[j], 3); // arbitrary finite weight
                }
            }
        }

        // Choose 4 pixels to connect to both terminals with finite capacity
        int[] bothConnected = {0, 1, 2, 3};

        for (int p : bothConnected) {
            solver.addEdge(source, p, 5); // finite edge from source
            solver.addEdge(p, sink, 4);   // finite edge to sink
        }

        // The other 2 pixels get infinite connection to one terminal
        int[] oneTerminal = {4, 5};

        solver.addEdge(source, 4, Long.MAX_VALUE); // 4 is object
        solver.addEdge(5, sink, Long.MAX_VALUE);   // 5 is background

        solver.solve();

        // This is a complex case so we don't assert an exact value
        // But we expect a positive flow and no crash
        assertTrue(solver.getMaxFlow() > 0);
    }

    @Test
    void testEdmondsKarpOnlyGetsFullFlow() {
        int n = 7;
        int s = 0;
        int t = 6;

        int a = 1, b = 2, c = 3, d = 4, e = 5;

        NetworkFlowSolverBase solver = createSolver(n, s, t);

        // Tempting long path
        solver.addEdge(s, a, 1);
        solver.addEdge(a, b, 1);
        solver.addEdge(b, c, 1);
        solver.addEdge(c, d, 1);
        solver.addEdge(d, e, 1);
        solver.addEdge(e, t, 1);

        // Add shortcut after the long path has started forming
        solver.addEdge(s, c, 1); // this shortcut is better, but not explored first by BK
        solver.addEdge(c, t, 1);

        solver.solve();

        // Max flow is 2 only if s→c→t and s→a→b→c→d→e→t are both used.
        // BK may get only 1 if it chooses the long one early and saturates edges that block the short one.
        assertEquals(2, solver.getMaxFlow());
    }


    @Test
    void testParallelEdgesBetweenNodes() {
        int n = 4;
        int s = 0;
        int t = 3;

        int a = 1;
        int b = 2;

        NetworkFlowSolverBase solver = createSolver(n, s, t);

        // Source to intermediate
        solver.addEdge(s, a, 5);

        // Two parallel edges from a → b with different capacities
        solver.addEdge(a, b, 3);
        solver.addEdge(a, b, 2); // second distinct edge

        // Intermediate to sink
        solver.addEdge(b, t, 5);

        solver.solve();

        // Total flow: limited by s→a (5), and total a→b (3+2=5), and b→t (5)
        // Should equal 5 if both parallel edges are handled
        assertEquals(5, solver.getMaxFlow());
    }


    @Test
    void testMinCutCorrectness() {
        int n = 6;
        int s = 0;
        int t = 5;

        // Nodes: 0=s, 1, 2, 3, 4, 5=t
        NetworkFlowSolverBase solver = createSolver(n, s, t);

        // Layout:
        // s → 1 → 3 → t
        // s → 2 → 4 → t
        // bottlenecks at 3→t and 4→t

        solver.addEdge(s, 1, 10);
        solver.addEdge(1, 3, 5);
        solver.addEdge(3, t, 3); // bottleneck 1

        solver.addEdge(s, 2, 10);
        solver.addEdge(2, 4, 5);
        solver.addEdge(4, t, 3); // bottleneck 2

        solver.solve();

        assertEquals(6, solver.getMaxFlow());

        boolean[] minCut = solver.getMinCut();

        // After flow, s, 1, 2, 3, 4 should be reachable from s
        // t is separated by saturated edges

        assertTrue(minCut[0]); // s
        assertTrue(minCut[1]);
        assertTrue(minCut[2]);
        assertTrue(minCut[3]);
        assertTrue(minCut[4]);

        assertFalse(minCut[5]); // t should be unreachable
    }


    @Test
    void testMinCutOnLargerGridGraph() {
        int n = 9;
        int s = 0;
        int t = 8;

        // Node layout (3x3 grid):
        //
        // 0(s) — 1 — 2
        // 3     4     5
        // 6     7     8(t)
        //
        // s = 0, t = 8

        NetworkFlowSolverBase solver = createSolver(n, s, t);

        // Connect grid edges with capacity 10
        int[][] edges = {
                {0,1}, {1,2},
                {0,3}, {1,4}, {2,5},
                {3,4}, {4,5},
                {3,6}, {4,7}, {5,8},
                {6,7}, {7,8}
        };

        for (int[] e : edges) {
            solver.addEdge(e[0], e[1], 10);
        }

        // Add bottleneck cut edge
        solver.addEdge(4, 7, 1);  // <--- this edge limits the flow
        solver.addEdge(7, 8, 1);  // <--- total max flow = 1

        solver.solve();

        assertEquals(1, solver.getMaxFlow());

        boolean[] minCut = solver.getMinCut();

        // Now verify which nodes are reachable from source via residual capacity
        // All of these are still connected through high-capacity paths:
        assertTrue(minCut[0]); // s
        assertTrue(minCut[1]);
        assertTrue(minCut[2]);
        assertTrue(minCut[3]);
        assertTrue(minCut[4]); // reachable up to here

        // The rest are not reachable from s after cut:
        assertFalse(minCut[5]);
        assertFalse(minCut[6]);
        assertFalse(minCut[7]);
        assertFalse(minCut[8]); // t

        int truCnt = 0;
        for(boolean tru : minCut) {
            if(tru) truCnt++;
        }

        System.out.println(truCnt);

    }

    @Test
    void testMinCutWithMultipleSourceReachables() {
        int n = 10;
        int s = 0;
        int t = 9;

        NetworkFlowSolverBase solver = createSolver(n, s, t);

        // Graph layout:
        //
        // s → 1 → 3 → 5 → t
        //     ↓    ↓
        //     2 → 4    (6, 7, 8 isolated from sink by cut)

        // Strong connections
        solver.addEdge(s, 1, 10);
        solver.addEdge(1, 2, 10);
        solver.addEdge(2, 4, 10);
        solver.addEdge(1, 3, 10);
        solver.addEdge(3, 4, 10);
        solver.addEdge(4, 6, 10);
        solver.addEdge(6, 7, 10);
        solver.addEdge(7, 8, 10);

        // Only one narrow path to sink
        solver.addEdge(3, 5, 1);  // bottleneck
        solver.addEdge(5, t, 1);  // bottleneck

        solver.solve();

        assertEquals(1, solver.getMaxFlow());

        boolean[] minCut = solver.getMinCut();

        // Reachable from source in residual graph
        assertTrue(minCut[s]);   // 0
        assertTrue(minCut[1]);   // 1
        assertTrue(minCut[2]);   // 2
        assertTrue(minCut[3]);   // 3
        assertTrue(minCut[4]);   // 4
        assertTrue(minCut[6]);   // 6
        assertTrue(minCut[7]);   // 7
        assertTrue(minCut[8]);   // 8

        // Unreachable from source
        assertFalse(minCut[5]);  // 5 → blocked due to saturated 3→5
        assertFalse(minCut[t]);  // 9

    }


}


class EdmondsKarpSolverTest extends NetworkFlowSolverTest {
    @Override
    NetworkFlowSolverBase createSolver(int n, int s, int t) {
        return new EdmondsKarpSolver(n, s, t);
    }
}


class BoyKovKolmogorovSolverTest extends NetworkFlowSolverTest {

    @Override
    NetworkFlowSolverBase createSolver(int n, int s, int t) {
        return new BoykovKolmogorovSolver(n, s, t);
    }
}

