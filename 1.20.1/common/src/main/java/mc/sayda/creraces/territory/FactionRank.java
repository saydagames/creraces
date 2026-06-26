package mc.sayda.creraces.territory;

public enum FactionRank {
    MEMBER,
    OFFICER,
    LEADER;

    public boolean isAtLeast(FactionRank required) {
        return this.ordinal() >= required.ordinal();
    }
}
