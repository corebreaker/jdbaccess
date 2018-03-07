package com.sun.legacy.misc;

public class ReflectUtil
{
    public static <T> T newInstance(final Class<?> cls, final Class<T> to)
        throws IllegalAccessException, InstantiationException
    {
        return to.cast(cls.newInstance());
    }

    public static Class<?> forName(final String name) throws ClassNotFoundException
    {
        return Class.forName(name);
    }
}
