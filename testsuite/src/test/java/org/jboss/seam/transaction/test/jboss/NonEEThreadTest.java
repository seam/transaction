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

package org.jboss.seam.transaction.test.jboss;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.enterprise.inject.spi.BeanManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.seam.transaction.SeamTransaction;
import org.jboss.seam.transaction.literal.DefaultTransactionLiteral;
import org.jboss.seam.transaction.test.util.JBossASTestUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.solder.beanManager.BeanManagerUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="http://community.jboss.org/people/LightGuard">Jason Porter</a>
 */
@RunWith(Arquillian.class)
public class NonEEThreadTest {
    @Deployment
    public static Archive<?> createTestArchive() {
        WebArchive war = JBossASTestUtils.createTestArchive();
        war.addClasses(NonEEThreadTest.class, NonEEThread.class);
        return war;
    }

    @Test
    public void assertNonEEThreadSeamTransactionIsNonNull(final BeanManager bm) throws SystemException, RollbackException, HeuristicRollbackException, HeuristicMixedException, InterruptedException {
        final CountDownLatch startLatch = new CountDownLatch(1);
        final NonEEThread testThread = new NonEEThread(bm, startLatch);

        Thread worker = new Thread(testThread);

        try {
            worker.start();
            startLatch.await(5L, TimeUnit.SECONDS);
            assertThat(testThread.isTxObtained(), is(true));
            assertThat(testThread.isTxStarted(), is(true));
            assertThat(testThread.isTxEnded(), is(true));
        } finally {
            if (worker.isAlive())
                worker.interrupt();
        }


    }

    public static class NonEEThread implements Runnable {
        private SeamTransaction stx;

        private final BeanManager bm;
        private final CountDownLatch begin;

        private boolean txObtained = false;
        private boolean txStarted = false;
        private boolean txEnded = false;

        public NonEEThread(final BeanManager bm, CountDownLatch start) {
            this.bm = bm;
            this.begin = start;
        }

        @Override
        public void run() {
            stx = BeanManagerUtils.getContextualInstance(bm, SeamTransaction.class, DefaultTransactionLiteral.INSTANCE);
            this.txObtained = true;

            try {
                startTransaction();
                endTransaction();
            } catch (SystemException e) {
                throw new RuntimeException(e);
            } catch (NotSupportedException e) {
                throw new RuntimeException(e);
            } finally {
                this.begin.countDown();
            }
        }

        void startTransaction() throws SystemException, NotSupportedException {
            this.stx.begin();
            this.txStarted = true;
        }

        void endTransaction() {
            try {
                this.stx.commit();
                this.txEnded = true;
            } catch (RollbackException e) {
                throw new RuntimeException(e);
            } catch (HeuristicMixedException e) {
                throw new RuntimeException(e);
            } catch (HeuristicRollbackException e) {
                throw new RuntimeException(e);
            } catch (SystemException e) {
                throw new RuntimeException(e);
            }
            this.begin.countDown();
        }

        public boolean isTxObtained() {
            return txObtained;
        }

        public boolean isTxStarted() {
            return txStarted;
        }

        public boolean isTxEnded() {
            return txEnded;
        }
    }
}
