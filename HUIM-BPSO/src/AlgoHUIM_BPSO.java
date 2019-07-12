package ca.pfv.spmf.algorithms.frequentpatterns.HUIM_BPSO;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.pfv.spmf.algorithms.frequentpatterns.HUIM_BPSO.Element;
//import ca.pfv.spmf.algorithms.frequentpatterns.HUIM_BPSO.PairItemUtility;
import ca.pfv.spmf.algorithms.frequentpatterns.HUIM_BPSO.UtilityList;

/**
 * * * * This is an implementation of the high utility itemset mining algorithm
 * based on Binary Particle Swarm Optimization Algorithm.
 * 
 * Copyright (c) 2016 Jerry Chun-Wei Lin, Lu Yang, Philippe Fournier-Viger
 * 
 * This file is part of the SPMF DATA MINING SOFTWARE *
 * (http://www.philippe-fournier-viger.com/spmf).
 * 
 * 
 * SPMF is free software: you can redistribute it and/or modify it under the *
 * terms of the GNU General Public License as published by the Free Software *
 * Foundation, either version 3 of the License, or (at your option) any later *
 * version. *
 * 
 * SPMF is distributed in the hope that it will be useful, but WITHOUT ANY *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * *
 * 
 * You should have received a copy of the GNU General Public License along with
 * * SPMF. If not, see .
 * 
 * @author Jerry Chun-Wei Lin, Lu Yang, Philippe Fournier-Viger
 */

public class AlgoHUIM_BPSO {
	// variable for statistics
	double maxMemory = 0; // the maximum memory usage
	long startTimestamp = 0; // the time the algorithm started
	long endTimestamp = 0; // the time the algorithm terminated
	final int pop_size = 5;// the size of populations
	final int iterations = 10;// the iterations of algorithms
	final int c1 = 2, c2 = 2;// the parameter used in BPSO algorithm
	final double w = 0.9;// the parameter used in BPSO algorithm
	
	int minUtility;
	boolean useEUCPstrategy = false;
	Map<Integer, Map<Integer, Integer>> mapFMAP;

	Map<Integer, Integer> mapItemToTWU;
	List<Integer> twuPattern;// the items which has twu value more than minUtil

	BufferedWriter writer = null; // writer to write the output file

	// this class represent an item and its utility in a transaction
	class Pair{
		int item = 0;
		int utility = 0;
		
		public String toString() {
			return "[" + item + "," + utility + "]";
		}
	}

	// this class represent the particles
	class Particle {
		List<Integer> X;// the particle
		int fitness;// fitness value of particle

		public Particle() {
			X = new ArrayList<Integer>();
		}

		public Particle(int length) {
			X = new ArrayList<Integer>();
			for (int i = 0; i < length; i++) {
				X.add(i, 0);
			}
		}
	}

	class HUI {
		String itemset;
		int fitness;
		int support = 0;

		public HUI(String itemset, int fitness,int support) {
			super();
			this.itemset = itemset;
			this.fitness = fitness;
			this.support = support;
		}

	}

	Particle gBest = new Particle();// the gBest particle in populations
	List<Particle> pBest = new ArrayList<Particle>();// each pBest particle in
														// populations,
	List<Particle> population = new ArrayList<Particle>();// populations
	List<HUI> huiSets = new ArrayList<HUI>();// the set of HUIs
	List<List<Double>> V = new ArrayList<List<Double>>();// the velocity of each
															// particle
	List<Double> percentage = new ArrayList<Double>();// the portation of twu
														// value of each
														// 1-HTWUIs in sum of
														// twu value

	// Create a list to store database
	List<List<Pair>> database = new ArrayList<List<Pair>>();
	List<UtilityList> listOfUtilityLists = new ArrayList<UtilityList>();
	Map<Integer, UtilityList> mapItemToUtilityList = new HashMap<Integer, UtilityList>();
	
	

	/**
	 * Default constructor
	 */
	public AlgoHUIM_BPSO() {
	}

