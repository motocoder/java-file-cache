package llc.berserkr.cache.util;

public class ParallelControl<Value> {
    
    private Object blockingLock = new Object();
    private Value value;
    private int unblockedCount;
    private int blockedCount;
    private boolean unblockAll;
    
    public void blockOnce() throws InterruptedException {
        
        synchronized(blockingLock) { 
            
            final int myUnblock = ++blockedCount;
            
            while(myUnblock > unblockedCount) {
                
                if(Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("<ParallelControl><1>, was interrupted");
                }
                
                if(unblockAll) { 
                    break;
                }

                blockingLock.wait(1000);
                
                
            }
            
        }
        
    }
    
    public void unBlockOnce() {
        
        synchronized(blockingLock) {
            
            unblockedCount++;
            blockingLock.notifyAll();
            
        }
        
    }
    
    public void unBlockAll() {
        
        synchronized(blockingLock) {
            
            unblockAll = true;
            blockingLock.notifyAll();
            
        }
        
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }
    
}
