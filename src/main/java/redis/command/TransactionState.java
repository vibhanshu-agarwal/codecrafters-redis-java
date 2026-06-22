package redis.command;

public class TransactionState {
    private boolean inTransaction = false;

    public boolean isInTransaction() {
        return inTransaction;
    }

    public void setInTransaction(boolean inTransaction) {
        this.inTransaction = inTransaction;
    }
}
