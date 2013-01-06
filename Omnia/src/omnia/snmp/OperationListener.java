package omnia.snmp;

/**
 * A listener for the SnmpOperation.
 *
 * @versionElement 1.0
 *
 * @author Marcus Hoff <marcus.hoff@ring2.dk>
 */
public interface OperationListener {

    /**
     * Called when the operation is finished.
     *
     * @param operation the operation, which has finished.
     */
    public void onStop(SnmpOperation operation);
}
