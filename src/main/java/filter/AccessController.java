package filter;

import java.util.HashSet;
import java.util.Set;

public class AccessController {

    private final Set<String> blacklist =
            new HashSet<>();

    public AccessController(){

        blacklist.add("www.blocked.com");
    }

    public boolean isAllowed(String host){

        return !blacklist.contains(host);
    }
}
