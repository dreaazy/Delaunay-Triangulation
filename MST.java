import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class MST {
    

    public static class MSTResult {
        public List<DelaunayTriangulation.QuarterEdge> edges;
        public double totalWeight;
        public boolean alphaProperty = true;

        public MSTResult(List<DelaunayTriangulation.QuarterEdge> edges, double totalWeight) {
            this.edges = edges;
            this.totalWeight = totalWeight;
        }
    }

    
    public static MSTResult computeMST(DelaunayTriangulation.EdgePair hull, double alpha) {
        Set<DelaunayTriangulation.QuarterEdge> allEdges = new HashSet<>();
        collectEdges(hull.ldo, allEdges);

        // 1. Sort all unique triangulation edges by length
        List<DelaunayTriangulation.QuarterEdge> edgeList = new ArrayList<>(allEdges);
        edgeList.sort(Comparator.comparingDouble(e -> 
            distance(e.getOrig(), e.getDest())));

        List<DelaunayTriangulation.QuarterEdge> mstEdges = new ArrayList<>();
        double sumWeight = 0;
        //to prevent loops
        DSU dsu = new DSU();

        // Apply Kruskal's
        for (DelaunayTriangulation.QuarterEdge edge : edgeList) {
            if (dsu.union(edge.getOrig(), edge.getDest())) {

                //to respect the alpha property
                if(distance(edge.getOrig(), edge.getDest()) <= alpha)
                {
                    mstEdges.add(edge);
                    // Accumulate the weight w(E)
                    sumWeight += distance(edge.getOrig(), edge.getDest());
                }
                else
                {
                    //the graph do not respect the property
                    //return the result with the alphaProperty not respected,
                    //and sum of weight until now
                    MSTResult result = new MSTResult(mstEdges, sumWeight);
                    result.alphaProperty = false;

                    return result;
                }
                
            }
        }
        
        return new MSTResult(mstEdges, sumWeight);
    }
    

    // Traverses the triangulation to find all unique edges
    private static void collectEdges(DelaunayTriangulation.QuarterEdge start, Set<DelaunayTriangulation.QuarterEdge> visited) {
        Stack<DelaunayTriangulation.QuarterEdge> stack = new Stack<>();
        stack.push(start);


        //Depth first search approach
        while (!stack.isEmpty()) {
            DelaunayTriangulation.QuarterEdge e = stack.pop();

            //we have two identical edges, one from A to B and the other accessed with sym from B to A
            // to avoid adding the same physical edge twice
            DelaunayTriangulation.QuarterEdge canonical = (System.identityHashCode(e) < System.identityHashCode(e.sym())) ? e : e.sym();
            
            //Return true, pushes the canonical and it explores the neighbors
            //we are moving around a point and across faces, because quad edge data structure represents the graph and its dual
            if (visited.add(canonical)) {
                // Explore neighbors using the provided navigation methods
                stack.push(e.oNext());
                stack.push(e.lNext());
                stack.push(e.rNext());
            }
        }
    }

    private static double distance(DelaunayTriangulation.Point p1, DelaunayTriangulation.Point p2) {
        double dx = p1.x - p2.x;
        double dy = p1.y - p2.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

}
