/**
 *
 */
package com.corebreaker.dbaccess.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

/**
 * @author Frédéric Meyer
 * @version 1.0
 */
public final class SortedValueMap<K, V extends Comparable<? super V>> implements Serializable, Map<K, V>
{
   /**
    * @author Frédéric Meyer
    * @param <T>
    */
   private static class ValueRef<T extends Comparable<? super T>> implements Serializable
   {
      /**
       *
       */
      private static final long serialVersionUID = -4999130181548954953L;

      /**
       * @param aNode
       * @return
       */
      private static <U extends Comparable<? super U>> int nodeHeight(final ValueRef<U> aNode)
      {
         int lH= 0;

         try
         {
            lH= aNode.mHeight;
         }
         catch(final NullPointerException eNp)
         {
         }

         return lH;
      }

      private final T     mValue;
      private ValueRef<T> mR;
      private ValueRef<T> mL;
      private int         mHeight;

      /**
       *
       */
      private void updateHeight()
      {
         mHeight= Math.max(ValueRef.nodeHeight(mL), ValueRef.nodeHeight(mR)) + 1;
      }

      /**
       * @return
       */
      private int balance()
      {
         return ValueRef.nodeHeight(mL) - ValueRef.nodeHeight(mR);
      }

      /**
       * @return
       */
      private ValueRef<T> rotateToRight()
      {
         final ValueRef<T> lRes= mL;

         mL= lRes.mR;
         lRes.mR= this;

         updateHeight();

         return lRes;
      }

      /**
       * @return
       */
      private ValueRef<T> rotateToLeft()
      {
         final ValueRef<T> lRes= mR;

         mR= lRes.mL;
         lRes.mL= this;

         updateHeight();

         return lRes;
      }

      /**
       * @param aValue
       */
      ValueRef(final T aValue)
      {
         super();

         mValue= aValue;
         mR= mL= null;
         mHeight= 1;
      }

      /**
       * @return
       */
      ValueRef<T> left()
      {
         return mL;
      }

      /**
       * @return
       */
      ValueRef<T> right()
      {
         return mR;
      }

      /**
       * @return
       */
      T get()
      {
         return mValue;
      }

      /**
       * @param aVal
       * @return
       */
      boolean has(final T aVal)
      {
         final int lCmp= aVal.compareTo(mValue);

         if( lCmp < 0 )
            try
            {
               return mL.has(aVal);
            }
            catch(final NullPointerException eNp)
            {
               return false;
            }
         else if( lCmp > 0 )
            try
            {
               return mR.has(aVal);
            }
            catch(final NullPointerException eNp)
            {
               return false;
            }
         else
            return true;
      }

      /**
       * @return
       */
      int count()
      {
         int lRes= 1;

         try
         {
            lRes+= mL.count();
         }
         catch(final NullPointerException eNp)
         {
         }

         try
         {
            lRes+= mR.count();
         }
         catch(final NullPointerException eNp)
         {
         }

         return lRes;
      }

      /**
       * @param aNode
       * @return
       */
      ValueRef<T> add(final ValueRef<T> aNode)
      {
         if( aNode.mValue.compareTo(mValue) <= 0 )
            try
            {
               mL= mL.add(aNode);
            }
            catch(final NullPointerException eNp)
            {
               mL= aNode;
            }
         else
            try
            {
               mR= mR.add(aNode);
            }
            catch(final NullPointerException eNp)
            {
               mR= aNode;
            }

         final int lBalance= balance();
         ValueRef<T> lRes= this;

         if( Math.abs(lBalance) > 1 )
         {
            if( lBalance > 0 )
            {
               if( mL.balance() < 0 )
                  mL= mL.rotateToLeft();

               lRes= rotateToRight();
            }
            else
            {
               if( mR.balance() > 0 )
                  mR= mR.rotateToRight();

               lRes= rotateToLeft();
            }
         }

         lRes.updateHeight();

         return lRes;
      }

