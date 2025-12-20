package llc.berserkr.cache.data;

import java.util.Objects;

public class Pair<ValueOne, ValueTwo> {

    private final ValueOne one;
    private final ValueTwo two;

    public Pair(ValueOne one, ValueTwo two) {
        this.one = one;
        this.two = two;
    }

    public ValueOne getOne() {
        return one;
    }

    public ValueTwo getTwo() {
        return two;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(one, pair.one) && Objects.equals(two, pair.two);
    }

    @Override
    public int hashCode() {
        return Objects.hash(one, two);
    }
}
