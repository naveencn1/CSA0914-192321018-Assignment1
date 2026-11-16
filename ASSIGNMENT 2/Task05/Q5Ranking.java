import java.util.*;

/**
 * Q5 - Generic ranking component derived from the consistency principle.
 * Rank: points DESC, then goal difference DESC, then name ASC (final tie-breaker -> total order).
 */
public class Q5Ranking {

    // ---------- Comparators ----------
    static final Comparator<Team> BY_RANK =
        Comparator.comparingInt((Team t) -> t.points).reversed()
                  .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
                  .thenComparing(t -> t.name);

    /** FAULTY 1: subtraction -> int overflow breaks transitivity/antisymmetry. */
    static final Comparator<Team> FAULTY_SUBTRACTION =
        (a, b) -> b.points - a.points;

    /** FAULTY 2: ignores the tie-breakers (equal points => "equal"), order depends on input order. */
    static final Comparator<Team> FAULTY_NO_TIEBREAK =
        Comparator.comparingInt((Team t) -> t.points).reversed();

    // ---------- Helpers ----------
    static <T> boolean isConsistent(List<T> data, Comparator<? super T> c) {
        // sgn(compare(a,b)) == -sgn(compare(b,a)) and transitivity for every triple
        for (T a : data) for (T b : data)
            if (Integer.signum(c.compare(a, b)) != -Integer.signum(c.compare(b, a))) return false;
        for (T a : data) for (T b : data) for (T x : data)
            if (c.compare(a, b) < 0 && c.compare(b, x) < 0 && c.compare(a, x) >= 0) return false;
        return true;
    }

    static List<Team> reversedCopy(List<Team> l) { List<Team> r = new ArrayList<>(l); Collections.reverse(r); return r; }

    public static void main(String[] args) {
        // ----- Data set 1: many ties at every level -----
        List<Team> ties = List.of(
            new Team("Delta", 10, 5), new Team("Alpha", 10, 5), new Team("Charlie", 10, 5),   // all tie on points+GD
            new Team("Bravo", 10, 2), new Team("Echo", 10, 2),                               // tie on points, GD lower
            new Team("Foxtrot", 7, 3), new Team("Golf", 7, 3), new Team("Hotel", 7, -1),
            new Team("India", 3, 0));

        Leaderboard<Team> lb = new Leaderboard<>(BY_RANK);
        lb.addAll(ties);                                  // wildcard: List<? extends T>
        System.out.println("Data set 1 (ties) ranking : " + lb.ranking());
        Leaderboard<Team> lb2 = new Leaderboard<>(BY_RANK);
        lb2.addAll(reversedCopy(ties));
        System.out.println("Same data, reversed input : " + lb2.ranking());
        System.out.println("Deterministic regardless of input order? " + lb.ranking().equals(lb2.ranking()));
        System.out.println("Original items untouched (insertion order): " + lb.items());
        System.out.println("Comparator consistent (antisymmetric + transitive)? " + isConsistent(ties, BY_RANK));

        // ----- Data set 2: designed to break faulty comparators -----
        List<Team> breaker = List.of(
            new Team("Max", Integer.MAX_VALUE, 0),
            new Team("Zero", 0, 0),
            new Team("Neg", -2_000_000_000, 0),      // Max - Neg overflows
            new Team("T1", 5, 9), new Team("T2", 5, 1), new Team("T3", 5, 1));
        System.out.println("\nData set 2 (designed to break faulty comparators)");
        System.out.println("Correct comparator consistent?    " + isConsistent(breaker, BY_RANK));
        System.out.println("Subtraction comparator consistent? " + isConsistent(breaker, FAULTY_SUBTRACTION));
        System.out.println("  compare(Max, Neg) via subtraction = " + FAULTY_SUBTRACTION.compare(
            breaker.get(0), breaker.get(2)) + "  (should be <0 because Max ranks above Neg; overflow gives a wrong sign)");
        try {
            List<Team> big = new ArrayList<>();
            Random r = new Random(5);
            for (int i = 0; i < 2000; i++) big.add(new Team("X" + i, r.nextInt(), 0));
            big.sort(FAULTY_SUBTRACTION);
            System.out.println("  sorting 2000 random-int teams with subtraction: no exception, but sorted correctly? "
                + isSortedBy(big, BY_RANK));
        } catch (IllegalArgumentException e) {
            System.out.println("  TimSort detected inconsistency: " + e.getMessage());
        }
        Leaderboard<Team> f1 = new Leaderboard<>(FAULTY_NO_TIEBREAK); f1.addAll(ties);
        Leaderboard<Team> f2 = new Leaderboard<>(FAULTY_NO_TIEBREAK); f2.addAll(reversedCopy(ties));
        System.out.println("No-tie-breaker ranking (input order) : " + f1.ranking());
        System.out.println("No-tie-breaker ranking (reversed)    : " + f2.ranking());
        System.out.println("Same result? " + f1.ranking().equals(f2.ranking()) + "  -> ranking depends on input order = incorrect");

        // ----- Alternative: three stable sorts (name, then goalDiff, then points) -----
        List<Team> three = new ArrayList<>(ties);
        three.sort(Comparator.comparing((Team t) -> t.name));
        three.sort(Comparator.comparingInt((Team t) -> t.goalDiff).reversed());
        three.sort(Comparator.comparingInt((Team t) -> t.points).reversed());
        System.out.println("\nThree stable sorts result equals comparator chain? " + three.equals(lb.ranking()));

        // ----- Comparable alternative -----
        List<RankedTeam> rt = new ArrayList<>();
        for (Team t : ties) rt.add(new RankedTeam(t.name, t.points, t.goalDiff));
        Collections.sort(rt);
        System.out.println("Comparable<RankedTeam> natural order: " + rt);

        // ----- Bounded generic method: works for anything Comparable -----
        System.out.println("max by natural order: " + best(rt));

        // ----- Timing List.sort vs repeated sorting -----
        List<Team> big = new ArrayList<>();
        Random r = new Random(11);
        for (int i = 0; i < 200_000; i++) big.add(new Team("T" + r.nextInt(50_000), r.nextInt(100), r.nextInt(50)));
        double chain = avg(() -> { List<Team> c = new ArrayList<>(big); c.sort(BY_RANK); });
        double thrice = avg(() -> {
            List<Team> c = new ArrayList<>(big);
            c.sort(Comparator.comparing((Team t) -> t.name));
            c.sort(Comparator.comparingInt((Team t) -> t.goalDiff).reversed());
            c.sort(Comparator.comparingInt((Team t) -> t.points).reversed());
        });
        System.out.printf("%nSort 200,000 teams (avg of 5 runs): comparator chain = %.1f ms, three stable sorts = %.1f ms%n", chain, thrice);
    }

