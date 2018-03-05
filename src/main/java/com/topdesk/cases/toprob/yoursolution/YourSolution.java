package com.topdesk.cases.toprob.yoursolution;

import static com.topdesk.cases.toprob.Instruction.EAST;
import static com.topdesk.cases.toprob.Instruction.NORTH;
import static com.topdesk.cases.toprob.Instruction.PAUSE;
import static com.topdesk.cases.toprob.Instruction.SOUTH;
import static com.topdesk.cases.toprob.Instruction.WEST;
import static java.lang.Integer.valueOf;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.topdesk.cases.toprob.Coordinate;
import com.topdesk.cases.toprob.Grid;
import com.topdesk.cases.toprob.Instruction;
import com.topdesk.cases.toprob.Solution;

public class YourSolution implements Solution {

	private final String clearStep = ".";
	private final String hole = "o";
	private final String kitchen = "k";
	private final String room = "r";
	private int[] start1 = new int[3];
	private int[] start2 = new int[3];
	private int[] end1 = new int[3];
	private int[] end2 = new int[3];
	private int xSize;
	private int ySize;

	/**
	 * Doing pathfinding in both directions, if bug and rob steps on the same node
	 * at next step, rob waits, then check again
	 * 
	 */
	@Override
	public List<Instruction> solve(Grid grid, int time) {
		if (time < 0) {
			throw new IllegalArgumentException("Invalid input!");
		}

		// pathfinding is working from end to start
		start1[0] = grid.getKitchen().getY();
		start1[1] = grid.getKitchen().getX();
		end1[0] = grid.getRoom().getY();
		end1[1] = grid.getRoom().getX();

		start2[0] = grid.getRoom().getY();
		start2[1] = grid.getRoom().getX();
		end2[0] = grid.getKitchen().getY();
		end2[1] = grid.getKitchen().getX();

		xSize = grid.getHeight();
		ySize = grid.getWidth();

		// create 3 matrix, the second and third are for mapping the path steps in a
		// clear matrix
		List<String[][]> matrixes = new ArrayList<>();
		IntStream.range(0, 3).forEach(i -> matrixes.add(createGridMatrix(grid)));

		System.out.println("### From room to kitchen start ###");
		int[][] coordinatesToKitchen = doBfs(matrixes.get(0), start1, end1);
		System.out.println("### From room to kitchen end ###");
		mapCoordinatesToGrid(matrixes.get(1), coordinatesToKitchen);

		List<Instruction> instructionsToKitchen = transformMappedGridToInstructions(matrixes.get(1), end1);
		List<Instruction> instructions = new ArrayList<>();
		instructions.addAll(instructionsToKitchen);

		// add five necessary sandwitch creation
		instructions.addAll(asList(PAUSE, PAUSE, PAUSE, PAUSE, PAUSE));

		System.out.println("### From kitchen to room start ###");
		int[][] coordinatesToRoom = doBfs(matrixes.get(0), start2, end2);
		System.out.println("### From kitchen to room end ###");
		mapCoordinatesToGrid(matrixes.get(2), coordinatesToRoom);
		List<Instruction> instructionsToRoom = transformMappedGridToInstructions(matrixes.get(2), end2);
		instructions.addAll(instructionsToRoom);

		// check the next step and wait if bug and rob collapse
		List<Instruction> finalInstructions = new ArrayList<>();
		int[] currentCoordinate = new int[] { grid.getRoom().getY(), grid.getRoom().getX() };
		for (int i = 0; i < instructions.size(); i++) {
			Coordinate bugCoordinateAtNextStep = grid.getBug(time + 1);
			int[] robCoordinateAtNextStep = getNextCoordinateByInstruction(currentCoordinate, instructions.get(i));
			if (bugCoordinateAtNextStep.getY() == robCoordinateAtNextStep[0]
					&& bugCoordinateAtNextStep.getX() == robCoordinateAtNextStep[1]) {
				finalInstructions.add(PAUSE);
				i--;
				time++;
			} else {
				finalInstructions.add(instructions.get(i));
				currentCoordinate = robCoordinateAtNextStep;
				time++;
			}
		}

		StringBuilder b = new StringBuilder();
		for (Instruction i : finalInstructions) {
			b.append(i).append(" ");
		}
		System.out.println("Final solution: " + b.toString());

		return finalInstructions;
	}

