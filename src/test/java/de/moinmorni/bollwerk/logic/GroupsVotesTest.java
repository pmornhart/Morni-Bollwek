package de.moinmorni.bollwerk.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GroupsVotesTest {

    private enum Map {
        A, B, C
    }

    private static List<UUID> ids(int n) {
        return java.util.stream.IntStream.range(0, n).mapToObj(i -> new UUID(0, i + 1)).toList();
    }

    @Test
    void partiesStayTogether() {
        List<UUID> u = ids(9);
        List<List<UUID>> groups = List.of(List.of(u.get(0)), List.of(u.get(1), u.get(2), u.get(3), u.get(4)),
                List.of(u.get(5), u.get(6)), List.of(u.get(7)), List.of(u.get(8)));
        List<UUID> picked = Groups.pick(groups, 6);
        assertEquals(List.of(u.get(0), u.get(1), u.get(2), u.get(3), u.get(4), u.get(7)), picked,
                "Zweier-Party passt nicht mehr, der Einzelne dahinter rückt nach");
    }

    @Test
    void hugePartyFillsTheRound() {
        List<UUID> u = ids(8);
        assertEquals(u.subList(0, 6), Groups.pick(List.of(u), 6));
        assertEquals(List.of(), Groups.pick(List.of(), 6));
    }

    @Test
    void mostVotesWinsAmongFreeMaps() {
        Votes<Map> v = new Votes<>(Map.class);
        List<UUID> u = ids(4);
        v.vote(u.get(0), Map.B);
        v.vote(u.get(1), Map.B);
        v.vote(u.get(2), Map.C);
        assertEquals(Map.B, v.winner(EnumSet.allOf(Map.class)));
        assertEquals(Map.C, v.winner(EnumSet.of(Map.A, Map.C)), "B ist belegt");
        assertNull(v.winner(EnumSet.noneOf(Map.class)));
        v.vote(u.get(1), Map.C);
        assertEquals(Map.C, v.winner(EnumSet.allOf(Map.class)), "Stimme geändert");
        v.retain(List.of(u.get(0)));
        assertEquals(Map.B, v.winner(EnumSet.allOf(Map.class)), "wer weg ist, zählt nicht");
    }

    @Test
    void tieGoesToTheMapPlayedLongestAgo() {
        Votes<Map> v = new Votes<>(Map.class);
        assertEquals(Map.A, v.winner(EnumSet.allOf(Map.class)));
        v.played(Map.A, 100);
        v.played(Map.B, 50);
        assertEquals(Map.C, v.winner(EnumSet.allOf(Map.class)), "C nie gespielt");
        v.played(Map.C, 200);
        assertEquals(Map.B, v.winner(EnumSet.allOf(Map.class)));
        List<UUID> u = ids(2);
        v.vote(u.get(0), Map.A);
        v.clear(u);
        assertEquals(Map.B, v.winner(EnumSet.allOf(Map.class)));
    }
}
