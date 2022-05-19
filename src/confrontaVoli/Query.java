package confrontaVoli;

import java.util.stream.Stream;

public interface Query {
    Stream<Flight> matches(Stream<Flight> sf);
}
