package com.reading.trackit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ConvexHull {

	private ArrayList<ConvexHullPoint> Llower = new ArrayList<ConvexHullPoint>();
	private ArrayList<ConvexHullPoint> Lupper = new ArrayList<ConvexHullPoint>();
	private ArrayList<ConvexHullPoint> convexHull = new ArrayList<ConvexHullPoint>();
	private ArrayList<ArrayList<ConvexHullPoint>> listOfLists = new ArrayList<ArrayList<ConvexHullPoint>>();

	public ArrayList<ConvexHullPoint> computeDCHull(
			ArrayList<ConvexHullPoint> startingList) {
		int i = 0;
		int k = 0;
		ArrayList<ConvexHullPoint> tempList = new ArrayList<ConvexHullPoint>();
		while (startingList.size() > 3) {
			ArrayList<ConvexHullPoint> dcList = new ArrayList<ConvexHullPoint>();
			dcList.add(startingList.get(0));
			dcList.add(startingList.get(1));
			dcList.add(startingList.get(2));
			dcList.add(startingList.get(3));
			startingList.remove(3);
			startingList.remove(2);
			startingList.remove(1);
			startingList.remove(0);

			listOfLists.add(dcList);
			i++;
		}

		while (startingList.size() > 0) {
			ArrayList<ConvexHullPoint> dcList = new ArrayList<ConvexHullPoint>();
			// dcList.add(startingList.get(j));
			dcList.add(startingList.get(0));
			startingList.remove(0);

			dcList.addAll(listOfLists.get(i - 1));

			listOfLists.set((i - 1), dcList);
		}

		// startingList.clear();
		convexHull.clear();
		while (k < listOfLists.size()) {

			tempList.clear();
			Lupper.clear();
			Llower.clear();

			ArrayList<ConvexHullPoint> backup3 = new ArrayList<ConvexHullPoint>();
			for (ConvexHullPoint obj : convexHull)
				backup3.add(obj.clone());
			convexHull.clear();
			// Add the all of the elements in the next sub list

			tempList.addAll(listOfLists.get(k));
			computeLowerHull(tempList);
			computeUpperHull(tempList);

			combineHulls();

			sortHull();
			tempList.addAll(backup3);

			ArrayList<ConvexHullPoint> backup2 = new ArrayList<ConvexHullPoint>();
			for (ConvexHullPoint obj : convexHull)
				backup2.add(obj.clone());

			tempList.addAll(backup2);
			convexHull.clear();

			ArrayList<ConvexHullPoint> tempList2 = unique(tempList);

			computeLowerHull(tempList2);
			computeUpperHull(tempList2);

			combineHulls();

			tempList.clear();
			k++;
		}

		return convexHull;
	}

	private void sortHull() {

		Set<ConvexHullPoint> set = new HashSet<ConvexHullPoint>();
		set.addAll(convexHull);
		convexHull.clear();
		convexHull.addAll(set);

		Collections.sort(this.convexHull);
	}

	private ArrayList<ConvexHullPoint> unique(ArrayList<ConvexHullPoint> input) {
		Set<ConvexHullPoint> set = new HashSet<ConvexHullPoint>();
		set.addAll(input);
		ArrayList<ConvexHullPoint> ret = new ArrayList<ConvexHullPoint>();
		ret.addAll(set);
		Collections.sort(ret);
		return ret;
	}

	private void computeUpperHull(ArrayList<ConvexHullPoint> inputList)

	{
		Lupper.clear();
		inputList = unique(inputList);
		int turn;
		Lupper.add(inputList.get(0));
		Lupper.add(inputList.get(1));

		for (int i = 2; i < inputList.size(); i++) {
			Lupper.add(inputList.get(i));

			turn = findTurn(Lupper.get((Lupper.size() - 3)),
					Lupper.get((Lupper.size() - 2)),
					Lupper.get((Lupper.size() - 1)));

			while ((Lupper.size() > 2) && (turn != -1)) {

				Lupper.remove((Lupper.size() - 2));

				if (Lupper.size() > 2) {
					// Check again to see if its a right turn
					turn = findTurn(Lupper.get((Lupper.size() - 3)),
							Lupper.get((Lupper.size() - 2)),
							Lupper.get((Lupper.size() - 1)));
				} else {

					break;
				}
			}
		}
	}

	private void computeLowerHull(ArrayList<ConvexHullPoint> inputList) {
		Llower.clear();
		inputList = unique(inputList);
		int size = inputList.size();
		int turn;

		Llower.add(inputList.get((size - 1)));
		Llower.add(inputList.get((size - 2)));

		for (int i = (size - 3); i >= 0; i--) {
			Llower.add(inputList.get(i));

			turn = findTurn(Llower.get((Llower.size() - 3)),
					Llower.get((Llower.size() - 2)),
					Llower.get((Llower.size() - 1)));

			while ((Llower.size() > 2) && (turn != -1)) {

				Llower.remove((Llower.size() - 2));

				if (Llower.size() > 2) {
					// Check again to see if its a right turn
					turn = findTurn(Llower.get((Llower.size() - 3)),
							Llower.get((Llower.size() - 2)),
							Llower.get((Llower.size() - 1)));
				}

				else {
					break;
				}
			}

		}
		Llower.remove((Llower.size() - 1));
		Llower.remove(0);
	}

	private void combineHulls() {
		convexHull.clear();
		convexHull.addAll(Lupper);
		convexHull.addAll(Llower);
		Llower.clear();
		Lupper.clear();
	}

	private int findTurn(ConvexHullPoint A, ConvexHullPoint B, ConvexHullPoint C) {
		// Computes the cross product
		double xProduct = (((B.getCartesianX() - A.getCartesianX()) * (C
				.getCartesianY() - A.getCartesianY())) - ((B.getCartesianY() - A
				.getCartesianY()) * (C.getCartesianX() - A.getCartesianX())));

		if (xProduct > 0) {
			return 1; // Left Turn
		}
		if (xProduct < 0) {
			return -1; // Right Turn
		}

		return 0;
	}
}
