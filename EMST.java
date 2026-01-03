/**
 * 
 * Author: Simone Piccinini
 * 
 */


//#region imports
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
//#endregion
public class EMST {

    //for disjoint set
    private Map<DelaunayTriangulation.Point, DelaunayTriangulation.Point> parent = new HashMap<>();
    private static final double EPSILON = 1e-12;

    //#region usefull classes
    public static class Point {
        public double x, y;
        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
        @Override
        public String toString() { return "(" + x + ", " + y + ")"; }
    }
    public static class EdgePair {
        public QuarterEdge ldo; // Left-most edge of the hull
        public QuarterEdge rdo; // Right-most edge of the hull

        public EdgePair(QuarterEdge ldo, QuarterEdge rdo) {
            this.ldo = ldo;
            this.rdo = rdo;
        }
    }

    public static class MSTResult {
        public List<DelaunayTriangulation.QuarterEdge> edges;
        public double totalWeight;
        public boolean alphaProperty = true;

        public MSTResult(List<DelaunayTriangulation.QuarterEdge> edges, double totalWeight) {
            this.edges = edges;
            this.totalWeight = totalWeight;
        }

        public void printEdges() {
            for (DelaunayTriangulation.QuarterEdge e : edges) {
                DelaunayTriangulation.Point p1 = e.getOrig();
                DelaunayTriangulation.Point p2 = e.getDest();
                
                // Cast to int if your Point class stores them as doubles, 
                // or just use %d if they are already int fields.
                System.out.printf("(%d, %d)(%d, %d)%n", (int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
            }
        }
    }
    //#endregion
    //#region helper

    public static List<DelaunayTriangulation.Point> readPointsFromFile(String filename) {
        List<DelaunayTriangulation.Point> points = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // Remove parentheses and split by comma
                line = line.replace("(", "").replace(")", "");
                String[] coords = line.split(",");
                if (coords.length != 2) continue;
                
                try {
                    double x = Double.parseDouble(coords[0].trim());
                    double y = Double.parseDouble(coords[1].trim());
                    points.add(new DelaunayTriangulation.Point(x, y));
                } catch (NumberFormatException ex) {
                    System.out.println("Skipping invalid line: " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return points;
    }
    //#endregion
    //#region MST

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

    //#endregion 
    //#region disjoint set
    public DelaunayTriangulation.Point find(DelaunayTriangulation.Point p) {
        if (!parent.containsKey(p)) parent.put(p, p);
        if (parent.get(p) == p) return p;
        parent.put(p, find(parent.get(p))); // Path compression
        return parent.get(p);
    }

    public boolean union(DelaunayTriangulation.Point p1, DelaunayTriangulation.Point p2) {
        DelaunayTriangulation.Point root1 = find(p1);
        DelaunayTriangulation.Point root2 = find(p2);
        if (root1 != root2) {
            parent.put(root1, root2);
            return true;
        }
        return false;
    }
    //#endregion
    public static class QuarterEdge {
        //attributes
        private QuarterEdge _next; //next quarter edge
        private QuarterEdge _rot; //rotate to the dual graph edge
        private Point _orig; //point
        private Object _data;

        // Private constructor
        private QuarterEdge(Point orig, QuarterEdge next, QuarterEdge rot) {
            _orig = orig;
            _next = next;
            _rot = rot;
        }
        
        //#region navigation helper
        //navigation helper
        public Point getOrig() { return _orig; }
        public Point getDest() { return sym().getOrig(); }
        public QuarterEdge rot() { return _rot; }
        public QuarterEdge sym() { return _rot.rot(); }
        public QuarterEdge rotInv() { return _rot.sym(); }
        
        public QuarterEdge oNext() { return _next; }
        public QuarterEdge oPrev() { return _rot.oNext().rot(); }
        public QuarterEdge lNext() { return rotInv().oNext().rot(); }
        public QuarterEdge lPrev() { return _next.sym(); }
        public QuarterEdge rNext() { return _rot.oNext().rotInv(); }
        public QuarterEdge rPrev() { return sym().oNext(); }

        public void setNext(QuarterEdge next) { _next = next; }
        public void setOrig(Point p) { _orig = p; }

        //#endregion
        // Fundamental topological operators
        public static void splice(QuarterEdge a, QuarterEdge b) {
            QuarterEdge alpha = a.oNext().rot();
            QuarterEdge beta  = b.oNext().rot();

            QuarterEdge t1 = b.oNext();
            QuarterEdge t2 = a.oNext();
            QuarterEdge t3 = beta.oNext();
            QuarterEdge t4 = alpha.oNext();

            a.setNext(t1);
            b.setNext(t2);
            alpha.setNext(t3);
            beta.setNext(t4);
        }
        public static QuarterEdge makeEdge(Point orig, Point dest) {
            QuarterEdge q0 = new QuarterEdge(orig, null, null);
            QuarterEdge e0 = new QuarterEdge(null, null, null);
            QuarterEdge q1 = new QuarterEdge(dest, null, null);
            QuarterEdge e1 = new QuarterEdge(null, null, null);

            q0._next = q0; q0._rot = e0;
            q1._next = q1; q1._rot = e1;
            e0._next = e1; e0._rot = q1;
            e1._next = e0; e1._rot = q0;

            return q0;
        }
        //derived operators from splice and makeEdge
        public static QuarterEdge connect(QuarterEdge a, QuarterEdge b) {
            QuarterEdge e = makeEdge(a.getDest(), b.getOrig());
            splice(e, a.lNext());
            splice(e.sym(), b);
            return e;
        }
        public static void delete(QuarterEdge e) {
            splice(e, e.oPrev());
            splice(e.sym(), e.sym().oPrev());
        }
    }
    //#region geometric predicates

    //check the orientation of a triangle
    private static boolean ccw(Point a, Point b, Point c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x) > EPSILON;
    }
    private static boolean rightOf(Point p, QuarterEdge e) {return ccw(p, e.getDest(), e.getOrig());}
    private static boolean leftOf(Point p, QuarterEdge e){return ccw(p, e.getOrig(), e.getDest());}

    //this compute the 4 x 4 determinant described in the paper
    private static boolean inCircle(Point a, Point b, Point c, Point d) {
        double adx = a.x - d.x;
        double ady = a.y - d.y;
        double bdx = b.x - d.x;
        double bdy = b.y - d.y;
        double cdx = c.x - d.x;
        double cdy = c.y - d.y;

        double abdet = adx * bdy - bdx * ady;
        double bcdet = bdx * cdy - cdx * bdy;
        double cadet = cdx * ady - adx * cdy;
        
        double alift = adx * adx + ady * ady;
        double blift = bdx * bdx + bdy * bdy;
        double clift = cdx * cdx + cdy * cdy;

        return (alift * bcdet + blift * cadet + clift * abdet) > EPSILON;
    }

    //#endregion
    //#region divide and conquer delaunay triangulation
    public static EdgePair computeDelaunay(List<Point> inputs) {

        //no need to triangulate
        if (inputs == null || inputs.size() < 2) return null;

        //duplicate points need to be removed and sorted first by x and then by y
        //complexity O(nlong)
        List<Point> points = new ArrayList<>(inputs);
        Collections.sort(points, (p1, p2) -> {
            if (Math.abs(p1.x - p2.x) < EPSILON) return Double.compare(p1.y, p2.y);
            return Double.compare(p1.x, p2.x);
        });

        //Delete duplicates - using set for safety and efficiency
        Set<String> seen = new HashSet<>();
        List<Point> uniquePoints = new ArrayList<>();
        for (Point p : points) {
            String key = p.x + "," + p.y;
            if (!seen.contains(key)) {
                seen.add(key);
                uniquePoints.add(p);
            }
        }

        if (uniquePoints.size() < 2) return null;
        //giving to the recursive function 0 and l -1 position of the list
        return computeRecursive(uniquePoints, 0, uniquePoints.size() - 1);
    }

    //apply the divide-and-conquer logic
    private static EdgePair computeRecursive(List<Point> S, int L, int R) {
        //base cases

        // base case (two points)
        if (R - L + 1 == 2) {
            QuarterEdge e = QuarterEdge.makeEdge(S.get(L), S.get(L + 1));
            return new EdgePair(e, e.sym());
        }
        
        // other base case (three points)
        if (R - L + 1 == 3) {
            QuarterEdge a = QuarterEdge.makeEdge(S.get(L), S.get(L + 1));
            QuarterEdge b = QuarterEdge.makeEdge(S.get(L + 1), S.get(R));
            QuarterEdge.splice(a.sym(), b);

            if (ccw(S.get(L), S.get(L + 1), S.get(R))) {
                QuarterEdge c = QuarterEdge.connect(b, a);
                return new EdgePair(a, b.sym());
            } else if (ccw(S.get(L), S.get(R), S.get(L + 1))) {
                QuarterEdge c = QuarterEdge.connect(b, a);
                return new EdgePair(c.sym(), c);
            } else { // the points are collinear
                return new EdgePair(a, b.sym());
            }
        }

        // DIVIDE
        int split = (L + R) / 2;
        EdgePair leftRes = computeRecursive(S, L, split);
        EdgePair rightRes = computeRecursive(S, split + 1, R);

        QuarterEdge ldo = leftRes.rdo;
        QuarterEdge rdi = rightRes.ldo;

        // Compute the lower common tangent
        while (true) {
            if (leftOf(rdi.getOrig(), ldo)) {
                ldo = ldo.lNext();
            } else if (rightOf(ldo.getOrig(), rdi)) {
                rdi = rdi.rPrev();
            } else {
                break;
            }
        }

        QuarterEdge basel = QuarterEdge.connect(rdi.sym(), ldo);
        
        // Adjust the hull edges
        if (ldo.getOrig().equals(leftRes.ldo.getOrig())) leftRes.ldo = basel.sym();
        if (rdi.getOrig().equals(rightRes.rdo.getOrig())) rightRes.rdo = basel;

        // Merge loop
        while (true) {
            // Locate the first L candidate to be deleted
            QuarterEdge lCand = basel.sym().oNext();
            if (rightOf(lCand.getDest(), basel)) {
                while (inCircle(basel.getDest(), basel.getOrig(), lCand.getDest(), lCand.oNext().getDest())) {
                    QuarterEdge t = lCand.oNext();
                    QuarterEdge.delete(lCand);
                    lCand = t;
                }
            }

            // Locate the first R candidate to be deleted
            QuarterEdge rCand = basel.oPrev();
            if (rightOf(rCand.getDest(), basel)) {
                while (inCircle(basel.getDest(), basel.getOrig(), rCand.getDest(), rCand.oPrev().getDest())) {
                    QuarterEdge t = rCand.oPrev();
                    QuarterEdge.delete(rCand);
                    rCand = t;
                }
            }

            // Terminate if no valid candidates
            boolean lValid = rightOf(lCand.getDest(), basel);
            boolean rValid = rightOf(rCand.getDest(), basel);

            if (!lValid && !rValid) break;

            // Select the next edge to connect to
            if (!lValid || (rValid && inCircle(lCand.getDest(), lCand.getOrig(), rCand.getOrig(), rCand.getDest()))) {
                basel = QuarterEdge.connect(rCand, basel.sym());
            } else {
                basel = QuarterEdge.connect(basel.sym(), lCand.sym());
            }
        }

        return new EdgePair(leftRes.ldo, rightRes.rdo);
    }
    //#endregion

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter the points file path: ");
        String filename = scanner.nextLine();

        System.out.print("Enter the alpha value: ");
        double alpha = scanner.nextDouble();

        try {
            List<DelaunayTriangulation.Point> points = readPointsFromFile(filename);
            DelaunayTriangulation.EdgePair result = DelaunayTriangulation.computeDelaunay(points);

            MSTResult resultMst = computeMST(result, alpha);
            
            if(resultMst.alphaProperty)
            {
                System.out.println(resultMst.totalWeight);
                if (points.size() <= 10) {resultMst.printEdges();}
            }
            else
            {
                System.out.println("FAIL" );
            }
            
        } catch (Exception e) {
            System.err.println("Error processing EMST: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}