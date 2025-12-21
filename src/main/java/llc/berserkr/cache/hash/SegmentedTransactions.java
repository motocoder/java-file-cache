package llc.berserkr.cache.hash;

import llc.berserkr.cache.exception.ReadFailure;
import llc.berserkr.cache.exception.WriteFailure;

public class SegmentedTransactions {

    public static void endTransactions(SegmentedStreamingFile segmentedFile) throws ReadFailure, WriteFailure {
        segmentedFile.writeTransactionalBytes(new byte[] {});
    }

    public static void startWritingTransaction(SegmentedStreamingFile segmentedFile, long address) throws ReadFailure, WriteFailure {

        final byte[] addressBytes = SegmentedStreamingFile.longToByteArray(address);
        final byte [] writeTransaction = new byte[] {
                SegmentedStreamingFile.WRITING_TRANSACTION, //reversal of a write just deletes it anyway
                addressBytes[0],
                addressBytes[1],
                addressBytes[2],
                addressBytes[3],
                addressBytes[4],
                addressBytes[5],
                addressBytes[6],
                addressBytes[7]
        };

        segmentedFile.writeTransactionalBytes(
                writeTransaction
        );

    }

    public static void startAddTransaction(SegmentedStreamingFile segmentedFile, int length) throws ReadFailure, WriteFailure {

        final byte[] lengthBytes = SegmentedStreamingFile.intToByteArray(length);

        final byte [] writeTransaction = new byte[] {
                SegmentedStreamingFile.ADD_END_TRANSACTION, //if merge fails we will finish the merge but leave it empty
                lengthBytes[0],
                lengthBytes[1],
                lengthBytes[2],
                lengthBytes[3]
        };

        segmentedFile.writeTransactionalBytes(
                writeTransaction
        );

    }

    public static void startMergeTransaction(SegmentedStreamingFile segmentedFile, long address, int segmentSize) throws ReadFailure, WriteFailure {

        final byte[] addressBytes = SegmentedStreamingFile.longToByteArray(address);
        final byte[] lengthBytes = SegmentedStreamingFile.intToByteArray(segmentSize);

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
                lengthBytes[3]
        };

        segmentedFile.writeTransactionalBytes(
                writeTransaction
        );

    }


}
