package llc.berserkr.cache.collection;

/*
 * dbXML - Native XML Database
 * Copyright (c) 1999-2006 The dbXML Group, L.L.C.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * $Id: SoftHashMap.java,v 1.3 2006/02/02 19:04:15 bradford Exp $
 */

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * SoftHashMap
 */

@SuppressWarnings("rawtypes")
public final class SoftHashMap extends AbstractMap {
   private Map hash = new HashMap();
   private final ReferenceQueue queue = new ReferenceQueue();

   public SoftHashMap() {
   }

   public Object get(Object key) {
      Object res = null;
      SoftReference sr = (SoftReference)hash.get(key);
      if ( sr != null ) {
         res = sr.get();
         if ( res == null )
            hash.remove(key);
      }
      return res;
   }

   private void processQueue() {
      for ( ;; ) {
         SoftValue sv = (SoftValue)queue.poll();
         if ( sv != null )
            hash.remove(sv.key);
         else
            return;
      }
   }

   @SuppressWarnings("unchecked")
   public Object put(Object key, Object value) {
      processQueue();
      return hash.put(key, new SoftValue(value, key, queue));
   }

   public Object remove(Object key) {
      processQueue();
      return hash.remove(key);
   }

   public void clear() {
      processQueue();
      hash.clear();
   }

   public int size() {
      processQueue();
      return hash.size();
   }

   public Set entrySet() {
      /** @todo Figure this out */
      throw new UnsupportedOperationException();
   }


   /**
    * SoftValue
    */

   private static class SoftValue extends SoftReference {
      private final Object key;

    @SuppressWarnings("unchecked")
    private SoftValue(Object k, Object key, ReferenceQueue q) {
         super(k, q);
         this.key = key;
      }
   }
}