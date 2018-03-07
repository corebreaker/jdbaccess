/**
 *
 */
package com.corebreaker.dbaccess.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Frédéric Meyer
 * @version 1.0
 */
public class Application
{
   public final static String           RUNTIME;
   public final static File             ROOT;
   public final static File             JARFILE;

   private final static Pattern         PROPERTY_VAR = Pattern.compile("^(.*)\\$\\{([^}]+)\\}(.*)$");

   private static ClassLoader           sLoader      = null;
   private static Set<File>             sDirs        = new HashSet<File>();
   private static Set<URL>              sJars        = new HashSet<URL>();
   private static Map<String, Class<?>> sClasses     = new Hashtable<String, Class<?>>();

   static
   {
      File lJar= null;
      File lPath= null;
      File lRoot= null;
      final URL lUrl= Application.class.getResource(Application.class.getSimpleName() + ".class");

      try
      {
         try
         {
            final JarURLConnection lConn= JarURLConnection.class.cast(lUrl.openConnection());

            lJar= lPath= new File(lConn.getJarFileURL().toURI());
         }
         catch(final IOException eIo)
         {
            eIo.printStackTrace();
            System.exit(1);
         }
         catch(final URISyntaxException eUs)
         {
            eUs.printStackTrace();
            System.exit(2);
         }
         catch(final ClassCastException eCc)
         {
            try
            {
               File lRes= new File(lUrl.toURI());

               for(@SuppressWarnings("unused")
               final String fPart : Application.class.getPackage().getName().split("[.]"))
                  lRes= lRes.getParentFile();

               lPath= lRes.getParentFile();
            }
            catch(final Exception eEx)
            {
               eEx.printStackTrace();
               System.exit(3);
            }
         }
      }
      catch(final Exception eEx)
      {
      }

      if( lPath != null )
      {
         lRoot= lPath.getParentFile();
         JARFILE= lJar;

         final String lHome= System.getProperty("java.home", "");
         String lRuntime= "java";

         if( lHome.length() == 0 )
         {
            for(final String fPath : System.getenv("PATH").split("[" + System.getProperty("path.separator") + "]"))
            {
               final File wDir= new File(fPath);

               if( !wDir.isDirectory() )
                  continue;

               final File[] wRes= wDir.listFiles(new FileFilter()
               {
                  @Override
                  public boolean accept(final File aPathname)
                  {
                     return aPathname.isFile() && aPathname.canExecute() && aPathname.getName().startsWith("java");
                  }
               });

               if( wRes.length > 0 )
               {
                  lRuntime= wRes[0].getAbsolutePath();
                  break;
               }
            }
         }
         else
         {
            final String iSep= System.getProperty("file.separator", "/");

            lRuntime= lHome + iSep + "bin" + iSep + lRuntime;
         }

         RUNTIME= lRuntime;
      }
      else
      {
         RUNTIME= "java";
         JARFILE= null;
      }

      ROOT= (lRoot != null) ? lRoot : new File(".").getAbsoluteFile();
   }

   /**
    * @param aName
    * @return
    * @throws ClassNotFoundException
    */
   private static Class<?> loadClass(final String aName) throws ClassNotFoundException
   {
      Class<?> lRes= Application.sClasses.get(aName);

      if( lRes == null )
      {
         lRes= Application.sLoader.loadClass(aName);
         Application.sClasses.put(aName, lRes);
      }

      return lRes;
   }

   /**
    * @param aLoader
    */
   public static void initProperties(final ClassLoader aLoader)
   {
      final Properties lLocalProps= new Properties();
      final Properties lAppProps= new Properties();

      try
      {
         lAppProps.load(aLoader.getResourceAsStream("META-INF/config.properties"));
      }
      catch(final Exception e)
      {
      }

      try
      {
         lLocalProps.load(aLoader.getResourceAsStream("META-INF/server.properties"));
      }
      catch(final Exception e)
      {
      }

      lAppProps.putAll(lLocalProps);

      String lDir= lAppProps.getProperty("webtagger.datadir", "${home}/.webtagger");
      final String lRoot= Application.ROOT.getPath();
      final String lHome= System.getProperty("user.home", lRoot);
      final Hashtable<String, String> lVars= new Hashtable<String, String>();

      lDir= lDir.replace("${root}", lRoot);
      lDir= lDir.replace("${home}", lHome);

      lAppProps.setProperty("webtagger.datadir", lDir);

      lVars.put("root", lRoot);
      lVars.put("home", lHome);

      for(final Object fName : Collections.list(lAppProps.propertyNames()))
      {
         final String wName= fName.toString();

         lAppProps.setProperty(wName, Application.replaceVars(lVars, lAppProps, wName));
      }

      System.getProperties().putAll(lAppProps);
   }

   /**
    * @param aVars
    * @param aProps
    * @param aName
    * @return
    */
   private static String replaceVars(final Hashtable<String, String> aVars, final Properties aProps, final String aName)
   {
      String lRes= aVars.get(aName);

      if( lRes == null)
      {
         lRes= aProps.getProperty(aName);

         Matcher lMatcher= Application.PROPERTY_VAR.matcher(lRes);

         while( lMatcher.matches() )
         {
            lRes= lMatcher.replaceFirst("$1" + Application.replaceVars(aVars, aProps, lMatcher.group(2)).replace("\\", "\\\\") + "$3");
            lMatcher= Application.PROPERTY_VAR.matcher(lRes);
         }

         aVars.put(aName, lRes);
         aProps.setProperty(aName, lRes);
      }

      return lRes;
   }

