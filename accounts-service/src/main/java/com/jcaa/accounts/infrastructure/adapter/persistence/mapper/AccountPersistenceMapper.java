package com.jcaa.accounts.infrastructure.adapter.persistence.mapper;

import com.jcaa.accounts.domain.enums.AccountStatus;
import com.jcaa.accounts.domain.model.AccountModel;
import com.jcaa.accounts.domain.valueobject.AccountId;
import com.jcaa.accounts.domain.valueobject.AccountUserId;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AccountPersistenceMapper {

  private static final String COLUMN_ID = "id";
  private static final String COLUMN_USER_ID = "user_id";
  private static final String COLUMN_STATUS = "status";
  private static final String COLUMN_CREATED_AT = "created_at";

  public AccountModel fromResultSetToModel(final ResultSet resultSet) throws SQLException {
    return new AccountModel(
        new AccountId(resultSet.getString(COLUMN_ID)),
        new AccountUserId(resultSet.getString(COLUMN_USER_ID)),
        AccountStatus.valueOf(resultSet.getString(COLUMN_STATUS)),
        resultSet.getTimestamp(COLUMN_CREATED_AT).toInstant());
  }
}