	private int[] getNextCoordinateByInstruction(int[] currentCoordinate, Instruction instruction) {
		int[] nextCoord = new int[2];
		if (instruction.equals(WEST)) {
			nextCoord[0] = currentCoordinate[0];
			nextCoord[1] = currentCoordinate[1] - 1;
		} else if (instruction.equals(NORTH)) {
			nextCoord[0] = currentCoordinate[0] - 1;
			nextCoord[1] = currentCoordinate[1];
		} else if (instruction.equals(EAST)) {
			nextCoord[0] = currentCoordinate[0];
			nextCoord[1] = currentCoordinate[1] + 1;
		} else if (instruction.equals(SOUTH)) {
			nextCoord[0] = currentCoordinate[0] + 1;
			nextCoord[1] = currentCoordinate[1];
		} else if (instruction.equals(PAUSE)) {
			nextCoord[0] = currentCoordinate[0];
			nextCoord[1] = currentCoordinate[1];
		}
		return nextCoord;
	}

	private List<Instruction> transformMappedGridToInstructions(String[][] mappedGrid, int[] start) {
		List<Instruction> instructions = new ArrayList<>();
		boolean done = false;
		int[] position = start;
		while (!done) {
			int[] nextMove = getNextMove(mappedGrid, position);
			if (nextMove[0] > position[0]) {
				instructions.add(Instruction.SOUTH);
			} else if (nextMove[0] < position[0]) {
				instructions.add(Instruction.NORTH);
			} else if (nextMove[1] > position[1]) {
				instructions.add(Instruction.EAST);
			} else if (nextMove[1] < position[1]) {
				instructions.add(Instruction.WEST);
			}
			position = nextMove;
			if (position[2] == 0) {
				done = true;
			}
		}
		return instructions;
	}

	private void mapCoordinatesToGrid(String[][] grid, int[][] coordinates) {
		for (int[] coordinate : coordinates) {
			grid[coordinate[0]][coordinate[1]] = String.valueOf(coordinate[2]);
		}
	}

	private int[][] doBfs(String[][] gridMatrix, int[] start, int[] end) {
		List<List<Integer>> mainList = new ArrayList<>();
		mainList.add(asList(start[0], start[1], 0));
		boolean done = false;
		int possibleDirectionFound = 1;
		for (int i = 0; done == false; i++) {
			List<Integer> actualPoint = mainList.get(i);
			List<List<Integer>> adjecants = getAdjecantsWithVisitCount(gridMatrix, actualPoint, possibleDirectionFound);
			for (int j = 0; j < mainList.size(); j++) {
				for (int k = 0; k < adjecants.size(); k++) {
					List<Integer> adjecantToSortOut = adjecants.get(k);
					if (mainList.get(j).get(0) == adjecantToSortOut.get(0)
							&& mainList.get(j).get(1) == adjecantToSortOut.get(1)
							&& mainList.get(j).get(2) <= adjecantToSortOut.get(2)) {
						adjecants.remove(adjecantToSortOut);
						break;
					} else if (gridMatrix[adjecantToSortOut.get(0)][adjecantToSortOut.get(1)]
							.equals(gridMatrix[end[0]][end[1]])) {
						end[2] = adjecantToSortOut.get(2);
						done = true;
					}
				}
			}
			if (!adjecants.isEmpty()) {
				possibleDirectionFound++;
			}
			for (List<Integer> adjecant : adjecants) {
				System.out.println(
						"Adding adjecant " + adjecant.get(0) + "," + adjecant.get(1) + " in step: " + adjecant.get(2));
				mainList.add(asList(adjecant.get(0), adjecant.get(1), adjecant.get(2)));
			}
		}
		
		int[][] bfsCoordinates = new int[mainList.size()][3];
		for (int i = 0; i < mainList.size(); i++) {
			bfsCoordinates[i][0] = mainList.get(i).get(0);
			bfsCoordinates[i][1] = mainList.get(i).get(1);
			bfsCoordinates[i][2] = mainList.get(i).get(2);
		}
		return bfsCoordinates;
	}

