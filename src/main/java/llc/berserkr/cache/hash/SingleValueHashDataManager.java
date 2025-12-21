package llc.berserkr.cache.hash;

import llc.berserkr.cache.data.Pair;
import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.WriteFailure;

import java.util.Set;

public interface SingleValueHashDataManager<Key, Value> {
   
    Value getBlobsAt(final long blobIndex) throws ReadFailure;

    long setBlobs(final long blobIndex, Value value) throws WriteFailure, ReadFailure;
   
    void eraseBlobs(final long blobIndex) throws WriteFailure, ReadFailure;

    void clear() throws WriteFailure, ReadFailure;
    
}
