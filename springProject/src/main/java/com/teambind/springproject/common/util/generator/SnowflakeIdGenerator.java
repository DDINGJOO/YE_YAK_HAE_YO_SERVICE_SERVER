package com.teambind.springproject.common.util.generator;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Hibernate custom ID generator using Snowflake algorithm.
 * Snowflake ID Generator를 Hibernate IdentifierGenerator로 래핑하여 JPA Entity의 ID 생성에 사용.
 */
@Component
public class SnowflakeIdGenerator implements IdentifierGenerator {

  private static PrimaryKeyGenerator primaryKeyGenerator;

  @Autowired
  public void setPrimaryKeyGenerator(final PrimaryKeyGenerator primaryKeyGenerator) {
    SnowflakeIdGenerator.primaryKeyGenerator = primaryKeyGenerator;
  }

  @Override
  public Object generate(final SharedSessionContractImplementor session, final Object object) {
    return primaryKeyGenerator.generateLongKey();
  }
}
