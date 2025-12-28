import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DelaunayTriangulation {

    private static final double EPSILON = 1e-12;
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
        List<DelaunayTriangulation.Point> uniquePoints = new ArrayList<>();
        for (DelaunayTriangulation.Point p : points) {
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

}