    static boolean isSortedBy(List<Team> l, Comparator<Team> c) {
        for (int i = 1; i < l.size(); i++) if (c.compare(l.get(i - 1), l.get(i)) > 0) return false;
        return true;
    }
    static <T extends Comparable<? super T>> T best(List<? extends T> l) {
        T m = null; for (T x : l) if (m == null || x.compareTo(m) < 0) m = x; return m;
    }
    static double avg(Runnable r) {
        for (int i = 0; i < 2; i++) r.run();
        long t = 0; for (int i = 0; i < 5; i++) { long a = System.nanoTime(); r.run(); t += System.nanoTime() - a; }
        return t / 5.0 / 1e6;
    }
}

class Team {
    final String name; final int points; final int goalDiff;
    Team(String name, int points, int goalDiff) {
        this.name = name; this.points = points; this.goalDiff = goalDiff;
    }
    @Override public String toString() {
        return name + "(" + points + "," + goalDiff + ")";
    }
}

/** Comparable alternative: ONE natural ordering baked into the class. */
class RankedTeam implements Comparable<RankedTeam> {
    final String name; final int points; final int goalDiff;
    RankedTeam(String n, int p, int g) { name = n; points = p; goalDiff = g; }
    @Override public int compareTo(RankedTeam o) {
        int c = Integer.compare(o.points, points);
        if (c != 0) return c;
        c = Integer.compare(o.goalDiff, goalDiff);
        return c != 0 ? c : name.compareTo(o.name);
    }
    @Override public String toString() { return name + "(" + points + "," + goalDiff + ")"; }
}

class Leaderboard<T> {
    private final List<T> items = new ArrayList<>();
    private final Comparator<? super T> order;

    Leaderboard(Comparator<? super T> order) { this.order = order; }

    void add(T item) { items.add(item); }

    /** Wildcard: accepts List<Team>, List<SubclassOfTeam>, ... (producer => extends). */
    void addAll(List<? extends T> more) { items.addAll(more); }

    List<T> items() { return Collections.unmodifiableList(items); }

    List<T> ranking() {
        // TODO 1 (completed): return a new sorted copy using 'order', without modifying 'items'
        List<T> copy = new ArrayList<>(items);
        copy.sort(order);
        return copy;
    }
}
