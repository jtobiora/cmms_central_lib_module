package ng.upperlink.nibss.cmms.enums.makerchecker;

public enum  EntityTypeEmandate {

    BANK("Bank"),
//    PSSP("PSSP"),
    BILLER("Biller"),
    UNKNOWN("UNKNOWN");

    String value;

    EntityTypeEmandate(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static synchronized EntityTypeEmandate find(String userType) {
        try {
            return EntityTypeEmandate.valueOf(userType);
        } catch (Exception e) {
            return findByValue(userType);
        }
    }

    private static EntityTypeEmandate findByValue(String value) {
        EntityTypeEmandate type = null;

        for (EntityTypeEmandate userType : EntityTypeEmandate.values()) {
            if( userType.value.equalsIgnoreCase(value)) {
                type = userType;
                break;
            }
        }

        return  type == null ? UNKNOWN : type;

    }


}
