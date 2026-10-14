package com.jcaa.accounts.infrastructure.adapter.persistence.repository;

import com.jcaa.accounts.application.port.out.GetAccountByUserIdPort;
import com.jcaa.accounts.application.port.out.SaveAccountPort;
import com.jcaa.accounts.domain.exception.AccountAlreadyExistsException;
import com.jcaa.accounts.domain.model.AccountModel;
import com.jcaa.accounts.domain.valueobject.AccountUserId;
import com.jcaa.accounts.infrastructure.adapter.persistence.exception.PersistenceException;
import com.jcaa.accounts.infrastructure.adapter.persistence.mapper.AccountPersistenceMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.Optional;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccountRepositoryMySQL implements SaveAccountPort, GetAccountByUserIdPort {

  private static final String SQL_INSERT =
      "INSERT INTO accounts (id, user_id, status, created_at) VALUES (?, ?, ?, ?)";

  private static final String SQL_SELECT_BY_USER_ID =
      "SELECT id, user_id, status, created_at FROM accounts WHERE user_id = ? LIMIT 1";

  private final DataSource dataSource;

  @Override
  public AccountModel save(final AccountModel account) {
    try (final Connection connection = dataSource.getConnection();
        final PreparedStatement statement = connection.prepareStatement(SQL_INSERT)) {
      statement.setString(1, account.getId().value());
      statement.setString(2, account.getUserId().value());
      statement.setString(3, account.getStatus().name());
      statement.setTimestamp(4, Timestamp.from(account.getCreatedAt()));
      statement.executeUpdate();
      return account;
    } catch (final SQLIntegrityConstraintViolationException duplicate) {
      // UNIQUE(user_id): otra petición creó la cuenta de este usuario al mismo tiempo
      throw AccountAlreadyExistsException.becauseUserAlreadyHasAccount(
          account.getUserId().value());
    } catch (final SQLException exception) {
      throw PersistenceException.becauseSaveFailed(account.getUserId().value(), exception);
    }
  }

  @Override
  public Optional<AccountModel> getByUserId(final AccountUserId userId) {
    try (final Connection connection = dataSource.getConnection();
        final PreparedStatement statement = connection.prepareStatement(SQL_SELECT_BY_USER_ID)) {
      statement.setString(1, userId.value());
      try (final ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return Optional.empty();
        }
        return Optional.of(AccountPersistenceMapper.fromResultSetToModel(resultSet));
      }
    } catch (final SQLException exception) {
      throw PersistenceException.becauseFindByUserIdFailed(userId.value(), exception);
    }
  }
}
