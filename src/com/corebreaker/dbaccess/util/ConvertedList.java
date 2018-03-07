/**
 *
 */
package com.corebreaker.dbaccess.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Frédéric Meyer
 * @version 1.0
 */
public final class ConvertedList<E> implements List<E>
{
   /**
    * @author Frédéric Meyer
    *
    */
   private class ConvertedListIterator implements ListIterator<E>
   {
      private final ListIterator<?> mIt;

      ConvertedListIterator(final ListIterator<?> aIt)
      {
         super();

         mIt= aIt;
      }

      public boolean hasNext()
      {
         return mIt.hasNext();
      }

      public E next()
      {
         return mClass.cast(mIt.next());
      }

      public void remove()
      {
         throw new UnsupportedOperationException();
      }

      public boolean hasPrevious()
      {
         return mIt.hasPrevious();
      }

      public E previous()
      {
         return mClass.cast(mIt.previous());
      }

      public int nextIndex()
      {
         return mIt.nextIndex();
      }

      public int previousIndex()
      {
         return mIt.previousIndex();
      }

      public void set(final E aE)
      {
         throw new UnsupportedOperationException();
      }

      public void add(final E aE)
      {
         throw new UnsupportedOperationException();
      }
   }

   private final List<?> mList;
   private final Class<E>                mClass;

   /**
    * @param aClass
    * @param aSet
    */
   public ConvertedList(final Class<E> aClass, final Object aList)
   {
      super();

      mList= List.class.cast(aList);
      mClass= aClass;
   }

   /**
    * @see java.util.List#iterator()
    */
   public Iterator<E> iterator()
   {
      return new ConvertedListIterator(mList.listIterator());
   }

   /**
    * @see java.util.List#listIterator()
    */
   public ListIterator<E> listIterator()
   {
      return new ConvertedListIterator(mList.listIterator());
   }

   /**
    * @see java.util.List#listIterator(int)
    */
   public ListIterator<E> listIterator(final int aIndex)
   {
      return new ConvertedListIterator(mList.listIterator(aIndex));
   }

   /**
    * @see java.util.List#subList(int, int)
    */
   public List<E> subList(final int aFromIndex, final int aToIndex)
   {
      return new ConvertedList<E>(mClass, mList.subList(aFromIndex, aToIndex));
   }

   /**
    * @see java.util.List#clear()
    */
   public void clear()
   {
      throw new UnsupportedOperationException();
   }

   /**
    * @see java.util.List#size()
    */
   public int size()
   {
      return mList.size();
   }

   /**
    * @see java.util.List#isEmpty()
    */
   public boolean isEmpty()
   {
      return mList.isEmpty();
   }

   /**
    * @see java.util.List#contains(java.lang.Object)
    */
   public boolean contains(final Object aO)
   {
      return mList.contains(aO);
   }

   /**
    * @see java.util.List#toArray()
    */
   public Object[] toArray()
   {
      return mList.toArray();
   }

   /**
    * @see java.util.List#toArray(T[])
    */
   public <T> T[] toArray(final T[] aA)
   {
      return mList.toArray(aA);
   }

   /**
    * @see java.util.List#add(java.lang.Object)
    */
   public boolean add(final E aE)
   {
      throw new UnsupportedOperationException();
   }

   /**
    * @see java.util.List#remove(java.lang.Object)
    */
   public boolean remove(final Object aO)
   {
      throw new UnsupportedOperationException();
   }

   /**
    * @see java.util.List#containsAll(java.util.Collection)
    */
   public boolean containsAll(final Collection<?> aC)
   {
      return mList.containsAll(aC);
   }

   /**
    * @see java.util.List#addAll(java.util.Collection)
    */
   public boolean addAll(final Collection<? extends E> aC)
   {
      throw new UnsupportedOperationException();
   }

   /**
    * @see java.util.List#addAll(int, java.util.Collection)
    */
   public boolean addAll(final int aIndex, final Collection<? extends E> aC)
   {
      throw new UnsupportedOperationException();
   }

   /**
    * @see java.util.List#removeAll(java.util.Collection)
    */
   public boolean removeAll(final Collection<?> aC)
   {
      throw new UnsupportedOperationException();
   }

   /**
    * @see java.util.List#retainAll(java.util.Collection)
    */
   public boolean retainAll(final Collection<?> aC)
   {
      throw new UnsupportedOperationException();
   }

   /**
    * @see java.util.List#get(int)
    */
   public E get(final int aIndex)
   {
      return mClass.cast(mList.get(aIndex));
   }

   /**
    * @see java.util.List#set(int, java.lang.Object)
    */
   public E set(final int aIndex, final E aElement)
   {
      throw new UnsupportedOperationException();
   }

   /**
    * @see java.util.List#add(int, java.lang.Object)
    */
   public void add(final int aIndex, final E aElement)
   {
      throw new UnsupportedOperationException();
   }

   /**
    * @see java.util.List#remove(int)
    */
   public E remove(final int aIndex)
   {
      throw new UnsupportedOperationException();
   }

   /**
    * @see java.util.List#indexOf(java.lang.Object)
    */
   public int indexOf(final Object aO)
   {
      return mList.indexOf(aO);
   }

   /**
    * @see java.util.List#lastIndexOf(java.lang.Object)
    */
   public int lastIndexOf(final Object aO)
   {
      return mList.lastIndexOf(aO);
   }
}
