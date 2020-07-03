package com.etheller.warsmash.viewer5.handlers.w3x.simulation.pathing;

import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import com.etheller.warsmash.viewer5.handlers.w3x.environment.PathingGrid;

public class CPathfindingProcessor {
	private final PathingGrid pathingGrid;
	private final Node[][] nodes;
	private final Node[][] cornerNodes;
	private Node goal;

	public CPathfindingProcessor(final PathingGrid pathingGrid) {
		this.pathingGrid = pathingGrid;
		this.nodes = new Node[pathingGrid.getHeight()][pathingGrid.getWidth()];
		this.cornerNodes = new Node[pathingGrid.getHeight() + 1][pathingGrid.getWidth() + 1];
		for (int i = 0; i < this.nodes.length; i++) {
			for (int j = 0; j < this.nodes[i].length; j++) {
				this.nodes[i][j] = new Node(new Point2D.Float(pathingGrid.getWorldX(j), pathingGrid.getWorldY(i)));
			}
		}
		for (int i = 0; i < this.cornerNodes.length; i++) {
			for (int j = 0; j < this.cornerNodes[i].length; j++) {
				this.cornerNodes[i][j] = new Node(
						new Point2D.Float(pathingGrid.getWorldXFromCorner(j), pathingGrid.getWorldYFromCorner(i)));
			}
		}
	}

	/**
	 * Finds the path to a point using a naive, slow, and unoptimized algorithm.
	 * Does not have optimizations yet, do this for a bunch of units and it will
	 * probably lag like a walrus. The implementation here was created by reading
	 * the wikipedia article on A* to jog my memory from data structures class back
	 * in college, and is meant only as a first draft to get things working.
	 *
	 * @param collisionSize
	 *
	 *
	 * @param start
	 * @param goal
	 * @return
	 */
	public List<Point2D.Float> findNaiveSlowPath(final float startX, final float startY, final float goalX,
			final float goalY, final PathingGrid.MovementType movementType, final float collisionSize) {
		System.out.println("beginning findNaiveSlowPath for  " + startX + "," + startY + "," + goalX + "," + goalY);
		if ((startX == goalX) && (startY == goalY)) {
			return Collections.emptyList();
		}
		Node[][] searchGraph;
		GridMapping gridMapping;
		if (isCollisionSizeBetterSuitedForCorners(collisionSize)) {
			searchGraph = this.cornerNodes;
			gridMapping = GridMapping.CORNERS;
			System.out.println("using corners");
		}
		else {
			searchGraph = this.nodes;
			gridMapping = GridMapping.CELLS;
			System.out.println("using cells");
		}
		this.goal = searchGraph[gridMapping.getY(this.pathingGrid, goalY)][gridMapping.getX(this.pathingGrid, goalX)];
		final Node start = searchGraph[gridMapping.getY(this.pathingGrid, startY)][gridMapping.getX(this.pathingGrid,
				startX)];
		for (int i = 0; i < searchGraph.length; i++) {
			for (int j = 0; j < searchGraph[i].length; j++) {
				final Node node = searchGraph[i][j];
				node.g = Float.POSITIVE_INFINITY;
				node.f = Float.POSITIVE_INFINITY;
				node.cameFrom = null;
			}
		}
		start.g = 0;
		start.f = h(start);
		final PriorityQueue<Node> openSet = new PriorityQueue<>(new Comparator<Node>() {
			@Override
			public int compare(final Node a, final Node b) {
				return Double.compare(f(a), f(b));
			}
		});
		openSet.add(start);

		while (!openSet.isEmpty()) {
			Node current = openSet.poll();
			if (current == this.goal) {
				final LinkedList<Point2D.Float> totalPath = new LinkedList<>();
				Direction lastCameFromDirection = null;
				while (current.cameFrom != null) {
					if ((lastCameFromDirection == null) || (current.cameFromDirection != lastCameFromDirection)) {
						totalPath.addFirst(current.point);
						lastCameFromDirection = current.cameFromDirection;
					}
					current = current.cameFrom;
				}
				return totalPath;
			}

			for (final Direction direction : Direction.VALUES) {
				final float x = current.point.x + (direction.xOffset * 32);
				final float y = current.point.y + (direction.yOffset * 32);
				if (this.pathingGrid.contains(x, y) && this.pathingGrid.isPathable(x, y, movementType, collisionSize)
						&& this.pathingGrid.isPathable(current.point.x, y, movementType, collisionSize)
						&& this.pathingGrid.isPathable(x, current.point.y, movementType, collisionSize)) {
					double turnCost;
					if ((current.cameFromDirection != null) && (direction != current.cameFromDirection)) {
						turnCost = 0.25;
					}
					else {
						turnCost = 0;
					}
					final double tentativeScore = current.g + ((direction.length + turnCost) * 32);
					final Node neighbor = searchGraph[gridMapping.getY(this.pathingGrid, y)][gridMapping
							.getX(this.pathingGrid, x)];
					if (tentativeScore < neighbor.g) {
						neighbor.cameFrom = current;
						neighbor.cameFromDirection = direction;
						neighbor.g = tentativeScore;
						neighbor.f = tentativeScore + h(neighbor);
						if (!openSet.contains(neighbor)) {
							openSet.add(neighbor);
						}
					}
				}
			}
		}
		return Collections.emptyList();
	}

	public static boolean isCollisionSizeBetterSuitedForCorners(final float collisionSize) {
		return (((2 * (int) collisionSize) / 32) % 2) == 1;
	}

	public double f(final Node n) {
		return n.g + h(n);
	}

	public double g(final Node n) {
		return n.g;
	}

	public float h(final Node n) {
		return (float) n.point.distance(this.goal.point);
	}

	public static final class Node {
		public Direction cameFromDirection;
		private final Point2D.Float point;
		private double f;
		private double g;
		private Node cameFrom;

		private Node(final Point2D.Float point) {
			this.point = point;
		}
	}

	private static enum Direction {
		NORTH_WEST(-1, 1),
		NORTH(0, 1),
		NORTH_EAST(1, 1),
		EAST(1, 0),
		SOUTH_EAST(1, -1),
		SOUTH(0, -1),
		SOUTH_WEST(-1, -1),
		WEST(-1, 0);

		public static final Direction[] VALUES = values();

		private final int xOffset;
		private final int yOffset;
		private final double length;

		private Direction(final int xOffset, final int yOffset) {
			this.xOffset = xOffset;
			this.yOffset = yOffset;
			final double sqrt = Math.sqrt((xOffset * xOffset) + (yOffset * yOffset));
			this.length = sqrt;
		}
	}

	public static interface GridMapping {
		int getX(PathingGrid grid, float worldX);

		int getY(PathingGrid grid, float worldY);

		public static final GridMapping CELLS = new GridMapping() {
			@Override
			public int getX(final PathingGrid grid, final float worldX) {
				return grid.getCellX(worldX);
			}

			@Override
			public int getY(final PathingGrid grid, final float worldY) {
				return grid.getCellY(worldY);
			}

		};

		public static final GridMapping CORNERS = new GridMapping() {
			@Override
			public int getX(final PathingGrid grid, final float worldX) {
				return grid.getCornerX(worldX);
			}

			@Override
			public int getY(final PathingGrid grid, final float worldY) {
				return grid.getCornerY(worldY);
			}

		};
	}
}