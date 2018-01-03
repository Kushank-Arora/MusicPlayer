package com.kushank.olaplaybykushank;

import java.util.ArrayList;

/**
 * It contains common utility functions.
 */

class util {
    static String arrayListToString(ArrayList<String> list) {
        StringBuilder artists = new StringBuilder();
        if (list.size() > 0)
            artists.append(list.get(0));
        for (int i = 1; i < list.size(); i++)
            artists.append(", ").append(list.get(i));
        return artists.toString();
    }
}
