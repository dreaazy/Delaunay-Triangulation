import java.util.*;

class DSU {
    private Map<DelaunayTriangulation.Point, DelaunayTriangulation.Point> parent = new HashMap<>();

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
}