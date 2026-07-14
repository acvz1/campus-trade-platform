-- Campus Trade Platform - PostgreSQL 14+
-- Idempotent schema used by Spring SQL initialization.

CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    student_id VARCHAR(32),
    real_name VARCHAR(50),
    nickname VARCHAR(50) NOT NULL DEFAULT '校园用户',
    avatar_url VARCHAR(500),
    password_hash VARCHAR(100) NOT NULL,
    contact_phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uk_app_user_phone UNIQUE (phone),
    CONSTRAINT uk_app_user_student_id UNIQUE (student_id),
    CONSTRAINT ck_app_user_phone CHECK (phone ~ '^1[3-9][0-9]{9}$'),
    CONSTRAINT ck_app_user_role CHECK (role IN ('USER', 'ADMIN', 'SUPER_ADMIN')),
    CONSTRAINT ck_app_user_status CHECK (status IN ('ACTIVE', 'BLACKLISTED', 'DISABLED'))
);

-- MVP 管理员账号：13800000000 / Admin@123（首次初始化时创建，可通过环境管理流程修改密码）
INSERT INTO app_user (phone, nickname, password_hash, role, status)
VALUES ('13800000000', '平台管理员', '$2a$10$qYFRqQ1P0mtGv97tsAi2iu.y18qiV36Kjv3/jyOwLwNEJPKQAhemS', 'ADMIN', 'ACTIVE')
ON CONFLICT (phone) DO NOTHING;

