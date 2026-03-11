package sorokin.java.course.account;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sorokin.java.course.tools.TransactionHelper;
import sorokin.java.course.user.User;

import java.math.BigDecimal;
import java.util.List;


@Component
public class AccountService {

    private final AccountProperties accountProperties;
    private final TransactionHelper transactionHelper;
    private final SessionFactory sessionFactory;


    @Autowired
    public AccountService(AccountProperties accountProperties, TransactionHelper transactionHelper, SessionFactory sessionFactory) {
        this.accountProperties = accountProperties;
        this.transactionHelper = transactionHelper;
        this.sessionFactory = sessionFactory;
    }

    public Account createAccount(User user) {

        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }

        return transactionHelper.executeInTransactionOrJoin(() -> {
            Session session = sessionFactory.getCurrentSession();
            Account newAccount = new Account(accountProperties.getDefaultAmount());
            newAccount.setUser(user);
            user.getAccountList().add(newAccount);
            session.persist(newAccount);
            return newAccount;
        });
    }


    public void withdraw(Long fromAccountId, BigDecimal amount) {
        validatePositiveId(fromAccountId, "account id");
        validatePositiveAmount(amount);

        transactionHelper.executeInTransaction(session -> {

            Account account = session.get(Account.class, fromAccountId);
            if (account == null) {
                throw new IllegalArgumentException("No such account: id=%s".formatted(fromAccountId));
            }

            if (amount.compareTo(account.getMoneyAmount()) > 0) {
                throw new IllegalArgumentException(
                    "insufficient funds on account id=%s, moneyAmount=%s, attempted withdraw=%s"
                            .formatted(account.getId(), account.getMoneyAmount(), amount)
                );
            }

            BigDecimal newBalance = account.getMoneyAmount().subtract(amount);
            account.setMoneyAmount(newBalance);
            //account.setMoneyAmount(account.getMoneyAmount().subtract(amount));

        });
    }


    public void deposit(Long toAccountId, BigDecimal amount) {
        validatePositiveId(toAccountId, "account id");
        validatePositiveAmount(amount);

        transactionHelper.executeInTransaction(session -> {

           Account account = session.get(Account.class, toAccountId);
           if (account == null) {
               throw new IllegalArgumentException("No such account: id=%s".formatted(toAccountId));
           }

            BigDecimal newBalance = account.getMoneyAmount().add(amount);
            account.setMoneyAmount(newBalance);
            //account.setMoneyAmount(account.getMoneyAmount().add(amount));
        });
    }


    public List<Account> getUserAccounts(Long userId, Session session) {
        User user = session.get(User.class, userId);
        return user.getAccountList();
    }


    public Account closeAccount(Long accountId) {
        validatePositiveId(accountId, "account id");

        return transactionHelper.executeInTransaction(session -> {

            Account accountToClose = session.get(Account.class, accountId);
            if (accountToClose == null) {
                throw new IllegalArgumentException("No such account: id=%s".formatted(accountId));
            }

            var user = accountToClose.getUser();
            var userId = user.getId();
            var userAccount = getUserAccounts(userId, session);

            if (userAccount.size() == 1) {
                throw new IllegalStateException("Can't close the only one account");
            }

            user.getAccountList().remove(accountToClose);

            Account accountToTransferMoney = userAccount.stream()
                       .filter(ac -> !(ac.getId().equals(accountId)))
                        .findFirst()
                        .orElseThrow();


            accountToTransferMoney.setMoneyAmount(accountToTransferMoney.getMoneyAmount()
                                                .add(accountToClose.getMoneyAmount()));

            session.remove(accountToClose);

            return accountToClose;
        });
    }


    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        validatePositiveId(fromAccountId, "source account id");
        validatePositiveId(toAccountId, "target account id");
        validatePositiveAmount(amount);

        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("source and target account id must be different");
        }

        transactionHelper.executeInTransaction(session -> {

            Account fromAccount = session.get(Account.class, fromAccountId);
            if (fromAccount == null) {
                throw new IllegalArgumentException("No such account: id=%s".formatted(fromAccountId));
            }

            Account toAccount = session.get(Account.class, toAccountId);
            if (toAccount == null) {
                throw new IllegalArgumentException("No such account: id=%s".formatted(toAccountId));
            }

            if (amount.compareTo(fromAccount.getMoneyAmount()) > 0 ) {
                throw new IllegalArgumentException(
                        "insufficient funds on account id=%s, moneyAmount=%s, attempted transfer=%s"
                                .formatted(fromAccount.getId(), fromAccount.getMoneyAmount(), amount)
                );
            }

            fromAccount.setMoneyAmount(fromAccount.getMoneyAmount().subtract(amount));

            BigDecimal amountToTransfer = toAccount.getUser().getId().equals(fromAccount.getUser().getId())
                    ? amount
                    : amount.multiply(BigDecimal.ONE.subtract(accountProperties.getTransferCommission()));

            toAccount.setMoneyAmount(toAccount.getMoneyAmount().add(amountToTransfer));

        });
    }


    private void validatePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be > 0");
        }
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
    }

}
