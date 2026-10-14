package com.jcaa.accounts.application.port.out;

import com.jcaa.accounts.domain.model.AccountModel;
import com.jcaa.accounts.domain.valueobject.AccountUserId;
import java.util.Optional;

public interface GetAccountByUserIdPort {
  Optional<AccountModel> getByUserId(AccountUserId userId);
}