	private List<List<Integer>> getAdjecantsWithVisitCount(String[][] gridMatrix, List<Integer> actualPoint,
			int counter) {
		List<List<Integer>> adjecants = new ArrayList<>();
		int x = actualPoint.get(0);
		int y = actualPoint.get(1);

		// node is on at least 2. column, add left adjecant
		if (y >= 1) {
			if (gridMatrix[x][y - 1] != hole) {
				adjecants.add(asList(x, y - 1, counter));
			}
		}

		// node is on at least 2. row, add upper adjecant
		if (x >= 1) {
			if (gridMatrix[x - 1][y] != hole) {
				adjecants.add(asList(x - 1, y, counter));
			}
		}

		// node is on at max-1 column, add right adjecant
		if (y <= ySize - 2) {
			if (gridMatrix[x][y + 1] != hole) {
				adjecants.add(asList(x, y + 1, counter));
			}
		}

		// node is on at max-1 row, add lower adjecant
		if (x <= xSize - 2) {
			if (gridMatrix[x + 1][y] != hole) {
				adjecants.add(asList(x + 1, y, counter));
			}
		}

		return adjecants;
	}

	private int[] getNextMove(String[][] mappedGrid, int[] from) {
		return getAdjecants(mappedGrid, asList(from[0], from[1], from[2])).stream()
				.filter(node -> node.get(2) <= from[2]).findFirst()
				.map(nextMove -> new int[] { nextMove.get(0), nextMove.get(1), nextMove.get(2) }).get();
	}

	private List<List<Integer>> getAdjecants(String[][] gridMatrix, List<Integer> actualPoint) {
		List<List<Integer>> adjecants = new ArrayList<>();
		int x = actualPoint.get(0);
		int y = actualPoint.get(1);

		// node is on at least 2. column, add left adjecant
		if (y >= 1) {
			if (gridMatrix[x][y - 1] != hole) {
				try {
					adjecants.add(asList(x, y - 1, valueOf(gridMatrix[x][y - 1])));
				} catch (Exception e) {
				}
			}
		}
		// node is on at least 2. row, add upper adjecant
		if (x >= 1) {
			if (gridMatrix[x - 1][y] != hole) {
				try {
					adjecants.add(asList(x - 1, y, valueOf(gridMatrix[x - 1][y])));
				} catch (Exception e) {
				}
			}
		}
		// node is on at max-1 column, add right adjecant
		if (y <= ySize - 2) {
			if (gridMatrix[x][y + 1] != hole) {
				try {
					adjecants.add(asList(x, y + 1, valueOf(gridMatrix[x][y + 1])));
				} catch (Exception e) {
				}
			}
		}
		// node is on at max-1 row, add lower adjecant
		if (x <= xSize - 2) {
			if (gridMatrix[x + 1][y] != hole) {
				try {
					adjecants.add(asList(x + 1, y, valueOf(gridMatrix[x + 1][y])));
				} catch (Exception e) {
				}
			}
		}
		return adjecants;
	}

	private String[][] createGridMatrix(Grid grid) {
		System.out.println("Creating grid matrix...");
		String[][] gridMatrix = new String[grid.getHeight()][grid.getWidth()];
		gridMatrix[grid.getRoom().getY()][grid.getRoom().getX()] = room;
		gridMatrix[grid.getKitchen().getY()][grid.getKitchen().getX()] = kitchen;
		for (Coordinate holeC : grid.getHoles()) {
			gridMatrix[holeC.getY()][holeC.getX()] = hole;
		}

		for (int i = 0; i < grid.getHeight(); i++) {
			for (int j = 0; j < grid.getWidth(); j++) {
				if (gridMatrix[i][j] == null) {
					gridMatrix[i][j] = clearStep;
				}
			}
		}

		return gridMatrix;
	}
}