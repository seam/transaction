package org.jboss.seam.transaction.util;

import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

/**
 * Try really hard to get a UserTransaction without getting caught by the EJB container
 *
 * This is needlessly difficult
 * 
 * This must happen in a bean-managed transaction container per EJB 3.2 spec (JSR 236)
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class UserTransactionUtil {
	
	public javax.transaction.UserTransaction getUserTransaction() throws NamingException {
        InitialContext context = new InitialContext();
        try {
            return (javax.transaction.UserTransaction) context.lookup("java:comp/UserTransaction");
        } catch (NamingException ne) {
            try {
                // Embedded JBoss has no java:comp/UserTransaction
                javax.transaction.UserTransaction ut = (javax.transaction.UserTransaction) context.lookup("UserTransaction");
                ut.getStatus(); // for glassfish, which can return an unusable UT
                return ut;
            } catch (NamingException ne2) {
           		return (UserTransaction) context.lookup("java:jboss/UserTransaction");
            } catch (Exception e) {
                throw ne;
            }
        }
    }
}
