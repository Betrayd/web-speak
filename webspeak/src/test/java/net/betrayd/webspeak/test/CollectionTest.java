package net.betrayd.webspeak.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.betrayd.webspeak.impl.util.WebSpeakUtils;
import net.betrayd.webspeak.impl.util.WebSpeakUtils.Pair;

public class CollectionTest {

    @Test
    public void testCompareAll() {
        List<Integer> ints = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            ints.add(i);
        }

        List<Pair<Integer, Integer>> comparing = new ArrayList<>();

        for (var pair : WebSpeakUtils.compareAll(ints)) {
            System.out.println(pair);
            comparing.add(pair);
        }

        System.out.println(comparing);
    }
}
