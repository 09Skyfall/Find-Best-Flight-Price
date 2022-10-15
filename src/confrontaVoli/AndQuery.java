package confrontaVoli;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

public class AndQuery implements Query{
    private final List<Query> queryList;

    public AndQuery(@NotNull List<Query> ql){
        this.queryList = ql;
    }

    @Override
    public Stream<Flight> matches(Stream<Flight> sf){
        for(Query q : queryList){
            sf = q.matches(sf);
        }
        return sf;
    }
}
