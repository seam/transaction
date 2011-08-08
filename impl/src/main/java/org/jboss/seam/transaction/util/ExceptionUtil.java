package org.jboss.seam.transaction.util;

import java.lang.reflect.Method;

import org.jboss.seam.transaction.SeamApplicationException;

/**
 * Utility class for dealing with application exceptions
 *
 * @author Stuart Douglas
 */
public class ExceptionUtil {

    private ExceptionUtil() {

    }

    public static boolean exceptionCausesRollback(Exception e) {
        boolean defaultRollback = false;
        if (e instanceof RuntimeException) {
            defaultRollback = true;
        }
        Class<?> exClass = e.getClass();
        if (exClass.isAnnotationPresent(SeamApplicationException.class)) {
            SeamApplicationException sae = exClass.getAnnotation(SeamApplicationException.class);
            return sae.rollback();
        } else if (exClass.isAnnotationPresent(EjbApi.APPLICATION_EXCEPTION)) {
            Object ae = exClass.getAnnotation(EjbApi.APPLICATION_EXCEPTION);
            try {
                Method rollback = EjbApi.APPLICATION_EXCEPTION.getMethod("rollback");
                return (Boolean) rollback.invoke(ae);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return defaultRollback;
    }
}