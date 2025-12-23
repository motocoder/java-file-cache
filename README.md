This project has a file cache written in java (works on android).

The file cache is architected as a hash mapping to a forwardly linked list of segments. 

Everything runs off of two files, the hash has a file where the hash index is the position of the file for the key. Each position has 4 bytes to store an int which works like a pointer into the segmented file.
Inside the segmented file is a bucket of key/value combinations which use hashcode and equals similiar to a java hashmap to deduce equality. 


```Java

final BytesFileCache cache = new BytesFileCache(hashCacheDir);

cache.put("key".getBytes(), "value".getBytes());

final String value = cache.get("key".getBytes());

assertEquals("value", value);

```
