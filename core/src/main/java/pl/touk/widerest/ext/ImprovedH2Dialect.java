package pl.touk.widerest.ext;

import org.hibernate.dialect.H2Dialect;

/**
 * Created by mst on 30.07.15.
 */
public class ImprovedH2Dialect extends H2Dialect {
    @Override
    public String getDropSequenceString(String sequenceName) {
        return "drop sequence if exists " + sequenceName;
    }

    @Override
    public boolean dropConstraints() {
        return false;
    }
}
