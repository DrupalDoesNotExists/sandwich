package ru.dldnex.bundle.util;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class CollectionUtils {
    public static <T> List<T> reverseList(List<T> list) {
        List<T> result = new ArrayList<>();
        ListIterator<T> listIterator = list.listIterator(list.size());
        while (listIterator.hasPrevious()) {
            result.add(listIterator.previous());
        }
        return result;
    }
}
