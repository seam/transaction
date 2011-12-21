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

import javax.inject.Inject;
import javax.transaction.Status;
import javax.transaction.SystemException;

import org.jboss.solder.exception.control.CaughtException;
import org.jboss.solder.exception.control.Handles;
import org.jboss.solder.exception.control.HandlesExceptions;
import org.jboss.solder.exception.control.Precedence;
import org.jboss.solder.exception.control.TraversalMode;
import org.jboss.solder.logging.Logger;

/**
 * Very simple exception handler to mark the transaction for rollback.
 * @author <a href="http://community.jboss.org/people/LightGuard">Jason Porter</a>
 */
@HandlesExceptions
public class SimpleTransactionExceptionHandler {
    @Inject
    @DefaultTransaction
    private SeamTransaction tx;

    private final Logger log = Logger.getLogger(SimpleTransactionExceptionHandler.class);

    public void markTransactionRollback(@Handles(precedence = Precedence.BUILT_IN, during = TraversalMode.BREADTH_FIRST)
                                        CaughtException<Throwable> t) {
        // Any exception that occurs is going to mark the transaction for rollback.
        try {
            switch (this.tx.getStatus()) {
                case Status.STATUS_ACTIVE:
                case Status.STATUS_COMMITTED:
                case Status.STATUS_COMMITTING:
                case Status.STATUS_MARKED_ROLLBACK:
                case Status.STATUS_PREPARED:
                case Status.STATUS_PREPARING:
                    this.tx.setRollbackOnly();
                    break;
                case Status.STATUS_ROLLING_BACK:
                case Status.STATUS_UNKNOWN:
                case Status.STATUS_ROLLEDBACK:
                case Status.STATUS_NO_TRANSACTION:
                    break;
            }
        } catch (SystemException e) {
            log.warn("Could not set transaction to rollback", e);
        }
        t.rethrow();
    }
}
