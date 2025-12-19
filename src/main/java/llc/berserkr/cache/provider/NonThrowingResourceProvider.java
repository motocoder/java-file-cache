package llc.berserkr.cache.provider;

/**
 * Created by seanr_000 on 12/17/2015.
 */
public interface NonThrowingResourceProvider<Value> extends ResourceProvider<Value> {

    Value provide();
}
