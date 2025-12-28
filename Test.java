import java.util.*;
import java.io.*;

public class Test {

    public static void main(String[] args) {

        String filename = "testFile.txt";
        int nTest = 100000;

        //create file with nTest points and create list
        DelaunayTester.createPointsFile(nTest, filename);
        List<DelaunayTriangulation.Point> points = readPointsFromFile(filename);

        //starting triangulation on file
        System.out.println("Triangulation started");
        DelaunayTriangulation.EdgePair result = DelaunayTriangulation.computeDelaunay(points);
        System.out.println("Triangulation ended");


        //testing the correctness of triangulation
        DelaunayTester.test(points, result);

        long N = 1_000_000;
        double cpuSpeed = 1_000_000_000.0; // 1 Billion ops/sec (Standard 1GHz core)

        // Compare O(N^2) vs O(N log2 N)
        DelaunayTester.compare(N, cpuSpeed,
            (n) -> (double) n * n,                 // N^2
            (n) -> n * (Math.log(n) / Math.log(2)) // N log N
        );

        
    }
    //#region HELPERS
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
    public static void printPoints(List<DelaunayTriangulation.Point> points) {
        for (DelaunayTriangulation.Point p : points) {
            System.out.println("(" + p.x + ", " + p.y + ")");
        }
    }
    //#endregion
}
