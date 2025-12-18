package llc.berserkr.cache;

import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.WriteFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MemoryHashDataManager<Key, Value> implements HashDataManager<Key, Value> {

    private static final Logger logger = LoggerFactory.getLogger(MemoryHashDataManager.class);

    private long blobIndexGenerate = 0;

    private final Map<Long, Set<Pair<Key, Value> >> data = new HashMap<>();

    @Override
    public Set<Pair<Key, Value>> getBlobsAt(long blobIndex) throws ReadFailure {
        return new HashSet<>(data.get(blobIndex));
    }

    @Override
    public long setBlobs(long blobIndex, Set<Pair<Key, Value>> blobs) throws WriteFailure, ReadFailure {

        final long myAddress = ++blobIndexGenerate;

        data.put(myAddress, blobs);
        data.put(blobIndex, null);

        return myAddress;

    }

    @Override
    public void eraseBlobs(long blobIndex) throws WriteFailure, ReadFailure {
        data.remove(blobIndex);
    }

    @Override
    public void clear() throws WriteFailure, ReadFailure {
        data.clear();
    }
}
