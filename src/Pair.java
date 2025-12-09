import java.util.Objects;

public record Pair<A, B>(A first, B second) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair<?, ?> p)) return false;
        return Objects.equals(first, p.first) &&
                Objects.equals(second, p.second);
    }

}
