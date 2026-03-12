package sorokin.java.course.operations.commands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sorokin.java.course.console.ConsoleInput;
import sorokin.java.course.operations.ConsoleOperationType;
import sorokin.java.course.operations.OperationCommand;
import sorokin.java.course.user.UserService;

@Component
public class DeleteUserCommand implements OperationCommand {

    private final UserService userService;
    private final ConsoleInput consoleInput;

    @Autowired
    public DeleteUserCommand(UserService userService, ConsoleInput consoleInput) {
        this.userService = userService;
        this.consoleInput = consoleInput;
    }

    @Override
    public void execute() {
        Long userId = consoleInput.readPositiveLong("Enter user id to delete:", "user id");
        var userDelete = userService.removeUser(userId);
        System.out.println("User: " + userDelete + " deleted");
    }

    @Override
    public ConsoleOperationType getOperationType() {
        return ConsoleOperationType.USER_DELETE;
    }
}
