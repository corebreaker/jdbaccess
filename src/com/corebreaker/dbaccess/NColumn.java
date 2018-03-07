/**
 *
 */
package com.corebreaker.dbaccess;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Frédéric Meyer
 * @version 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NColumn
{
   String value() default "";
   Class<? extends DbObject> entity() default DbObject.class;
}