	/**
	 * Run the algorithm
	 * 
	 * @param input
	 *            the input file path
	 * @param output
	 *            the output file path
	 * @param minUtility
	 *            the minimum utility threshold
	 * @throws IOException
	 *             exception if error while writing the file
	 */
	public void runAlgorithm(String input, String output, int minUtility)
			throws IOException {
		// reset maximum
		maxMemory = 0;
		this.minUtility = minUtility;

		startTimestamp = System.currentTimeMillis();

		if(useEUCPstrategy) {
			mapFMAP =  new HashMap<Integer, Map<Integer, Integer>>();
		}
		writer = new BufferedWriter(new FileWriter(output));

		// We create a map to store the TWU of each item
		mapItemToTWU = new HashMap<Integer, Integer>();

		// We scan the database a first time to calculate the TWU of each item.
		BufferedReader myInput = null;
		String thisLine;
		try {
			// prepare the object for reading the file
			myInput = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File(input))));
			// for each line (transaction) until the end of file
			while ((thisLine = myInput.readLine()) != null) {
				// if the line is a comment, is empty or is a
				// kind of metadata
				if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#'
						|| thisLine.charAt(0) == '%'
						|| thisLine.charAt(0) == '@') {
					continue;
				}

				// split the transaction according to the : separator
				String split[] = thisLine.split(":");
				// the first part is the list of items
				String items[] = split[0].split(" ");
				// the second part is the transaction utility
				int transactionUtility = Integer.parseInt(split[1]);
				// for each item, we add the transaction utility to its TWU
				for (int i = 0; i < items.length; i++) {
					// convert item to integer
					Integer item = Integer.parseInt(items[i]);
					// get the current TWU of that item
					Integer twu = mapItemToTWU.get(item);
					// add the utility of the item in the current transaction to
					// its twu
					twu = (twu == null) ? transactionUtility : twu
							+ transactionUtility;
					mapItemToTWU.put(item, twu);
				}
			}
		} catch (Exception e) {
			// catches exception if error while reading the input file
			e.printStackTrace();
		} finally {
			if (myInput != null) {
				myInput.close();
			}
		}
		
		// CREATE A LIST TO STORE THE UTILITY LIST OF ITEMS WITH TWU  >= MIN_UTILITY.
		// CREATE A MAP TO STORE THE UTILITY LIST FOR EACH ITEM.
		// Key : item    Value :  utility list associated to that item
		
		// For each item
		for(Integer item: mapItemToTWU.keySet()){
			// if the item is promising  (TWU >= minutility)
			if(mapItemToTWU.get(item) >= minUtility){
				// create an empty Utility List that we will fill later.
				UtilityList uList = new UtilityList(item);
				this.mapItemToUtilityList.put(item, uList);
				// add the item to the list of high TWU items
				this.listOfUtilityLists.add(uList); 
			}
		}
		// SORT THE LIST OF HIGH TWU ITEMS IN ASCENDING ORDER
		Collections.sort(listOfUtilityLists, new Comparator<UtilityList>(){
			public int compare(UtilityList o1, UtilityList o2) {
				// compare the TWU of the items
				return compareItems(o1.item, o2.item);
			}
		} );

		twuPattern = new ArrayList<Integer>(mapItemToTWU.keySet());
		Collections.sort(twuPattern);

		// SECOND DATABASE PASS TO CONSTRUCT THE DATABASE
		// OF 1-ITEMSETS HAVING TWU >= minutil (promising items)
		try {
			// prepare object for reading the file
			myInput = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File(input))));
			
			int tid =0;
			// variable to count the number of transaction
			// for each line (transaction) until the end of file
			while ((thisLine = myInput.readLine()) != null) {
				// if the line is a comment, is empty or is a
				// kind of metadata
				if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#'
						|| thisLine.charAt(0) == '%'
						|| thisLine.charAt(0) == '@') {
					continue;
				}

				// split the line according to the separator
				String split[] = thisLine.split(":");
				// get the list of items
				String items[] = split[0].split(" ");
				// get the list of utility values corresponding to each item
				// for that transaction
				String utilityValues[] = split[2].split(" ");
				
				int newTU =0;

				// Create a list to store items and its utility
				List<Pair> revisedTransaction = new ArrayList<Pair>();
				// Create a list to store items
				List<Integer> pattern = new ArrayList<Integer>();
				// for each item
				for (int i = 0; i < items.length; i++) {
					// / convert values to integers
					Pair pair = new Pair();
					pair.item = Integer.parseInt(items[i]);
					pair.utility = Integer.parseInt(utilityValues[i]);
					// if the item has enough utility
					if (mapItemToTWU.get(pair.item) >= minUtility) {
						// add it
						revisedTransaction.add(pair);
						newTU += pair.utility; // NEW OPTIMIZATION
						pattern.add(pair.item);
					}
				}
				
				
				
				
				//********************** START OF BUG FIX *****************************/
				// PHILIPPE 2017-10 : The algorithm assumes that transaction are lexicographically
				// sorted. If not, the result is incorrect. To fix the bug, I now sort the transaction:
				Collections.sort(revisedTransaction, new Comparator<Pair>(){

					@Override
					public int compare(Pair o1, Pair o2) {
						return o1.item - o2.item;
					}});
				
				//********************** END OF BUG FIX *****************************/
				
				// Copy the transaction into database but
				// without items with TWU < minutility
				database.add(revisedTransaction);
				int remainingUtility = newTU;
				
				// for each item left in the transaction
				for(int i = 0; i< revisedTransaction.size(); i++){
					Pair pair1 =  revisedTransaction.get(i);
	
					// subtract the utility of this item from the remaining utility
					remainingUtility = remainingUtility - pair1.utility;
					
					// get the utility list of this item
					UtilityList utilityListOfItem = mapItemToUtilityList.get(pair1.item);
					
					// Add a new Element to the utility list of this item corresponding to this transaction
					Element element = new Element(tid, pair1.utility, remainingUtility);
					
					utilityListOfItem.addElement(element);
										
					// BEGIN CODE for updating the structure used
					// BY THE EUCP STRATEGY INTRODUCED IN CHUIMiner
					if(useEUCPstrategy) {
						Map<Integer, Integer> mapFMAPItem = mapFMAP.get(pair1.item);
						if(mapFMAPItem == null) {
							mapFMAPItem = new HashMap<Integer, Integer>();
							mapFMAP.put(pair1.item, mapFMAPItem);
						}
		
						for(int j = i+1; j< revisedTransaction.size(); j++){
							Pair pairAfter = revisedTransaction.get(j);
							Integer twuSum = mapFMAPItem.get(pairAfter.item);
							if(twuSum == null) {
								mapFMAPItem.put(pairAfter.item, newTU);
							}else {
								mapFMAPItem.put(pairAfter.item, twuSum + newTU);
							}
						}
					}
					// END OF CODE FOR EUCP STRATEGY

				}
				tid++; // increase tid number for next transaction
			}
		} catch (Exception e) {
			// to catch error while reading the input file
			e.printStackTrace();
		} finally {
			if (myInput != null) {
				myInput.close();
			}
		}
		// check the memory usage
		checkMemory();
		// Mine the database recursively
		if (twuPattern.size() > 0) {
			// initial population
			generatePop(minUtility);

			for (int i = 0; i < iterations; i++) {
				// update population and HUIset
				update(minUtility);
//				System.out.println(i + "-update end. HUIs No. is "
//						+ huiSets.size());
			}
		}

		writeOut();
		// check the memory usage again and close the file.
		checkMemory();
		// close output file
		writer.close();
		// record end time
		endTimestamp = System.currentTimeMillis();
	}

	/**
	 * This is the method to initial population
	 * 
	 * @param minUtility
	 *            minimum utility threshold
	 */
	private void generatePop(int minUtility)//
	{
		int i, j, k, temp;
		// initial percentage according to the twu value of 1-HTWUIs
		percentage = roulettePercent();

		for (i = 0; i < pop_size; i++) {
			// initial particles
			Particle tempParticle = new Particle(twuPattern.size());
			j = 0;
			// k is the count of 1 in particle
			k = (int) (Math.random() * twuPattern.size());

			while (j < k) {
				// roulette select the position of 1 in population
				temp = rouletteSelect(percentage);
				if (tempParticle.X.get(temp) == 0) {
					j++;
					tempParticle.X.set(temp, 1);
				}

			}
			// calculate the fitness of each particle
			tempParticle.fitness = fitCalculate(tempParticle.X, k);
			// insert particle into population
			population.add(i, tempParticle);
			// initial pBest
			pBest.add(i, population.get(i));
			// update huiSets
			if (population.get(i).fitness >= minUtility) {
				insert(population.get(i));
			}
			// update gBest
			if (i == 0) {
				gBest = pBest.get(i);
			} else {
				if (pBest.get(i).fitness > gBest.fitness) {
					gBest = pBest.get(i);
				}
			}
			// update velocity
			List<Double> tempV = new ArrayList<Double>();
			for (j = 0; j < twuPattern.size(); j++) {
				tempV.add(j, Math.random());
			}
			V.add(i, tempV);
		}
	}

	/**
	 * Methos to update particle, velocity, pBest and gBest
	 * 
	 * @param minUtility
	 */
	private void update(int minUtility) {
		int i, j, k;
		double r1, r2, temp1, temp2;

		for (i = 0; i < pop_size; i++) {
			k = 0;// record the count of 1 in particle
			r1 = Math.random();
			r2 = Math.random();
			// update velocity
			for (j = 0; j < twuPattern.size(); j++) {
				double temp = V.get(i).get(j) + r1
						* (pBest.get(i).X.get(j) - population.get(i).X.get(j))
						+ r2 * (gBest.X.get(j) - population.get(i).X.get(j));
				V.get(i).set(j, temp);
				if (V.get(i).get(j) < -2.0)
					V.get(i).set(j, -2.0);
				else if (V.get(i).get(j) > 2.0)
					V.get(i).set(j, 2.0);
			}
			// update particle
			for (j = 0; j < twuPattern.size(); j++) {
				temp1 = Math.random();
				temp2 = 1 / (1.0 + Math.exp(-V.get(i).get(j)));
				if (temp1 < temp2) {
					population.get(i).X.set(j, 1);
					k++;
				} else {
					population.get(i).X.set(j, 0);
				}
			}
			// calculate fitness
			population.get(i).fitness = fitCalculate(population.get(i).X, k);
			// update pBest & gBest
			if (population.get(i).fitness > pBest.get(i).fitness) {
				pBest.set(i, population.get(i));
				if (pBest.get(i).fitness > gBest.fitness) {
					gBest = pBest.get(i);
				}
			}
			// update huiSets
			if (population.get(i).fitness >= minUtility) {
				insert(population.get(i));
			}
		}
	}

	/**
	 * Method to inseret tempParticle to huiSets
	 * 
	 * @param tempParticle
	 *            the particle to be inserted
	 */
	private void insert(Particle tempParticle) {
		int i;
		StringBuilder temp = new StringBuilder();
		for (i = 0; i < twuPattern.size(); i++) {
			if (tempParticle.X.get(i) == 1) {
				temp.append(twuPattern.get(i));
				temp.append(' ');
			}
		}
		// huiSets is null
		String itemset;
		String new_itemset = temp.toString();
		int support = support_cal(new_itemset);
//				
		new_itemset = new_itemset.replace("",".*");
		if (huiSets.size() == 0) {
			huiSets.add(new HUI(temp.toString(), tempParticle.fitness,support));
		} else {
			// huiSets is not null, judge whether exist an itemset in huiSets
			// same with tempParticle
			for (i = 0; i < huiSets.size(); i++) {
				if (temp.toString().equals(huiSets.get(i).itemset)) {
					break;
				}
				/*else if(huiSets.get(i).itemset.matches(new_itemset) && support == huiSets.get(i).support ){
					break;
				}
				else {
					itemset = huiSets.get(i).itemset;
					itemset = itemset.replace("",".*");
					if(temp.toString().matches(itemset)){
						if(support == huiSets.get(i).support){
							huiSets.remove(i);
							i--;
						}
					}
				}*/
			}
			// if not exist same itemset in huiSets with tempParticle,insert it
			// into huiSets
			
			if (i == huiSets.size())
				huiSets.add(new HUI(temp.toString(), tempParticle.fitness,support));
		}
	}

	/**
	 * Method to initial percentage
	 * 
	 * @return percentage
	 */
	
	private int support_cal(String itemset){
		itemset = itemset.replace(" ","");
		boolean firstTime = true;
		UtilityList newgen_TIDs = null;
		for(int i = 0;i<itemset.length();i++){
			char item = itemset.charAt(i);
			UtilityList listofItem = mapItemToUtilityList.get((int)item - (int)'0');
			if(firstTime){
				firstTime = false;
				newgen_TIDs = listofItem;
			}
			else{
//				iSet = appendItem(iSet, (int)item);
				newgen_TIDs = construct(newgen_TIDs, listofItem);
			}
//			System.out.print(item + " ");
		}
//		System.out.print("support " + newgen_TIDs.elements.size());
		return newgen_TIDs.elements.size();
	}
	