   /**
    * @param aDir
    * @param aSubdirs
    */
   private static void loadDir(final File aDir, final boolean aSubdirs)
   {
      for(final File fJar : aDir.listFiles(new FilenameFilter()
      {

         @Override
         public boolean accept(final File aDir, final String aName)
         {
            return (new File(aDir, aName).isDirectory()) || aName.endsWith(".jar");
         }
      }))
      {
         if( fJar.isDirectory() )
         {
            if( aSubdirs )
               Application.loadDir(fJar, true);
         }
         else
            try
            {
               Application.sJars.add(fJar.toURI().toURL());
            }
            catch(final MalformedURLException e)
            {
            }
      }
   }

   /**
    * @param aLoader
    */
   public static void init(final ClassLoader aLoader)
   {
      if( Application.sLoader != null )
         return;

      Application.sLoader= aLoader;

      final File lLibdir= new File(Application.ROOT, "lib");

      Application.initProperties(aLoader);

      try
      {
         if( Application.JARFILE == null )
            Application.sJars.add(Application.ROOT.toURI().toURL());
         else
            Application.sJars.add(Application.JARFILE.toURI().toURL());
      }
      catch(final MalformedURLException e)
      {
      }

      if( lLibdir.exists() && lLibdir.isDirectory() )
         Application.loadDir(lLibdir, true);

      for(final File fDir : Application.sDirs)
         Application.loadDir(fDir, false);

      Method lAddURL= null;

      try
      {
         final Method lMeth= URLClassLoader.class.getDeclaredMethod("addURL", URL.class);

         lMeth.setAccessible(true);
         lAddURL= lMeth;

         Application.sLoader= aLoader;
      }
      catch(final Exception eEx)
      {
      }

      if( lAddURL != null )
         for(final URL fJar : Application.sJars)
            try
            {
               lAddURL.invoke(aLoader, fJar);
            }
            catch(final IllegalArgumentException eIa)
            {
            }
            catch(final IllegalAccessException eIa)
            {
            }
            catch(final InvocationTargetException eIt)
            {
            }
      else
         Application.sLoader= new URLClassLoader(Application.sJars.toArray(new URL[0]), aLoader)
         {
            /**
             * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
             */
            @Override
            protected synchronized Class<?> loadClass(final String aName, final boolean aResolve)
               throws ClassNotFoundException
            {
               Class<?> lRes= null;

               try
               {
                  lRes= findClass(aName);
               }
               catch(final Exception eEx)
               {
               }

               return (lRes != null) ? lRes : super.loadClass(aName, aResolve);
            }
         };
   }

   /**
    * @param aDir
    */
   public static void addDir(final String aDir)
   {
      try
      {
         final File lDir= new File(Application.ROOT, aDir).getCanonicalFile().getAbsoluteFile();

         if( lDir.isDirectory() )
            Application.sDirs.add(lDir);
      }
      catch(final Exception eEx)
      {
         eEx.printStackTrace();
      }
   }

   /**
    * @param aMainClass
    * @param aArgs
    */
   public static void launch(final Class<?> aMainClass, final String[] aArgs)
   {
      Application.init(aMainClass.getClassLoader());

      try
      {
         final Class<?> lClass= Application.loadClass(aMainClass.getPackage().getName() + ".Launcher");
         final Method lMain= lClass.getMethod("execute", aArgs.getClass());

         lMain.invoke(null, (Object) aArgs);
      }
      catch(final Throwable eThrow)
      {
         eThrow.printStackTrace();

         System.exit(1);
      }
   }

   /**
    * @param aMainClass
    * @param aName
    * @param aReturnType
    * @param aArgsTypes
    * @param aParameters
    * @return
    */
   public static <T> T call(final Class<?> aMainClass,
                            final String aName,
                            final Class<T> aReturnType,
                            final Class<?>[] aArgsTypes,
                            final Object... aParameters)
   {
      Application.init(aMainClass.getClassLoader());

      T lRes= null;

      try
      {
         final Class<?> lClass= Application.loadClass(aMainClass.getPackage().getName() + ".Launcher");
         final Method lMain= lClass.getMethod(aName, aArgsTypes);

         lRes= aReturnType.cast(lMain.invoke(null, aParameters));
      }
      catch(final Throwable eThrow)
      {
         eThrow.printStackTrace();
      }

      return lRes;
   }

   /**
    * @param aMainClass
    * @return
    */
   public static <T> T newInstance(final Class<?> aMainClass)
   {
      Application.init(aMainClass.getClassLoader());

      T lRes= null;

      try
      {
         @SuppressWarnings("unchecked")
         final Class<T> lClass= (Class<T>)Application.loadClass(aMainClass.getPackage().getName() + ".Launcher");

         lRes= lClass.newInstance();
      }
      catch(final Throwable eThrow)
      {
         eThrow.printStackTrace();
      }

      return lRes;
   }
}