CREATE TABLE IF NOT EXISTS user_auth (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    auth_type VARCHAR(20) NOT NULL,
    auth_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    identifier VARCHAR(100) NOT NULL,
    verified_at TIMESTAMPTZ,
    reject_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_user_auth_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT uk_user_auth_identity UNIQUE (user_id, auth_type, identifier),
    CONSTRAINT ck_user_auth_type CHECK (auth_type IN ('PHONE', 'STUDENT_ID', 'MANUAL')),
    CONSTRAINT ck_user_auth_status CHECK (auth_status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE TABLE IF NOT EXISTS user_address (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    contact_name VARCHAR(50) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    campus VARCHAR(100) NOT NULL,
    building VARCHAR(100) NOT NULL,
    room VARCHAR(100),
    detail VARCHAR(255),
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_user_address_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT ck_user_address_phone CHECK (contact_phone ~ '^1[3-9][0-9]{9}$')
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_address_default
    ON user_address(user_id) WHERE is_default = true AND deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS category (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT,
    name VARCHAR(50) NOT NULL,
    icon VARCHAR(255),
    sort_order INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category(id),
    CONSTRAINT ck_category_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_category_sort CHECK (sort_order >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_category_root_name ON category(name) WHERE parent_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_category_child_name ON category(parent_id, name) WHERE parent_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS product (
    id BIGSERIAL PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    title VARCHAR(80) NOT NULL,
    description TEXT NOT NULL,
    price NUMERIC(10,2) NOT NULL,
    original_price NUMERIC(10,2),
    condition_level VARCHAR(20) NOT NULL,
    trade_type VARCHAR(20) NOT NULL,
    trade_location VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_REVIEW',
    view_count INTEGER NOT NULL DEFAULT 0,
    favorite_count INTEGER NOT NULL DEFAULT 0,
    published_at TIMESTAMPTZ,
    sold_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_product_seller FOREIGN KEY (seller_id) REFERENCES app_user(id),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT ck_product_title_len CHECK (char_length(title) BETWEEN 2 AND 80),
    CONSTRAINT ck_product_description_len CHECK (char_length(description) BETWEEN 10 AND 5000),
    CONSTRAINT ck_product_price CHECK (price > 0 AND price <= 99999.99),
    CONSTRAINT ck_product_original_price CHECK (original_price IS NULL OR original_price >= price),
    CONSTRAINT ck_product_condition CHECK (condition_level IN ('NEW', 'LIKE_NEW', 'USED', 'OLD')),
    CONSTRAINT ck_product_trade_type CHECK (trade_type IN ('PICKUP', 'DELIVERY', 'BOTH')),
    CONSTRAINT ck_product_status CHECK (status IN (
        'PENDING_REVIEW', 'ON_SALE', 'REJECTED', 'OFF_SHELF', 'SOLD', 'VIOLATION_DELISTED', 'DELETED'
    )),
    CONSTRAINT ck_product_count CHECK (view_count >= 0 AND favorite_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_product_status_created ON product(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_product_category_status_created ON product(category_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_product_seller_status_created ON product(seller_id, status, created_at DESC);

CREATE TABLE IF NOT EXISTS product_image (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 1,
    is_main BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_product_image_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    CONSTRAINT uk_product_image_sort UNIQUE (product_id, sort_order),
    CONSTRAINT ck_product_image_sort CHECK (sort_order BETWEEN 1 AND 9)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_product_image_main ON product_image(product_id) WHERE is_main = true;

CREATE TABLE IF NOT EXISTS favorite (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_favorite_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    CONSTRAINT uk_favorite_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_favorite_user_created ON favorite(user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS trade_order (
    id BIGSERIAL PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    address_id BIGINT,
    price NUMERIC(10,2) NOT NULL,
    trade_type VARCHAR(20) NOT NULL,
    buyer_remark VARCHAR(200),
    pickup_time TIMESTAMPTZ,
    pickup_location VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_COMMUNICATION',
    cancel_reason VARCHAR(255),
    cancelled_by BIGINT,
    confirmed_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_trade_order_no UNIQUE (order_no),
    CONSTRAINT fk_trade_order_buyer FOREIGN KEY (buyer_id) REFERENCES app_user(id),
    CONSTRAINT fk_trade_order_seller FOREIGN KEY (seller_id) REFERENCES app_user(id),
    CONSTRAINT fk_trade_order_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_trade_order_address FOREIGN KEY (address_id) REFERENCES user_address(id),
    CONSTRAINT fk_trade_order_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES app_user(id),
    CONSTRAINT ck_trade_order_buyer_seller CHECK (buyer_id <> seller_id),
    CONSTRAINT ck_trade_order_price CHECK (price > 0 AND price <= 99999.99),
    CONSTRAINT ck_trade_order_trade_type CHECK (trade_type IN ('PICKUP', 'DELIVERY', 'BOTH')),
    CONSTRAINT ck_trade_order_status CHECK (status IN (
        'PENDING_COMMUNICATION', 'PENDING_PICKUP', 'COMPLETED', 'CANCELLED'
    )),
    CONSTRAINT ck_trade_order_cancel CHECK (
        status <> 'CANCELLED' OR (cancel_reason IS NOT NULL AND cancelled_by IS NOT NULL AND cancelled_at IS NOT NULL)
    )
);

ALTER TABLE trade_order ADD COLUMN IF NOT EXISTS address_id BIGINT;
ALTER TABLE trade_order ADD COLUMN IF NOT EXISTS buyer_remark VARCHAR(200);

CREATE UNIQUE INDEX IF NOT EXISTS uk_trade_order_product_active
    ON trade_order(product_id) WHERE status <> 'CANCELLED';
CREATE INDEX IF NOT EXISTS idx_trade_order_buyer_status_created ON trade_order(buyer_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_trade_order_seller_status_created ON trade_order(seller_id, status, created_at DESC);

CREATE TABLE IF NOT EXISTS conversation (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    last_message VARCHAR(500),
    last_message_at TIMESTAMPTZ,
    buyer_unread_count INTEGER NOT NULL DEFAULT 0,
    seller_unread_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_conversation_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_conversation_buyer FOREIGN KEY (buyer_id) REFERENCES app_user(id),
    CONSTRAINT fk_conversation_seller FOREIGN KEY (seller_id) REFERENCES app_user(id),
    CONSTRAINT uk_conversation_unique UNIQUE (product_id, buyer_id, seller_id),
    CONSTRAINT ck_conversation_buyer_seller CHECK (buyer_id <> seller_id),
    CONSTRAINT ck_conversation_status CHECK (status IN ('ACTIVE', 'CLOSED')),
    CONSTRAINT ck_conversation_unread CHECK (buyer_unread_count >= 0 AND seller_unread_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_conversation_buyer_updated ON conversation(buyer_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_conversation_seller_updated ON conversation(seller_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS message (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    is_read BOOLEAN NOT NULL DEFAULT false,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE,
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES app_user(id),
    CONSTRAINT ck_message_content_len CHECK (char_length(content) BETWEEN 1 AND 1000),
    CONSTRAINT ck_message_type CHECK (message_type IN ('TEXT', 'SYSTEM'))
);

CREATE INDEX IF NOT EXISTS idx_message_conversation_created ON message(conversation_id, created_at DESC);

CREATE TABLE IF NOT EXISTS wanted_post (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    title VARCHAR(80) NOT NULL,
    description TEXT NOT NULL,
    min_price NUMERIC(10,2),
    max_price NUMERIC(10,2),
    expire_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_wanted_post_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_wanted_post_category FOREIGN KEY (category_id) REFERENCES category(id),
    CONSTRAINT ck_wanted_post_title_len CHECK (char_length(title) BETWEEN 2 AND 80),
    CONSTRAINT ck_wanted_post_description_len CHECK (char_length(description) BETWEEN 10 AND 3000),
    CONSTRAINT ck_wanted_post_price CHECK (
        (min_price IS NULL OR min_price >= 0)
        AND (max_price IS NULL OR max_price >= 0)
        AND (min_price IS NULL OR max_price IS NULL OR max_price >= min_price)
    ),
    CONSTRAINT ck_wanted_post_status CHECK (status IN ('OPEN', 'CLOSED', 'EXPIRED', 'DELETED'))
);

CREATE INDEX IF NOT EXISTS idx_wanted_post_category_status_created
    ON wanted_post(category_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_wanted_post_user_created ON wanted_post(user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS blacklist (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    operator_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    removed_reason VARCHAR(255),
    removed_by BIGINT,
    removed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_blacklist_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_blacklist_operator FOREIGN KEY (operator_id) REFERENCES app_user(id),
    CONSTRAINT fk_blacklist_removed_by FOREIGN KEY (removed_by) REFERENCES app_user(id),
    CONSTRAINT ck_blacklist_status CHECK (status IN ('ACTIVE', 'REMOVED')),
    CONSTRAINT ck_blacklist_remove CHECK (
        (status = 'ACTIVE' AND removed_at IS NULL)
        OR (status = 'REMOVED' AND removed_by IS NOT NULL AND removed_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_blacklist_active_user ON blacklist(user_id) WHERE status = 'ACTIVE';

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_audit_log_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_audit_log_operator FOREIGN KEY (operator_id) REFERENCES app_user(id),
    CONSTRAINT ck_audit_log_action CHECK (
        action IN ('APPROVE', 'REJECT', 'VIOLATION_DELIST', 'RELIEVE_VIOLATION')
    )
);

CREATE INDEX IF NOT EXISTS idx_audit_log_product_created ON audit_log(product_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_operator_created ON audit_log(operator_id, created_at DESC);
