package org.jboss.seam.transaction;

import java.io.IOException;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;

import org.jboss.solder.exception.control.ExceptionToCatch;
import org.jboss.solder.logging.Logger;

/**
 * This filter rolls back the current transaction if it is still active.
 *
 * @author Nicklas Karlsson
 * @author <a href="http://community.jboss.org/people/lightguard">Jason Porter</a>
 */
@WebFilter(value = "/*")
public class TransactionFilter implements Filter {
    private final Logger log = Logger.getLogger(TransactionManagerSynchronizations.class);

    @Inject
    @DefaultTransaction
    private SeamTransaction tx;

    @Inject
    Event<ExceptionToCatch> txException;

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            if (this.tx.isActiveOrMarkedRollback()) {
                this.log.warn("Transaction was already started before the TransactionFilter");
            } else {
                this.tx.begin();
            }
            chain.doFilter(request, response);
        } catch (SystemException se) {
            this.log.warn("Error starting the transaction, or checking status", se);
            this.txException.fire(new ExceptionToCatch(se));
        } catch (NotSupportedException e) {
            this.log.warn("Error starting the transaction", e);
            this.txException.fire(new ExceptionToCatch(e));
        } finally {
            rollbackTransactionIfActive();
        }
    }

    private void rollbackTransactionIfActive() {
        try {
            if (this.tx.isActive() && !this.tx.isRolledBackOrMarkedRollback()) {
                this.tx.commit();
            } else if (tx.getStatus() != Status.STATUS_UNKNOWN || tx.getStatus() != Status.STATUS_NO_TRANSACTION) {
                tx.rollback();
            }
        } catch (SystemException e) {
            this.log.warn("Error rolling back the transaction", e);
            this.txException.fire(new ExceptionToCatch(e));
        } catch (HeuristicRollbackException e) {
            this.log.warn("Error committing the transaction", e);
            this.txException.fire(new ExceptionToCatch(e));
        } catch (RollbackException e) {
            this.log.warn("Error committing the transaction", e);
            this.txException.fire(new ExceptionToCatch(e));
        } catch (HeuristicMixedException e) {
            this.log.warn("Error committing the transaction", e);
            this.txException.fire(new ExceptionToCatch(e));
        }
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
    }
}
