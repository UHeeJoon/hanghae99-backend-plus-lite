package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public UserPoint charge(long amount) {
        PointPolicy.CHARGE.validate(this.point, amount);
        return new UserPoint(this.id, this.point + amount, System.currentTimeMillis());
    }

    public UserPoint use(long amount) {
        PointPolicy.USE.validate(this.point, amount);
        return new UserPoint(this.id, this.point - amount, System.currentTimeMillis());
    }
}
