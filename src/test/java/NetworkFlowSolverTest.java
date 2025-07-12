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

