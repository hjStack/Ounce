package ounce.market.demo.common.Exception;

public class MemberNotFoundException extends RuntimeException{

    public MemberNotFoundException(String email) {
        super(email);
    }
}
