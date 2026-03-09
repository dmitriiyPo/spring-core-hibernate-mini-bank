package sorokin.java.course.account;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sorokin.java.course.tools.TransactionHelper;
import sorokin.java.course.user.User;

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


    public void withdraw(Long fromAccountId, Integer amount) {
        validatePositiveId(fromAccountId, "account id");
        validatePositiveAmount(amount);

        transactionHelper.executeInTransaction(session -> {

            Account account = session.get(Account.class, fromAccountId);
            if (account == null) {
                throw new IllegalArgumentException("No such account: id=%s".formatted(fromAccountId));
            }

            if (amount > account.getMoneyAmount()) {
                throw new IllegalArgumentException(
                    "insufficient funds on account id=%s, moneyAmount=%s, attempted withdraw=%s"
                            .formatted(account.getId(), account.getMoneyAmount(), amount)
                );
            }

            account.setMoneyAmount(account.getMoneyAmount() - amount);
        });
    }


    public void deposit(Long toAccountId, Integer amount) {
        validatePositiveId(toAccountId, "account id");
        validatePositiveAmount(amount);

        transactionHelper.executeInTransaction(session -> {

           Account account = session.get(Account.class, toAccountId);
           if (account == null) {
               throw new IllegalArgumentException("No such account: id=%s".formatted(toAccountId));
           }

           account.setMoneyAmount(account.getMoneyAmount() + amount);
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


            var newAmount = accountToTransferMoney.getMoneyAmount() + accountToClose.getMoneyAmount();
            accountToTransferMoney.setMoneyAmount(newAmount);

            session.remove(accountToClose);

            return accountToClose;
        });
    }


    public void transfer(Long fromAccountId, Long toAccountId, Integer amount) {
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

            if (amount > fromAccount.getMoneyAmount()) {
                throw new IllegalArgumentException(
                        "insufficient funds on account id=%s, moneyAmount=%s, attempted transfer=%s"
                                .formatted(fromAccount.getId(), fromAccount.getMoneyAmount(), amount)
                );
            }

            fromAccount.setMoneyAmount(fromAccount.getMoneyAmount() - amount);

            int amountToTransfer = toAccount.getUser().getId().equals(fromAccount.getUser().getId())
                    ? amount
                    : (int) Math.round(amount * (1 - accountProperties.getTransferCommission()));

            toAccount.setMoneyAmount(toAccount.getMoneyAmount() + amountToTransfer);

        });
    }


    private void validatePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be > 0");
        }
    }

    private void validatePositiveAmount(Integer amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
    }

}