private UtilityList construct(UtilityList uX, UtilityList uE) {
		
		// create an empy utility list for pXY
		UtilityList uXE = new UtilityList(uE.item);

		// for each element in the utility list of pX
		for(Element elmX : uX.elements){
			// do a binary search to find element ey in py with tid = ex.tid
			Element elmE = findElementWithTID(uE, elmX.tid);
			if(elmE == null){
				continue;
			}
			// Create the new element
			//     IMPORTANT :  TRICKY PART :  WE NEED TO SUBTRACT  ELMX.RUTIL - ELME.iutil
			// THIS IS BECAUSE DCI  DOES NOT ADD ITEMS TO AN ITEMSET ACCORDING TO THE TOTAL ORDER
			Element elmXe = new Element(elmX.tid, elmX.iutils + elmE.iutils, elmX.rutils - elmE.iutils);
			// add the new element to the utility list of pXY
			uXE.addElement(elmXe);
		}
		// return the utility list of Xe.
		return uXE;
	}
	
private Element findElementWithTID(UtilityList ulist, int tid){
	List<Element> list = ulist.elements;
	
	// perform a binary search to check if  the subset appears in  level k-1.
    int first = 0;
    int last = list.size() - 1;
   
    // the binary search
    while( first <= last )
    {
    	int middle = ( first + last ) >>> 1; // divide by 2

        if(list.get(middle).tid < tid){
        	first = middle + 1;  //  the itemset compared is larger than the subset according to the lexical order
        }
        else if(list.get(middle).tid > tid){
        	last = middle - 1; //  the itemset compared is smaller than the subset  is smaller according to the lexical order
        }
        else{
        	return list.get(middle);
        }
    }
	return null;
}


	private List<Double> roulettePercent() {
		int i, sum = 0, tempSum = 0;
		double tempPercent;

		// calculate the sum of twu value of each 1-HTWUIs
		for (i = 0; i < twuPattern.size(); i++) {
			sum = sum + mapItemToTWU.get(twuPattern.get(i));
		}
		// calculate the portation of twu value of each item in sum
		for (i = 0; i < twuPattern.size(); i++) {
			tempSum = tempSum + mapItemToTWU.get(twuPattern.get(i));
			tempPercent = tempSum / (sum + 0.0);
			percentage.add(tempPercent);
		}
		return percentage;
	}

	/**
	 * Method to ensure the posotion of 1 in particle use roulette selection
	 * 
	 * @param percentage
	 *            the portation of twu value of each 1-HTWUIs in sum of twu
	 *            value
	 * @return the position of 1
	 */
	private int rouletteSelect(List<Double> percentage) {
		int i, temp = 0;
		double randNum;
		randNum = Math.random();
		for (i = 0; i < percentage.size(); i++) {
			if (i == 0) {
				if ((randNum >= 0) && (randNum <= percentage.get(0))) {
					temp = 0;
					break;
				}
			} else if ((randNum > percentage.get(i - 1))
					&& (randNum <= percentage.get(i))) {
				temp = i;
				break;
			}
		}
		return temp;
	}

	/**
	 * Method to calculate the fitness of each particle
	 * 
	 * @param tempParticle
	 * @param k
	 *            the number of 1 in particle
	 * @return fitness
	 */
	private int fitCalculate(List<Integer> tempParticle, int k) {
		if (k == 0)
			return 0;
		int i, j, p, q, temp;

		int sum, fitness = 0;
		for (p = 0; p < database.size(); p++) {// p scan the transactions in
												// database
			i = 0;
			j = 0;
			q = 0;
			temp = 0;
			sum = 0;
			// j scan the 1 in particle, q scan each transaction, i scan each
			// particle
			while (j < k && q < database.get(p).size()
					&& i < tempParticle.size()) {
				if (tempParticle.get(i) == 1) {
					if (database.get(p).get(q).item < twuPattern.get(i))
						q++;
					else if (database.get(p).get(q).item == twuPattern.get(i)) {
						sum = sum + database.get(p).get(q).utility;
						j++;
						q++;
						temp++;
						i++;
					} else if (database.get(p).get(q).item > twuPattern.get(i)) {
						j++;
						i++;
					}
				} else
					i++;
			}
			if (temp == k) {
				fitness = fitness + sum;
			}
		}
		return fitness;
	}

	/**
	 * Method to write a high utility itemset to the output file.
	 * 
	 * @throws IOException
	 */
	private void writeOut() throws IOException {
		// Create a string buffer
		StringBuilder buffer = new StringBuilder();
		// append the prefix
		for (int i = 0; i < huiSets.size(); i++) {
			buffer.append(huiSets.get(i).itemset);
			// append the utility value
			buffer.append(" #UTIL: ");
			buffer.append(huiSets.get(i).fitness);
			buffer.append("\t#Support: ");
			buffer.append(huiSets.get(i).support);
			buffer.append(System.lineSeparator());
		}
		// write to file
		writer.write(buffer.toString());
		writer.newLine();
	}

	/**
	 * Method to check the memory usage and keep the maximum memory usage.
	 */
	private void checkMemory() {
		// get the current memory usage
		double currentMemory = (Runtime.getRuntime().totalMemory() - Runtime
				.getRuntime().freeMemory()) / 1024d / 1024d;
		// if higher than the maximum until now
		if (currentMemory > maxMemory) {
			// replace the maximum with the current memory usage
			maxMemory = currentMemory;
		}
	}

	/**
	 * Print statistics about the latest execution to System.out.
	 */
	public void printStats() {
		System.out
				.println("=============  HUIM-BPSO ALGORITHM v.2.11 - STATS =============");
		System.out.println(" Total time ~ " + (endTimestamp - startTimestamp)
				+ " ms");
		System.out.println(" Memory ~ " + maxMemory + " MB");
		System.out.println(" High-utility itemsets count : " + huiSets.size());
		System.out
				.println("===================================================");
	}
	
	private int compareItems(int item1, int item2) {
		int compare = mapItemToTWU.get(item1) - mapItemToTWU.get(item2);
		// if the same, use the lexical order otherwise use the TWU
		return (compare == 0)? item1 - item2 :  compare;
	}
}
