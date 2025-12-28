import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class DelaunayTester {

    // Tester allows a larger margin of error to avoid flagging borderline cases
    private static final double TESTER_TOLERANCE = 1e-5; 

    public static void test(List<DelaunayTriangulation.Point> points, DelaunayTriangulation.EdgePair pair)
    {
        System.out.println("Starting test with: " + points.size() + " points");
        testEulerProperty(points, pair);
        testDelaunayProperty(points, pair);
    }

    public static void compare(long n, double opsPerSec, 
                               Function<Long, Double> alg1, 
                               Function<Long, Double> alg2) {
        
        double ops1 = alg1.apply(n);
        double ops2 = alg2.apply(n);

        double time1 = ops1 / opsPerSec;
        double time2 = ops2 / opsPerSec;
        double speedup = time1 / time2;

        System.out.println("\n--- Performance Comparison (N = " + n + ") ---");
        System.out.printf("Algorithm 1 Operations: %.0f%n", ops1);
        System.out.printf("Algorithm 2 Operations: %.0f%n", ops2);
        System.out.printf("Estimated Time 1      : %.6f seconds%n", time1);
        System.out.printf("Estimated Time 2      : %.6f seconds%n", time2);
        System.out.printf("Speedup Factor        : %.2fx faster%n", speedup);
    }

    //#region tests
    public static void testEulerProperty(List<DelaunayTriangulation.Point> points, DelaunayTriangulation.EdgePair pair) {
        if (pair == null || pair.ldo == null) {
            System.out.println("⚠️ Cannot check Euler property: Graph is null.");
            return;
        }


        Set<DelaunayTriangulation.QuarterEdge> visitedEdges = new HashSet<>();
        Set<DelaunayTriangulation.Point> visitedVertices = new HashSet<>();
        Queue<DelaunayTriangulation.QuarterEdge> queue = new LinkedList<>();
        queue.add(pair.ldo);
        visitedEdges.add(pair.ldo);

        while (!queue.isEmpty()) {
            DelaunayTriangulation.QuarterEdge e = queue.poll();
            
            // Count vertices
            visitedVertices.add(e.getOrig());
            visitedVertices.add(e.getDest());

            DelaunayTriangulation.QuarterEdge[] neighbors = { e.sym(), e.oNext(), e.lNext() };
            for (DelaunayTriangulation.QuarterEdge n : neighbors) {
                if (n != null && !visitedEdges.contains(n)) {
                    visitedEdges.add(n);
                    queue.add(n);
                }
            }
        }

        long V = visitedVertices.size();
        

        long E = visitedEdges.size() / 2;

        long F = 0;
        Set<DelaunayTriangulation.QuarterEdge> faceVisited = new HashSet<>();

        for (DelaunayTriangulation.QuarterEdge e : visitedEdges) {
            if (!faceVisited.contains(e)) {
                F++; // Found a new face
                
                // Walk around the face to mark all its boundary edges
                DelaunayTriangulation.QuarterEdge curr = e;
                do {
                    faceVisited.add(curr);
                    curr = curr.lNext();
                } while (curr != e && !faceVisited.contains(curr)); // Safety check for loops
            }
        }

        // 3. Verify Euler's Formula: V - E + F = 2
        long characteristic = V - E + F;

        System.out.println("\n--- 📐 Euler Characteristic Check ---");
        System.out.println("   Vertices (V): " + V);
        System.out.println("   Edges    (E): " + E);
        System.out.println("   Faces    (F): " + F + " (including infinite outer face)");
        System.out.println("   Formula     : " + V + " - " + E + " + " + F + " = " + characteristic);

        if (characteristic == 2) {
            System.out.println("✅ TOPOLOGY VALID: Euler characteristic is 2.");
        } else {
            System.out.println("❌ TOPOLOGY BROKEN: Euler characteristic is " + characteristic + " (Should be 2).");
            System.out.println("   (This usually means the graph has twisted edges or isn't planar)");
        }
    }

    public static void testDelaunayProperty(List<DelaunayTriangulation.Point> points, 
                                        DelaunayTriangulation.EdgePair pair) {
        
        // Track visited edges by object reference (very fast)
        Set<DelaunayTriangulation.QuarterEdge> visited = new HashSet<>();
        Queue<DelaunayTriangulation.QuarterEdge> queue = new LinkedList<>();

        if (pair != null && pair.ldo != null) {
            queue.add(pair.ldo);
        }

        boolean foundViolation = false;
        int edgesChecked = 0;

        while (!queue.isEmpty()) {
            DelaunayTriangulation.QuarterEdge e = queue.poll();

            if (visited.contains(e)) continue;
            visited.add(e);


            if (!visited.contains(e.sym())) queue.add(e.sym());
            if (!visited.contains(e.lNext())) queue.add(e.lNext());

            DelaunayTriangulation.Point a = e.getOrig();
            DelaunayTriangulation.Point b = e.getDest();
            DelaunayTriangulation.Point c = e.lNext().getDest();

            DelaunayTriangulation.Point d = e.sym().lNext().getDest();

            boolean isLeftTriangle = isCCW(a, b, c);
            boolean isRightTriangle = isCCW(b, a, d);

            // We only test edges that are shared by two real triangles (Internal Edges).
            // If either side is the "Outer Face", it's a boundary edge, which cannot be flipped.
            if (isLeftTriangle && isRightTriangle) {
                edgesChecked++;
                
                // Check: Is the neighbor 'd' inside the circle of 'abc'?
                // Use the permissive check (TESTER_TOLERANCE) defined previously
                if (isStrictlyInsideCircle(a, b, c, d)) {
                    System.out.println("❌ VIOLATION on Edge: " + a + " -> " + b);
                    System.out.println("   Triangle (" + a + "," + b + "," + c + ")");
                    System.out.println("   Contains neighbor point: " + d);
                    foundViolation = true;
                }
            }
        }

        if (!foundViolation) {
            System.out.println("✅ FAST TEST PASSED: Verified " + edgesChecked + " internal edges locally.");
        }
    }
    //#endregion

    //#regions slow test
    public static void slowTestDelaunayProperty(List<DelaunayTriangulation.Point> points, 
                                            DelaunayTriangulation.EdgePair pair) {
        
        Set<String> checkedTriangles = new HashSet<>();
        Set<DelaunayTriangulation.QuarterEdge> visitedEdges = new HashSet<>();
        Queue<DelaunayTriangulation.QuarterEdge> queue = new LinkedList<>();

        if (pair != null && pair.ldo != null) {
            queue.add(pair.ldo);
            visitedEdges.add(pair.ldo);
        }

        boolean foundViolation = false;

        while (!queue.isEmpty()) {
            DelaunayTriangulation.QuarterEdge e = queue.poll();

            // 1. Identify potential triangle vertices
            DelaunayTriangulation.QuarterEdge eNext = e.lNext(); // Use lNext (Left Next) to walk the face
            DelaunayTriangulation.QuarterEdge ePrev = e.lPrev();

            DelaunayTriangulation.Point a = e.getOrig();
            DelaunayTriangulation.Point b = e.getDest();
            DelaunayTriangulation.Point c = eNext.getDest();

            // 2. Queue neighbors (standard traversal)
            DelaunayTriangulation.QuarterEdge[] neighbors = { e.sym(), eNext.sym(), ePrev.sym() };
            for (DelaunayTriangulation.QuarterEdge ne : neighbors) {
                if (ne != null && !visitedEdges.contains(ne)) {
                    visitedEdges.add(ne);
                    queue.add(ne);
                }
            }

            // 3. ROBUST FILTER: Is this a real triangle?
            // If the face is the "Outer Face" (Right side of hull), the CCW check usually fails.
            // But we also check for degeneracy.
            if (a.equals(b) || a.equals(c) || b.equals(c)) continue;
            
            // Only test faces that are strictly Counter-Clockwise
            if (!isCCW(a, b, c)) continue;

            // 4. PREVENT DUPLICATES
            // Sort keys to ensure triangle (A,B,C) is same as (B,C,A)
            List<DelaunayTriangulation.Point> tri = Arrays.asList(a, b, c);
            tri.sort((p1, p2) -> {
                if (Math.abs(p1.x - p2.x) > 1e-9) return Double.compare(p1.x, p2.x);
                return Double.compare(p1.y, p2.y);
            });
            String key = tri.get(0) + "|" + tri.get(1) + "|" + tri.get(2);
            
            if (checkedTriangles.contains(key)) continue;
            checkedTriangles.add(key);

            // 5. THE TEST: Check all points against this triangle
            for (DelaunayTriangulation.Point p : points) {
                if (p.equals(a) || p.equals(b) || p.equals(c)) continue;

                // Use the Permissive Check
                if (isStrictlyInsideCircle(a, b, c, p)) {
                    System.out.println("❌ VIOLATION: Point " + p + " is inside triangle " + key);
                    System.out.println("   Distance Det: " + circleDet(a, b, c, p));
                    foundViolation = true;
                    // Stop checking this triangle to avoid spamming console
                    break; 
                }
            }
        }

        if (!foundViolation) {
            System.out.println("✅ TEST PASSED: Verified " + checkedTriangles.size() + " triangles.");
        }
    }
    //#endregion

    // --- Helper Predicates for Tester ---

    //#region helpers
    private static boolean isCCW(DelaunayTriangulation.Point a, DelaunayTriangulation.Point b, DelaunayTriangulation.Point c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x) > 1e-9;
    }
    private static double circleDet(DelaunayTriangulation.Point a, DelaunayTriangulation.Point b, DelaunayTriangulation.Point c, DelaunayTriangulation.Point d) {
        double adx = a.x - d.x; double ady = a.y - d.y;
        double bdx = b.x - d.x; double bdy = b.y - d.y;
        double cdx = c.x - d.x; double cdy = c.y - d.y;
        double abdet = adx * bdy - bdx * ady;
        double bcdet = bdx * cdy - cdx * bdy;
        double cadet = cdx * ady - adx * cdy;
        double alift = adx * adx + ady * ady;
        double blift = bdx * bdx + bdy * bdy;
        double clift = cdx * cdx + cdy * cdy;
        return alift * bcdet + blift * cadet + clift * abdet;
    }

    private static boolean isStrictlyInsideCircle(DelaunayTriangulation.Point a, DelaunayTriangulation.Point b, DelaunayTriangulation.Point c, DelaunayTriangulation.Point d) {
        return circleDet(a, b, c, d) > TESTER_TOLERANCE;
    }
    public static void createPointsFile(int size, String filename) {
        // Force .txt extension if missing
        if (filename != null && !filename.endsWith(".txt")) {
            filename += ".txt";
        }

        Random random = new Random();

        try (FileWriter writer = new FileWriter(filename)) {
            for (int i = 0; i < size; i++) {
                // Generate random integers between 0 and 1000
                int x = random.nextInt(1001);
                int y = random.nextInt(1001);
                String line = String.format("(%d,%d)%n", x, y);
                writer.write(line);
            }
            System.out.println("Successfully created '" + filename + "' with " + size + " points.");
        } catch (IOException e) {
            System.err.println("An error occurred while writing the file.");
            e.printStackTrace();
        }
    }
    //#endregion
}