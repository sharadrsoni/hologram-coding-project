package com.hologramsciences;

import java.util.ArrayList;
import java.util.List;

public class Algorithms {
    /**
     *
     *  Compute the cartesian product of a list of lists of any type T
     *  the result is a list of lists of type T, where each element comes
     *  each successive element of the each list.
     *
     *  https://en.wikipedia.org/wiki/Cartesian_product
     *
     *  For this problem order matters.
     *
     *  Example:
     *
     *   listOfLists = Arrays.asList(
     *                         Arrays.asList("A", "B"),
     *                         Arrays.asList("K", "L")
     *                 )
     *
     *   returns:
     *
     *   Arrays.asList(
     *         Arrays.asList("A", "K"),
     *         Arrays.asList("A", "L"),
     *         Arrays.asList("B", "K"),
     *         Arrays.asList("B", "L")
     *   )
     *
     *
     *
     */
    public static <T> List<List<T>> cartesianProductForLists(final List<List<T>> listOfLists) {
        List<List<T>> result = new ArrayList<>();
        List<T> firstRow = listOfLists.get(0);
        for (T t : firstRow) {
            List<T> row = new ArrayList<>();
            row.add(t);
            recursiveCall(1, listOfLists, row, result);
        }
        return result;
    }

    private static <T> void recursiveCall(int index, List<List<T>> listOfList, List<T> row, List<List<T>> result) {
        if(listOfList.size() > index+1) {
            List<T> subsequentRow = listOfList.get(index);
            for (T t : subsequentRow) {
                List<T> newRow = new ArrayList<>(row);
                newRow.add(t);
                recursiveCall(index + 1, listOfList, newRow, result);
            }
        } else {
            List<T> lastRow = listOfList.get(index);
            for (T t : lastRow) {
                List<T> newRow = new ArrayList<>(row);
                row.add(t);
                result.add(row);
                row = newRow;
            }
        }
    }

    /**
     *
     *  In the United States there are six coins:
     *  1¢ 5¢ 10¢ 25¢ 50¢ 100¢
     *  Assuming you have an unlimited supply of each coin,
     *  implement a method which returns the number of distinct ways to make totalCents
     */
    public static long countNumWaysMakeChange(final int totalCents) {
        final int[] coins = new int[]{1,5,10,25,50,100};
        long[][] valueData = new long[coins.length+1][totalCents+1];
        for(int coinIndex = 0; coinIndex <= coins.length; coinIndex++) {
            for(int centIndex = 0; centIndex <= totalCents; centIndex++) {
                if(centIndex == 0) {
                    valueData[coinIndex][centIndex] = 1;
                } else if (coinIndex == 0) {
                    break;
                } else {
                    int useTheCoin = centIndex - coins[coinIndex-1];
                    if(useTheCoin >= 0) {
                        valueData[coinIndex][centIndex] = valueData[coinIndex][useTheCoin] + valueData[coinIndex - 1][centIndex];
                    } else {
                        valueData[coinIndex][centIndex] = valueData[coinIndex - 1][centIndex];
                    }
                }
            }
        }
        return valueData[coins.length][totalCents];
    }
}