      T find(final T aRef)
      {
         T lRes= null;
         final int lCmp= aRef.compareTo(mValue);

         if( lCmp == 0 )
            lRes= mValue;
         else if( lCmp < 0 )
            try
            {
               lRes= mL.find(aRef);
            }
            catch(final NullPointerException eNp)
            {
            }
         else
            try
            {
               lRes= mR.find(aRef);
            }
            catch(final NullPointerException eNp)
            {
            }

         return lRes;
      }

      /**
       * @return
       */
      String makeString()
      {
         String lL= "", lR= "";

         try
         {
            lL= mL.makeString();
         }
         catch(final NullPointerException eNp)
         {
         }

         try
         {
            lR= mR.makeString();
         }
         catch(final NullPointerException eNp)
         {
         }

         return lL + ", " + mValue + lR;
      }

      /**
       * @see java.lang.Object#toString()
       */
      @Override
      public String toString()
      {
         return mValue.toString();
      }
   }

   /**
    * @author Frédéric Meyer
    * @param <N>
    */
   private static class NodeState<N extends Comparable<? super N>>
   {
      private final ValueRef<N>   mRef;
      private Stack<NodeState<N>> mStack;

      /**
       * @param aStack
       */
      private void setStack(final Stack<NodeState<N>> aStack)
      {
         mStack= aStack;
      }

      /**
       * @param aRef
       */
      private NodeState(final ValueRef<N> aRef)
      {
         super();

         mRef= aRef;
         mStack= null;
      }

      /**
       * @param aStack
       * @param aRoot
       */
      NodeState(final Stack<NodeState<N>> aStack, final ValueRef<N> aRoot)
      {
         super();

         ValueRef<N> lNode= aRoot;

         while( lNode.left() != null )
         {
            aStack.push(new NodeState<N>(lNode));
            lNode= lNode.left();
         }

         mRef= lNode;
         setStack(aStack);
      }

      /**
       * @return
       */
      ValueRef<N> ref()
      {
         return mRef;
      }

      /**
       * @return
       */
      public NodeState<N> next()
      {
         NodeState<N> lRes= null;

         try
         {
            lRes= new NodeState<N>(mStack, mRef.right());
         }
         catch(final NullPointerException eNp)
         {
            try
            {
               lRes= mStack.pop();
               lRes.setStack(mStack);
            }
            catch(final EmptyStackException eEs)
            {
            }
         }

         return lRes;
      }

      /**
       * @see java.lang.Object#toString()
       */
      @Override
      public String toString()
      {
         return mRef.toString();
      }
   }

   /**
    *
    */
   private static final long         serialVersionUID = -6076013092430290081L;

   private final Map<K, ValueRef<V>> mMap             = new Hashtable<K, ValueRef<V>>();
   private ValueRef<V>               mRoot            = null;

   /**
    *
    */
   public SortedValueMap()
   {
      super();
   }

   /**
    * @param aRef
    * @return
    */
   public V findValue(final V aRef)
   {
      try
      {
         return mRoot.find(aRef);
      }
      catch(final NullPointerException eNp)
      {
         return null;
      }
   }

   /**
    * @see java.util.Map#size()
    */
   public int size()
   {
      return mMap.size();
   }

   /**
    * @see java.util.Map#isEmpty()
    */
   public boolean isEmpty()
   {
      return mMap.isEmpty();
   }

   /**
    * @see java.util.Map#containsKey(java.lang.Object)
    */
   public boolean containsKey(final Object aKey)
   {
      return mMap.containsKey(aKey);
   }

   /**
    * @see java.util.Map#containsValue(java.lang.Object)
    */
   @SuppressWarnings("unchecked")
   public boolean containsValue(final Object aValue)
   {
      try
      {
         return mRoot.has((V) aValue);
      }
      catch(final Exception eNp)
      {
         return false;
      }
   }

   /**
    * @see java.util.Map#get(java.lang.Object)
    */
   public V get(final Object aKey)
   {
      try
      {
         return mMap.get(aKey).get();
      }
      catch(final Exception eEx)
      {
         return null;
      }
   }

