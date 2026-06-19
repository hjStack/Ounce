package ounce.market.demo.member.exception;

public class InsufficientPointException extends RuntimeException {

    public InsufficientPointException(Long memberId) {
        super("포인트가 부족합니다. memberId=" + memberId);
    }
}
