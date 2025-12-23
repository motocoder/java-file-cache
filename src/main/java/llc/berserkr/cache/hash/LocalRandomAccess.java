package llc.berserkr.cache.hash;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

public class LocalRandomAccess {

    private final File file;

    public LocalRandomAccess(final File file) {
        this.file = file;
    }

    public void giveReader(RandomAccessFile reader) {
        //old method leaving hooks in for now
    }

    private final ThreadLocal<RandomAccessFile> localReader = new ThreadLocal<>();

    public RandomAccessFile getReader() {

        if(localReader.get() == null) {
            try {
                localReader.set(new RandomAccessFile(file, "r"));
            } catch (FileNotFoundException e) {
                throw new RuntimeException("bad hash file could not write to it");
            }
        }

        return localReader.get();

    }

    public void giveWriter(RandomAccessFile reader) {
        //old method leaving hooks in for now.
    }

    //TODO convert to a system that manages a map to a writer that's at a position close to the seek position
    //we will use it at. I think this can probably just be a tree map. This will improve performance some I think.
    //currently the seek operation is one of the performance choke points.
    private final ThreadLocal<RandomAccessFile> localWriter = new ThreadLocal<>();

    public RandomAccessFile getWriter() {

        if(localWriter.get() == null) {
            try {
                localWriter.set(new RandomAccessFile(file, "rws"));
            } catch (FileNotFoundException e) {
                throw new RuntimeException("bad hash file could not write to it");
            }
        }

        return localWriter.get();
    }

}
