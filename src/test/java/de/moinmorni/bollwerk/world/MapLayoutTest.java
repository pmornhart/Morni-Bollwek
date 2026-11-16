package de.moinmorni.bollwerk.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.moinmorni.bollwerk.logic.DefenceType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MapLayoutTest {

    @Test
    void everyMapHasSeveralLanesEndingAtTheHeart() {
        for (MapType m : MapType.values()) {
            MapLayout l = m.layout();
            assertTrue(l.lanes().size() >= 3, m + ": aus mehreren Richtungen");
            for (MapLayout.Lane lane : l.lanes()) {
                MapLayout.Point end = lane.path().get(lane.path().size() - 1);
                assertTrue(end.flat(l.heart().x(), l.heart().z()) <= l.heartReach() + 0.5, m + "/" + lane.id() + " endet am Herzen");
                assertTrue(lane.start().flat(l.heart().x(), l.heart().z()) > 35, m + "/" + lane.id() + " beginnt weit draußen");
                for (int i = 1; i < lane.path().size(); i++) {
                    assertTrue(lane.path().get(i - 1).distance(lane.path().get(i)) < 22,
                            m + "/" + lane.id() + ": Wegpunkte nah genug für die Wegsuche");
                }
            }
        }
    }

    @Test
    void laneSpotsLieOnTheirLaneSideSpotsBesideIt() {
        for (MapType m : MapType.values()) {
            MapLayout l = m.layout();
            Set<Integer> ids = new HashSet<>();
            int lane = 0;
            int side = 0;
            for (MapLayout.Spot s : l.spots()) {
                assertTrue(ids.add(s.id()), "eindeutige Nummer");
                if (s.kind() == DefenceType.Spot.LANE) {
                    lane++;
                    double d = MapKit.segmentDistanceToLane(l.lanes().get(s.lane()), s.x() + 0.5, s.z() + 0.5);
                    assertTrue(d < 1.0, m + " Platz " + s.id() + " liegt auf dem Weg (" + d + ")");
                } else {
                    side++;
                    assertTrue(MapKit.laneDistance(l, s.x() + 0.5, s.z() + 0.5) > 2.5, m + " Platz " + s.id() + " neben dem Weg");
                    assertEquals(-1, s.lane());
                }
            }
            assertTrue(lane >= l.lanes().size() && side >= 4, m + ": genug Bauplätze");
            for (MapLayout.Spot a : l.spots()) {
                for (MapLayout.Spot b : l.spots()) {
                    if (a != b) {
                        assertTrue(Math.abs(a.x() - b.x()) > 2 || Math.abs(a.z() - b.z()) > 2, m + ": Plätze " + a.id() + "/" + b.id() + " überlappen");
                    }
                }
            }
        }
    }

    @Test
    void progressAlongALane() {
        MapLayout.Lane lane = new MapLayout.Lane("t", List.of(new MapLayout.Point(0, 1, 0), new MapLayout.Point(10, 1, 0),
                new MapLayout.Point(10, 1, 10)));
        assertEquals(20, lane.length(), 1e-9);
        assertEquals(0, lane.progressAt(-5, 0), 1e-9);
        assertEquals(0.25, lane.progressAt(5, 1), 1e-9);
        assertEquals(0.75, lane.progressAt(11, 5), 1e-9);
        assertEquals(1, lane.progressAt(10, 30), 1e-9);
        MapLayout.Point p = lane.pointAt(15);
        assertEquals(10, p.x(), 1e-9);
        assertEquals(5, p.z(), 1e-9);
        assertEquals(new MapLayout.Point(0, 1, 0), lane.pointAt(-3));
        assertEquals(new MapLayout.Point(10, 1, 10), lane.pointAt(99));
    }

    @Test
    void klosterhofRampClimbsOneBlockAtATime() {
        MapLayout l = MapType.KLOSTERHOF.layout();
        int last = KlosterhofPlan.floor(l, 0, 45);
        for (int z = 44; z >= 0; z--) {
            int y = KlosterhofPlan.floor(l, 0, z);
            assertTrue(Math.abs(y - last) <= 1, "Stufe bei z=" + z);
            last = y;
        }
        assertEquals(0, KlosterhofPlan.floor(l, 0, 0));
        assertEquals(-5, KlosterhofPlan.floor(l, 0, 45));
        assertEquals(Integer.MIN_VALUE, KlosterhofPlan.floor(l, 30, 0), "Schlucht unter der Hängebrücke");
    }
}
