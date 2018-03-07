/**
 *
 */
package com.corebreaker.dbaccess.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Permet d'éviter des attentes actives en "synchronisant" des bouts de code entre threads.
 * Un thread peut être mis en sommeil avec await() et un autre thread peut le réveiller avec signal().
 *
 * @author Frédéric Meyer
 * @version 1.0
 */
public class Notifier
{
   private final ReentrantLock mNotifier   = new ReentrantLock();
   private final Condition     mCond       = mNotifier.newCondition();
   private final AtomicBoolean mNeedNotify = new AtomicBoolean();
   private final AtomicBoolean mNeedWait   = new AtomicBoolean(true);

   /**
    *
    */
   public Notifier()
   {
   }

   /**
    *
    */
   public void await()
   {
      lock();

      if( mNeedWait.getAndSet(true) )
      {
         mNeedNotify.set(true);

         try
         {
            mCond.await();
         }
         catch(final InterruptedException e)
         {
         }

         mNeedNotify.set(false);
      }

      unlock();
   }

   /**
    * @param aTime
    * @param aUnit
    */
   public void await(final long aTime, final TimeUnit aUnit)
   {
      lock();

      if( mNeedWait.getAndSet(true) )
      {
         mNeedNotify.set(true);

         try
         {
            mCond.await(aTime, aUnit);
         }
         catch(final InterruptedException e)
         {
         }

         mNeedNotify.set(false);
      }

      unlock();
   }

   /**
    *
    */
   public void signal()
   {
      lock();

      if( mNeedNotify.get() )
         mCond.signal();
      else
         mNeedWait.set(false);

      unlock();
   }

   /**
    *
    */
   public void signalAll()
   {
      lock();

      if( mNeedNotify.get() )
         mCond.signalAll();
      else
         mNeedWait.set(false);

      unlock();
   }

   /**
    * s
    */
   public void lock()
   {
      mNotifier.lock();
   }

   /**
    *
    */
   public void unlock()
   {
      mNotifier.unlock();
   }
}
