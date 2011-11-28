package org.jboss.seam.transaction;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

/**
 * This filter rolls back the current transaction if it is still active. 
 * 
 * @author Nicklas Karlsson
 *
 */
@WebFilter(value = "/*")
public class RollbackFilter implements Filter
{
    @Inject
    private UserTransaction userTransaction;

    @Override
    public void destroy()
    {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        try
        {
            rollbackTransactionIfActive();
            chain.doFilter(request, response);
        }
        finally
        {
            rollbackTransactionIfActive();
        }
    }

    private void rollbackTransactionIfActive()
    {
        try
        {
            if (userTransaction.getStatus() == Status.STATUS_ACTIVE)
            {
                userTransaction.rollback();
            }
        }
        catch (SystemException e)
        {
            // so not?
        }
    }

    @Override
    public void init(FilterConfig config) throws ServletException
    {
    }

}

