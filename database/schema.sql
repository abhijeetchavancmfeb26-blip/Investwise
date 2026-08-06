-- =====================================================================
--  InvestWise-Lite :: MySQL schema
--
--  Simplified from the original: no created_by / updated_by / version
--  columns (auditing of *who* changed a row lives in MongoDB, and
--  optimistic locking was removed as unnecessary for this workload).
-- =====================================================================

CREATE DATABASE IF NOT EXISTS investwise_user       DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS investwise_investment DEFAULT CHARACTER SET utf8mb4;

CREATE USER IF NOT EXISTS 'investwise'@'%' IDENTIFIED BY 'investwise123';
GRANT ALL PRIVILEGES ON investwise_user.*       TO 'investwise'@'%';
GRANT ALL PRIVILEGES ON investwise_investment.* TO 'investwise'@'%';
FLUSH PRIVILEGES;

-- ---------------------------------------------------------------------
--  USER SERVICE
-- ---------------------------------------------------------------------
USE investwise_user;

CREATE TABLE IF NOT EXISTS roles (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name        VARCHAR(50)  NOT NULL,
    last_name         VARCHAR(50)  NOT NULL,
    email             VARCHAR(120) NOT NULL UNIQUE,
    password          VARCHAR(255) NOT NULL,
    phone             VARCHAR(10)  NOT NULL UNIQUE,
    date_of_birth     DATE,
    gender            VARCHAR(20),
    pan_number        VARCHAR(10) UNIQUE,
    annual_income     DECIMAL(15,2),
    occupation        VARCHAR(100),
    address           VARCHAR(300),
    city              VARCHAR(60),
    state             VARCHAR(60),
    pincode           VARCHAR(6),
    email_verified    BOOLEAN  NOT NULL DEFAULT FALSE,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    tier              VARCHAR(20) NOT NULL DEFAULT 'FREE',
    failed_logins     INT      NOT NULL DEFAULT 0,
    last_login_at     DATETIME,
    created_at        DATETIME NOT NULL,
    updated_at        DATETIME NOT NULL,
    INDEX idx_users_status (status),
    INDEX idx_users_tier (tier)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- One table serves both email verification and password reset; the `purpose`
-- column distinguishes them, which is simpler than two near-identical tables.
CREATE TABLE IF NOT EXISTS tokens (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    token      VARCHAR(64)  NOT NULL UNIQUE,
    purpose    VARCHAR(20)  NOT NULL,
    user_id    BIGINT       NOT NULL,
    expires_at DATETIME     NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME     NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_tokens_user (user_id, purpose)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
--  INVESTMENT SERVICE
-- ---------------------------------------------------------------------
USE investwise_investment;

CREATE TABLE IF NOT EXISTS products (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    code           VARCHAR(20)  NOT NULL UNIQUE,
    name           VARCHAR(150) NOT NULL,
    description    TEXT,
    category       VARCHAR(40)  NOT NULL,
    risk_level     VARCHAR(20)  NOT NULL,
    expected_return DECIMAL(5,2) NOT NULL,
    min_investment DECIMAL(15,2) NOT NULL,
    lock_in_months INT          NOT NULL DEFAULT 0,
    fund_house     VARCHAR(120),
    expense_ratio  DECIMAL(4,2),
    rating         INT          NOT NULL DEFAULT 3,
    premium_only   BOOLEAN      NOT NULL DEFAULT FALSE,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME     NOT NULL,
    INDEX idx_products_category (category),
    INDEX idx_products_active (active)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS goals (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id              BIGINT        NOT NULL,
    title                VARCHAR(120)  NOT NULL,
    description          VARCHAR(500),
    goal_type            VARCHAR(40)   NOT NULL,
    target_amount        DECIMAL(15,2) NOT NULL,
    current_amount       DECIMAL(15,2) NOT NULL DEFAULT 0,
    monthly_contribution DECIMAL(15,2) NOT NULL DEFAULT 0,
    target_date          DATE          NOT NULL,
    priority             VARCHAR(20)   NOT NULL DEFAULT 'MEDIUM',
    status               VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at           DATETIME      NOT NULL,
    updated_at           DATETIME      NOT NULL,
    INDEX idx_goals_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS risk_assessments (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT        NOT NULL,
    age              INT           NOT NULL,
    annual_income    DECIMAL(15,2) NOT NULL,
    monthly_surplus  DECIMAL(15,2) NOT NULL,
    dependents       INT           NOT NULL DEFAULT 0,
    horizon_years    INT           NOT NULL,
    knowledge_level  VARCHAR(20)   NOT NULL,
    loss_tolerance   VARCHAR(20)   NOT NULL,
    has_emergency_fund   BOOLEAN   NOT NULL DEFAULT FALSE,
    has_health_insurance BOOLEAN   NOT NULL DEFAULT FALSE,
    score            INT           NOT NULL,
    profile          VARCHAR(30)   NOT NULL,
    equity_pct       INT           NOT NULL,
    debt_pct         INT           NOT NULL,
    gold_pct         INT           NOT NULL,
    created_at       DATETIME      NOT NULL,
    updated_at       DATETIME      NOT NULL,
    INDEX idx_risk_user (user_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS portfolios (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT        NOT NULL UNIQUE,
    total_invested DECIMAL(15,2) NOT NULL DEFAULT 0,
    current_value  DECIMAL(15,2) NOT NULL DEFAULT 0,
    created_at     DATETIME      NOT NULL,
    updated_at     DATETIME      NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS holdings (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id    BIGINT        NOT NULL,
    product_id      BIGINT        NOT NULL,
    goal_id         BIGINT,
    units           DECIMAL(18,4) NOT NULL,
    buy_price       DECIMAL(15,4) NOT NULL,
    current_price   DECIMAL(15,4) NOT NULL,
    invested_amount DECIMAL(15,2) NOT NULL,
    current_value   DECIMAL(15,2) NOT NULL,
    purchase_date   DATE          NOT NULL,
    created_at      DATETIME      NOT NULL,
    updated_at      DATETIME      NOT NULL,
    FOREIGN KEY (portfolio_id) REFERENCES portfolios (id) ON DELETE CASCADE,
    FOREIGN KEY (product_id)   REFERENCES products (id),
    FOREIGN KEY (goal_id)      REFERENCES goals (id) ON DELETE SET NULL,
    INDEX idx_holdings_portfolio (portfolio_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transactions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT        NOT NULL,
    product_id   BIGINT        NOT NULL,
    type         VARCHAR(20)   NOT NULL,
    units        DECIMAL(18,4) NOT NULL,
    price        DECIMAL(15,4) NOT NULL,
    amount       DECIMAL(15,2) NOT NULL,
    reference_no VARCHAR(40)   NOT NULL UNIQUE,
    created_at   DATETIME      NOT NULL,
    INDEX idx_tx_user (user_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS plans (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(20)   NOT NULL UNIQUE,
    name            VARCHAR(80)   NOT NULL,
    description     VARCHAR(500),
    tier            VARCHAR(20)   NOT NULL,
    price           DECIMAL(10,2) NOT NULL,
    duration_months INT           NOT NULL,
    features        TEXT,
    max_goals       INT           NOT NULL DEFAULT 3,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      DATETIME      NOT NULL,
    updated_at      DATETIME      NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS subscriptions (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    plan_id    BIGINT      NOT NULL,
    start_date DATE        NOT NULL,
    end_date   DATE        NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME    NOT NULL,
    updated_at DATETIME    NOT NULL,
    FOREIGN KEY (plan_id) REFERENCES plans (id),
    INDEX idx_sub_user (user_id, status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    user_email      VARCHAR(120),
    subscription_id BIGINT,
    order_id        VARCHAR(60)   NOT NULL UNIQUE,
    payment_id      VARCHAR(60),
    amount          DECIMAL(12,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'CREATED',
    method          VARCHAR(30),
    failure_reason  VARCHAR(255),
    invoice_no      VARCHAR(30),
    created_at      DATETIME      NOT NULL,
    updated_at      DATETIME      NOT NULL,
    FOREIGN KEY (subscription_id) REFERENCES subscriptions (id) ON DELETE SET NULL,
    INDEX idx_pay_user (user_id, created_at),
    INDEX idx_pay_status (status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS articles (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    slug         VARCHAR(220) NOT NULL UNIQUE,
    summary      VARCHAR(500),
    content      LONGTEXT     NOT NULL,
    category     VARCHAR(40)  NOT NULL,
    author       VARCHAR(120),
    read_minutes INT          NOT NULL DEFAULT 5,
    premium_only BOOLEAN      NOT NULL DEFAULT FALSE,
    published    BOOLEAN      NOT NULL DEFAULT TRUE,
    view_count   BIGINT       NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL,
    updated_at   DATETIME     NOT NULL,
    INDEX idx_articles_category (category, published)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS recommendations (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT        NOT NULL,
    goal_id       BIGINT,
    product_id    BIGINT        NOT NULL,
    allocation_pct DECIMAL(5,2) NOT NULL,
    amount        DECIMAL(15,2) NOT NULL,
    match_score   DECIMAL(5,2)  NOT NULL,
    rationale     VARCHAR(500),
    created_at    DATETIME      NOT NULL,
    FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    FOREIGN KEY (goal_id)    REFERENCES goals (id)    ON DELETE CASCADE,
    INDEX idx_rec_user (user_id, created_at)
) ENGINE=InnoDB;
