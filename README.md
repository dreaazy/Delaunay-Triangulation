# Delaunay triangulation for EMST

The goal of this article is to present an implementation of **Delaunay triangulation** in Java.

The implementation follows the seminal work of Leonidas Guibas and Jorge Stolfi, who introduced the **QuadEdge** data structure in their original paper. This structure provides a unified representation of both a planar graph and its dual, making it particularly well suited for computational geometry algorithms such as Delaunay triangulation.

This article first introduces the theoretical foundations of Delaunay triangulation. It then describes the core operators of the QuadEdge structure used throughout the implementation.

Finally, a working implementation for computing a **Minimum Spanning Tree (MST)** with time complexity **O(n log n)** is presented. One of the key properties of Delaunay triangulations is that every edge of the minimum spanning tree is guaranteed to belong to the triangulation itself.

As a consequence, it is unnecessary to consider all possible edges, as done in naïve applications of classical algorithms such as Prim’s or Kruskal’s. Restricting computation to the triangulation significantly improves efficiency.

- Full documentation and explanation on my website: [Link](https://example.com/docs)
- Original paper: [link](https://www.math.ucdavis.edu/~deloera/MISC/LA-BIBLIO/trunk/Guibas.pdf)
