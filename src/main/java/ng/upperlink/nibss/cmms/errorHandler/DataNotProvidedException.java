package ng.upperlink.nibss.cmms.errorHandler;

public class DataNotProvidedException extends Exception{
    private static final long serialVersionUID = 1L;

    public DataNotProvidedException(String message) {
        super(message);
    }
}
