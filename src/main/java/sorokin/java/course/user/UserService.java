package sorokin.java.course.user;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sorokin.java.course.account.AccountService;
import sorokin.java.course.tools.TransactionHelper;

import java.util.*;

@Component
public class UserService {

    private final AccountService accountService;
    private final TransactionHelper transactionHelper;
    private final SessionFactory sessionFactory;

    @Autowired
    public UserService(AccountService accountService, TransactionHelper transactionHelper, SessionFactory sessionFactory) {
        this.accountService = accountService;
        this.transactionHelper = transactionHelper;
        this.sessionFactory = sessionFactory;
    }

    public User createUser(String login) {

        return transactionHelper.executeInTransactionOrJoin(() -> {
            String normalizedLogin = validateLogin(login);

            User user = new User(normalizedLogin, new ArrayList<>());

            Session session = sessionFactory.getCurrentSession();
            session.persist(user);

            var account = accountService.createAccount(user);

            return user;
        });

    }


    public User removeUser(Long userId) {
        validatePositiveId(userId, "userId");

        return transactionHelper.executeInTransaction(session -> {
            User user = session.get(User.class, userId);
            if (user == null) {
                throw new IllegalArgumentException("No such user: id=%s".formatted(userId));
            }

            session.remove(user);
            return user;
        });
    }


    public User findUserById(Long id) {
        try (Session session = sessionFactory.openSession()) {

            if (id == null || id <= 0) {
                throw new IllegalArgumentException("user id must be > 0");
            }

            User user = session.get(User.class, id);
            if (user == null) {
                throw new IllegalArgumentException("No such user with id=%s".formatted(id));
            }
            return user;
        }
    }


    public List<User> findAll() {

        try (Session session = sessionFactory.openSession()) {

            return session.createQuery("""
                                    SELECT u FROM User u 
                                    JOIN FETCH u.accountList 
                                    """, User.class)
                                    .list();
        }
    }


    private String validateLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("login must not be blank");
        }
        return login.trim();
    }

    private void validatePositiveId(Long userId, String fieldName) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(fieldName + "must be > 0");
        }
    }

}
