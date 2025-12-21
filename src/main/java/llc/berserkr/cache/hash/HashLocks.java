package llc.berserkr.cache.hash;

/**
 * Reading and writing operations can happen simultaneous as long as they don't conflict at several points.
 *
 * Each write operation manipulated the transactional data portion of the SegmentedFile. This currently is a conflict
 * that could be changed.
 *
 * If a segment is writing to an existing segment it can't be read at the same time.
 *
 * A segment also can't be written to by 2 threads at the same time.
 *
 * Searching for the end item and or writing to the end item can't happen by 2 threads at the same time.
 *
 * Merging/splitting can only happen per master address by one thread at the same time.
 *
 * The hash buckets are stored in their own segmented file that has these same rules but the overall policy used for
 * them will just be a global lock per index since the read/write operations of the hash blob are fairly isolated
 * and fast.
 *
 * This class handles just the locks for the hash index blobs.
 */
public class HashLocks {

    public enum LockType {READER, WRITER}

}