   /**
    * @see java.util.Map#put(java.lang.Object, java.lang.Object)
    */
   public V put(final K aKey, final V aValue)
   {
      if( mMap.containsKey(aKey) )
         return mMap.get(aKey).get();

      final ValueRef<V> lRef= new ValueRef<V>(aValue);

      mMap.put(aKey, lRef);

      try
      {
         mRoot= mRoot.add(lRef);
      }
      catch(final NullPointerException eNp)
      {
         mRoot= lRef;
      }

      return null;
   }

   /**
    * @see java.util.Map#remove(java.lang.Object)
    */
   public V remove(final Object aKey)
   {
      throw new UnsupportedOperationException();
   }

   /**
    * @see java.util.Map#putAll(java.util.Map)
    */
   public void putAll(final Map<? extends K, ? extends V> aMap)
   {
      for(final K fKey : aMap.keySet())
         put(fKey, aMap.get(fKey));
   }

   /**
    * @see java.util.Map#clear()
    */
   public void clear()
   {
      mRoot= null;
      mMap.clear();
   }

   /**
    * @see java.util.Map#keySet()
    */
   public Set<K> keySet()
   {
      return Collections.unmodifiableSet(mMap.keySet());
   }

   /**
    * @see java.util.Map#values()
    */
   @Override
   public Collection<V> values()
   {
      return new Collection<V>()
      {
         public boolean add(final V aE)
         {
            throw new UnsupportedOperationException();
         }

         public boolean addAll(final Collection<? extends V> aC)
         {
            throw new UnsupportedOperationException();
         }

         public void clear()
         {
            throw new UnsupportedOperationException();
         }

         @SuppressWarnings("unchecked")
         public boolean contains(final Object aVal)
         {
            try
            {
               return mRoot.has((V) aVal);
            }
            catch(final Exception e)
            {
               return false;
            }
         }

         public boolean containsAll(final Collection<?> aC)
         {
            for(final Object fObj : aC)
               if( !contains(fObj) )
                  return false;

            return true;
         }

         public boolean isEmpty()
         {
            return mRoot == null;
         }

         public Iterator<V> iterator()
         {
            return new Iterator<V>()
            {
               private NodeState<V> mNode = init();

               private NodeState<V> init()
               {
                  try
                  {
                     return new NodeState<V>(new Stack<NodeState<V>>(), mRoot);
                  }
                  catch(final NullPointerException eNp)
                  {
                     return null;
                  }
               }

               public boolean hasNext()
               {
                  return mNode != null;
               }

               public V next()
               {
                  try
                  {
                     return mNode.ref().get();
                  }
                  catch(final NullPointerException eNp)
                  {
                     throw new NoSuchElementException();
                  }
                  finally
                  {
                     try
                     {
                        mNode= mNode.next();
                     }
                     catch(final NullPointerException eNp)
                     {
                     }
                  }
               }

               public void remove()
               {
                  throw new UnsupportedOperationException();
               }
            };
         }

         public boolean remove(final Object aO)
         {
            throw new UnsupportedOperationException();
         }

         public boolean removeAll(final Collection<?> aC)
         {
            throw new UnsupportedOperationException();
         }

         public boolean retainAll(final Collection<?> aC)
         {
            throw new UnsupportedOperationException();
         }

         public int size()
         {
            try
            {
               return mRoot.count();
            }
            catch(final Exception e)
            {
               return 0;
            }
         }

         public Object[] toArray()
         {
            int lI= 0;
            final Object[] lRes= new Object[size()];

            for(final V fVal : this)
               lRes[lI++]= fVal;

            return lRes;
         }

         @SuppressWarnings("unchecked")
         public <T> T[] toArray(final T[] aArray)
         {
            T[] lRes= aArray;
            final int lSz= size();

            if( lRes.length < lSz )
            {
               int iI= 0;

               lRes= Arrays.copyOf(aArray, lSz);

               for(final V fVal : this)
                  lRes[iI++]= (T) fVal;

               return lRes;
            }

            System.arraycopy(toArray(), 0, lRes, 0, lSz);
            if( lRes.length > lSz )
               lRes[lSz]= null;

            return lRes;
         }
      };
   }

