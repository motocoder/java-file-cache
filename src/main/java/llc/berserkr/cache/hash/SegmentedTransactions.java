package llc.berserkr.cache.hash;

import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.WriteFailure;

import static llc.berserkr.cache.util.DataUtils.intToByteArray;
import static llc.berserkr.cache.util.DataUtils.longToByteArray;

public class SegmentedTransactions {

    /**
     *
     * @param segmentedFile
     * @throws ReadFailure
     * @throws WriteFailure
     */
    public static void endTransactions(SegmentedStreamingFile segmentedFile, long address) throws ReadFailure, WriteFailure {
        segmentedFile.clearTransaction(address);
    }

    /**
     * sets a writing transaction, address data is stored in the event we crash we can delete that segment and mark it free
     *
     * @param segmentedFile
     * @param address
     * @throws ReadFailure
     * @throws WriteFailure
     */
    public static long startWritingTransaction(SegmentedStreamingFile segmentedFile, long address) throws ReadFailure, WriteFailure {

        final byte[] addressBytes = longToByteArray(address);
        final byte [] writeTransaction = new byte[] {
            SegmentedStreamingFile.WRITING_TRANSACTION, //reversal of a write just deletes it anyway
            addressBytes[0],
            addressBytes[1],
            addressBytes[2],
            addressBytes[3],
            addressBytes[4],
            addressBytes[5],
            addressBytes[6],
            addressBytes[7],
            SegmentedStreamingFile.WRITING_TRANSACTION //end item is set as well so we know it wrote everything in between
        };

        return segmentedFile.writeTransactionalBytes(
            writeTransaction
        );

    }

    /**
     * start add transaction, store length so we can finish adding it to the end if we crash
     *
     * @param segmentedFile
     * @param length
     * @throws ReadFailure
     * @throws WriteFailure
     */
    public static long startAddTransaction(SegmentedStreamingFile segmentedFile, int length) throws ReadFailure, WriteFailure {

        final byte[] lengthBytes = intToByteArray(length);

        final byte [] writeTransaction = new byte[] {
            SegmentedStreamingFile.ADD_END_TRANSACTION, //if merge fails we will finish the merge but leave it empty
            lengthBytes[0],
            lengthBytes[1],
            lengthBytes[2],
            lengthBytes[3],
            SegmentedStreamingFile.ADD_END_TRANSACTION    //end item is set as well so we know it wrote everything in between
        };

        return segmentedFile.writeTransactionalBytes(
            writeTransaction
        );

    }

    /**
     * start merge, store the address and segment size so if we crash we can finish merging the segments.
     *
     * @param segmentedFile
     * @param address
     * @param segmentSize
     * @throws ReadFailure
     * @throws WriteFailure
     */
    public static long startMergeTransaction(SegmentedStreamingFile segmentedFile, long address, int segmentSize) throws ReadFailure, WriteFailure {

        final byte[] addressBytes = longToByteArray(address);
        final byte[] lengthBytes = intToByteArray(segmentSize);

        final byte [] writeTransaction = new byte[] {
            SegmentedStreamingFile.MERGE_TRANSACTION, //if merge fails we will finish the merge but leave it empty
            addressBytes[0],
            addressBytes[1],
            addressBytes[2],
            addressBytes[3],
            addressBytes[4],
            addressBytes[5],
            addressBytes[6],
            addressBytes[7],
            lengthBytes[0],
            lengthBytes[1],
            lengthBytes[2],
            lengthBytes[3],
            SegmentedStreamingFile.MERGE_TRANSACTION //end item is set as well so we know it wrote everything in between
        };

        return segmentedFile.writeTransactionalBytes(
            writeTransaction
        );

    }

}
