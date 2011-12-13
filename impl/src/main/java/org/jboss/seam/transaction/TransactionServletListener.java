/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.seam.transaction;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;

import org.jboss.solder.exception.control.ExceptionToCatch;
import org.jboss.solder.logging.Logger;

/**
 * Listener to begin / commit / rollback a transaction around each request.
 * @author <a href="http://community.jboss.org/people/LightGuard">Jason Porter</a>
 */
@WebListener
public class TransactionServletListener implements ServletRequestListener {

    /**
     * context-param to disable the listener.
     */
    public static String DISABLE_LISTENER_PARAM = "org.jboss.seam.transaction.disableListener";

    private final Logger log = Logger.getLogger(TransactionServletListener.class);

    @Inject
    @DefaultTransaction
    private SeamTransaction tx;

    @Inject
    Event<ExceptionToCatch> txException;

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        final String listenerDisabledParam = sre.getServletContext().getInitParameter(DISABLE_LISTENER_PARAM);
        if (listenerDisabledParam != null && "true".equals(listenerDisabledParam.trim().toLowerCase())) {
            return;
        }

        try {
            switch (this.tx.getStatus()) {
                case Status.STATUS_ACTIVE:
                    this.log.debugf("Committing a transaction for request %s", sre.getServletRequest());
                    tx.commit();
                    break;
                case Status.STATUS_MARKED_ROLLBACK:
                case Status.STATUS_PREPARED:
                case Status.STATUS_PREPARING:
                    this.log.debugf("Rolling back a transaction for request %s", sre.getServletRequest());
                    tx.rollback();
                    break;
                case Status.STATUS_COMMITTED:
                case Status.STATUS_COMMITTING:
                case Status.STATUS_ROLLING_BACK:
                case Status.STATUS_UNKNOWN:
                case Status.STATUS_ROLLEDBACK:
                case Status.STATUS_NO_TRANSACTION:
                    break;
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
    public void requestInitialized(ServletRequestEvent sre) {
        final String listenerDisabledParam = sre.getServletContext().getInitParameter(DISABLE_LISTENER_PARAM);
        if (listenerDisabledParam != null && "true".equals(listenerDisabledParam.trim().toLowerCase())) {
            return;
        }

        try {
            if (this.tx.getStatus() == Status.STATUS_ACTIVE) {
                this.log.warn("Transaction was already started before the listener");
            } else {
                this.log.debugf("Beginning transaction for request %s", sre.getServletRequest());
                this.tx.begin();
            }
        } catch (SystemException se) {
            this.log.warn("Error starting the transaction, or checking status", se);
            this.txException.fire(new ExceptionToCatch(se));
        } catch (NotSupportedException e) {
            this.log.warn("Error starting the transaction", e);
            this.txException.fire(new ExceptionToCatch(e));
        }
    }
}
