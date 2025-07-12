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

