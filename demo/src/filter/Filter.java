package filter;

import user.User;

import java.util.function.Predicate;

public interface Filter extends Predicate<User> {

    boolean test(User user);
}