   /**
    * @see java.util.Map#entrySet()
    */
   @Override
   public Set<Entry<K, V>> entrySet()
   {
      return new Set<Entry<K, V>>()
      {
         private final Set<Entry<K, ValueRef<V>>> mSet = mMap.entrySet();

         public int size()
         {
            return mSet.size();
         }

         public boolean isEmpty()
         {
            return mSet.isEmpty();
         }

         public boolean contains(final Object aO)
         {
            try
            {
               @SuppressWarnings("unchecked")
               final Entry<K, V> lEntry= (Entry<K, V>) aO;

               return mMap.get(lEntry.getKey()).get().equals(lEntry.getValue());
            }
            catch(final Exception e)
            {
               return false;
            }
         }

         public Iterator<Entry<K, V>> iterator()
         {
            return new Iterator<Entry<K, V>>()
            {
               private final Iterator<Entry<K, ValueRef<V>>> mIt = mSet.iterator();

               public boolean hasNext()
               {
                  return mIt.hasNext();
               }

               public Entry<K, V> next()
               {
                  return new Entry<K, V>()
                  {
                     private final Entry<K, ValueRef<V>> mEntry = mIt.next();

                     public K getKey()
                     {
                        return mEntry.getKey();
                     }

                     public V getValue()
                     {
                        return mEntry.getValue().get();
                     }

                     public V setValue(final V aValue)
                     {
                        throw new UnsupportedOperationException();
                     }

                     /**
                      * @see java.lang.Object#toString()
                      */
                     @Override
                     public String toString()
                     {
                        return getKey().toString() + ":" + getValue().toString();
                     }
                  };
               }

               public void remove()
               {
                  throw new UnsupportedOperationException();
               }
            };
         }

         public Object[] toArray()
         {
            int lI= 0;
            final Object[] lRes= new Object[size()];

            for(final Entry<K, V> fEntry : this)
               lRes[lI++]= fEntry;

            return lRes;
         }

         @SuppressWarnings("unchecked")
         public <T> T[] toArray(final T[] aArray)
         {
            T[] lRes= aArray;
            final int lSz= size();

            if( lRes.length < lSz )
            {
               int iI= 0;

               lRes= Arrays.copyOf(aArray, lSz);

               for(final Entry<K, V> fEntry : this)
                  lRes[iI++]= (T) fEntry;

               return lRes;
            }

            System.arraycopy(toArray(), 0, lRes, 0, lSz);
            if( lRes.length > lSz )
               lRes[lSz]= null;

            return lRes;
         }

         public boolean add(final java.util.Map.Entry<K, V> aE)
         {
            throw new UnsupportedOperationException();
         }

         public boolean remove(final Object aO)
         {
            throw new UnsupportedOperationException();
         }

         public boolean containsAll(final Collection<?> aC)
         {
            for(final Object fObj : aC)
               if( !contains(fObj) )
                  return false;

            return true;
         }

         public boolean addAll(final Collection<? extends java.util.Map.Entry<K, V>> aC)
         {
            throw new UnsupportedOperationException();
         }

         public boolean retainAll(final Collection<?> aC)
         {
            throw new UnsupportedOperationException();
         }

         public boolean removeAll(final Collection<?> aC)
         {
            throw new UnsupportedOperationException();
         }

         public void clear()
         {
            throw new UnsupportedOperationException();
         }
      };
   }

   /**
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString()
   {
      String lRes= "";

      try
      {
         lRes= mRoot.makeString();
      }
      catch(final NullPointerException eNp)
      {
      }

      if( lRes.length() > 0 )
         lRes= lRes.substring(2);

      return mMap.toString() + "\n[" + lRes + "]";
   }
}
