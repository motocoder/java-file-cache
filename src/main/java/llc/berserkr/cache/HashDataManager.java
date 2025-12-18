package llc.berserkr.cache;

import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.WriteFailure;

import java.util.Map.Entry;
import java.util.Set;

public interface HashDataManager<Key, Value> {
   
    Set<Pair<Key, Value>> getBlobsAt(final long blobIndex) throws ReadFailure;

    long setBlobs(final long blobIndex, final Set<Pair<Key, Value>> blobs) throws WriteFailure, ReadFailure;
   
    void eraseBlobs(final long blobIndex) throws WriteFailure, ReadFailure;

    void clear() throws WriteFailure, ReadFailure;
    
}
